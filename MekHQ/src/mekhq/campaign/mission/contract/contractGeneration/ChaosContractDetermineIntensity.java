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

import static megamek.common.compute.Compute.d6;

public class ChaosContractDetermineIntensity {
    public static int determineTrackCount(ChaosObjectiveType objectiveType) {
        int roll = d6(2);

        return switch (objectiveType) {
            case EXPEDITION, PIRATE_HUNT, GUERILLA_OPERATION, RAID, PIRATE_RAID ->
                  determineRaidOrExpeditionTrackCount(roll);
            case GARRISON, CADRE_DUTY -> determineGarrisonOrRetainerTrackCount(roll);
            case INVASION -> determineInvasionTrackCount(roll);
        };
    }

    private static int determineInvasionTrackCount(int roll) {
        return switch (roll) {
            case 2, 5, 3, 4 -> 2;
            case 6, 7 -> 3;
            case 8, 9 -> 4;
            case 10, 11 -> 5;
            case 12 -> 6;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static int determineGarrisonOrRetainerTrackCount(int roll) {
        return switch (roll) {
            case 2, 3, 4 -> 0;
            case 5, 6 -> 1;
            case 7, 8 -> 2;
            case 9 -> 3;
            case 10, 11 -> 4;
            case 12 -> 5;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }

    private static int determineRaidOrExpeditionTrackCount(int roll) {
        return switch (roll) {
            case 2, 3, 4, 5, 6, 7, 8 -> 1;
            case 9, 10, 11 -> 2;
            case 12 -> 3;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }
}
