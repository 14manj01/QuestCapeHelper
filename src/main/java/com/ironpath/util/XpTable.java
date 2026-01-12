package com.ironpath.util;

/**
 * OSRS experience table helpers.
 *
 * RuneLite's API does not expose exact per-skill XP for an Ironman plan simulation,
 * so we convert from a conservative baseline: the minimum XP for the current real level.
 *
 * This still improves correctness over heuristics when applying quest XP rewards.
 */
public final class XpTable
{
    private static final int MAX_LEVEL = 126;
    private static final int[] XP_FOR_LEVEL = build();

    private XpTable() {}

    public static int xpForLevel(int level)
    {
        int l = clamp(level, 1, MAX_LEVEL);
        return XP_FOR_LEVEL[l];
    }

    public static int levelForXp(int xp)
    {
        int x = Math.max(0, xp);
        int lo = 1;
        int hi = MAX_LEVEL;

        while (lo < hi)
        {
            int mid = (lo + hi + 1) >>> 1;
            if (XP_FOR_LEVEL[mid] <= x)
            {
                lo = mid;
            }
            else
            {
                hi = mid - 1;
            }
        }

        return lo;
    }

    private static int[] build()
    {
        // Standard RuneScape formula. Index 0 unused; XP_FOR_LEVEL[level] = minimum XP for that level.
        int[] xp = new int[MAX_LEVEL + 1];
        int points = 0;

        xp[1] = 0;
        for (int lvl = 2; lvl <= MAX_LEVEL; lvl++)
        {
            points += (int) Math.floor(lvl - 1 + 300.0 * Math.pow(2.0, (lvl - 1) / 7.0));
            xp[lvl] = points / 4;
        }
        return xp;
    }

    private static int clamp(int v, int lo, int hi)
    {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }
}
