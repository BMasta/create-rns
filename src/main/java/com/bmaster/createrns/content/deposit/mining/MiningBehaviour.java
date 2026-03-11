package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerInstanceHolder;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerOutlineRenderer;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class MiningBehaviour extends BlockEntityBehaviour implements IDepositBlockClaimer {
    public static final BehaviourType<MiningBehaviour> BEHAVIOUR_TYPE = new BehaviourType<>(CreateRNS.ID + ":mining");
    public static final ClaimerType CLAIMER_TYPE = new ClaimerType(CreateRNS.ID + ":mining");

    protected final KineticBlockEntity kBE;
    protected final Supplier<Direction> claimingDirection;
    protected @Nullable Set<BlockPos> claimedDepositBlocks = null;
    protected MinerSpec spec = null;
    protected MiningProcess process = null;

    // Used by client to defer process sync until it is initialized
    protected Tuple<CompoundTag, Boolean> pendingProcessTag = null;

    public MiningBehaviour(KineticBlockEntity be, Supplier<Direction> claimingDirection) {
        super(be);
        kBE = be;
        this.claimingDirection = claimingDirection;
    }

    public abstract void collect();

    protected abstract boolean tryInitSpec();

    @Override
    public void initialize() {
        var level = getLevel();
        assert level != null;

        DepositClaimerInstanceHolder.addClaimer(this, level);
    }

    @Override
    public void tick() {
        var level = getLevel();
        if (level == null || level.isClientSide) return;
        if ((process == null && !tryInitProcess(false)) || !isMining()) return;

        process.advance(getCurrentProgressIncrement());
        collect();
    }

    @Override
    public void unload() {
        var level = getLevel();
        assert level != null;

        DepositClaimerInstanceHolder.removeClaimer(this, level);

        level.invalidateCapabilities(getPos());
        if (level.isClientSide()) DepositClaimerOutlineRenderer.removeClaimer(this);
    }

    @Override
    public BehaviourType<?> getType() {
        return BEHAVIOUR_TYPE;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
        super.write(nbt, provider, clientPacket);
        nbt.put("claimer", serializeDepositBlockClaimer(provider));
        if (process != null || tryInitProcess(false)) {
            var processNBT = process.write(provider, clientPacket);
            if (processNBT != null) nbt.put("process", processNBT);
        }
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
        super.read(nbt, provider, clientPacket);

        if (nbt.get("claimer") instanceof CompoundTag claimerTag) {
            deserializeDepositBlockClaimer(provider, claimerTag);
        }

        if (nbt.contains("process")) {
            var processTag = nbt.getCompound("process");
            if (process != null) process.read(processTag, provider, clientPacket);
            else pendingProcessTag = new Tuple<>(processTag, clientPacket);
        }
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
    public ClaimingMode getClaimingMode() {
        return ClaimingMode.EXCLUSIVE;
    }

    @Override
    public ClaimerType getClaimerType() {
        return CLAIMER_TYPE;
    }

    @Override
    public Direction getClaimingDirection() {
        return claimingDirection.get();
    }

    @Override
    public @Nullable ClaimingArea getClaimingArea() {
        var spec = getSpec();
        if (spec == null) return null;
        return spec.miningArea();
    }

    @Override
    public @Nullable Level getLevel() {
        return kBE.getLevel();
    }

    @Override
    public @Nullable Set<BlockPos> getClaimedDepositBlocks() {
        return claimedDepositBlocks;
    }

    @Override
    public void setClaimedDepositBlocks(@Nullable Set<BlockPos> claimedBlocks) {
        claimedDepositBlocks = claimedBlocks;

        // Recompute mining process based on claimed mining area
        tryInitProcess(true);

        var level = getLevel();
        if (level != null && !level.isClientSide) {
            kBE.notifyUpdate();
        }

        var pos = getPos();
        CreateRNS.LOGGER.trace("Synced area of miner at {}, {}, {}", pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void claimDepositBlocks() {
        var level = getLevel();
        if (level == null || level.isClientSide || (spec == null & !tryInitSpec())) return;
        var catalysts = getCatalysts();
        if (catalysts == null) return;

        claimedDepositBlocks = getClaimableDepositVein(level).stream()
                .filter(pos -> MiningRecipeLookup.isDepositMineable(level, level.getBlockState(pos).getBlock(), catalysts))
                .collect(Collectors.toSet());

        // Recompute mining process based on claimed mining area
        tryInitProcess(true);

        // Initialize deposit durabilities as needed
        for (var bp : claimedDepositBlocks) {
            DepositDurabilityManager.initDepositVeinDurability((ServerLevel) level, bp);
        }

        var pos = getPos();
        CreateRNS.LOGGER.trace("Miner at {}, {}, {} claimed {} deposit blocks", pos.getX(), pos.getY(), pos.getZ(),
                claimedDepositBlocks.size());

        kBE.notifyUpdate();
    }

    public int getCurrentProgressIncrement() {
        if (spec == null || !tryInitSpec()) return 0;
        return (int) (spec.miningSpeed * Math.abs(kBE.getSpeed()));
    }

    public @Nullable Set<Catalyst> getCatalysts() {
        return new ObjectOpenHashSet<>();
    }

    protected boolean tryInitProcess(boolean refresh) {
        if (process != null && !refresh) return true;
        var level = getLevel();
        if (level == null || (spec == null && !tryInitSpec()) || claimedDepositBlocks == null) return false;
        for (var bp : claimedDepositBlocks) {
            if (!level.isLoaded(bp)) return false;
        }
        var catalysts = getCatalysts();
        if (catalysts == null) return false;

        process = new MiningProcess(level, catalysts, claimedDepositBlocks);

        // If we got mining progress data from NBT, now is the time to set it
        if (pendingProcessTag != null) {
            process.read(pendingProcessTag.getA(), level.registryAccess(), pendingProcessTag.getB());
            pendingProcessTag = null;
        }

        return true;
    }

    public record MinerSpec(ClaimingArea miningArea, float miningSpeed) {}
}
