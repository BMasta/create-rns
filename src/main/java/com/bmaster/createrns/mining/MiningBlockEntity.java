package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.deposit.capability.IDepositIndex;
import com.bmaster.createrns.mining.miner.MinerBlock;
import com.bmaster.createrns.mining.miner.MinerSpec;
import com.bmaster.createrns.mining.miner.MinerSpecLookup;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class MiningBlockEntity extends KineticBlockEntity {
    public Set<BlockPos> reservedDepositBlocks = new HashSet<>();
    protected MinerSpec spec = null;
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

    public abstract boolean isMining();

    public @Nullable MinerSpec getSpec() {
        if (spec == null && !tryInitSpec()) return null;
        return spec;
    }

    public MiningEntityItemHandler getInventory() {
        return inventory;
    }

    public @Nullable BoundingBox getMiningArea() {
        if (level == null || (spec == null && !tryInitSpec())) return null;
        var pos = this.getBlockPos();
        int px = pos.getX(), py = pos.getY(), pz = pos.getZ();
        int minBuildHeight = level.getMinBuildHeight(), maxBuildHeight = level.getMaxBuildHeight();

        var area = spec.miningArea();
        int yMin = Mth.clamp(py + area.verticalOffset() - area.height() + 1, minBuildHeight, maxBuildHeight);
        int yMax = Mth.clamp(py + area.verticalOffset(), minBuildHeight, maxBuildHeight);

        return new BoundingBox(
                px - area.radius(), yMin, pz - area.radius(),
                px + area.radius(), yMax, pz + area.radius());
    }

    public int getCurrentProgressIncrement() {
        if (spec == null || !tryInitSpec()) return 0;
        return (int) (spec.minesPerHour() * Math.abs(getSpeed()));
    }

    public void reserveDepositBlocks() {
        if (level == null || (spec == null & !tryInitSpec())) return;

        reservedDepositBlocks = getDepositVein().stream()
                .filter(pos -> MiningRecipeLookup.isDepositMineable(level, level.getBlockState(pos).getBlock(),
                        spec.tier()))
                .collect(Collectors.toSet());

        // Exclude deposit blocks reserved by nearby mining entities
        for (var m : MiningBlockEntityInstanceHolder.getInstancesWithIntersectingMiningArea(this)) {
            reservedDepositBlocks.removeAll(m.reservedDepositBlocks);
        }

        // Recompute mining process based on claimed mining area
        tryInitProcess(true);

        // Initialize deposit durabilities as needed
        if (level instanceof ServerLevel sl) {
            var depIdx = IDepositIndex.fromLevel(sl);
            if (depIdx != null) {
                for (var bp : reservedDepositBlocks) {
                    depIdx.initDepositVeinDurability(bp);
                }
            }
        }

        setChanged();
    }

    @Override
    public void tick() {
        super.tick();

        if (level == null) return;
        if ((process == null && !tryInitProcess(false))) return;
        if (level.isClientSide || !isMining()) return;

        process.advance(getCurrentProgressIncrement());
        inventory.collectMinedItems(process);
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
        if (process != null || tryInitProcess(false)) tag.put("MiningProgress", process.getProgressAsNBT());
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

        // Deserialize mining progress
        if (tag.contains("MiningProgress")) {
            miningProgressTag = tag.getCompound("MiningProgress");
        }

        // Recompute mining process yields based on claimed mining area. This also happens on process initialization.
        tryInitProcess(true);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean tryInitSpec() {
        if (level == null) return false;
        spec = MinerSpecLookup.get(level.registryAccess(), (MinerBlock) getBlockState().getBlock());
        return true;
    }

    protected boolean tryInitProcess(boolean recompute) {
        if (process != null && !recompute) return true;
        if (level == null || (spec == null && !tryInitSpec())) return false;
        for (var bp : reservedDepositBlocks) {
            if (!level.isLoaded(bp)) return false;
        }

        process = new MiningProcess(level, spec.tier(), reservedDepositBlocks);

        // If we got mining progress data from NBT, now is the time to set it
        if (miningProgressTag != null) {
            process.setProgressFromNBT(miningProgressTag);
            miningProgressTag = null;
        }

        return true;
    }

    private Set<BlockPos> getDepositVein() {
        if (level == null) return Set.of();
        var ma = getMiningArea();
        if (ma == null) return Set.of();

        Queue<BlockPos> q = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet(ma.getXSpan() * ma.getYSpan() * ma.getZSpan());

        q.offer(worldPosition.relative(Direction.Axis.Y, spec.miningArea().verticalOffset()));
        while (!q.isEmpty()) {
            var bp = q.poll();
            var b = level.getBlockState(bp);
            if (visited.contains(bp.asLong()) || !ma.isInside(bp) || !b.is(RNSTags.Block.DEPOSIT_BLOCKS)) continue;
            visited.add(bp.asLong());

            Direction.stream().forEach(d -> q.add(bp.relative(d)));
        }
        return visited.longStream().mapToObj(BlockPos::of).collect(Collectors.toSet());
    }
}
