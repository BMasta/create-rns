package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class ResonatorRenderer extends SmartBlockEntityRenderer<ResonatorBlockEntity> {
    public ResonatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ResonatorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buf, int light, int overlay) {
        var bs = be.getBlockState();
        var superBuffer = CachedBuffers.partial(((AbstractResonatorBlock) bs.getBlock()).getShard(false), bs);
        var face = bs.getValue(AbstractResonatorBlock.FACE);
        var facing = bs.getValue(AbstractResonatorBlock.FACING);

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

    public static void renderInContraption(
            MovementContext context, ContraptionMatrices matrices, MultiBufferSource buf, boolean active
    ) {
        var bs = context.state;
        var superBuffer = CachedBuffers.partial(((AbstractResonatorBlock) bs.getBlock()).getShard(active), bs);
        var face = bs.getValue(AbstractResonatorBlock.FACE);
        var facing = bs.getValue(AbstractResonatorBlock.FACING);

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
