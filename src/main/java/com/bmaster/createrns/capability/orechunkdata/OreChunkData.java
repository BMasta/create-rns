package com.bmaster.createrns.capability.orechunkdata;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

public class OreChunkData implements IOreChunkData, INBTSerializable<CompoundTag> {
    public static final OreChunkData EMPTY = new OreChunkData(false, ItemStack.EMPTY, OreChunkPurity.NONE);

    private boolean isOreChunk;
    private ItemStack excavatedItemStack;
    private OreChunkPurity purity;

    public OreChunkData(boolean isOreChunk, ItemStack excavatedItemStack, OreChunkPurity purity) {
        this.isOreChunk = isOreChunk;
        this.excavatedItemStack = excavatedItemStack;
        this.purity = purity;
    }

    @Override
    public boolean isOreChunk() {
        return isOreChunk;
    }

    @Override
    public ItemStack getExcavatedItemStack() {
        return excavatedItemStack;
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
            tag.put("Yield", excavatedItemStack.serializeNBT());
            tag.putString("Purity", purity.name());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        CreateRNS.LOGGER.info("Deserializing {}", tag);
        isOreChunk = tag.getBoolean("IsOreChunk");
        if (isOreChunk) {
            CompoundTag yield = tag.getCompound("Yield");
            if (!yield.isEmpty()) {
                excavatedItemStack = ItemStack.of(yield);
            }
            CreateRNS.LOGGER.info("Deserialized item stack as {} from {}", excavatedItemStack,
                    tag.getCompound("Yield"));

            purity = OreChunkPurity.valueOf(tag.getString("Purity"));
        } else {
            excavatedItemStack = ItemStack.EMPTY;
            purity = OreChunkPurity.NONE;
        }
    }
}
