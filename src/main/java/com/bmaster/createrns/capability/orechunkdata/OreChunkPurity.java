package com.bmaster.createrns.capability.orechunkdata;

public enum OreChunkPurity {
    NONE(1),
    IMPURE(20 * 60),
    NORMAL(20 * 30),
    PURE(20 * 15);

    private final int ticksPerExcavation;

    OreChunkPurity(int ticksPerExcavation) {
        this.ticksPerExcavation = ticksPerExcavation;
    }

    public int getTicksPerExcavation() {
        return this.ticksPerExcavation;
    }
}
