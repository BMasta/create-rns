package com.bmaster.createrns.event;

import com.bmaster.createrns.AllContent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent.MouseScrollingEvent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {
    @SubscribeEvent
    public static void onTick(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            DepositScannerClientHandler.tick();
        }
    }



    @SubscribeEvent
    public static void onWorldScroll(MouseScrollingEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (mc.player.getMainHandItem().is(AllContent.DEPOSIT_SCANNER_ITEM.get()) ||
                mc.player.getOffhandItem().is(AllContent.DEPOSIT_SCANNER_ITEM.get())) {
            if (e.getScrollDelta() > 0) {
                DepositScannerClientHandler.scrollUp();
            } else {
                DepositScannerClientHandler.scrollDown();
            }
            if (DepositScannerClientHandler.MODE == DepositScannerClientHandler.Mode.ACTIVE) {
                e.setCanceled(true);
            }
        }

    }
}
