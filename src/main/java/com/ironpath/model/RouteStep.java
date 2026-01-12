package com.ironpath.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * A single row in the canonical route spine.
 *
 * This represents the *order* of progression and can include non-quest steps
 * like miniquests, unlock activities, lamps/diary rewards, and explicit training guidance.
 *
 * Runtime planning produces {@link PlanStep} instances from this spine by
 * simulating completion and inserting required gates.
 */
public final class RouteStep
{
    private final PlanStepType type;

    // QUEST
    private final Quest quest;

    // TRAIN
    private final Skill skill;
    private final Integer toLevel;

    // Generic display
    private final String displayName;
    private final String why;

    // Optional overrides; primarily used for filler tagging for appended quests.
    private final Set<String> tagsOverride;
    private final Map<Skill, Integer> minSkillsOverride;

    private RouteStep(Builder b)
    {
        this.type = b.type;
        this.quest = b.quest;
        this.skill = b.skill;
        this.toLevel = b.toLevel;
        this.displayName = b.displayName;
        this.why = b.why;
        this.tagsOverride = b.tagsOverride == null ? Set.of() : Collections.unmodifiableSet(b.tagsOverride);
        this.minSkillsOverride = b.minSkillsOverride == null ? Map.of() : Collections.unmodifiableMap(b.minSkillsOverride);
    }

    public PlanStepType getType()
    {
        return type;
    }

    public Quest getQuest()
    {
        return quest;
    }

    public Skill getSkill()
    {
        return skill;
    }

    public Integer getToLevel()
    {
        return toLevel;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getWhy()
    {
        return why;
    }

    public Set<String> getTagsOverride()
    {
        return tagsOverride;
    }

    public Map<Skill, Integer> getMinSkillsOverride()
    {
        return minSkillsOverride;
    }

    public static Builder builder(PlanStepType type)
    {
        return new Builder(type);
    }

    public static final class Builder
    {
        private final PlanStepType type;

        private Quest quest;
        private Skill skill;
        private Integer toLevel;
        private String displayName;
        private String why;
        private Set<String> tagsOverride;
        private Map<Skill, Integer> minSkillsOverride;

        private Builder(PlanStepType type)
        {
            this.type = type;
        }

        public Builder quest(Quest quest)
        {
            this.quest = quest;
            return this;
        }

        public Builder skill(Skill skill)
        {
            this.skill = skill;
            return this;
        }

        public Builder toLevel(Integer toLevel)
        {
            this.toLevel = toLevel;
            return this;
        }

        public Builder displayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }

        public Builder why(String why)
        {
            this.why = why;
            return this;
        }

        public Builder tagsOverride(Set<String> tagsOverride)
        {
            this.tagsOverride = tagsOverride;
            return this;
        }

        public Builder minSkillsOverride(Map<Skill, Integer> minSkillsOverride)
        {
            this.minSkillsOverride = minSkillsOverride;
            return this;
        }

        public RouteStep build()
        {
            return new RouteStep(this);
        }
    }
}
