package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler.Mode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/// A humble rip-off of Create's linked controller
public class DepositScannerItemRenderer extends CustomRenderedItemModelRenderer {
    protected enum RenderType {
        NORMAL
    }

    private static final PartialModel POWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/powered"));
    private static final PartialModel WHEEL = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/wheel"));

    private static final LerpedFloat equipProgress;
    private static final LerpedFloat scrollProgress;

    static {
        equipProgress = LerpedFloat.linear().startWithValue(0);
        scrollProgress = LerpedFloat.linear().startWithValue(0).chase(0, 0.5f, Chaser.EXP);
    }

    protected static void tick() {
        if (Minecraft.getInstance().isPaused()) return;

        boolean active = DepositScannerClientHandler.MODE != Mode.IDLE;

        equipProgress.chase(active ? 1 : 0, .2f, Chaser.EXP);
        equipProgress.tickChaser();

        if (!active) return;

        if (scrollProgress.settled()) {
            scrollProgress.startWithValue(scrollProgress.getValue() % 360);
            scrollProgress.updateChaseTarget(scrollProgress.getValue());
        }

        scrollProgress.tickChaser();
    }

    protected static void scrollUp() {
        scrollProgress.updateChaseTarget(scrollProgress.getChaseTarget() + 90);
    }

    protected static void scrollDown() {
        scrollProgress.updateChaseTarget(scrollProgress.getChaseTarget() - 90);
    }

    protected static void resetWheel() {
        scrollProgress.startWithValue(0);
        scrollProgress.updateChaseTarget(0);
    }

    @Override
    protected void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
                          ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light,
                          int overlay) {
        render(stack, model, renderer, transformType, ms, light);
    }

    private static void render(ItemStack stack, CustomRenderedItemModel model,
                               PartialItemModelRenderer renderer, ItemDisplayContext transformType, PoseStack ms,
                               int light) {
        float pt = AnimationTickHolder.getPartialTicks();
        var msr = TransformStack.of(ms);
        boolean active = false;
        Minecraft mc = Minecraft.getInstance();
        boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
        ItemDisplayContext mainHand =
                rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        ItemDisplayContext offHand =
                rightHanded ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        boolean noControllerInMain = !AllContent.DEPOSIT_SCANNER_ITEM.isIn(mc.player.getMainHandItem());

        ms.pushPose();
        if (transformType == mainHand || (transformType == offHand && noControllerInMain)) {
            float equip = equipProgress.getValue(pt);
            int handModifier = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
            msr.translate(0, equip / 4, equip / 4 * handModifier);
            msr.rotateYDegrees(equip * -30 * handModifier);
            msr.rotateZDegrees(equip * -30);
            active = true;
        }

        if (transformType == ItemDisplayContext.GUI) {
            if (stack == mc.player.getMainHandItem())
                active = true;
            if (stack == mc.player.getOffhandItem() && noControllerInMain)
                active = true;
        }

        active &= DepositScannerClientHandler.MODE != Mode.IDLE;

        renderer.render(active ? POWERED.get() : model.getOriginalModel(), light);

        if (!active) {
            ms.popPose();
            return;
        }

        BakedModel wheel = WHEEL.get();
        // Subtract 0.5 to convert from corner-based to center-based
        float centerX = 8f / 16f - 0.5f;
        float centerY = 2f / 16f - 0.5f;
        float centerZ = 4.5f / 16f - 0.5f;

        ms.pushPose();
        msr.translate(centerX, centerY, centerZ);
        msr.rotateZDegrees(scrollProgress.getValue(pt) % 360);
        msr.translate(-centerX, -centerY, -centerZ);
        renderer.renderSolid(wheel, light);
        ms.popPose();

        ms.popPose();
    }
}
