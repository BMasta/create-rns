package com.bmaster.createrns.item.DepositScanner;

import com.bmaster.createrns.AllContent;
import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.infrastructure.ServerConfig;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = CreateRNS.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DepositScannerClientHandler {
    public static Mode MODE = Mode.IDLE;

    public enum Mode {
        IDLE, ACTIVE
    }

    private static int selectedIndex = 0;

    public static void toggle() {
        if (MODE == Mode.IDLE) {
            MODE = Mode.ACTIVE;
        } else {
            MODE = Mode.IDLE;
            onReset();
        }
    }

    public static void scrollDown() {
        if (MODE != Mode.IDLE) {
            selectedIndex++;
            DepositScannerItemRenderer.scrollDown();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                AllSoundEvents.SCROLL_VALUE.playAt(player.level(), player.blockPosition(), 1f, 1f, true);
            }
        }
    }

    public static void scrollUp() {
        if (MODE != Mode.IDLE) {
            selectedIndex--;
            DepositScannerItemRenderer.scrollUp();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                AllSoundEvents.SCROLL_VALUE.playAt(player.level(), player.blockPosition(), 1f, 1f, true);
            }
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        DepositScannerItemRenderer.tick();
        refreshMode();
    }

    protected static ItemStack getSelectedItem() {
        if (ServerConfig.OVERWORLD_ORES.isEmpty()) return ItemStack.EMPTY;
        int size = ServerConfig.OVERWORLD_ORES.size();
        int normalizedIndex = (selectedIndex % size + size) % size;
        return new ItemStack(ServerConfig.OVERWORLD_ORES.get(normalizedIndex));
    }

    private static void refreshMode() {
        if (MODE == Mode.IDLE) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        ItemStack heldItem = player.getMainHandItem();

        if (player.isSpectator()) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        if (!AllContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
            heldItem = player.getOffhandItem();
            if (!AllContent.DEPOSIT_SCANNER_ITEM.isIn(heldItem)) {
                MODE = Mode.IDLE;
                onReset();
                return;
            }
        }

        if (mc.screen != null) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }

        if (InputConstants.isKeyDown(mc.getWindow()
                .getWindow(), GLFW.GLFW_KEY_ESCAPE)) {
            MODE = Mode.IDLE;
            onReset();
            return;
        }
    }

    private static void onReset() {
        DepositScannerItemRenderer.resetWheel();
    }
}
