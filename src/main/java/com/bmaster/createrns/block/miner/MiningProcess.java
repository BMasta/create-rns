package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.capability.orechunkdata.IOreChunkData;
import com.bmaster.createrns.capability.orechunkdata.OreChunkPurity;
import net.minecraft.world.item.ItemStack;

public class MiningProcess {
    public final ItemStack minedItemStack;

    private final boolean isPossible;
    private int ticksToMine;
    private boolean isActive;
    private int ticksMined = 0;

    public MiningProcess(IOreChunkData data) {
        if (data.isOreChunk()) {
            isPossible = true;
            // TODO: may be false once energy requirements are added
            isActive = true;
            minedItemStack = data.getMinedItemStack();
            ticksToMine = data.getPurity().getTicksToMine();
        } else {
            isPossible = false;
            isActive = false;
            minedItemStack = ItemStack.EMPTY;
            ticksToMine = OreChunkPurity.NONE.getTicksToMine();
        }
    }

    public void advance() {
        if (isPossible && isActive && (ticksMined < ticksToMine)) {
            ticksMined++;
        }
    }

    public ItemStack collect() {
        if (isDone()) {
            ticksMined = 0;
            return minedItemStack.copy();
        }
        return ItemStack.EMPTY;
    }

    public int getProgress() {
        return ticksMined;
    }

    public void setProgress(int ticks) {
        ticksMined = ticks;
    }

    public int getMaxProgress() {
        return ticksToMine;
    }

    public void setMaxProgress(int ticks) {
        ticksToMine = ticks;
    }

    public boolean isPossible() {
        return isPossible;
    }

    public boolean isActive() {
        return isPossible && isActive;
    }

    public boolean isDone() {
        return (isPossible && isActive && (ticksMined >= ticksToMine));
    }

    public void activate() {
        if (isPossible) {
            isActive = true;
        }
    }

    public void deactivate() {
        isActive = false;
    }
}
