package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerInstanceHolder;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerOutlineRenderer;
import com.bmaster.createrns.content.deposit.info.DepositDurabilityManager;
import com.bmaster.createrns.content.deposit.mining.recipe.MiningRecipeLookup;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.content.deposit.operating.HybridOperatingBehaviour;
import com.bmaster.createrns.content.deposit.operating.IDepositBlockOperator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.function.Supplier;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class MiningBehaviour extends HybridOperatingBehaviour {
    public static final BehaviourType<MiningBehaviour> BEHAVIOUR_TYPE = new BehaviourType<>(CreateRNS.ID + ":mining");
    public static final ClaimerType CLAIMER_TYPE = new ClaimerType(CreateRNS.ID + ":mining");

    protected final KineticBlockEntity kBE;
    protected @Nullable MinerSpec spec = null;
    protected @Nullable MiningProcess process = null;

    // Defers disk or client process state until the process can be initialized.
    protected @Nullable Tuple<CompoundTag, Boolean> pendingProcessTag = null;

    private int recipeVersion = 0;

    public MiningBehaviour(KineticBlockEntity be, Supplier<Direction> operatingDirection) {
        super(be, operatingDirection);
        this.kBE = be;
    }

    public abstract void collect();

    protected abstract boolean refreshSpec();

    @Override
    public void initialize() {
        var level = getLevel();
        assert level != null;

        this.recipeVersion = MiningRecipeLookup.version(level.isClientSide);
        DepositClaimerInstanceHolder.addClaimer(this, level);
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

        if (!tryInitProcess() || level.isClientSide || !isMining()) return;

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

        if (tryInitProcess()) {
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
            unInitProcess();
        }
    }

    public boolean isMining() {
        if (!tryInitProcess()) return false;
        return process.isPossible() && kBE.isSpeedRequirementFulfilled();
    }

    public @Nullable MinerSpec getSpec() {
        if (spec == null && !refreshSpec()) return null;
        return spec;
    }

    public @Nullable MiningProcess getProcess() {
        if (!tryInitProcess()) return null;
        return process;
    }

    @Override
    protected boolean isDepositBlockOperable(BlockPos pos) {
        var level = getLevel();
        if (level == null || level.isClientSide || (spec == null && !refreshSpec())) return false;
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
    public ClaimerType getClaimerType() {
        return CLAIMER_TYPE;
    }

    @Override
    protected void onOperatingSelectionChanged() {
        unInitProcess();
        var level = getLevel();
        if (level == null || level.isClientSide || operatingSelection == null) return;
        for (var bp : operatingSelection.positions) {
            DepositDurabilityManager.initDepositVeinDurability((ServerLevel) level, bp);
        }
    }

    public int getCurrentProgressIncrement() {
        if (spec == null || !refreshSpec()) return 0;
        return (int) (spec.miningSpeed * Math.abs(kBE.getSpeed()));
    }

    public @Nullable Set<Catalyst> getCatalysts() {
        return new ObjectOpenHashSet<>();
    }

    protected boolean tryInitProcess() {
        if (process != null) return true;
        return tryReInitProcess();
    }

    protected boolean tryReInitProcess() {
        unInitProcess();
        var level = getLevel();
        if (level == null || (spec == null && !refreshSpec()) || !tryInitOperatingSelection()) return false;
        for (var bp : operatingSelection.positions) {
            if (!level.isLoaded(bp)) return false;
        }
        var catalysts = getCatalysts();
        if (catalysts == null) return false;

        process = new MiningProcess(level, catalysts, operatingSelection.positions);

        // Deserialize process state from the pending tag
        if (pendingProcessTag != null) {
            process.read(pendingProcessTag.getA(), level.registryAccess(), pendingProcessTag.getB());
            pendingProcessTag = null;
        }

        return true;
    }

    protected void unInitProcess() {
        if (process != null) process.uninitialize();
        process = null;
    }

    public record MinerSpec(OperatingDimensions miningDimensions, double miningSpeed) {}

}
