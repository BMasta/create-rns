package com.bmaster.createrns.content.deposit.scanning;

import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler.AntennaStatus;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler.HeightStatus;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerServerHandler.RequestType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record DepositScannerS2CPacket(
        AntennaStatus antennaStatus, HeightStatus heightStatus, int interval, boolean found, RequestType rt
) {
    public static void send(
            ServerPlayer receiver, AntennaStatus antennaStatus, HeightStatus heightStatus,
            int interval, boolean found, RequestType rt
    ) {
        DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new DepositScannerS2CPacket(antennaStatus, heightStatus, interval, found, rt));
    }

    public static void encode(DepositScannerS2CPacket p, FriendlyByteBuf buf) {
        buf.writeEnum(p.antennaStatus);
        buf.writeEnum(p.heightStatus);
        buf.writeInt(p.interval);
        buf.writeBoolean(p.found);
        buf.writeEnum(p.rt);
    }

    public static DepositScannerS2CPacket decode(FriendlyByteBuf buf) {
        return new DepositScannerS2CPacket(buf.readEnum(AntennaStatus.class), buf.readEnum(HeightStatus.class),
                buf.readInt(), buf.readBoolean(), buf.readEnum(RequestType.class));
    }

    public static void handle(DepositScannerS2CPacket p, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null || !mc.player.level().isClientSide()) return;
            switch (p.rt) {
                case DISCOVER -> DepositScannerClientHandler.processDiscoverReply(p.antennaStatus);
                case TRACK -> DepositScannerClientHandler.processTrackingReply(p.antennaStatus, p.heightStatus, p.interval, p.found);
            }
        });
        ctx.setPacketHandled(true);
    }
}
