package com.bmaster.createrns.capability.orechunkdata;

public enum OreChunkPurity {
    NONE(0),
    IMPURE(2F),
    NORMAL(1F),
    PURE(0.5F);

    private final float multiplier;

    OreChunkPurity(float multiplier) {
        this.multiplier = multiplier;
    }

    public float getMultiplier() {
        return this.multiplier;
    }
}
