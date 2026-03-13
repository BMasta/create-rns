package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

import com.bmaster.createrns.RNSBlocks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MineHeadMultiblock {
    public static Set<BlockPos> getOccupiedPositions(BlockPos controllerPos, BlockState controllerState) {
        var direction = MineHeadBlock.getConnectedDirection(controllerState);
        var size = controllerState.getValue(MineHeadBlock.SIZE);
        return getOccupiedPositions(controllerPos, direction, size);
    }

    public static Set<BlockPos> getOccupiedPositions(BlockPos controllerPos, Direction direction, MineHeadSize size) {
        Set<BlockPos> positions = new HashSet<>();
        Direction u = getUDirection(direction);
        Direction v = getVDirection(direction);
        switch (size) {
            case SMALL -> positions.add(controllerPos);
            case LARGE -> {
                addSquareLayer(positions, controllerPos, u, v);
                positions.add(controllerPos.relative(direction));
            }
        }
        return positions;
    }

    public static boolean canOccupyPositions(
            LevelReader level, Set<BlockPos> currentPositions, Set<BlockPos> targetPositions
    ) {
        for (var p : targetPositions) {
            if (currentPositions.contains(p)) continue;
            if (!canReplaceForUpgrade(level.getBlockState(p))) return false;
        }
        return true;
    }

    public static @Nullable BlockPos findControllerPosForUpgrade(
            LevelReader level, BlockPos controllerPos, BlockState controllerState, MineHeadSize targetSize
    ) {
        var direction = MineHeadBlock.getConnectedDirection(controllerState);
        var currentPositions = getOccupiedPositions(controllerPos, controllerState);

        var preferredTargets = getOccupiedPositions(controllerPos, direction, targetSize);
        if (canOccupyPositions(level, currentPositions, preferredTargets)) return controllerPos;

        var fallbackControllerPos = controllerPos.relative(direction.getOpposite());
        var fallbackTargets = getOccupiedPositions(fallbackControllerPos, direction, targetSize);
        if (canOccupyPositions(level, currentPositions, fallbackTargets)) return fallbackControllerPos;

        return null;
    }

    public static boolean tryUpgrade(Level level, BlockPos controllerPos, BlockState controllerState) {
        var currentSize = controllerState.getValue(MineHeadBlock.SIZE);
        if (!currentSize.canGrow()) return false;
        return tryUpgrade(level, controllerPos, controllerState, currentSize.getNext());
    }

    protected static boolean tryUpgrade(
            Level level, BlockPos controllerPos, BlockState controllerState, MineHeadSize targetSize
    ) {
        var upgradedControllerPos = findControllerPosForUpgrade(level, controllerPos, controllerState, targetSize);
        if (upgradedControllerPos == null) return false;

        var upgradedState = controllerState.setValue(MineHeadBlock.SIZE, targetSize);
        if (upgradedControllerPos.equals(controllerPos)) {
            level.setBlock(controllerPos, upgradedState, Block.UPDATE_ALL);
            placePartBlocksForSize(level, controllerPos, upgradedState);
            return true;
        }

        level.setBlock(controllerPos, RNSBlocks.MINE_HEAD_PART.getDefaultState(), Block.UPDATE_ALL);
        level.setBlock(upgradedControllerPos, upgradedState, Block.UPDATE_ALL);
        placePartBlocksForSize(level, upgradedControllerPos, upgradedState);
        return true;
    }

    protected static boolean canReplaceForUpgrade(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    public static @Nullable BlockPos findOwnerController(LevelReader level, BlockPos occupiedPos) {
        BlockPos foundPos = null;
        int bestDist = Integer.MAX_VALUE;

        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    var candidatePos = occupiedPos.offset(x, y, z);
                    var candidateState = level.getBlockState(candidatePos);
                    if (!candidateState.is(RNSBlocks.MINE_HEAD.get())) continue;

                    var occupiedByCandidate = getOccupiedPositions(candidatePos, candidateState);
                    if (!occupiedByCandidate.contains(occupiedPos)) continue;

                    int dist = Math.abs(x) + Math.abs(y) + Math.abs(z);
                    if (dist < bestDist) {
                        foundPos = candidatePos;
                        bestDist = dist;
                    } else if (dist == bestDist && !candidatePos.equals(foundPos)) {
                        return null;
                    }
                }
            }
        }

        return foundPos;
    }

    public static void placePartBlocksForSize(Level level, BlockPos controllerPos, BlockState controllerState) {
        var occupied = getOccupiedPositions(controllerPos, controllerState);
        for (var p : occupied) {
            if (p.equals(controllerPos)) continue;
            var desiredState = getPartStateForPosition(controllerPos, controllerState, p);
            if (level.getBlockState(p).equals(desiredState)) continue;
            level.setBlock(p, desiredState, Block.UPDATE_ALL);
        }
    }

    public static void removePartBlocks(Level level, BlockPos controllerPos, BlockState controllerState) {
        var occupied = getOccupiedPositions(controllerPos, controllerState);
        for (var p : occupied) {
            if (p.equals(controllerPos)) continue;
            if (!level.getBlockState(p).is(RNSBlocks.MINE_HEAD_PART.get())) continue;
            level.destroyBlock(p, false);
        }
    }

    protected static Direction getUDirection(Direction direction) {
        return switch (direction.getAxis()) {
            case X -> Direction.UP;
            case Y, Z -> Direction.EAST;
        };
    }

    protected static Direction getVDirection(Direction direction) {
        return switch (direction.getAxis()) {
            case X, Y -> Direction.SOUTH;
            case Z -> Direction.UP;
        };
    }

    protected static BlockPos offsetInPlane(BlockPos pos, Direction u, int du, Direction v, int dv) {
        return pos.offset(
                u.getStepX() * du + v.getStepX() * dv,
                u.getStepY() * du + v.getStepY() * dv,
                u.getStepZ() * du + v.getStepZ() * dv
        );
    }

    protected static void addSquareLayer(Set<BlockPos> positions, BlockPos center, Direction u, Direction v) {
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                positions.add(offsetInPlane(center, u, du, v, dv));
            }
        }
    }

    protected static BlockState getPartStateForPosition(BlockPos controllerPos, BlockState controllerState, BlockPos partPos) {
        var direction = MineHeadBlock.getConnectedDirection(controllerState);
        var position = switch (controllerState.getValue(MineHeadBlock.SIZE)) {
            case SMALL, LARGE -> MineHeadPartPosition.CORE;
        };

        return RNSBlocks.MINE_HEAD_PART.getDefaultState()
                .setValue(MineHeadPartBlock.FACING, direction)
                .setValue(MineHeadPartBlock.POSITION, position);
    }

}
