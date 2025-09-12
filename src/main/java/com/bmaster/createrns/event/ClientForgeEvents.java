package com.bmaster.createrns.event;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.item.DepositScanner.DepositScannerServerHandler;
import com.bmaster.createrns.mining.MiningAreaOutlineRenderer;
import com.bmaster.createrns.deposit.capability.*;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {
    private static final int SCANNER_INTERACT_COOLDOWN = 5;
    // This is also enforced on the server side
    private static final int SCANNER_DISCOVER_COOLDOWN = DepositIndex.MIN_COMPUTE_INTERVAL + 10;
    private static long scannerLastLeftClickedAt = 0;
    private static long scannerLastRightClickedAt = 0;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            DepositScannerClientHandler.tick();
            MiningAreaOutlineRenderer.tick();
        }
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered e) {
        var mc = Minecraft.getInstance();
        var p = mc.player;

        if (p != null && mc.screen == null) {
            var l = p.level();
            var t = l.getGameTime();
            var mainItem = p.getMainHandItem();
            var offItem = p.getOffhandItem();

            // Scanner
            if (l.isClientSide() && (mainItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get()) ||
                    offItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get()))) {
                if (e.isUseItem()) {
                    e.setSwingHand(false);
                    e.setCanceled(true);
                    if (scannerLastRightClickedAt + SCANNER_INTERACT_COOLDOWN < t) {
                        scannerLastRightClickedAt = t;
                        DepositScannerClientHandler.toggle();
                    }
                }
            }

            // Scanner - main hand
            if (l.isClientSide() && mainItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get())) {
                if (e.isAttack() && DepositScannerClientHandler.getMode() == DepositScannerClientHandler.Mode.ACTIVE) {
                    e.setSwingHand(false);
                    e.setCanceled(true);
                    if (scannerLastLeftClickedAt + SCANNER_DISCOVER_COOLDOWN < t) {
                        scannerLastLeftClickedAt = t;
                        DepositScannerClientHandler.discoverDeposit();
                        p.getCooldowns().addCooldown(RNSContent.DEPOSIT_SCANNER_ITEM.get(), SCANNER_DISCOVER_COOLDOWN);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onScrollInput(InputEvent.MouseScrollingEvent e) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (mc.player != null && mc.screen == null) {
            var mainItem = p.getMainHandItem();
            var offItem = p.getOffhandItem();

            // Scanner
            if (p.level().isClientSide() && (mainItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get()) ||
                    offItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get()))) {
                // Handle scroll when active
                if (DepositScannerClientHandler.getMode() == DepositScannerClientHandler.Mode.ACTIVE) {
                    if (e.getScrollDelta() > 0) {
                        DepositScannerClientHandler.scrollUp();
                    } else {
                        DepositScannerClientHandler.scrollDown();
                    }
                    e.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        scannerLastLeftClickedAt = 0;
        scannerLastRightClickedAt = 0;
        DepositScannerClientHandler.clearState();
        MiningAreaOutlineRenderer.clearOutline();
    }
}
