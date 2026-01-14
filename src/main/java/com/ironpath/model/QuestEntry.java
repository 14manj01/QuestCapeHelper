package com.ironpath.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Data model for a single quest in the route.
 *
 * This plugin intentionally keeps requirements conservative. Fields can be expanded over time.
 */
public final class QuestEntry
{
    /**
     * Canonical quest identifier from the RuneLite API.
     *
     * This is the source of truth for quest state and is intentionally NOT derived from user-provided strings.
     * Using {@link Quest} avoids brittle name matching issues (e.g. "Vampyre" vs "Vampire").
     */
    private final Quest quest;

    /**
     * Fallback display name for any temporary/legacy entries that are not backed by a {@link Quest}.
     * Prefer {@link #quest} wherever possible.
     */
    private final String questName;
    private final String shortWhy;
    private final String wikiUrl;

    /** Minimum tracked skill requirements (subset). */
    private final Map<Skill, Integer> minSkills;

    /** Tracked prerequisite quests by display name (subset). */
    private final List<String> prereqQuestNames;

    /** Optional XP reward weights (subset). */
    private final Map<Skill, Integer> xpRewards;

    /** Tags used for scoring and grouping. */
    private final Set<String> tags;

    public QuestEntry(Quest quest, String shortWhy)
    {
        this(quest, shortWhy, null, Map.of(), List.of(), Map.of(), Set.of());
    }

    public QuestEntry(Quest quest, String shortWhy, String wikiUrl)
    {
        this(quest, shortWhy, wikiUrl, Map.of(), List.of(), Map.of(), Set.of());
    }

    public QuestEntry(Quest quest, String shortWhy, Map<Skill, Integer> minSkills)
    {
        this(quest, shortWhy, null, minSkills, List.of(), Map.of(), Set.of());
    }

    public QuestEntry(
            Quest quest,
            String shortWhy,
            String wikiUrl,
            Map<Skill, Integer> minSkills,
            List<String> prereqQuestNames,
            Map<Skill, Integer> xpRewards,
            Set<String> tags)
    {
        this.quest = quest;
        this.questName = quest != null ? quest.getName() : "";
        this.shortWhy = shortWhy;
        this.wikiUrl = wikiUrl;
        this.minSkills = minSkills == null ? Collections.emptyMap() : Collections.unmodifiableMap(minSkills);
        this.prereqQuestNames = prereqQuestNames == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(prereqQuestNames));
        this.xpRewards = xpRewards == null ? Collections.emptyMap() : Collections.unmodifiableMap(xpRewards);
        this.tags = tags == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(tags));
    }

    public QuestEntry(String questName, String shortWhy)
    {
        this(null, questName, shortWhy, null, Map.of(), List.of(), Map.of(), Set.of());
    }

    public QuestEntry(String questName, String shortWhy, Map<Skill, Integer> minSkills)
    {
        this(null, questName, shortWhy, null, minSkills, List.of(), Map.of(), Set.of());
    }

    public QuestEntry(
            String questName,
            String shortWhy,
            Map<Skill, Integer> minSkills,
            List<String> prereqQuestNames,
            Map<Skill, Integer> xpRewards,
            Set<String> tags)
    {
        this(null, questName, shortWhy, null, minSkills, prereqQuestNames, xpRewards, tags);
    }

    private QuestEntry(
            Quest quest,
            String questName,
            String shortWhy,
            String wikiUrl,
            Map<Skill, Integer> minSkills,
            List<String> prereqQuestNames,
            Map<Skill, Integer> xpRewards,
            Set<String> tags)
    {
        this.quest = quest;
        this.questName = questName;
        this.shortWhy = shortWhy;
        this.wikiUrl = wikiUrl;
        this.minSkills = minSkills == null ? Collections.emptyMap() : Collections.unmodifiableMap(minSkills);
        this.prereqQuestNames = prereqQuestNames == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(prereqQuestNames));
        this.xpRewards = xpRewards == null ? Collections.emptyMap() : Collections.unmodifiableMap(xpRewards);
        this.tags = tags == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(tags));
    }

    public Quest getQuest()
    {
        return quest;
    }

    public String getQuestName()
    {
        return quest != null ? quest.getName() : questName;
    }

    public String getShortWhy()
    {
        return shortWhy;
    }

    public String getWikiUrl()
    {
        return wikiUrl;
    }

    public Map<Skill, Integer> getMinSkills()
    {
        return minSkills;
    }

    public List<String> getPrereqQuestNames()
    {
        return prereqQuestNames;
    }

    public Map<Skill, Integer> getXpRewards()
    {
        return xpRewards;
    }

    public Set<String> getTags()
    {
        return tags;
    }
}
