package com.ironpath.service;

import com.google.inject.Singleton;
import com.ironpath.model.PlanSnapshot;
import com.ironpath.model.QuestBlocker;
import com.ironpath.model.QuestEntry;
import com.ironpath.model.QuestReadiness;
import com.ironpath.model.QuestRow;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

@Singleton
public class QuestPlannerService
{
    private final Client client;
    private final QuestStatusService statusService;

    @Inject
    public QuestPlannerService(Client client, QuestStatusService statusService)
    {
        this.client = client;
        this.statusService = statusService;
    }

    public PlanSnapshot compute(List<QuestEntry> route)
    {
        final List<QuestRow> inProgress = new ArrayList<>();
        final List<QuestRow> ready = new ArrayList<>();
        final List<QuestRow> blocked = new ArrayList<>();
        final List<QuestRow> done = new ArrayList<>();

        int completed = 0;
        int i = 0;
        for (QuestEntry e : route)
        {
            final QuestState qs = statusService.getState(e);
            if (qs == QuestState.FINISHED)
            {
                completed++;
            }

            final List<QuestBlocker> blockersForQuest = computeBlockers(e);
            final QuestReadiness readiness = computeReadiness(qs, blockersForQuest);
            final double score = score(e, qs, readiness, blockersForQuest, i);
            final QuestRow row = new QuestRow(e, qs, readiness, blockersForQuest, score, i);

            switch (readiness)
            {
                case DONE:
                    done.add(row);
                    break;
                case IN_PROGRESS:
                    inProgress.add(row);
                    break;
                case READY:
                    ready.add(row);
                    break;
                case BLOCKED:
                default:
                    blocked.add(row);
                    break;
            }
            i++;
        }

        // Choose recommended quest: in-progress first, else best scored READY, else best scored BLOCKED.
        QuestRow recommended = null;
        if (!inProgress.isEmpty())
        {
            recommended = inProgress.stream().max(Comparator.comparingDouble(QuestRow::getScore)).orElse(null);
        }
        if (recommended == null)
        {
            recommended = ready.stream().max(Comparator.comparingDouble(QuestRow::getScore)).orElse(null);
        }
        if (recommended == null)
        {
            recommended = blocked.stream().max(Comparator.comparingDouble(QuestRow::getScore)).orElse(null);
        }

        // Sort each bucket for stable UX.
        final Comparator<QuestRow> byPriority = Comparator
                .comparingDouble(QuestRow::getScore).reversed()
                .thenComparingInt(QuestRow::getRouteIndex);

        inProgress.sort(byPriority);
        ready.sort(byPriority);
        blocked.sort(byPriority);
        done.sort(Comparator.comparingInt(QuestRow::getRouteIndex));

        return new PlanSnapshot(completed, route.size(), recommended, inProgress, ready, blocked, done);
    }

    private List<QuestBlocker> computeBlockers(QuestEntry entry)
    {
        final List<QuestBlocker> blockers = new ArrayList<>();

        // Prerequisite quests (tracked subset).
        for (String prereq : entry.getPrereqQuestNames())
        {
            if (prereq == null || prereq.isBlank())
            {
                continue;
            }
            final QuestState st = statusService.getStateByName(prereq);
            if (st != QuestState.FINISHED)
            {
                blockers.add(QuestBlocker.prereqQuest(prereq));
            }
        }

        // Skill requirements (tracked subset).
        for (var req : entry.getMinSkills().entrySet())
        {
            final Skill s = req.getKey();
            final int need = req.getValue() == null ? 0 : req.getValue();
            if (need <= 0)
            {
                continue;
            }
            final int have = client.getRealSkillLevel(s);
            if (have < need)
            {
                blockers.add(QuestBlocker.skill(s, have, need));
            }
        }

        return blockers;
    }

    private static QuestReadiness computeReadiness(QuestState questState, List<QuestBlocker> blockers)
    {
        if (questState == QuestState.FINISHED)
        {
            return QuestReadiness.DONE;
        }
        if (questState == QuestState.IN_PROGRESS)
        {
            return QuestReadiness.IN_PROGRESS;
        }
        if (blockers == null || blockers.isEmpty())
        {
            return QuestReadiness.READY;
        }
        return QuestReadiness.BLOCKED;
    }

    private static double score(QuestEntry e, QuestState qs, QuestReadiness readiness, List<QuestBlocker> blockers, int routeIndex)
    {
        // Base bias: earlier in the route is generally better for the MVP.
        double score = 10000 - routeIndex;

        // State bias.
        if (qs == QuestState.IN_PROGRESS)
        {
            score += 1000;
        }

        // Readiness bias.
        switch (readiness)
        {
            case READY:
                score += 500;
                break;
            case BLOCKED:
                score -= 250;
                break;
            case DONE:
                score -= 10_000;
                break;
            case IN_PROGRESS:
                score += 750;
                break;
            default:
                break;
        }

        // Penalty for missing levels, scaled by deficit.
        int deficit = 0;
        if (blockers != null)
        {
            for (QuestBlocker b : blockers)
            {
                if (b.getType() == QuestBlocker.Type.SKILL_LEVEL)
                {
                    deficit += Math.max(0, b.getNeed() - b.getHave());
                }
                else
                {
                    deficit += 3; // rough cost per missing prerequisite
                }
            }
        }
        score -= deficit * 40;

        // Tags bias.
        if (e.getTags().contains("combatxp")) score += 200;
        if (e.getTags().contains("unlock")) score += 200;
        if (e.getTags().contains("capstone")) score -= 150;
        if (e.getTags().contains("grandmaster")) score -= 100;
        if (e.getTags().contains("early")) score += 100;

        return score;
    }
}
