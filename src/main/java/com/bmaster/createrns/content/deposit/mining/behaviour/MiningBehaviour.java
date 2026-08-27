package com.bmaster.createrns.content.deposit.mining.behaviour;

import com.bmaster.createrns.content.deposit.claiming.ClaimingBehaviour;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.mining.IDepositBlockMiner;
import com.bmaster.createrns.content.deposit.mining.MiningProcess;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class MiningBehaviour extends ClaimingBehaviour implements IDepositBlockMiner {
    protected final KineticBlockEntity kBE;
    protected @Nullable MinerSpec spec = null;
    protected @Nullable MiningProcess process = null;

    // When claimed area changes, the progress percentage towards mining a block is saved here.
    // This way, no matter how the area changes, the progress does not get reset until the miner is destroyed or unloaded.
    protected Object2FloatOpenHashMap<Block> savedProgress = new Object2FloatOpenHashMap<>();

    // Defers disk or client process state until the process can be initialized.
    protected @Nullable Tuple<CompoundTag, Boolean> pendingProcessTag = null;

    private int recipeVersion = 0;

    public MiningBehaviour(KineticBlockEntity be, Supplier<Direction> operatingDirection) {
        super(be, operatingDirection);
        this.kBE = be;
    }

    public abstract void collect();

    @Override
    public void initialize() {
        super.initialize();

        var level = getLevel();
        assert level != null;

        this.recipeVersion = MiningRecipeLookup.version(level.isClientSide);
    }

    @Override
    public void claimDepositBlocks() {
        var level = getLevel();
        if (level != null && !level.isClientSide) collect();
        super.claimDepositBlocks();
    }

    @Override
    public void claimCrossSublevelDepositBlocks() {
        var level = getLevel();
        if (level != null && !level.isClientSide) collect();
        super.claimCrossSublevelDepositBlocks();
    }

    @Override
    public void setClaimedDepositBlocks(@Nullable Set<BlockPos> claimedBlocks, boolean crossSublevel) {
        var level = getLevel();
        if (level != null && !level.isClientSide) collect();
        super.setClaimedDepositBlocks(claimedBlocks, crossSublevel);
    }

    @Override
    public void tick() {
        super.tick();

        var level = getLevel();
        if (level == null) return;

        // Act on /reload
        int latestRecipeVersion = MiningRecipeLookup.version(level.isClientSide);
        if (recipeVersion != latestRecipeVersion) {
            recipeVersion = latestRecipeVersion;
            unInitProcess();
            claimDepositBlocks();
        }

        if ((process == null && !tryInitProcess()) || level.isClientSide || !isMining()) return;

        process.advance(getCurrentProgressIncrement());
        collect();
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
        super.write(nbt, provider, clientPacket);

        if (process != null || tryInitProcess()) {
            var processNBT = process.write(provider, clientPacket);
            if (processNBT != null) nbt.put("process", processNBT);
        }
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
        super.read(nbt, provider, clientPacket);

        pendingProcessTag = null;
        if (nbt.contains("process")) {
            pendingProcessTag = new Tuple<>(nbt.getCompound("process"), clientPacket);
        }
    }

    @Override
    public boolean isMining() {
        if (process == null && !tryInitProcess()) return false;
        return process.isPossible() && kBE.isSpeedRequirementFulfilled();
    }

    @Override
    public @Nullable MiningProcess getProcess() {
        if (!tryInitProcess()) return null;
        return process;
    }

    @Override
    protected boolean isDepositBlockOperable(BlockPos pos) {
        var level = getLevel();
        if (level == null || level.isClientSide) return false;
        var catalysts = getCatalysts();
        if (catalysts == null) return false;

        return MiningRecipeLookup.isDepositMineable(level, level.getBlockState(pos).getBlock(), catalysts);
    }

    @Override
    public @Nullable IDepositBlockOperator.OperatingDimensions getOperatingDimensions() {
        var spec = getSpec();
        if (spec == null) return null;
        return spec.miningDimensions();
    }

    @Override
    public @Nullable IDepositBlockOperator.DetectionDimensions getDetectionDimensions() {
        var spec = getSpec();
        if (spec == null) return null;
        return spec.crossSublevelMiningDimensions();
    }

    @Override
    protected void onClaimedBlocksChanged() {
        super.onClaimedBlocksChanged();
        unInitProcess();
        var level = getLevel();
        if (level == null || level.isClientSide || claimedDepositBlocks == null) return;
        for (var bp : claimedDepositBlocks) {
            DepositDurabilityManager.initVein((ServerLevel) level, bp, true);
        }
    }

    @Override
    public int getCurrentProgressIncrement() {
        var spec = getSpec();
        if (spec == null) return 0;
        return (int) (spec.miningSpeed() * Math.abs(kBE.getSpeed()));
    }

    protected boolean tryInitProcess() {
        if (process != null) {
            var level = getLevel();
            if (level != null && pendingClaimerTag == null && pendingProcessTag != null) {
                process.read(pendingProcessTag.getA(), level.registryAccess(), pendingProcessTag.getB());
                pendingProcessTag = null;
            }
            return true;
        }
        return tryReInitProcess();
    }

    protected boolean tryReInitProcess() {
        if (pendingClaimerTag != null) return false;
        unInitProcess();
        var level = getLevel();
        if (level == null || claimedDepositBlocks == null) return false;
        for (var bp : claimedDepositBlocks) {
            if (!level.isLoaded(bp)) return false;
        }
        var catalysts = getCatalysts();
        if (catalysts == null) return false;

        process = new MiningProcess(level, catalysts, claimedDepositBlocks, savedProgress);

        // Deserialize process state from the pending tag
        if (pendingProcessTag != null) {
            process.read(pendingProcessTag.getA(), level.registryAccess(), pendingProcessTag.getB());
            pendingProcessTag = null;
        }

        return true;
    }

    protected void unInitProcess() {
        if (process != null) {
            savedProgress.putAll(process.uninitialize());
        }
        process = null;
    }
}
