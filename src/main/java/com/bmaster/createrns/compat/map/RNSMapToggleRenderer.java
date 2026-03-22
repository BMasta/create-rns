package com.bmaster.createrns.compat.map;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.RNSGuiTextures;
import com.bmaster.createrns.RNSItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSMapToggleRenderer {
    public static final ItemStack DEPOSITS_DISABLED = new ItemStack(RNSItems.FAKE_UNPOWERED_DEPOSIT_SCANNER.get());
    public static final ItemStack DEPOSITS_ENABLED = new ItemStack(RNSItems.FAKE_POWERED_DEPOSIT_SCANNER.get());

    private static boolean overlayEnabled = true;

    private RNSMapToggleRenderer() {
    }

    public static boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public static void render(GuiGraphics gui, Screen screen, int mouseX, int mouseY, ToggleLocation location) {
        renderWidget(gui, location);
        if (!isHovered(mouseX, mouseY, location)) return;

        var pose = gui.pose();
        pose.pushPose();
        pose.translate(0, 0, 1400);
        RemovedGuiUtils.drawHoveringText(gui,
                List.of(CreateRNS.translatable("map.toggle")),
                mouseX, mouseY + 5, screen.width, screen.height, 256, Minecraft.getInstance().font);
        pose.popPose();
    }

    public static boolean handleClick(double mouseX, double mouseY, int button, ToggleLocation location) {
        if (button != 0 || !isHovered(mouseX, mouseY, location)) return false;
        overlayEnabled = !overlayEnabled;
        return true;
    }

    private static void renderWidget(GuiGraphics gui, ToggleLocation location) {
        RenderSystem.enableBlend();
        float scale = 0.75f;
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(location.x, location.y, 1000);
        RNSGuiTextures.DEPOSIT_MAP_TOGGLE_BG.render(gui, 0, 0);
        pose.translate(5, 5, 0);
        pose.scale(scale, scale, scale);
        pose.translate(-5, -5, 900);
        gui.renderItem(overlayEnabled ? DEPOSITS_ENABLED : DEPOSITS_DISABLED, 0, 0);
        pose.popPose();
    }

    private static boolean isHovered(double mouseX, double mouseY, ToggleLocation location) {
        return mouseX >= location.x
                && mouseX < location.x + 15
                && mouseY >= location.y
                && mouseY < location.y + 15;
    }

    public enum ToggleLocation {
        XAERO(3, 193),
        JOURNEY(7, 76);

        public final int x;
        public final int y;

        ToggleLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
