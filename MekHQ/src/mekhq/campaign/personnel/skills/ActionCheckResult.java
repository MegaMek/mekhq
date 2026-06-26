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

import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;

import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;

/**
 * An immutable record representing the outcome of an action check.
 *
 * @param roll            Roll result for the action check
 * @param marginOfSuccess Calculated margin of success for this action check. Represents how much better (or worse)
 *                        the roll was compared to the target number
 * @param usedEdge        Indicates whether edge was used during the action check
 * @param resultsText     A string representing the outcome of the action check
 *
 * @author Hokk
 * @since 0.51.01
 */
public record ActionCheckResult(
      int roll,
      int marginOfSuccess,
      boolean usedEdge,
      String resultsText
) {

    /**
     * Determines whether the action check was successful.
     *
     * <p>An action check is considered successful if the calculated margin of success is equal or exceeds
     * {@link MarginOfSuccess#BARELY_MADE_IT}.</p>
     *
     * @return {@code true} if the action check succeeded, {@code false} otherwise
     */
    public boolean isSuccess() {
        return isSuccess(marginOfSuccess);
    }

    /**
     * Determines whether a given margin of success represents a successful action check.
     *
     * <p>A margin is considered successful if it is equal or exceeds {@link MarginOfSuccess#BARELY_MADE_IT}.</p>
     *
     * @param marginOfSuccess the margin of success to evaluate
     * @return {@code true} if the margin qualifies as a success, {@code false} otherwise
     */
    public static boolean isSuccess(int marginOfSuccess) {
        return marginOfSuccess >= BARELY_MADE_IT.getValue();
    }
}
