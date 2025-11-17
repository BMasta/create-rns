package com.bmaster.createrns.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.deposit.DepositClaimerInstanceHolder;
import com.bmaster.createrns.deposit.DepositClaimerOutlineRenderer;
import com.bmaster.createrns.deposit.IDepositBlockClaimer;
import com.bmaster.createrns.deposit.capability.IDepositIndex;
import com.bmaster.createrns.mining.miner.MinerBlock;
import com.bmaster.createrns.mining.miner.MinerSpecLookup;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class MiningBehaviour extends BlockEntityBehaviour implements IDepositBlockClaimer {
    public static final BehaviourType<MiningBehaviour> TYPE = new BehaviourType<>(CreateRNS.MOD_ID + "mining");

    private final KineticBlockEntity kBE;
    private Set<BlockPos> claimedDepositBlocks = new HashSet<>();
    private MinerSpec spec = null;
    private MiningProcess process = null;

    private CompoundTag pendingProcessTag = null;

    public MiningBehaviour(MiningBlockEntity be) {
        super(be);
        kBE = be;
    }

    @Override
    public void initialize() {
        DepositClaimerInstanceHolder.addClaimer(this, getLevel());
    }

    @Override
    public void tick() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;
        if ((process == null && !tryInitProcess(false)) || !isMining()) return;

        process.advance(getCurrentProgressIncrement());
        var inv = kBE.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
        if (!(inv instanceof MiningEntityItemHandler mInv)) throw new IllegalStateException(
                "BE with this mining behavior does not have a mining item handler");
        mInv.collectMinedItems(process);
    }

    @Override
    public void unload() {
        var level = getLevel();
        DepositClaimerInstanceHolder.removeClaimer(this, level);

        kBE.invalidateCaps();
        if (level.isClientSide()) DepositClaimerOutlineRenderer.removeClaimer(this);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void write(CompoundTag nbt, boolean clientPacket) {
        super.write(nbt, clientPacket);
        nbt.put("claimer", serializeDepositBlockClaimer());
        if (process != null || tryInitProcess(false)) nbt.put("process", process.getProgressAsNBT());
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        super.read(nbt, clientPacket);

        // Clear outline for the claimed area of this BE (client side)
        if (clientPacket) DepositClaimerOutlineRenderer.removeClaimer(this);

        // Deserialize claimed area
        deserializeDepositBlockClaimer(nbt.getCompound("claimer"));

        // Add outline for the freshly deserialized claimed area back in (client side)
        if (clientPacket) DepositClaimerOutlineRenderer.addClaimer(this);

        // Schedule deserialization of the mining process
        if (nbt.contains("process")) {
            pendingProcessTag = nbt.getCompound("process");
        }

        // Recompute mining process yields based on claimed mining area. This also happens on process initialization.
        tryInitProcess(true);
    }

    public boolean isMining() {
        if ((process == null && !tryInitProcess(false))) return false;
        return process.isPossible() && kBE.isSpeedRequirementFulfilled();
    }

    public @Nullable MinerSpec getSpec() {
        if (spec == null && !tryInitSpec()) return null;
        return spec;
    }

    public @Nullable MiningProcess getProcess() {
        if (process == null && !tryInitProcess(false)) return null;
        return process;
    }

    @Override
    public ClaimingAreaSpec getClaimingAreaSpec() {
        var spec = getSpec();
        if (spec == null) throw new RuntimeException("Failed to get miner spec");
        return spec.miningArea();
    }

    @Override
    public Level getLevel() {
        return kBE.getLevel();
    }

    @Override
    public BlockPos getAnchor() {
        return getPos();
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
        var level = getLevel();
        if (level == null || (spec == null & !tryInitSpec())) return;

        claimedDepositBlocks = getClaimableDepositVein(level).stream()
                .filter(pos -> MiningRecipeLookup.isDepositMineable(level, level.getBlockState(pos).getBlock(),
                        spec.tier()))
                .collect(Collectors.toSet());

        // Recompute mining process based on claimed mining area
        tryInitProcess(true);

        // Initialize deposit durabilities as needed
        if (kBE.getLevel() instanceof ServerLevel sl) {
            var depIdx = IDepositIndex.fromLevel(sl);
            if (depIdx != null) {
                for (var bp : claimedDepositBlocks) {
                    depIdx.initDepositVeinDurability(bp);
                }
            }
        }

        kBE.setChanged();
        kBE.notifyUpdate();
    }

    public int getCurrentProgressIncrement() {
        if (spec == null || !tryInitSpec()) return 0;
        return (int) (spec.minesPerHour() * Math.abs(kBE.getSpeed()));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean tryInitSpec() {
        var level = getLevel();
        if (level == null) return false;
        spec = MinerSpecLookup.get(level.registryAccess(), (MinerBlock) kBE.getBlockState().getBlock());
        return true;
    }

    protected boolean tryInitProcess(boolean recompute) {
        if (process != null && !recompute) return true;
        var level = getLevel();
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
