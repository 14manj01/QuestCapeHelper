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
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.SwingUtilities;

@Slf4j
@PluginDescriptor(
        name = "Optimal Quest Order",
        description = "Quest-first ironman progression panel (quest cape route, readiness, search).",
        tags = {"ironman", "quest", "route", "efficiency"}
)
public class IronmanPathPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar clientToolbar;
    @Inject private SpriteManager spriteManager;

    @Inject private QuestRouteService questRouteService;
    @Inject private ProgressionPlanService progressionPlanService;

    @Inject private OverlayManager overlayManager;
    @Inject private ActiveStepOverlay activeStepOverlay;

    @Inject private IronmanPathConfig config;

    private IronmanPathPanel panel;
    private NavigationButton navButton;

    // Delay the first refresh after login so skills/quests/varbits settle and we avoid UI thrash.
    private int refreshInTicks = 0;

    // Varbits can change extremely frequently (run energy, timers, etc.).
    // Mark the panel as dirty and refresh at a low, human-friendly rate.
    private boolean panelDirty = false;
    private int varbitRefreshCooldownTicks = 0;

    @Provides
    IronmanPathConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(IronmanPathConfig.class);
    }

    @Override
    protected void startUp()
    {
        panel = new IronmanPathPanel(questRouteService, progressionPlanService, clientThread, spriteManager);

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
            // Wait a couple ticks so quest state/skills/varbits are populated.
            refreshInTicks = 2;
            panelDirty = false;
            varbitRefreshCooldownTicks = 0;
            panel.showLoading();
            return;
        }

        if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
        {
            refreshInTicks = 0;
            panelDirty = false;
            varbitRefreshCooldownTicks = 0;
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (panel == null)
        {
            return;
        }

        if (refreshInTicks > 0)
        {
            refreshInTicks--;
            if (refreshInTicks == 0)
            {
                requestRefreshIfPanelShowing();
            }
        }

        // Rate-limit refreshes caused by frequent varbit changes.
        if (varbitRefreshCooldownTicks > 0)
        {
            varbitRefreshCooldownTicks--;
        }

        if (panelDirty && varbitRefreshCooldownTicks == 0)
        {
            panelDirty = false;
            // ~1.8s at 0.6s per tick. Keeps UI responsive without constant rebuilds.
            varbitRefreshCooldownTicks = 3;
            requestRefreshIfPanelShowing();
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        if (panel == null)
        {
            return;
        }

        // Varbits change very frequently. Mark dirty and refresh on a short tick-based cadence.
        // This avoids constant panel rebuilds (and prevents icon flicker).
        panelDirty = true;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (panel == null)
        {
            return;
        }

        requestRefreshIfPanelShowing();
    }

    private void requestRefreshIfPanelShowing()
    {
        // Only rebuild the sidebar when it's actually visible.
        // This prevents background varbit churn from constantly recreating cards.
        SwingUtilities.invokeLater(() ->
        {
            if (panel != null && panel.isShowing())
            {
                panel.requestRefresh();
            }
        });
    }
}
