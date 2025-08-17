package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class DepositScannerClientHandler {
    public static Mode mode = Mode.IDLE;

    public enum Mode {
        IDLE, ACTIVE
    }

    private static int selectedIndex = 0;

    public static boolean isSelectionLocked() {
        return isAttackPressed();
    }

    public static void toggle() {
        if (mode == Mode.IDLE) {
            mode = Mode.ACTIVE;
        } else {
            mode = Mode.IDLE;
            onReset();
        }
    }

    public static void scrollDown() {
        if (mode != Mode.IDLE) {
            selectedIndex++;
            DepositScannerItemRenderer.scrollDown();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                AllSoundEvents.SCROLL_VALUE.playAt(player.level(), player.blockPosition(), 1f, 1f, true);
            }
            DepositScannerC2SPacket.send(getSelectedItem().getItem());
        }
    }

    public static void scrollUp() {
        if (mode != Mode.IDLE) {
            selectedIndex--;
            DepositScannerItemRenderer.scrollUp();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                AllSoundEvents.SCROLL_VALUE.playAt(player.level(), player.blockPosition(), 1f, 1f, true);
            }
        }
    }

    public static void tick() {
        DepositScannerItemRenderer.tick();
        refreshMode();
    }

    protected static ItemStack getSelectedItem() {
        if (ServerConfig.OVERWORLD_ORES.isEmpty()) return ItemStack.EMPTY;
        int size = ServerConfig.OVERWORLD_ORES.size();
        int normalizedIndex = (selectedIndex % size + size) % size;
        return new ItemStack(ServerConfig.OVERWORLD_ORES.get(normalizedIndex));
    }

    private static boolean isAttackPressed() {
        InputConstants.Key key = Minecraft.getInstance().options.keyAttack.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return AllKeys.isMouseButtonDown(key.getValue());
        } else {
            return AllKeys.isKeyDown(key.getValue());
        }
    }

    private static void refreshMode() {
        if (mode == Mode.IDLE) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        ItemStack heldItem = player.getMainHandItem();

        if (player.isSpectator()) {
            mode = Mode.IDLE;
            onReset();
            return;
        }

        if (!AllContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
            heldItem = player.getOffhandItem();
            if (!AllContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
                mode = Mode.IDLE;
                onReset();
                return;
            }
        }

        if (mc.screen != null) {
            mode = Mode.IDLE;
            onReset();
            return;
        }

        if (InputConstants.isKeyDown(mc.getWindow()
                .getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
            mode = Mode.IDLE;
            onReset();
            return;
        }
    }

    private static void onReset() {
        DepositScannerItemRenderer.resetWheel();
    }
}
