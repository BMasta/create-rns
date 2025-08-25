package com.bmaster.createrns.block.miner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class MinerRenderer extends KineticBlockEntityRenderer<MinerBlockEntity> {
    public MinerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(MinerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        final Axis axis = Axis.Y;
        float time = AnimationTickHolder.getRenderTime(be.getLevel());

        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(),
                Direction.UP);
        SuperByteBuffer drill_head = CachedBuffers.partialFacing(AllPartialModels.DRILL_HEAD, be.getBlockState(),
                Direction.DOWN);


        float angle = (time * be.getSpeed() * 3f / 10) % 360; // Degrees
        angle = angle / 180f * (float) Math.PI; // Radians

        kineticRotationTransform(shaft, be, axis, angle, light);
        kineticRotationTransform(drill_head, be, axis, angle, light);
        shaft.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        drill_head.renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
    }
}
