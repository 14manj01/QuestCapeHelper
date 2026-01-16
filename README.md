# Optimal Quest Order 

The goal of the Optimal Quest Order is to minimize unnecessary manual training on the way to a Quest Cape and ultimately level 126, by completing quests at points where their experience rewards naturally satisfy future requirements. Rather than grinding skills early, the guide sequences quests so that experience from quest rewards does as much of the work as possible.

This plugin does not attempt to optimize, reorder, or “solve” progression on its own. Instead, it faithfully walks a recommended order (near exactly the wiki's) step-by-step, showing you exactly what to do next and why it matters in the broader progression.

Each step includes concise context explaining:

What the quest or miniquest unlocks

Why it appears at that point in the route

How it contributes to long-term progression

For quest and miniquest steps, the plugin provides a Quest Guide button that links directly to the relevant OSRS Wiki article. This allows you to open the full walkthrough instantly without leaving the game or searching manually. These links are stored per-step in the route data, ensuring accuracy and allowing future refinement or customization.

By combining a strict quest order, minimal assumptions, and direct access to official wiki guidance, the plugin acts as a reliable companion for players who want a clean, efficient, and transparent path through Old School RuneScape’s quest progression.
This plugin is not a solver and not an optimizer. It does not reorder steps, infer training, or generate new requirements. It walks the guide in order and shows you the next unfinished steps.

---

## What it does

- Loads a curated route (`wiki_route.json`) that represents the Optimal Quest progression
- Walks the route strictly in order, step-by-step
- Displays the next N unfinished steps in the sidebar
- Shows a small in-game overlay for the current active step
- Adds a **Quest Guide** button on steps to open the OSRS Wiki page for that quest/miniquest

---

## Strict behavior rules

The route JSON is the authoritative script. Order is preserved.

The planner only skips a step if it can be provably complete:

- QUEST steps: skipped only if the quest is completed in the quest journal
- TRAIN steps: skipped only if the player’s real skill level meets or exceeds the target
- DIARY steps: skipped only if the diary tier varbit is complete
- Tutorial Island: skipped automatically

The planner may apply quest XP internally only to avoid showing already-satisfied TRAIN targets.

The planner must NOT:

- Insert “catch-up” training
- Infer requirements from `quest_db.json`
- Jump ahead to future quests
- Reorder the spine
- Generate steps that are not explicitly in the route JSON

---

## Data files

### `wiki_route.json` (authoritative)
Full progression spine, in order. Includes quests, miniquests, training blocks, diaries, unlocks, and notes.

Each entry may include:
- `displayName`
- `type` (QUEST, MINIQUEST, TRAIN, NOTE, DIARY, etc.)
- `why` (short subtext shown on the card)
- `wikiUrl` (optional, opens when clicking Quest Guide)

### `quest_db.json` (metadata overlay only)
Used as a sparse overlay for:
- XP rewards
- hard gating requirements
- tags

It is not authoritative for pacing, ordering, or training.

---

## UI

### Sidebar panel
- Header shows:
  - **Optimal Quest Order**
  - “Order is optimized following OSRS Wiki.”
  - Refresh button
- Section: **Next 10 steps**
  - Cards show:
    - step number in the spine (X / Y)
    - title and why text
    - **Quest Guide** button (opens wiki)

### Active step overlay
- Displays the current active step on the game screen
- Shows compact spine progress (X / Y)
- Uses the same title/body resolution as the sidebar cards

---

## Quest Guide links

Steps can store a `wikiUrl` in `wiki_route.json`.

- If present, clicking **Quest Guide** opens that exact URL.
- If missing, the plugin falls back to generating a wiki URL from the step name.

Recommended: keep explicit `wikiUrl` values for full control and easy edits.

---

## Plugin Hub compliance

- Java, RuneLite APIs only
- No subprocesses, no native binaries, no reflection/JNA
- External links open via RuneLite-safe browser utility
- No user-configurable URLs
- No scraping or network calls to the wiki

---

## Development notes

- `ProgressionPlanService` is responsible for walking the spine and emitting the next N unfinished steps.
- `PlanStepCard`, `QuestCard`, and `MiniquestCard` handle rendering consistently.
- Refresh behavior is debounced to prevent UI thrash during login and varbit bursts.

---

## Credits

Author: Myip of TDD  
Route basis: OSRS Wiki Optimal Quest Guide (encoded into `wiki_route.json`)

---

## Disclaimer

Old School RuneScape and RuneLite are trademarks of their respective owners. This is a community plugin and is not affiliated with Jagex or RuneLite.
