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

import java.util.List;

import mekhq.campaign.force.Formation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class PregnantCombatantNagLogic {
    /**
     * Determines if the current campaign contains any personnel who are pregnant and actively assigned to a combat
     * force.
     *
     * <p>This method evaluates the following conditions to return {@code true}:</p>
     * <ul>
     *     <li>The campaign has an active contract.</li>
     *     <li>There are pregnant personnel in the list of active personnel.</li>
     *     <li>A pregnant person is assigned to a unit that is part of a combat force
     *         (i.e., a force with an ID other than {@link Formation#FORMATION_NONE}).</li>
     * </ul>
     *
     * <p>If no active contract exists in the campaign, the method immediately returns {@code false}.
     * This prevents unnecessary processing of the personnel list.</p>
     *
     * @param hasActiveContract A flag indicating whether the campaign currently has an active contract.
     * @param activePersonnel   A list of {@link Person} objects representing the active personnel in the campaign.
     *
     * @return {@code true} if there are pregnant personnel assigned to a combat force; {@code false} otherwise.
     */
    public static boolean hasActivePregnantCombatant(boolean hasActiveContract, List<Person> activePersonnel) {
        if (!hasActiveContract) {
            return false;
        }

        // There is no reason to use a stream here, as there won't be enough iterations to warrant it.
        for (Person person : activePersonnel) {
            if (person.isPregnant()) {
                Unit unit = person.getUnit();

                if (unit != null) {
                    if (unit.getFormationId() != Formation.FORMATION_NONE) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
