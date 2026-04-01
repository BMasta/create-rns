package com.bmaster.createrns.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class StrictOptionalField {
    private StrictOptionalField() {
    }

    public static <T> MapCodec<T> of(String fieldName, Codec<T> codec, T defaultValue) {
        return MapCodec.of(
                new MapEncoder.Implementation<>() {
                    @Override
                    public <O> Stream<O> keys(DynamicOps<O> ops) {
                        return Stream.of(ops.createString(fieldName));
                    }

                    @Override
                    public <O> RecordBuilder<O> encode(T input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
                        if (Objects.equals(input, defaultValue)) return prefix;
                        return prefix.add(fieldName, codec.encodeStart(ops, input));
                    }
                },
                new MapDecoder.Implementation<>() {
                    @Override
                    public <O> Stream<O> keys(DynamicOps<O> ops) {
                        return Stream.of(ops.createString(fieldName));
                    }

                    @Override
                    public <O> DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
                        var value = input.get(fieldName);
                        if (value == null) return DataResult.success(defaultValue);
                        return codec.parse(ops, value);
                    }
                },
                () -> "StrictOptionalField[" + fieldName + "]"
        );
    }

    public static <T> MapCodec<Optional<T>> of(String fieldName, Codec<T> codec) {
        return MapCodec.of(
                new MapEncoder.Implementation<>() {
                    @Override
                    public <O> Stream<O> keys(DynamicOps<O> ops) {
                        return Stream.of(ops.createString(fieldName));
                    }

                    @Override
                    public <O> RecordBuilder<O> encode(Optional<T> input, DynamicOps<O> ops, RecordBuilder<O> prefix) {
                        if (input.isEmpty()) return prefix;
                        return prefix.add(fieldName, codec.encodeStart(ops, input.get()));
                    }
                },
                new MapDecoder.Implementation<>() {
                    @Override
                    public <O> Stream<O> keys(DynamicOps<O> ops) {
                        return Stream.of(ops.createString(fieldName));
                    }

                    @Override
                    public <O> DataResult<Optional<T>> decode(DynamicOps<O> ops, MapLike<O> input) {
                        var value = input.get(fieldName);
                        if (value == null) return DataResult.success(Optional.empty());
                        return codec.parse(ops, value).map(Optional::of);
                    }
                },
                () -> "StrictOptionalField[" + fieldName + "]"
        );
    }
}
