package com.bmaster.createrns.event;

import com.bmaster.createrns.RNSContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.mining.MiningAreaOutlineRenderer;
import com.bmaster.createrns.deposit.capability.*;
import com.bmaster.createrns.deposit.spec.DepositSpecLookup;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    private static final int SCANNER_INTERACT_COOLDOWN = 5;
    private static long scannerLastRightClickedAt = 0;

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            DepositScannerClientHandler.tick();
            MiningAreaOutlineRenderer.tick();
        }
    }

    @SubscribeEvent
    public static void onAttachCaps(AttachCapabilitiesEvent<Level> event) {
        if (!(event.getObject() instanceof ServerLevel sl)) return;

        event.addCapability(
                ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "deposit_index"),
                new DepositIndexProvider()
        );
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
                // Cancel use animations and usage of item in other hand, toggle scanner mode
                if (e.isUseItem()) {
                    e.setSwingHand(false);
                    e.setCanceled(true);
                    if (scannerLastRightClickedAt + SCANNER_INTERACT_COOLDOWN < t) {
                        scannerLastRightClickedAt = t;
                        DepositScannerClientHandler.toggle();
                        p.getCooldowns().addCooldown(RNSContent.DEPOSIT_SCANNER_ITEM.get(), 2);
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
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel sl)) return;
        var depIdxOpt = IDepositIndex.fromLevel(sl).resolve();
        if (depIdxOpt.isEmpty()) return;
        var depIdx = depIdxOpt.get();

        ChunkPos pos = e.getChunk().getPos();
        var sm = sl.structureManager();

        for (var start : sm.startsForStructure(pos, DepositSpecLookup.isDeposit(sl.registryAccess()))) {
            sl.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .getResourceKey(start.getStructure())
                    .ifPresent(structKey -> depIdx.add(structKey, start, sl));
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut e) {
        scannerLastRightClickedAt = 0;
        DepositScannerClientHandler.clearState();
        MiningAreaOutlineRenderer.clearOutline();
    }
}
