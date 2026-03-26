package com.bmaster.createrns.compat.map;

import com.bmaster.createrns.content.deposit.info.FoundDepositClientCache;
import com.bmaster.createrns.content.deposit.spec.DepositSpecLookup;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSMapOverlayRenderer {
    private static final int ITEM_MARKER_SIZE = 16;
    private static final float ITEM_MARKER_SCALE_EXPONENT = 0.4f;
    private static final int SCREEN_PADDING = 16;

    public static void render(Context context, GuiGraphics gui, int width, int height) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.level() == null) return;

        render(context, gui, width, height, player.level().dimension());
    }

    public static void render(
            Context context, GuiGraphics gui, int width, int height, ResourceKey<Level> dimension
    ) {
        var player = Minecraft.getInstance().player;
        if (player == null || player.level() == null || !RNSMapToggleRenderer.isOverlayEnabled()) return;

        var foundDeposits = FoundDepositClientCache.getDeposits(dimension);
        for (var deposit : foundDeposits) {
            var mapIconItem = DepositSpecLookup.getMapIcon(player.level().registryAccess(), deposit.getKey());
            if (mapIconItem == null) continue;
            renderItemMarker(gui, context, width, height,
                    deposit.getLocation().getX() + 0.5, deposit.getLocation().getZ() + 0.5, mapIconItem);
        }
    }

    protected static @Nullable ScreenPosition worldToScreen(
            Context context, int width, int height, double worldX, double worldZ
    ) {
        if (context.create_rns$getScale() <= 0 || context.create_rns$getScreenScale() <= 0) {
            return null;
        }

        var x = width / 2.0 + (worldX - context.create_rns$getCameraX()) *
                context.create_rns$getScale() / context.create_rns$getScreenScale();
        var y = height / 2.0 + (worldZ - context.create_rns$getCameraZ()) *
                context.create_rns$getScale() / context.create_rns$getScreenScale();
        if (x < -SCREEN_PADDING || x > width + SCREEN_PADDING || y < -SCREEN_PADDING || y > height + SCREEN_PADDING) {
            return null;
        }

        return new ScreenPosition(x, y);
    }

    protected static void renderItemMarker(
            GuiGraphics gui, Context context, int width, int height, double worldX, double worldZ, ItemStack stack
    ) {
        var screenPosition = worldToScreen(context, width, height, worldX, worldZ);
        if (screenPosition == null) return;

        float mapScale = (float) (context.create_rns$getScale() / context.create_rns$getScreenScale());
        float itemScale = (float) Math.pow(mapScale, ITEM_MARKER_SCALE_EXPONENT);
        var scaledItemSize = ITEM_MARKER_SIZE * itemScale;
        var left = screenPosition.x() - scaledItemSize / 2.0;
        var top = screenPosition.y() - scaledItemSize / 2.0;

        gui.pose().pushPose();
        gui.pose().translate(left, top, 0);
        gui.pose().scale(itemScale, itemScale, 1);
        gui.renderItem(stack, 0, 0);
        gui.pose().popPose();
    }

    public interface Context {
        double create_rns$getCameraX();
        double create_rns$getCameraZ();
        double create_rns$getScale();
        double create_rns$getScreenScale();
    }

    protected record ScreenPosition(double x, double y) {}
}
