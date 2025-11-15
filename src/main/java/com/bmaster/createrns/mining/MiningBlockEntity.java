package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.mining.miner.MinerBlock;
import com.bmaster.createrns.mining.miner.MinerSpec;
import com.bmaster.createrns.mining.miner.MinerSpecLookup;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class MiningBlockEntity extends KineticBlockEntity {
    public Set<BlockPos> reservedDepositBlocks = new HashSet<>();
    protected MinerSpec spec = null;
    protected MiningProcess process = null;

    protected final MiningEntityItemHandler inventory = new MiningEntityItemHandler(() -> {
        if (level != null && !level.isClientSide) {
            level.invalidateCapabilities(worldPosition);
            setChanged();
            notifyUpdate();
        }
    });

    private CompoundTag miningProgressTag = null;

    public MiningBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract boolean isMining();

    public @Nullable MinerSpec getSpec() {
        if (spec == null && !tryInitSpec()) return null;
        return spec;
    }

    public @Nullable MiningEntityItemHandler getItemHandler(Direction side) {
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
        for (var bp : reservedDepositBlocks) {
            level.getData(RNSContent.LEVEL_DEPOSIT_DATA.get()).initDepositVeinDurability(bp);
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
        MiningBlockEntityInstanceHolder.addInstance(this);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        MiningBlockEntityInstanceHolder.removeInstance(this);

        if (level == null) return;

        level.invalidateCapabilities(worldPosition);
        if (level.isClientSide()) MiningAreaOutlineRenderer.removeMiningBE(this);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider p, boolean clientPacket) {
        super.write(tag, p, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(p));
        var packed = reservedDepositBlocks.stream().mapToLong(BlockPos::asLong).toArray();
        tag.putLongArray("ReservedDepositBlocks", packed);
        if (process != null || tryInitProcess(false)) tag.put("MiningProgress", process.getProgressAsNBT());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider p, boolean clientPacket) {
        super.read(tag, p, clientPacket);
        if (clientPacket)
            CreateRNS.LOGGER.trace("Client mining BE synced at {}, {}", worldPosition.getX(), worldPosition.getZ());
        inventory.deserializeNBT(p, tag.getCompound("Inventory"));

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
            if (visited.contains(bp.asLong()) || !ma.isInside(bp) || !level.getBlockState(bp).is(RNSTags.Block.DEPOSIT_BLOCKS)) continue;
            visited.add(bp.asLong());

            Direction.stream().forEach(d -> q.add(bp.relative(d)));
        }
        return visited.longStream().mapToObj(BlockPos::of).collect(Collectors.toSet());
    }
}
