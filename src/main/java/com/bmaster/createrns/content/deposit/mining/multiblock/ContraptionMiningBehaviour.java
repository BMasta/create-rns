package com.bmaster.createrns.content.deposit.mining.multiblock;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.content.deposit.claiming.IDepositBlockClaimer;
import com.bmaster.createrns.content.deposit.mining.MinerSpec;
import com.bmaster.createrns.content.deposit.mining.MinerSpecLookup;
import com.bmaster.createrns.content.deposit.mining.block.MiningBehaviour;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ContraptionMiningBehaviour extends MiningBehaviour {
    protected final MinerBearingBlockEntity bearing;
    protected MinerSpec baseSpec = null;

    public ContraptionMiningBehaviour(MinerBearingBlockEntity bearing) {
        super(bearing, () -> bearing.getBlockState().getValue(DirectionalKineticBlock.FACING));
        this.bearing = bearing;
    }

    public @Nullable Pair<StructureBlockInfo, MovementContext> getDrillHeadActor() {
        var ce = bearing.getMovedContraption();
        if (ce == null) return null;
        var c = ce.getContraption();
        if (c == null) return null;
        for (var a : c.getActors()) {
            if (a.left.state().is(RNSContent.DRILL_HEAD_BLOCK.get())) {
                return new Pair<>(a.left, a.right);
            }
        }
        return null;
    }

    public @Nullable BlockPos getDrillHeadAbsPos() {
        var actor = getDrillHeadActor();
        if (actor == null) return null;
        return actor.getFirst().pos().relative(claimingDirection.get()).offset(bearing.getBlockPos());
    }

    @Override
    public boolean isMining() {
        var mc = bearing.getMovedContraption();
        return mc != null && bearing.isRunning() && !mc.isStalled() && super.isMining();
    }

    @Override
    public void collect() {
        var drillActor = getDrillHeadActor();
        if (process == null || drillActor == null) return;
        for (var s : process.collect()) {
            DrillHeadMovementBehaviour.dropItemStatic(drillActor.getSecond(), s);
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
        spec = null;
        process = null;

        // We get the first opportunity to reclaim
        claimDepositBlocks();

        // Other claimers in the area get what is left
        var area = getClaimingBoundingBox();
        if (area != null) IDepositBlockClaimer.reclaimArea(getLevel(), area, getClaimerType());
    }

    @Override
    protected boolean tryInitSpec() {
        if (!bearing.isRunning()) return false;

        var drillActor = getDrillHeadActor();
        if (drillActor == null) return false;
        var headPos = drillActor.getFirst().pos();
        var offset = Math.abs(headPos.getX()) + Math.abs(headPos.getY()) + Math.abs(headPos.getZ()) + 2;

        if (baseSpec == null) {
            var level = getLevel();
            if (level == null) return false;
            baseSpec = MinerSpecLookup.get(level.registryAccess(), bearing.getBlockState().getBlock());
        }
        spec = new MinerSpec(RNSContent.MINER_BEARING_BLOCK.get(), baseSpec.tier(), baseSpec.minesPerHour(),
                new ClaimingAreaSpec(baseSpec.miningArea().radius(), baseSpec.miningArea().length(), offset));
        return true;
    }
}
