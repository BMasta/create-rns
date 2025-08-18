package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.bmaster.createrns.util.Utils;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import org.lwjgl.glfw.GLFW;
import com.bmaster.createrns.item.DepositScanner.DepositScannerItemRenderer.AntennaStatus;

public class DepositScannerClientHandler {
    public static AntennaStatus antennaStatus = AntennaStatus.INACTIVE;
    public static Mode mode = Mode.IDLE;

    public enum Mode {
        IDLE, ACTIVE
    }

    public static final int MIN_PING_INTERVAL = 3;
    public static final int MAX_PING_INTERVAL = 60;

    private static int selectedIndex = 0;
    private static int pingInterval = MAX_PING_INTERVAL;
    private static int ticksSinceLastPing = 0;
    private static boolean selectionLocked = false;
    private static boolean pingResultPending = false;
    private static ChunkPos occupiedOreChunk;


    public static void toggle() {
        if (mode == Mode.IDLE) {
            mode = Mode.ACTIVE;
        } else {
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

        if (selectionLocked) {
            ticksSinceLastPing = pingInterval; // Ping right away
            AllSoundEvents.CONTROLLER_CLICK.playAt(p.level(), p.blockPosition(), 1f, .75f, true);
        } else {
            occupiedOreChunk = null; // Invalidate cached chunk if present
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
        if (pingResultPending) processPing();
        DepositScannerItemRenderer.tick();
        refreshMode();

        var p = Minecraft.getInstance().player;
        if (mode != Mode.ACTIVE || !selectionLocked || p == null) return;

        ticksSinceLastPing++;
        if (ticksSinceLastPing >= pingInterval) {
            ticksSinceLastPing = 0;

            boolean inOreChunk = (occupiedOreChunk != null) && Utils.isPosInChunk(p.blockPosition(), occupiedOreChunk);

            // Invalidate cached chunk if we left its vicinity
            if (occupiedOreChunk != null && !inOreChunk) {
                occupiedOreChunk = null;
            }

            if (occupiedOreChunk == null) {
                DepositScannerItemRenderer.powerOff();
                pingForItem();
            } else {
                DepositScannerItemRenderer.powerOn();
            }
        }
    }

    public static ItemStack getSelectedItem() {
        if (ServerConfig.OVERWORLD_ORES.isEmpty()) return ItemStack.EMPTY;
        int size = ServerConfig.OVERWORLD_ORES.size();
        int normalizedIndex = (selectedIndex % size + size) % size;
        return new ItemStack(ServerConfig.OVERWORLD_ORES.get(normalizedIndex));
    }

    private static void pingForItem() {
        DepositScannerC2SPacket.send(getSelectedItem().getItem());
    }

    protected static void setPingResult(AntennaStatus status, int interval, boolean found) {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide()) return;

        pingResultPending = true;
        antennaStatus = status;
        pingInterval = (antennaStatus == AntennaStatus.INACTIVE) ? MAX_PING_INTERVAL : interval;

        if (found) {
            pingInterval = MIN_PING_INTERVAL;
            antennaStatus = AntennaStatus.BOTH_ACTIVE;

            // Cache discovered ore chunk
            if (occupiedOreChunk == null || !Utils.isPosInChunk(p.blockPosition(), occupiedOreChunk)) {
                occupiedOreChunk = new ChunkPos(p.blockPosition());
            }
        }
    }

    private static void processPing() {
        if (!pingResultPending) return;
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide()) return;

        pingResultPending = false;

        // Render as powered
        DepositScannerItemRenderer.powerFor(2);

        if (occupiedOreChunk != null && Utils.isPosInChunk(p.blockPosition(), occupiedOreChunk)) {
            AllSoundEvents.FWOOMP.playAt(p.level(), p.blockPosition(), 1f, 2f, true);
        } else {
            // Play ding
            int max = MAX_PING_INTERVAL - MIN_PING_INTERVAL;
            float pitchMultiplier = 1 - ((float) (pingInterval - MIN_PING_INTERVAL) / max);
            AllSoundEvents.CONFIRM_2.playAt(p.level(), p.blockPosition(), 1f,
                    0.8f + 0.4f * pitchMultiplier, true);
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
        ticksSinceLastPing = pingInterval; // Ping as soon as scanner becomes active
        occupiedOreChunk = null;
        DepositScannerItemRenderer.resetWheel();
    }
}
