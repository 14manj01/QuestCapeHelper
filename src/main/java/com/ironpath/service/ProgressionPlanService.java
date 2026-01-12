package com.ironpath.service;

import com.google.inject.Singleton;
import com.ironpath.model.InfoPlanStep;
import com.ironpath.model.PlanStep;
import com.ironpath.model.PlanStepType;
import com.ironpath.model.QuestEntry;
import com.ironpath.model.QuestPlanStep;
import com.ironpath.model.RouteStep;
import com.ironpath.model.TrainPlanStep;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import com.ironpath.service.QuestDatabase;

/**
 * Builds an actionable "next steps" plan from the canonical route spine.
 *
 * Behavior:
 * - Produces a deterministic list of up to N steps.
 * - Steps are either a quest, an explicit training gate, or a non-quest informational step.
 * - Simulates completion in sequence to produce a coherent list.
 *
 * Simulation details:
 * - Finished quests are detected via {@link QuestStatusService}.
 * - Non-quest steps are not verifiable; the planner assumes completion once shown.
 * - Quest XP rewards (from {@link QuestDatabase}) are applied to simulated levels so that
 *   later requirements can be satisfied by earlier quest rewards.
 */
@Singleton
public class ProgressionPlanService
{
    /**
     * When a user is behind the spine's expected skill levels, avoid inserting a single massive training wall.
     * Instead, emit catch-up training in small chunks so the plan stays readable and motivational.
     */
    private static final int CATCH_UP_MAX_LEVEL_DELTA_PER_STEP = 5;

    private static final Pattern LAMP_ON_PATTERN = Pattern.compile("(?i)\\blamp\\s+on\\s+([a-z ]+)\\b");

    private final Client client;
    private final QuestStatusService statusService;
    private final QuestDatabase questDatabase;
    private final StepCompletionService completionService;


    @Inject
    public ProgressionPlanService(Client client, QuestStatusService statusService, QuestDatabase questDatabase, StepCompletionService completionService)
    {
        this.client = client;
        this.statusService = statusService;
        this.questDatabase = questDatabase;
        this.completionService = completionService;
    }

    
    public List<PlanStep> buildNextSteps(List<RouteStep> spine, int maxSteps)
    {
        final List<PlanStep> steps = new ArrayList<>();
        if (spine == null || spine.isEmpty() || maxSteps <= 0)
        {
            return steps;
        }

        for (int i = 0; i < spine.size() && steps.size() < maxSteps; i++)
        {
            final RouteStep step = spine.get(i);
            if (step == null)
            {
                continue;
            }

            if (shouldSkip(step))
            {
                continue;
            }

            final PlanStepType type = step.getType();

            // Quests are authoritative via the quest journal.
            if (type == PlanStepType.QUEST && step.getQuest() != null)
            {
                final Quest q = step.getQuest();
                final QuestState state = statusService.getState(new QuestEntry(q, mergedWhy(step)));
                if (state == QuestState.FINISHED)
                {
                    continue;
                }

                final QuestEntry qe = new QuestEntry(q, mergedWhy(step));
                steps.add(new QuestPlanStep(qe, state));
                continue;
            }

            // Non-quest steps: use explicit completion checks where available.
            if (completionService.isComplete(step))
            {
                continue;
            }

            if (type == PlanStepType.TRAIN && step.getSkill() != null && step.getToLevel() != null)
            {
                final Skill skill = step.getSkill();
                final int have = safeRealLevel(skill);
                final int target = step.getToLevel();
                if (have >= target)
                {
                    continue;
                }

                steps.add(new TrainPlanStep(skill, have, target, mergedWhy(step)));
                continue;
            }

            // Generic informational step.
            final String why = mergedWhy(step);
            steps.add(InfoPlanStep.builder(type)
                .title(step.getDisplayName())
                .detail(why)
                .build());

            // If the step text includes lamp instructions, surface them explicitly.
            final String lamp = extractLampInstruction(why);
            if (lamp != null)
            {
                steps.add(InfoPlanStep.builder(PlanStepType.LAMP)
                    .title("Use lamp")
                    .detail(lamp)
                    .build());
            }
        }

        return steps;
    }


    private static String safeTitle(RouteStep s)
    {
        if (s.getDisplayName() != null && !s.getDisplayName().trim().isEmpty())
        {
            return s.getDisplayName().trim();
        }
        if (s.getType() == PlanStepType.QUEST && s.getQuest() != null)
        {
            return s.getQuest().getName();
        }
        if (s.getType() == PlanStepType.TRAIN && s.getSkill() != null && s.getToLevel() != null)
        {
            return "Train " + s.getSkill().getName() + " to " + s.getToLevel();
        }
        return s.getType().name();
    }

    private int safeRealLevel(Skill skill)
    {
        if (skill == null)
        {
            return 1;
        }

        try
        {
            return Math.max(1, client.getRealSkillLevel(skill));
        }
        catch (Exception e)
        {
            return 1;
        }
    }


    private RouteStep nextUnfinished(List<RouteStep> spine, int startIdx,
                                     Set<Quest> simulatedFinished,
                                     Map<Skill, Integer> simulatedLevels)
    {
        for (int i = startIdx; i < spine.size(); i++)
        {
            RouteStep s = spine.get(i);
            if (s == null)
            {
                continue;
            }

            if (s.getType() == PlanStepType.QUEST && s.getQuest() != null)
            {
                if (!simulatedFinished.contains(s.getQuest()))
                {
                    return s;
                }
            }
            else if (s.getType() == PlanStepType.TRAIN && s.getSkill() != null && s.getToLevel() != null)
            {
                int have = simulatedLevels.getOrDefault(s.getSkill(), 1);
                if (have < s.getToLevel())
                {
                    return s;
                }
                // else already done
            }
            else
            {
                // Non-quest steps are not verifiable, but a small subset can be inferred from account state.
                // If inferred complete, skip it so the quest spine remains clean.
                if (completionService.isComplete(s))
                {
                    continue;
                }

                // Otherwise treat as unfinished until shown.
                return s;
            }
        }
        return null;
    }

    private static int indexOf(List<RouteStep> spine, RouteStep step)
    {
        // RouteStep is immutable and small; linear search is OK at plugin scale.
        for (int i = 0; i < spine.size(); i++)
        {
            if (spine.get(i) == step)
            {
                return i;
            }
        }
        return 0;
    }

    private QuestState stateFor(Quest q, Set<Quest> simulatedFinished)
    {
        return simulatedFinished.contains(q) ? QuestState.FINISHED : QuestState.NOT_STARTED;
    }

    private void applyQuestXp(Quest q, Map<Skill, Integer> simulatedLevels)
    {
        Map<Skill, Integer> xp = questDatabase.getXpRewards(q);
        if (xp == null || xp.isEmpty())
        {
            return;
        }

        // MVP: approximate XP -> level by assuming reward is enough to reach at least the requirement gate
        // if the level delta is small. Exact XP tables can be added later.
        for (Map.Entry<Skill, Integer> en : xp.entrySet())
        {
            Skill s = en.getKey();
            int addXp = en.getValue() == null ? 0 : en.getValue();
            if (s == null || addXp <= 0)
            {
                continue;
            }

            int curLevel = simulatedLevels.getOrDefault(s, 1);
            int baseXp = com.ironpath.util.XpTable.xpForLevel(curLevel);
            int bumped = com.ironpath.util.XpTable.levelForXp(baseXp + addXp);
            if (bumped > curLevel)
            {
                simulatedLevels.put(s, bumped);
            }
        }
    }

    private Map<Skill, Integer> mergedMinSkills(RouteStep step)
    {
        Map<Skill, Integer> base = questDatabase.getMinSkills(step.getQuest());
        if (step.getMinSkillsOverride() == null || step.getMinSkillsOverride().isEmpty())
        {
            return base == null ? Map.of() : base;
        }

        Map<Skill, Integer> out = new EnumMap<>(Skill.class);
        if (base != null)
        {
            out.putAll(base);
        }
        out.putAll(step.getMinSkillsOverride());
        return out;
    }

    private String mergedWhy(RouteStep step)
    {
        final Quest q = step == null ? null : step.getQuest();
        String base = null;

        if (q != null)
        {
            base = questDatabase.getWhy(q);
        }

        final String candidate = base != null ? base : (step == null ? null : step.getWhy());
        if (candidate != null && !candidate.trim().isEmpty())
        {
            return candidate.trim();
        }

        if (step == null || step.getType() == null)
        {
            return null;
        }

        // Fallback copy when the quest DB does not have an entry yet.
        switch (step.getType())
        {
            case QUEST:
                return "Complete this quest in the OSRS Wiki Optimal Quest Guide order to minimise extra training.";
            case MINIQUEST:
                return "Recommended miniquest in the OSRS Wiki Optimal Quest Guide.";
            case UNLOCK:
                return "Recommended unlock in the OSRS Wiki Optimal Quest Guide.";
            case DIARY:
                return "Recommended diary step in the OSRS Wiki Optimal Quest Guide.";
            case TRAIN:
                return "Recommended training checkpoint in the OSRS Wiki Optimal Quest Guide.";
            default:
                return "Follow the OSRS Wiki Optimal Quest Guide order.";
        }
    }

    private static List<TrainPlanStep> missingSkills(RouteStep target, Map<Skill, Integer> req, Map<Skill, Integer> simulatedLevels, int maxDeltaPerStep)
    {
        final List<TrainPlanStep> out = new ArrayList<>();
        if (req == null || req.isEmpty())
        {
            return out;
        }

        String title = target.getDisplayName();
        if (title == null || title.isEmpty())
        {
            title = target.getQuest() != null ? target.getQuest().getName() : "next quest";
        }

        for (Map.Entry<Skill, Integer> en : req.entrySet())
        {
            final Skill s = en.getKey();
            final int need = en.getValue() == null ? 1 : en.getValue();
            final int have = simulatedLevels.getOrDefault(s, 1);
            if (have < need)
            {
                final int to = maxDeltaPerStep > 0 ? Math.min(need, have + maxDeltaPerStep) : need;
                out.add(new TrainPlanStep(s, have, to, "Required for " + title + "."));
            }
        }

        // Stable ordering: biggest deficit first, then skill name.
        out.sort((a, b) ->
        {
            int da = a.getToLevel() - a.getFromLevel();
            int db = b.getToLevel() - b.getFromLevel();
            if (da != db)
            {
                return Integer.compare(db, da);
            }
            return a.getSkill().name().compareToIgnoreCase(b.getSkill().name());
        });

        return out;
    }


    private static String extractLampInstruction(String why)
    {
        if (why == null || why.isBlank())
        {
            return null;
        }

        final Matcher m = LAMP_ON_PATTERN.matcher(why);
        if (!m.find())
        {
            return null;
    }

        return why.substring(m.start()).trim();
    }

    private boolean shouldSkip(RouteStep step)
    {
        if (step == null || step.getType() == null)
        {
            return true;
        }

        switch (step.getType())
        {
            case QUEST:
            case MINIQUEST:
                return step.getQuest() == null;

            case TRAIN:
                return step.getSkill() == null || step.getToLevel() == null;

            case DIARY:
            case NOTE:
                return step.getDisplayName() == null || step.getDisplayName().trim().isEmpty();

            default:
                return false;
        }
    }
}
