package com.bmaster.createrns.compat.map.journey;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer.ToggleLocation;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.FullscreenRenderEvent;
import journeymap.api.v2.client.fullscreen.IFullscreen;
import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import journeymap.client.ui.fullscreen.Fullscreen;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.InputEvent;

import javax.annotation.ParametersAreNonnullByDefault;

@JourneyMapPlugin(apiVersion = "2.0.0")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RNSJourneyMap implements IClientPlugin {
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public String getModId() {
        return CreateRNS.ID;
    }

    @Override
    public void initialize(IClientAPI jmClientApi) {
        FullscreenEventRegistry.FULLSCREEN_RENDER_EVENT.subscribe(CreateRNS.ID, RNSJourneyMap::onFullscreenRender);
    }

    public static void onMouseClick(InputEvent.MouseButton.Pre event) {
        var mc = Minecraft.getInstance();
        if (!(mc.screen instanceof Fullscreen screen)) return;

        var window = mc.getWindow();
        double mouseX = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        if (RNSMapToggleRenderer.handleClick(mouseX, mouseY, event.getButton(), screen, ToggleLocation.JOURNEY)) {
            event.setCanceled(true);
        }
    }

    private static void onFullscreenRender(FullscreenRenderEvent event) {
        var uiState = event.getFullscreen().getUiState();
        if (uiState == null || !uiState.active) return;

        var screen = event.getFullscreen().getScreen();
        RNSMapOverlayRenderer.render(
                new JourneyMapContext(event.getFullscreen(), uiState, getGuiScale()),
                event.getGraphics(),
                screen.width,
                screen.height,
                uiState.dimension
        );
        RNSMapToggleRenderer.render(event.getGraphics(), screen, event.getMouseX(), event.getMouseY(),
                ToggleLocation.JOURNEY);
    }

    private static double getGuiScale() {
        var window = Minecraft.getInstance().getWindow();
        return (double) window.getScreenWidth() / window.getGuiScaledWidth();
    }

    private record JourneyMapContext(IFullscreen fullscreen, UIState uiState,
                                     double guiScale) implements RNSMapOverlayRenderer.Context {
        @Override
        public double create_rns$getCameraX() {
            return fullscreen.getCenterBlockX(true);
        }

        @Override
        public double create_rns$getCameraZ() {
            return fullscreen.getCenterBlockZ(true);
        }

        @Override
        public double create_rns$getScale() {
            return uiState.blockSize;
        }

        @Override
        public double create_rns$getScreenScale() {
            return guiScale;
        }
    }
}
