package com.ironpath.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
 
import net.runelite.api.SpriteID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * Quest-like card styling for MINIQUEST steps.
 * Pure UI component: does not change planner behavior.
 */
public final class MiniquestCard
{
    private static final int ICON_SIZE = 16;

    // Icons are sourced from OSRS cache sprites via SpriteManager.
    // We intentionally do not use bundled fallbacks to avoid icon swapping during refreshes.

    private MiniquestCard() {}

    public static JPanel compact(String titleText, String detail, int spineIndex, int spineTotal)
    {
        return compact(null, titleText, detail, null, spineIndex, spineTotal);
    }

    public static JPanel compact(SpriteManager spriteManager, String titleText, String detail, String wikiUrl, int spineIndex, int spineTotal)
    {
        return compact(spriteManager, null, titleText, detail, wikiUrl, spineIndex, spineTotal);
    }

    public static JPanel compact(SpriteManager spriteManager, ClientThread clientThread, String titleText, String detail, String wikiUrl, int spineIndex, int spineTotal)
    {
        // Use the quests page icon sprite from the OSRS cache.
        return compact(spriteManager, clientThread, titleText, detail, wikiUrl, spineIndex, spineTotal, SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS);
    }

    public static JPanel compact(String titleText, String detail, String wikiUrl, int spineIndex, int spineTotal)
    {
        return compact(null, titleText, detail, wikiUrl, spineIndex, spineTotal);
    }

    private static JPanel compact(SpriteManager spriteManager, ClientThread clientThread, String titleText, String detail, String wikiUrl, int spineIndex, int spineTotal, int spriteId)
    {
        JPanel card = baseCard();
        card.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        // Row 1: icon + title (tight)
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 0, 0);
        card.add(iconTitleRow(spriteManager, clientThread, spriteId, titleText == null ? "" : titleText), c);

        // Row 2: "Step X of Y" (left) + Quest Guide (right, if present)
        String stepLine = formatProgressLong(spineIndex, spineTotal);

        c.gridwidth = 1;
        c.gridy = 1;
        c.insets = new Insets(2, 0, 0, 0);

        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        JLabel step = new JLabel(stepLine);
        step.setFont(step.getFont().deriveFont(Font.PLAIN, 10.5f));
        step.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
        card.add(step, c);

        if (wikiUrl != null && !wikiUrl.trim().isEmpty())
        {
            c.gridx = 1;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHEAST;
            card.add(wikiPill(wikiUrl), c);
        }

        // Detail text (full width)
        if (detail != null && !detail.isBlank())
        {
            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 2;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(6, 0, 0, 0);

            card.add(wrapTextFlush(detail, 11f, false), c);
        }

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
            BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }

    private static JTextArea wrapTextFlush(String text, float fontSize, boolean bold)
    {
        JTextArea area = new JTextArea(text);
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
        area.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        // Keep preferred width small so the card never exceeds the sidebar.
        area.setColumns(1);

        return area;
    }

    private static JPanel iconTitleRow(SpriteManager spriteManager, ClientThread clientThread, int spriteId, String titleText)
    {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints r = new GridBagConstraints();
        r.gridy = 0;
        r.anchor = GridBagConstraints.NORTHWEST;
        r.insets = new Insets(0, 0, 0, 6);

        JLabel iconLabel = new JLabel();
        iconLabel.setOpaque(false);
        // Attach the OSRS sprite icon (cached) without fallbacks to avoid swapping during refreshes.
        SpriteIconCache.attach(iconLabel, spriteManager, clientThread, spriteId, ICON_SIZE);
        iconLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        // OSRS cache sprites often include a small amount of transparent padding.
        // Nudge the icon up slightly so it aligns better with the title text.
        iconLabel.setBorder(BorderFactory.createEmptyBorder(-2, 0, 0, 0));
        iconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setMinimumSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setMaximumSize(new Dimension(ICON_SIZE, ICON_SIZE));

        r.gridx = 0;
        r.weightx = 0;
        row.add(iconLabel, r);

        r.gridx = 1;
        r.weightx = 1;
        r.fill = GridBagConstraints.HORIZONTAL;
        r.insets = new Insets(0, 0, 0, 0);
        JTextArea title = wrapTextFlush(titleText == null ? "" : titleText, 12f, true);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        row.add(title, r);

        return row;
    }

    private static JPanel wikiPill(String wikiUrl)
    {
        JButton b = new JButton("Quest Guide");
        b.setFocusable(false);
        b.setFont(FontManager.getRunescapeSmallFont());
        b.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        b.addActionListener(e -> LinkBrowser.browse(wikiUrl.trim()));

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(b);

        Dimension pref = wrap.getPreferredSize();
        wrap.setMaximumSize(new Dimension(pref.width, pref.height));
        return wrap;
    }


}
