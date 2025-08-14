package com.bmaster.createrns.block.excavator;

import com.bmaster.createrns.CreateRNS;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

public class ExcavatorScreen extends AbstractContainerScreen<ExcavatorMenu> {

    private static final ResourceLocation EXCAVATOR_MENU_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateRNS.MOD_ID, "textures/gui/container/excavator.png");

    private static final int PROGRESS_BAR_SEGMENT_COUNT = 12;

    public ExcavatorScreen(ExcavatorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private void renderBaseMenu(GuiGraphics pGuiGraphics) {
        // Draw excavator menu
        pGuiGraphics.blit(EXCAVATOR_MENU_TEXTURE, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight);
    }

    private void renderGhostItem(GuiGraphics pGuiGraphics) {
        Slot slot = this.menu.slots.get(ExcavatorMenu.YIELD_SLOT_INDEX);
        Item ghostItem = menu.getGhostItem();
        if (ghostItem != null && !slot.hasItem()) {
            int x = leftPos + ExcavatorMenu.YIELD_PIXEL_OFFSET_X;
            int y = topPos + ExcavatorMenu.YIELD_PIXEL_OFFSET_Y;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 0.45f);

            pGuiGraphics.renderItem(new ItemStack(ghostItem, 1), x, y);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
    }

    private void renderProgressBar(GuiGraphics pGuiGraphics) {
        int filled_segment_count = (int) Math.round(
                (double) menu.getProgress() / menu.getMaxProgress() * PROGRESS_BAR_SEGMENT_COUNT);

        int pbFilledTexX = imageWidth;
        int pbFilledTexY = 0;
        int pbFilledTexWidth = 12;
        // Progress bar texture height must be divisible by segment count - 1 as
        // the texture only starts being rendered when 1 bar is filled.
        int pbFilledTexHeight = filled_segment_count - 1;
        int pbDstX = leftPos + 82;
        int pbDstY = topPos + 33;

        if (filled_segment_count > 0) {
            pGuiGraphics.blit(EXCAVATOR_MENU_TEXTURE, pbDstX, pbDstY,
                    pbFilledTexX, pbFilledTexY, pbFilledTexWidth, pbFilledTexHeight);
        }
    }

    @ParametersAreNonnullByDefault
    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        renderBaseMenu(pGuiGraphics);
        renderGhostItem(pGuiGraphics);
        renderProgressBar(pGuiGraphics);
    }

    @ParametersAreNonnullByDefault
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
}
