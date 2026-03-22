package com.bmaster.createrns.content.deposit.info;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;

public class ClientDepositLocation extends DepositLocation {
    protected BlockPos location;

    public ClientDepositLocation(ResourceKey<Structure> key, ChunkPos origin, BlockPos location) {
        super(key, origin);
        this.location = location;
    }

    @Override
    public BlockPos getLocation() {
        return location;
    }

    @Override
    public String getLocationStr() {
        return location.getX() + "," + location.getY() + "," + location.getZ();
    }
}
