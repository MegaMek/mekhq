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
import static org.mockito.ArgumentMatchers.anyInt;
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
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.Skills;
import org.junit.jupiter.api.BeforeAll;
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

    @BeforeAll
    static void loadSkillTypes() {
        // Scoring resolves skills by name, which needs the skill table initialised.
        SkillType.initializeTypes();
    }

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
        when(person.getSkills()).thenReturn(new Skills());
        return person;
    }

    /** Gives a person a skill at the given level, so scoring has something to weigh. */
    private static Person withSkill(Person person, String skillName, int level) {
        person.getSkills().addSkill(skillName, new Skill(skillName, level, 0));
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

    @Test
    void theBestQualifiedCandidateTakesThePostEvenIfOutranked() {
        // The rule this encodes: the post follows competence. A highly skilled doctor should become CMO
        // ahead of a better-ranked but less capable one.
        Campaign campaign = campaign();
        Person seniorButLessSkilled = senior(PersonnelRole.DOCTOR);
        withSkill(seniorButLessSkilled, SkillType.S_SURGERY, 3);
        Person skilled = junior(PersonnelRole.DOCTOR);
        withSkill(skilled, SkillType.S_SURGERY, 8);

        SeniorAppointmentAssigner.assign(campaign, List.of(seniorButLessSkilled, skilled));

        verify(skilled).setChiefMedicalOfficer(true);
        verify(seniorButLessSkilled, never()).setChiefMedicalOfficer(anyBoolean());
    }

    @Test
    void leadershipAndAdministrationCountTowardsThePost() {
        // Two equally capable technicians; the one who can also lead and administer gets the post.
        Campaign campaign = campaign();
        Person pureTechnician = junior(PersonnelRole.MEK_TECH);
        withSkill(pureTechnician, SkillType.S_TECH_MEK, 6);
        Person managerTechnician = junior(PersonnelRole.MEK_TECH);
        withSkill(managerTechnician, SkillType.S_TECH_MEK, 6);
        withSkill(managerTechnician, SkillType.S_LEADER, 4);
        withSkill(managerTechnician, SkillType.S_ADMIN, 3);

        SeniorAppointmentAssigner.assign(campaign, List.of(pureTechnician, managerTechnician));

        verify(managerTechnician).setHeadTechnician(true);
        verify(pureTechnician, never()).setHeadTechnician(anyBoolean());
    }

    @Test
    void administrationIsNotCountedTwiceForTheChiefAdministrator() {
        // Administration is that post's discipline skill. Counting it again as a management skill would
        // let a pure administrator beat someone equally administrative who can also lead.
        Campaign campaign = campaign();
        Person pureAdministrator = junior(PersonnelRole.ADMINISTRATOR_COMMAND);
        withSkill(pureAdministrator, SkillType.S_ADMIN, 6);
        Person leadingAdministrator = junior(PersonnelRole.ADMINISTRATOR_HR);
        withSkill(leadingAdministrator, SkillType.S_ADMIN, 6);
        withSkill(leadingAdministrator, SkillType.S_LEADER, 2);

        SeniorAppointmentAssigner.assign(campaign, List.of(pureAdministrator, leadingAdministrator));

        verify(leadingAdministrator).setChiefAdministrator(true);
        verify(pureAdministrator, never()).setChiefAdministrator(anyBoolean());
    }

    @Test
    void theAppointeeIsPromotedToMatchTheirHighestRankedStaff() {
        // A section head outranked by their own staff is the thing this prevents.
        Campaign campaign = campaign();
        Person skilledButJunior = junior(PersonnelRole.DOCTOR);
        withSkill(skilledButJunior, SkillType.S_SURGERY, 9);
        when(skilledButJunior.getRankNumeric()).thenReturn(12);

        Person seniorDoctor = senior(PersonnelRole.DOCTOR);
        withSkill(seniorDoctor, SkillType.S_SURGERY, 2);
        when(seniorDoctor.getRankNumeric()).thenReturn(35);

        SeniorAppointmentAssigner.assign(campaign, List.of(skilledButJunior, seniorDoctor));

        verify(skilledButJunior).setChiefMedicalOfficer(true);
        verify(skilledButJunior).setRank(35);
    }

    @Test
    void anAppointeeWhoAlreadyOutranksEveryoneIsNotPromoted() {
        Campaign campaign = campaign();
        Person topDoctor = senior(PersonnelRole.DOCTOR);
        withSkill(topDoctor, SkillType.S_SURGERY, 9);
        when(topDoctor.getRankNumeric()).thenReturn(35);
        Person otherDoctor = junior(PersonnelRole.DOCTOR);
        withSkill(otherDoctor, SkillType.S_SURGERY, 2);
        when(otherDoctor.getRankNumeric()).thenReturn(12);

        SeniorAppointmentAssigner.assign(campaign, List.of(topDoctor, otherDoctor));

        verify(topDoctor).setChiefMedicalOfficer(true);
        verify(topDoctor, never()).setRank(anyInt());
    }

    @Test
    void staffWithNoSkillsAtAllStillProduceAnAppointment() {
        // Support staff are currently generated without Leadership, and Administration is gated behind
        // campaign options, so scoring must cope with candidates who have none of the three.
        Campaign campaign = campaign();
        Person doctor = junior(PersonnelRole.DOCTOR);

        SeniorAppointmentAssigner.assign(campaign, List.of(doctor));

        verify(doctor).setChiefMedicalOfficer(true);
    }
}
