package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.RNSContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.level.Level;

public class MinerMenu extends AbstractContainerMenu {
    public static final int YIELD_SLOT_INDEX = 0;

    public static final int SLOT_SIZE = 18;

    public static final int YIELD_PIXEL_OFFSET_X = 80;
    public static final int YIELD_PIXEL_OFFSET_Y = 52;

    public static final int PLAYER_PIXEL_OFFSET_X = 8;
    public static final int PLAYER_PIXEL_OFFSET_Y = 86;
    public static final int HOTBAR_PIXEL_OFFSET_Y = 4;

    private static final int CD_INDEX_CURRENT_PROGRESS = 0;
    private static final int CD_INDEX_MAX_PROGRESS = 1;
    private static final int CD_ARG_COUNT = 2;

    // Server and client side
    // Note to self: do not rely on the client-side block entity too much as it can easily become stale.
    private final MinerBlockEntity blockEntity;

    // Client side only
    private int clientProgress, clientMaxProgress;

    // Server-side constructor
    public MinerMenu(MenuType<?> type, int id, Inventory pPlayerInv, MinerBlockEntity pBE) {
        super(type, id);
        this.blockEntity = pBE;

        // Add Yield slot
        this.addSlot(new SlotItemHandler(
                pBE.getInventory(),
                0,
                YIELD_PIXEL_OFFSET_X,
                YIELD_PIXEL_OFFSET_Y
        ));

        // Add data slots for progress bar
        final ContainerData data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case CD_INDEX_CURRENT_PROGRESS -> blockEntity.process.getProgress();
                    case CD_INDEX_MAX_PROGRESS -> blockEntity.process.getMaxProgress();
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                if (pIndex == CD_INDEX_CURRENT_PROGRESS) {
                    clientProgress = pValue;
                    // Not strictly necessary, but it's nice to keep the client-side BE in sync
                    if (blockEntity.process != null) {
                        blockEntity.process.setProgress(pValue);
                    }
                } else if (pIndex == CD_INDEX_MAX_PROGRESS) {
                    clientMaxProgress = pValue;
                    // Not strictly necessary, but it's nice to keep the client-side BE in sync
                    if (blockEntity.process != null) {
                        blockEntity.process.setMaxProgress(pValue);
                    }
                }
            }

            @Override
            public int getCount() {
                return CD_ARG_COUNT;
            }
        };

        // Add data slots (used for progress bar)
        addDataSlots(data);

        layoutPlayerInventorySlots(pPlayerInv);
    }

    // Client-side constructor
    public MinerMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf buf) {
        this(type, id, inv, getBlockEntity(inv, buf));
    }

    public int getProgress() {
        return clientProgress;
    }

    public int getMaxProgress() {
        return clientMaxProgress;
    }

    private static MinerBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MinerBlockEntity ex) return ex;
        throw new IllegalStateException("BlockEntity not found at " + pos);
    }

    private void layoutPlayerInventorySlots(Inventory inv) {
        // main 3×9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        inv,
                        9 + col + (row * 9),
                        PLAYER_PIXEL_OFFSET_X + col * SLOT_SIZE,
                        PLAYER_PIXEL_OFFSET_Y + row * SLOT_SIZE
                ));
            }
        }
        // hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    inv,
                    col,
                    PLAYER_PIXEL_OFFSET_X + col * SLOT_SIZE,
                    PLAYER_PIXEL_OFFSET_Y + (3 * SLOT_SIZE) + HOTBAR_PIXEL_OFFSET_Y
            ));
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return false;
        }
        return AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, RNSContent.MINER_BLOCK.get()
        );
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack original = slot.getItem();
            moved = original.copy();

            final int MINER_START = 0;
            final int MINER_END = MinerBlockEntity.INVENTORY_SIZE;
            final int PLAYER_START = MINER_END;
            final int PLAYER_END = MINER_END + 36;

            if (index < MINER_END) {
                if (!moveItemStackTo(original, PLAYER_START, PLAYER_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(original, MINER_START, MINER_END, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (original.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();
            slot.onTake(player, original);
        }
        return moved;
    }
}
