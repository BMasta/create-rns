package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.RNSSoundEvents;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.mining.MinerEffectsGenerator;
import com.bmaster.createrns.content.deposit.mining.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.MinerEquipmentManager;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ContraptionMiningBehaviour extends MiningBehaviour {
    public final MinerBearingBlockEntity bearing;
    public @Nullable MinerEquipmentManager equipment;
    protected MinerEffectsGenerator effects;

    // Used by client to determine when a refresh is needed
    protected boolean wasAssembled = false;

    public ContraptionMiningBehaviour(MinerBearingBlockEntity bearing) {
        super(bearing, () -> bearing.getBlockState().getValue(DirectionalKineticBlock.FACING));
        this.bearing = bearing;
    }

    public boolean isMiningOrStalled() {
        var mc = bearing.getMovedContraption();
        return mc != null && bearing.isRunning() && super.isMining();
    }

    @Override
    public boolean isMining() {
        var mc = bearing.getMovedContraption();
        return isMiningOrStalled() && !mc.isStalled();
    }

    @Override
    public @Nullable BlockPos getAnchor() {
        if (equipment == null) return null;
        return equipment.drillHeadPos;
    }

    @Override
    public @Nullable Set<Catalyst> getCatalysts() {
        if (equipment == null) return null;
        return equipment.catalysts;
    }

    @Override
    public void initialize() {
        super.initialize();
        var level = getLevel();
        assert level != null;
        if (level.isClientSide) effects = new MinerEffectsGenerator(bearing);
    }

    @Override
    public void unload() {
        super.unload();
        var level = getLevel();
        assert level != null;
        if (level.isClientSide) effects.uninitialize();
    }

    @Override
    public void tick() {
        super.tick();
        var level = bearing.getLevel();
        assert level != null;
        if (level.isClientSide && isMining()) effects.tick();
    }

    @Override
    public void collect() {
        if ((process == null && !tryInitProcess(false)) ||
                (equipment == null && !refreshEquipment())) return;
        var spoils = process.collect();
        boolean collected = false;
        while (!spoils.isEmpty()) {
            collected = true;
            for (var s : spoils) {
                equipment.dropItem(s);
            }
            spoils = process.collect();
        }
        if (collected) {
            var level = getLevel();
            assert level != null;
            RNSSoundEvents.MINED.playServer(level, equipment.drillHeadPos);
        }
    }

    @Override
    public void claimDepositBlocks() {
        if (!bearing.isRunning()) return;
        super.claimDepositBlocks();
    }

    public void refresh() {
        // Make sure all items are collected before destroying existing process
        collect();

        // Release claimed blocks and reset equipment, spec, and process
        claimedDepositBlocks = null;
        equipment = null;
        spec = null;
        if (process != null) process.uninitialize();
        process = null;

        var ce = bearing.getMovedContraption();
        if (ce != null && bearing.isRunning()) {
            // We get the first opportunity to reclaim
            claimDepositBlocks();

            // Other claimers in the area get what is left
            var area = getClaimingBoundingBox();
            var level = getLevel();
            assert level != null;
            if (area != null) IDepositBlockClaimer.reclaimArea(level, area, getClaimerType());
        }
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        if (clientPacket) {
            var level = getLevel();
            if (level != null && level.isClientSide && effects != null) effects.refresh();
            refreshEquipment();
            tryInitSpec();
        }

        super.read(nbt, clientPacket);
    }

    protected boolean refreshEquipment() {
        var ce = bearing.getMovedContraption();
        var level = getLevel();
        assert level != null;
        if ((!level.isClientSide || wasAssembled) && (!bearing.isRunning() || ce == null)) {
            equipment = null;
            wasAssembled = false;
            return false;
        } else if ((!level.isClientSide || !wasAssembled) && bearing.isRunning() && ce != null) {
            equipment = MinerEquipmentManager.from((BearingContraption) ce.getContraption());
            wasAssembled = true;
            return true;
        }
        return equipment != null;
    }

    @Override
    protected boolean tryInitProcess(boolean refresh) {
        boolean needsInit = process == null || refresh;
        boolean initialized = super.tryInitProcess(refresh);
        var level = getLevel();
        if (level != null && level.isClientSide && needsInit && initialized) effects.refresh();
        return initialized;
    }

    @Override
    protected boolean tryInitSpec() {
        if (!bearing.isRunning() || (equipment == null && !refreshEquipment())) {
            spec = null;
            return false;
        }
        int radius = Math.max(0, ServerConfig.MINING_RADIUS.get() + equipment.propagatorCount - equipment.bufferCount);
        var area = new ClaimingArea(radius, ServerConfig.MINING_DEPTH.get());
        spec = new MinerSpec(area, ServerConfig.MINING_SPEED.get());

        return true;
    }
}
