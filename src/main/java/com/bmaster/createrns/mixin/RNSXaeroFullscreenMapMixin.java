package com.bmaster.createrns.mixin;

import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer.ToggleLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;

@Mixin(value = GuiMap.class, priority = 900 /* Must run before Create's XaeroFullscreenMapMixin */)
public abstract class RNSXaeroFullscreenMapMixin {
    @Inject(method = "render", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"),
            require = 0)
    private void create_rns$renderToggle(
            GuiGraphics gui, int mouseX, int mouseY, float partialTicks, CallbackInfo ci
    ) {
        var window = Minecraft.getInstance().getWindow();
        var accessor = (RNSXaeroFullscreenMapAccessor) this;
        var currentDimension = accessor.create_rns$getMapProcessor().getMapWorld().getCurrentDimension();
        if (currentDimension == null) return;

        RNSMapOverlayRenderer.render(accessor, gui, window.getGuiScaledWidth(), window.getGuiScaledHeight(),
                currentDimension.getDimId());
        RNSMapToggleRenderer.render(gui, window.getGuiScaledWidth(), window.getGuiScaledHeight(),
                mouseX, mouseY, ToggleLocation.XAERO);
    }

    @Inject(method = "mouseClicked", remap = false, at = @At("HEAD"), cancellable = true)
    private void create_rns$handleOverlayClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!RNSMapToggleRenderer.handleClick(mouseX, mouseY, button, ToggleLocation.XAERO)) return;
        cir.setReturnValue(true);
    }
}
