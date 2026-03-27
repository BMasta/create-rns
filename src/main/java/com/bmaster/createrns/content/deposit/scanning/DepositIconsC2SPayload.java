package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record DepositIconsC2SPayload() implements CustomPacketPayload {
    public static final DepositIconsC2SPayload INSTANCE = new DepositIconsC2SPayload();

    public static final Type<DepositIconsC2SPayload> TYPE =
            new Type<>(CreateRNS.asResource("deposit_icons_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositIconsC2SPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {},
            buffer -> INSTANCE
    );

    public static void handle(DepositIconsC2SPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player instanceof ServerPlayer sp)) return;
            PacketDistributor.sendToPlayer(sp, new DepositIconsS2CPayload(DepositSpecLookup.getScannerIcons(sp.server)));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
