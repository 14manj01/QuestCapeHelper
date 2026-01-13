package com.ironpath.ui;

import com.ironpath.model.InfoPlanStep;
import com.ironpath.model.PlanStep;
import com.ironpath.model.SpineStepView;
import com.ironpath.model.PlanStepType;
import com.ironpath.model.QuestPlanStep;
import com.ironpath.model.TrainPlanStep;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Renders a {@link PlanStep} as a compact sidebar-safe card.
 */
public final class PlanStepCard
{
    private PlanStepCard() {}

    public static JPanel compact(SpineStepView view)
    {
        if (view == null)
        {
            return empty("Unknown step", -1, -1);
        }

        return compact(view.getStep(), view.getSpineIndex(), view.getSpineTotal());
    }

    public static JPanel compact(PlanStep step, int spineIndex, int spineTotal)
    {
        if (step == null)
        {
            return empty("Unknown step", spineIndex, spineTotal);
        }

        if (step.getType() == PlanStepType.QUEST && step instanceof QuestPlanStep)
        {
            QuestPlanStep q = (QuestPlanStep) step;
            return QuestCard.compact(q.getEntry(), q.getState(), spineIndex, spineTotal);
        }

        if (step.getType() == PlanStepType.TRAIN && step instanceof TrainPlanStep)
        {
            return train((TrainPlanStep) step, spineIndex, spineTotal);
        }

        if (step instanceof InfoPlanStep)
        {
            return info((InfoPlanStep) step, spineIndex, spineTotal);
        }

        return empty(step.getType().name(), spineIndex, spineTotal);
    }

    private static JPanel train(TrainPlanStep t, int spineIndex, int spineTotal)
    {
        JPanel p = base();
        String title = "Train " + t.getSkill().getName() + " to " + t.getToLevel();

        p.add(buildHeader(title, spineIndex, spineTotal, true), BorderLayout.NORTH);

        JTextArea body = bodyArea(t.getReason());
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private static JPanel info(InfoPlanStep s, int spineIndex, int spineTotal)
    {
        // Render MINIQUEST with quest-like card styling for consistency.
        if (s.getType() == PlanStepType.MINIQUEST)
        {
            String title = s.getTitle() == null ? "Miniquest" : s.getTitle();
            String detail = s.getDetail();
            return MiniquestCard.compact(title, detail, spineIndex, spineTotal);
        }

        JPanel p = base();

        String label = labelFor(s.getType());
        String title = s.getTitle() == null ? label : s.getTitle();
        String headerTitle = label + ": " + title;

        p.add(buildHeader(headerTitle, spineIndex, spineTotal, true), BorderLayout.NORTH);

        JTextArea body = bodyArea(s.getDetail());
        body.setFont(FontManager.getRunescapeSmallFont());
        body.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        p.add(body, BorderLayout.CENTER);
        return p;
    }


    private static String labelFor(PlanStepType t)
    {
        if (t == null)
        {
            return "";
        }

        switch (t)
        {
            case QUEST: return "";
            case MINIQUEST: return "Miniquest";
            case DIARY: return "Diary";
            case TRAIN: return "Train";
            case NOTE: return "";
            default: return "";
        }
    }

    private static JPanel empty(String title, int spineIndex, int spineTotal)
    {
        JPanel p = base();
        p.add(buildHeader(title, spineIndex, spineTotal, true), BorderLayout.NORTH);
        return p;
    }

    private static JPanel base()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    
    private static JPanel buildHeader(String leftTitle, int spineIndex, int spineTotal, boolean showLongLine)
    {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));

        // Row 1: title (left) + progress (right)
        JPanel row1 = new JPanel(new BorderLayout());
        row1.setOpaque(false);

        JLabel title = new JLabel(leftTitle == null ? "" : leftTitle);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setFont(FontManager.getRunescapeSmallFont());
        title.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        JLabel progress = new JLabel(formatProgressCompact(spineIndex, spineTotal));
        progress.setFont(FontManager.getRunescapeSmallFont());
        progress.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);

        row1.add(title, BorderLayout.WEST);
        row1.add(progress, BorderLayout.EAST);

        header.add(row1);

        if (showLongLine)
        {
            // Row 2: "Step X of Y"
            JLabel stepLine = new JLabel(formatProgressLong(spineIndex, spineTotal));
            stepLine.setFont(FontManager.getRunescapeSmallFont());
            stepLine.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
            stepLine.setAlignmentX(Component.LEFT_ALIGNMENT);
            stepLine.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
            header.add(stepLine);
        }
        return header;
    }

    private static String formatProgressCompact(int spineIndex, int spineTotal)
    {
        if (spineIndex < 0 || spineTotal <= 0)
        {
            return "";
        }
        return (spineIndex + 1) + " / " + spineTotal;
    }

    private static String formatProgressLong(int spineIndex, int spineTotal)
    {
        if (spineIndex < 0 || spineTotal <= 0)
        {
            return "";
        }
        return "Step " + (spineIndex + 1) + " of " + spineTotal;
    }


private static JTextArea bodyArea(String text)
    {
        JTextArea ta = new JTextArea(text == null ? "" : text);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        ta.setFocusable(false);
        ta.setOpaque(false);
        // Keep preferred width small so the card never exceeds the sidebar.
        ta.setColumns(1);
        ta.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        return ta;
    }
}