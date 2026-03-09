package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.util.Utils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record DepositDurability(long core, long edge, float randomSpread) {
    public static final MapCodec<DepositDurability> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Utils.longRangeCodec(1, Long.MAX_VALUE).fieldOf("core").forGetter(DepositDurability::core),
            Utils.longRangeCodec(1, Long.MAX_VALUE).fieldOf("edge").forGetter(DepositDurability::edge),
            Codec.floatRange(0f, 1f).fieldOf("random_spread").forGetter(DepositDurability::randomSpread)
    ).apply(i, DepositDurability::new));

    public static final MapCodec<DepositDurability> STREAM_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.fieldOf("core").forGetter(DepositDurability::core),
            Codec.LONG.fieldOf("edge").forGetter(DepositDurability::edge),
            Codec.FLOAT.fieldOf("random_spread").forGetter(DepositDurability::randomSpread)
    ).apply(i, DepositDurability::new));
}
