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

import jakarta.annotation.Nullable;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.MissionLocationProfile;
import mekhq.campaign.universe.RandomFactionGenerator;

public class ContractDeterminationLocation {
    private ContractDeterminationLocation() {}

    public static @Nullable String determineContractLocation(AtBContractType objectiveType, boolean isPlayerAttacker,
          String employerFactionCode, String enemyFactionCode, ILocation currentLocation) {
        MissionLocationProfile missionLocationProfile = MissionLocationProfile.fromContractType(objectiveType);

        RandomFactionGenerator randomFactionGenerator = RandomFactionGenerator.getInstance();
        String attackerFactionCode = isPlayerAttacker ? employerFactionCode : enemyFactionCode;
        String defenderFactionCode = !isPlayerAttacker ? employerFactionCode : enemyFactionCode;

        return randomFactionGenerator.getMissionTarget(attackerFactionCode, defenderFactionCode,
              currentLocation, missionLocationProfile);
    }
}
