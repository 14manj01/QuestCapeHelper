package com.ironpath.model;

/**
 * UI metadata wrapper for a plan step that preserves its absolute position within the wiki route spine.
 * This keeps Option A behavior intact: the planner still follows the spine, we just display context.
 */
public final class SpineStepView
{
    private final PlanStep step;
    private final int spineIndex; // 0-based index in wiki_route spine
    private final int spineTotal;

    public SpineStepView(PlanStep step, int spineIndex, int spineTotal)
    {
        this.step = step;
        this.spineIndex = spineIndex;
        this.spineTotal = spineTotal;
    }

    public PlanStep getStep()
    {
        return step;
    }

    public int getSpineIndex()
    {
        return spineIndex;
    }

    public int getSpineTotal()
    {
        return spineTotal;
    }
}
