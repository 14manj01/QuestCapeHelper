package com.ironpath.ui;

import com.ironpath.model.QuestEntry;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Image;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.api.QuestState;
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;

public final class QuestCard
{
    private static final int PAD = 10;
    private static final int ICON_SIZE = 16;

    // Fallback icon only. Real icons are pulled from the OSRS cache via SpriteManager.
    // If the resource is missing, the layout still reserves space and stays stable.
    private static final ImageIcon FALLBACK_QUEST_ICON = loadIcon("icons/quest.png");

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
            card.add(wikiPill(recommended), c);
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
        return compact(null, entry, state, -1, -1);
    }

    public static JPanel compact(QuestEntry entry, QuestState state, int spineIndex, int spineTotal)
    {
        return compact(null, entry, state, spineIndex, spineTotal);
    }

    public static JPanel compact(SpriteManager spriteManager, QuestEntry entry, QuestState state, int spineIndex, int spineTotal)
    {
        return compact(spriteManager, null, entry, state, spineIndex, spineTotal);
    }

    public static JPanel compact(SpriteManager spriteManager, ClientThread clientThread, QuestEntry entry, QuestState state, int spineIndex, int spineTotal)
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
        // RuneLite SpriteID doesn't expose a consistent "QUESTS_TAB" constant across versions.
        // Use the quests page icon sprite, which is stable and matches the in-game quests UI.
        card.add(iconTitleRow(spriteManager, clientThread, SpriteID.QUESTS_PAGE_ICON_BLUE_QUESTS, FALLBACK_QUEST_ICON, entry.getQuestName()), c);

        // Row 2: "Step X of Y" (left) + Quest Guide (right)
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

        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        card.add(wikiPill(entry), c);

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


    private static String resolveWikiUrl(QuestEntry entry)
    {
        if (entry != null)
        {
            String u = entry.getWikiUrl();
            if (u != null && !u.trim().isEmpty())
            {
                return u.trim();
            }

            String name = entry.getQuestName();
            if (name != null && !name.trim().isEmpty())
            {
                final String slug = name.trim().replace(' ', '_');
                try
                {
                    return "https://oldschool.runescape.wiki/w/" + java.net.URLEncoder.encode(slug, java.nio.charset.StandardCharsets.UTF_8)
                            .replace("+", "%20");
                }
                catch (Exception ignored)
                {
                    // fall through
                }
            }
        }
        return "https://oldschool.runescape.wiki/";
    }
    private static JTextArea wrapTextFlush(String text, float fontSize, boolean bold)
    {
        JTextArea area = new JTextArea(text == null ? "" : text);
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

    private static JPanel wikiPill(QuestEntry entry)
    {
        final String url = resolveWikiUrl(entry);

        JButton b = new JButton("Quest Guide");
        b.setFocusable(false);
        b.setFont(FontManager.getRunescapeSmallFont());
        b.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        b.addActionListener(e -> LinkBrowser.browse(url));

        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(b);

        Dimension pref = wrap.getPreferredSize();
        wrap.setMaximumSize(new Dimension(pref.width, pref.height));
        return wrap;
    }

    private static JPanel iconTitleRow(SpriteManager spriteManager, ClientThread clientThread, int spriteId, ImageIcon fallbackIcon, String titleText)
    {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints r = new GridBagConstraints();
        r.gridy = 0;
        r.anchor = GridBagConstraints.NORTHWEST;
        r.insets = new Insets(0, 0, 0, 6);

        // Icon (fixed slot, prevents text jitter)
        JLabel iconLabel = buildSpriteIconLabel(spriteManager, clientThread, spriteId, fallbackIcon);
        // OSRS cache sprites often include a small amount of transparent padding.
        // Nudge the icon up slightly so it aligns better with the title text.
        iconLabel.setBorder(BorderFactory.createEmptyBorder(-2, 0, 0, 0));
        iconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setMinimumSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLabel.setMaximumSize(new Dimension(ICON_SIZE, ICON_SIZE));
        r.gridx = 0;
        r.weightx = 0;
        row.add(iconLabel, r);

        // Title (wrapping)
        r.gridx = 1;
        r.weightx = 1;
        r.fill = GridBagConstraints.HORIZONTAL;
        r.insets = new Insets(0, 0, 0, 0);
        JTextArea title = wrapTextFlush(titleText == null ? "" : titleText, 12f, true);
        // Title row is tight, so no extra right padding.
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        row.add(title, r);

        return row;
    }

    private static JLabel buildSpriteIconLabel(SpriteManager spriteManager, ClientThread clientThread, int spriteId, ImageIcon fallbackIcon)
    {
        JLabel label = new JLabel();
        label.setOpaque(false);

        if (spriteManager == null)
        {
            if (fallbackIcon != null)
            {
                label.setIcon(fallbackIcon);
            }
            return label;
        }

        // Always set fallback first so the layout is stable even if sprite loading is delayed.
        if (fallbackIcon != null)
        {
            label.setIcon(fallbackIcon);
        }

        // SpriteManager access is safest from the client thread.
        Runnable request = () ->
        {
            BufferedImage img = spriteManager.getSprite(spriteId, 0);
            if (img != null)
            {
                SwingUtilities.invokeLater(() ->
                {
                    label.setIcon(new ImageIcon(scale(img)));
                    label.revalidate();
                    label.repaint();
                });
                return;
            }

            spriteManager.getSpriteAsync(spriteId, 0, sprite ->
            {
                if (sprite == null)
                {
                    return;
                }

                SwingUtilities.invokeLater(() ->
                {
                    label.setIcon(new ImageIcon(scale(sprite)));
                    label.revalidate();
                    label.repaint();
                });
            });
        };

        if (clientThread != null)
        {
            clientThread.invokeLater(request);
        }
        else
        {
            // Best-effort if clientThread isn't available.
            request.run();
        }
        return label;
    }

    private static Image scale(java.awt.image.BufferedImage img)
    {
        return img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
    }

    private static ImageIcon loadIcon(String relPath)
    {
        try
        {
            java.net.URL u = QuestCard.class.getResource(relPath.startsWith("/") ? relPath : "/com/ironpath/ui/" + relPath);
            if (u == null)
            {
                return null;
            }

            Image img = javax.imageio.ImageIO.read(u);
            if (img == null)
            {
                return null;
            }

            Image scaled = img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        catch (Exception ignored)
        {
            return null;
        }
    }


    private static void forceFillWidth(JPanel panel)
    {
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}