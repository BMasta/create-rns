package com.bmaster.createrns.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.items.IItemHandler;

public class Utils {
    public static boolean isPosInChunk(BlockPos pos, ChunkPos chunkPos) {
        return (pos.getX() >> 4) == chunkPos.x && (pos.getZ() >> 4) == chunkPos.z;
    }

    public static float easeOut(float val, int deg) {
        return 1 - (float) Math.pow(1 - val, deg);
    }

    /// Tries to insert item stack into container while prioritizing earlier slots.
    /// @return Remaining item stack that could not be inserted. ItemStack.EMPTY if all items were inserted.
    public static ItemStack insertItemIntoContainer(IItemHandler container, ItemStack insertedStack) {
        ItemStack hotPotato = insertedStack.copy();
        for (int i = 0; i < container.getSlots(); ++i) {
            hotPotato = container.insertItem(i, hotPotato, false);
            if (hotPotato.isEmpty()) return ItemStack.EMPTY;
        }
        return hotPotato;
    }
}
