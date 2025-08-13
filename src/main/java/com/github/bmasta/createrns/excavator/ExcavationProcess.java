package com.github.bmasta.createrns.excavator;

import com.github.bmasta.createrns.capability.orechunkdata.IOreChunkData;
import com.github.bmasta.createrns.capability.orechunkdata.OreChunkPurity;
import net.minecraft.world.item.ItemStack;

public class ExcavationProcess {
    public final ItemStack excavatedItemStack;

    private final boolean isPossible;
    private int ticksPerExcavation;
    private boolean isActive;
    private int ticksExcavated = 0;

    public ExcavationProcess(IOreChunkData data) {
        if (data.isOreChunk()) {
            isPossible = true;
            // TODO: may be false once energy requirements are added
            isActive = true;
            excavatedItemStack = data.getExcavatedItemStack();
            ticksPerExcavation = data.getPurity().getTicksPerExcavation();
        } else {
            isPossible = false;
            isActive = false;
            excavatedItemStack = ItemStack.EMPTY;
            ticksPerExcavation = OreChunkPurity.NONE.getTicksPerExcavation();
        }
    }

    public void advance() {
        if (isPossible && isActive && (ticksExcavated < ticksPerExcavation)) {
            ticksExcavated++;
        }
    }

    public ItemStack collect() {
        if (isDone()) {
            ticksExcavated = 0;
            return excavatedItemStack.copy();
        }
        return ItemStack.EMPTY;
    }

    public int getProgress() {
        return ticksExcavated;
    }

    public void setProgress(int ticks) {
        ticksExcavated = ticks;
    }

    public int getMaxProgress() {
        return ticksPerExcavation;
    }

    public void setMaxProgress(int ticks) {
        ticksPerExcavation = ticks;
    }

    public boolean isPossible() {
        return isPossible;
    }

    public boolean isActive() {
        return isPossible && isActive;
    }

    public boolean isDone() {
        return (isPossible && isActive && (ticksExcavated >= ticksPerExcavation));
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
