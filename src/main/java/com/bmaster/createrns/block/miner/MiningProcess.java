package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.capability.depositindex.DepositSpecLookup;
import com.bmaster.createrns.util.ItemsToStackSetCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Set;

public class MiningProcess {
    private static final float BASE_MAX_PROGRESS = 20 * 256 * 30; // 30 seconds at 256 RPM without multipliers


    private Set<ItemStack> yield;
    private int maxProgress;
    private int progress = 0;

    public MiningProcess(ServerLevel sl, Set<BlockPos> depositBlocks, float progressMultiplier) {
        setYield(sl, depositBlocks);
        this.maxProgress = Math.round(BASE_MAX_PROGRESS * progressMultiplier);
    }

    public void advance(int by) {
        if (isPossible() && !isDone()) {
            progress += by;
        }
        if (progress > maxProgress) progress = maxProgress;
    }

    public Set<ItemStack> collect() {
        if (isDone()) {
            progress = 0;
            return yield;
        }
        return Set.of(ItemStack.EMPTY);
    }

    public int getProgress() {
        return progress;
    }

    /// Server thread should never call this method.
    public void setProgress(int val) {
        progress = val;
    }

    public int getMaxProgress() {

        return maxProgress;
    }

    /// Server thread should never call this method.
    public void setMaxProgress(int val) {
        maxProgress = val;
    }

    public boolean isPossible() {
        return !yield.isEmpty();
    }

    public boolean isDone() {
        return isPossible() && progress >= maxProgress;
    }

    public Set<ItemStack> getYield() {
        return yield;
    }

    public void setYield(Level l, Set<BlockPos> depositBlocks) {
        this.yield = depositBlocks.stream()
                .map(bp -> l.getBlockState(bp).getBlock())
                .filter(db -> db.defaultBlockState().is(RNSTags.Block.DEPOSIT_BLOCKS))
                .map(db -> DepositSpecLookup.getSpec(l, db).yield())
                .collect(new ItemsToStackSetCollector());
    }
}
