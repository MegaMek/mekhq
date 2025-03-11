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
 */
package mekhq.gui.dialog.nagDialogs.nagLogic;

import mekhq.campaign.personnel.Person;

import java.util.List;

public class UntreatedPersonnelNagLogic {
    /**
     * Determines whether the campaign has any personnel with untreated injuries.
     *
     * <p>This method evaluates the active personnel in the campaign to identify individuals who:</p>
     * <ul>
     *     <li>Require medical treatment ({@link Person#needsFixing()}).</li>
     *     <li>Have not been assigned to a doctor (their {@code getDoctorId()} is {@code null}).</li>
     * </ul>
     *
     * <p>If any personnel meet these criteria, the method returns {@code true}.</p>
     *
     * @param activePersonnel A {@link List} of active personnel in the campaign.
     * @return {@code true} if there are untreated injuries among the personnel, {@code false} otherwise.
     */
    public static boolean campaignHasUntreatedInjuries(List<Person> activePersonnel) {
        for (Person person : activePersonnel) {
            if (person.needsFixing() && person.getDoctorId() == null) {
                return true;
            }
        }
        return false;
    }
}
