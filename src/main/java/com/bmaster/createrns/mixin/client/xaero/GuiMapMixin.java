package com.bmaster.createrns.mixin.client.xaero;

import com.bmaster.createrns.compat.map.RNSMapRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
abstract class GuiMapMixin extends Screen {
    protected GuiMapMixin(Component title) {
        super(title);
    }

    @Inject(method = "renderPreDropdown", at = @At("TAIL"))
    private void create_rns$renderOverlay(
            GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci
    ) {
        RNSMapRenderer.render((GuiMapAccessor) this, guiGraphics, width, height);
    }
}
