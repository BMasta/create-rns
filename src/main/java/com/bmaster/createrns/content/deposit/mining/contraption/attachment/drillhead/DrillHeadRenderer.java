package com.bmaster.createrns.content.deposit.mining.contraption.attachment.drillhead;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LevelRenderer;
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
        var size = bs.getValue(DrillHeadBlock.SIZE);
        var scale = size.getModelScale();
        var offset = size.getModelOffset();
        var direction = DrillHeadBlock.getConnectedDirection(bs);
        var superBuffer = CachedBuffers.block(bs);

        assert be.getLevel() != null;
        superBuffer
                .center()
                .scale(scale)
                .uncenter()
                .translate(direction.getStepX() * offset, direction.getStepY() * offset, direction.getStepZ() * offset)
                .light(light)
                .useLevelLight(be.getLevel())
                .renderInto(ms, buf.getBuffer(RenderType.solid()));
    }

    public static void renderInContraption(MovementContext context, ContraptionMatrices matrices, MultiBufferSource buf) {
        var bs = context.state;
        var size = bs.getValue(DrillHeadBlock.SIZE);
        var scale = size.getModelScale();
        var offset = size.getModelOffset();
        var direction = DrillHeadBlock.getConnectedDirection(bs);
        var superBuffer = CachedBuffers.block(bs);

        superBuffer
                .transform(matrices.getModel())
                .center()
                .scale(scale)
                .uncenter()
                .translate(direction.getStepX() * offset, direction.getStepY() * offset, direction.getStepZ() * offset)
                .light(LevelRenderer.getLightColor(context.world, context.localPos))
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), buf.getBuffer(RenderType.solid()));
    }
}
