package com.bmaster.createrns.compat.jei;

import com.bmaster.createrns.RNSBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.block.Block;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AnimatedMiner extends AnimatedKinetics {
    private Block depositBlock;
    private float scale = 14;

    public void draw(GuiGraphics graphics, int xOffset, int yOffset, Block depositBlock, float scale) {
        this.depositBlock = depositBlock;
        this.scale = scale;
        draw(graphics, xOffset, yOffset);
    }

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        double scaleOffset = 1 - scale;
        pose.translate(xOffset + 0.2, yOffset - (6.5 * scaleOffset), 0);
        pose.mulPose(Axis.XP.rotationDegrees(-22.5f));
        pose.mulPose(Axis.YP.rotationDegrees(45f));

        // Keep the miner centered horizontally when scaled, but anchor it at the top with extra clearance
        pose.translate(0.5 * scaleOffset, 6 * scaleOffset, 3.5 * scaleOffset);

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

        blockElement(RNSBlocks.MINE_HEAD.getDefaultState())
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

        pose.popPose();
    }
}
