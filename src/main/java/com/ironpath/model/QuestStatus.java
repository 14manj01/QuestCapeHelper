package com.ironpath.model;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;

public final class QuestStatus
{
    private final Quest quest;
    private final QuestState state;

    public QuestStatus(Quest quest, QuestState state)
    {
        this.quest = quest;
        this.state = state;
    }

    public Quest getQuest()
    {
        return quest;
    }

    public QuestState getState()
    {
        return state;
    }

    public boolean isCompleted()
    {
        return state == QuestState.FINISHED;
    }

    public boolean isInProgress()
    {
        return state == QuestState.IN_PROGRESS;
    }
}
