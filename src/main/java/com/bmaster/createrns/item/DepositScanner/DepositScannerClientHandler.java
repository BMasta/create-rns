package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.Utils;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllKeys;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.lwjgl.glfw.GLFW;
import com.bmaster.createrns.item.DepositScanner.DepositScannerItemRenderer.AntennaStatus;

public class DepositScannerClientHandler {
    public static Mode mode = Mode.IDLE;

    public enum Mode {
        IDLE, ACTIVE
    }

    private static final int DEFAULT_PING_INTERVAL = 40;
    private static int selectedIndex = 0;
    private static int pingInterval = DEFAULT_PING_INTERVAL;
    private static int ticksSinceLastPing = 0;
    private static boolean selectionLocked = false;
    private static ChunkPos occupiedOreChunk;


    public static void toggle() {
        if (mode == Mode.IDLE) {
            CreateRNS.LOGGER.info("Mode is now active");
            mode = Mode.ACTIVE;
        } else {
            CreateRNS.LOGGER.info("Mode is now idle");
            mode = Mode.IDLE;
            onReset();
        }
    }

    public static boolean isSelectionLocked() {
        return selectionLocked;
    }

    public static void toggleSelectionLocked() {
        var p = Minecraft.getInstance().player;
        if (mode != Mode.ACTIVE || p == null) return;
        selectionLocked = !selectionLocked;
        CreateRNS.LOGGER.info("Selection toggled {} -> {}", !selectionLocked, selectionLocked);

        if (selectionLocked) {
            ticksSinceLastPing = pingInterval; // Ping right away
            AllSoundEvents.CONTROLLER_CLICK.playAt(p.level(), p.blockPosition(), 1f, .75f, true);
        } else {
            AllSoundEvents.CONTROLLER_CLICK.playAt(p.level(), p.blockPosition(), 1f, .5f, true);
        }
    }

    public static void scrollDown() {
        if (mode != Mode.ACTIVE || selectionLocked) return;

        selectedIndex++;
        DepositScannerItemRenderer.scrollDown();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            AllSoundEvents.SCROLL_VALUE.playAt(player.level(), player.blockPosition(), 1f, 1f, true);
        }
    }

    public static void scrollUp() {
        if (mode != Mode.ACTIVE || selectionLocked) return;

        selectedIndex--;
        DepositScannerItemRenderer.scrollUp();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            AllSoundEvents.SCROLL_VALUE.playAt(player.level(), player.blockPosition(), 1f, 1f, true);
        }
    }

    public static void tick() {
        DepositScannerItemRenderer.tick();
        refreshMode();

        if (mode != Mode.ACTIVE || !selectionLocked) return;
        var p = Minecraft.getInstance().player;

        // We're in an ore chunk, ping as soon as we leave
        if (occupiedOreChunk != null && p != null && Utils.isPosInChunk(p.blockPosition(), occupiedOreChunk)) {
            ticksSinceLastPing = pingInterval;
            return;
        }

        // We're not in an ore chunk, business as usual
        ticksSinceLastPing++;
        if (ticksSinceLastPing >= pingInterval) {
            ticksSinceLastPing = 0;
            occupiedOreChunk = null;
            pingForItem();
        }
    }

    public static ItemStack getSelectedItem() {
        if (ServerConfig.OVERWORLD_ORES.isEmpty()) return ItemStack.EMPTY;
        int size = ServerConfig.OVERWORLD_ORES.size();
        int normalizedIndex = (selectedIndex % size + size) % size;
        return new ItemStack(ServerConfig.OVERWORLD_ORES.get(normalizedIndex));
    }

    public static boolean isAttackPressed() {
        InputConstants.Key key = Minecraft.getInstance().options.keyAttack.getKey();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return AllKeys.isMouseButtonDown(key.getValue());
        } else {
            return AllKeys.isKeyDown(key.getValue());
        }
    }

    private static void pingForItem() {
        DepositScannerC2SPacket.send(getSelectedItem().getItem());
    }

    protected static void setPingResult(AntennaStatus antennaStatus, int interval, boolean found) {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide()) return;

        if (found) {
            // Yoink
            occupiedOreChunk = new ChunkPos(p.blockPosition());
            interval = DEFAULT_PING_INTERVAL;
            antennaStatus = AntennaStatus.BOTH_ACTIVE;
            Minecraft.getInstance().player.displayClientMessage(Component.literal(
                    "Found ore chunk"), false);
            return;
        }

        pingInterval = (antennaStatus == AntennaStatus.INACTIVE) ? DEFAULT_PING_INTERVAL : interval;

        switch (antennaStatus) {
            case INACTIVE -> Minecraft.getInstance().player.displayClientMessage(Component.literal(
                    "No signal"), false);
            case LEFT_ACTIVE -> Minecraft.getInstance().player.displayClientMessage(Component.literal(
                    "Signal on the left"), false);
            case RIGHT_ACTIVE -> Minecraft.getInstance().player.displayClientMessage(Component.literal(
                    "Signal on the right"), false);
            case BOTH_ACTIVE -> Minecraft.getInstance().player.displayClientMessage(Component.literal(
                    "Signal up ahead"), false);
        }

        AllSoundEvents.CONFIRM.playAt(p.level(), p.blockPosition(), 1f, 1f, true);
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
        ticksSinceLastPing = pingInterval; // Ping as soon as scanner becomes active
        occupiedOreChunk = null;
        DepositScannerItemRenderer.resetWheel();
    }
}
