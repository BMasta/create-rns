package com.bmaster.createrns.capability;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class MinerItemStackHandler extends ItemStackHandler {
    private Item minedItem = null;

    public MinerItemStackHandler() {
        super();
    }

    public MinerItemStackHandler(int size) {
        super(size);
    }

    public MinerItemStackHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    public MinerItemStackHandler(Item minedItem) {
        super();
        this.minedItem = minedItem;
    }

    public MinerItemStackHandler(int size, Item minedItem) {
        super(size);
        this.minedItem = minedItem;
    }

    public MinerItemStackHandler(NonNullList<ItemStack> stacks, Item minedItem) {
        super(stacks);
        this.minedItem = minedItem;
    }

    public Item getMinedItem() {
        return minedItem;
    }

    public void setMinedItem(Item minedItem) {
        this.minedItem = minedItem;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (stack.getItem() != minedItem) {
            return false;
        }
        return super.isItemValid(slot, stack);
    }
}
