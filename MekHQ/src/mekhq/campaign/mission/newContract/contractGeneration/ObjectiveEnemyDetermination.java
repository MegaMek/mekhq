package mekhq.campaign.mission.newContract.contractGeneration;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.REBEL_FACTION_CODE;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.EnemySelectionProfile;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;

public class ObjectiveEnemyDetermination {
    private static final MMLogger LOGGER = MMLogger.create(ObjectiveEnemyDetermination.class);

    private static final int MERCENARY_ENEMY_CHANCE = 20;

    static Faction generateEnemyFactionForObjective(AbstractLocation currentLocation, LocalDate currentDate,
          Faction employerFaction, AtBContractType objectiveType) {
        RandomFactionGenerator randomFactionGenerator = RandomFactionGenerator.getInstance();
        EnemySelectionProfile enemySelectionProfile = EnemySelectionProfile.fromContractType(objectiveType);
        Faction enemyFaction = randomFactionGenerator.getRandomEnemy(currentLocation, currentDate, employerFaction,
              enemySelectionProfile);

        if (enemyFaction == null) {
            LOGGER.warn("Failed to fetch random enemy faction for employer code {}. Returning Rebels.",
                  employerFaction.getShortName());
            return Factions.getInstance().getFaction(REBEL_FACTION_CODE);
        }

        return enemyFaction;
    }

    private static @Nullable Faction hasEmployedMercenaries(Faction employerFaction, LocalDate currentDate) {
        boolean allowsMercenaries = employerFaction.isUsesMercenaries(currentDate.getYear());
        if (allowsMercenaries) {
            int roll = randomInt(MERCENARY_ENEMY_CHANCE);
            if (roll == 0) {
                return Factions.getInstance().getFaction(MERCENARY_FACTION_CODE);
            }
        }

        return null;
    }
}
