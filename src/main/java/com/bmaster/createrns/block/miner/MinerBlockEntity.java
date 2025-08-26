package com.bmaster.createrns.block.miner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.capability.MinerItemStackHandler;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MinerBlockEntity extends KineticBlockEntity implements MenuProvider {
    public static final int INVENTORY_SIZE = 1;
    public static final int MINEABLE_DEPOSIT_RADIUS = 1; // 3x3
    public static final int MINEABLE_DEPOSIT_DEPTH = 5;

    public MiningProcess process;
    public Set<BlockPos> reservedDepositBlocks = new HashSet<>();

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private LazyOptional<IItemHandler> inventoryCap = LazyOptional.empty();
    private int setProgressWhenPossibleTo = -1;

    public MinerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(AllContent.MINER_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean isMining() {
        return process != null && process.isPossible() && !process.isDone() &&
                getSpeed() != 0 && isSpeedRequirementFulfilled();
    }

    public void reserveDepositBlocks() {
        if (!(level instanceof ServerLevel sl) || level.isClientSide) return;
        var pos = this.getBlockPos();
        int px = pos.getX(), py = pos.getY(), pz = pos.getZ();
        int minBuildHeight = level.getMinBuildHeight(), maxBuildHeight = level.getMaxBuildHeight();

        int yMin = Mth.clamp(py - MINEABLE_DEPOSIT_DEPTH, minBuildHeight, maxBuildHeight);
        int yMax = Mth.clamp(py - 1, minBuildHeight, maxBuildHeight);

        BlockPos min = new BlockPos(px - MINEABLE_DEPOSIT_RADIUS, yMin, pz - MINEABLE_DEPOSIT_RADIUS);
        BlockPos max = new BlockPos(px + MINEABLE_DEPOSIT_RADIUS, yMax, pz + MINEABLE_DEPOSIT_RADIUS);

        var depBlockStream = BlockPos.betweenClosedStream(min, max)
                .filter(bp -> level.getBlockState(bp).is(RNSTags.Block.DEPOSIT_BLOCKS))
                .map(BlockPos::immutable);

        // Exclude deposit blocks reserved by nearby miners
        for (var m : MinerBlockEntityInstanceHolder.getInstancesWithIntersectingMiningArea(this)) {
            depBlockStream = depBlockStream.filter(bp -> !m.reservedDepositBlocks.contains(bp));
        }
        reservedDepositBlocks = depBlockStream.collect(Collectors.toUnmodifiableSet());
        if (process != null) process.setYield(sl, reservedDepositBlocks);

        setChanged();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel sl)) return;

        // Try initializing mining process
        if (process == null) {
            // Create the mining process object
            int nDepBlocks = reservedDepositBlocks.size();
            process = new MiningProcess(sl, reservedDepositBlocks, 0.2f);

            // If we got progress data from NBT, now is the time to set it
            if (setProgressWhenPossibleTo >= 0) {
                process.setProgress(setProgressWhenPossibleTo);
            }
            // Better luck next time
            if (process == null) return;
        }

        if (isMining()) {
            process.advance((int) Math.abs(getSpeed()));
            setChanged();
        }

        if (process.isDone()) {
            var yield = process.collect();
            for (var is : yield) {
                if (inventory.insertItem(0, is, false) == ItemStack.EMPTY) {
                    CreateRNS.LOGGER.info("Mined {} {}, at {},{}", is.getItem(), is.getCount(),
                            getBlockPos().getX(), getBlockPos().getZ());
                } else {
                    CreateRNS.LOGGER.info("Could not fully mine {} at {},{}", is.getItem(),
                            getBlockPos().getX(), getBlockPos().getZ());
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        // Initialize the inventory capability when the BE is first loaded
        inventoryCap = LazyOptional.of(() -> inventory);

        MinerBlockEntityInstanceHolder.addInstance(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();

        inventoryCap.invalidate();

        MinerBlockEntityInstanceHolder.removeInstance(this);
        if (level != null && level.isClientSide()) MiningAreaOutlineRenderer.refreshOutline();
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
    public void write(@NotNull CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("Items", inventory.serializeNBT());
        if (process != null) {
            tag.putInt("Progress", process.getProgress());
        }

        var packed = reservedDepositBlocks.stream().mapToLong(BlockPos::asLong).toArray();
        tag.putLongArray("ReservedDepositBlocks", packed);
    }

    @Override
    public void read(@NotNull CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        // Mining process is not yet created, so we save the progress value for later
        setProgressWhenPossibleTo = tag.getInt("Progress");
        inventory.deserializeNBT(tag.getCompound("Items"));

        reservedDepositBlocks.clear();
        var packed = tag.getLongArray("ReservedDepositBlocks");
        for (var l : packed) reservedDepositBlocks.add(BlockPos.of(l));
        if (level != null && process != null) process.setYield(level, reservedDepositBlocks);

        if (clientPacket) MiningAreaOutlineRenderer.addReservedDepositBlocksToOutline(this);
    }

    @NotNull
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.%s.miner".formatted(CreateRNS.MOD_ID));
    }

    @Nullable
    @ParametersAreNonnullByDefault
    @Override
    public @org.jetbrains.annotations.Nullable AbstractContainerMenu
    createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new MinerMenu(AllContent.MINER_MENU.get(), pContainerId, pPlayerInventory, this);
    }

    @Override
    protected void addStressImpactStats(List<Component> tooltip, float stressAtBase) {
        super.addStressImpactStats(tooltip, stressAtBase);
    }
}
