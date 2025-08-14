package com.bmaster.createrns.capability;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class ExcavatorItemStackHandler extends ItemStackHandler {
    private Item excavatedItem = null;

    public ExcavatorItemStackHandler() {
        super();
    }

    public ExcavatorItemStackHandler(int size) {
        super(size);
    }

    public ExcavatorItemStackHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    public ExcavatorItemStackHandler(Item excavatedItem) {
        super();
        this.excavatedItem = excavatedItem;
    }

    public ExcavatorItemStackHandler(int size, Item excavatedItem) {
        super(size);
        this.excavatedItem = excavatedItem;
    }

    public ExcavatorItemStackHandler(NonNullList<ItemStack> stacks, Item excavatedItem) {
        super(stacks);
        this.excavatedItem = excavatedItem;
    }

    public Item getExcavatedItem() {
        return excavatedItem;
    }

    public void setExcavatedItem(Item excavatedItem) {
        this.excavatedItem = excavatedItem;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (stack.getItem() != excavatedItem) {
            return false;
        }
        return super.isItemValid(slot, stack);
    }
}
