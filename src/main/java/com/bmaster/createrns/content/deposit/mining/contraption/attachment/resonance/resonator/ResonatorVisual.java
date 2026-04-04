package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;

import java.util.function.Consumer;

public class ResonatorVisual extends AbstractBlockEntityVisual<ResonatorBlockEntity> {
    protected final TransformedInstance shard;

    public ResonatorVisual(VisualizationContext ctx, ResonatorBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        shard = instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(ResonatorRenderer.getShardModel(blockState, false)))
                .createInstance();

        ResonatorRenderer.applyLocalTransforms(shard.setIdentityTransform().translate(getVisualPosition()), blockState)
                .setChanged();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shard);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(shard);
    }

    @Override
    protected void _delete() {
        shard.delete();
    }
}
