package com.bmaster.createrns.content.deposit.info.sync;

import com.bmaster.createrns.content.deposit.info.ClientDepositLocation;
import com.bmaster.createrns.content.deposit.info.ServerDepositLocation;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FoundDepositSyncEntry(ResourceKey<Level> dimension, ResourceKey<Structure> structureKey, ChunkPos origin,
                                    BlockPos location) {
    private static final StreamCodec<RegistryFriendlyByteBuf, ChunkPos> CHUNK_POS_STREAM_CODEC = StreamCodec.of(
            (buffer, pos) -> ByteBufCodecs.VAR_LONG.encode(buffer, pos.toLong()),
            buffer -> new ChunkPos(ByteBufCodecs.VAR_LONG.decode(buffer))
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FoundDepositSyncEntry> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceKey.streamCodec(Registries.DIMENSION), FoundDepositSyncEntry::dimension,
                    ResourceKey.streamCodec(Registries.STRUCTURE), FoundDepositSyncEntry::structureKey,
                    CHUNK_POS_STREAM_CODEC, FoundDepositSyncEntry::origin,
                    BlockPos.STREAM_CODEC, FoundDepositSyncEntry::location,
                    FoundDepositSyncEntry::new
            );

    public static FoundDepositSyncEntry of(ResourceKey<Level> dimension, ServerDepositLocation deposit) {
        deposit.computePreciseLocation();
        return new FoundDepositSyncEntry(dimension, deposit.getKey(), deposit.getOrigin(), deposit.getLocation());
    }

    public ClientDepositLocation toClientDepositLocation() {
        return new ClientDepositLocation(structureKey, origin, location);
    }
}
