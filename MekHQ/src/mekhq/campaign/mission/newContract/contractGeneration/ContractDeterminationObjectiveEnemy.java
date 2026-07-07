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

public class ContractDeterminationObjectiveEnemy {
    private static final MMLogger LOGGER = MMLogger.create(ContractDeterminationObjectiveEnemy.class);

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
