package com.bmaster.createrns.deposit;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.deposit.capability.IDepositIndex;
import com.bmaster.createrns.mining.MiningBlockEntityInstanceHolder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DepositBlock extends Block {
    public static final int MAX_DEPOSIT_VEIN_SIZE = 128;
    private static final Set<Direction> xzDirections = Set.of(
            Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH);

    public static Object2IntOpenHashMap<BlockPos> getVein(Level level, BlockPos start) {
        Queue<BlockPos> q = new ArrayDeque<>();
        Object2IntOpenHashMap<BlockPos> visited = new Object2IntOpenHashMap<>();
        if (!level.getBlockState(start).is(RNSTags.Block.DEPOSIT_BLOCKS)) return visited;
        q.add(start);

        // Collect all blocks in the deposit vein. Assign depth of outer blocks to 0, all other to MAX_VALUE.
        int depth = 0;
        while (!q.isEmpty() && depth < MAX_DEPOSIT_VEIN_SIZE) {
            var bp = q.poll();
            if (visited.containsKey(bp)) continue;

            AtomicBoolean external = new AtomicBoolean(false);
            Direction.stream().forEach(d -> {
                var nb = bp.relative(d);
                if (level.getBlockState(nb).is(RNSTags.Block.DEPOSIT_BLOCKS)) {
                    q.add(bp.relative(d));
                } else {
                    if (xzDirections.contains(d)) external.set(true);
                }
            });
            visited.put(bp, external.get() ? 0 : Integer.MAX_VALUE);
            ++depth;
        }

        // Start with outer blocks whose depth is 0. Compute depth of their neighbors until all blocks are processed.
        for (depth = 0; depth < MAX_DEPOSIT_VEIN_SIZE; ++depth) {
            int finalDepth = depth;
            var curDepthBlocks = visited.object2IntEntrySet().stream()
                    .filter(e -> e.getIntValue() == finalDepth)
                    .collect(Collectors.toSet());
            if (curDepthBlocks.isEmpty()) break;

            for (var e : curDepthBlocks) {
                xzDirections.forEach(d -> {
                    var neighbor = e.getKey().relative(d);
                    if (!visited.containsKey(neighbor)) return;
                    visited.computeInt(neighbor, (k, v) -> Math.min(v, finalDepth + 1));
                });
            }
        }

        if (visited.containsValue(Integer.MAX_VALUE)) {
            throw new IllegalStateException("Could not process deposit vein starting at %s,%s,%s"
                    .formatted(start.getX(), start.getY(), start.getZ()));
        }

        return visited;
    }

    public DepositBlock(Properties pProperties) {
        super(pProperties);
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) return;
        for (var m : MiningBlockEntityInstanceHolder.getInstancesMiningAt(level, pos)) {
            m.reserveDepositBlocks();
            var mPos = m.getBlockPos();
            var mState = level.getBlockState(mPos);
            // onRemove is not called for client levels, so a sync is necessary
            level.sendBlockUpdated(mPos, mState, mState, Block.UPDATE_CLIENTS);
        }
    }

    @SuppressWarnings("deprecation")
    @ParametersAreNonnullByDefault
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!(level instanceof ServerLevel sl)) return;
        // Only non-depleted deposits have durability
        if (state.is(RNSTags.Block.DEPOSIT_BLOCKS)) {
            var depIdx = IDepositIndex.fromLevel(sl);
            if (depIdx == null) return;
            depIdx.removeDepositBlockDurability(pos);
        }
        for (var m : MiningBlockEntityInstanceHolder.getInstancesMiningAt(level, pos)) {
            m.reserveDepositBlocks();
            var mPos = m.getBlockPos();
            var mState = level.getBlockState(mPos);
            // onRemove is not called for client levels, so a sync is necessary
            level.sendBlockUpdated(mPos, mState, mState, Block.UPDATE_CLIENTS);
        }
    }
}
