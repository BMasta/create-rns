package com.bmaster.createrns.event;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.capability.orechunkdata.OreChunkDataProvider;
import com.bmaster.createrns.item.DepositScanner.DepositScannerClientHandler;
import com.bmaster.createrns.item.DepositScanner.DepositScannerItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            DepositScannerClientHandler.tick();
        }
    }

    @SubscribeEvent
    public static void onAttachCaps(AttachCapabilitiesEvent<LevelChunk> event) {
        LevelChunk chunk = event.getObject();
        if (chunk.getLevel().isClientSide()) return;

        event.addCapability(
                ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "ore_chunk_data"),
                new OreChunkDataProvider(event.getObject(), true)
        );
    }

    @SubscribeEvent
    public static void onClickInput(InputEvent.InteractionKeyMappingTriggered e) {
        var mc = Minecraft.getInstance();
        var p = mc.player;

        if (p != null && mc.screen == null) {
            var mainItem = p.getMainHandItem();
            var offItem = p.getOffhandItem();

            // Scanner
            if (mainItem.is(AllContent.DEPOSIT_SCANNER_ITEM.get()) ||
                    offItem.is(AllContent.DEPOSIT_SCANNER_ITEM.get())) {
                // Cancel attack event
                if (e.isAttack() && DepositScannerClientHandler.mode == DepositScannerClientHandler.Mode.ACTIVE) {
                    e.setSwingHand(false);
                    e.setCanceled(true);
                }
                // Cancel use animations and usage of item in other hand, toggle scanner mode
                if (e.isUseItem()) {
                    e.setSwingHand(false);
                    e.setCanceled(true);
                    if (p.level().isClientSide) DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> DepositScannerClientHandler::toggle);
                    p.getCooldowns().addCooldown(AllContent.DEPOSIT_SCANNER_ITEM.get(), 2);
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
            if (mainItem.is(AllContent.DEPOSIT_SCANNER_ITEM.get()) ||
                    offItem.is(AllContent.DEPOSIT_SCANNER_ITEM.get())) {
                // Handle scroll when active
                if (DepositScannerClientHandler.mode == DepositScannerClientHandler.Mode.ACTIVE) {
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
}
