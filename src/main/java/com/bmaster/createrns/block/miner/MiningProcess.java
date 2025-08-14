package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.capability.orechunkdata.IOreChunkData;
import net.minecraft.world.item.ItemStack;

public class MiningProcess {
    private static final float BASE_MAX_PROGRESS = 20 * 256 * 30; // 30 seconds at 256 RPM without multipliers

    public final ItemStack minedItemStack;

    private final boolean isPossible;
    private int progress = 0;
    private int maxProgress;

    public static MiningProcess from(IOreChunkData data) {
        return new MiningProcess(data.getMinedItemStack().copy(), data.getPurity().getMultiplier(), data.isOreChunk());
    }

    private MiningProcess(ItemStack minedItemStack, float progressMultiplier, boolean isPossible) {
        this.isPossible = isPossible;
        if (isPossible) {
            this.minedItemStack = minedItemStack;
            this.maxProgress = Math.round(BASE_MAX_PROGRESS * progressMultiplier);
        } else {
            this.minedItemStack = ItemStack.EMPTY;
            this.maxProgress = 0;
        }
    }

    public void advance(int by) {
        if (!isDone()) {
            progress += by;
        }
        if (progress > maxProgress) progress = maxProgress;
    }

    public ItemStack collect() {
        if (isDone()) {
            progress = 0;
            return minedItemStack.copy();
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
        return isPossible;
    }

    public boolean isDone() {
        return (progress >= maxProgress);
    }

}
