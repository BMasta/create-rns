package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.FaceAttachedMinerComponentBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.NotImplementedException;
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

    public static void formMultiblock(Level level, BlockPos tipPos, BlockState tipState) {
        BlockPos controllerPos = null;
        Direction direction = null;

        // Resolve a unique formation direction from the placed tip position and nearby iron patterns.
        for (var candidateDirection : Direction.values()) {
            var candidateControllerPos = tipPos.relative(candidateDirection.getOpposite());
            var u = getUDirection(candidateDirection);
            var v = getVDirection(candidateDirection);

            boolean hasIronPattern = true;
            for (int du = -1; du <= 1; du++) {
                for (int dv = -1; dv <= 1; dv++) {
                    var ironPos = offsetInPlane(candidateControllerPos, u, du, v, dv);
                    if (level.getBlockState(ironPos).is(Blocks.IRON_BLOCK)) continue;
                    hasIronPattern = false;
                    break;
                }
                if (!hasIronPattern) break;
            }
            if (!hasIronPattern) continue;

            if (controllerPos != null) return;
            controllerPos = candidateControllerPos;
            direction = candidateDirection;
        }

        // Abort when no valid pattern exists around the placed mine head.
        if (controllerPos == null) return;

        // Replace the pattern with the large mine head multiblock and enforce orientation from placement position.
        var controllerState = FaceAttachedMinerComponentBlock.withConnectedDirection(tipState, direction)
                .setValue(MineHeadBlock.SIZE, MineHeadSize.LARGE);
        level.setBlock(controllerPos, controllerState, Block.UPDATE_ALL);
        placePartBlocksForSize(level, controllerPos, controllerState);
    }

    public static void breakMultiblock(
            Level level, BlockPos controllerPos, BlockState controllerState, BlockPos brokenPos
    ) {
        if (!controllerState.is(RNSBlocks.MINE_HEAD.get())) return;
        var mineHeadSize = controllerState.getValue(MineHeadBlock.SIZE);
        if (mineHeadSize == MineHeadSize.SMALL) return;

        var direction = MineHeadBlock.getConnectedDirection(controllerState);
        var u = getUDirection(direction);
        var v = getVDirection(direction);
        var smallMineHead = FaceAttachedMinerComponentBlock.withConnectedDirection(
                RNSBlocks.MINE_HEAD.getDefaultState(), direction);
        var tipPos = controllerPos.relative(direction);
        Set<BlockPos> basePositions = new HashSet<>();

        // Gather base positions
        switch (mineHeadSize) {
            case LARGE -> {
                addSquareLayer(basePositions, controllerPos, u, v);
                if (!brokenPos.equals(tipPos) && !basePositions.contains(brokenPos)) return;

            }
            default -> throw new NotImplementedException(mineHeadSize.name() + " Mine Head size is not implemented");
        }

        // First restore the tip to avoid retriggering multiblock formation. Then restore the base.
        level.setBlock(tipPos, smallMineHead, Block.UPDATE_ALL);
        for (var basePos : basePositions) {
            level.setBlock(basePos, Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
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
