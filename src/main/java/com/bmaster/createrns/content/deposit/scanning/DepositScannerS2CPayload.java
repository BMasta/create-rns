package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler.AntennaStatus;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerServerHandler.RequestType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record DepositScannerS2CPayload(AntennaStatus antennaStatus, int interval, boolean found,
                                       RequestType rt) implements CustomPacketPayload {
    public static final Type<DepositScannerS2CPayload> TYPE =
            new Type<>(CreateRNS.asResource("deposit_scanner_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DepositScannerS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    NeoForgeStreamCodecs.enumCodec(AntennaStatus.class), DepositScannerS2CPayload::antennaStatus,
                    ByteBufCodecs.INT, DepositScannerS2CPayload::interval,
                    ByteBufCodecs.BOOL, DepositScannerS2CPayload::found,
                    NeoForgeStreamCodecs.enumCodec(RequestType.class), DepositScannerS2CPayload::rt,
                    DepositScannerS2CPayload::new
            );

    public static void handle(DepositScannerS2CPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || !mc.player.level().isClientSide()) return;
            switch (p.rt) {
                case DISCOVER -> DepositScannerClientHandler.processDiscoverReply(p.antennaStatus);
                case TRACK -> DepositScannerClientHandler.processTrackingReply(p.antennaStatus, p.interval, p.found);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
