package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.propagator;

import com.bmaster.createrns.RNSPartialModels;
import com.bmaster.createrns.content.deposit.mining.contraption.attachment.FaceAttachedMinerComponentBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class ResonancePropagatorRenderer extends SmartBlockEntityRenderer<ResonancePropagatorBlockEntity> {
    public ResonancePropagatorRenderer(Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ResonancePropagatorBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buf, int light, int overlay) {
        var bs = be.getBlockState();
        var superBuffer = CachedBuffers.partial(RNSPartialModels.PROPAGATOR_SHARD, bs);
        var face = bs.getValue(ResonancePropagatorBlock.FACE);
        var facing = bs.getValue(ResonancePropagatorBlock.FACING);

        int xRot = 0;
        if (face == AttachFace.CEILING) {
            xRot = 180;
        } else if (face == AttachFace.WALL) {
            xRot = 90;
        }

        superBuffer
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .rotateXDegrees(xRot)
                .uncenter()
                .light(light)
                .renderInto(ms, buf.getBuffer(RenderType.solid()));
    }

    public static void renderInContraption(MovementContext context, ContraptionMatrices matrices,
                                           MultiBufferSource buf, PartialModel pm) {
        var bs = context.state;
        var superBuffer = CachedBuffers.partial(pm, bs);
        var face = bs.getValue(FaceAttachedMinerComponentBlock.FACE);
        var facing = bs.getValue(FaceAttachedMinerComponentBlock.FACING);

        int xRot;
        if (face == AttachFace.FLOOR) {
            xRot = 0;
        } else if (face == AttachFace.CEILING) {
            xRot = 180;
        } else {
            xRot = 90;
        }

        superBuffer
                .transform(matrices.getModel())
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(facing))
                .rotateXDegrees(xRot)
                .uncenter()
                .light(LightTexture.FULL_BRIGHT)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), buf.getBuffer(RenderType.cutout()));
    }
}
