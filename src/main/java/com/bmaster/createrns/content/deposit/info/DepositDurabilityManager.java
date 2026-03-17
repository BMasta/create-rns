package com.bmaster.createrns.content.deposit.info;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.DepositBlock;
import com.bmaster.createrns.content.deposit.mining.recipe.DepositDurability;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.Utils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DepositDurabilityManager {
    public static int initDepositVeinDurability(ServerLevel sl, BlockPos start) {
        var dd = IDepositIndex.get(sl);
        if (ServerConfig.infiniteDeposits) return 0;
        if (dd.depositDurabilities.containsKey(start)) return 0;
        var startRecipe = MiningRecipeLookup.find(sl, sl.getBlockState(start).getBlock());
        if (startRecipe == null) return 0;
        var startDur = startRecipe.getDurability();
        // Infinite starts never initialize vein durabilities (but can be initialized from finite starts)
        if (startDur.edge() <= 0 || startDur.core() <= 0) return 0;

        var blockToDepth = DepositBlock.getVein(sl, start);
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
        var dd = IDepositIndex.get(sl);
        if (ServerConfig.infiniteDeposits) return 0;
        if (initIfNeeded) initDepositVeinDurability(sl, dbPos);
        if (!dd.depositDurabilities.containsKey(dbPos)) return -1;
        return dd.depositDurabilities.getLong(dbPos);
    }

    /// Returns 0 if infinite, actual durability otherwise.
    public static long getDepositBlockDurability(ServerLevel sl, BlockPos dbPos) {
        return getDepositBlockDurability(sl, dbPos, true);
    }

    public static boolean setDepositBlockDurability(ServerLevel sl, BlockPos dbPos, long durability) {
        var dd = IDepositIndex.get(sl);
        if (ServerConfig.infiniteDeposits) return false;
        dd.depositDurabilities.put(dbPos, durability);
        return true;
    }

    public static void removeDepositBlockDurability(ServerLevel sl, BlockPos dbPos) {
        var dd = IDepositIndex.get(sl);
        dd.depositDurabilities.removeLong(dbPos);
    }

    public static void useDepositBlock(ServerLevel sl, BlockPos dbPos, BlockState replacementBlock) {
        if (ServerConfig.infiniteDeposits) return;
        var dd = IDepositIndex.get(sl);
        initDepositVeinDurability(sl, dbPos); // No-op if already initialized
        var dur = dd.depositDurabilities.getLong(dbPos);
        if (dur == 1) {
            removeDepositBlockDurability(sl, dbPos);
            sl.setBlockAndUpdate(dbPos, replacementBlock);
            CreateRNS.LOGGER.trace("Depleted deposit at {},{},{}", dbPos.getX(), dbPos.getY(), dbPos.getZ());
        } else if (dur > 1) {
            dd.depositDurabilities.addTo(dbPos, -1);
            CreateRNS.LOGGER.trace("Used deposit at {},{},{}: {} -> {}", dbPos.getX(), dbPos.getY(), dbPos.getZ(), dur, dur - 1);
        }
        // <= 0 means deposit is infinite
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
