package com.bmaster.createrns.content.deposit.mining.contraption;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSSoundEvents;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.mining.MinerEffectsGenerator;
import com.bmaster.createrns.content.deposit.mining.behaviour.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.MinerEquipmentManager;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ContraptionMiningBehaviour extends MiningBehaviour {
    public static final BehaviourType<ContraptionMiningBehaviour> BEHAVIOUR_TYPE =
            new BehaviourType<>(CreateRNS.ID + ":contraption_mining");

    public final MinerBearingBlockEntity bearing;
    public @Nullable MinerEquipmentManager equipment;
    protected MinerEffectsGenerator effects;

    // Used by client to determine when a refresh is needed
    protected boolean wasAssembled = false;

    public ContraptionMiningBehaviour(MinerBearingBlockEntity bearing) {
        super(bearing, () -> bearing.getBlockState().getValue(DirectionalKineticBlock.FACING));
        this.bearing = bearing;
    }

    @Override
    public BehaviourType<?> getType() {
        return BEHAVIOUR_TYPE;
    }

    @Override
    public boolean isRunning() {
        var mc = bearing.getMovedContraption();
        return bearing.isRunning() && mc != null && !mc.isStalled();
    }

    @Override
    public boolean isMining() {
        var mc = bearing.getMovedContraption();
        return isMiningOrStalled() && !mc.isStalled();
    }

    public boolean isMiningOrStalled() {
        var mc = bearing.getMovedContraption();
        return mc != null && bearing.isRunning() && super.isMining();
    }

    @Override
    public @Nullable BlockPos getOperatingStart() {
        if (equipment == null && !refreshEquipment()) return null;
        return equipment.mineHeadTipPos.relative(getOperatingDirection());
    }

    @Override
    public @Nullable Set<Catalyst> getCatalysts() {
        if (equipment == null && !refreshEquipment()) return null;
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
        if (level.isClientSide && effects != null) effects.uninitialize();
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
        if (process == null || (equipment == null && !refreshEquipment())) return;
        var spoils = process.collect();
        for (var s : spoils) equipment.dropItem(s);
        if (!spoils.isEmpty()) {
            var level = getLevel();
            assert level != null;
            RNSSoundEvents.MINED.playServer(level, equipment.mineHeadTipPos);
        }
    }

    @Override
    public @Nullable MinerSpec getSpec() {
        if (equipment == null && !refreshEquipment()) return null;

        var size = equipment.mineHeadSize;

        int radius = Math.max(0, ServerConfig.MINING_RADIUS.get() + size.claimBonus);
        int depth = ServerConfig.MINING_DEPTH.get();

        var operatingDims = new OperatingDimensions(radius, depth);

        spec = new MinerSpec(operatingDims, ServerConfig.MINING_SPEED.get());

        return spec;
    }

    public void refresh() {
        var level = getLevel();
        assert level != null;
        if (level.isClientSide) return;

        // Make sure all items are collected before destroying existing process
        collect();

        if (isRunning()) {
            claimDepositBlocks();
        } else {
            // Get area before resetting the miner state
            var area = getOperatingBoundingBox();

            // Reset miner state
            setClaimedDepositBlocks(Set.of());
            equipment = null;

            // Let other miners reclaim the cleared area
            if (area != null) IDepositBlockClaimer.reclaimArea(area, this);
        }
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        if (clientPacket) {
            refreshEquipment();
        }

        super.read(nbt, clientPacket);

        var level = getLevel();
        if (clientPacket && level != null && level.isClientSide && effects != null) effects.refresh();
    }

    protected boolean refreshEquipment() {
        var ce = bearing.getMovedContraption();
        var level = getLevel();
        if (level == null) return false;
        if ((!level.isClientSide || wasAssembled) && (!bearing.isRunning() || ce == null)) {
            equipment = null;
            wasAssembled = false;
            return false;
        } else if ((!level.isClientSide || !wasAssembled) && bearing.isRunning() && ce != null) {
            try {
                equipment = MinerEquipmentManager.from((BearingContraption) ce.getContraption());
                wasAssembled = true;
                return true;
            } catch (IllegalStateException e) {
                CreateRNS.LOGGER.error("Failed to initialize miner equipment for miner at {},{},{}",
                        getPos().getX(), getPos().getY(), getPos().getZ());
                return false;
            }
        }
        return equipment != null;
    }

    @Override
    protected boolean tryReInitProcess() {
        boolean initialized = super.tryReInitProcess();
        var level = getLevel();
        if (level != null && level.isClientSide && initialized && effects != null) effects.refresh();
        return initialized;
    }
}
