/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.mission.contract.contractGeneration;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.mission.utilities.RandomFactionCamouflage.pickRandomCamouflage;
import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomMercenaryCompanyNameGenerator;
import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomPirateCompanyNameGenerator;
import static mekhq.campaign.personnel.backgrounds.BackgroundsController.randomRebelCompanyNameGenerator;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.Faction.REBEL_FACTION_CODE;

import java.time.LocalDate;

import megamek.common.icons.Camouflage;
import mekhq.campaign.Campaign;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractData.EnemyData;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.OpposingCommander;
import mekhq.campaign.mission.contract.contractGeneration.targetFinder.EnemySelectionProfile;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.RandomFactionGenerator;

/**
 * Determines the enemy faction for a generated contract objective, drawing a random enemy appropriate to the objective
 * type and falling back to the Rebels faction when none can be found. This is a static utility class and is not
 * instantiable.
 */
public class ChaosContractDeterminationEnemy {
    private static final int MERCENARY_ENEMY_CHANCE = 10;

    private ChaosContractDeterminationEnemy() {}

    public static EnemyData generateEnemyFactionForObjective(Campaign campaign, ILocation currentLocation,
          LocalDate currentDate, Faction employerFaction, ContractObjectiveType objectiveType) {
        RandomFactionGenerator randomFactionGenerator = RandomFactionGenerator.getInstance();

        EnemySelectionProfile enemySelectionProfile = objectiveType.getEnemySelectionProfile();
        Faction enemyFaction = randomFactionGenerator.getRandomEnemy(currentLocation, currentDate, employerFaction,
              enemySelectionProfile);

        return generateEnemyForFaction(campaign, enemyFaction, currentDate);
    }

    /**
     * Wraps a pre-determined enemy faction into {@link EnemyData}, applying the same mercenary-sponsor substitution and
     * camouflage/display resolution as {@link #generateEnemyFactionForObjective}. Used when the enemy is fixed by
     * context rather than drawn from the objective's enemy pool &mdash; for example a rebellion, where the enemy is the
     * ruling power the rebels are fighting rather than a randomly selected belligerent.
     *
     * @param enemyFaction the faction that will oppose the player
     * @param currentDate  the date used for mercenary eligibility, display name, and camouflage
     *
     * @return the enemy data for the given faction
     */
    static EnemyData generateEnemyForFaction(Campaign campaign, Faction enemyFaction, LocalDate currentDate) {
        String factionCode = enemyFaction.getShortName();

        String sponsorFactionCode = null;
        boolean hasEmployedMercenaries = hasEmployedMercenaries(enemyFaction, currentDate);
        if (hasEmployedMercenaries) {
            sponsorFactionCode = factionCode;
            factionCode = MERCENARY_FACTION_CODE;
        }

        int currentYear = currentDate.getYear();
        String generatedName = switch (factionCode) {
            case MERCENARY_FACTION_CODE -> randomMercenaryCompanyNameGenerator(null);
            case PIRATE_FACTION_CODE -> randomPirateCompanyNameGenerator();
            case REBEL_FACTION_CODE -> randomRebelCompanyNameGenerator(null);
            default -> enemyFaction.getFullName(currentYear);
        };
        String displayName = (generatedName != null) ? generatedName : enemyFaction.getFullName(currentYear);

        Camouflage camouflage = pickRandomCamouflage(currentYear, factionCode);
        Person opposingCommander = OpposingCommander.generateOpposingCommander(campaign, enemyFaction);

        return new EnemyData(factionCode, sponsorFactionCode, displayName, opposingCommander, camouflage);
    }

    private static boolean hasEmployedMercenaries(Faction employerFaction, LocalDate currentDate) {
        boolean allowsMercenaries = employerFaction.isUsesMercenaries(currentDate.getYear());
        if (allowsMercenaries) {
            int roll = randomInt(MERCENARY_ENEMY_CHANCE);
            return roll == 0;
        }

        return false;
    }
}
