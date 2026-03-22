package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.content.deposit.info.ClientDepositLocation;
import com.bmaster.createrns.content.deposit.info.ServerDepositLocation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositSyncEntry(ResourceKey<Level> dimension, ResourceKey<Structure> structureKey, ChunkPos origin,
                                    BlockPos location) {
    public static void encode(FoundDepositSyncEntry entry, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(entry.dimension.location());
        buffer.writeResourceLocation(entry.structureKey.location());
        buffer.writeVarLong(entry.origin.toLong());
        buffer.writeBlockPos(entry.location);
    }

    public static FoundDepositSyncEntry decode(FriendlyByteBuf buffer) {
        return new FoundDepositSyncEntry(
                ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()),
                ResourceKey.create(Registries.STRUCTURE, buffer.readResourceLocation()),
                new ChunkPos(buffer.readVarLong()),
                buffer.readBlockPos()
        );
    }

    public static FoundDepositSyncEntry of(ResourceKey<Level> dimension, ServerDepositLocation deposit) {
        deposit.computePreciseLocation();
        return new FoundDepositSyncEntry(dimension, deposit.getKey(), deposit.getOrigin(), deposit.getLocation());
    }

    public ClientDepositLocation toClientDepositLocation() {
        return new ClientDepositLocation(structureKey, origin, location);
    }
}
