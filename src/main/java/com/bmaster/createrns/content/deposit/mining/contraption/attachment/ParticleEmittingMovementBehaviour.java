package com.bmaster.createrns.content.deposit.mining.contraption.attachment;

import com.bmaster.createrns.content.deposit.mining.contraption.MinerBearingBlockEntity;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public abstract class ParticleEmittingMovementBehaviour implements MovementBehaviour {

    public abstract @Nullable ParticleOptions getParticle(MovementContext context);

    public abstract Vec3 getDisplacement(MovementContext context);

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    public void tick(MovementContext context) {
        if (!context.world.isClientSide) return;
        if (!isActive(context) || !(context.contraption instanceof BearingContraption bc)) return;
        var bearing = getBearing(context);
        if (bearing == null) return;
        var equipment = bearing.miningBehaviour.equipment;
        if (equipment == null) return;

        float rotationRatio = Math.abs(bearing.getSpeed() / AllConfigs.server().kinetics.maxRotationSpeed.get());
        float particleChance = 0.015f + (bc.stalled ? 0 : rotationRatio) * 0.395f;
        spawnParticle(context, particleChance);
    }

    @OnlyIn(value = Dist.CLIENT)
    public boolean spawnParticle(MovementContext context, float chance) {
        var particle = getParticle(context);
        if (particle == null) return false;
        if (!(context.contraption instanceof BearingContraption bc)) return false;
        if (context.world.random.nextFloat() >= chance) return false;

        var displacement = getDisplacement(context);
        var local = Vec3.atCenterOf(context.localPos).add(displacement);
        var worldPos = bc.entity.toGlobalVector(local, AnimationTickHolder.getPartialTicks());
        context.world.addParticle(particle,
                worldPos.x,
                worldPos.y - 1,
                worldPos.z,
                0, 0, 0);
        return true;
    }

    protected @Nullable MinerBearingBlockEntity getBearing(MovementContext context) {
        var c = ((BearingContraption) context.contraption);
        var pos = c.anchor.relative(c.getFacing().getOpposite());
        var be = context.world.getBlockEntity(pos);
        if (be instanceof MinerBearingBlockEntity mbe) return mbe;
        else return null;
    }
}
