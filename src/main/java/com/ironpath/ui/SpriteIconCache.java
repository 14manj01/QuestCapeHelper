package com.ironpath.ui;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;

/**
 * Small UI-only cache for OSRS cache sprites (via RuneLite SpriteManager).
 *
 * Goals:
 * - Avoid repeated fallback->sprite swapping across frequent panel rebuilds.
 * - Ensure sprite requests occur on the client thread.
 * - Update any labels that are currently showing the sprite when it becomes available.
 */
final class SpriteIconCache
{
    private static final Map<Integer, ImageIcon> CACHE = new ConcurrentHashMap<>();
    private static final Set<Integer> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Map<Integer, CopyOnWriteArrayList<WeakReference<JLabel>>> WATCHERS = new ConcurrentHashMap<>();

    private SpriteIconCache()
    {
    }

    static void attach(JLabel label, SpriteManager spriteManager, ClientThread clientThread, int spriteId, int size)
    {
        if (label == null)
        {
            return;
        }

        ImageIcon cached = CACHE.get(spriteId);
        if (cached != null)
        {
            label.setIcon(cached);
            return;
        }

        if (spriteManager == null)
        {
            // No sprite manager available; leave icon empty but preserve slot sizing in the caller.
            return;
        }

        // Register the label so it will be updated when the sprite is available.
        WATCHERS.computeIfAbsent(spriteId, k -> new CopyOnWriteArrayList<>()).add(new WeakReference<>(label));

        // Only one load per spriteId.
        if (!IN_FLIGHT.add(spriteId))
        {
            return;
        }

        Runnable request = () ->
        {
            BufferedImage img = spriteManager.getSprite(spriteId, 0);
            if (img != null)
            {
                publish(spriteId, img, size);
                return;
            }

            spriteManager.getSpriteAsync(spriteId, 0, sprite ->
            {
                if (sprite != null)
                {
                    publish(spriteId, sprite, size);
                }
                else
                {
                    IN_FLIGHT.remove(spriteId);
                }
            });
        };

        if (clientThread != null)
        {
            clientThread.invokeLater(request);
        }
        else
        {
            request.run();
        }
    }

    private static void publish(int spriteId, BufferedImage img, int size)
    {
        ImageIcon icon = new ImageIcon(scale(img, size));
        CACHE.put(spriteId, icon);
        IN_FLIGHT.remove(spriteId);

        CopyOnWriteArrayList<WeakReference<JLabel>> list = WATCHERS.get(spriteId);
        if (list == null)
        {
            return;
        }

        SwingUtilities.invokeLater(() ->
        {
            Iterator<WeakReference<JLabel>> it = list.iterator();
            while (it.hasNext())
            {
                JLabel l = it.next().get();
                if (l == null)
                {
                    it.remove();
                    continue;
                }
                l.setIcon(icon);
                l.revalidate();
                l.repaint();
            }
        });
    }

    private static Image scale(BufferedImage img, int size)
    {
        return img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
    }
}
