package com.ironpath.model;

import java.util.Collections;
import java.util.List;

public final class PlanSnapshot
{
    private final int completed;
    private final int total;
    private final QuestRow recommended;
    private final List<QuestRow> inProgress;
    private final List<QuestRow> ready;
    private final List<QuestRow> blocked;
    private final List<QuestRow> done;

    public PlanSnapshot(int completed, int total, QuestRow recommended,
                        List<QuestRow> inProgress, List<QuestRow> ready, List<QuestRow> blocked, List<QuestRow> done)
    {
        this.completed = completed;
        this.total = total;
        this.recommended = recommended;
        this.inProgress = inProgress == null ? List.of() : Collections.unmodifiableList(inProgress);
        this.ready = ready == null ? List.of() : Collections.unmodifiableList(ready);
        this.blocked = blocked == null ? List.of() : Collections.unmodifiableList(blocked);
        this.done = done == null ? List.of() : Collections.unmodifiableList(done);
    }

    public int getCompleted() { return completed; }
    public int getTotal() { return total; }
    public QuestRow getRecommended() { return recommended; }
    public List<QuestRow> getInProgress() { return inProgress; }
    public List<QuestRow> getReady() { return ready; }
    public List<QuestRow> getBlocked() { return blocked; }
    public List<QuestRow> getDone() { return done; }
}
