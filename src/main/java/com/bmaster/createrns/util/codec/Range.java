package com.bmaster.createrns.util.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Range(int min, int max) {
    public static final Codec<Range> STRICT_CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.fieldOf("min").forGetter(Range::min),
                    Codec.INT.fieldOf("max").forGetter(Range::max)
            )
            .apply(i, Range::new));

    public static final Codec<Range> FLEXIBLE_CODEC = Codec.either(Codec.INT, STRICT_CODEC)
            .comapFlatMap(Range::decode, Range::encode);

    private static DataResult<Range> decode(Either<Integer, Range> value) {
        var setting = value.map(single -> new Range(single, single),
                range -> new Range(range.min(), range.max()));
        if (setting.min > setting.max) {
            return DataResult.error(() -> "Range min must not exceed max");
        }
        return DataResult.success(setting);
    }

    private Either<Integer, Range> encode() {
        if (min == max)
            return Either.left(min);
        return Either.right(new Range(min, max));
    }
}
