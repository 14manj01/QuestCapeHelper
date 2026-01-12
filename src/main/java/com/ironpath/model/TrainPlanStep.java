package com.ironpath.model;

import net.runelite.api.Skill;

/**
 * A plan step representing manual skilling needed to unlock the next quest.
 */
public final class TrainPlanStep implements PlanStep
{
    private final Skill skill;
    private final int fromLevel;
    private final int toLevel;
    private final String reason;

    public TrainPlanStep(Skill skill, int fromLevel, int toLevel, String reason)
    {
        this.skill = skill;
        this.fromLevel = fromLevel;
        this.toLevel = toLevel;
        this.reason = reason;
    }

    @Override
    public PlanStepType getType()
    {
        return PlanStepType.TRAIN;
    }

    public Skill getSkill()
    {
        return skill;
    }

    public int getFromLevel()
    {
        return fromLevel;
    }

    public int getToLevel()
    {
        return toLevel;
    }

    public String getReason()
    {
        return reason;
    }
}
