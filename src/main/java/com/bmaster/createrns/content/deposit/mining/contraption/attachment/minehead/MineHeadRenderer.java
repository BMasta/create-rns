package com.bmaster.createrns.content.deposit.mining.contraption.attachment.minehead;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MineHeadRenderer extends SmartBlockEntityRenderer<MineHeadBlockEntity> {
    public MineHeadRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(MineHeadBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buf, int light, int overlay) {
        var bs = be.getBlockState();
        var size = bs.getValue(MineHeadBlock.SIZE);
        float scale = size.modelScale;
        float offset = size.modelOffset;
//        float scale = 2f;
//        float offset = 0.248f;
        var direction = MineHeadBlock.getConnectedDirection(bs);
        var superBuffer = CachedBuffers.block(bs);

        assert be.getLevel() != null;
        superBuffer
                .center()
                .scale(scale)
                .uncenter()
                .translate(direction.getStepX() * offset, direction.getStepY() * offset, direction.getStepZ() * offset)
                .light(light)
                .renderInto(ms, buf.getBuffer(RenderType.solid()));
    }
}
