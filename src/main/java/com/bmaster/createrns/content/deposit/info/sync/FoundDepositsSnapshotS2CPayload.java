package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSMisc;
import com.bmaster.createrns.content.deposit.info.ClientDepositLocation;
import com.bmaster.createrns.content.deposit.info.FoundDepositClientCache;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositsSnapshotS2CPayload(List<FoundDepositSyncEntry> entries) implements CustomPacketPayload {
    public static final Type<FoundDepositsSnapshotS2CPayload> TYPE =
            new Type<>(CreateRNS.asResource("found_deposits_snapshot_s2c"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoundDepositsSnapshotS2CPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, FoundDepositSyncEntry.STREAM_CODEC),
                    payload -> new ArrayList<>(payload.entries),
                    FoundDepositsSnapshotS2CPayload::new
            );

    public static FoundDepositsSnapshotS2CPayload of(MinecraftServer server) {
        var entries = new ArrayList<FoundDepositSyncEntry>();
        for (var level : server.getAllLevels()) {
            var depData = level.getData(RNSMisc.LEVEL_DEPOSIT_DATA.get());
            for (var deposit : depData.getFoundDeposits()) {
                entries.add(FoundDepositSyncEntry.of(level.dimension(), deposit));
            }
        }
        return new FoundDepositsSnapshotS2CPayload(entries);
    }

    public static void handle(FoundDepositsSnapshotS2CPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var depositsByDimension = new Object2ObjectOpenHashMap<ResourceKey<Level>, Set<ClientDepositLocation>>();
            for (var entry : payload.entries) {
                depositsByDimension.computeIfAbsent(entry.dimension(), ignored -> new ObjectOpenHashSet<>())
                        .add(entry.toClientDepositLocation());
            }
            FoundDepositClientCache.replaceAll(depositsByDimension);
        });
    }

    public FoundDepositsSnapshotS2CPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
