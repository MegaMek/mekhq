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

package mekhq.campaign.personnel.skills;

import java.util.List;

import megamek.common.compute.Compute;

/**
 * Represents the outcome of an action check using d6.
 *
 * <p>Standard checks roll 2d6. Checks made with advantage or disadvantage roll 3d6, keeping the highest or lowest two
 * results, respectively.</p>
 *
 * @param result         The final computed value of the roll (falls between 2 and 12, inclusive).
 * @param individualDice An unmodifiable list of the individual dice rolled. This will contain more than two elements if
 *                       the check was performed with advantage or disadvantage.
 *
 * @author Hokk
 * @since 0.51.01
 */
public record ActionCheckRoll(
      int result,
      List<Integer> individualDice
) {

    public enum RollType {
        NORMAL,
        ADVANTAGE,
        DISADVANTAGE
    }

    /**
     * Executes an action check roll based on the provided roll type. Rolls 2 to 3 d6 dice and chooses 2 of them to
     * calculate the final result.
     *
     * @param rollType Determines how the dice are rolled and evaluated.
     *                 If {@link RollType#ADVANTAGE}, rolls 3d6 and keeps the highest 2.
     *                 If {@link RollType#DISADVANTAGE}, rolls 3d6 and keeps the lowest 2.
     *                 If {@link RollType#NORMAL}, rolls 2d6 and keeps both.
     *
     * @return An {@link ActionCheckRoll} detailing the final result and the individual dice rolled.
     */
    public static ActionCheckRoll perform(RollType rollType) {
        return switch (rollType) {
            case NORMAL -> {
                int d1 = Compute.d6();
                int d2 = Compute.d6();
                int result = d1 + d2;
                yield new ActionCheckRoll(result, List.of(d1, d2));
            }
            case ADVANTAGE -> {
                int d1 = Compute.d6();
                int d2 = Compute.d6();
                int d3 = Compute.d6();
                int min = Math.min(d1, Math.min(d2, d3));
                int result = (d1 + d2 + d3) - min;
                yield new ActionCheckRoll(result, List.of(d1, d2, d3));
            }
            case DISADVANTAGE -> {
                int d1 = Compute.d6();
                int d2 = Compute.d6();
                int d3 = Compute.d6();
                int max = Math.max(d1, Math.max(d2, d3));
                int result = (d1 + d2 + d3) - max;
                yield new ActionCheckRoll(result, List.of(d1, d2, d3));
            }
        };
    }
}
