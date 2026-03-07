package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.RNSBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Block;

public class AnimatedMiner extends AnimatedKinetics {
    private Block depositBlock;

    public void draw(GuiGraphics graphics, int xOffset, int yOffset, Block depositBlock) {
        this.depositBlock = depositBlock;
        draw(graphics, xOffset, yOffset);
    }

    @Deprecated
    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack matrixStack = graphics.pose();
        matrixStack.pushPose();
        matrixStack.translate(xOffset + 0.7, yOffset, 0);
        matrixStack.mulPose(Axis.XP.rotationDegrees(-22.5f));
        matrixStack.mulPose(Axis.YP.rotationDegrees(45f));
        int scale = 16;
        float angle = getCurrentAngle() * 8;

        blockElement(AllPartialModels.SHAFT_HALF)
                .scale(scale)
                .rotateBlock(-angle, 90, 90)
                .atLocal(0, -1, 3)
                .render(graphics);

        blockElement(RNSBlocks.MINER_BEARING.getDefaultState())
                .scale(scale)
                .rotateBlock(-90, 0, 0)
                .atLocal(0, -1, 3)
                .render(graphics);

        blockElement(AllPartialModels.BEARING_TOP)
                .scale(scale)
                .rotateBlock(0, angle, 180)
                .atLocal(0, -1, 3)
                .render(graphics);

        blockElement(AllBlocks.ANDESITE_CASING.getDefaultState())
                .scale(scale)
                .rotateBlock(0, angle, 180)
                .atLocal(0, 0, 3)
                .render(graphics);

        blockElement(RNSBlocks.DRILL_HEAD.getDefaultState())
                .scale(scale)
                .rotateBlock(0, angle, 180)
                .atLocal(0, 1, 3)
                .render(graphics);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                blockElement(depositBlock.defaultBlockState())
                        .scale(scale)
                        .atLocal(i - 1, 2, j + 2)
                        .render(graphics);
            }
        }

        matrixStack.popPose();
    }
}
