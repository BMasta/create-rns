package com.bmaster.createrns.mixin.xaero;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer.ToggleLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.gui.GuiMap;

@Mixin(value = GuiMap.class, priority = 900 /* Must run before Create's XaeroFullscreenMapMixin */)
public abstract class RNSXaeroFullscreenMapMixin {
    @Unique
    boolean create_rns$failedToRenderDepositOverlay = false;

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"),
            require = 0)
    private void create_rns$renderToggle(
            GuiGraphics gui, int mouseX, int mouseY, float partialTicks, CallbackInfo ci
    ) {
        if (create_rns$failedToRenderDepositOverlay) return;
        try {
            var screen = (Screen) (Object) this;
            var accessor = (RNSXaeroFullscreenMapAccessor) this;
            var currentDimension = accessor.create_rns$getMapProcessor().getMapWorld().getCurrentDimension();
            if (currentDimension == null) return;

            RNSMapOverlayRenderer.render(accessor, gui, screen.width, screen.height,
                    currentDimension.getDimId());
            RNSMapToggleRenderer.render(gui, screen.width, screen.height,
                    mouseX, mouseY, ToggleLocation.XAERO);
        } catch (Exception e) {
            CreateRNS.LOGGER.error("Create RNS: failed to render Xaero's World Map deposit overlay", e);
            create_rns$failedToRenderDepositOverlay = true;
        }
    }
}
