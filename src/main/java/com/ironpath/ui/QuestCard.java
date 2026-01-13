package com.ironpath.ui;

import com.ironpath.model.QuestEntry;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.api.QuestState;
import net.runelite.client.ui.ColorScheme;

public final class QuestCard
{
    private static final int PAD = 10;

    private QuestCard() {}

    public static JPanel summary(int completed, int total, QuestEntry recommended, QuestState state)
    {
        JPanel card = baseCard();
        card.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 0, 0);

        // Progress line (left)
        JTextArea progress = wrapTextFlush("Progress: " + completed + " / " + total, 13f, true);
        card.add(progress, c);

        // Pill (right)
        if (recommended != null)
        {
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHEAST;
            card.add(pillForState(state), c);
        }

        // Title + reason
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(8, 0, 0, 0);

        if (recommended == null)
        {
            card.add(wrapTextFlush("Quest cape achieved.", 11f, false), c);
        }
        else
        {
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new GridBagLayout());

            GridBagConstraints b = new GridBagConstraints();
            b.gridx = 0;
            b.gridy = 0;
            b.weightx = 1;
            b.fill = GridBagConstraints.HORIZONTAL;
            b.anchor = GridBagConstraints.NORTHWEST;
            b.insets = new Insets(0, 0, 0, 0);

            body.add(wrapTextFlush(recommended.getQuestName(), 13f, true), b);

            String why = recommended.getShortWhy();
            if (why == null || why.isBlank())
            {
                why = "Next best quest in your route.";
            }

            b.gridy = 1;
            b.insets = new Insets(4, 0, 0, 0);
            body.add(wrapTextFlush(why, 11f, false), b);

            card.add(body, c);
        }

        forceFillWidth(card);
        return card;
    }

public static JPanel compact(QuestEntry entry, QuestState state)
    {
        return compact(entry, state, -1, -1);
    }

    public static JPanel compact(QuestEntry entry, QuestState state, int spineIndex, int spineTotal)
    {
        JPanel card = baseCard();
        card.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 0, 8);

        // WRAPPING TITLE (left, takes remaining width)
        JTextArea title = wrapTextFlush(entry.getQuestName(), 12f, true);
        card.add(title, c);

        // Right header: progress + pill
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new Insets(0, 0, 0, 0);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new javax.swing.BoxLayout(right, javax.swing.BoxLayout.Y_AXIS));

        JLabel progress = new JLabel(formatProgressCompact(spineIndex, spineTotal));
        progress.setFont(progress.getFont().deriveFont(Font.PLAIN, 10f));
        progress.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        progress.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel pill = pillForState(state);
        pill.setAlignmentX(Component.RIGHT_ALIGNMENT);

        if (progress.getText() != null && !progress.getText().isBlank())
        {
            right.add(progress);
            right.add(javax.swing.Box.createVerticalStrut(2));
        }
        right.add(pill);

        card.add(right, c);

        // Step line under title (full width)
        String stepLine = formatProgressLong(spineIndex, spineTotal);
        if (stepLine != null && !stepLine.isBlank())
        {
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(2, 0, 0, 0);

            JLabel step = new JLabel(stepLine);
            step.setFont(step.getFont().deriveFont(Font.PLAIN, 10.5f));
            step.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
            card.add(step, c);
        }

        // Reason (full width under step line)
        String why = entry.getShortWhy();
        if (why != null && !why.isBlank())
        {
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(6, 0, 0, 0);

            card.add(wrapTextFlush(why, 11f, false), c);
        }

        forceFillWidth(card);
        return card;
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

    private static JPanel baseCard()
    {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1, true),
                BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD)
        ));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static JTextArea wrapTextFlush(String text, float fontSize, boolean bold)
    {
        JTextArea area = new JTextArea(text == null ? "" : text);
        // Keep columns small so preferred width doesn't exceed the sidebar.
        area.setColumns(1);
        area.setRows(1);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);

        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        Font f = new JLabel().getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, fontSize);
        area.setFont(f);
        area.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        area.setMargin(new Insets(0, 0, 0, 0));
        // Small right gutter so wrapped lines don't touch the card edge and don't run under the scrollbar.
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        // Do not inflate preferred width (prevents right-edge clipping when horizontal
        // scrolling is disabled). Wrapping is handled by the viewport width.

        // Prevent width inflation but allow height growth
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMinimumSize(new Dimension(0, 0));
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
	return area;
    }

    private static JPanel pillForState(QuestState state)
    {
        String text;
        switch (state)
        {
            case FINISHED:
                text = "Done";
                break;
            case IN_PROGRESS:
                text = "In progress";
                break;
            case NOT_STARTED:
            default:
                text = "Ready";
                break;
        }

        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(true);
        wrap.setBackground(ColorScheme.DARK_GRAY_COLOR);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR, 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        wrap.add(label);

        Dimension pref = wrap.getPreferredSize();
        wrap.setMaximumSize(new Dimension(pref.width, pref.height));
        return wrap;
    }

    private static void forceFillWidth(JPanel panel)
    {
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
