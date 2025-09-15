package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.deposit.spec.DepositSpecLookup;
import com.bmaster.createrns.RNSSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static com.bmaster.createrns.item.DepositScanner.DepositScannerServerHandler.*;

public class DepositScannerClientHandler {
    public enum AntennaStatus {
        INACTIVE, LEFT_ACTIVE, RIGHT_ACTIVE, BOTH_ACTIVE
    }

    private static State state = new State();

    public static AntennaStatus getAntennaStatus() {
        return state.antennaStatus;
    }

    public static boolean isTracking() {
        return state.isTracking;
    }

    public static boolean isDepositFound() {
        return state.depositFound;
    }

    public static void cancelTracking(boolean playSound) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        state.depositFound = false;
        state.isTracking = false;
        if (playSound) RNSSoundEvents.SCANNER_CLICK.playInHand(p.level(), p.blockPosition());
    }

    public static void discoverDeposit() {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        // Server limits how often it processes discover requests. It will be a no-op if called too soon.
        DepositScannerC2SPacket.send(getSelectedItem().getItem(), RequestType.DISCOVER);
        RNSSoundEvents.SCANNER_CLICK.playInHand(p.level(), p.blockPosition());
        RNSSoundEvents.SCANNER_DISCOVERY_PING.playInHand(p.level(), p.blockPosition());
        DepositScannerItemRenderer.shakeItem();
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

        if (state.depositFound || !state.isTracking) return;

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
        state.ticksSinceLastPing = 0;
        state.pingInterval = MAX_PING_INTERVAL;
        state.trackingStateUpdatePending = false;

        if (state.isTracking) {
            RNSSoundEvents.SCANNER_DISCOVERY_SUCCESS.playInHand(p.level(), p.blockPosition());
        }
    }

    protected static void processTrackingReply(AntennaStatus status, int interval, @Nullable BlockPos foundDepositCenter) {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide() || !state.isTracking) return;
        if (status == AntennaStatus.INACTIVE) {
            cancelTracking(false);
            return;
        }

        state.antennaStatus = status;
        state.pingInterval = interval;

        // Delay ping result processing so it can be synchronized with the renderer
        state.trackingStateUpdatePending = true;

        if (foundDepositCenter != null) {
            // We are close enough to the deposit to consider it found
            state.antennaStatus = AntennaStatus.BOTH_ACTIVE;
            state.depositFound = true;
        }
    }

    private static void processTrackingStateUpdate() {
        var p = Minecraft.getInstance().player;
        if (p == null || !p.level().isClientSide()) return;

        state.trackingStateUpdatePending = false;

        if (state.depositFound) {
            // FWOOMP!
            RNSSoundEvents.DEPOSIT_FOUND.playInHand(p.level(), p.blockPosition());
            DepositScannerItemRenderer.shakeItem();
            return;
        }

        // Render as powered for a brief moment
        DepositScannerItemRenderer.powerBriefly();
        // Play ding
        int max = MAX_PING_INTERVAL - MIN_PING_INTERVAL;
        float pitchMultiplier = 1 - ((float) (state.pingInterval - MIN_PING_INTERVAL) / max);
        RNSSoundEvents.SCANNER_TRACKING_PING.playInHand(p.level(), p.blockPosition(), 1f,
                0.8f + 0.4f * pitchMultiplier, true);
    }

    private static void afterScroll() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        cancelTracking(false);

        RNSSoundEvents.SCANNER_SCROLL.playInHand(player.level(), player.blockPosition());

        var dRL = DepositSpecLookup.getStructureKey(player.level().registryAccess(), getSelectedItem().getItem()).location();
        var dName = Component.translatable(dRL.getNamespace() + ".structure." + dRL.getPath());
        player.displayClientMessage(dName, true);
    }

    private static class State {
        private AntennaStatus antennaStatus = AntennaStatus.INACTIVE;
        private int pingInterval = MAX_PING_INTERVAL;
        private int selectedIndex = 0;
        private int ticksSinceLastPing = pingInterval;
        private boolean trackingStateUpdatePending = false;
        private boolean isTracking = false;
        private boolean depositFound = false;
    }
}
