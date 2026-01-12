package com.ironpath.service;

import com.google.inject.Singleton;
import com.ironpath.model.QuestEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;

@Singleton
public class QuestStatusService
{
    private final Client client;
    private final Map<String, Quest> byNormalizedName = new HashMap<>();

    @Inject
    public QuestStatusService(Client client)
    {
        this.client = client;

        for (Quest q : Quest.values())
        {
            byNormalizedName.put(normalize(q.getName()), q);
        }
    }

    /**
     * Resolves a quest name (legacy string) to the canonical RuneLite Quest enum.
     * This exists only as a bridge while prereqs are still stored as strings.
     */
    public Quest resolveQuest(String questName)
    {
        if (questName == null)
        {
            return null;
        }
        return byNormalizedName.get(normalize(questName));
    }

    public QuestState getState(QuestEntry entry)
    {
        if (entry == null)
        {
            return QuestState.NOT_STARTED;
        }

        final Quest direct = entry.getQuest();
        if (direct != null)
        {
            return direct.getState(client);
        }

        // Legacy fallback only. Route entries should be backed by Quest to avoid localization issues.
        final Quest q = resolveQuest(entry.getQuestName());
        return q == null ? QuestState.NOT_STARTED : q.getState(client);
    }

    public QuestState getStateByName(String questName)
    {
        final Quest q = resolveQuest(questName);
        return q == null ? QuestState.NOT_STARTED : q.getState(client);
    }

    public int countCompleted(List<QuestEntry> route)
    {
        int done = 0;
        for (QuestEntry e : route)
        {
            if (getState(e) == QuestState.FINISHED)
            {
                done++;
            }
        }
        return done;
    }

    public QuestEntry findNext(List<QuestEntry> route)
    {
        for (QuestEntry e : route)
        {
            final QuestState state = getState(e);
            if (state != QuestState.FINISHED)
            {
                return e;
            }
        }
        return null;
    }

    private static String normalize(String s)
    {
        if (s == null)
        {
            return "";
        }

        final String lower = s.toLowerCase(Locale.ROOT);
        final StringBuilder sb = new StringBuilder(lower.length());

        for (int i = 0; i < lower.length(); i++)
        {
            final char c = lower.charAt(i);
            if (Character.isLetterOrDigit(c))
            {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
