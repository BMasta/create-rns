package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSItems;
import com.bmaster.createrns.RNSParticleTypes;
import com.bmaster.createrns.compat.ponder.RNSPonderPlugin;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerOutlineRenderer;
import com.bmaster.createrns.content.deposit.mining.MinerEffectsGenerator;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = CreateRNS.ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        DepositScannerClientHandler.tick();
        DepositClaimerOutlineRenderer.tick();
        MinerEffectsGenerator.globalTick();
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
            if (p.level().isClientSide() && p.isShiftKeyDown() && (mainItem.is(RNSItems.DEPOSIT_SCANNER_ITEM.get()) ||
                    offItem.is(RNSItems.DEPOSIT_SCANNER_ITEM.get()))) {
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
        MinerEffectsGenerator.clearState();
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        RNSParticleTypes.RESONANCE.register(RNSParticleTypes.RESONANCE_TYPE.get(), event);
        RNSParticleTypes.STABILIZING_RESONANCE.register(RNSParticleTypes.STABILIZING_RESONANCE_TYPE.get(), event);
        RNSParticleTypes.SHATTERING_RESONANCE.register(RNSParticleTypes.SHATTERING_RESONANCE_TYPE.get(), event);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var tooltip = event.getToolTip();
        var itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var alwaysTooltipKey = "item." + itemId.getNamespace() + "." + itemId.getPath() + ".tooltip.always";
        var descriptionBasedKey = stack.getDescriptionId() + ".tooltip.always";
        for (int i = 1;;++i) {
            if (I18n.exists(alwaysTooltipKey + i)) {
                tooltip.add(Component.translatable(alwaysTooltipKey + i));
            } else if (I18n.exists(descriptionBasedKey + i)) {
                tooltip.add(Component.translatable(descriptionBasedKey + i));
            } else {
                break;
            }
        }
    }
}
