package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.propagator;

import com.bmaster.createrns.RNSPartialModels;
import com.bmaster.createrns.RNSParticleTypes;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.ParticleEmittingMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
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
public class ResonancePropagatorMovementBehaviour extends ParticleEmittingMovementBehaviour {
    @Override
    public @Nullable ParticleOptions getParticle(MovementContext context) {
        return RNSParticleTypes.RESONANCE;
    }

    @Override
    public Vec3 getDisplacement(MovementContext context) {
        Direction facing = ResonancePropagatorBlock.getConnectedDirection(context.state);
        return new Vec3(facing.getStepX() * 0.40, facing.getStepY() * 0.40, facing.getStepZ() * 0.40);
    }

    @Override
    @OnlyIn(value = Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                    ContraptionMatrices matrices, MultiBufferSource buffer) {
        var pm = isActive(context) ? RNSPartialModels.PROPAGATOR_SHARD_ACTIVE : RNSPartialModels.PROPAGATOR_SHARD;
        ResonancePropagatorRenderer.renderInContraption(context, matrices, buffer, pm);
    }
}
