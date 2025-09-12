package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.item.DepositScanner.DepositScannerServerHandler.RequestType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler.AntennaStatus;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record DepositScannerS2CPacket(AntennaStatus antennaStatus, int interval, @Nullable BlockPos foundDepositCenter,
                                      RequestType rt) {
    public static void send(ServerPlayer receiver, AntennaStatus status, int interval, BlockPos foundDepositCenter,
                            RequestType rt) {
        DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new DepositScannerS2CPacket(status, interval, foundDepositCenter, rt));
    }

    public static void encode(DepositScannerS2CPacket p, FriendlyByteBuf buf) {
        buf.writeNullable(p.foundDepositCenter, FriendlyByteBuf::writeBlockPos);
        buf.writeEnum(p.antennaStatus);
        buf.writeInt(p.interval);
        buf.writeEnum(p.rt);
    }

    public static DepositScannerS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos foundDepositCenter = buf.readNullable(FriendlyByteBuf::readBlockPos);
        return new DepositScannerS2CPacket(buf.readEnum(AntennaStatus.class), buf.readInt(), foundDepositCenter,
                buf.readEnum(RequestType.class));
    }

    public static void handle(DepositScannerS2CPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || !mc.player.level().isClientSide()) return;
            switch (p.rt) {
                case DISCOVER -> DepositScannerClientHandler.processDiscoverReply(
                        p.antennaStatus, p.interval, p.foundDepositCenter);
                case TRACK -> DepositScannerClientHandler.processTrackingReply(
                        p.antennaStatus, p.interval, p.foundDepositCenter);
            }
        });
        ctx.setPacketHandled(true);
    }
}
