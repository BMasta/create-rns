package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.client.renderer.LightTexture;

public class ResonatorActorVisual extends ActorVisual {
    protected final TransformedInstance inactiveShard;
    protected final TransformedInstance activeShard;

    public ResonatorActorVisual(
            VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext context
    ) {
        super(visualizationContext, simulationWorld, context);

        inactiveShard = instancerProvider.instancer(
                InstanceTypes.TRANSFORMED, Models.partial(ResonatorRenderer.getShardModel(context.state, false))
        ).createInstance();
        activeShard = instancerProvider.instancer(
                InstanceTypes.TRANSFORMED, Models.partial(ResonatorRenderer.getShardModel(context.state, true))
        ).createInstance();

        activeShard.light(LightTexture.FULL_BRIGHT);
    }

    @Override
    public void beginFrame() {
        updateTransform(inactiveShard, context.disabled);
        updateTransform(activeShard, !context.disabled);
    }

    protected void updateTransform(TransformedInstance instance, boolean visible) {
        if (!visible) {
            instance.setZeroTransform().setChanged();
            return;
        }

        ResonatorRenderer.applyLocalTransforms(instance.setIdentityTransform().translate(context.localPos), context.state)
                .setChanged();
    }

    @Override
    protected void _delete() {
        inactiveShard.delete();
        activeShard.delete();
    }
}
