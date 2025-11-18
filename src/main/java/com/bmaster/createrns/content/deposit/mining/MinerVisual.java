package com.bmaster.createrns.content.deposit.mining;

import com.bmaster.createrns.RNSContent;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;

public class MinerVisual extends SingleAxisRotatingVisual<MinerBlockEntity> {
    public MinerVisual(VisualizationContext context, MinerBlockEntity be, float partialTick) {
        super(context, be, partialTick, Models.partial(
                (be.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE).getSpec() == null || be.getBehaviour(MiningBehaviour.BEHAVIOUR_TYPE).getSpec().tier() <= 1)
                        ? RNSContent.MINER_MK1_DRILL : RNSContent.MINER_MK2_DRILL));
    }
}
