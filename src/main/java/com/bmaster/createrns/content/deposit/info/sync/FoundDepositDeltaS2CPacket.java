package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.content.deposit.info.FoundDepositClientCache;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerChannel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositDeltaS2CPacket(Operation operation, FoundDepositSyncEntry entry) {

    public enum Operation {
        ADD, REMOVE
    }

    public static void sendToAll(MinecraftServer server, Operation operation, FoundDepositSyncEntry entry) {
        var packet = new FoundDepositDeltaS2CPacket(operation, entry);
        for (var player : server.getPlayerList().getPlayers()) {
            DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void encode(FoundDepositDeltaS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.operation);
        FoundDepositSyncEntry.encode(packet.entry, buffer);
    }

    public static FoundDepositDeltaS2CPacket decode(FriendlyByteBuf buffer) {
        return new FoundDepositDeltaS2CPacket(buffer.readEnum(Operation.class), FoundDepositSyncEntry.decode(buffer));
    }

    public static void handle(FoundDepositDeltaS2CPacket packet, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var location = packet.entry.toClientDepositLocation();
            switch (packet.operation) {
                case ADD -> FoundDepositClientCache.add(packet.entry.dimension(), location);
                case REMOVE -> FoundDepositClientCache.remove(packet.entry.dimension(), location);
            }
        });
        ctx.setPacketHandled(true);
    }
}
