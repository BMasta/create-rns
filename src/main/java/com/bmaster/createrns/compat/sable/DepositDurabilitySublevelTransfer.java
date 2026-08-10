package com.bmaster.createrns.compat.sable;

import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.OptionalLong;

public record DepositDurabilitySublevelTransfer(
        ResourceKey<Level> oldDimension,
        ResourceKey<Level> newDimension,
        BlockState state,
        BlockPos oldPos,
        BlockPos newPos,
        OptionalLong durability
) {
    public static DepositDurabilitySublevelTransfer capture(
            ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos
    ) {
        return new DepositDurabilitySublevelTransfer(
                oldLevel.dimension(), newLevel.dimension(), state, oldPos.immutable(), newPos.immutable(),
                DepositDurabilityManager.getRaw(oldLevel, oldPos));
    }

    public boolean matches(
            ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos
    ) {
        return oldDimension.equals(oldLevel.dimension())
                && newDimension.equals(newLevel.dimension())
                && this.state.equals(state)
                && this.oldPos.equals(oldPos)
                && this.newPos.equals(newPos);
    }
}
