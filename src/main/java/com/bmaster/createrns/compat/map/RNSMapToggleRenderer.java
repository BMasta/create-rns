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

    private static final int ICON_OFFSET = 5;
    private static final float ICON_SCALE = 0.75f;

    private static boolean overlayEnabled = true;

    private RNSMapToggleRenderer() {
    }

    public static boolean isOverlayEnabled() {
        return overlayEnabled;
    }

    public static void render(GuiGraphics gui, Screen screen, int mouseX, int mouseY, ToggleLocation location) {
        int x = location.resolveX(screen.width);
        int y = location.resolveY(screen.height);

        renderWidget(gui, x, y);
        if (!isHovered(mouseX, mouseY, x, y)) return;

        var pose = gui.pose();
        pose.pushPose();
        pose.translate(0, 0, 1400);
        RemovedGuiUtils.drawHoveringText(gui,
                List.of(CreateRNS.translatable("map.toggle")),
                mouseX, mouseY + 5, screen.width, screen.height, 256, Minecraft.getInstance().font);
        pose.popPose();
    }

    public static boolean handleClick(double mouseX, double mouseY, int button, Screen screen, ToggleLocation location) {
        int x = location.resolveX(screen.width);
        int y = location.resolveY(screen.height);
        if (button != 0 || !isHovered(mouseX, mouseY, x, y)) return false;

        overlayEnabled = !overlayEnabled;
        return true;
    }

    private static void renderWidget(GuiGraphics gui, int x, int y) {
        RenderSystem.enableBlend();
        var pose = gui.pose();
        pose.pushPose();
        pose.translate(x, y, 1000);
        RNSGuiTextures.DEPOSIT_MAP_TOGGLE_BG.render(gui, 0, 0);
        pose.translate(ICON_OFFSET, ICON_OFFSET, 0);
        pose.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
        pose.translate(-ICON_OFFSET, -ICON_OFFSET, 900);
        gui.renderItem(overlayEnabled ? DEPOSITS_ENABLED : DEPOSITS_DISABLED, 0, 0);
        pose.popPose();
    }

    private static boolean isHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x
                && mouseX < x + ToggleLocation.TOGGLE_WIDTH
                && mouseY >= y
                && mouseY < y + ToggleLocation.TOGGLE_HEIGHT;
    }

    public enum Anchor {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM,
        CENTER
    }

    public enum ToggleLocation {
        XAERO(3, 64, Anchor.LEFT, Anchor.BOTTOM),
        JOURNEY(7, -50, Anchor.LEFT, Anchor.CENTER);

        public static final int TOGGLE_WIDTH = RNSGuiTextures.DEPOSIT_MAP_TOGGLE_BG.getWidth();
        public static final int TOGGLE_HEIGHT = RNSGuiTextures.DEPOSIT_MAP_TOGGLE_BG.getHeight();

        public final int x;
        public final int y;
        public final Anchor xAnchor;
        public final Anchor yAnchor;

        ToggleLocation(int x, int y, Anchor xAnchor, Anchor yAnchor) {
            this.x = x;
            this.y = y;
            this.xAnchor = xAnchor;
            this.yAnchor = yAnchor;
        }

        public int resolveX(int screenWidth) {
            return switch (xAnchor) {
                case LEFT -> x;
                case RIGHT -> screenWidth - TOGGLE_WIDTH - x;
                case CENTER -> (screenWidth - TOGGLE_WIDTH) / 2 + x;
                default -> throw new IllegalStateException("Unsupported x anchor: " + xAnchor);
            };
        }

        public int resolveY(int screenHeight) {
            return switch (yAnchor) {
                case TOP -> y;
                case BOTTOM -> screenHeight - TOGGLE_HEIGHT - y;
                case CENTER -> (screenHeight - TOGGLE_HEIGHT) / 2 + y;
                default -> throw new IllegalStateException("Unsupported y anchor: " + yAnchor);
            };
        }
    }
}
