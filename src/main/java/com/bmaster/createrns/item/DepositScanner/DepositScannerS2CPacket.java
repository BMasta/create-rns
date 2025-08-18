package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.bmaster.createrns.item.DepositScanner.DepositScannerItemRenderer.AntennaStatus;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record DepositScannerS2CPacket(AntennaStatus antennaStatus, int interval, boolean found) {
    public static void send(ServerPlayer receiver, AntennaStatus status, int interval, boolean oreChunkFound) {
        DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new DepositScannerS2CPacket(status, interval, oreChunkFound));
    }

    public static void encode(DepositScannerS2CPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.antennaStatus);
        buf.writeInt(p.interval);
        buf.writeBoolean(p.found);
    }

    public static DepositScannerS2CPacket decode(FriendlyByteBuf buf) {
        return new DepositScannerS2CPacket(buf.readEnum(AntennaStatus.class), buf.readInt(), buf.readBoolean());
    }

    public static void handle(DepositScannerS2CPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || !mc.player.level().isClientSide()) return;
            DepositScannerClientHandler.processScanReply(p.antennaStatus, p.interval, p.found);
        });
        ctx.setPacketHandled(true);
    }
}
