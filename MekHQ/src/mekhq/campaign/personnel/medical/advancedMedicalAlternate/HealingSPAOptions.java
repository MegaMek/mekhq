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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static mekhq.campaign.personnel.PersonnelOptions.EDGE_MEDICAL;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_HOLISTIC_CARE;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_HYPOCHONDRIAC;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_PATHOLOGIC_INSIGHT;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_PROTHESIS_TECHNICIAN;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_TRAUMA_SURGEON;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;

record HealingSPAOptions(
      boolean hasMedicalEdge,
      boolean hasHolisticCareSPA,
      boolean hasTraumaSurgeon,
      boolean hasProthesisTechnician,
      boolean hasPathologicInsight,
      boolean hasHypochondriac) {

    static HealingSPAOptions from(Person doctor, Person patient) {
        PersonnelOptions doctorOptions = doctor.getOptions();
        PersonnelOptions patientOptions = patient.getOptions();

        return new HealingSPAOptions(
              doctorOptions.booleanOption(EDGE_MEDICAL),
              doctorOptions.booleanOption(UNOFFICIAL_HOLISTIC_CARE),
              doctorOptions.booleanOption(UNOFFICIAL_TRAUMA_SURGEON),
              doctorOptions.booleanOption(UNOFFICIAL_PROTHESIS_TECHNICIAN),
              doctorOptions.booleanOption(UNOFFICIAL_PATHOLOGIC_INSIGHT),
              patientOptions.booleanOption(UNOFFICIAL_HYPOCHONDRIAC));
    }
}
