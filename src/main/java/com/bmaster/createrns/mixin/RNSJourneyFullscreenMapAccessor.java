package com.bmaster.createrns.mixin;

import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import journeymap.client.api.model.IFullscreen;
import journeymap.client.ui.fullscreen.Fullscreen;
import journeymap.client.render.map.GridRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.awt.geom.Point2D;

@Mixin(Fullscreen.class)
public abstract class RNSJourneyFullscreenMapAccessor implements RNSMapOverlayRenderer.Context {
    @Final
    @Shadow(remap = false)
    private static GridRenderer gridRenderer;

    @Shadow(remap = false)
    private Boolean isScrolling;

    @Shadow(remap = false)
    protected abstract Point2D.Double getMouseDrag();

    @Override
    public double create_rns$getCameraX() {
        var mouseDrag = getMouseDrag();
        boolean isDragging = isScrolling && mouseDrag != null;
        return gridRenderer.getCenterBlockX() - (isDragging ? mouseDrag.x : 0);
    }

    @Override
    public double create_rns$getCameraZ() {
        var mouseDrag = getMouseDrag();
        boolean isDragging = isScrolling && mouseDrag != null;
        return gridRenderer.getCenterBlockZ() - (isDragging ? mouseDrag.y : 0);
    }

    @Override
    public double create_rns$getScale() {
        return ((IFullscreen) this).getUiState().blockSize;
    }

    @Override
    public double create_rns$getScreenScale() {
        var window = ((IFullscreen) this).getMinecraft().getWindow();
        return (double) window.getScreenWidth() / window.getGuiScaledWidth();
    }
}
