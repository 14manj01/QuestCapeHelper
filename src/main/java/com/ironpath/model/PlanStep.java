package com.ironpath.model;

/**
 * A single actionable step in the progression plan.
 *
 * Steps are either a quest to complete or a skill level to train to.
 */
public interface PlanStep
{
    PlanStepType getType();
}
