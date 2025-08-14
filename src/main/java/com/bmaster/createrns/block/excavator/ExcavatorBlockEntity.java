package com.bmaster.createrns.block.excavator;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.capability.ExcavatorItemStackHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

public class ExcavatorBlockEntity extends KineticBlockEntity implements MenuProvider {
    public ExcavationProcess process;

    public static final int INVENTORY_SIZE = 1;
    private final ExcavatorItemStackHandler inventory = new ExcavatorItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };;
    private LazyOptional<IItemHandler> inventoryCap = LazyOptional.empty();
    private int setProgressWhenPossibleTo = -1;

    public ExcavatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ExcavatorBlockEntity(BlockPos pos, BlockState state) {
        super(AllContent.EXCAVATOR_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        // Try initializing excavation process
        if (process == null) {
            LevelChunk chunk = level.getChunkAt(this.getBlockPos());
            chunk.getCapability(AllContent.ORE_CHUNK_DATA).ifPresent(data -> {
                // Create the excavation process object
                process = new ExcavationProcess(data);

                // Restrict inventory to only accept the item type being excavated
                inventory.setExcavatedItem(process.excavatedItemStack.getItem());

                // If we got progress data from NBT, now is the time to set it
                if (setProgressWhenPossibleTo >= 0) {
                    process.setProgress(setProgressWhenPossibleTo);
                }
            });
        }
        // Better luck next time
        if (process == null) return;

        process.advance();
        setChanged();

        if (process.isDone()) {
            ItemStack yield = process.collect();
            if (inventory.insertItem(0, yield, false) == ItemStack.EMPTY) {
                CreateRNS.LOGGER.info("Excavated {} -> {} at {},{}", inventory.getStackInSlot(0).getCount() - 1,
                        inventory.getStackInSlot(0).getCount(), getBlockPos().getX(), getBlockPos().getZ());
            } else {
                CreateRNS.LOGGER.info("Could not excavate at {},{}", getBlockPos().getX(), getBlockPos().getZ());
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        // Initialize the inventory capability when the BE is first loaded
        inventoryCap = LazyOptional.of(() -> inventory);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        // Clean up when chunk unloads or block is broken
        inventoryCap.invalidate();
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void read(@NotNull CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        // Excavation process is not yet created, so we save the progress value for later
        setProgressWhenPossibleTo = tag.getInt("Progress");
        inventory.deserializeNBT(tag.getCompound("Items"));
    }

    @Override
    public void write(@NotNull CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("Items", inventory.serializeNBT());
        if (process != null) {
            tag.putInt("Progress", process.getProgress());
        }
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.%s.excavator".formatted(CreateRNS.MOD_ID));
    }

    @Nullable
    @ParametersAreNonnullByDefault
    @Override
    public @org.jetbrains.annotations.Nullable AbstractContainerMenu
    createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        Item ghostItem = null;
        if (process != null) {
            ghostItem = process.excavatedItemStack.getItem();
        }
        return new ExcavatorMenu(AllContent.EXCAVATOR_MENU.get(), pContainerId, pPlayerInventory, this, ghostItem);
    }
}
