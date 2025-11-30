package com.bmaster.createrns.content.deposit.mining.recipe;

import com.bmaster.createrns.util.Utils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DepositDurability(long core, long edge, float randomSpread) {
    public static final MapCodec<DepositDurability> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Utils.longRangeCodec(1, Long.MAX_VALUE).fieldOf("core").forGetter(DepositDurability::core),
            Utils.longRangeCodec(1, Long.MAX_VALUE).fieldOf("edge").forGetter(DepositDurability::edge),
            Codec.floatRange(0f, 1f).fieldOf("random_spread").forGetter(DepositDurability::randomSpread)
    ).apply(i, DepositDurability::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositDurability> STREAM_CODEC = StreamCodec.of(
            DepositDurability::toNetwork, DepositDurability::fromNetwork);

    public static void toNetwork(RegistryFriendlyByteBuf buffer, DepositDurability dur) {
        ByteBufCodecs.VAR_LONG.encode(buffer, dur.core);
        ByteBufCodecs.VAR_LONG.encode(buffer, dur.edge);
        ByteBufCodecs.FLOAT.encode(buffer, dur.randomSpread);
    }

    public static DepositDurability fromNetwork(RegistryFriendlyByteBuf buffer) {
        return new DepositDurability(
                ByteBufCodecs.VAR_LONG.decode(buffer),
                ByteBufCodecs.VAR_LONG.decode(buffer),
                ByteBufCodecs.FLOAT.decode(buffer)
        );
    }
}
