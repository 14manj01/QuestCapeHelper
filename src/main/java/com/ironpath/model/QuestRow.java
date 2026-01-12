package com.ironpath.model;

import java.util.Collections;
import java.util.List;
import net.runelite.api.QuestState;

public final class QuestRow
{
    private final QuestEntry entry;
    private final QuestState questState;
    private final QuestReadiness readiness;
    private final List<QuestBlocker> blockers;
    private final double score;
    private final int routeIndex;

    public QuestRow(QuestEntry entry, QuestState questState, QuestReadiness readiness,
                    List<QuestBlocker> blockers, double score, int routeIndex)
    {
        this.entry = entry;
        this.questState = questState;
        this.readiness = readiness;
        this.blockers = blockers == null ? List.of() : Collections.unmodifiableList(blockers);
        this.score = score;
        this.routeIndex = routeIndex;
    }

    public QuestEntry getEntry() { return entry; }
    public QuestState getQuestState() { return questState; }
    public QuestReadiness getReadiness() { return readiness; }
    public List<QuestBlocker> getBlockers() { return blockers; }
    public double getScore() { return score; }
    public int getRouteIndex() { return routeIndex; }
}
