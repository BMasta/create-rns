package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator;

import com.bmaster.createrns.content.deposit.mining.contraption.attachment.ParticleEmittingMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ResonatorMovementBehaviour extends ParticleEmittingMovementBehaviour {
    @Override
    public ParticleOptions getParticle(MovementContext context) {
        return ((AbstractResonatorBlock) context.state.getBlock()).getParticle();
    }

    @Override
    public Vec3 getDisplacement(MovementContext context) {
        Direction facing = ResonatorBlock.getConnectedDirection(context.state);
        return new Vec3(facing.getStepX() * 0.40, facing.getStepY() * 0.40, facing.getStepZ() * 0.40);
    }

    @Override
    public @Nullable ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
        return new ResonatorActorVisual(visualizationContext, simulationWorld, movementContext);
    }

    @Override
    @OnlyIn(value = Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                    ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (VisualizationManager.supportsVisualization(context.world)) return;
        ResonatorRenderer.renderInContraption(context, matrices, buffer, isActive(context));
    }
}
