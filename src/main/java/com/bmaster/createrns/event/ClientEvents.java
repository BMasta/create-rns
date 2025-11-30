package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.RNSParticleTypes;
import com.bmaster.createrns.compat.ponder.RNSPonderPlugin;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerOutlineRenderer;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = CreateRNS.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        DepositScannerClientHandler.tick();
        DepositClaimerOutlineRenderer.tick();
    }

    @SubscribeEvent
    public static void clientInit(final FMLClientSetupEvent event) {
        RNSPonderPlugin.register();
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
        DepositClaimerOutlineRenderer.clearOutline();
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        RNSParticleTypes.RESONANCE.register(RNSParticleTypes.RESONANCE_TYPE.get(), event);
        RNSParticleTypes.STABILIZING_RESONANCE.register(RNSParticleTypes.STABILIZING_RESONANCE_TYPE.get(), event);
        RNSParticleTypes.SHATTERING_RESONANCE.register(RNSParticleTypes.SHATTERING_RESONANCE_TYPE.get(), event);
    }
}
