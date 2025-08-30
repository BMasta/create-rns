package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSTags;
import com.bmaster.createrns.mining.MiningAreaOutlineRenderer;
import com.bmaster.createrns.mining.MiningProcess;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.sound.SoundScapes;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class MinerBlockEntity extends KineticBlockEntity {
    public static final int INVENTORY_SIZE = 9;
    public static final int MINEABLE_DEPOSIT_RADIUS = 1; // 3x3
    public static final int MINEABLE_DEPOSIT_DEPTH = 5;

    public MiningProcess process;
    public Set<BlockPos> reservedDepositBlocks = new HashSet<>();
    public List<BlockState> particleOptions = null;

    private final ItemStackHandler inventory = new ItemStackHandler(INVENTORY_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private LazyOptional<IItemHandler> inventoryCap = LazyOptional.empty();
    private CompoundTag serializedMiningProcess = null;

    public MinerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(RNSContent.MINER_BE.get(), pos, state);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public boolean isMining() {
        if (level == null) return false;
        return !reservedDepositBlocks.isEmpty() && isSpeedRequirementFulfilled();
    }

    public void reserveDepositBlocks() {
        if (level == null) return;

        reservedDepositBlocks = getDepositVein();

        // Exclude deposit blocks reserved by nearby miners
        for (var m : MinerBlockEntityInstanceHolder.getInstancesWithIntersectingMiningArea(this)) {
            reservedDepositBlocks.removeAll(m.reservedDepositBlocks);
        }

        // Mark particle options for recalculation (lazy)
        particleOptions = null;

        // Recompute mining process yields based on claimed mining area (server side).
        // This also happens on process initialization.
        if (process != null && level instanceof ServerLevel sl) process.setYields(sl, reservedDepositBlocks);

        setChanged();
    }

    @Override
    public void tick() {
        super.tick();
        clientTick();
        serverTick();
    }

    public void clientTick() {
        if (level == null || !level.isClientSide) return;
        if (isMining()) spawnParticles();
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;

        // Initialize mining process if not already
        if (process == null) {
            // Create the mining process object
            process = new MiningProcess(sl, reservedDepositBlocks);

            // If we got mining process data from NBT, now is the time to set it
            if (serializedMiningProcess != null) {
                process.setProgressFromNBT(serializedMiningProcess);
                serializedMiningProcess = null;
            }
        }

        if (!isMining()) return;

        process.advance((int) Math.abs(getSpeed()));
        setChanged();

        var yields = process.collect();
        if (yields.isEmpty()) return;

        for (var is : yields) {
            var remainder = Utils.insertItemIntoContainer(inventory, is);
            if (remainder.equals(ItemStack.EMPTY)) {
                CreateRNS.LOGGER.info("Mined {} {}, at {},{}", is.getItem(), is.getCount(),
                        getBlockPos().getX(), getBlockPos().getZ());
            } else {
                CreateRNS.LOGGER.info("Could not fully mine {} at {},{} ({} remaining)", remainder.getItem(),
                        getBlockPos().getX(), getBlockPos().getZ(), remainder.getCount());
            }
        }
    }

    @Override
    public void tickAudio() {
        if (!isMining()) return;
        float speed = Math.abs(getSpeed());

        float pitch = Mth.clamp((speed / 256f) + .45f, .85f, 1f);
        SoundScapes.play(SoundScapes.AmbienceGroup.CRUSHING, worldPosition, pitch);
    }

    protected void spawnParticles() {
        if (level == null) return;
        if (particleOptions == null) {
            particleOptions = reservedDepositBlocks.stream()
                    .map(bp -> level.getBlockState(bp))
                    .toList();
        }

        var r = level.random;
        BlockState selectedParticle = particleOptions.get(r.nextInt(0, particleOptions.size()));
        ParticleOptions particleData = new BlockParticleOption(ParticleTypes.BLOCK, selectedParticle);

        for (int i = 0; i < 2; i++)
            level.addParticle(particleData,
                    worldPosition.getX() + r.nextFloat(),
                    worldPosition.getY() - 0.5 + r.nextFloat(),
                    worldPosition.getZ() + r.nextFloat(),
                    0, 0, 0);
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
        if (level != null && level.isClientSide()) MiningAreaOutlineRenderer.removeMiner(this);
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
            tag.put("MiningProcess", process.getProgressAsNBT());
        }

        var packed = reservedDepositBlocks.stream().mapToLong(BlockPos::asLong).toArray();
        tag.putLongArray("ReservedDepositBlocks", packed);
    }

    @Override
    public void read(@NotNull CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        // Mining process may not be created yet, so we defer serialization (server side)
        if (!clientPacket) {
            serializedMiningProcess = tag.getCompound("MiningProcess");
            inventory.deserializeNBT(tag.getCompound("Items"));
        }

        // Clear outline for the claimed mining area of this miner (client side)
        if (clientPacket) MiningAreaOutlineRenderer.removeMiner(this);

        // Deserialize claimed mining area
        reservedDepositBlocks.clear();
        var packed = tag.getLongArray("ReservedDepositBlocks");
        for (var l : packed) reservedDepositBlocks.add(BlockPos.of(l));

        // Add outline for the freshly deserialized claimed mining area back in (client side)
        if (clientPacket) MiningAreaOutlineRenderer.addMiner(this);

        // Mark particle options for recalculation (lazy)
        particleOptions = null;

        // Recompute mining process yields based on claimed mining area (server side).
        // This also happens on process initialization.
        if (process != null && level instanceof ServerLevel sl) process.setYields(sl, reservedDepositBlocks);
    }

    @Override
    protected void addStressImpactStats(List<Component> tooltip, float stressAtBase) {
        super.addStressImpactStats(tooltip, stressAtBase);
    }

    private BoundingBox getMiningArea(@NotNull Level l) {
        var pos = this.getBlockPos();
        int px = pos.getX(), py = pos.getY(), pz = pos.getZ();
        int minBuildHeight = l.getMinBuildHeight(), maxBuildHeight = l.getMaxBuildHeight();

        int yMin = Mth.clamp(py - MINEABLE_DEPOSIT_DEPTH, minBuildHeight, maxBuildHeight);
        int yMax = Mth.clamp(py - 1, minBuildHeight, maxBuildHeight);

        return new BoundingBox(
                px - MINEABLE_DEPOSIT_RADIUS, yMin, pz - MINEABLE_DEPOSIT_RADIUS,
                px + MINEABLE_DEPOSIT_RADIUS, yMax, pz + MINEABLE_DEPOSIT_RADIUS);
    }

    private Set<BlockPos> getDepositVein() {
        if (level == null) return Set.of();

        var depTag = RNSTags.Block.DEPOSIT_BLOCKS;
        var ma = getMiningArea(level);
        Queue<BlockPos> q = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet(ma.getXSpan() * ma.getYSpan() * ma.getZSpan());

        q.offer(worldPosition.below());
        while (!q.isEmpty()) {
            var bp = q.poll();

            if (visited.contains(bp.asLong()) || !ma.isInside(bp) || !level.getBlockState(bp).is(depTag)) continue;
            visited.add(bp.asLong());

            Direction.stream().forEach(d -> q.add(bp.relative(d)));
        }
        return visited.longStream().mapToObj(BlockPos::of).collect(Collectors.toSet());
    }
}
