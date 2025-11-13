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

    @Nullable BlockPos getNearest(ResourceKey<Structure> depositKey, ServerLevel level, BlockPos pos,
                                  int searchRadiusChunks, boolean allow_discovered, boolean generatedOnly);

    @Nullable BlockPos getNearestCached(ResourceKey<Structure> depositKey, ServerPlayer sp, int searchRadiusChunks);

    @Nullable ResourceKey<Structure> getType(BlockPos pos);

    void addDeposit(ResourceKey<Structure> depositKey, StructureStart ss);

    void addDeposit(ResourceKey<Structure> depositKey, BlockPos pos);

    boolean removeDeposit(BlockPos pos);

    boolean isFound(BlockPos pos);

    boolean setFound(ResourceKey<Structure> depositKey, BlockPos centerPos, boolean val);

    int initDepositVeinDurability(BlockPos start);

    long getDepositBlockDurability(BlockPos dbPos, boolean initIfNeeded);

    long getDepositBlockDurability(BlockPos dbPos);

    boolean setDepositBlockDurability(BlockPos dbPos, long durability);

    void removeDepositBlockDurability(BlockPos dbPos);

    void useDepositBlock(BlockPos dbPos, BlockState replacementBlock);

    static @Nullable DepositIndex fromLevel(ServerLevel level) {
        var cap = level.getCapability(RNSContent.DEPOSIT_INDEX).resolve().orElse(null);
        if (!(cap instanceof DepositIndex depIdx)) return null;
        depIdx.setLevel(level);
        return depIdx;
    }

    static DepositIndex fromLevelOrThrow(ServerLevel level) {
        var cap = fromLevel(level);
        if (cap == null) throw new RuntimeException("Level " + level + " does not have a deposit index");
        return cap;
    }
}
