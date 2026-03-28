package com.bmaster.createrns.mixin.xaero;

import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.MapProcessor;
import xaero.map.gui.GuiMap;

@Mixin(GuiMap.class)
public interface RNSXaeroFullscreenMapAccessor extends RNSMapOverlayRenderer.Context {
    @Override
    @Accessor(value = "cameraX", remap = false)
    double create_rns$getCameraX();

    @Override
    @Accessor(value = "cameraZ", remap = false)
    double create_rns$getCameraZ();

    @Override
    @Accessor(value = "scale", remap = false)
    double create_rns$getScale();

    @Override
    @Accessor(value = "screenScale", remap = false)
    double create_rns$getScreenScale();

    @Accessor(value = "mapProcessor", remap = false)
    MapProcessor create_rns$getMapProcessor();
}
