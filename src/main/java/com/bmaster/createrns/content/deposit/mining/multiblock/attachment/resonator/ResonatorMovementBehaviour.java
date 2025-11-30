package com.bmaster.createrns.content.deposit.mining.multiblock.attachment.resonator;

import com.bmaster.createrns.content.deposit.mining.multiblock.MinerBearingBlockEntity;
import com.bmaster.createrns.content.deposit.mining.multiblock.attachment.MiningEquipmentMovementBehaviour;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class ResonatorMovementBehaviour extends MiningEquipmentMovementBehaviour {

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    public void tick(MovementContext context) {
        if (!isActive(context) || !(context.contraption instanceof BearingContraption bc)) return;
        var bearing = getBearing(context);
        if (bearing == null) return;
        var equipment = bearing.getEquipmentManager();
        if (equipment == null) return;

        float rotationRatio = Math.abs(bearing.getSpeed() / AllConfigs.server().kinetics.maxRotationSpeed.get());
        float particleChance = 0.015f + (bc.stalled ? 0 : rotationRatio) * 0.395f;
        spawnParticle(context, particleChance);
    }

    @Override
    public boolean isActive(MovementContext context) {
        return super.isActive(context);
    }

    @Override
    @OnlyIn(value = Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                    ContraptionMatrices matrices, MultiBufferSource buffer) {
        ResonatorRenderer.renderInContraption(context, renderWorld, matrices, buffer, isActive(context));
    }

    public void spawnParticle(MovementContext context, float chance) {
        if (!(context.contraption instanceof BearingContraption bc)) return;
        if (!(context.state.getBlock() instanceof AbstractResonatorBlock rb)) return;
        if (context.world.random.nextFloat() < chance) {
            Vec3 local = Vec3.atCenterOf(context.localPos);
            Vec3 worldPos = bc.entity.toGlobalVector(local, AnimationTickHolder.getPartialTicks());
            Direction facing = AbstractResonatorBlock.getConnectedDirection(context.state);

            context.world.addParticle(rb.getParticle(),
                    worldPos.x + facing.getStepX() * 0.40,
                    worldPos.y - 1 + facing.getStepY() * 0.40,
                    worldPos.z + facing.getStepZ() * 0.40,
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
}
