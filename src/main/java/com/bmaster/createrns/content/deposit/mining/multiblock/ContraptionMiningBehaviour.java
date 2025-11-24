package com.bmaster.createrns.content.deposit.mining.multiblock;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.mining.MinerSpec;
import com.bmaster.createrns.content.deposit.mining.MinerSpecLookup;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.bmaster.createrns.content.deposit.mining.multiblock.equipment.EquipmentManager;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ContraptionMiningBehaviour extends MiningBehaviour {
    public final MinerBearingBlockEntity bearing;
    public EquipmentManager equipment;
    protected MinerSpec baseSpec = null;

    public ContraptionMiningBehaviour(MinerBearingBlockEntity bearing) {
        super(bearing, () -> bearing.getBlockState().getValue(DirectionalKineticBlock.FACING));
        this.bearing = bearing;
    }

    @Override
    public boolean isMining() {
        var mc = bearing.getMovedContraption();
        return mc != null && bearing.isRunning() && !mc.isStalled() && super.isMining();
    }

    @Override
    public int getByproductChanceStacks() {
        return equipment != null ? equipment.collectorCount : -1;
    }

    @Override
    public void collect() {
        if ((process == null && !tryInitProcess(false)) ||
                (equipment == null && !refreshEquipment())) return;
        for (var s : process.collect()) {
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

        // Release claimed blocks and reset process and spec
        claimedDepositBlocks.clear();
        pendingSync.claimer = true;
        equipment = null;
        spec = null;
        process = null;

        var ce = bearing.getMovedContraption();
        if (ce != null && bearing.isRunning()) {
            equipment = EquipmentManager.from(ce.getContraption());

            // We get the first opportunity to reclaim
            claimDepositBlocks();

            // Other claimers in the area get what is left
            var area = getClaimingBoundingBox();
            if (area != null) IDepositBlockClaimer.reclaimArea(getLevel(), area, getClaimerType());
        }
    }

    @Override
    public void read(CompoundTag nbt, boolean clientPacket) {
        if (clientPacket) {
            refreshEquipment();
            refreshSpec();
        }

        super.read(nbt, clientPacket);
    }

    protected boolean refreshEquipment() {
        var ce = bearing.getMovedContraption();
        if (ce == null || !bearing.isRunning()) {
            equipment = null;
            return false;
        } else {
            equipment = EquipmentManager.from(ce.getContraption());
            return true;
        }
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

        int tier = baseSpec.tier() + equipment.resonatorCount;
        int offset = baseSpec.miningArea().offset() + headOffset;

        spec = new MinerSpec(RNSContent.MINER_BEARING_BLOCK.get(), tier, baseSpec.minesPerHour(),
                new ClaimingAreaSpec(baseSpec.miningArea().radius(), baseSpec.miningArea().length(), offset));
        return true;
    }
}
