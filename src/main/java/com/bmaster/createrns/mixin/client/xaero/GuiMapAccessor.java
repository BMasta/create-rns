package com.bmaster.createrns.mixin.client.xaero;

import com.bmaster.createrns.compat.map.RNSMapRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public interface GuiMapAccessor extends RNSMapRenderer.Context {
    @Override
    @Accessor("cameraX")
    double rns$getCameraX();

    @Override
    @Accessor("cameraZ")
    double rns$getCameraZ();

    @Override
    @Accessor("scale")
    double rns$getScale();

    @Override
    @Accessor("screenScale")
    double rns$getScreenScale();
}
