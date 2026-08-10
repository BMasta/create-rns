package com.bmaster.createrns.content.deposit.operating.space;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public interface OperatingSpaceAdapter {
    OperatingSpace getOperatingSpace(Level level, BlockPos pos);

    default boolean isSameSpace(Level firstLevel, BlockPos firstPos, Level secondLevel, BlockPos secondPos) {
        return getOperatingSpace(firstLevel, firstPos).equals(getOperatingSpace(secondLevel, secondPos));
    }

    List<DepositGroupCandidate> findRemoteDepositGroup(OperatingSpaceScanContext context);

    record OperatingSpace(ResourceKey<Level> dimension, String identity) {
        public static final String MAIN_SPACE = "main";
    }

    record DepositGroupCandidate(OperatingSpace space, Set<BlockPos> positions) {
        public DepositGroupCandidate {
            positions = Set.copyOf(positions);
        }
    }

    record OperatingSpaceScanContext(
            Level sourceLevel,
            OperatingSpace sourceSpace,
            BlockPos scanStartPos,
            Direction operatingDirection,
            BoundingBox operatingArea
    ) {}

}
