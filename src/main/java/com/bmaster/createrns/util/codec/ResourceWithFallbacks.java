package com.bmaster.createrns.util.codec;

import com.bmaster.createrns.CreateRNS;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ResourceWithFallbacks<T> {
    protected static <T, F extends ResourceWithFallbacks<T>> Codec<F> createCodec(
            BiFunction<List<ResourceOrTagLocation>, Boolean, F> factory, boolean lenient
    ) {
        return Codec.either(ResourceOrTagLocation.CODEC, ResourceOrTagLocation.CODEC.listOf()).comapFlatMap(
                serialized -> {
                    var fallback = serialized.map(
                            entry -> factory.apply(List.of(entry), false),
                            entries -> factory.apply(entries, true)
                    );
                    if (!lenient && fallback.originalEntries.isEmpty()) {
                        return DataResult.error(() -> "No " + fallback.resourceLogIdentifier()
                                + " resources or tags specified");
                    }
                    return DataResult.success(fallback);
                },
                fallback -> !fallback.encodedAsList && fallback.originalEntries.size() == 1
                        ? Either.left(fallback.originalEntries.getFirst())
                        : Either.right(fallback.originalEntries)
        );
    }

    /// Tags must be resolved later, once live registry data is available.
    protected static <T, F extends ResourceWithFallbacks<T>> DataResult<F> validateResolvable(F fallback) {
        if (fallback.originalEntries.stream().anyMatch(ResourceOrTagLocation::isTag)
                || fallback.originalEntries.stream().anyMatch(entry -> fallback.registry().getOptional(entry.id())
                        .filter(value -> value != fallback.emptyFallbackValue()).isPresent())) {
            return DataResult.success(fallback);
        }
        return DataResult.error(() -> "None of the " + fallback.resourceLogIdentifier()
                + " resources or tags resolved: "
                + fallback.originalEntries.stream().map(ResourceOrTagLocation::encode).toList());
    }

    protected static <T, F extends ResourceWithFallbacks<T>> StreamCodec<RegistryFriendlyByteBuf, F> createStreamCodec(
            ResourceKey<? extends Registry<T>> registryKey,
            Function3<T, List<ResourceOrTagLocation>, Boolean, F> factory
    ) {
        return StreamCodec.composite(
                ByteBufCodecs.registry(registryKey), F::value,
                ByteBufCodecs.collection(ArrayList::new, ResourceOrTagLocation.STREAM_CODEC),
                fallback -> new ArrayList<>(fallback.originalEntries),
                ByteBufCodecs.BOOL, fallback -> fallback.encodedAsList,
                factory
        );
    }

    protected final List<ResourceOrTagLocation> originalEntries;
    protected final boolean encodedAsList;
    private T value;

    public T value() {
        return value;
    }

    public boolean resolve(RegistryAccess access, boolean lenient) {
        var report = new ArrayList<String>();
        var registry = registry();
        var lookup = access.lookupOrThrow(registry.key());
        var resourceName = resourceLogIdentifier();

        T resolved = null;
        for (var entry : originalEntries) {
            if (!entry.isTag()) {
                resolved = registry.getOptional(entry.id()).orElse(null);
                if (resolved == null) {
                    report.add("Could not resolve " + resourceName + " \"" + entry.id() + "\"");
                    continue;
                }
            } else {
                var tag = TagKey.create(registry.key(), entry.id());
                var named = lookup.get(tag).orElse(null);
                if (named == null) {
                    report.add("Could not resolve " + resourceName + " tag \"#" + entry.id() + "\"");
                    continue;
                }

                resolved = named.stream().map(Holder::value).findFirst().orElse(null);
                if (resolved == null) {
                    report.add("Tag \"#" + entry.id() + "\" does not contain any " + resourceName + " resources");
                    continue;
                }

                // One Enough Item can substitute tag results through resource stacks.
                resolved = resolveFromResourceStack(resolved);
            }
            break;
        }

        if (lenient && resolved == null) resolved = emptyFallbackValue();
        if (resolved == null) {
            if (report.isEmpty()) CreateRNS.LOGGER.error("No {} resources or tags specified", resourceName);
            for (var line : report) CreateRNS.LOGGER.error(line);
            return false;
        }
        value = resolved;
        return true;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    protected ResourceWithFallbacks(T value, List<ResourceOrTagLocation> originalEntries, boolean encodedAsList) {
        if (originalEntries.size() > 1 && !encodedAsList) {
            throw new IllegalArgumentException("More than one fallback specified, "
                    + "but expected to be encoded as a single entry");
        }
        this.value = value;
        this.originalEntries = List.copyOf(originalEntries);
        this.encodedAsList = encodedAsList;
    }

    protected abstract Registry<T> registry();

    protected abstract T emptyFallbackValue();

    protected abstract String resourceLogIdentifier();

    /// Optional compatibility hook for resources with a stack representation.
    protected T resolveFromResourceStack(T resource) {
        return resource;
    }
}
