package com.bmaster.createrns.content.deposit.operating.space;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class VanillaOperatingSpaceAdapter implements OperatingSpaceAdapter {
    @Override
    public OperatingSpace getOperatingSpace(Level level, BlockPos pos) {
        return new OperatingSpace(level.dimension(), OperatingSpace.MAIN_SPACE);
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
    public List<DepositGroupCandidate> findRemoteDepositGroup(OperatingSpaceScanContext context) {
        return List.of();
    }
}
