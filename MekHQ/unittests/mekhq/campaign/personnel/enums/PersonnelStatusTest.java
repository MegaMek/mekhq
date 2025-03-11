/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static mekhq.campaign.personnel.enums.PersonnelStatus.*;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonnelStatusTest {
    @Test
    public void testFromString_ValidStatus() {
        PersonnelStatus status = PersonnelStatus.fromString(STUDENT.name());
        assertEquals(STUDENT, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        PersonnelStatus status = PersonnelStatus.fromString("INVALID_STATUS");

        assertEquals(ACTIVE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        PersonnelStatus status = PersonnelStatus.fromString(null);

        assertEquals(ACTIVE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        PersonnelStatus status = PersonnelStatus.fromString("");

        assertEquals(ACTIVE, status);
    }

    @Test
    public void testFromString_Ordinal() {
        PersonnelStatus status = PersonnelStatus.fromString(STUDENT.ordinal() + "");

        assertEquals(STUDENT, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (PersonnelStatus status : PersonnelStatus.values()) {
            String label = status.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetToolTipText_notInvalid() {
        for (PersonnelStatus status : PersonnelStatus.values()) {
            String toolTipText = status.getToolTipText();
            assertTrue(isResourceKeyValid(toolTipText));
        }
    }

    @Test
    public void testGetReportText_notInvalid() {
        for (PersonnelStatus status : PersonnelStatus.values()) {
            String reportText = status.getReportText();
            assertTrue(isResourceKeyValid(reportText));
        }
    }

    @Test
    public void testGetLogText_notInvalid() {
        for (PersonnelStatus status : PersonnelStatus.values()) {
            String logText = status.getLogText();
            assertTrue(isResourceKeyValid(logText));
        }
    }

    @Test
    public void testIsAbsent() {
        List<PersonnelStatus> validStatuses = List.of(MIA, POW, ENEMY_BONDSMAN, ON_LEAVE,
            ON_MATERNITY_LEAVE, AWOL, STUDENT, MISSING);

        for (PersonnelStatus status : PersonnelStatus.values()) {
            boolean isAbsent = validStatuses.contains(status);

            assertEquals(status.isAbsent(), isAbsent);
        }
    }

    @Test
    public void testIsDepartedUnit() {
        List<PersonnelStatus> deadStatuses = List.of(KIA, HOMICIDE, WOUNDS, DISEASE, ACCIDENTAL,
            NATURAL_CAUSES, OLD_AGE, MEDICAL_COMPLICATIONS, PREGNANCY_COMPLICATIONS, UNDETERMINED,
            SUICIDE, BONDSREF);
        List<PersonnelStatus> validStatuses = List.of(RETIRED, RESIGNED, SACKED, DESERTED,
            DEFECTED, MISSING, LEFT, ENEMY_BONDSMAN);

        for (PersonnelStatus status : PersonnelStatus.values()) {
            boolean hasDepartedUnit = validStatuses.contains(status) || deadStatuses.contains(status);

            assertEquals(status.isDepartedUnit(), hasDepartedUnit);
        }
    }

    @Test
    public void testIsDead() {
        List<PersonnelStatus> validStatuses = List.of(KIA, HOMICIDE, WOUNDS, DISEASE, ACCIDENTAL,
            NATURAL_CAUSES, OLD_AGE, MEDICAL_COMPLICATIONS, PREGNANCY_COMPLICATIONS, UNDETERMINED,
            SUICIDE, BONDSREF);

        for (PersonnelStatus status : PersonnelStatus.values()) {
            boolean isDead = validStatuses.contains(status);

            assertEquals(status.isDead(), isDead);
        }
    }

    @Test
    public void testIsDeadOrMIA() {
        List<PersonnelStatus> validStatuses = List.of(KIA, HOMICIDE, WOUNDS, DISEASE, ACCIDENTAL,
            NATURAL_CAUSES, OLD_AGE, MEDICAL_COMPLICATIONS, PREGNANCY_COMPLICATIONS, UNDETERMINED,
            SUICIDE, BONDSREF, MIA);

        for (PersonnelStatus status : PersonnelStatus.values()) {
            boolean isDeadOrMIA = validStatuses.contains(status);

            assertEquals(status.isDeadOrMIA(), isDeadOrMIA);
        }
    }
}
