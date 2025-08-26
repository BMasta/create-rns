package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.capability.depositindex.DepositSpecLookup;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import static com.bmaster.createrns.item.DepositScanner.DepositScannerServerHandler.*;

public class DepositScannerClientHandler {
    public enum Mode {
        IDLE, ACTIVE
    }

    public enum AntennaStatus {
        INACTIVE, LEFT_ACTIVE, RIGHT_ACTIVE, BOTH_ACTIVE
    }

    public enum DepositProximity {
        AWAY, FOUND, NEAR, LEFT
    }

    private static State state = new State();

    public static Mode getMode() {
        return state.mode;
    }

    public static AntennaStatus getAntennaStatus() {
        return state.antennaStatus;
    }

    public static DepositProximity getDepositProximity() {
        return state.depProximity;
    }

    public static void toggle() {
        if (state.mode == Mode.IDLE) {
            state.ticksSinceLastPing = state.pingInterval; // Ping as soon as possible
            state.needScanRecompute = true;
            state.mode = Mode.ACTIVE;
        } else {
            state.mode = Mode.IDLE;
            onIdle();
        }
    }

    public static void scrollDown() {
        if (state.mode != Mode.ACTIVE) return;
        state.selectedIndex++;
        DepositScannerItemRenderer.scrollDown();
        afterScroll();
    }

    public static void scrollUp() {
        if (state.mode != Mode.ACTIVE) return;
        state.selectedIndex--;
        DepositScannerItemRenderer.scrollUp();
        afterScroll();
    }

    public static void clearState() {
        state = new State();
    }

    public static void tick() {
        if (state.pingResultPending) processPing();
        DepositScannerItemRenderer.tick();
        refreshMode();

        var p = Minecraft.getInstance().player;
        if (state.mode != Mode.ACTIVE || p == null) return;


        if (state.cachedDepositPos != null) {
            if (Math.sqrt(p.blockPosition().distSqr(state.cachedDepositPos)) <= FOUND_DISTANCE) {
                // Still within close proximity of the deposit
                return;
            } else {
                // Far enough away to start pinging again
                state.depProximity = DepositProximity.LEFT;
                state.cachedDepositPos = null;
            }
        }

        state.ticksSinceLastPing++;
        if (state.ticksSinceLastPing >= state.pingInterval) {
            state.ticksSinceLastPing = 0;
            pingForItem();
        }
    }

    public static ItemStack getSelectedItem() {
        var allItems = DepositSpecLookup.getAllYields(Minecraft.getInstance().level);
        int size = allItems.size();
        int normalizedIndex = (state.selectedIndex % size + size) % size;
        return new ItemStack(allItems.get(normalizedIndex));
    }

    private static void pingForItem() {
        DepositScannerC2SPacket.send(getSelectedItem().getItem(), state.needScanRecompute);
    }

    protected static void processScanReply(AntennaStatus status, int interval, @Nullable BlockPos foundDepositCenter) {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide()) return;

        // Delay ping processing so it can be synchronized with the renderer
        state.pingResultPending = true;
        state.antennaStatus = status;
        state.pingInterval = (state.antennaStatus == AntennaStatus.INACTIVE) ? MAX_PING_INTERVAL : interval;
        state.needScanRecompute = (state.antennaStatus == AntennaStatus.INACTIVE);
        state.cachedDepositPos = foundDepositCenter;

        if (foundDepositCenter != null) {
            state.pingInterval = MIN_PING_INTERVAL;
            state.antennaStatus = AntennaStatus.BOTH_ACTIVE;
            if (state.depProximity == DepositProximity.AWAY) state.depProximity = DepositProximity.FOUND;
        } else {
            if (state.depProximity == DepositProximity.NEAR) state.depProximity = DepositProximity.LEFT;
        }
    }

    private static void processPing() {
        if (!state.pingResultPending) return;
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide()) return;

        state.pingResultPending = false;

        switch(state.depProximity) {
            case FOUND -> {
                // FWOOMP!
                AllSoundEvents.FWOOMP.playAt(p.level(), p.blockPosition(), 1f, 2f, true);
                state.depProximity = DepositProximity.NEAR;
            }
            case LEFT -> {
                state.ticksSinceLastPing = state.pingInterval; // Ping as soon as possible
                state.depProximity = DepositProximity.AWAY;
            }
            case AWAY -> {
                // Render as powered for a brief moment
                DepositScannerItemRenderer.powerFor(2);
                // Play ding
                int max = MAX_PING_INTERVAL - MIN_PING_INTERVAL;
                float pitchMultiplier = 1 - ((float) (state.pingInterval - MIN_PING_INTERVAL) / max);
                AllSoundEvents.CONFIRM_2.playAt(p.level(), p.blockPosition(), 1f,
                        0.8f + 0.4f * pitchMultiplier, true);
            }
        }
    }

    private static void afterScroll() {
        if (state.mode != Mode.ACTIVE) return;

        state.needScanRecompute = true;

        if (state.cachedDepositPos != null) {
            state.cachedDepositPos = null;
            state.depProximity = DepositProximity.LEFT;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            AllSoundEvents.SCROLL_VALUE.playAt(player.level(), player.blockPosition(), 1f, 1f, true);
        }
    }

    private static void refreshMode() {
        if (state.mode == Mode.IDLE) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        ItemStack heldItem = p.getMainHandItem();

        if (p.isSpectator()) {
            state.mode = Mode.IDLE;
            onIdle();
            return;
        }

        if (!AllContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
            heldItem = p.getOffhandItem();
            if (!AllContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
                state.mode = Mode.IDLE;
                onIdle();
                return;
            }
        }

        if (mc.screen != null) {
            state.mode = Mode.IDLE;
            onIdle();
            return;
        }

        if (InputConstants.isKeyDown(mc.getWindow()
                .getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
            state.mode = Mode.IDLE;
            onIdle();
            return;
        }
    }

    private static void onIdle() {
        state.ticksSinceLastPing = state.pingInterval; // Ping as soon as scanner becomes active
        DepositScannerItemRenderer.resetWheel();
    }

    private static class State {
        private Mode mode = Mode.IDLE;

        private AntennaStatus antennaStatus = AntennaStatus.INACTIVE;
        private int pingInterval = MAX_PING_INTERVAL;
        private DepositProximity depProximity = DepositProximity.AWAY;
        private int selectedIndex = 0;
        private int ticksSinceLastPing = pingInterval;
        private BlockPos cachedDepositPos = null;
        private boolean pingResultPending = false;
        private boolean needScanRecompute = true;
    }
}
