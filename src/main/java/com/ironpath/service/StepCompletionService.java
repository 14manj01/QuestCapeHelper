package com.ironpath.service;

import com.google.inject.Singleton;
import com.ironpath.model.PlanStepType;
import com.ironpath.model.RouteStep;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Varbits;

/**
 * Centralized completion checks for non-quest progression steps.
 *
 * Plugin Hub compliance: uses only in-client varps/varbits (no networking, no disk).
 */
@Singleton
public class StepCompletionService
{
    private final Client client;

    @Inject
    public StepCompletionService(Client client)
    {
        this.client = client;
    }

    public boolean isComplete(RouteStep step)
    {
        if (step == null)
        {
            return false;
        }

        final PlanStepType type = step.getType();
        final String name = step.getDisplayName();
        final String normalized = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);

        // Tutorial Island is represented as a NOTE/UNLOCK in some spines.
        if (normalized.contains("tutorial island"))
        {
            return isTutorialIslandComplete();
        }

        if (type == PlanStepType.DIARY)
        {
            return isAchievementDiaryTierComplete(normalized);
        }

        // Optional: combat achievements tier completion (if you add steps for it).
        if (normalized.contains("combat achievements") || normalized.contains("combat achievement"))
        {
            return isCombatAchievementTierComplete(normalized);
        }

        return false;
    }

    private boolean isTutorialIslandComplete()
    {
        // Tutorial progress varp is 281 and completion is 1000.
        final int tutorialProgress = client.getVarpValue(281);
        return tutorialProgress >= 1000;
    }

    private boolean isAchievementDiaryTierComplete(String normalizedDisplayName)
    {
        final Tier tier = Tier.parse(normalizedDisplayName);
        final Region region = Region.parse(normalizedDisplayName);
        if (tier == null || region == null)
        {
            return false;
        }

        final int varbitId = diaryVarbitId(region, tier);
        if (varbitId <= 0)
        {
            return false;
        }

        // Diary tier completion varbits are typically 1 when rewards are claimed.
        return client.getVarbitValue(varbitId) >= 1;
    }

    private static int diaryVarbitId(Region region, Tier tier)
    {
        switch (region)
        {
            case ARDOUGNE:
                return tier.pick(Varbits.DIARY_ARDOUGNE_EASY, Varbits.DIARY_ARDOUGNE_MEDIUM, Varbits.DIARY_ARDOUGNE_HARD, Varbits.DIARY_ARDOUGNE_ELITE);
            case DESERT:
                return tier.pick(Varbits.DIARY_DESERT_EASY, Varbits.DIARY_DESERT_MEDIUM, Varbits.DIARY_DESERT_HARD, Varbits.DIARY_DESERT_ELITE);
            case FALADOR:
                return tier.pick(Varbits.DIARY_FALADOR_EASY, Varbits.DIARY_FALADOR_MEDIUM, Varbits.DIARY_FALADOR_HARD, Varbits.DIARY_FALADOR_ELITE);
            case FREMENNIK:
                return tier.pick(Varbits.DIARY_FREMENNIK_EASY, Varbits.DIARY_FREMENNIK_MEDIUM, Varbits.DIARY_FREMENNIK_HARD, Varbits.DIARY_FREMENNIK_ELITE);
            case KANDARIN:
                return tier.pick(Varbits.DIARY_KANDARIN_EASY, Varbits.DIARY_KANDARIN_MEDIUM, Varbits.DIARY_KANDARIN_HARD, Varbits.DIARY_KANDARIN_ELITE);
            case KARAMJA:
                return tier.pick(Varbits.DIARY_KARAMJA_EASY, Varbits.DIARY_KARAMJA_MEDIUM, Varbits.DIARY_KARAMJA_HARD, Varbits.DIARY_KARAMJA_ELITE);
            case KOUREND_KEBOS:
                return tier.pick(Varbits.DIARY_KOUREND_EASY, Varbits.DIARY_KOUREND_MEDIUM, Varbits.DIARY_KOUREND_HARD, Varbits.DIARY_KOUREND_ELITE);
            case LUMBRIDGE_DRAYNOR:
                return tier.pick(Varbits.DIARY_LUMBRIDGE_EASY, Varbits.DIARY_LUMBRIDGE_MEDIUM, Varbits.DIARY_LUMBRIDGE_HARD, Varbits.DIARY_LUMBRIDGE_ELITE);
            case MORYTANIA:
                return tier.pick(Varbits.DIARY_MORYTANIA_EASY, Varbits.DIARY_MORYTANIA_MEDIUM, Varbits.DIARY_MORYTANIA_HARD, Varbits.DIARY_MORYTANIA_ELITE);
            case VARROCK:
                return tier.pick(Varbits.DIARY_VARROCK_EASY, Varbits.DIARY_VARROCK_MEDIUM, Varbits.DIARY_VARROCK_HARD, Varbits.DIARY_VARROCK_ELITE);
            case WESTERN:
                return tier.pick(Varbits.DIARY_WESTERN_EASY, Varbits.DIARY_WESTERN_MEDIUM, Varbits.DIARY_WESTERN_HARD, Varbits.DIARY_WESTERN_ELITE);
            case WILDERNESS:
                return tier.pick(Varbits.DIARY_WILDERNESS_EASY, Varbits.DIARY_WILDERNESS_MEDIUM, Varbits.DIARY_WILDERNESS_HARD, Varbits.DIARY_WILDERNESS_ELITE);
            default:
                return -1;
        }
    }

    private boolean isCombatAchievementTierComplete(String normalizedDisplayName)
    {
        final Tier tier = Tier.parse(normalizedDisplayName);
        if (tier == null)
        {
            return false;
        }

        final int varbitId;
        switch (tier)
        {
            case EASY:
                varbitId = Varbits.COMBAT_ACHIEVEMENT_TIER_EASY;
                break;
            case MEDIUM:
                varbitId = Varbits.COMBAT_ACHIEVEMENT_TIER_MEDIUM;
                break;
            case HARD:
                varbitId = Varbits.COMBAT_ACHIEVEMENT_TIER_HARD;
                break;
            case ELITE:
                varbitId = Varbits.COMBAT_ACHIEVEMENT_TIER_ELITE;
                break;
            case MASTER:
                varbitId = Varbits.COMBAT_ACHIEVEMENT_TIER_MASTER;
                break;
            case GRANDMASTER:
                varbitId = Varbits.COMBAT_ACHIEVEMENT_TIER_GRANDMASTER;
                break;
            default:
                return false;
        }

        return client.getVarbitValue(varbitId) >= 1;
    }

    private enum Tier
    {
        EASY, MEDIUM, HARD, ELITE, MASTER, GRANDMASTER;

        static Tier parse(String normalized)
        {
            if (normalized == null)
            {
                return null;
            }
            if (normalized.contains("grandmaster"))
            {
                return GRANDMASTER;
            }
            if (normalized.contains("master"))
            {
                return MASTER;
            }
            if (normalized.contains("elite"))
            {
                return ELITE;
            }
            if (normalized.contains("hard"))
            {
                return HARD;
            }
            if (normalized.contains("medium"))
            {
                return MEDIUM;
            }
            if (normalized.contains("easy"))
            {
                return EASY;
            }
            return null;
        }

        int pick(int easy, int medium, int hard, int elite)
        {
            switch (this)
            {
                case EASY:
                    return easy;
                case MEDIUM:
                    return medium;
                case HARD:
                    return hard;
                case ELITE:
                    return elite;
                default:
                    return -1;
            }
        }
    }

    private enum Region
    {
        ARDOUGNE,
        DESERT,
        FALADOR,
        FREMENNIK,
        KANDARIN,
        KARAMJA,
        KOUREND_KEBOS,
        LUMBRIDGE_DRAYNOR,
        MORYTANIA,
        VARROCK,
        WESTERN,
        WILDERNESS;

        static Region parse(String normalized)
        {
            if (normalized == null)
            {
                return null;
            }

            if (normalized.contains("ardougne"))
            {
                return ARDOUGNE;
            }
            if (normalized.contains("desert"))
            {
                return DESERT;
            }
            if (normalized.contains("falador"))
            {
                return FALADOR;
            }
            if (normalized.contains("fremennik"))
            {
                return FREMENNIK;
            }
            if (normalized.contains("kandarin"))
            {
                return KANDARIN;
            }
            if (normalized.contains("karamja"))
            {
                return KARAMJA;
            }
            if (normalized.contains("kourend") || normalized.contains("kebos"))
            {
                return KOUREND_KEBOS;
            }
            if (normalized.contains("lumbridge") || normalized.contains("draynor"))
            {
                return LUMBRIDGE_DRAYNOR;
            }
            if (normalized.contains("morytania"))
            {
                return MORYTANIA;
            }
            if (normalized.contains("varrock"))
            {
                return VARROCK;
            }
            if (normalized.contains("western"))
            {
                return WESTERN;
            }
            if (normalized.contains("wilderness"))
            {
                return WILDERNESS;
            }

            return null;
        }
    }
}
