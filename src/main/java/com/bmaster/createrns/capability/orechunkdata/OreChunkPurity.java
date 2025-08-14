package com.bmaster.createrns.capability.orechunkdata;

public enum OreChunkPurity {
    NONE(1),
    IMPURE(20 * 60),
    NORMAL(20 * 30),
    PURE(20 * 15);

    private final int ticksToMine;

    OreChunkPurity(int ticksToMine) {
        this.ticksToMine = ticksToMine;
    }

    public int getTicksToMine() {
        return this.ticksToMine;
    }
}
