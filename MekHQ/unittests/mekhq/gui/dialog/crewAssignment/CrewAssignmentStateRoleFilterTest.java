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

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.RoleGroup;
import mekhq.gui.dialog.crewAssignment.CrewAssignmentState.SingleRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the Personnel page's Role filter in {@link CrewAssignmentState}, and who {@link CrewAssignmentData} lists
 * and lets auto-fill choose.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CrewAssignmentStateRoleFilterTest {
    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "MEK_TECH", "MECHANIC", "AERO_TEK", "BA_TECH" })
    void techRolesAreOffered(PersonnelRole role) {
        assertTrue(CrewAssignmentState.isFilterableRole(role));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "MEKWARRIOR", "VESSEL_CREW" })
    void combatRolesAreOffered(PersonnelRole role) {
        assertTrue(CrewAssignmentState.isFilterableRole(role));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "ASTECH", "DOCTOR", "MEDIC", "ADMINISTRATOR_COMMAND", "NONE" })
    void otherSupportRolesAreNotOffered(PersonnelRole role) {
        assertFalse(CrewAssignmentState.isFilterableRole(role));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "MEK_TECH", "MECHANIC", "AERO_TEK", "BA_TECH", "ASTECH" })
    void vehicleTechsAreAllTechs(PersonnelRole role) {
        assertTrue(RoleGroup.ALL_TECHS.matches(role));
        assertFalse(RoleGroup.OTHER_PROFESSIONS.matches(role));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "DOCTOR", "MEDIC", "COMMS_OPERATOR", "TECH_COMMUNICATIONS",
                                                       "SENSOR_TECHNICIAN", "ADMINISTRATOR", "CHEF" })
    void otherVehicleProfessionsAreOtherProfessions(PersonnelRole role) {
        assertTrue(RoleGroup.OTHER_PROFESSIONS.matches(role));
        assertFalse(RoleGroup.ALL_TECHS.matches(role));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "SOLDIER", "VEHICLE_CREW_GROUND", "MEKWARRIOR", "NONE",
                                                       "VESSEL_CREW" })
    void combatAndUnrelatedRolesAreInNoGroup(PersonnelRole role) {
        assertFalse(RoleGroup.ALL_TECHS.matches(role));
        assertFalse(RoleGroup.OTHER_PROFESSIONS.matches(role));
    }

    @Test
    void groupMatchesOnSecondaryRole() {
        Person person = person(PersonnelStatus.ACTIVE, PersonnelRole.MEKWARRIOR, PersonnelRole.MEK_TECH);
        assertTrue(CrewAssignmentState.matches(person, RoleGroup.ALL_TECHS));
        assertTrue(CrewAssignmentState.matches(person, new SingleRole(PersonnelRole.MEKWARRIOR)));
        assertFalse(CrewAssignmentState.matches(person, RoleGroup.OTHER_PROFESSIONS));
    }

    @Test
    void otherProfessionsAreListedButNotAutoFilled() {
        Person doctor = person(PersonnelStatus.ACTIVE, PersonnelRole.DOCTOR, PersonnelRole.NONE);
        assertTrue(CrewAssignmentData.isShown(doctor));
        assertFalse(CrewAssignmentData.isAutoFillCandidate(doctor));
    }

    @Test
    void campFollowersAreNeverListed() {
        Person follower = person(PersonnelStatus.CAMP_FOLLOWER, PersonnelRole.VEHICLE_CREW_GROUND,
              PersonnelRole.NONE);
        assertFalse(CrewAssignmentData.isShown(follower));
    }

    @Test
    void unrelatedSupportStaffAreNotListed() {
        Person lawyer = person(PersonnelStatus.ACTIVE, PersonnelRole.LAWYER, PersonnelRole.NONE);
        assertFalse(CrewAssignmentData.isShown(lawyer));
    }

    private static Person person(PersonnelStatus status, PersonnelRole primaryRole, PersonnelRole secondaryRole) {
        Person person = mock(Person.class);
        when(person.getStatus()).thenReturn(status);
        when(person.isEmployed()).thenReturn(status != PersonnelStatus.CAMP_FOLLOWER);
        when(person.getPrimaryRole()).thenReturn(primaryRole);
        when(person.getSecondaryRole()).thenReturn(secondaryRole);
        return person;
    }
}
