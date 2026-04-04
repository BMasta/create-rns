package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.resonator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.Rotate;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.transform.Translate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class ResonatorRenderer extends SmartBlockEntityRenderer<ResonatorBlockEntity> {
    public ResonatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static PartialModel getShardModel(BlockState state, boolean active) {
        return ((AbstractResonatorBlock) state.getBlock()).getShard(active);
    }

    public static <T extends Translate<T> & Rotate<T>> T applyLocalTransforms(T transform, BlockState state) {
        return transform
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(state.getValue(AbstractResonatorBlock.FACING)))
                .rotateXDegrees(getXRotation(state))
                .uncenter();
    }

    @Override
    protected void renderSafe(ResonatorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buf, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        var bs = be.getBlockState();
        var superBuffer = CachedBuffers.partial(getShardModel(bs, false), bs);

        ms.pushPose();
        applyLocalTransforms(TransformStack.of(ms), bs);
        superBuffer
                .light(light)
                .renderInto(ms, buf.getBuffer(RenderType.solid()));
        ms.popPose();
    }

    public static void renderInContraption(
            MovementContext context, ContraptionMatrices matrices, MultiBufferSource buf, boolean active
    ) {
        if (VisualizationManager.supportsVisualization(context.world)) return;

        var bs = context.state;
        var superBuffer = CachedBuffers.partial(getShardModel(bs, active), bs);

        superBuffer
                .transform(matrices.getModel())
                .center()
                .rotateYDegrees(AngleHelper.horizontalAngle(bs.getValue(AbstractResonatorBlock.FACING)))
                .rotateXDegrees(getXRotation(bs))
                .uncenter()
                .light(active ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(context.world, context.localPos));
        if (!active) superBuffer.useLevelLight(context.world, matrices.getWorld());

        superBuffer.renderInto(matrices.getViewProjection(), buf.getBuffer(active ? RenderType.cutout() : RenderType.solid()));
    }

    public static int getXRotation(BlockState state) {
        return switch (state.getValue(AbstractResonatorBlock.FACE)) {
            case FLOOR -> 0;
            case CEILING -> 180;
            case WALL -> 90;
        };
    }
}
