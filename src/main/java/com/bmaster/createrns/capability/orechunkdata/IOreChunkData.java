package com.bmaster.createrns.capability.orechunkdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public interface IOreChunkData {
    boolean isOreChunk();

    OreChunkPurity getPurity();

    ItemStack getMinedItemStack();

    CompoundTag serializeNBT();

    void deserializeNBT(CompoundTag tag);

}
