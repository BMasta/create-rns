package com.bmaster.createrns.content.deposit.operating.sublevel;

import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class VanillaOperatingSublevelAdapter implements OperatingSublevelAdapter {
    @Override
    public OperatingSublevel getOperatingSublevel(Level level, BlockPos pos) {
        return new OperatingSublevel(level.dimension(), OperatingSublevel.MAIN_ID);
    }

    @Override
    public SidedOperatingSublevel getSidedOperatingSublevel(Level level, BlockPos pos) {
        return new SidedOperatingSublevel(level.dimension(), OperatingSublevel.MAIN_ID, level.isClientSide);
    }

    @Override
    public Direction getLogicalDirection(Level level, BlockPos pos, BlockPos relativeTo, Direction direction) {
        return direction;
    }

    @Override
    public double distManhattan(Level level, BlockPos firstPos, BlockPos secondPos) {
        return firstPos.distManhattan(secondPos);
    }

    @Override
    public double distSqr(Level level, BlockPos firstPos, BlockPos secondPos) {
        return firstPos.distSqr(secondPos);
    }

    @Override
    public CrossSublevelDepositBlocks getCrossSublevelDepositBlocks(
            Level level, OperatingSublevel operatorSublevel, BlockPos contact,
            Direction operatingDirection, IDepositBlockOperator.DetectionDimensions operatingDimensions
    ) {
        // No sublevels in vanilla
        return CrossSublevelDepositBlocks.EMPTY;
    }
}
