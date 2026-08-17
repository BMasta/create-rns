package com.bmaster.createrns.util.codec;

import com.bmaster.createrns.CreateRNS;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class FluidWithFallbacks {
    // "minecraft:lava"                         <-> FluidFallbackEntry("minecraft:lava", false)
    // "#minecraft:lava"                        <-> FluidFallbackEntry("minecraft:lava", true)
    protected static final Codec<FluidFallbackEntry> FLUID_FALLBACK_ENTRY_CODEC = Codec.STRING.comapFlatMap(
            FluidFallbackEntry::decode,
            FluidFallbackEntry::encode
    );
    protected static final StreamCodec<RegistryFriendlyByteBuf, FluidFallbackEntry> FLUID_FALLBACK_ENTRY_STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, FluidFallbackEntry::id,
                    ByteBufCodecs.BOOL, FluidFallbackEntry::isTag,
                    FluidFallbackEntry::new
            );

    // []                                       <-> FluidWithFallbacks([Fluids.EMPTY], encodedAsList=false
    // "minecraft:lava"                         <-> FluidWithFallbacks(<entry list of size 1>, encodedAsList=false)
    // ["minecraft:lava"]                       <-> FluidWithFallbacks(<entry list of size 1>, encodedAsList=true)
    // ["minecraft:lava", "minecraft:water"]    <-> FluidWithFallbacks(<entry list of size 2>, encodedAsList=true)
    public static final Codec<FluidWithFallbacks> LENIENT_CODEC =
            Codec.either(FLUID_FALLBACK_ENTRY_CODEC, FLUID_FALLBACK_ENTRY_CODEC.listOf()).comapFlatMap(
                    FluidWithFallbacks::decodeLenient,
                    FluidWithFallbacks::encode
            );

    public static final Codec<FluidWithFallbacks> STRICT_CODEC =
            Codec.either(FLUID_FALLBACK_ENTRY_CODEC, FLUID_FALLBACK_ENTRY_CODEC.listOf()).comapFlatMap(
                    FluidWithFallbacks::decodeStrict,
                    FluidWithFallbacks::encode
            );

    /// In addition to the correct format of fluids and tags, also requires at least one fluid to resolve.
    /// Disables the resolution check if at least one of the fallback entries is a tag.
    public static final Codec<FluidWithFallbacks> STRICT_RESOLVABLE_CODEC = STRICT_CODEC.validate(fwf ->
            fwf.originalEntries.stream().anyMatch(FluidFallbackEntry::isTag) || fwf.originalEntries.stream().anyMatch(FluidWithFallbacks::isRegisteredFluid)
                    ? DataResult.success(fwf)
                    : DataResult.error(() -> "None of the fluids resolved: " + fwf.originalEntries.stream().map(FluidFallbackEntry::encode).toList()));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidWithFallbacks> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.FLUID), fwf -> fwf.fluid,
            ByteBufCodecs.collection(ArrayList::new, FLUID_FALLBACK_ENTRY_STREAM_CODEC),
            fwf -> new ArrayList<>(fwf.originalEntries),
            ByteBufCodecs.BOOL, fwf -> fwf.encodedAsList,
            FluidWithFallbacks::new
    );

    public static final FluidWithFallbacks EMPTY = new FluidWithFallbacks(List.of(), true);

    private static DataResult<FluidWithFallbacks> decode(List<FluidFallbackEntry> entries, boolean asList, boolean lenient) {
        if (entries.isEmpty() && !lenient) return DataResult.error(() -> "No fluids or fluid tags specified");
        return DataResult.success(new FluidWithFallbacks(entries, (entries.isEmpty() || asList)));
    }

    private static boolean isRegisteredFluid(FluidFallbackEntry entry) {
        return BuiltInRegistries.FLUID.getOptional(entry.id())
                .filter(fluid -> fluid != Fluids.EMPTY)
                .isPresent();
    }

    private static DataResult<FluidWithFallbacks> decodeStrict(Either<FluidFallbackEntry, List<FluidFallbackEntry>> serialized) {
        return serialized.map(
                entry -> decode(List.of(entry), false, false),
                entries -> decode(entries, true, false)
        );
    }

    private static DataResult<FluidWithFallbacks> decodeLenient(Either<FluidFallbackEntry, List<FluidFallbackEntry>> serialized) {
        return serialized.map(
                entry -> decode(List.of(entry), false, true),
                entries -> decode(entries, true, true)
        );
    }

    private static Either<FluidFallbackEntry, List<FluidFallbackEntry>> encode(FluidWithFallbacks fwf) {
        if (!fwf.encodedAsList && fwf.originalEntries.size() == 1) {
            return Either.left(fwf.originalEntries.getFirst());
        }
        return Either.right(fwf.originalEntries);
    }

    public Fluid fluid = Fluids.EMPTY;
    protected final List<FluidFallbackEntry> originalEntries;
    protected final boolean encodedAsList;

    public boolean resolve(RegistryAccess access, boolean lenient) {
        var report = new ArrayList<String>();
        var fluidLookup = access.lookupOrThrow(Registries.FLUID);

        Fluid resolved = null;
        for (var entry : originalEntries) {
            if (!entry.isTag()) {
                resolved = BuiltInRegistries.FLUID.getOptional(entry.id()).orElse(null);
                if (resolved == null) {
                    report.add("Could not resolve fluid \"" + entry.id + "\"");
                    continue;
                }
            } else {
                var tag = TagKey.create(Registries.FLUID, entry.id());
                var named = fluidLookup.get(tag).orElse(null);
                if (named == null) {
                    report.add("Could not resolve fluid tag \"#" + entry.id + "\"");
                    continue;
                }

                resolved = named.stream()
                        .map(Holder::value)
                        .findFirst()
                        .orElse(null);
                if (resolved == null) {
                    report.add("Fluid tag \"#" + entry.id + "\" does not contain any fluids");
                    continue;
                }

                // Mods like One Enough Item may dynamically substitute certain fluids with others.
                // If a fluid stack is created with a different fluid, use the stack fluid instead.
                var testStack = new FluidStack(resolved, 1);
                if (resolved != testStack.getFluid()) {
                    resolved = testStack.getFluid();
                }
            }
            break;
        }

        if (lenient && resolved == null) resolved = Fluids.EMPTY;
        if (resolved == null) {
            if (report.isEmpty()) CreateRNS.LOGGER.error("No fluids or fluid tags specified");
            for (var line : report) CreateRNS.LOGGER.error(line);
            return false;
        } else {
            fluid = resolved;
            return true;
        }
    }

    /// For codec
    protected FluidWithFallbacks(List<FluidFallbackEntry> originalEntries, boolean encodedAsList) {
        if (originalEntries.size() > 1 && !encodedAsList) {
            throw new IllegalArgumentException("More than one fallback specified, " +
                    "but expected to be encoded as a single entry");
        }

        this.originalEntries = List.copyOf(originalEntries);
        this.encodedAsList = encodedAsList;
    }

    /// For stream codec
    protected FluidWithFallbacks(Fluid resolvedFluid, List<FluidFallbackEntry> originalEntries, boolean encodedAsList) {
        if (originalEntries.size() > 1 && !encodedAsList) {
            throw new IllegalArgumentException("More than one fallback specified, " +
                    "but expected to be encoded as a single entry");
        }

        this.fluid = resolvedFluid;
        this.originalEntries = List.copyOf(originalEntries);
        this.encodedAsList = encodedAsList;
    }

    @Override
    public String toString() {
        return fluid.toString();
    }

    protected record FluidFallbackEntry(ResourceLocation id, boolean isTag) {
        public String encode() {
            return isTag ? "#" + id : id.toString();
        }

        private static DataResult<FluidFallbackEntry> decode(String entry) {
            if (entry.startsWith("#")) {
                if (entry.length() == 1) {
                    return DataResult.error(() -> "Fluid tag entry must include a tag id after '#'");
                }
                return ResourceLocation.read(entry.substring(1))
                        .map(id -> new FluidFallbackEntry(id, true));
            }

            return ResourceLocation.read(entry)
                    .map(id -> new FluidFallbackEntry(id, false));
        }
    }
}
