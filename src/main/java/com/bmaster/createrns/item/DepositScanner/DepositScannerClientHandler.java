package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.AllContent;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.utility.ControlsUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

public class DepositScannerClientHandler {
    public static Mode MODE = Mode.IDLE;

    public enum Mode {
        IDLE, ACTIVE
    }

    public static void toggle() {
        if (MODE == Mode.IDLE) {
            MODE = Mode.ACTIVE;
        } else {
            MODE = Mode.IDLE;
            onReset();
        }
    }

    public static void scrollDown() {
        if (MODE != Mode.IDLE) {
            DepositScannerItemRenderer.scrollDown();
        }
    }

    public static void scrollUp() {
        if (MODE != Mode.IDLE) {
            DepositScannerItemRenderer.scrollUp();
        }
    }

    public static void tick() {
        DepositScannerItemRenderer.tick();

        if (MODE == Mode.IDLE)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ItemStack heldItem = player.getMainHandItem();

        if (player.isSpectator()) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        if (!AllContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
            heldItem = player.getOffhandItem();
            if (!AllContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
                MODE = Mode.IDLE;
                onReset();
                return;
            }
        }

        if (mc.screen != null) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        if (InputConstants.isKeyDown(mc.getWindow()
                .getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }
    }

    protected static void onReset() {
        DepositScannerItemRenderer.resetWheel();
    }
}
