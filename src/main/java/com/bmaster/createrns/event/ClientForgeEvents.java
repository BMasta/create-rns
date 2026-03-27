package com.bmaster.createrns.event;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSItems;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer.ToggleLocation;
import com.bmaster.createrns.content.deposit.claiming.DepositClaimerOutlineRenderer;
import com.bmaster.createrns.content.deposit.info.FoundDepositClientCache;
import com.bmaster.createrns.content.deposit.info.sync.FoundDepositsSnapshotC2SPacket;
import com.bmaster.createrns.content.deposit.mining.MinerEffectsGenerator;
import com.bmaster.createrns.content.deposit.scanning.DepositIconsC2SPacket;
import com.bmaster.createrns.content.deposit.scanning.DepositScannerClientHandler;
import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import journeymap.client.ui.fullscreen.Fullscreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import xaero.map.gui.GuiMap;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mod.EventBusSubscriber(modid = CreateRNS.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            DepositScannerClientHandler.tick();
            DepositClaimerOutlineRenderer.tick();
            MinerEffectsGenerator.globalTick();
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn e) {
        FoundDepositsSnapshotC2SPacket.send();
        DepositIconsC2SPacket.send();
    }

    @SubscribeEvent
    public static void onScrollInput(InputEvent.MouseScrollingEvent e) {
        var mc = Minecraft.getInstance();
        var p = mc.player;
        if (mc.player != null && mc.screen == null) {
            var mainItem = p.getMainHandItem();
            var offItem = p.getOffhandItem();
            var scrollDelta = e.getScrollDelta();

            // Scanner - sneaking
            if (p.level().isClientSide() && p.isShiftKeyDown() && (mainItem.is(RNSItems.DEPOSIT_SCANNER.get()) ||
                    offItem.is(RNSItems.DEPOSIT_SCANNER.get()))) {
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
    public static void onMouseButtonPre(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        var mc = Minecraft.getInstance();
        var toggleLocations = ToggleLocation.infer();
        if (toggleLocations.isEmpty()) return;

        int width = 1, height = 1;
        boolean clickConsumed = false;
        for (var l : toggleLocations) {
            switch (l) {
                case XAERO -> {
                    if (mc.screen instanceof GuiMap mapScreen) {
                        width = mapScreen.width;
                        height = mapScreen.height;
                    }
                }
                case JOURNEY -> {
                    if (mc.screen instanceof Fullscreen screen) {
                        width = screen.width;
                        height = screen.height;
                    }
                }
            }
            if (width == -1 || height == -1) continue;
            var window = mc.getWindow();
            double mouseX = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
            if (RNSMapToggleRenderer.handleClick(width, height, mouseX, mouseY, event.getButton(), l)) {
                clickConsumed = true;
            }
        }

        if (clickConsumed) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var tooltip = event.getToolTip();
        var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        assert itemId != null;
        var alwaysTooltipKey = "item." + itemId.getNamespace() + "." + itemId.getPath() + ".tooltip.always";
        var descriptionBasedKey = stack.getDescriptionId() + ".tooltip.always";
        for (int i = 1; ; ++i) {
            if (I18n.exists(alwaysTooltipKey + i)) {
                tooltip.add(Component.translatable(alwaysTooltipKey + i));
            } else if (I18n.exists(descriptionBasedKey + i)) {
                tooltip.add(Component.translatable(descriptionBasedKey + i));
            } else {
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        FoundDepositClientCache.clear();
        DepositSpecLookup.setScannerIcons(Map.of());
        DepositScannerClientHandler.clearState();
        DepositClaimerOutlineRenderer.clearOutline();
        MinerEffectsGenerator.clearState();
    }
}
