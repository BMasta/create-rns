package com.bmaster.createrns.compat.map;

import com.bmaster.createrns.CreateRNS;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public final class RNSMapRenderer {
    public static final ResourceLocation DEPOSIT_TEXTURE = CreateRNS.asResource(
            "textures/block/iron_deposit_block.png");

    private static final int TEXTURE_MARKER_SIZE = 12;
    private static final int TEXTURE_MARKER_BORDER_COLOR = 0xCC101010;
    private static final int UNICODE_MARKER_COLOR = 0xFFF0C84B;
    private static final int UNICODE_MARKER_SHADOW_COLOR = 0xCC101010;
    private static final int SCREEN_PADDING = 16;
    private static final int TEXTURE_MARKER_OFFSET_X = 32;
    private static final int TEXT_MARKER_OFFSET_Z = 32;
    private static final String TEXT_MARKER_GLYPH = "\u25C6";

    public static void renderTextureMarker(
            GuiGraphics guiGraphics, Context context, int width, int height, double worldX, double worldZ
    ) {
        var screenPosition = worldToScreen(context, width, height, worldX, worldZ);
        if (screenPosition == null) {
            return;
        }

        var left = screenPosition.x() - TEXTURE_MARKER_SIZE / 2;
        var top = screenPosition.y() - TEXTURE_MARKER_SIZE / 2;
        guiGraphics.fill(left - 1, top - 1,
                left + TEXTURE_MARKER_SIZE + 1, top + TEXTURE_MARKER_SIZE + 1,
                TEXTURE_MARKER_BORDER_COLOR);
        guiGraphics.blit(DEPOSIT_TEXTURE, left, top, 0, 0,
                TEXTURE_MARKER_SIZE, TEXTURE_MARKER_SIZE,
                TEXTURE_MARKER_SIZE, TEXTURE_MARKER_SIZE);
    }

    @Nullable
    private static ScreenPosition worldToScreen(
            Context context, int width, int height, double worldX, double worldZ
    ) {
        if (context.rns$getScale() <= 0 || context.rns$getScreenScale() <= 0) {
            return null;
        }

        var x = width / 2.0 + (worldX - context.rns$getCameraX()) * context.rns$getScale() / context.rns$getScreenScale();
        var y = height / 2.0 + (worldZ - context.rns$getCameraZ()) * context.rns$getScale() / context.rns$getScreenScale();
        if (x < -SCREEN_PADDING || x > width + SCREEN_PADDING || y < -SCREEN_PADDING || y > height + SCREEN_PADDING) {
            return null;
        }

        return new ScreenPosition(Mth.floor(x), Mth.floor(y));
    }

    public static void render(Context context, GuiGraphics guiGraphics, int width, int height) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        var basePos = player.blockPosition();
        renderTextureMarker(guiGraphics, context, width, height,
                basePos.getX() + TEXTURE_MARKER_OFFSET_X + 0.5, basePos.getZ() + 0.5);
    }

    public interface Context {
        double rns$getCameraX();
        double rns$getCameraZ();
        double rns$getScale();
        double rns$getScreenScale();
    }

    private record ScreenPosition(int x, int y) {}
}
