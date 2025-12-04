package com.bmaster.createrns;

import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public enum RNSGuiTextures implements ScreenElement {
    JEI_SLOT("jei/widgets", 0, 0, 18, 18),
    JEI_CHANCE_SLOT("jei/widgets", 0, 20, 18, 18),
    JEI_RESONANCE_SLOT("jei/widgets", 20, 0, 18, 18),
    JEI_RESONANCE_CHANCE_SLOT("jei/widgets", 20, 20, 18, 18),
    JEI_SHATTERING_RESONANCE_SLOT("jei/widgets", 40, 0, 18, 18),
    JEI_SHATTERING_RESONANCE_CHANCE_SLOT("jei/widgets", 40, 20, 18, 18),
    JEI_STABILIZING_RESONANCE_SLOT("jei/widgets", 60, 0, 18, 18),
    JEI_STABILIZING_RESONANCE_CHANCE_SLOT("jei/widgets", 60, 20, 18, 18);

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
