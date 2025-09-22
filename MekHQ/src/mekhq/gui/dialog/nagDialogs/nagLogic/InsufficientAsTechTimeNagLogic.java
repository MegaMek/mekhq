/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs.nagLogic;

import java.util.Collection;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class InsufficientAsTechTimeNagLogic {
    /**
     * Determines whether there is an AsTech time deficit in the campaign.
     *
     * <p>This method calculates the AsTech time deficit by evaluating the total maintenance time
     * required by valid campaign units in comparison to the available AsTech work time. It returns {@code true} if the
     * deficit is positive, indicating that the campaign's available AsTech time is insufficient. Otherwise, it returns
     * {@code false}.</p>
     *
     * <p>An AsTech time deficit occurs when the required maintenance time exceeds the available
     * AsTech work time, even after accounting for any overtime that may be allowed.</p>
     *
     * @param units                      A collection of {@link Unit} objects to evaluate for maintenance needs.
     * @param possibleAsTechPoolMinutes  The total available AsTech work minutes without considering overtime.
     * @param isOvertimeAllowed          A flag indicating whether overtime is allowed, which adds to the available work
     *                                   time.
     * @param possibleAsTechPoolOvertime The additional AsTech work minutes available if overtime is allowed.
     *
     * @return {@code true} if there is a positive AsTech time deficit (deficit > 0), {@code false} otherwise.
     */
    public static boolean hasAsTechTimeDeficit(Collection<Unit> units, int possibleAsTechPoolMinutes,
          boolean isOvertimeAllowed, int possibleAsTechPoolOvertime) {
        int asTechsTimeDeficit = getAsTechTimeDeficit(units,
              possibleAsTechPoolMinutes,
              isOvertimeAllowed,
              possibleAsTechPoolOvertime);
        return asTechsTimeDeficit > 0;
    }

    /**
     * Calculates the AsTech time deficit for the campaign.
     *
     * <p>This method determines the total maintenance time required by valid hangar units
     * and compares it to the available AsTech work time in the campaign. The deficit, if any, is calculated, rounded
     * up, and returned as an integer value.</p>
     *
     * <p>A unit is considered valid for maintenance if it satisfies all the following conditions:</p>
     * <ul>
     *   <li>It is not marked as unmaintained.</li>
     *   <li>It is present in the hangar.</li>
     *   <li>It is not self-crewed (units maintained by their own crew are excluded).</li>
     * </ul>
     *
     * <p>Each valid unit contributes six AsTechs per unit of maintenance time to the total need.
     * If overtime is allowed, the additional overtime minutes are added to the available AsTech
     * work pool. The deficit is then calculated as the difference between total maintenance time
     * and available AsTech time, ensuring the result is never negative.</p>
     *
     * @param units                      A collection of {@link Unit} objects to evaluate for maintenance needs.
     * @param possibleAsTechPoolMinutes  The total available AsTech work minutes without considering overtime.
     * @param isOvertimeAllowed          A flag indicating whether overtime is allowed, which adds to the available work
     *                                   time.
     * @param possibleAsTechPoolOvertime The additional AsTech work minutes available if overtime is allowed.
     *
     * @return The rounded-up AsTech time deficit, or {@code 0} if there is no deficit.
     */
    public static int getAsTechTimeDeficit(Collection<Unit> units, int possibleAsTechPoolMinutes,
          boolean isOvertimeAllowed, int possibleAsTechPoolOvertime) {
        // Calculate the total maintenance time needed using a traditional loop
        int need = 0;
        for (Unit unit : units) {
            if (unit.isMaintained() && unit.isPresent() && !unit.isSelfCrewed()) {
                need += unit.getMaintenanceTime() * 6;
            }
        }

        if (isOvertimeAllowed) {
            possibleAsTechPoolMinutes += possibleAsTechPoolOvertime;
        }

        // Ensure deficit is non-negative
        return Math.max(0,
              (int) Math.ceil((need - possibleAsTechPoolMinutes) / (double) Person.PRIMARY_ROLE_SUPPORT_TIME));
    }
}
