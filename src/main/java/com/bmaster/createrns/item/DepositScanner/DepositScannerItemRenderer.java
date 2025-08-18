package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler.Mode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationFunctions;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import com.bmaster.createrns.CreateRNS;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

/// A humble rip-off of Create's linked controller
public class DepositScannerItemRenderer extends CustomRenderedItemModelRenderer {
    public enum AntennaStatus {
        INACTIVE, LEFT_ACTIVE, RIGHT_ACTIVE, BOTH_ACTIVE
    }

    protected enum RenderType {
        NORMAL
    }

    private static final PartialModel POWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/powered"));
    private static final PartialModel WHEEL = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/wheel"));

    private static final LerpedFloat equipProgress;
    private static final LerpedFloat scrollProgress;
    private static final LerpedFloat pressProgress;

    static {
        equipProgress = LerpedFloat.linear().startWithValue(0).chase(0, 0.3f, Chaser.EXP);
        scrollProgress = LerpedFloat.linear().startWithValue(0).chase(0, 0.5f, Chaser.EXP);
        pressProgress = LerpedFloat.linear().startWithValue(0).chase(0, 0.6f, Chaser.EXP);
    }

    protected static void tick() {
        if (Minecraft.getInstance().isPaused()) return;

        boolean active = DepositScannerClientHandler.mode != Mode.IDLE;

        equipProgress.updateChaseTarget(active ? 1 : 0);
        equipProgress.tickChaser();

        if (!active) return;

        if (scrollProgress.settled()) {
            scrollProgress.startWithValue(scrollProgress.getValue() % 360);
            scrollProgress.updateChaseTarget(scrollProgress.getValue());
        }
        pressProgress.updateChaseTarget(DepositScannerClientHandler.isSelectionLocked() ? 1 : 0);

        scrollProgress.tickChaser();
        pressProgress.tickChaser();
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
                          ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buf,
                          int light, int overlay) {
        staticRender(stack, model, renderer, transformType, ms, buf, light, overlay);
    }

    private static void staticRender(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer,
                                     ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buf,
                                     int light, int overlay) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;

        boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
        var mainDisplay = rightHanded ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        var offDisplay = rightHanded ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        int handModifier = transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
        float pt = AnimationTickHolder.getPartialTicks();
        var msr = TransformStack.of(ms);
        float equip = equipProgress.getValue(pt);
        boolean equipInProgress = !Mth.equal(equip, 0);
        boolean active = false;

        ms.pushPose();

        // Two-arm equip
        if (equipInProgress && transformType == mainDisplay && p.getOffhandItem().isEmpty()) {
            var transform = model.getTransforms().getTransform(transformType);
            float viewAngleMultiplier = AnimationFunctions.easeIn(
                    Mth.clamp(p.getViewXRot(pt), 0, 45) / 45);

            undoModelTransform(transform, ms, msr, pt, handModifier);

            // ItemInHandRenderer.ITEM_POS_X is private, so enjoy the magic number
            ms.translate(-0.56f * equip * handModifier, 0.1 * equip, 0f * equip);

            // Base model is already rotated 90 deg
            msr.rotateYDegrees(-90 * equip);

            // Behaves like vanilla map
            float moveOutWhenLookingDown = -0.2f * viewAngleMultiplier;
            float moveDownWhenLookingDown = -0.4f * viewAngleMultiplier;
            float rotateInWhenLookingDown = 60 * viewAngleMultiplier;

            // Set pivot to the bottom of scanner's base
            ms.translate((0.4f) * equip, -7f / 16f * equip, 0);
            msr.rotateZDegrees((-10 - rotateInWhenLookingDown) * equip);
            ms.translate((-0.4f + moveOutWhenLookingDown) * equip, (7f / 16f + moveDownWhenLookingDown) * equip, 0);

            active = true;
        }
        // One-arm equip
        else if (equipInProgress && (transformType == mainDisplay || transformType == offDisplay)) {
            msr.translate(0, equip / 4, equip / 4 * handModifier);
            msr.rotateYDegrees(equip * -30 * handModifier);
            msr.rotateZDegrees(equip * -30);

            active = true;
        }
        // In inventory
        else if (transformType == ItemDisplayContext.GUI) {
            if (stack == p.getMainHandItem()) active = true;
            if (stack == p.getOffhandItem()) active = true;
        }

        active &= DepositScannerClientHandler.mode != Mode.IDLE;

        renderer.render(active ? POWERED.get() : model.getOriginalModel(), light);
        renderSelectedItem(ms, msr, buf, light, overlay);

        if (!active) {
            ms.popPose();
            return;
        }

        renderWheel(ms, msr, renderer, light, pt);

        ms.popPose();
    }

    private static void undoModelTransform(ItemTransform transform, PoseStack ms, PoseTransformStack msr,
                                           float partialTicks, int handModifier) {
        float equip = equipProgress.getValue(partialTicks);
        Vector3f trn = transform.translation;
        Vector3f rot = transform.rotation;
        Vector3f scl = transform.scale;

        float sx_equip = ((scl.x() == 0f) ? 1f : 1f / scl.x()) * equip + 1 - equip;
        float sy_equip = ((scl.y() == 0f) ? 1f : 1f / scl.y()) * equip + 1 - equip;
        float sz_equip = ((scl.z() == 0f) ? 1f : 1f / scl.z()) * equip + 1 - equip;

        ms.scale(sx_equip, sy_equip, sz_equip);

        msr.rotateZDegrees(-rot.z() * equip * handModifier);
        msr.rotateYDegrees(-rot.y() * equip * handModifier);
        msr.rotateXDegrees(-rot.x() * equip * handModifier);

        ms.translate(-trn.x() / 16f * equip, -trn.y() / 16f * equip, -trn.z() / 16f * equip);
    }

    private static void renderWheel(PoseStack ms, PoseTransformStack msr, PartialItemModelRenderer renderer,
                                    int light, float partialTicks) {
        BakedModel wheel = WHEEL.get();
        // Subtract 0.5 to convert from corner-based to center-based
        float pressed = -0.4f / 16f;
        float centerX = 8f / 16f - 0.5f;
        float centerY = 2f / 16f - 0.5f;
        float centerZ = 4.5f / 16f - 0.5f;

        ms.pushPose();
        msr.translate(centerX, centerY + pressed * pressProgress.getValue(partialTicks), centerZ);
        msr.rotateZDegrees(scrollProgress.getValue(partialTicks) % 360);
        msr.translate(-centerX, -centerY, -centerZ);
        renderer.renderSolid(wheel, light);
        ms.popPose();
    }

    private static void renderSelectedItem(PoseStack ms, PoseTransformStack msr, MultiBufferSource buf,
                                           int light, int overlay) {
        float cx = 0;
        float cy = 4f / 16f - 0.5f;
        float cz = 0;

        ms.pushPose();
        ms.translate(cx, cy, cz);
        ms.scale(0.2f, 0.2f, 0.2f);
        msr.rotateXDegrees(-90);
        msr.rotateZDegrees(90);
        Minecraft.getInstance().getItemRenderer().renderStatic(DepositScannerClientHandler.getSelectedItem(),
                ItemDisplayContext.GUI, light, overlay, ms, buf, null, 0);
        ms.popPose();
    }
}
