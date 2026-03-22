package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.CreateRNS;
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
public record FoundDepositsSnapshotC2SPayload() implements CustomPacketPayload {
    public static final FoundDepositsSnapshotC2SPayload INSTANCE = new FoundDepositsSnapshotC2SPayload();

    public static final Type<FoundDepositsSnapshotC2SPayload> TYPE =
            new Type<>(CreateRNS.asResource("found_deposits_snapshot_c2s"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoundDepositsSnapshotC2SPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {},
            buffer -> INSTANCE
    );

    public static void handle(FoundDepositsSnapshotC2SPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (!(player instanceof ServerPlayer sp)) return;
            PacketDistributor.sendToPlayer(sp, FoundDepositsSnapshotS2CPayload.of(sp.server));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
