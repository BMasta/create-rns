package com.bmaster.createrns.capability.orechunkdata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

public class OreChunkData implements IOreChunkData, INBTSerializable<CompoundTag> {
    public static final OreChunkData EMPTY = new OreChunkData(false, ItemStack.EMPTY, OreChunkPurity.NONE);

    private boolean isOreChunk;
    private ItemStack minedItemStack;
    private OreChunkPurity purity;

    public OreChunkData(boolean isOreChunk, ItemStack minedItemStack, OreChunkPurity purity) {
        this.isOreChunk = isOreChunk;
        this.minedItemStack = minedItemStack;
        this.purity = purity;
    }

    @Override
    public boolean isOreChunk() {
        return isOreChunk;
    }

    @Override
    public ItemStack getMinedItemStack() {
        return minedItemStack;
    }

    @Override
    public OreChunkPurity getPurity() {
        return purity;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IsOreChunk", isOreChunk);
        if (isOreChunk) {
            tag.put("Yield", minedItemStack.serializeNBT());
            tag.putString("Purity", purity.name());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        isOreChunk = tag.getBoolean("IsOreChunk");
        if (isOreChunk) {
            CompoundTag yield = tag.getCompound("Yield");
            if (!yield.isEmpty()) {
                minedItemStack = ItemStack.of(yield);
            }

            purity = OreChunkPurity.valueOf(tag.getString("Purity"));
        } else {
            minedItemStack = ItemStack.EMPTY;
            purity = OreChunkPurity.NONE;
        }
    }
}
