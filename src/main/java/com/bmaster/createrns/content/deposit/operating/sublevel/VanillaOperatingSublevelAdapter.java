package com.bmaster.createrns.content.deposit.operating.sublevel;

import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class VanillaOperatingSublevelAdapter implements OperatingSublevelAdapter {
    @Override
    public OperatingSublevel getOperatingSublevel(Level level, BlockPos pos) {
        return new OperatingSublevel(level.dimension(), OperatingSublevel.MAIN_ID);
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
    public Set<BlockPos> getCrossSublevelDepositBlocks(
            Level level, OperatingSublevel operatorSublevel, BlockPos anchor,
            Direction operatingDirection, IDepositBlockOperator.CrossSublevelOperatingDimensions operatingDimensions
    ) {
        // No sublevels in vanilla
        return Set.of();
    }
}
