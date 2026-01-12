package com.ironpath.ui;

import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/**
 * A JPanel that tracks the viewport width inside a JScrollPane.
 *
 * This prevents horizontal clipping when child components report a wide
 * preferred size (e.g., wrapped JTextArea + BoxLayout in the RuneLite sidebar).
 */
public class ScrollablePanel extends JPanel implements Scrollable
{
    @Override
    public Dimension getPreferredScrollableViewportSize()
    {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return Math.max(visibleRect.height - 32, 32);
    }

    @Override
    public boolean getScrollableTracksViewportWidth()
    {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }
}
