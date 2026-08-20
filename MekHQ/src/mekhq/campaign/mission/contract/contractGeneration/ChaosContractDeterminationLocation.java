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

import jakarta.annotation.Nullable;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.targetFinder.MissionLocationProfile;
import mekhq.campaign.universe.RandomFactionGenerator;

/**
 * Determines the target star system for a generated contract by delegating to the random faction generator with the
 * attacker and defender resolved from the player's role. This is a static utility class and is not instantiable.
 */
public class ChaosContractDeterminationLocation {
    private ChaosContractDeterminationLocation() {}

    /**
     * Determines the contract's target system. The employer and enemy are assigned to attacker/defender roles based on
     * whether the player is attacking, then a mission target is drawn using the objective type's location profile.
     *
     * @param objectiveType       the contract's objective type, whose location profile guides target selection
     * @param isPlayerAttacker    whether the player's employer is the attacker (otherwise the defender)
     * @param employerFactionCode the employer faction's short code
     * @param enemyFactionCode    the enemy faction's short code
     * @param currentLocation     the campaign's current location
     *
     * @return the target system id, or {@code null} if no valid target could be found
     */
    public static @Nullable String determineContractLocation(ContractObjectiveType objectiveType,
          boolean isPlayerAttacker,
          String employerFactionCode, String enemyFactionCode, ILocation currentLocation) {
        MissionLocationProfile missionLocationProfile = objectiveType.getMissionLocationProfile();

        RandomFactionGenerator randomFactionGenerator = RandomFactionGenerator.getInstance();
        String attackerFactionCode = isPlayerAttacker ? employerFactionCode : enemyFactionCode;
        String defenderFactionCode = !isPlayerAttacker ? employerFactionCode : enemyFactionCode;

        return randomFactionGenerator.getMissionTarget(attackerFactionCode, defenderFactionCode,
              currentLocation, missionLocationProfile);
    }
}
