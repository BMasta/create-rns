package com.bmaster.createrns.util.codec;

import com.bmaster.createrns.CreateRNS;
import com.mojang.datafixers.util.Either;
import com.mojang.math.MethodsReturnNonnullByDefault;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemWithFallbacks {
    // "minecraft:dirt"                         <-> ItemFallbackEntry("minecraft:dirt", false)
    // "#minecraft:dirt"                        <-> ItemFallbackEntry("minecraft:dirt", true)
    protected static final Codec<ItemFallbackEntry> ITEM_FALLBACK_ENTRY_CODEC = Codec.STRING.comapFlatMap(
            ItemFallbackEntry::decode,
            ItemFallbackEntry::encode
    );
    protected static final StreamCodec<RegistryFriendlyByteBuf, ItemFallbackEntry> ITEM_FALLBACK_ENTRY_STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC, ItemFallbackEntry::id,
                    ByteBufCodecs.BOOL, ItemFallbackEntry::isTag,
                    ItemFallbackEntry::new
            );

    // []                                       <-> ItemWithFallbacks([Items.AIR], encodedAsList=false
    // "minecraft:dirt"                         <-> ItemWithFallbacks(<entry list of size 1>, encodedAsList=false)
    // ["minecraft:dirt"]                       <-> ItemWithFallbacks(<entry list of size 1>, encodedAsList=true)
    // ["minecraft:dirt", "minecraft:stone"]    <-> ItemWithFallbacks(<entry list of size 2>, encodedAsList=true)
    public static final Codec<ItemWithFallbacks> LENIENT_CODEC =
            Codec.either(ITEM_FALLBACK_ENTRY_CODEC, ITEM_FALLBACK_ENTRY_CODEC.listOf()).comapFlatMap(
                    ItemWithFallbacks::decodeLenient,
                    ItemWithFallbacks::encode
            );

    public static final Codec<ItemWithFallbacks> STRICT_CODEC =
            Codec.either(ITEM_FALLBACK_ENTRY_CODEC, ITEM_FALLBACK_ENTRY_CODEC.listOf()).comapFlatMap(
                    ItemWithFallbacks::decodeStrict,
                    ItemWithFallbacks::encode
            );

    /// In addition to the correct format of items and tags, also requires at least one item to resolve.
    /// Disables the resolution check if at least one of the fallback entries is a tag.
    public static final Codec<ItemWithFallbacks> STRICT_RESOLVABLE_CODEC = STRICT_CODEC.validate(iwf ->
            iwf.originalEntries.stream().anyMatch(ItemFallbackEntry::isTag) || iwf.originalEntries.stream().anyMatch(ItemWithFallbacks::isRegisteredItem)
                    ? DataResult.success(iwf)
                    : DataResult.error(() -> "None of the items resolved: " + iwf.originalEntries.stream().map(ItemFallbackEntry::encode).toList()));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemWithFallbacks> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), iwf -> iwf.item,
            ByteBufCodecs.collection(ArrayList::new, ITEM_FALLBACK_ENTRY_STREAM_CODEC),
            iwf -> new ArrayList<>(iwf.originalEntries),
            ByteBufCodecs.BOOL, iwf -> iwf.encodedAsList,
            ItemWithFallbacks::new
    );

    public static final ItemWithFallbacks EMPTY = new ItemWithFallbacks(List.of(), true);

    private static DataResult<ItemWithFallbacks> decode(List<ItemFallbackEntry> entries, boolean asList, boolean lenient) {
        if (entries.isEmpty() && !lenient) return DataResult.error(() -> "No items or item tags specified");
        return DataResult.success(new ItemWithFallbacks(entries, (entries.isEmpty() || asList)));
    }

    private static boolean isRegisteredItem(ItemFallbackEntry entry) {
        return BuiltInRegistries.ITEM.getOptional(entry.id())
                .filter(item -> item != Items.AIR)
                .isPresent();
    }

    private static DataResult<ItemWithFallbacks> decodeStrict(Either<ItemFallbackEntry, List<ItemFallbackEntry>> serialized) {
        return serialized.map(
                entry -> decode(List.of(entry), false, false),
                entries -> decode(entries, true, false)
        );
    }

    private static DataResult<ItemWithFallbacks> decodeLenient(Either<ItemFallbackEntry, List<ItemFallbackEntry>> serialized) {
        return serialized.map(
                entry -> decode(List.of(entry), false, true),
                entries -> decode(entries, true, true)
        );
    }

    private static Either<ItemFallbackEntry, List<ItemFallbackEntry>> encode(ItemWithFallbacks iwf) {
        if (!iwf.encodedAsList && iwf.originalEntries.size() == 1) {
            return Either.left(iwf.originalEntries.getFirst());
        }
        return Either.right(iwf.originalEntries);
    }

    public Item item = Items.AIR;
    protected final List<ItemFallbackEntry> originalEntries;
    protected final boolean encodedAsList;

    public boolean resolve(RegistryAccess access, boolean lenient) {
        var report = new ArrayList<String>();
        var itemLookup = access.lookupOrThrow(Registries.ITEM);

        Item resolved = null;
        for (var entry : originalEntries) {
            if (!entry.isTag()) {
                resolved = BuiltInRegistries.ITEM.getOptional(entry.id()).orElse(null);
                if (resolved == null) {
                    report.add("Could not resolve item \"" + entry.id + "\"");
                    continue;
                }
            } else {
                var tag = TagKey.create(Registries.ITEM, entry.id());
                var named = itemLookup.get(tag).orElse(null);
                if (named == null) {
                    report.add("Could not resolve item tag \"#" + entry.id + "\"");
                    continue;
                }

                resolved = named.stream()
                        .map(Holder::value)
                        .findFirst()
                        .orElse(null);
                if (resolved == null) {
                    report.add("Item tag \"#" + entry.id + "\" does not contain any items");
                    continue;
                }

                // Mods like One Enough Item may dynamically substitute certain items with others.
                // If an item stack is created with a different item, use the stack item instead.
                var testStack = new ItemStack(resolved);
                if (resolved != testStack.getItem()) {
                    resolved = testStack.getItem();
                }
            }
            break;
        }

        if (lenient && resolved == null) resolved = Items.AIR;
        if (resolved == null) {
            if (report.isEmpty()) CreateRNS.LOGGER.error("No items or item tags specified");
            for (var line : report) CreateRNS.LOGGER.error(line);
            return false;
        } else {
            item = resolved;
            return true;
        }
    }

    /// For codec
    protected ItemWithFallbacks(List<ItemFallbackEntry> originalEntries, boolean encodedAsList) {
        if (originalEntries.size() > 1 && !encodedAsList) {
            throw new IllegalArgumentException("More than one fallback specified, " +
                    "but expected to be encoded as a single entry");
        }

        this.originalEntries = List.copyOf(originalEntries);
        this.encodedAsList = encodedAsList;
    }

    /// For stream codec
    protected ItemWithFallbacks(Item resolvedItem, List<ItemFallbackEntry> originalEntries, boolean encodedAsList) {
        if (originalEntries.size() > 1 && !encodedAsList) {
            throw new IllegalArgumentException("More than one fallback specified, " +
                    "but expected to be encoded as a single entry");
        }

        this.item = resolvedItem;
        this.originalEntries = List.copyOf(originalEntries);
        this.encodedAsList = encodedAsList;
    }

    @Override
    public String toString() {
        return item.toString();
    }

    protected record ItemFallbackEntry(ResourceLocation id, boolean isTag) {
        public String encode() {
            return isTag ? "#" + id : id.toString();
        }

        private static DataResult<ItemFallbackEntry> decode(String entry) {
            if (entry.startsWith("#")) {
                if (entry.length() == 1) {
                    return DataResult.error(() -> "Item tag entry must include a tag id after '#'");
                }
                return ResourceLocation.read(entry.substring(1))
                        .map(id -> new ItemFallbackEntry(id, true));
            }

            return ResourceLocation.read(entry)
                    .map(id -> new ItemFallbackEntry(id, false));
        }
    }
}
