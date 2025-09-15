package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.util.Utils;
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

import java.util.Random;

/// A humble rip-off of Create's linked controller
public class DepositScannerItemRenderer extends CustomRenderedItemModelRenderer {
    private static final PartialModel UNPOWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/unpowered"));
    private static final PartialModel POWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/powered"));
    private static final PartialModel ANTENNA_UNPOWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/antenna_unpowered"));
    private static final PartialModel ANTENNA_POWERED = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/antenna_powered"));
    public static final PartialModel WHEEL = PartialModel.of(ResourceLocation.fromNamespaceAndPath(
            CreateRNS.MOD_ID, "item/deposit_scanner/wheel"));

    private static final int JITTER_TICKS_PER_SHAKE = 20;
    private static final float JITTER_SCALE = 0.002f;

    private static final Random rng = new Random();
    private static final LerpedFloat scrollProgress;
    private static final LerpedFloat ambientItemMovement;
    private static int poweredTicks = 0;

    private static final LerpedFloat itemJitterX;
    private static final LerpedFloat itemJitterZ;
    private static int remainingJitterTicks = 0;

    static {
        scrollProgress = LerpedFloat.linear().startWithValue(0).chase(0, 0.5f, Chaser.EXP);
        ambientItemMovement = LerpedFloat.linear().startWithValue(2).chase(4, 0.04f, Chaser.LINEAR);
        itemJitterX = LerpedFloat.linear().startWithValue(0).chase(0, 2f, Chaser.EXP);
        itemJitterZ = LerpedFloat.linear().startWithValue(0).chase(0, 2f, Chaser.EXP);
    }

    protected static void tick() {
        if (Minecraft.getInstance().isPaused()) return;
        if (poweredTicks > 0) poweredTicks--;

        if (scrollProgress.settled()) {
            scrollProgress.startWithValue(scrollProgress.getValue() % 360);
            scrollProgress.updateChaseTarget(scrollProgress.getValue());
        }

        var am = ambientItemMovement.getValue();
        if (Mth.equal(am,4)) ambientItemMovement.setValue(0);

        if (remainingJitterTicks > 0) {
            itemJitterX.updateChaseTarget(rng.nextFloat() - 0.5f);
            itemJitterZ.updateChaseTarget(rng.nextFloat() - 0.5f);
            remainingJitterTicks--;
        } else {
            itemJitterX.updateChaseTarget(0);
            itemJitterZ.updateChaseTarget(0);
        }

        scrollProgress.tickChaser();
        ambientItemMovement.tickChaser();
        itemJitterX.tickChaser();
        itemJitterZ.tickChaser();
    }

    protected static void powerBriefly() {
        poweredTicks = 2;
    }

    protected static void scrollUp() {
        scrollProgress.updateChaseTarget(scrollProgress.getChaseTarget() + 90);
    }

    protected static void scrollDown() {
        scrollProgress.updateChaseTarget(scrollProgress.getChaseTarget() - 90);
    }

    protected static void shakeItem() {
        remainingJitterTicks = JITTER_TICKS_PER_SHAKE;
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
        boolean dynamic = false;

        ms.pushPose();

        // Two-hand
        if (transformType == mainDisplay && p.getOffhandItem().isEmpty()) {
            var transform = model.getTransforms().getTransform(transformType);
            float viewAngleMultiplier = AnimationFunctions.easeIn(
                    Mth.clamp(p.getViewXRot(pt), 0, 45) / 45);

            undoModelTransform(transform, ms, msr, handModifier);

            ms.translate(-0.69f * handModifier, 0.1, 0f);

            // Base model is already rotated 90 deg
            msr.rotateYDegrees(-90);

            // Behaves like vanilla map
            float moveOutWhenLookingDown = -0.2f * viewAngleMultiplier;
            float moveDownWhenLookingDown = -0.4f * viewAngleMultiplier;
            float rotateInWhenLookingDown = 60 * viewAngleMultiplier;

            // Set pivot to the bottom of scanner's base
            ms.translate((0.4f), -7f / 16f, 0);
            msr.rotateZDegrees((-10 - rotateInWhenLookingDown));
            ms.translate((-0.4f + moveOutWhenLookingDown), (7f / 16f + moveDownWhenLookingDown), 0);

            dynamic = true;
        }
        // Left hand
        else if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            msr.translate(-1.5 / 16, 2.5 / 16, (1.25 / 16) * handModifier);
            msr.rotateYDegrees(12 * handModifier);
            dynamic = true;
        }
        // Right hand
        else if (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            msr.translate(-1.5f / 16, 3f / 16, 2f / 16 * handModifier);
            msr.rotateYDegrees(12 * handModifier);
            dynamic = true;
        }

        var dynamic_scanner_base = DepositScannerClientHandler.isTracking() ? POWERED.get() : UNPOWERED.get();
        renderer.render(dynamic ? dynamic_scanner_base : model.getOriginalModel(), light);
        renderSelectedItem(ms, msr, buf, light, overlay, pt);

        if (!dynamic) {
            ms.popPose();
            return;
        }

        renderWheel(ms, msr, renderer, light, pt);
        renderAntennas(ms, renderer, light);

        ms.popPose();
    }

    private static void undoModelTransform(ItemTransform transform, PoseStack ms, PoseTransformStack msr,
                                           int handModifier) {
        Vector3f trn = transform.translation;
        Vector3f rot = transform.rotation;
        Vector3f scl = transform.scale;

        float sx = (scl.x() == 0f) ? 1f : 1f / scl.x();
        float sy = (scl.y() == 0f) ? 1f : 1f / scl.y();
        float sz = (scl.z() == 0f) ? 1f : 1f / scl.z();

        ms.scale(sx, sy, sz);

        msr.rotateZDegrees(-rot.z() * handModifier);
        msr.rotateYDegrees(-rot.y() * handModifier);
        msr.rotateXDegrees(-rot.x() * handModifier);

        ms.translate(-trn.x() / 16f, -trn.y() / 16f, -trn.z() / 16f);
    }

    private static void renderAntennas(PoseStack ms, PartialItemModelRenderer renderer, int light) {
        ms.pushPose();
        PartialModel partialAntenna1;
        PartialModel partialAntenna2;

        if (DepositScannerClientHandler.isDepositFound() || poweredTicks > 0) {
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
                                           int light, int overlay, float partialTicks) {
        float am = ambientItemMovement.getValue(partialTicks);
        int phase = (am < 2) ? 1 : -1;
        float amLiftEased = (Utils.easeInOut(am % 2 / 2, 2f) - 0.5f) * 2 * phase;
        float amRotEased = Utils.easeInOut(1f - Math.abs(am % 2 / 2 - 0.5f) * 2, 1.2f) * phase;
        float jitterX = itemJitterX.getValue(partialTicks) * JITTER_SCALE * remainingJitterTicks / JITTER_TICKS_PER_SHAKE;
        float jitterZ = itemJitterZ.getValue(partialTicks) * JITTER_SCALE * remainingJitterTicks / JITTER_TICKS_PER_SHAKE;

        float cx = 0;
        float cy = 4f / 16f - 0.5f;
        float cz = 0;

        ms.pushPose();
        ms.translate(cx + jitterX, cy + amLiftEased / 96, cz + jitterZ);
        ms.scale(0.15f, 0.15f, 0.15f);
        msr.rotateXDegrees(-90);
        msr.rotateZDegrees(90);
        msr.rotateXDegrees(10 * amRotEased);
        Minecraft.getInstance().getItemRenderer().renderStatic(DepositScannerClientHandler.getSelectedItem(),
                ItemDisplayContext.GUI, light, overlay, ms, buf, null, 0);
        ms.popPose();
    }
}
