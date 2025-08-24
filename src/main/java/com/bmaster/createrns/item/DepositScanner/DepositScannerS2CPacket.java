package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler.AntennaStatus;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public record DepositScannerS2CPacket(AntennaStatus antennaStatus, int interval, @Nullable BlockPos foundDepositCenter) {
    public static void send(ServerPlayer receiver, AntennaStatus status, int interval, BlockPos foundDepositCenter) {
        DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new DepositScannerS2CPacket(status, interval, foundDepositCenter));
    }

    public static void encode(DepositScannerS2CPacket p, FriendlyByteBuf buf) {
        buf.writeNullable(p.foundDepositCenter, FriendlyByteBuf::writeBlockPos);
        buf.writeEnum(p.antennaStatus);
        buf.writeInt(p.interval);
    }

    public static DepositScannerS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos foundDepositCenter = buf.readNullable(FriendlyByteBuf::readBlockPos);
        return new DepositScannerS2CPacket(buf.readEnum(AntennaStatus.class), buf.readInt(), foundDepositCenter);
    }

    public static void handle(DepositScannerS2CPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || !mc.player.level().isClientSide()) return;
            DepositScannerClientHandler.processScanReply(p.antennaStatus, p.interval, p.foundDepositCenter);
        });
        ctx.setPacketHandled(true);
    }
}
