package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
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
        superBuffer
                .light(light)
                .renderInto(ms, buf.getBuffer(RenderType.solid()));
    }

    public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld,
                                           ContraptionMatrices matrices, MultiBufferSource buf, boolean active) {
        var bs = context.state;
        var superBuffer = CachedBuffers.partial(((AbstractResonatorBlock) bs.getBlock()).getShard(active), bs);

        superBuffer
                .transform(matrices.getModel())
                .light(LightTexture.FULL_BRIGHT)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), buf.getBuffer(RenderType.cutout()));
    }
}
