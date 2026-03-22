package com.bmaster.createrns.content.deposit.info;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Objects;

public abstract class DepositLocation {
    protected ResourceKey<Structure> key;
    protected ChunkPos origin;

    public DepositLocation(ResourceKey<Structure> key, ChunkPos origin) {
        this.key = key;
        this.origin = origin;
    }

    public ResourceKey<Structure> getKey() {
        return key;
    }

    public ChunkPos getOrigin() {
        return origin;
    }

    public abstract BlockPos getLocation();

    public abstract String getLocationStr();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DepositLocation other)) return false;
        return Objects.equals(key, other.key) && Objects.equals(origin, other.origin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, origin);
    }
}
