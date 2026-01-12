package com.ironpath.ui;

import com.ironpath.model.InfoPlanStep;
import com.ironpath.model.PlanStep;
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

    public static JPanel compact(PlanStep step)
    {
        if (step == null)
        {
            return empty("Unknown step");
        }

        if (step.getType() == PlanStepType.QUEST && step instanceof QuestPlanStep)
        {
            QuestPlanStep q = (QuestPlanStep) step;
            return QuestCard.compact(q.getEntry(), q.getState());
        }

        if (step.getType() == PlanStepType.TRAIN && step instanceof TrainPlanStep)
        {
            return train((TrainPlanStep) step);
        }

        if (step instanceof InfoPlanStep)
        {
            return info((InfoPlanStep) step);
        }

        return empty(step.getType().name());
    }

    private static JPanel train(TrainPlanStep t)
    {
        JPanel p = base();
        String title = "Train " + t.getSkill().getName() + " to " + t.getToLevel();
        JLabel top = new JLabel(title);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea body = bodyArea(t.getReason());
        p.add(top, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private static JPanel info(InfoPlanStep s)
    {
        JPanel p = base();

        String label = labelFor(s.getType());
        String title = s.getTitle() == null ? label : s.getTitle();

        JLabel top = new JLabel(label + ": " + title);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.setFont(FontManager.getRunescapeSmallFont());
        top.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        JTextArea body = bodyArea(s.getDetail());
        body.setFont(FontManager.getRunescapeSmallFont());
        body.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        p.add(top, BorderLayout.NORTH);
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

    private static JPanel empty(String title)
    {
        JPanel p = base();
        JLabel top = new JLabel(title);
        p.add(top, BorderLayout.NORTH);
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