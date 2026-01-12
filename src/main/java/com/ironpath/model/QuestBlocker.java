package com.ironpath.model;

import net.runelite.api.Skill;

public final class QuestBlocker
{
    public enum Type
    {
        PREREQUISITE_QUEST,
        SKILL_LEVEL
    }

    private final Type type;
    private final String questName;
    private final Skill skill;
    private final int have;
    private final int need;

    private QuestBlocker(Type type, String questName, Skill skill, int have, int need)
    {
        this.type = type;
        this.questName = questName;
        this.skill = skill;
        this.have = have;
        this.need = need;
    }

    public static QuestBlocker prereqQuest(String questName)
    {
        return new QuestBlocker(Type.PREREQUISITE_QUEST, questName, null, 0, 0);
    }

    public static QuestBlocker skill(Skill skill, int have, int need)
    {
        return new QuestBlocker(Type.SKILL_LEVEL, null, skill, have, need);
    }

    public Type getType()
    {
        return type;
    }

    public String getQuestName()
    {
        return questName;
    }

    public Skill getSkill()
    {
        return skill;
    }

    public int getHave()
    {
        return have;
    }

    public int getNeed()
    {
        return need;
    }
}
