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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.LocalPersonnel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.Test;

/**
 * Verifies that a generated command's senior posts are filled by the right people.
 *
 * <p>Seniority is stubbed rather than derived from real rank objects: these tests pin which post each
 * role is eligible for and that an occupied post is left alone, not the rank comparison itself, which
 * belongs to {@link Person} and is exercised elsewhere. Appointments are checked with {@code verify}
 * because a mocked setter records nothing for a getter to read back.</p>
 */
class SeniorAppointmentAssignerTest {

    /** A campaign with nobody yet on the books, so every post starts vacant. */
    private static Campaign campaign() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getPersonnel()).thenReturn(new LocalPersonnel());
        return campaign;
    }

    /**
     * A person in the given role who loses every rank comparison, so they can never be picked when a
     * senior alternative exists.
     */
    private static Person junior(PersonnelRole role) {
        Person person = mock(Person.class);
        when(person.getPrimaryRole()).thenReturn(role);
        return person;
    }

    /** A person in the given role who wins every rank comparison. */
    private static Person senior(PersonnelRole role) {
        Person person = junior(role);
        when(person.outRanksUsingSkillTiebreaker(any(Campaign.class), any())).thenReturn(true);
        return person;
    }

    @Test
    void eachPostGoesToItsOwnDiscipline() {
        Campaign campaign = campaign();
        Person doctor = junior(PersonnelRole.DOCTOR);
        Person mekTech = junior(PersonnelRole.MEK_TECH);
        Person administrator = junior(PersonnelRole.ADMINISTRATOR_COMMAND);

        SeniorAppointmentAssigner.assign(campaign, List.of(doctor, mekTech, administrator));

        verify(doctor).setChiefMedicalOfficer(true);
        verify(mekTech).setHeadTechnician(true);
        verify(administrator).setChiefAdministrator(true);
    }

    @Test
    void postsDoNotCrossDisciplines() {
        // A doctor must not end up as head technician just because no tech was generated.
        Campaign campaign = campaign();
        Person doctor = junior(PersonnelRole.DOCTOR);

        SeniorAppointmentAssigner.assign(campaign, List.of(doctor));

        verify(doctor).setChiefMedicalOfficer(true);
        verify(doctor, never()).setHeadTechnician(anyBoolean());
        verify(doctor, never()).setChiefAdministrator(anyBoolean());
    }

    @Test
    void vesselCrewCountAsTechnicians() {
        // PersonnelRole.isTech() counts vessel crew, so a command whose only technical staff keep its
        // large craft running still gets a head technician rather than leaving the post vacant.
        Campaign campaign = campaign();
        Person vesselCrew = junior(PersonnelRole.VESSEL_CREW);

        SeniorAppointmentAssigner.assign(campaign, List.of(vesselCrew));

        verify(vesselCrew).setHeadTechnician(true);
    }

    @Test
    void theSeniorCandidateTakesThePost() {
        Campaign campaign = campaign();
        Person juniorDoctor = junior(PersonnelRole.DOCTOR);
        Person seniorDoctor = senior(PersonnelRole.DOCTOR);

        // Listed junior-first so a naive "take the first match" implementation would fail this.
        SeniorAppointmentAssigner.assign(campaign, List.of(juniorDoctor, seniorDoctor));

        verify(seniorDoctor).setChiefMedicalOfficer(true);
        verify(juniorDoctor, never()).setChiefMedicalOfficer(anyBoolean());
    }

    @Test
    void anOccupiedPostIsLeftAlone() {
        // A player who appointed their own CMO should keep them when support is regenerated.
        Campaign campaign = mock(Campaign.class);
        Person incumbent = mock(Person.class);
        when(incumbent.isChiefMedicalOfficer()).thenReturn(true);
        LocalPersonnel roster = new LocalPersonnel();
        roster.put(UUID.randomUUID(), incumbent);
        when(campaign.getPersonnel()).thenReturn(roster);

        Person newDoctor = senior(PersonnelRole.DOCTOR);
        SeniorAppointmentAssigner.assign(campaign, List.of(newDoctor));

        verify(newDoctor, never()).setChiefMedicalOfficer(anyBoolean());
    }

    @Test
    void aPostWithNoEligibleCandidateIsLeftVacant() {
        // A build that produced no administrators must leave that post empty rather than fall through
        // to somebody unqualified.
        Campaign campaign = campaign();
        Person mekTech = junior(PersonnelRole.MEK_TECH);

        SeniorAppointmentAssigner.assign(campaign, List.of(mekTech));

        verify(mekTech).setHeadTechnician(true);
        verify(mekTech, never()).setChiefAdministrator(anyBoolean());
    }

    @Test
    void anEmptyRosterAppointsNobody() {
        SeniorAppointmentAssigner.assign(campaign(), List.of());
    }
}
