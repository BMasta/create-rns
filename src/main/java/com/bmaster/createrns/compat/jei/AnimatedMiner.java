//package com.bmaster.createrns.compat.jei;
//
//import com.bmaster.createrns.RNSContent;
//import com.bmaster.createrns.mining.miner.MinerBlock;
//import com.bmaster.createrns.mining.miner.MinerBlockEntity;
//import com.ibm.icu.text.MessagePattern;
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.math.Axis;
//import com.simibubi.create.AllBlocks;
//import com.simibubi.create.AllPartialModels;
//import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
//import com.simibubi.create.foundation.gui.AllGuiTextures;
//import dev.engine_room.flywheel.lib.model.baked.PartialModel;
//import net.minecraft.client.gui.GuiGraphics;
//import net.minecraft.core.Direction;
//
//public class AnimatedMiner extends AnimatedKinetics {
//    private final MinerBlock<? extends MinerBlockEntity> miner;
//    private final PartialModel drill;
//
//    public AnimatedMiner(MinerBlock<? extends MinerBlockEntity> minerBlock, PartialModel drill) {
//        this.miner = minerBlock;
//        this.drill = drill;
//    }
//
//    @Override
//    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
//        PoseStack matrixStack = graphics.pose();
//        matrixStack.pushPose();
//        matrixStack.translate(xOffset, yOffset, 0);
//        matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5f));
//        matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));
//        int scale = 26;
//
//        blockElement(miner.defaultBlockState())
//                .scale(scale)
//                .render(graphics);
//
//        blockElement(drill)
//                .rotateBlock(0, getCurrentAngle(), 0)
//                .scale(scale)
//                .render(graphics);
//
//        matrixStack.popPose();
//    }
//}
