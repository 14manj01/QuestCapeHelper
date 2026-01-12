package com.ironpath.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import net.runelite.client.ui.ColorScheme;

final class ChipButton extends JButton
{
    ChipButton(String text)
    {
        super(text);
        setFocusable(false);
        setFont(getFont().deriveFont(Font.PLAIN, 11f));
        setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        setOpaque(true);
        super.setSelected(false);
        updateBackground();

        addActionListener(e ->
        {
            setSelected(!isSelected());
        });
    }

    @Override
    public boolean isSelected()
    {
        return super.isSelected();
    }

    @Override
    public void setSelected(boolean selected)
    {
        super.setSelected(selected);
        updateBackground();
    }

    private void updateBackground()
    {
        final Color bg = isSelected() ? ColorScheme.DARKER_GRAY_COLOR : new Color(54, 54, 54);
        setBackground(bg);
        repaint();
    }
}
