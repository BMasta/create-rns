package com.bmaster.createrns.deposit.capability;

import com.bmaster.createrns.RNSContent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

public interface IDepositIndex {
    @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks);

    @Nullable BlockPos getNearestCached(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks);

    void add(ResourceKey<Structure> depositKey, StructureStart ss, ServerLevel sl);

    void markAsFound(BlockPos centerPos);

    // TODO: Level caps are gone. Migrate to level data attachments
//    static LazyOptional<IDepositIndex> fromLevel(ServerLevel level) {
//        return level.getCapability(RNSContent.DEPOSIT_INDEX);
//    }
}
