package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.content.deposit.mining.recipe.DepositDurability;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.Utils;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositDurabilityManager {
    public static final int MAX_DEPOSIT_VEIN_DEPTH = 128;
    private static final Set<Direction> xzDirections = Set.of(
            Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.NORTH);

    public static Object2IntOpenHashMap<BlockPos> getVein(Level level, BlockPos start) {
        Queue<BlockPos> q = new ArrayDeque<>();
        Object2IntOpenHashMap<BlockPos> visited = new Object2IntOpenHashMap<>();
        if (!level.getBlockState(start).is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)) return visited;
        q.add(start);

        // Collect all blocks in the deposit vein. Assign depth of outer blocks to 0, all other to MAX_VALUE.
        int depth = 0;
        while (!q.isEmpty() && depth < MAX_DEPOSIT_VEIN_DEPTH) {
            var bp = q.poll();
            if (visited.containsKey(bp)) continue;

            AtomicBoolean external = new AtomicBoolean(false);
            Direction.stream().forEach(d -> {
                var nb = bp.relative(d);
                if (level.getBlockState(nb).is(RNSTags.RNSBlockTags.DEPOSIT_BLOCKS)) {
                    q.add(bp.relative(d));
                } else {
                    if (xzDirections.contains(d)) external.set(true);
                }
            });
            visited.put(bp, external.get() ? 0 : Integer.MAX_VALUE);
            ++depth;
        }

        // Start with outer blocks whose depth is 0. Compute depth of their neighbors until all blocks are processed.
        for (depth = 0; depth < MAX_DEPOSIT_VEIN_DEPTH; ++depth) {
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

    public static int initDepositVeinDurability(ServerLevel sl, BlockPos start, boolean fullScan) {
        if (ServerConfig.INFINITE_DEPOSITS.get()) return 0;
        var dd = IDepositIndex.get(sl);
        if (dd.depositDurabilities.containsKey(start)) return 0;

        if (!fullScan) {
            // Return early if start is initialized
            var startRecipe = MiningRecipeLookup.find(sl, sl.getBlockState(start).getBlock());
            if (startRecipe == null) return 0;
            var startDur = startRecipe.getDurability();
            if (startDur.edge() <= 0 || startDur.core() <= 0) {
                dd.depositDurabilities.put(start, 0);
                return 1;
            }
        }

        var blockToDepth = getVein(sl, start);
        if (blockToDepth.isEmpty()) return 0;
        var maxDepth = blockToDepth.values().intStream().max().orElseThrow();

        int initCount = 0;
        for (var e : blockToDepth.object2IntEntrySet()) {
            var bp = e.getKey();
            if (dd.depositDurabilities.containsKey(bp)) continue;
            var b = sl.getBlockState(bp).getBlock();
            var r = MiningRecipeLookup.find(sl, b);
            if (r == null) continue;
            float depthRatio = (maxDepth != 0) ? ((float) e.getIntValue() / maxDepth) : 0.5f;
            dd.depositDurabilities.put(bp, rollDurability(sl, r.getDurability(), depthRatio));
            initCount++;
        }

        return initCount;
    }

    /// Returns -1 if not initialized, 0 if infinite, actual durability otherwise.
    public static long getDepositBlockDurability(ServerLevel sl, BlockPos dbPos, boolean initIfNeeded) {
        if (ServerConfig.INFINITE_DEPOSITS.get()) return 0;
        var dd = IDepositIndex.get(sl);
        if (initIfNeeded) initDepositVeinDurability(sl, dbPos, false);
        if (!dd.depositDurabilities.containsKey(dbPos)) return -1;
        return dd.depositDurabilities.getLong(dbPos);
    }

    /// Returns 0 if infinite, actual durability otherwise.
    public static long getDepositBlockDurability(ServerLevel sl, BlockPos dbPos) {
        return getDepositBlockDurability(sl, dbPos, true);
    }

    public static boolean setDepositBlockDurability(ServerLevel sl, BlockPos dbPos, long durability) {
        if (ServerConfig.INFINITE_DEPOSITS.get()) return false;
        var dd = IDepositIndex.get(sl);
        dd.depositDurabilities.put(dbPos, durability);
        return true;
    }

    public static void removeDepositBlockDurability(ServerLevel sl, BlockPos dbPos) {
        var dd = IDepositIndex.get(sl);
        dd.depositDurabilities.removeLong(dbPos);
    }

    public static boolean useDepositBlock(ServerLevel sl, BlockPos dbPos, BlockState replacementBlock) {
        if (ServerConfig.INFINITE_DEPOSITS.get()) return true;
        var dd = IDepositIndex.get(sl);
        initDepositVeinDurability(sl, dbPos, false); // No-op if already initialized
        if (!dd.depositDurabilities.containsKey(dbPos)) {
            CreateRNS.LOGGER.error("Failed to initialize deposit durability at {},{},{}", dbPos.getX(), dbPos.getY(), dbPos.getZ());
            return false;
        }
        var dur = dd.depositDurabilities.getLong(dbPos);
        if (dur < 0) {
            CreateRNS.LOGGER.error("Tried using an invalid block as deposit at {},{},{}", dbPos.getX(), dbPos.getY(), dbPos.getZ());
            return false;
        }

        if (dur == 1) {
            removeDepositBlockDurability(sl, dbPos);
            sl.setBlockAndUpdate(dbPos, replacementBlock);
            CreateRNS.LOGGER.trace("Depleted deposit at {},{},{}", dbPos.getX(), dbPos.getY(), dbPos.getZ());
        } else if (dur > 1) {
            dd.depositDurabilities.addTo(dbPos, -1);
            CreateRNS.LOGGER.trace("Used deposit at {},{},{}: {} -> {}", dbPos.getX(), dbPos.getY(), dbPos.getZ(), dur, dur - 1);
        }

        return true;
    }

    /// Durabilities for all deposits fall within that range based on their depth.
    /// For each depth, there is a yet another range which confines the possible random durability values.
    ///
    /// E.g. assume depth ratio is 0.3, then:
    /// \[--(--)--------] where
    /// Square brackets are minimum and maximum durabilities across all deposits in the vein.
    /// Parentheses are minimum and maximum durabilities for the given depth.
    ///
    /// If vein is infinite, 0 is returned. Otherwise, the return value is random, but guaranteed to lie within both ranges.
    protected static long rollDurability(ServerLevel sl, DepositDurability dur, float depthRatio) {
        assert 0f <= depthRatio && depthRatio <= 1f;

        long minDur = dur.edge();
        long maxDur = dur.core();
        long range = maxDur - minDur;
        if (minDur <= 0 || maxDur <= 0) {
            CreateRNS.LOGGER.trace("Skipped roll for infinite deposit");
            return 0;
        }

        // Average durability at given depth and its maximum spread (deviation)
        long curDur = (long) ((maxDur - minDur) * depthRatio + minDur);
        long spread = (long) (dur.randomSpread() * curDur);

        // Range of depth durabilities (aka the parentheses) are clamped to the absolute range (aka the square brackets)
        long minDepthDur = Utils.longClamp(curDur - spread, minDur, maxDur - spread);
        long maxDepthDur = Utils.longClamp(curDur + spread, minDur + spread, maxDur);
        long depthRange = maxDepthDur - minDepthDur;

        long roll = (depthRange != 0) ? ((Math.abs(sl.random.nextLong()) % depthRange) + minDepthDur) : minDepthDur;

        long numBarsBefore = (range != 0) ? (Math.round(30 * ((double) (roll - minDur) / range))) : 15;
        CreateRNS.LOGGER.trace("Rolled deposit durability: [{}]{}x{}[{}] {}",
                minDur, "-".repeat((int) numBarsBefore), "-".repeat(30 - (int) numBarsBefore), maxDur, roll);

        return roll;
    }
}
