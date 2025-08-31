package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MiningEntityItemHandler implements IItemHandler, INBTSerializable<CompoundTag> {
    private static final int MAX_COUNT_PER_TYPE = 64;
    private List<Item> types;
    private Object2ObjectOpenHashMap<Item, ItemStack> typeToStack;
    private final Runnable onContentsChangedRunnable;

    public MiningEntityItemHandler(Runnable onContentsChanged) {
        types = new ArrayList<>();
        typeToStack = new Object2ObjectOpenHashMap<>();
        this.onContentsChangedRunnable = onContentsChanged;
    }

    public MiningEntityItemHandler(List<Item> slotTypes, Runnable onContentsChanged) {
        types = new ArrayList<>(slotTypes);
        typeToStack = slotTypes.stream().collect(Collectors.toMap(
                i -> i, i -> ItemStack.EMPTY, (o, n) -> n,
                () -> new Object2ObjectOpenHashMap<>(slotTypes.size())
        ));
        this.onContentsChangedRunnable = onContentsChanged;
    }

    public boolean isEmpty() {
        return typeToStack.values().stream().allMatch(s -> s.getCount() < 1);
    }

    @Override
    public int getSlots() {
        return typeToStack.size();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        var stack = typeToStack.get(types.get(slot));
        return (stack.getCount() > 0) ? stack.copy() : ItemStack.EMPTY;
    }

    /// Attempts to collect items mined by the given mining process.
    ///
    /// @return True if any items were added to the inventory. False otherwise.
    public boolean collectMinedItems(MiningProcess process) {
        if (process == null) return false;
        boolean invUpdated = false;
        for (var minedStack : process.collect()) {
            var type = minedStack.getItem();
            var minedCount = minedStack.getCount();

            // Add new type if not already present
            if (!typeToStack.containsKey(type)) {
                types.add(0, type);
                typeToStack.put(type, new ItemStack(type, 0));
            }

            // Increase count for that type
            var existingStack = typeToStack.get(type);
            int existingCount = existingStack.getCount();
            int updatedCount = Math.min(MAX_COUNT_PER_TYPE, existingCount + minedCount);

            invUpdated = updatedCount != existingCount;
            if (invUpdated) {
                existingStack.setCount(updatedCount);
                CreateRNS.LOGGER.info("Mined {}", existingStack.getItem());
            } else {
                CreateRNS.LOGGER.info("Could not mine {}", existingStack.getItem());
            }
        }

        if (invUpdated) onContentsChanged();
        return invUpdated;
    }

    /// Always a no-op as mining entities only permit insertion from a mining process.
    /// @return Copy of the given item stack.
    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return stack;
    }

    /// Extracts an ItemStack from the given slot
    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot >= types.size()) return ItemStack.EMPTY;

        var type = types.get(slot);
        var existingStack = typeToStack.get(type);
        var existingStackCount = existingStack.getCount();

        var extractedCount = Math.min(amount, existingStackCount);
        var updatedCount = existingStackCount - extractedCount;

        if (!simulate && updatedCount != existingStackCount) {
            if (updatedCount > 0) {
                existingStack.setCount(updatedCount);
            } else {
                types.remove(type);
                typeToStack.remove(type);
            }
            onContentsChanged();
        }

        return (extractedCount > 0) ? new ItemStack(existingStack.getItem(), extractedCount) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return MAX_COUNT_PER_TYPE;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return false;
    }

    private void onContentsChanged() {
        onContentsChangedRunnable.run();
    }

    @Override
    public CompoundTag serializeNBT()
    {
        ListTag nbtTagList = new ListTag();
        for (int i = 0; i < types.size(); ++i) {
            var type = types.get(i);
            var stack = typeToStack.get(type);
            if (stack.isEmpty()) continue;

            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("Slot", i);
            stack.save(itemTag);
            nbtTagList.add(itemTag);
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", nbtTagList);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        typeToStack.clear();

        ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
        var tags = IntStream.range(0, tagList.size())
                .mapToObj(tagList::getCompound)
                .toList();
        types = new ArrayList<>(tags.size());
        for (var t : tags) {
            int slot = t.getInt("Slot");
            if (0 <= slot && slot < tags.size()) {
                var newStack = ItemStack.of(t);
                var newType = newStack.getItem();
                types.add(slot, newType);
                typeToStack.put(newType, newStack);
            }
        }
    }
}
