package com.ironpath.model;

import net.runelite.api.Skill;

/**
 * A non-quest plan step that cannot be verified automatically by the client.
 *
 * The planner assumes the user completes these steps when they appear, and uses them
 * primarily for guidance and for simulated state (e.g., training or lamp allocation).
 */
public final class InfoPlanStep implements PlanStep
{
    private final PlanStepType type;
    private final String title;
    private final String detail;
    private final String wikiUrl;

    // Optional fields (used by TRAIN/LAMP/DIARY guidance)
    private final Skill skill;
    private final Integer fromLevel;
    private final Integer toLevel;
    private final Integer xp; // best-effort, may be null

    private InfoPlanStep(Builder b)
    {
        this.type = b.type;
        this.title = b.title;
        this.detail = b.detail;
        this.wikiUrl = b.wikiUrl;
        this.skill = b.skill;
        this.fromLevel = b.fromLevel;
        this.toLevel = b.toLevel;
        this.xp = b.xp;
    }

    @Override
    public PlanStepType getType()
    {
        return type;
    }

    public String getTitle()
    {
        return title;
    }

    public String getDetail()
    {
        return detail;
    }

    public String getWikiUrl()
    {
        return wikiUrl;
    }

    public Skill getSkill()
    {
        return skill;
    }

    public Integer getFromLevel()
    {
        return fromLevel;
    }

    public Integer getToLevel()
    {
        return toLevel;
    }

    public Integer getXp()
    {
        return xp;
    }

    public static Builder builder(PlanStepType type)
    {
        return new Builder(type);
    }

    public static final class Builder
    {
        private final PlanStepType type;
        private String title;
        private String detail;
        private String wikiUrl;
        private Skill skill;
        private Integer fromLevel;
        private Integer toLevel;
        private Integer xp;

        private Builder(PlanStepType type)
        {
            this.type = type;
        }

        public Builder title(String title)
        {
            this.title = title;
            return this;
        }

        public Builder detail(String detail)
        {
            this.detail = detail;
            return this;
        }

        public Builder wikiUrl(String wikiUrl)
        {
            this.wikiUrl = wikiUrl;
            return this;
        }

        public Builder skill(Skill skill)
        {
            this.skill = skill;
            return this;
        }

        public Builder fromLevel(Integer fromLevel)
        {
            this.fromLevel = fromLevel;
            return this;
        }

        public Builder toLevel(Integer toLevel)
        {
            this.toLevel = toLevel;
            return this;
        }

        public Builder xp(Integer xp)
        {
            this.xp = xp;
            return this;
        }

        public InfoPlanStep build()
        {
            return new InfoPlanStep(this);
        }
    }
}
