package com.ironpath.overlay;

import com.ironpath.IronmanPathConfig;
import com.ironpath.model.InfoPlanStep;
import com.ironpath.model.PlanStep;
import com.ironpath.model.PlanStepType;
import com.ironpath.model.QuestPlanStep;
import com.ironpath.model.TrainPlanStep;
import com.ironpath.service.ProgressionPlanService;
import com.ironpath.service.QuestRouteService;
import java.awt.Dimension;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.QuestState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.util.ArrayList;

/**
 * Displays the current active step (first step in the Next N list) in the game view.
 *
 * This intentionally reuses the same PlanStep fields that the sidebar cards render
 * (QuestEntry.shortWhy, TrainPlanStep.reason, InfoPlanStep.detail). No new model accessors.
 */
public class ActiveStepOverlay extends OverlayPanel
{
    private static final int MAX_WIDTH = 210;
    private static final int WRAP_CHARS = 36;

    private final Client client;
    private final IronmanPathConfig config;
    private final QuestRouteService routeService;
    private final ProgressionPlanService planService;

    @Inject
    public ActiveStepOverlay(
            Client client,
            IronmanPathConfig config,
            QuestRouteService routeService,
            ProgressionPlanService planService)
    {
        this.client = client;
        this.config = config;
        this.routeService = routeService;
        this.planService = planService;

        setPosition(OverlayPosition.TOP_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        PanelComponent pc = getPanelComponent();
        pc.setBackgroundColor(ColorScheme.DARKER_GRAY_COLOR);
        pc.setPreferredSize(new Dimension(MAX_WIDTH, 0));
    }

    @Override
    public Dimension render(java.awt.Graphics2D graphics)
    {
        if (!config.showActiveStepOverlay())
        {
            return null;
        }

        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return null;
        }

        final List<PlanStep> next = planService.buildNextSteps(routeService.getSpine(), 1);
        if (next.isEmpty())
        {
            return null;
        }

        final PlanStep step = next.get(0);
        final PanelComponent pc = getPanelComponent();
        pc.getChildren().clear();

        pc.getChildren().add(TitleComponent.builder().text("Active step").build());

        // Quest
        if (step instanceof QuestPlanStep)
        {
            QuestPlanStep q = (QuestPlanStep) step;
            String title = q.getEntry().getQuestName();
            String why = q.getEntry().getShortWhy();
            String pill = pillText(q.getState());

            pc.getChildren().add(LineComponent.builder()
                    .left(title)
                    .right(pill)
                    .build());

            if (why != null && !why.isBlank())
            {
                addWrapped(pc, why);
            }

            return super.render(graphics);
        }

        // Train
        if (step instanceof TrainPlanStep)
        {
            TrainPlanStep t = (TrainPlanStep) step;
            String title = "Train " + t.getSkill().getName() + " to " + t.getToLevel();
            pc.getChildren().add(LineComponent.builder().left(title).build());

            String why = t.getReason();
            if (why != null && !why.isBlank())
            {
                addWrapped(pc, why);
            }

            return super.render(graphics);
        }

        // Info/Note/Diary/etc
        if (step instanceof InfoPlanStep)
        {
            InfoPlanStep s = (InfoPlanStep) step;
            String label = labelFor(s.getType());
            String title = s.getTitle() == null ? "" : s.getTitle();
            String header = label.isEmpty() ? title : (label + ": " + title);
            pc.getChildren().add(LineComponent.builder().left(header).build());

            String why = s.getDetail();
            if (why != null && !why.isBlank())
            {
                addWrapped(pc, why);
            }

            return super.render(graphics);
        }

        // Fallback
        pc.getChildren().add(LineComponent.builder().left(step.getType().name()).build());
        return super.render(graphics);
    }

    private static String pillText(QuestState state)
    {
        if (state == null)
        {
            return "";
        }

        switch (state)
        {
            case FINISHED:
                return "Done";
            case IN_PROGRESS:
                return "In progress";
            case NOT_STARTED:
            default:
                return "Ready";
        }
    }

    private static void addWrapped(PanelComponent pc, String text)
    {
        for (String line : wrap(text))
        {
            pc.getChildren().add(LineComponent.builder()
                .left(line)
                .build());
        }
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
}
