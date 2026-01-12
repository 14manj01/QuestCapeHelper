package com.ironpath.model;

import net.runelite.api.QuestState;

/**
 * A plan step representing a quest to complete.
 */
public final class QuestPlanStep implements PlanStep
{
    private final QuestEntry entry;
    private final QuestState state;

    public QuestPlanStep(QuestEntry entry, QuestState state)
    {
        this.entry = entry;
        this.state = state;
    }

    @Override
    public PlanStepType getType()
    {
        return PlanStepType.QUEST;
    }

    public QuestEntry getEntry()
    {
        return entry;
    }

    public QuestState getState()
    {
        return state;
    }
}
