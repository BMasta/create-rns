package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead.DrillHeadMultiblock;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSMovementChecks {
    protected static boolean registered = false;

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        BlockMovementChecks.registerAttachedCheck(RNSMovementChecks::isDrillHeadAttachedTowards);
        BlockMovementChecks.registerBrittleCheck(RNSMovementChecks::isDrillHeadBrittle);
    }

    protected static BlockMovementChecks.CheckResult isDrillHeadAttachedTowards(
            BlockState state, Level level, BlockPos pos, Direction direction
    ) {
        BlockPos controllerPos = null;
        BlockState controllerState = null;

        if (state.is(RNSBlocks.DRILL_HEAD.get())) {
            controllerPos = pos;
            controllerState = state;
        } else if (state.is(RNSBlocks.DRILL_HEAD_PART.get())) {
            controllerPos = DrillHeadMultiblock.findOwnerController(level, pos);
            if (controllerPos == null) return BlockMovementChecks.CheckResult.PASS;
            controllerState = level.getBlockState(controllerPos);
            if (!controllerState.is(RNSBlocks.DRILL_HEAD.get())) return BlockMovementChecks.CheckResult.PASS;
        } else {
            return BlockMovementChecks.CheckResult.PASS;
        }

        var neighborPos = pos.relative(direction);
        var occupied = DrillHeadMultiblock.getOccupiedPositions(controllerPos, controllerState);
        if (!occupied.contains(neighborPos)) return BlockMovementChecks.CheckResult.PASS;

        var neighborState = level.getBlockState(neighborPos);
        if (neighborState.is(RNSBlocks.DRILL_HEAD.get())) {
            return BlockMovementChecks.CheckResult.of(neighborPos.equals(controllerPos));
        }

        if (!neighborState.is(RNSBlocks.DRILL_HEAD_PART.get())) return BlockMovementChecks.CheckResult.PASS;
        var neighborOwner = DrillHeadMultiblock.findOwnerController(level, neighborPos);
        return BlockMovementChecks.CheckResult.of(controllerPos.equals(neighborOwner));
    }

    protected static BlockMovementChecks.CheckResult isDrillHeadBrittle(BlockState state) {
        if (state.is(RNSBlocks.DRILL_HEAD.get()) || state.is(RNSBlocks.DRILL_HEAD_PART.get())) {
            return BlockMovementChecks.CheckResult.FAIL;
        }

        return BlockMovementChecks.CheckResult.PASS;
    }
}
