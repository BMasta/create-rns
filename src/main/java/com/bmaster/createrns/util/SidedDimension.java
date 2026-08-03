package com.bmaster.createrns.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

public record SidedDimension(ResourceKey<Level> dimension, boolean isClientSide) {
    public static SidedDimension of(Level level) {
        return new SidedDimension(level.dimension(), level.isClientSide);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, isClientSide);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SidedDimension other)) return false;
        return (isClientSide == other.isClientSide) && (dimension.equals(other.dimension));
    }
}
