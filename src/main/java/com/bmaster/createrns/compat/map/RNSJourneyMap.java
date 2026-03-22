package com.bmaster.createrns.compat.map;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer.ToggleLocation;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.JourneyMapPlugin;
import journeymap.api.v2.client.event.FullscreenMapEvent;
import journeymap.api.v2.client.event.FullscreenRenderEvent;
import journeymap.api.v2.client.fullscreen.IFullscreen;
import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;

import javax.annotation.ParametersAreNonnullByDefault;

@SuppressWarnings("removal")
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
        FullscreenEventRegistry.FULLSCREEN_MAP_CLICK_EVENT.subscribe(CreateRNS.ID, RNSJourneyMap::onFullscreenClick);
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

    private static void onFullscreenClick(FullscreenMapEvent.ClickEvent event) {
        if (event.getStage() != FullscreenMapEvent.Stage.PRE) return;
        boolean clicked = RNSMapToggleRenderer.handleClick(event.getMouseX(), event.getMouseY(), event.getButton(),
                ToggleLocation.JOURNEY);
        if (clicked) event.cancel();
    }

    private static double getGuiScale() {
        var window = Minecraft.getInstance().getWindow();
        return (double) window.getScreenWidth() / window.getGuiScaledWidth();
    }

    private record JourneyMapContext(IFullscreen fullscreen, UIState uiState, double guiScale) implements RNSMapOverlayRenderer.Context {
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
