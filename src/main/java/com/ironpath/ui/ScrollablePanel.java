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
        // RuneLite's sidebar layout can honor a scrollpane's preferred size. During a refresh
        // (e.g., after removeAll() and before all cards are re-added), our preferred height can
        // momentarily shrink to a single card, causing the entire list area to collapse.
        //
        // Provide a stable minimum viewport height so the scroll area stays full-sized.
        Dimension pref = getPreferredSize();

        int minH = 400;
        if (getParent() != null)
        {
            // If we're already inside a viewport, prefer its current height.
            minH = Math.max(minH, getParent().getHeight());
        }

        return new Dimension(pref.width, Math.max(pref.height, minH));
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
