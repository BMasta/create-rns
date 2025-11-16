package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.deposit.DepositClaimerInstanceHolder;
import com.bmaster.createrns.deposit.DepositClaimerOutlineRenderer;
import com.bmaster.createrns.deposit.IDepositBlockClaimer;
import com.bmaster.createrns.deposit.capability.IDepositIndex;
import com.bmaster.createrns.mining.miner.MinerBlock;
import com.bmaster.createrns.mining.miner.MinerSpec;
import com.bmaster.createrns.mining.miner.MinerSpecLookup;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class MiningBlockEntity extends KineticBlockEntity implements IDepositBlockClaimer {
    public Set<BlockPos> claimedDepositBlocks = new HashSet<>();
    protected MinerSpec spec = null;
    protected MiningProcess process = null;

    protected final MiningEntityItemHandler inventory = new MiningEntityItemHandler(() -> {
        if (level != null && !level.isClientSide) {
            setChanged();
            notifyUpdate();
        }
    });

    private LazyOptional<IItemHandler> inventoryCap = LazyOptional.empty();
    private CompoundTag pendingProcessTag = null;

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

    @Override
    public ClaimingAreaSpec getClaimingAreaSpec() {
        var spec = getSpec();
        if (spec == null) throw new RuntimeException("Failed to get miner spec");
        return spec.miningArea();
    }

    @Override
    public BlockPos getAnchor() {
        return worldPosition;
    }

    @Override
    public Set<BlockPos> getClaimedDepositBlocks() {
        return claimedDepositBlocks;
    }

    @Override
    public void setClaimedDepositBlocks(Set<BlockPos> claimedBlocks) {
        claimedDepositBlocks = claimedBlocks;
    }

    @Override
    public void claimDepositBlocks() {
        if (level == null || (spec == null & !tryInitSpec())) return;

        claimedDepositBlocks = getClaimableDepositVein(level).stream()
                .filter(pos -> MiningRecipeLookup.isDepositMineable(level, level.getBlockState(pos).getBlock(),
                        spec.tier()))
                .collect(Collectors.toSet());

        // Recompute mining process based on claimed mining area
        tryInitProcess(true);

        // Initialize deposit durabilities as needed
        if (level instanceof ServerLevel sl) {
            var depIdx = IDepositIndex.fromLevel(sl);
            if (depIdx != null) {
                for (var bp : claimedDepositBlocks) {
                    depIdx.initDepositVeinDurability(bp);
                }
            }
        }

        setChanged();
        notifyUpdate();
    }

    public int getCurrentProgressIncrement() {
        if (spec == null || !tryInitSpec()) return 0;
        return (int) (spec.minesPerHour() * Math.abs(getSpeed()));
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

        DepositClaimerInstanceHolder.addClaimer(this, Objects.requireNonNull(level));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        DepositClaimerInstanceHolder.removeClaimer(this, Objects.requireNonNull(level));
        inventoryCap.invalidate();
        if (level.isClientSide()) DepositClaimerOutlineRenderer.removeClaimer(this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("inventory", inventory.serializeNBT());
        tag.put("claimer", serializeDepositBlockClaimer());
        if (process != null || tryInitProcess(false)) tag.put("process", process.getProgressAsNBT());
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (clientPacket)
            CreateRNS.LOGGER.trace("Client mining BE synced at {}, {}", worldPosition.getX(), worldPosition.getZ());

        inventory.deserializeNBT(tag.getCompound("inventory"));

        // Clear outline for the claimed mining area of this BE (client side)
        if (clientPacket) DepositClaimerOutlineRenderer.removeClaimer(this);

        // Deserialize claimed mining area
        deserializeDepositBlockClaimer(tag.getCompound("claimer"));

        // Add outline for the freshly deserialized claimed mining area back in (client side)
        if (clientPacket) DepositClaimerOutlineRenderer.addClaimer(this);

        // Schedule deserialization of the mining process
        if (tag.contains("process")) {
            pendingProcessTag = tag.getCompound("process");
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
        for (var bp : claimedDepositBlocks) {
            if (!level.isLoaded(bp)) return false;
        }

        process = new MiningProcess(level, spec.tier(), claimedDepositBlocks);

        // If we got mining progress data from NBT, now is the time to set it
        if (pendingProcessTag != null) {
            process.setProgressFromNBT(pendingProcessTag);
            pendingProcessTag = null;
        }

        return true;
    }
}
