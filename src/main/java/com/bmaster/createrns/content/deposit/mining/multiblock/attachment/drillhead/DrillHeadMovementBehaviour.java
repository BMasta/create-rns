package com.bmaster.createrns.content.deposit.mining.multiblock.attachment.drillhead;

import com.bmaster.createrns.RNSParticleTypes;
import com.bmaster.createrns.content.deposit.mining.multiblock.MinerBearingBlockEntity;
import com.bmaster.createrns.util.Utils;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class DrillHeadMovementBehaviour implements MovementBehaviour {

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
        var equipment = bearing.getEquipmentManager();
        if (equipment == null) return;

        float rotationRatio = Math.abs(bearing.getSpeed() / AllConfigs.server().kinetics.maxRotationSpeed.get());
        float particleChance = 0.015f + (bc.stalled ? 0 : rotationRatio) * 0.395f;
        spawnParticle(context, particleChance);
    }

    @OnlyIn(value = Dist.CLIENT)
    public void spawnParticle(MovementContext context, float chance) {
        if (!(context.contraption instanceof BearingContraption bc)) return;
        if (!(context.state.getBlock() instanceof DrillHeadBlock)) return;
        if (context.world.random.nextFloat() < chance) {
            var facing = DrillHeadBlock.getConnectedDirection(context.state);
            var flippedNormal = Utils.normalVecFlip(facing, true);
            var randomDisplacement = new Vec3(
                    flippedNormal.getX() * 0.3 * (context.world.random.nextBoolean() ? 1 : -1),
                    flippedNormal.getY() * 0.3 * (context.world.random.nextBoolean() ? 1 : -1),
                    flippedNormal.getZ() * 0.3 * (context.world.random.nextBoolean() ? 1 : -1)
            );
            var local = Vec3.atCenterOf(context.localPos).add(randomDisplacement);
            var worldPos = bc.entity.toGlobalVector(local, AnimationTickHolder.getPartialTicks());
            context.world.addParticle(getParticle(),
                    worldPos.x + facing.getStepX() * 0.30,
                    worldPos.y + facing.getStepY() * 0.30 - 1,
                    worldPos.z + facing.getStepZ() * 0.30,
                    0, 0, 0);
        }
    }

    protected @Nullable MinerBearingBlockEntity getBearing(MovementContext context) {
        var c = ((BearingContraption) context.contraption);
        var pos = c.anchor.relative(c.getFacing().getOpposite());
        var be = context.world.getBlockEntity(pos);
        if (be instanceof MinerBearingBlockEntity mbe) return mbe;
        else return null;
    }

    protected ParticleOptions getParticle() {
        return RNSParticleTypes.RESONANCE;
    }
}
