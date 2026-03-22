package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.content.deposit.info.ClientDepositLocation;
import com.bmaster.createrns.content.deposit.info.FoundDepositClientCache;
import com.bmaster.createrns.content.deposit.info.IDepositIndex;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerChannel;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositsSnapshotS2CPacket(List<FoundDepositSyncEntry> entries) {
    public static void send(ServerPlayer receiver, MinecraftServer server) {
        DepositScannerChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), of(server));
    }

    public static void encode(FoundDepositsSnapshotS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entries.size());
        for (var entry : packet.entries) {
            FoundDepositSyncEntry.encode(entry, buffer);
        }
    }

    public static FoundDepositsSnapshotS2CPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        var entries = new ArrayList<FoundDepositSyncEntry>(size);
        for (int i = 0; i < size; ++i) {
            entries.add(FoundDepositSyncEntry.decode(buffer));
        }
        return new FoundDepositsSnapshotS2CPacket(entries);
    }

    public static FoundDepositsSnapshotS2CPacket of(MinecraftServer server) {
        var entries = new ArrayList<FoundDepositSyncEntry>();
        for (var level : server.getAllLevels()) {
            var depIdx = IDepositIndex.get(level);
            if (depIdx == null) continue;
            for (var deposit : depIdx.getFoundDeposits()) {
                entries.add(FoundDepositSyncEntry.of(level.dimension(), deposit));
            }
        }
        return new FoundDepositsSnapshotS2CPacket(entries);
    }

    public static void handle(FoundDepositsSnapshotS2CPacket packet, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            var depositsByDimension = new Object2ObjectOpenHashMap<ResourceKey<Level>, Set<ClientDepositLocation>>();
            for (var entry : packet.entries) {
                depositsByDimension.computeIfAbsent(entry.dimension(), ignored -> new ObjectOpenHashSet<>())
                        .add(entry.toClientDepositLocation());
            }
            FoundDepositClientCache.replaceAll(depositsByDimension);
        });
        ctx.setPacketHandled(true);
    }

    public FoundDepositsSnapshotS2CPacket {
        entries = List.copyOf(entries);
    }
}
