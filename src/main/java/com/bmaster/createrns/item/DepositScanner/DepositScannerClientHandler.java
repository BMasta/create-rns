package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.deposit.spec.DepositSpecLookup;
import com.bmaster.createrns.RNSSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static com.bmaster.createrns.item.DepositScanner.DepositScannerServerHandler.*;

public class DepositScannerClientHandler {
    public enum AntennaStatus {
        INACTIVE, LEFT_ACTIVE, RIGHT_ACTIVE, BOTH_ACTIVE
    }

    public enum DepositProximity {
        AWAY, FOUND, NEAR, LEFT
    }

    private static State state = new State();

    public static AntennaStatus getAntennaStatus() {
        return state.antennaStatus;
    }

    public static DepositProximity getDepositProximity() {
        return state.depProximity;
    }

    public static boolean isTracking() {
        return state.isTracking;
    }

    public static void cancelTracking() {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        if (!state.isTracking) return;
        RNSSoundEvents.SCANNER_CLICK.playInHand(p.level(), p.blockPosition());
        state.isTracking = false;
    }

    public static void discoverDeposit() {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        // Server limits how often it processes discover requests. It will be a no-op if called too soon.
        DepositScannerC2SPacket.send(getSelectedItem().getItem(), RequestType.DISCOVER);
        RNSSoundEvents.SCANNER_CLICK.playInHand(p.level(), p.blockPosition());
        RNSSoundEvents.SCANNER_DISCOVERY_PING.playInHand(p.level(), p.blockPosition());
    }

    public static void scrollDown() {
        state.selectedIndex++;
        DepositScannerItemRenderer.scrollDown();
        afterScroll();
    }

    public static void scrollUp() {
        state.selectedIndex--;
        DepositScannerItemRenderer.scrollUp();
        afterScroll();
    }

    public static void clearState() {
        state = new State();
    }

    public static void tick() {
        if (state.trackingStateUpdatePending) processTrackingStateUpdate();
        DepositScannerItemRenderer.tick();

        var mc = Minecraft.getInstance();
        if (mc.isPaused()) return;

        var p = mc.player;
        if (p == null || p.isSpectator()) return;

        // Make sure we are holding the scanner
        ItemStack heldItem = p.getMainHandItem();
        if (!RNSContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
            heldItem = p.getOffhandItem();
            if (!RNSContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
                DepositScannerItemRenderer.resetWheel();
                return;
            }
        }

        // Deposit found
        if (state.cachedDepositPos != null) {
            if (Math.sqrt(p.blockPosition().distSqr(state.cachedDepositPos)) <= FOUND_DISTANCE) {
                // Still within close proximity of the deposit
                return;
            } else {
                // Far enough away to reset
                state.depProximity = DepositProximity.LEFT;
                state.cachedDepositPos = null;
                state.isTracking = false;
            }
        }

        if (!state.isTracking) return;

        // Send tracking request to server
        state.ticksSinceLastPing++;
        if (state.ticksSinceLastPing >= state.pingInterval) {
            state.ticksSinceLastPing = 0;
            DepositScannerC2SPacket.send(getSelectedItem().getItem(), RequestType.TRACK);
        }
    }

    public static ItemStack getSelectedItem() {
        var l = Minecraft.getInstance().level;
        if (l == null) return ItemStack.EMPTY;
        var allItems = DepositSpecLookup.getAllScannerIcons(l.registryAccess());
        int size = allItems.size();
        int normalizedIndex = (state.selectedIndex % size + size) % size;
        return new ItemStack(allItems.get(normalizedIndex));
    }

    protected static void processDiscoverReply(AntennaStatus status) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        state.isTracking = status != AntennaStatus.INACTIVE;
        state.pingInterval = MAX_PING_INTERVAL;
        state.trackingStateUpdatePending = false;

        if (state.isTracking) {
            RNSSoundEvents.SCANNER_DISCOVERY_SUCCESS.playInHand(p.level(), p.blockPosition());
        }
    }

    protected static void processTrackingReply(AntennaStatus status, int interval, @Nullable BlockPos foundDepositCenter) {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide()) return;
        if (status == AntennaStatus.INACTIVE) {
            state.isTracking = false;
            return;
        }

        state.antennaStatus = status;
        state.pingInterval = interval;
        state.cachedDepositPos = foundDepositCenter;

        // Delay ping result processing so it can be synchronized with the renderer
        state.trackingStateUpdatePending = true;

        if (foundDepositCenter != null) {
            // We close enough to the deposit to consider it found
            state.pingInterval = MIN_PING_INTERVAL;
            state.antennaStatus = AntennaStatus.BOTH_ACTIVE;
            if (state.depProximity == DepositProximity.AWAY) state.depProximity = DepositProximity.FOUND;
        } else {
            if (state.depProximity == DepositProximity.NEAR) state.depProximity = DepositProximity.LEFT;
        }
    }

    private static void processTrackingStateUpdate() {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide()) return;

        state.trackingStateUpdatePending = false;

        switch (state.depProximity) {
            case FOUND -> {
                // FWOOMP!
                RNSSoundEvents.DEPOSIT_FOUND.playInHand(p.level(), p.blockPosition());
                state.depProximity = DepositProximity.NEAR;
            }
            case LEFT -> {
                state.ticksSinceLastPing = state.pingInterval; // Ping as soon as possible
                state.depProximity = DepositProximity.AWAY;
            }
            case AWAY -> {
                // Render as powered for a brief moment
                DepositScannerItemRenderer.powerBriefly();
                // Play ding
                int max = MAX_PING_INTERVAL - MIN_PING_INTERVAL;
                float pitchMultiplier = 1 - ((float) (state.pingInterval - MIN_PING_INTERVAL) / max);
                RNSSoundEvents.SCANNER_TRACKING_PING.playInHand(p.level(), p.blockPosition(), 1f,
                        0.8f + 0.4f * pitchMultiplier, true);
            }
        }
    }

    private static void afterScroll() {
        state.isTracking = false;

        if (state.cachedDepositPos != null) {
            state.cachedDepositPos = null;
            state.depProximity = DepositProximity.LEFT;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            RNSSoundEvents.SCANNER_SCROLL.playInHand(player.level(), player.blockPosition());
        }
    }

    private static class State {
        private AntennaStatus antennaStatus = AntennaStatus.INACTIVE;
        private int pingInterval = MAX_PING_INTERVAL;
        private DepositProximity depProximity = DepositProximity.AWAY;
        private int selectedIndex = 0;
        private int ticksSinceLastPing = pingInterval;
        private BlockPos cachedDepositPos = null;
        private boolean trackingStateUpdatePending = false;
        private boolean isTracking = false;
    }
}
