package com.bmaster.createrns.mixin.journey;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer.ToggleLocation;
import journeymap.client.ui.fullscreen.Fullscreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Fullscreen.class, priority = 900 /* Must run before Create's JourneyFullscreenMapMixin */)
public abstract class RNSJourneyFullscreenMapMixin {
    @Unique
    boolean create_rns$failedToRenderDepositOverlay = false;

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(target = "Ljourneymap/client/ui/fullscreen/Fullscreen;drawMap(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            value = "INVOKE", shift = Shift.AFTER))
    private void create_rns$renderOverlay(GuiGraphics gui, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (create_rns$failedToRenderDepositOverlay) return;
        try {
            var fullscreen = (Fullscreen) (Object) this;
            var uiState = fullscreen.getUiState();
            if (uiState == null || !uiState.active) return;

            RNSMapOverlayRenderer.render(
                    (RNSMapOverlayRenderer.Context) this,
                    gui,
                    fullscreen.width,
                    fullscreen.height,
                    uiState.dimension
            );
            RNSMapToggleRenderer.render(gui, fullscreen.width, fullscreen.height,
                    mouseX, mouseY, ToggleLocation.JOURNEY);
        } catch (Exception e) {
            CreateRNS.LOGGER.error("Create RNS: failed to render Journey Map deposit overlay", e);
            create_rns$failedToRenderDepositOverlay = true;
        }
    }
}
