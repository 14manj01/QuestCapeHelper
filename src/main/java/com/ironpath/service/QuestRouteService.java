package com.ironpath.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Singleton;
import com.ironpath.model.PlanStepType;
import com.ironpath.model.RouteStep;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

/**
 * Provides the canonical progression spine.
 *
 * The spine is loaded from a resource file (wiki_route.json) and then completed by appending
 * any missing RuneLite quests so the plugin always remains quest-cape complete.
 *
 * Important: this service provides *order*. Facts (requirements, XP rewards, tags, why) come
 * from {@link QuestDatabase}.
 */
@Slf4j
@Singleton
public class QuestRouteService
{
    private static final Gson GSON = new Gson();
private static final String ROUTE_RESOURCE = "/com/ironpath/wiki_route.json";
    private static final String TAG_FILLER = "filler";

    private final List<RouteStep> spine;

    @Inject
    public QuestRouteService()
    {
        this.spine = buildSpine();
    }

    public List<RouteStep> getSpine()
    {
        return spine;
    }

    private static List<RouteStep> buildSpine()
    {
        final List<RouteStep> out = new ArrayList<>();
        final Set<Quest> addedQuests = new LinkedHashSet<>();

        // 1) Load route steps from JSON.
        List<RouteStepJson> raw = loadJson();
        if (raw != null)
        {
            Map<String, Quest> questIndex = indexByNormalizedName();

            for (RouteStepJson r : raw)
            {
                if (r == null || r.type == null)
                {
                    continue;
                }

                PlanStepType type;
                try
                {
                    type = PlanStepType.valueOf(r.type.trim().toUpperCase(Locale.ROOT));
                }
                catch (Exception e)
                {
                    continue;
                }

                RouteStep.Builder b = RouteStep.builder(type);

                if (type == PlanStepType.QUEST)
                {
                    Quest q = null;

                    // Prefer explicit enum name
                    if (r.quest != null && !r.quest.trim().isEmpty())
                    {
                        try
                        {
                            q = Quest.valueOf(r.quest.trim());
                        }
                        catch (Exception ignored)
                        {
                            // fall through
                        }
                    }

                    // Fallback: resolve via normalized display name
                    if (q == null && r.displayName != null)
                    {
                        q = questIndex.get(normalize(r.displayName));
                    }

                    if (q == null)
                    {
                        // If we cannot resolve to a RuneLite quest, degrade it to NOTE
                        // so the spine order remains visible and deterministic.
                        out.add(RouteStep.builder(PlanStepType.NOTE)
                                .displayName(r.displayName == null ? "Unresolved quest" : r.displayName)
                                .why(r.why == null ? "Unresolved quest row from wiki route." : r.why)
                                .build());
                        continue;
                    }

                    if (addedQuests.contains(q))
                    {
                        continue;
                    }

                    b.quest(q)
                            .displayName(r.displayName != null ? r.displayName : q.getName())
                            .why(r.why);

                    if (r.tags != null && !r.tags.isEmpty())
                    {
                        b.tagsOverride(Set.copyOf(r.tags));
                    }
                    if (r.minSkills != null && !r.minSkills.isEmpty())
                    {
                        b.minSkillsOverride(parseSkillMap(r.minSkills));
                    }

                    out.add(b.build());
                    addedQuests.add(q);
                }
                else if (type == PlanStepType.TRAIN)
                {
                    Skill s = parseSkill(r.skill);
                    if (s == null || r.toLevel == null || r.toLevel < 1)
                    {
                        continue;
                    }
                    out.add(b.skill(s)
                            .toLevel(r.toLevel)
                            .displayName(r.displayName != null ? r.displayName : ("Train " + s.getName()))
                            .why(r.why)
                            .build());
                }
                else
                {
                    out.add(b.displayName(r.displayName).why(r.why).build());
                }
            }
        }

                // 2) Option A: Do NOT append non-wiki steps or remaining quests here.

return out;
    }


    private static List<RouteStepJson> loadJson()
    {
        try (InputStream in = QuestRouteService.class.getResourceAsStream(ROUTE_RESOURCE))
        {
            if (in == null)
            {
                log.warn("Route resource not found: {}", ROUTE_RESOURCE);
                return null;
            }

            Type type = new TypeToken<List<RouteStepJson>>() {}.getType();
            return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), type);
        }
        catch (Exception e)
        {
            log.warn("Failed to load route JSON: {}", ROUTE_RESOURCE, e);
            return null;
        }
    }

    private static Map<String, Quest> indexByNormalizedName()
    {
        // LinkedHashMap to preserve deterministic iteration if ever needed
        java.util.LinkedHashMap<String, Quest> map = new java.util.LinkedHashMap<>();
        for (Quest q : Quest.values())
        {
            String name = q.getName();
            if (name != null)
            {
                map.put(normalize(name), q);
            }

            // Also index by enum constant formatting as a fallback.
            map.putIfAbsent(normalize(q.name().replace('_', ' ')), q);
            map.putIfAbsent(normalize(q.name()), q);
        }
        return map;
    }

    private static String normalize(String s)
    {
        if (s == null)
        {
            return "";
        }

        String t = s.trim().toLowerCase(Locale.ROOT);
        // Keep letters/digits only to be resilient to punctuation differences (e.g., apostrophes, dashes).
        t = t.replaceAll("[^a-z0-9]+", "");
        return t;
    }

    private static Map<Skill, Integer> parseSkillMap(Map<String, Number> raw)
    {
        java.util.EnumMap<Skill, Integer> out = new java.util.EnumMap<>(Skill.class);
        if (raw == null)
        {
            return out;
        }

        for (Map.Entry<String, Number> e : raw.entrySet())
        {
            Skill s = parseSkill(e.getKey());
            if (s == null || e.getValue() == null)
            {
                continue;
            }
            out.put(s, e.getValue().intValue());
        }
        return out;
    }

    private static Skill parseSkill(String raw)
    {
        if (raw == null)
        {
            return null;
        }

        String key = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        // Common aliases found in wiki / JSON exports
        if ("HP".equals(key))
        {
            key = "HITPOINTS";
        }
        if ("HITPOINT".equals(key))
        {
            key = "HITPOINTS";
        }
        if ("RUNECRAFTING".equals(key))
        {
            key = "RUNECRAFT";
        }

        try
        {
            return Skill.valueOf(key);
        }
        catch (IllegalArgumentException ex)
        {
            log.debug("Unknown skill in route JSON: {}", raw);
            return null;
        }
    }


    private static final class RouteStepJson
    {
        private String type;
        private String quest;
        private String displayName;
        private String why;

        private String skill;
        private Integer toLevel;

        private java.util.Set<String> tags;
        private Map<String, Number> minSkills;
    }
}
