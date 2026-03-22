package com.bmaster.createrns.mixin;

import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer.ToggleLocation;
import journeymap.client.ui.fullscreen.Fullscreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Fullscreen.class, priority = 900 /* Must run before Create's JourneyFullscreenMapMixin */)
public abstract class RNSJourneyFullscreenMapMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", remap = false, at = @At(value = "INVOKE",
            target = "Ljourneymap/client/ui/fullscreen/Fullscreen;drawMap(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            shift = Shift.AFTER), require = 0)
    private void create_rns$renderOverlay(GuiGraphics gui, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        var fullscreen = (Fullscreen) (Object) this;
        var uiState = fullscreen.getUiState();
        if (uiState == null || !uiState.active) return;

        var window = Minecraft.getInstance().getWindow();
        RNSMapOverlayRenderer.render(
                (RNSMapOverlayRenderer.Context) this,
                gui,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight(),
                uiState.dimension
        );
        RNSMapToggleRenderer.render(gui, window.getGuiScaledWidth(), window.getGuiScaledHeight(),
                mouseX, mouseY, ToggleLocation.JOURNEY);
    }

    @Inject(method = "mouseClicked", remap = false, at = @At("HEAD"), cancellable = true)
    private void create_rns$handleOverlayClick(
            double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!RNSMapToggleRenderer.handleClick(mouseX, mouseY, button, ToggleLocation.JOURNEY)) return;
        cir.setReturnValue(true);
    }
}
