package com.bmaster.createrns.event;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler;
import com.bmaster.createrns.mining.MiningAreaOutlineRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = CreateRNS.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        DepositScannerClientHandler.tick();
        MiningAreaOutlineRenderer.tick();
    }

    @SubscribeEvent
    public static void onScrollInput(InputEvent.MouseScrollingEvent e) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (mc.player != null && mc.screen == null) {
            var mainItem = p.getMainHandItem();
            var offItem = p.getOffhandItem();
            var scrollDelta = e.getScrollDeltaY();

            // Scanner - sneaking
            if (p.level().isClientSide() && p.isShiftKeyDown() && (mainItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get()) ||
                    offItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get()))) {
                if (scrollDelta > 0) {
                    DepositScannerClientHandler.scrollUp();
                } else if (scrollDelta < 0) {
                    DepositScannerClientHandler.scrollDown();
                }
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        DepositScannerClientHandler.clearState();
        MiningAreaOutlineRenderer.clearOutline();
    }
}
