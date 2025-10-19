//package com.bmaster.createrns.mining.miner.impl;
//
//import com.bmaster.createrns.RNSContent;
//import com.bmaster.createrns.mining.miner.MinerBlockEntity;
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
//import dev.engine_room.flywheel.api.visualization.VisualizationManager;
//import net.createmod.catnip.render.CachedBuffers;
//import net.createmod.catnip.render.SuperByteBuffer;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//
//public class MinerMk2Renderer extends KineticBlockEntityRenderer<MinerBlockEntity> {
//    public MinerMk2Renderer(BlockEntityRendererProvider.Context context) {
//        super(context);
//    }
//
//    @Override
//    protected void renderSafe(MinerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
//                              int light, int overlay) {
//        if (VisualizationManager.supportsVisualization(be.getLevel())) return;
//
//        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
//        SuperByteBuffer head = CachedBuffers.partial(RNSContent.MINER_MK2_DRILL, be.getBlockState());
//
//        renderRotatingBuffer(be, head, ms, vb, light);
//    }
//}
