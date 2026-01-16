package com.ironpath.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Central, data-driven quest metadata store.
 *
 * The canonical route defines order; this database defines facts:
 * - minimum skill requirements (for readiness gates)
 * - XP rewards (for simulation)
 * - tags (e.g., filler/capstone/unlock)
 * - "why" blurbs
 *
 * All data is loaded from resources so it can evolve without code changes.
 */
@Slf4j
@Singleton
public class QuestDatabase
{
    private static final String RESOURCE_PATH = "/com/ironpath/quest_db.json";

    private final Gson gson;

    private final Map<Quest, QuestMeta> metaByQuest;

    @Inject
    public QuestDatabase(final Gson gson)
    {
        this.gson = gson;
        this.metaByQuest = Collections.unmodifiableMap(load());
    }

    public Map<Skill, Integer> getMinSkills(Quest quest)
    {
        QuestMeta m = metaByQuest.get(quest);
        return m == null ? Map.of() : m.minSkills;
    }

    public Map<Skill, Integer> getXpRewards(Quest quest)
    {
        QuestMeta m = metaByQuest.get(quest);
        return m == null ? Map.of() : m.xpRewards;
    }

    public Set<String> getTags(Quest quest)
    {
        QuestMeta m = metaByQuest.get(quest);
        return m == null ? Set.of() : m.tags;
    }

    public String getWhy(Quest quest)
    {
        QuestMeta m = metaByQuest.get(quest);
        return m == null ? null : m.why;
    }

    private Map<Quest, QuestMeta> load()
    {
        InputStream in = QuestDatabase.class.getResourceAsStream(RESOURCE_PATH);
        if (in == null)
        {
            log.warn("Quest DB resource not found at {}. Using empty metadata.", RESOURCE_PATH);
            return Map.of();
        }

        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8))
        {
            Type t = new TypeToken<Map<String, QuestMetaJson>>() {}.getType();
            Map<String, QuestMetaJson> raw = gson.fromJson(r, t);
            if (raw == null || raw.isEmpty())
            {
                return Map.of();
            }

            Map<Quest, QuestMeta> out = new EnumMap<>(Quest.class);
            int unresolved = 0;

            for (Map.Entry<String, QuestMetaJson> en : raw.entrySet())
            {
                Quest q = resolveQuestEnum(en.getKey());
                if (q == null)
                {
                    unresolved++;
                    continue;
                }

                QuestMetaJson j = en.getValue();
                QuestMeta meta = new QuestMeta();
                meta.why = j.why;

                meta.tags = j.tags == null ? Set.of() : Collections.unmodifiableSet(new HashSet<>(j.tags));

                // minSkills
                Map<Skill, Integer> req = new EnumMap<>(Skill.class);
                if (j.minSkills != null)
                {
                    for (Map.Entry<String, Number> rs : j.minSkills.entrySet())
                    {
                        Skill s = resolveSkillEnum(rs.getKey());
                        if (s != null && rs.getValue() != null)
                        {
                            req.put(s, rs.getValue().intValue());
                        }
                    }
                }
                meta.minSkills = Collections.unmodifiableMap(req);

                // xpRewards
                Map<Skill, Integer> xp = new EnumMap<>(Skill.class);
                if (j.xpRewards != null)
                {
                    for (Map.Entry<String, Number> xs : j.xpRewards.entrySet())
                    {
                        Skill s = resolveSkillEnum(xs.getKey());
                        if (s != null && xs.getValue() != null)
                        {
                            xp.put(s, xs.getValue().intValue());
                        }
                    }
                }
                meta.xpRewards = Collections.unmodifiableMap(xp);

                out.put(q, meta);
            }

            if (unresolved > 0)
            {
                log.warn("Quest DB had {} entries that did not map to a RuneLite Quest enum.", unresolved);
            }

            return out;
        }
        catch (Exception e)
        {
            log.warn("Failed to load quest metadata; using empty metadata.", e);
            return Map.of();
        }
    }

    private static Quest resolveQuestEnum(String s)
    {
        if (s == null)
        {
            return null;
        }
        String key = s.trim();
        if (key.isEmpty())
        {
            return null;
        }

        try
        {
            return Quest.valueOf(key);
        }
        catch (IllegalArgumentException ignored)
        {
            // Not an enum constant name.
        }

        // Best-effort: match by Quest.getName()
        String norm = normalizeName(key);
        for (Quest q : Quest.values())
        {
            if (normalizeName(q.getName()).equals(norm))
            {
                return q;
            }
        }
        return null;
    }

    private static Skill resolveSkillEnum(String s)
    {
        if (s == null)
        {
            return null;
        }
        String key = s.trim();
        if (key.isEmpty())
        {
            return null;
        }
        try
        {
            return Skill.valueOf(key);
        }
        catch (IllegalArgumentException ignored)
        {
            // not enum name
        }

        // Best-effort common variants.
        String upper = key.toUpperCase();
        upper = upper.replace(' ', '_').replace('-', '_');
        try
        {
            return Skill.valueOf(upper);
        }
        catch (IllegalArgumentException ignored)
        {
            return null;
        }
    }

    private static String normalizeName(String s)
    {
        String lower = s == null ? "" : s.toLowerCase();
        lower = lower.replaceAll("[^a-z0-9]", "");
        return lower;
    }

    private static final class QuestMeta
    {
        private Map<Skill, Integer> minSkills = Map.of();
        private Map<Skill, Integer> xpRewards = Map.of();
        private Set<String> tags = Set.of();
        private String why;
    }

    private static final class QuestMetaJson
    {
        private Map<String, Number> minSkills;
        private Map<String, Number> xpRewards;
        private Set<String> tags;
        private String why;
    }
}
