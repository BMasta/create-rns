package com.bmaster.createrns.util;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ExtendedHomogenousList {
    private static final String TAG_PREFIX = "#";

    // Keep holder-set authoring stable across 1.20 and 1.21 by accepting a single id, a list of ids, or a tag.
    public static <T> Codec<HolderSet<T>> of(ResourceKey<? extends Registry<T>> registryKey) {
        var sourceCodec = Codec.either(
                tagKeyCodec(registryKey),
                Codec.either(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf())
        );

        return new Codec<>() {
            @Override
            public <U> DataResult<Pair<HolderSet<T>, U>> decode(DynamicOps<U> ops, U input) {
                return sourceCodec.decode(ops, input)
                        .flatMap(pair -> decodeHolderSet(registryKey, ops, pair));
            }

            @Override
            public <U> DataResult<U> encode(HolderSet<T> input, DynamicOps<U> ops, U prefix) {
                return encodeSource(input).flatMap(source -> sourceCodec.encode(source, ops, prefix));
            }
        };
    }

    private static <T, U> DataResult<Pair<HolderSet<T>, U>> decodeHolderSet(
            ResourceKey<? extends Registry<T>> registryKey, DynamicOps<U> ops,
            Pair<Either<TagKey<T>, Either<ResourceLocation, List<ResourceLocation>>>, U> pair
    ) {
        return pair.getFirst().map(
                tagKey -> decodeNamedHolderSet(registryKey, ops, tagKey),
                direct -> decodeDirectHolderSet(registryKey, ops, direct.map(List::of, ids -> ids))
        ).map(holderSet -> Pair.of(holderSet, pair.getSecond()));
    }

    private static <T, U> DataResult<HolderSet<T>> decodeNamedHolderSet(
            ResourceKey<? extends Registry<T>> registryKey, DynamicOps<U> ops, TagKey<T> tagKey
    ) {
        if (!(ops instanceof RegistryOps<?> registryOps)) {
            return DataResult.error(() -> "Cannot decode tag-backed holder set " + tagKey.location()
                    + " without registry ops for " + registryKey.location());
        }

        var getter = registryOps.getter(registryKey).orElse(null);
        if (getter == null) {
            return DataResult.error(() -> "Missing registry lookup for " + registryKey.location());
        }

        return DataResult.success(getter.getOrThrow(tagKey));
    }

    private static <T, U> DataResult<HolderSet<T>> decodeDirectHolderSet(
            ResourceKey<? extends Registry<T>> registryKey, DynamicOps<U> ops, List<ResourceLocation> ids
    ) {
        if (!(ops instanceof RegistryOps<?> registryOps)) {
            return DataResult.error(() -> "Cannot decode holder set " + ids + " without registry ops for "
                    + registryKey.location());
        }

        var getter = registryOps.getter(registryKey).orElse(null);
        if (getter == null) {
            return DataResult.error(() -> "Missing registry lookup for " + registryKey.location());
        }

        var holders = new ArrayList<Holder<T>>(ids.size());
        for (var id : ids) {
            var key = ResourceKey.create(registryKey, id);
            var holder = getter.get(key).orElse(null);
            if (holder == null) {
                return DataResult.error(() -> "Failed to get element " + id);
            }
            holders.add(holder);
        }

        return DataResult.success(HolderSet.direct(holders));
    }

    private static <T> DataResult<Either<TagKey<T>, Either<ResourceLocation, List<ResourceLocation>>>> encodeSource(
            HolderSet<T> holderSet
    ) {
        return holderSet.unwrap().map(
                tagKey -> DataResult.success(Either.left(tagKey)),
                holders -> holdersToIds(holders).map(ids -> (ids.size() == 1)
                        ? Either.right(Either.left(ids.get(0)))
                        : Either.right(Either.right(ids)))
        );
    }

    private static <T> DataResult<List<ResourceLocation>> holdersToIds(List<Holder<T>> holders) {
        var ids = new ArrayList<ResourceLocation>(holders.size());
        for (var holder : holders) {
            var key = holder.unwrapKey().orElse(null);
            if (key == null) {
                return DataResult.error(() -> "Cannot encode direct holder set without bound registry keys");
            }
            ids.add(key.location());
        }
        return DataResult.success(ids);
    }

    private static <T> Codec<TagKey<T>> tagKeyCodec(ResourceKey<? extends Registry<T>> registryKey) {
        return Codec.STRING.comapFlatMap(
                value -> {
                    if (!value.startsWith(TAG_PREFIX)) {
                        return DataResult.error(() -> "Tag holder sets must start with '#': " + value);
                    }

                    var location = ResourceLocation.tryParse(value.substring(TAG_PREFIX.length()));
                    if (location == null) {
                        return DataResult.error(() -> "Invalid tag id: " + value);
                    }

                    return DataResult.success(TagKey.create(registryKey, location));
                },
                tagKey -> TAG_PREFIX + tagKey.location()
        );
    }
}
