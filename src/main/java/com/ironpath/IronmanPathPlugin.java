package com.ironpath;

import com.google.inject.Provides;
import com.ironpath.service.QuestRouteService;
import com.ironpath.service.ProgressionPlanService;
import com.ironpath.overlay.ActiveStepOverlay;
import com.ironpath.ui.IronmanPathPanel;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
        name = "Ironman Path",
        description = "Quest-first ironman progression panel (quest cape route, readiness, search).",
        tags = {"ironman", "quest", "route", "efficiency"}
)
public class IronmanPathPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;

    @Inject private QuestRouteService questRouteService;
    @Inject private ProgressionPlanService progressionPlanService;

    @Inject private OverlayManager overlayManager;
    @Inject private ActiveStepOverlay activeStepOverlay;

    @Inject private IronmanPathConfig config;

    private IronmanPathPanel panel;
    private NavigationButton navButton;

    // Debounce refresh spam (varbits can fire frequently).
    private long lastRefreshMs = 0L;
    private static final long REFRESH_DEBOUNCE_MS = 400L;

    @Provides
    IronmanPathConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(IronmanPathConfig.class);
    }

    @Override
    protected void startUp()
    {
        panel = new IronmanPathPanel(questRouteService, progressionPlanService, clientThread);

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/ironpath/icon.png");

        navButton = NavigationButton.builder()
                .tooltip("Ironman Path")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        // Overlay in the game view (toggled via config).
        overlayManager.add(activeStepOverlay);
        log.info("Ironman Path started");
    }

    @Override
    protected void shutDown()
    {
        if (overlayManager != null && activeStepOverlay != null)
        {
            overlayManager.remove(activeStepOverlay);
        }

        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        panel = null;
        log.info("Ironman Path stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (panel == null)
        {
            return;
        }

        if (event.getGameState() == GameState.LOGGED_IN)
        {
            panel.refresh();
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        // Quest progress updates are reflected through varbits/varps.
        // Refresh with a small debounce to avoid spamming UI rebuilds.
        if (panel == null)
        {
            return;
        }

        final long now = System.currentTimeMillis();
        if (now - lastRefreshMs < REFRESH_DEBOUNCE_MS)
        {
            return;
        }
        lastRefreshMs = now;
        panel.refresh();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        panel.refresh();
    }
}
