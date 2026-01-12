package com.ironpath.ui;

import com.ironpath.model.QuestReadiness;
import com.ironpath.model.QuestRow;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

final class StatusPill
{
    private StatusPill() {}

    static JPanel build(QuestRow row)
    {
        final String text;
        final Color bg;

        final QuestReadiness r = row.getReadiness();
        switch (r)
        {
            case DONE:
                text = "Done";
                bg = new Color(54, 94, 64);
                break;
            case IN_PROGRESS:
                text = "In progress";
                bg = new Color(88, 86, 58);
                break;
            case BLOCKED:
                text = "Blocked";
                bg = new Color(92, 64, 64);
                break;
            case READY:
            default:
                text = "Ready";
                bg = new Color(56, 76, 94);
                break;
        }

        final JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        final JPanel pill = new JPanel();
        pill.setBackground(bg);
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        pill.add(label);
        return pill;
    }
}
