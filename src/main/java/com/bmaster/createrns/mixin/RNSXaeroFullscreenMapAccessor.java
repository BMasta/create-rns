package com.bmaster.createrns.mixin;

import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import xaero.map.MapProcessor;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public interface RNSXaeroFullscreenMapAccessor extends RNSMapOverlayRenderer.Context {
    @Override
    @Accessor("cameraX")
    double create_rns$getCameraX();

    @Override
    @Accessor("cameraZ")
    double create_rns$getCameraZ();

    @Override
    @Accessor("scale")
    double create_rns$getScale();

    @Override
    @Accessor("screenScale")
    double create_rns$getScreenScale();

    @Accessor("mapProcessor")
    MapProcessor create_rns$getMapProcessor();
}
