package com.bmaster.createrns.content.deposit.mining.contraption.attachment.resonance.buffer;

import com.bmaster.createrns.RNSPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ResonanceBufferRenderer extends SmartBlockEntityRenderer<ResonanceBufferBlockEntity> {
    public ResonanceBufferRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(ResonanceBufferBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buf, int light, int overlay) {
        var bs = be.getBlockState();
        var superBuffer = CachedBuffers.partial(RNSPartialModels.RESONANCE_BUFFER_SHARD, bs);
        superBuffer
                .light(light)
                .renderInto(ms, buf.getBuffer(RenderType.solid()));
    }

    public static void renderInContraption(MovementContext context, ContraptionMatrices matrices,
                                           MultiBufferSource buf, PartialModel pm) {
        var bs = context.state;
        var superBuffer = CachedBuffers.partial(pm, bs);

        superBuffer
                .transform(matrices.getModel())
                .light(LightTexture.FULL_BRIGHT)
                .useLevelLight(context.world, matrices.getWorld())
                .renderInto(matrices.getViewProjection(), buf.getBuffer(RenderType.cutout()));
    }
}
