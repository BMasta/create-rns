package com.bmaster.createrns.mining.miner;

import com.bmaster.createrns.RNSContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class MinerRenderer extends KineticBlockEntityRenderer<MinerBlockEntity> {
    public MinerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(MinerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

        var spec = be.getSpec();
        int tier = (spec == null) ? 1 : spec.tier();
        SuperByteBuffer head = CachedBuffers.partial(
                (tier <= 1) ? RNSContent.MINER_MK1_DRILL : RNSContent.MINER_MK2_DRILL, be.getBlockState());

        renderRotatingBuffer(be, head, ms, vb, light);
    }
}
