package com.bmaster.createrns.content.deposit.operating.sublevel;

import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator.DetectionDimensions;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalDouble;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface OperatingSublevelAdapter {
    OperatingSublevel getOperatingSublevel(Level level, BlockPos pos);

    SidedOperatingSublevel getSidedOperatingSublevel(Level level, BlockPos pos);

    Direction getLogicalDirection(Level level, BlockPos pos, BlockPos relativeTo, Direction direction);

    default boolean isSameSublevel(Level firstLevel, BlockPos firstPos, Level secondLevel, BlockPos secondPos) {
        return getOperatingSublevel(firstLevel, firstPos).equals(getOperatingSublevel(secondLevel, secondPos));
    }

    double distManhattan(Level level, BlockPos firstPos, BlockPos secondPos);

    double distSqr(Level level, BlockPos firstPos, BlockPos secondPos);

    default OptionalDouble getCrossSublevelBlockHitDistance(
            Level level, BlockPos contact, Direction operatingDirection, BlockPos target, double rayLength
    ) {
        return OptionalDouble.empty();
    }

    CrossSublevelDepositBlocks getCrossSublevelDepositBlocks(
            Level level, OperatingSublevel operatorSublevel, BlockPos contact,
            Direction operatingDirection, DetectionDimensions operatingDimensions
    );

    record CrossSublevelDepositBlocks(@Nullable BlockPos closest, Set<BlockPos> blocks) {
        public static final CrossSublevelDepositBlocks EMPTY =
                new CrossSublevelDepositBlocks(null, Set.of());

        public boolean isEmpty() {
            return blocks.isEmpty();
        }

        public boolean contains(BlockPos pos) {
            return blocks.contains(pos);
        }
    }

    record OperatingSublevel(ResourceKey<Level> dimension, String identity) {
        public static final String MAIN_ID = "main";

        public static OperatingSublevel of(Level level, BlockPos pos) {
            return OperatingSublevelAdapterHolder.getAdapter().getOperatingSublevel(level, pos);
        }
    }

    record SidedOperatingSublevel(ResourceKey<Level> dimension, String identity, boolean isClientSide) {
        public static SidedOperatingSublevel of(Level level, BlockPos pos) {
            return OperatingSublevelAdapterHolder.getAdapter().getSidedOperatingSublevel(level, pos);
        }
        public static SidedOperatingSublevel of(Level level, OperatingSublevel sublevel) {
            assert level.dimension().equals(sublevel.dimension());
            return new SidedOperatingSublevel(sublevel.dimension(), sublevel.identity(), level.isClientSide);
        }
    }
}
