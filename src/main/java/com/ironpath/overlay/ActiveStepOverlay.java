package com.ironpath.overlay;

import com.ironpath.IronmanPathConfig;
import com.ironpath.model.InfoPlanStep;
import com.ironpath.model.PlanStep;
import com.ironpath.model.PlanStepType;
import com.ironpath.model.QuestPlanStep;
import com.ironpath.model.SpineStepView;
import com.ironpath.model.TrainPlanStep;
import com.ironpath.service.ProgressionPlanService;
import com.ironpath.service.QuestRouteService;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.SpriteID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Displays the current active step (first step in the Next N list) in the game view.
 *
 * This intentionally reuses the same PlanStep fields that the sidebar cards render
 * (QuestEntry.shortWhy, TrainPlanStep.reason, InfoPlanStep.detail). No new model accessors.
 */
public class ActiveStepOverlay extends OverlayPanel
{
    private static final int MAX_WIDTH = 240;
    private static final int WRAP_CHARS = 44;

    private static final int PAD = 8;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 6;
    private static final int LINE_GAP = 3;

    // Cache OSRS sprites for overlay rendering to avoid flicker on refresh.
    private static final Map<Integer, BufferedImage> SPRITE_CACHE = new ConcurrentHashMap<>();
    private static final Set<Integer> SPRITE_IN_FLIGHT = ConcurrentHashMap.newKeySet();

    private final Client client;
    private final IronmanPathConfig config;
    private final QuestRouteService routeService;
    private final ProgressionPlanService planService;

    private final ClientThread clientThread;
    private final SpriteManager spriteManager;

    @Inject
    public ActiveStepOverlay(
            Client client,
            IronmanPathConfig config,
            QuestRouteService routeService,
            ProgressionPlanService planService,
            ClientThread clientThread,
            SpriteManager spriteManager)
    {
        this.client = client;
        this.config = config;
        this.routeService = routeService;
        this.planService = planService;
        this.clientThread = clientThread;
        this.spriteManager = spriteManager;

        setPosition(OverlayPosition.TOP_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        PanelComponent pc = getPanelComponent();
        pc.setBackgroundColor(ColorScheme.DARKER_GRAY_COLOR);
        pc.setPreferredSize(new Dimension(MAX_WIDTH, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showActiveStepOverlay())
        {
            return null;
        }

        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        final List<SpineStepView> next = planService.buildNextStepViews(routeService.getSpine(), 1);
        if (next.isEmpty())
        {
            return null;
        }

        final SpineStepView view = next.get(0);
        final PlanStep step = view.getStep();
        final PanelComponent pc = getPanelComponent();

        // Compact overlay:
        // Row 1: [icon] Title
        // Row 2: "Step X of Y" (left)
        // Body: why text wrapped
        // NOTE: "Quest Guide" text removed.
        pc.getChildren().clear();

        final int stepNum = view.getSpineIndex() + 1;
        final int total = view.getSpineTotal();

        String title;
        String why = null;
        boolean showQuestIcon = false;

        if (step instanceof QuestPlanStep)
        {
            QuestPlanStep q = (QuestPlanStep) step;
            title = q.getEntry().getQuestName();
            why = q.getEntry().getShortWhy();
            showQuestIcon = true;
        }
        else if (step instanceof TrainPlanStep)
        {
            TrainPlanStep t = (TrainPlanStep) step;
            title = "Train " + t.getSkill().getName() + " to " + t.getToLevel();
            why = t.getReason();
        }
        else if (step instanceof InfoPlanStep)
        {
            InfoPlanStep s = (InfoPlanStep) step;
            String label = labelFor(s.getType());
            String t = s.getTitle() == null ? "" : s.getTitle();
            title = label.isEmpty() ? t : (label + ": " + t);
            why = s.getDetail();
        }
        else
        {
            title = step.getType().name();
        }

        // Render hints
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Font base = graphics.getFont();
        final Font titleFont = base.deriveFont(Font.BOLD, base.getSize2D());
        final Font bodyFont = base.deriveFont(Font.PLAIN, base.getSize2D());

        final int width = MAX_WIDTH;
        int x = PAD;
        int y = PAD;

        // Pre-wrap body
        final List<String> wrapped = (why == null || why.isBlank()) ? new ArrayList<>() : wrap(why);

        // Measure
        graphics.setFont(titleFont);
        FontMetrics titleFm = graphics.getFontMetrics();
        int titleLineH = titleFm.getHeight();

        graphics.setFont(bodyFont);
        FontMetrics bodyFm = graphics.getFontMetrics();
        int bodyLineH = bodyFm.getHeight();

        // Layout heights
        int headerH = titleLineH + LINE_GAP + bodyLineH;
        int bodyH = wrapped.size() * bodyLineH;
        int height = PAD + headerH + (wrapped.isEmpty() ? 0 : (LINE_GAP + bodyH)) + PAD;

        // Background box
        graphics.setColor(ColorScheme.DARKER_GRAY_COLOR);
        graphics.fillRoundRect(0, 0, width, height, 10, 10);

        // Icon (OSRS quest sprite)
        BufferedImage icon = null;
        if (showQuestIcon)
        {
            icon = getOrRequestSprite(SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS);
        }

        graphics.setColor(java.awt.Color.WHITE);

        // Row 1: icon + title
        graphics.setFont(titleFont);
        int titleBaseline = y + titleFm.getAscent();
        int titleX = x;

        if (icon != null)
        {
            /*
             * Vertically center the icon on the title text baseline.
             * This prevents the icon from sitting too high/low across UI scales.
             */
            int iconY = titleBaseline - (ICON_SIZE / 2) - (titleFm.getAscent() / 2) + 1;
            graphics.drawImage(icon, x, iconY, ICON_SIZE, ICON_SIZE, null);
            titleX += ICON_SIZE + ICON_GAP;
        }

        graphics.drawString(title, titleX, titleBaseline);

        // Row 2: Step X of Y
        y += titleLineH + LINE_GAP;
        graphics.setFont(bodyFont);
        int row2Baseline = y + bodyFm.getAscent();

        String stepText = "Step " + stepNum + " of " + total;
        graphics.drawString(stepText, x, row2Baseline);

        // Body why text
        if (!wrapped.isEmpty())
        {
            y += bodyLineH + LINE_GAP;
            int lineY = y + bodyFm.getAscent();
            for (String line : wrapped)
            {
                graphics.drawString(line, x, lineY);
                lineY += bodyLineH;
            }
        }

        return new Dimension(width, height);
    }

    private static List<String> wrap(String text)
    {
        final ArrayList<String> lines = new ArrayList<>();
        if (text == null)
        {
            return lines;
        }

        String remaining = text.trim();
        while (remaining.length() > WRAP_CHARS)
        {
            int split = remaining.lastIndexOf(' ', WRAP_CHARS);
            if (split <= 0)
            {
                split = WRAP_CHARS;
            }

            lines.add(remaining.substring(0, split).trim());
            remaining = remaining.substring(split).trim();
        }

        if (!remaining.isEmpty())
        {
            lines.add(remaining);
        }
        return lines;
    }

    private static String labelFor(PlanStepType t)
    {
        if (t == null)
        {
            return "";
        }

        switch (t)
        {
            case MINIQUEST:
                return "Miniquest";
            case DIARY:
                return "Diary";
            case TRAIN:
                return "Train";
            case NOTE:
                return "Note";
            case UNLOCK:
                return "Unlock";
            case LAMP:
                return "Lamp";
            default:
                return "";
        }
    }

    private BufferedImage getOrRequestSprite(int spriteId)
    {
        BufferedImage cached = SPRITE_CACHE.get(spriteId);
        if (cached != null)
        {
            return cached;
        }

        if (spriteManager == null)
        {
            return null;
        }

        if (!SPRITE_IN_FLIGHT.add(spriteId))
        {
            return null;
        }

        Runnable request = () ->
        {
            BufferedImage img = spriteManager.getSprite(spriteId, 0);
            if (img != null)
            {
                SPRITE_CACHE.put(spriteId, img);
                SPRITE_IN_FLIGHT.remove(spriteId);
                return;
            }

            spriteManager.getSpriteAsync(spriteId, 0, sprite ->
            {
                if (sprite != null)
                {
                    SPRITE_CACHE.put(spriteId, sprite);
                }
                SPRITE_IN_FLIGHT.remove(spriteId);
            });
        };

        if (clientThread != null)
        {
            clientThread.invokeLater(request);
        }
        else
        {
            request.run();
        }

        return null;
    }
}
