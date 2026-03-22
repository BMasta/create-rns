package com.bmaster.createrns;

import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public enum RNSGuiTextures implements ScreenElement {
    DEPOSIT_MAP_TOGGLE_BG("widgets", 0, 0, 14, 14);

    public final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    RNSGuiTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    RNSGuiTextures(String location, int startX, int startY, int width, int height) {
        this(CreateRNS.ID, location, startX, startY, width, height);
    }

    RNSGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/" + location + ".png");
        this.width = width;
        this.height = height;
        this.startX = startX;
        this.startY = startY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(location, x, y, startX, startY, width, height);
    }
}
