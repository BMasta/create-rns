package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public abstract class MiningBlockEntity extends KineticBlockEntity {
    public Set<BlockPos> reservedDepositBlocks = new HashSet<>();
    protected MiningProcess process = null;

    protected final MiningEntityItemHandler inventory = new MiningEntityItemHandler(() -> {
        if (level != null && !level.isClientSide) {
            setChanged();
            notifyUpdate();
        }
    });

    private LazyOptional<IItemHandler> inventoryCap = LazyOptional.empty();
    private CompoundTag miningProgressTag = null;

    public MiningBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract int getMiningAreaRadius();

    public abstract int getMiningAreaDepth();

    public abstract int getMiningAreaYOffset();

    public abstract int getCurrentProgressIncrement();

    public abstract int getBaseProgress();

    public abstract boolean isMining();

    public abstract MiningLevel getMiningLevel();

    public MiningEntityItemHandler getInventory() {
        return inventory;
    }

    public BoundingBox getMiningArea(@NotNull Level l) {
        var pos = this.getBlockPos();
        int px = pos.getX(), py = pos.getY(), pz = pos.getZ();
        int minBuildHeight = l.getMinBuildHeight(), maxBuildHeight = l.getMaxBuildHeight();

        int mineRadius = getMiningAreaRadius();
        int yMin = Mth.clamp(py + getMiningAreaYOffset() - getMiningAreaDepth() + 1, minBuildHeight, maxBuildHeight);
        int yMax = Mth.clamp(py + getMiningAreaYOffset(), minBuildHeight, maxBuildHeight);

        return new BoundingBox(
                px - mineRadius, yMin, pz - mineRadius,
                px + mineRadius, yMax, pz + mineRadius);
    }

    public void reserveDepositBlocks() {
        if (level == null) return;

        var ml = getMiningLevel();
        reservedDepositBlocks = getDepositVein().stream()
                .filter(pos -> MiningRecipeLookup.isDepositMineable(level, level.getBlockState(pos).getBlock(), ml))
                .collect(Collectors.toSet());

        // Exclude deposit blocks reserved by nearby mining entities
        for (var m : MiningBlockEntityInstanceHolder.getInstancesWithIntersectingMiningArea(this)) {
            reservedDepositBlocks.removeAll(m.reservedDepositBlocks);
        }

        // Recompute mining process yields based on claimed mining area. This also happens on process initialization.
        if (process != null && level != null) process.setYields(level, reservedDepositBlocks, getBaseProgress());

        setChanged();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;

        if (process == null) {
            // Create the mining process object
            process = new MiningProcess(level, getMiningLevel(), reservedDepositBlocks, getBaseProgress());

            // If we got mining process data from NBT, now is the time to set it
            if (miningProgressTag != null) {
                process.setProgressFromNBT(miningProgressTag);
                miningProgressTag = null;
            }
        }

        if (isMining()) {
            if (!level.isClientSide) {
                process.advance(getCurrentProgressIncrement());
                inventory.collectMinedItems(process);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();

        // Initialize the inventory capability when the BE is first loaded
        inventoryCap = LazyOptional.of(() -> inventory);

        MiningBlockEntityInstanceHolder.addInstance(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();

        inventoryCap.invalidate();

        MiningBlockEntityInstanceHolder.removeInstance(this);
        if (level != null && level.isClientSide()) MiningAreaOutlineRenderer.removeMiningBE(this);
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
        tag.put("Inventory", inventory.serializeNBT());
        var packed = reservedDepositBlocks.stream().mapToLong(BlockPos::asLong).toArray();
        tag.putLongArray("ReservedDepositBlocks", packed);
    }

    @Override
    public void read(@NotNull CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (clientPacket)
            CreateRNS.LOGGER.trace("Client mining BE synced at {}, {}", worldPosition.getX(), worldPosition.getZ());
        inventory.deserializeNBT(tag.getCompound("Inventory"));

        // Clear outline for the claimed mining area of this BE (client side)
        if (clientPacket) MiningAreaOutlineRenderer.removeMiningBE(this);

        // Deserialize claimed mining area
        reservedDepositBlocks.clear();
        var packed = tag.getLongArray("ReservedDepositBlocks");
        for (var l : packed) reservedDepositBlocks.add(BlockPos.of(l));

        // Add outline for the freshly deserialized claimed mining area back in (client side)
        if (clientPacket) MiningAreaOutlineRenderer.addMiningBE(this);

        // Recompute mining process yields based on claimed mining area. This also happens on process initialization.
        if (process != null && level != null) process.setYields(level, reservedDepositBlocks, getBaseProgress());
    }

    private Set<BlockPos> getDepositVein() {
        if (level == null) return Set.of();

        var depTag = RNSTags.Block.DEPOSIT_BLOCKS;
        var ma = getMiningArea(level);
        Queue<BlockPos> q = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet(ma.getXSpan() * ma.getYSpan() * ma.getZSpan());

        q.offer(worldPosition.relative(Direction.Axis.Y, getMiningAreaYOffset()));
        while (!q.isEmpty()) {
            var bp = q.poll();

            if (visited.contains(bp.asLong()) || !ma.isInside(bp) || !level.getBlockState(bp).is(depTag)) continue;
            visited.add(bp.asLong());

            Direction.stream().forEach(d -> q.add(bp.relative(d)));
        }
        return visited.longStream().mapToObj(BlockPos::of).collect(Collectors.toSet());
    }
}
