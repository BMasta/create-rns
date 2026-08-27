package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.Scale;
import dev.engine_room.flywheel.lib.transform.Translate;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MineHeadRenderer extends SmartBlockEntityRenderer<MineHeadBlockEntity> {
    public MineHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    public static <T extends Translate<T> & Scale<T>> T applyLocalTransforms(T transform, BlockState state) {
        var size = state.getValue(MineHeadBlock.SIZE);
        var direction = MineHeadBlock.getConnectedDirection(state);

        return transform
                .center()
                .scale(size.modelScale)
                .uncenter()
                .translate(
                        direction.getStepX() * size.modelOffset,
                        direction.getStepY() * size.modelOffset,
                        direction.getStepZ() * size.modelOffset
                );
    }

    @Override
    protected void renderSafe(MineHeadBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buf, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        var bs = be.getBlockState();
        var superBuffer = CachedBuffers.block(bs);

        applyLocalTransforms(superBuffer, bs)
                .light(light)
                .renderInto(ms, buf.getBuffer(RenderType.solid()));
    }
}
