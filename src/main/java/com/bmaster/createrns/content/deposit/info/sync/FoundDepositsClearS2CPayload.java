package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.info.FoundDepositClientCache;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositsClearS2CPayload(ResourceKey<Level> dimension) implements CustomPacketPayload {
    public static final Type<FoundDepositsClearS2CPayload> TYPE =
            new Type<>(CreateRNS.asResource("found_deposits_clear_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoundDepositsClearS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.DIMENSION), FoundDepositsClearS2CPayload::dimension,
                    FoundDepositsClearS2CPayload::new
            );

    public static void handle(FoundDepositsClearS2CPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> FoundDepositClientCache.clear(payload.dimension()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
