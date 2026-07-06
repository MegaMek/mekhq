package mekhq.campaign.mission.newContract.contractGeneration;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.universe.Faction.BANDIT_CASTE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.REBEL_FACTION_CODE;

import java.time.LocalDate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;

public class ObjectiveEnemyDetermination {
    private static final MMLogger LOGGER = MMLogger.create(ObjectiveEnemyDetermination.class);

    private static final int MERCENARY_ENEMY_CHANCE = 20;

    static @Nullable Faction generateEnemyFactionForObjective(Faction employerFaction,
          AtBContractType objectiveType, LocalDate currentDate) {
        Faction specialFaction = getSpecialFaction(employerFaction, objectiveType, currentDate);
        if (specialFaction != null) {
            return specialFaction;
        }

        RandomFactionGenerator randomFactionGenerator = RandomFactionGenerator.getInstance();
        // TODO have this return a Faction object
        String enemyCode = randomFactionGenerator.getEnemy(employerFaction, false, false);
        Faction enemyFaction = Factions.getInstance().getFaction(enemyCode);

        if (objectiveType.isPirateHunting()) {
            String pirateFactionCode = enemyFaction.isClan() ? BANDIT_CASTE_FACTION_CODE : PIRATE_FACTION_CODE;
            return Factions.getInstance().getFaction(pirateFactionCode);
        }

        return enemyFaction;
    }

    private static Faction getSpecialFaction(Faction employerFaction, AtBContractType objectiveType,
          LocalDate currentDate) {
        if (objectiveType.isRiotDuty()) {
            return Factions.getInstance().getFaction(REBEL_FACTION_CODE);
        }

        boolean allowsMercenaries = employerFaction.isUsesMercenaries(currentDate.getYear());
        if (allowsMercenaries) {
            int roll = randomInt(MERCENARY_ENEMY_CHANCE);
            if (roll == 0) {
                return Factions.getInstance().getFaction(MERCENARY_FACTION_CODE);
            }
        }

        return null;
    }

    static Faction getEnemyFaction(CampaignTypeForContractDetermination campaignType, AtBContractType objective,
          @Nonnull Faction employerFaction, LocalDate currentDate) {
        Faction enemyFaction;

        if (campaignType == CampaignTypeForContractDetermination.PIRATE) {
            // Under CamOps pirates don't generate employers they generate targets, so employer is actually the enemy,
            // CamOps pg 39 rev 5th edition
            enemyFaction = employerFaction;
        } else {
            enemyFaction = ObjectiveEnemyDetermination.generateEnemyFactionForObjective(employerFaction,
                  objective, currentDate);
        }

        if (enemyFaction == null) {
            LOGGER.warn("Failed to fetch random enemy faction for employer code {}. Returning Rebels.",
                  employerFaction.getShortName());
            return Factions.getInstance().getFaction(REBEL_FACTION_CODE);
        }

        return enemyFaction;
    }
}
