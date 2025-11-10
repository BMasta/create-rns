package com.bmaster.createrns.deposit.capability;

import com.bmaster.createrns.RNSContent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public interface IDepositIndex {
    @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks);

    @Nullable BlockPos getNearestCached(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks);

    void add(ResourceKey<Structure> depositKey, StructureStart ss, ServerLevel sl);

    void markAsFound(BlockPos centerPos);

    void initDepositVeinDurability(BlockPos start);

    void removeDepositBlockDurability(BlockPos dbPos);

    void useDepositBlock(BlockPos dbPos, BlockState replacementBlock);

    static @Nullable DepositIndex fromLevel(ServerLevel level) {
        var cap = level.getCapability(RNSContent.DEPOSIT_INDEX).resolve().orElse(null);
        if (!(cap instanceof DepositIndex depIdx)) return null;
        depIdx.setLevel(level);
        return depIdx;
    }
}
