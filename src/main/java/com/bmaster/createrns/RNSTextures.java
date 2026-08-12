package com.bmaster.createrns;

import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.render.BindableTexture;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum RNSTextures implements ScreenElement, BindableTexture {
    DEPOSIT_MAP_TOGGLE_BG("gui/widgets", 0, 0, 14, 14),
    BLOCKFACE_TINTED("special/tinted", 0, 0, 16, 16);

    public final ResourceLocation location;
    private final int width;
    private final int height;
    private final int startX;
    private final int startY;

    RNSTextures(String location, int width, int height) {
        this(location, 0, 0, width, height);
    }

    RNSTextures(String location, int startX, int startY, int width, int height) {
        this(CreateRNS.ID, location, startX, startY, width, height);
    }

    RNSTextures(String namespace, String location, int startX, int startY, int width, int height) {
        this.location = ResourceLocation.fromNamespaceAndPath(namespace, "textures/" + location + ".png");
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

    @Override
    public ResourceLocation getLocation() {
        return this.location;
    }
}
