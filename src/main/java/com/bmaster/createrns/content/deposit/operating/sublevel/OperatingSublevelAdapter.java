package com.bmaster.createrns.content.deposit.operating.sublevel;

import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator.CrossSublevelOperatingDimensions;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface OperatingSublevelAdapter {
    OperatingSublevel getOperatingSublevel(Level level, BlockPos pos);

    default boolean isSameSublevel(Level firstLevel, BlockPos firstPos, Level secondLevel, BlockPos secondPos) {
        return getOperatingSublevel(firstLevel, firstPos).equals(getOperatingSublevel(secondLevel, secondPos));
    }

    double distManhattan(Level level, BlockPos firstPos, BlockPos secondPos);

    double distSqr(Level level, BlockPos firstPos, BlockPos secondPos);

    Set<BlockPos> getCrossSublevelDepositBlocks(
            Level level, OperatingSublevel operatorSublevel, BlockPos anchor,
            Direction operatingDirection, CrossSublevelOperatingDimensions operatingDimensions
    );

    record OperatingSublevel(ResourceKey<Level> dimension, String identity) {
        public static final String MAIN_ID = "main";
    }
}
