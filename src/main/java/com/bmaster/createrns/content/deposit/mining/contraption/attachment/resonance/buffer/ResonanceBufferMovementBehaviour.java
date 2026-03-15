package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer;

import com.bmaster.createrns.RNSPartialModels;
import com.bmaster.createrns.RNSParticleTypes;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.ParticleEmittingMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ResonanceBufferMovementBehaviour extends ParticleEmittingMovementBehaviour {
    @Override
    public ParticleOptions getParticle(MovementContext context) {
        return RNSParticleTypes.RESONANCE;
    }

    @Override
    public Vec3 getDisplacement(MovementContext context) {
        int displaceX = context.world.random.nextIntBetweenInclusive(-1, 1);
        int displaceY = context.world.random.nextIntBetweenInclusive(-1, 1);
        int displaceZ = context.world.random.nextIntBetweenInclusive(-1, 1);
        return new Vec3(
                0.5f * displaceX,
                0.1f * displaceY,
                0.5f * displaceZ
        );
    }

    @Override
    @OnlyIn(value = Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                    ContraptionMatrices matrices, MultiBufferSource buffer) {
        ResonanceBufferRenderer.renderInContraption(context, matrices, buffer, isActive(context)
                ? RNSPartialModels.RESONANCE_BUFFER_SHARD_ACTIVE
                : RNSPartialModels.RESONANCE_BUFFER_SHARD);
    }
}
