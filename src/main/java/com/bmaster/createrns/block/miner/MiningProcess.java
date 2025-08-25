package com.bmaster.createrns.block.miner;

import net.minecraft.world.item.ItemStack;

public class MiningProcess {
    private static final float BASE_MAX_PROGRESS = 20 * 256 * 30; // 30 seconds at 256 RPM without multipliers


    private ItemStack yield;
    private int maxProgress;
    private int progress = 0;

    public MiningProcess(ItemStack yield, float progressMultiplier) {
        this.yield = yield;
        this.maxProgress = Math.round(BASE_MAX_PROGRESS * progressMultiplier);
    }

    public void advance(int by) {
        if (isPossible() && !isDone()) {
            progress += by;
        }
        if (progress > maxProgress) progress = maxProgress;
    }

    public ItemStack collect() {
        if (isDone()) {
            progress = 0;
            return yield.copy();
        }
        return ItemStack.EMPTY;
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

    public ItemStack getYield() {
        return yield;
    }

    public void setYield(ItemStack yield) {
        this.yield = yield;
    }

    public void setYieldCount(int count) {
        this.yield.setCount(count);
    }
}
