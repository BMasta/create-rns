package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.content.deposit.scanning.DepositScannerChannel;
import com.bmaster.createrns.content.deposit.info.FoundDepositClientCache;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositsClearS2CPacket(ResourceKey<Level> dimension) {
    public static void send(ServerPlayer receiver, ResourceKey<Level> dimension) {
        DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new FoundDepositsClearS2CPacket(dimension));
    }

    public static void sendToAll(MinecraftServer server, ResourceKey<Level> dimension) {
        var packet = new FoundDepositsClearS2CPacket(dimension);
        for (var player : server.getPlayerList().getPlayers()) {
            DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void encode(FoundDepositsClearS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension.location());
    }

    public static FoundDepositsClearS2CPacket decode(FriendlyByteBuf buffer) {
        return new FoundDepositsClearS2CPacket(ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()));
    }

    public static void handle(FoundDepositsClearS2CPacket packet, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> FoundDepositClientCache.clear(packet.dimension()));
        ctx.setPacketHandled(true);
    }
}
