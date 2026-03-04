package com.bmaster.createrns.content.deposit.mining.multiblock;

import com.bmaster.createrns.RNSBlocks;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.mining.MinerSpec;
import com.bmaster.createrns.content.deposit.mining.MinerSpecLookup;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.EquipmentManager;
import com.bmaster.createrns.content.deposit.mining.recipe.catalyst.Catalyst;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ContraptionMiningBehaviour extends MiningBehaviour {
    public final MinerBearingBlockEntity bearing;
    public EquipmentManager equipment;
    protected MinerSpec baseSpec = null;

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
    public @Nullable Set<Catalyst> getCatalysts() {
        if (equipment == null) return null;
        return equipment.catalysts;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void collect() {
        if ((process == null && !tryInitProcess(false)) ||
                (equipment == null && !refreshEquipment())) return;
        var spoils = process.collect();
        for (var s : spoils) {
            equipment.dropItem(s);
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
        claimedDepositBlocks.clear();
        pendingSync.claimer = true;
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
            if (area != null) IDepositBlockClaimer.reclaimArea(getLevel(), area, getClaimerType());
        }
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider provider, boolean clientPacket) {
        if (clientPacket) {
            refreshEquipment();
            refreshSpec();
        }

        super.read(nbt, provider, clientPacket);
    }

    protected boolean refreshEquipment() {
        var ce = bearing.getMovedContraption();
        if ((!getLevel().isClientSide || wasAssembled) && (!bearing.isRunning() || ce == null)) {
            equipment = null;
            wasAssembled = false;
            return false;
        } else if ((!getLevel().isClientSide || !wasAssembled) && bearing.isRunning() && ce != null) {
            equipment = EquipmentManager.from((BearingContraption) ce.getContraption());
            wasAssembled = true;
            return true;
        }
        return equipment != null;
    }

    @Override
    protected boolean refreshSpec() {
        if (!bearing.isRunning() || (equipment == null && !refreshEquipment())) {
            spec = null;
            return false;
        }

        var headLocalPos = equipment.drillHeadPos.subtract(getPos());
        var headOffset = Math.abs(headLocalPos.getX()) + Math.abs(headLocalPos.getY()) + Math.abs(headLocalPos.getZ());

        // Base spec is provided by datapacks
        if (baseSpec == null) {
            var level = getLevel();
            if (level == null) {
                spec = null;
                return false;
            }
            baseSpec = MinerSpecLookup.get(level.registryAccess(), bearing.getBlockState().getBlock());
        }

        int tier = baseSpec.tier();
        int offset = baseSpec.miningArea().offset() + headOffset;

        spec = new MinerSpec(RNSBlocks.MINER_BEARING_BLOCK.get(), tier, baseSpec.minesPerHour(),
                new ClaimingAreaSpec(baseSpec.miningArea().radius(), baseSpec.miningArea().length(), offset));
        return true;
    }
}
