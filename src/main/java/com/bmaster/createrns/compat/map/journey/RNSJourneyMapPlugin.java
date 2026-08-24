package com.bmaster.createrns.compat.map.journey;

import com.bmaster.createrns.CreateRNS;
import com.bmaster.createrns.compat.map.RNSMapOverlayRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer;
import com.bmaster.createrns.compat.map.RNSMapToggleRenderer.ToggleLocation;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.FullscreenMapEvent;
import journeymap.api.v2.client.event.FullscreenRenderEvent;
import journeymap.api.v2.client.fullscreen.IFullscreen;
import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.common.Context.UI;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@JourneyMapPlugin(apiVersion = "2.0.0")
public class RNSJourneyMapPlugin implements IClientPlugin {
    private static boolean failedToRenderDepositOverlay = false;

    public RNSJourneyMapPlugin() {
    }

    @Override
    public void initialize(IClientAPI jmClientApi) {
        FullscreenEventRegistry.FULLSCREEN_RENDER_EVENT.subscribe(CreateRNS.ID, RNSJourneyMapPlugin::onRender);
        FullscreenEventRegistry.FULLSCREEN_MAP_CLICK_EVENT.subscribe(CreateRNS.ID, RNSJourneyMapPlugin::onClick);
    }

    @Override
    public String getModId() {
        return CreateRNS.ID;
    }

    private static void onRender(FullscreenRenderEvent event) {
        if (failedToRenderDepositOverlay) return;

        try {
            var fullscreen = event.getFullscreen();
            var uiState = fullscreen.getUiState();
            if (uiState == null || uiState.ui != UI.Fullscreen || !uiState.active) return;

            var screen = fullscreen.getScreen();
            var gui = event.getGraphics();
            RNSMapOverlayRenderer.render(new JourneyMapContext(fullscreen, uiState),
                    gui, screen.width, screen.height, uiState.dimension);
            RNSMapToggleRenderer.render(gui, screen.width, screen.height,
                    event.getMouseX(), event.getMouseY(), ToggleLocation.JOURNEY);
        } catch (Exception e) {
            CreateRNS.LOGGER.error("Create RNS: failed to render JourneyMap deposit overlay", e);
            failedToRenderDepositOverlay = true;
        }
    }

    private static void onClick(FullscreenMapEvent.ClickEvent event) {
        if (event.getStage() != FullscreenMapEvent.Stage.PRE) return;

        var mc = Minecraft.getInstance();
        var screen = mc.screen;
        if (screen == null) return;

        var window = mc.getWindow();
        double mouseX = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        if (RNSMapToggleRenderer.handleClick(screen.width, screen.height,
                mouseX, mouseY, event.getButton(), ToggleLocation.JOURNEY)) event.cancel();
    }

    private record JourneyMapContext(
            IFullscreen fullscreen, UIState uiState
    ) implements RNSMapOverlayRenderer.Context {
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
            var window = fullscreen.getMinecraft().getWindow();
            return (double) window.getScreenWidth() / window.getGuiScaledWidth();
        }
    }
}
