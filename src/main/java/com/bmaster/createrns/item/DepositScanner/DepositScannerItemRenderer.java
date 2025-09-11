package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler.DepositProximity;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler.Mode;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
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
import net.minecraft.client.renderer.RenderType;
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
    private static final PartialModel POWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/powered"));
    private static final PartialModel ANTENNA_UNPOWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/antenna_unpowered"));
    private static final PartialModel ANTENNA_POWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/antenna_powered"));
    public static final PartialModel WHEEL = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/wheel"));

    private static final LerpedFloat equipProgress;
    private static final LerpedFloat scrollProgress;
    private static int poweredTicks = 0;

    static {
        equipProgress = LerpedFloat.linear().startWithValue(0).chase(0, 0.3f, Chaser.EXP);
        scrollProgress = LerpedFloat.linear().startWithValue(0).chase(0, 0.5f, Chaser.EXP);
    }

    protected static void tick() {
        if (Minecraft.getInstance().isPaused()) return;

        boolean active = DepositScannerClientHandler.getMode() != Mode.IDLE;

        equipProgress.updateChaseTarget(active ? 1 : 0);
        equipProgress.tickChaser();

        if (!active) return;
        if (poweredTicks > 0) poweredTicks--;

        if (scrollProgress.settled()) {
            scrollProgress.startWithValue(scrollProgress.getValue() % 360);
            scrollProgress.updateChaseTarget(scrollProgress.getValue());
        }

        scrollProgress.tickChaser();
    }

    protected static void powerFor(int ticks) {
        poweredTicks = ticks;
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

            ms.translate(-0.69f * equip * handModifier, 0.1 * equip, 0f * equip);

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
        // Left hand equip
        else if (equipInProgress && transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            msr.translate(equip * -1.5 / 16, equip * 2.5 / 16, equip * (1.25 / 16) * handModifier);
            msr.rotateYDegrees(equip * 12 * handModifier);
            active = true;
        }
        // Right hand equip
        else if (equipInProgress && transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            msr.translate(equip * -1.5 / 16, equip * 3 / 16, equip * 2 / 16 * handModifier);
            msr.rotateYDegrees(equip * 12 * handModifier);
            active = true;
        }

        active &= DepositScannerClientHandler.getMode() != Mode.IDLE;

        renderer.render(active ? POWERED.get() : model.getOriginalModel(), light);
        renderSelectedItem(ms, msr, buf, light, overlay);

        if (!active) {
            ms.popPose();
            return;
        }

        renderWheel(ms, msr, renderer, light, pt);
        renderAntennas(ms, renderer, light);

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

    private static void renderAntennas(PoseStack ms, PartialItemModelRenderer renderer, int light) {
        ms.pushPose();
        PartialModel partialAntenna1;
        PartialModel partialAntenna2;
        boolean nearDeposit = DepositScannerClientHandler.getDepositProximity() == DepositProximity.FOUND ||
                DepositScannerClientHandler.getDepositProximity() == DepositProximity.NEAR;

        if (nearDeposit || poweredTicks > 0) {
            partialAntenna1 = switch (DepositScannerClientHandler.getAntennaStatus()) {
                case INACTIVE, RIGHT_ACTIVE -> ANTENNA_UNPOWERED;
                case LEFT_ACTIVE, BOTH_ACTIVE -> ANTENNA_POWERED;
            };
            partialAntenna2 = switch (DepositScannerClientHandler.getAntennaStatus()) {
                case INACTIVE, LEFT_ACTIVE -> ANTENNA_UNPOWERED;
                case RIGHT_ACTIVE, BOTH_ACTIVE -> ANTENNA_POWERED;
            };
        } else {
            partialAntenna1 = ANTENNA_UNPOWERED;
            partialAntenna2 = ANTENNA_UNPOWERED;
        }
        renderer.render(partialAntenna1.get(), light);
        ms.translate(0, 0, -0.5f);
        renderer.render(partialAntenna2.get(), light);
        ms.popPose();
    }

    private static void renderWheel(PoseStack ms, PoseTransformStack msr, PartialItemModelRenderer renderer,
                                    int light, float partialTicks) {
        BakedModel wheel = WHEEL.get();

        ms.pushPose();
        msr.rotateYDegrees(scrollProgress.getValue(partialTicks) % 360);
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
