package com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class DrillHeadRenderer extends SmartBlockEntityRenderer<DrillHeadBlockEntity> {
    public DrillHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(DrillHeadBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buf, int light, int overlay) {
        var bs = be.getBlockState();
        var scale = getScaleForSize(bs.getValue(DrillHeadBlock.SIZE));
        var superBuffer = CachedBuffers.block(bs);

        superBuffer
                .center()
                .scale(scale)
                .uncenter()
                .light(LightTexture.FULL_BRIGHT)
                .useLevelLight(be.getLevel())
                .renderInto(ms, buf.getBuffer(RenderType.solid()));
    }

    public static void renderInContraption(MovementContext context, ContraptionMatrices matrices, MultiBufferSource buf) {
        var bs = context.state;
        var scale = getScaleForSize(bs.getValue(DrillHeadBlock.SIZE));
        var superBuffer = CachedBuffers.block(bs);

        superBuffer
                .transform(matrices.getModel())
                .center()
                .scale(scale)
                .uncenter()
                .light(LightTexture.FULL_BRIGHT)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), buf.getBuffer(RenderType.solid()));
    }

    protected static float getScaleForSize(DrillHeadSize size) {
        return switch (size) {
            case SMALL -> 1f;
            case MEDIUM -> 1.5f;
            case LARGE -> 2f;
        };
    }
}
