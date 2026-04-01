package com.bmaster.createrns.util;

import com.mojang.serialization.*;

import java.util.Objects;
import java.util.stream.Stream;

public class StrictOptionalField {
    public static <T> MapCodec<T> of(String fieldName, Codec<T> codec, T defaultValue) {
        return MapCodec.of(new MapEncoder.Implementation<>() {
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
                    public <O> com.mojang.serialization.DataResult<T> decode(DynamicOps<O> ops, MapLike<O> input) {
                        var value = input.get(fieldName);
                        if (value == null) return com.mojang.serialization.DataResult.success(defaultValue);
                        return codec.parse(ops, value);
                    }
                },
                () -> "StrictOptionalField[" + fieldName + "]");
    }
}
