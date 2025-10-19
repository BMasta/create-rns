//package com.bmaster.createrns.event;
//
//import com.bmaster.createrns.RNSContent;
//import com.bmaster.createrns.CreateRNS;
//import com.bmaster.createrns.mining.MiningAreaOutlineRenderer;
//import com.bmaster.createrns.deposit.capability.*;
//import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler;
//import net.minecraft.client.Minecraft;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
//import net.minecraftforge.client.event.InputEvent;
//import net.minecraftforge.event.TickEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
//public class ClientForgeEvents {
//    @SubscribeEvent
//    public static void clientTick(TickEvent.ClientTickEvent event) {
//        if (event.phase == TickEvent.Phase.START) {
//            DepositScannerClientHandler.tick();
//            MiningAreaOutlineRenderer.tick();
//        }
//    }
//
//    @SubscribeEvent
//    public static void onScrollInput(InputEvent.MouseScrollingEvent e) {
//        var mc = Minecraft.getInstance();
//        var p = mc.player;
//        if (mc.player != null && mc.screen == null) {
//            var mainItem = p.getMainHandItem();
//            var offItem = p.getOffhandItem();
//            var scrollDelta = e.getScrollDelta();
//
//            // Scanner - sneaking
//            if (p.level().isClientSide() && p.isShiftKeyDown() && (mainItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get()) ||
//                    offItem.is(RNSContent.DEPOSIT_SCANNER_ITEM.get()))) {
//                if (scrollDelta > 0) {
//                    DepositScannerClientHandler.scrollUp();
//                } else if (scrollDelta < 0) {
//                    DepositScannerClientHandler.scrollDown();
//                }
//                e.setCanceled(true);
//            }
//        }
//    }
//
//    @SubscribeEvent
//    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
//        DepositScannerClientHandler.clearState();
//        MiningAreaOutlineRenderer.clearOutline();
//    }
//}
