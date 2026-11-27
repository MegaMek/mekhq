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
package mekhq.gui.dialog.crewAssignment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentData.PersonEntry;
import org.junit.jupiter.api.Test;

/**
 * Tests who {@link CrewAssignmentData#isAutoFillCandidate} lets auto-fill choose.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CrewAssignmentAutoFillCandidateTest {
    private static PersonEntry soldier(int hits, boolean isPregnant, Integer fatigue) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.SOLDIER);
        when(person.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(person.isPregnant()).thenReturn(isPregnant);
        return new PersonEntry(person, SkillLevel.REGULAR, List.of(), Map.of(), hits, fatigue, "",
              CrewAssignmentData.FamiliarityEntry.NONE);
    }

    @Test
    void aFitRestedSoldierIsChosen() {
        assertTrue(CrewAssignmentData.isAutoFillCandidate(soldier(0, false, 0)));
    }

    @Test
    void aWoundedSoldierIsPassedOver() {
        assertFalse(CrewAssignmentData.isAutoFillCandidate(soldier(1, false, 0)));
    }

    @Test
    void aPregnantSoldierIsPassedOver() {
        assertFalse(CrewAssignmentData.isAutoFillCandidate(soldier(0, true, 0)));
    }

    @Test
    void fatigueBelowTheLimitIsAllowed() {
        int belowLimit = CrewAssignmentData.AUTO_FILL_FATIGUE_LIMIT - 1;
        assertTrue(CrewAssignmentData.isAutoFillCandidate(soldier(0, false, belowLimit)));
    }

    @Test
    void fatigueAtTheLimitIsPassedOver() {
        int atLimit = CrewAssignmentData.AUTO_FILL_FATIGUE_LIMIT;
        assertFalse(CrewAssignmentData.isAutoFillCandidate(soldier(0, false, atLimit)));
    }

    @Test
    void withFatigueOffNoOneIsPassedOverForIt() {
        assertTrue(CrewAssignmentData.isAutoFillCandidate(soldier(0, false, null)));
    }
}
