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

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

public class UntreatedPersonnelNagLogic {
    /**
     * Checks whether the campaign has any untreated personnel with injuries.
     *
     * <p>
     * This method iterates over the campaign's active personnel and identifies individuals
     * who meet the following criteria:
     * <ul>
     *     <li>The individual requires treatment ({@link Person#needsFixing()}).</li>
     *     <li>The individual has not been assigned to a doctor.</li>
     *     <li>The individual is not currently classified as a prisoner.</li>
     * </ul>
     * If any personnel match these conditions, the method returns {@code true}.
     *
     * @return {@code true} if untreated injuries are present, otherwise {@code false}.
     */
    public static boolean campaignHasUntreatedInjuries(Campaign campaign) {
        for (Person person : campaign.getActivePersonnel()) {
            if (!person.getPrisonerStatus().isCurrentPrisoner()
                && person.needsFixing()
                && person.getDoctorId() == null) {
                return true;
            }
        }
        return false;
    }
}
