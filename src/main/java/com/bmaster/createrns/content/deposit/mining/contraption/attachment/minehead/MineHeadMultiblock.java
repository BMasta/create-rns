package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

import com.bmaster.createrns.CreateRNS;
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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MineHeadMultiblock {
    public static Set<BlockPos> getOccupiedPositions(BlockPos controllerPos, BlockState controllerState) {
        var direction = MineHeadBlock.getConnectedDirection(controllerState);
        var size = controllerState.getValue(MineHeadBlock.SIZE);
        return size.getOccupiedPositions(controllerPos, direction);
    }

    public static void formMultiblock(Level level, BlockPos tipPos, BlockState tipState) {
        Direction direction = MineHeadBlock.getConnectedDirection(tipState);
        MineHeadSize size = MineHeadSize.SMALL;

        // Find the largest available size and direction required for that size.
        // If bigger sizes aren't available, preserve the direction of the placed tip.
        for (var candidateDirection : Direction.values()) {
            var candidateSize = calculateSizeForPlacement(level, candidateDirection, tipPos);
            if (candidateSize.ordinal() > size.ordinal()) {
                direction = candidateDirection;
                size = candidateSize;
            }
            if (size.ordinal() >= MineHeadSize.values().length - 1) break;
        }
        if (size == MineHeadSize.SMALL) return;

        // Replace the pattern with the large mine head multiblock and enforce orientation from placement position.
        var controllerPos = size.getControllerPos(tipPos, direction);
        var controllerState = FaceAttachedMinerComponentBlock.withConnectedDirection(tipState, direction)
                .setValue(MineHeadBlock.SIZE, size);
        level.setBlock(controllerPos, controllerState, Block.UPDATE_ALL);
        placePartBlocksForSize(level, controllerPos, controllerState);
    }

    public static void breakMultiblock(Level level, BlockPos controllerPos, BlockState controllerState) {
        if (!controllerState.is(RNSBlocks.MINE_HEAD.get())) return;
        var size = controllerState.getValue(MineHeadBlock.SIZE);
        if (size == MineHeadSize.SMALL) return;

        var direction = MineHeadBlock.getConnectedDirection(controllerState);
        var smallMineHead = FaceAttachedMinerComponentBlock.withConnectedDirection(
                RNSBlocks.MINE_HEAD.getDefaultState(), direction);
        var tipPos = size.getTipPos(controllerPos, direction);

        // First restore the tip to avoid retriggering multiblock formation. Then restore the base.
        level.setBlock(tipPos, smallMineHead, Block.UPDATE_ALL);
        for (var pos : size.getOccupiedPositions(controllerPos, direction)) {
            if (pos.equals(tipPos)) continue;
            level.setBlock(pos, Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    public static @Nullable BlockPos findOwnerController(LevelReader level, BlockPos occupiedPos) {
        BlockPos foundPos = null;
        int bestDist = Integer.MAX_VALUE;
        int r = MineHeadSize.values()[MineHeadSize.values().length - 1].tipOffset;

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
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
        var part = RNSBlocks.MINE_HEAD_PART.getDefaultState();
        for (var p : occupied) {
            if (p.getY() == 67) {
                CreateRNS.LOGGER.error("TESTTEST: reached");
            }
            if (p.equals(controllerPos)) continue;
            if (level.getBlockState(p).equals(part)) continue;
            level.setBlock(p, part, Block.UPDATE_ALL);
        }
    }

    public static MineHeadSize calculateSizeForPlacement(Level level, Direction direction, BlockPos placedPos) {
        MineHeadSize computedSize = MineHeadSize.SMALL;
        var sizes = MineHeadSize.values();
        // Try from largest to smallest
        for (int i = sizes.length - 1; i >= 0; i--) {
            var controllerPos = sizes[i].getControllerPos(placedPos, direction);
            boolean accepted = true;
            for (var pos : sizes[i].getOccupiedPositions(controllerPos, direction)) {
                if (pos.equals(placedPos)) continue;
                if (level.getBlockState(pos).is(Blocks.IRON_BLOCK)) continue;
                accepted = false;
                break;
            }
            if (!accepted) continue;
            computedSize = sizes[i];
            break;
        }
        return computedSize;
    }
}
