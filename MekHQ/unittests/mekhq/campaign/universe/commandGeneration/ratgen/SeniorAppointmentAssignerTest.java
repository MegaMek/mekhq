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

import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.force.PlayerForce;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.LocalPersonnel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.Skills;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies that a generated command's senior posts are filled by the right people.
 *
 * <p>Seniority is stubbed rather than derived from real rank objects: these tests pin which post each
 * role is eligible for and that an occupied post is left alone, not the rank comparison itself, which
 * belongs to {@link Person} and is exercised elsewhere. Appointments are checked with {@code verify}
 * because a mocked setter records nothing for a getter to read back.</p>
 */
class SeniorAppointmentAssignerTest {

    /** Colonel, the hard ceiling for any support post. */
    private static final int COLONEL = 38;

    /** Captain, where the skill ladder now stops for commissioned support staff. */
    private static final int CAPTAIN = 34;

    @BeforeAll
    static void loadSkillTypesAndRanks() {
        // Scoring resolves skills by name and promotion walks a real rank table, so both need loading.
        SkillType.initializeTypes();
        Ranks.initializeRankSystems();
    }

    /** A campaign with nobody yet on the books, so every post starts vacant. */
    /**
     * Wires the personnel chain on a campaign mock. The roster now hangs off the player force's human
     * resources rather than the campaign itself, so the intermediate mocks have to be stubbed for the
     * chained call to resolve.
     *
     * @param campaign the campaign mock to wire
     * @param roster   the personnel the campaign should report
     */
    private static void stubPersonnel(Campaign campaign, LocalPersonnel roster) {
        PlayerForce playerForce = mock(PlayerForce.class);
        ForceHumanResources humanResources = mock(ForceHumanResources.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(humanResources.getPersonnel()).thenReturn(roster.values());
    }

    private static Campaign campaign() {
        Campaign campaign = mock(Campaign.class);
        stubPersonnel(campaign, new LocalPersonnel());
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

    /** The shipped SLDF rank system, which leaves indices 16 and 37 unnamed. */
    private static RankSystem sldf() {
        return Ranks.getRankSystems().get("SLDF");
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
        stubPersonnel(campaign, roster);

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
    void theAppointeeIsPromotedAboveTheirHighestRankedStaff() {
        // A section head outranked by their own staff is the thing this prevents.
        Campaign campaign = campaign();
        Person skilledButJunior = junior(PersonnelRole.DOCTOR);
        withSkill(skilledButJunior, SkillType.S_SURGERY, 9);
        when(skilledButJunior.getRankNumeric()).thenReturn(12);

        Person seniorDoctor = senior(PersonnelRole.DOCTOR);
        withSkill(seniorDoctor, SkillType.S_SURGERY, 2);
        when(seniorDoctor.getRankNumeric()).thenReturn(35);
        when(skilledButJunior.getRankSystem()).thenReturn(sldf());

        SeniorAppointmentAssigner.assign(campaign, List.of(skilledButJunior, seniorDoctor));

        verify(skilledButJunior).setChiefMedicalOfficer(true);
        // One named rung above the senior doctor at 35, not level with them.
        ArgumentCaptor<Integer> promotedTo = ArgumentCaptor.forClass(Integer.class);
        verify(skilledButJunior).setRank(promotedTo.capture());
        assertTrue(promotedTo.getValue() > 35, "the head should outrank their staff");
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

    @Test
    void takingAPostConfersLeadership() {
        // Support staff are generated with no command skills at all, so without this a section head
        // would be running a section with no ability to lead one.
        Campaign campaign = campaign();
        Person doctor = junior(PersonnelRole.DOCTOR);
        withSkill(doctor, SkillType.S_SURGERY, 7);

        SeniorAppointmentAssigner.assign(campaign, List.of(doctor));

        verify(doctor).setChiefMedicalOfficer(true);
        ArgumentCaptor<Skill> granted = ArgumentCaptor.forClass(Skill.class);
        verify(doctor).addSkill(eq(SkillType.S_LEADER), granted.capture());
        assertTrue(granted.getValue().getLevel() > 0, "the granted Leadership should be a real level");
    }

    @Test
    void leadershipIsScaledToTheirDisciplineCompetence() {
        // An elite surgeon should lead their section better than a green one, so the grant tracks the
        // skill that got them the post rather than being a flat value.
        Campaign campaign = campaign();
        Person greenDoctor = junior(PersonnelRole.DOCTOR);
        withSkill(greenDoctor, SkillType.S_SURGERY, 2);
        SeniorAppointmentAssigner.assign(campaign, List.of(greenDoctor));

        Campaign secondCampaign = campaign();
        Person eliteDoctor = junior(PersonnelRole.DOCTOR);
        withSkill(eliteDoctor, SkillType.S_SURGERY, 9);
        SeniorAppointmentAssigner.assign(secondCampaign, List.of(eliteDoctor));

        ArgumentCaptor<Skill> greenGrant = ArgumentCaptor.forClass(Skill.class);
        verify(greenDoctor).addSkill(eq(SkillType.S_LEADER), greenGrant.capture());
        ArgumentCaptor<Skill> eliteGrant = ArgumentCaptor.forClass(Skill.class);
        verify(eliteDoctor).addSkill(eq(SkillType.S_LEADER), eliteGrant.capture());

        assertTrue(eliteGrant.getValue().getLevel() > greenGrant.getValue().getLevel(),
              "the elite surgeon should be granted the stronger Leadership");
    }

    @Test
    void anExistingBetterLeadershipIsNotDowngraded() {
        Campaign campaign = campaign();
        Person doctor = junior(PersonnelRole.DOCTOR);
        withSkill(doctor, SkillType.S_SURGERY, 2);
        withSkill(doctor, SkillType.S_LEADER, 10);

        SeniorAppointmentAssigner.assign(campaign, List.of(doctor));

        verify(doctor).setChiefMedicalOfficer(true);
        verify(doctor, never()).addSkill(eq(SkillType.S_LEADER), any(Skill.class));
    }

    @Test
    void noLeadershipIsGrantedWithoutADisciplineSkillToScaleFrom() {
        // Nothing to scale from means no defensible level, so the post is filled but the skill is not
        // invented out of thin air.
        Campaign campaign = campaign();
        Person doctor = junior(PersonnelRole.DOCTOR);

        SeniorAppointmentAssigner.assign(campaign, List.of(doctor));

        verify(doctor).setChiefMedicalOfficer(true);
        verify(doctor, never()).addSkill(eq(SkillType.S_LEADER), any(Skill.class));
    }

    @Test
    void eachTechSpecialityGetsItsOwnDepartmentHead() {
        // The point of the second tier: a Mek Tech department and a Mechanic department each get their
        // own head, judged on their own speciality rather than lumped together.
        Campaign campaign = campaign();
        Person bestMekTech = junior(PersonnelRole.MEK_TECH);
        withSkill(bestMekTech, SkillType.S_TECH_MEK, 8);
        Person otherMekTech = junior(PersonnelRole.MEK_TECH);
        withSkill(otherMekTech, SkillType.S_TECH_MEK, 3);
        Person bestMechanic = junior(PersonnelRole.MECHANIC);
        withSkill(bestMechanic, SkillType.S_TECH_MECHANIC, 7);
        Person otherMechanic = junior(PersonnelRole.MECHANIC);
        withSkill(otherMechanic, SkillType.S_TECH_MECHANIC, 2);

        SeniorAppointmentAssigner.assign(campaign,
              List.of(bestMekTech, otherMekTech, bestMechanic, otherMechanic));

        verify(bestMekTech).setDepartmentHead(true);
        verify(bestMechanic).setDepartmentHead(true);
        verify(otherMekTech, never()).setDepartmentHead(anyBoolean());
        verify(otherMechanic, never()).setDepartmentHead(anyBoolean());
    }

    @Test
    void administrativeSpecialitiesGetDepartmentHeadsToo() {
        Campaign campaign = campaign();
        Person bestLogistics = junior(PersonnelRole.ADMINISTRATOR_LOGISTICS);
        withSkill(bestLogistics, SkillType.S_ADMIN, 8);
        Person otherLogistics = junior(PersonnelRole.ADMINISTRATOR_LOGISTICS);
        withSkill(otherLogistics, SkillType.S_ADMIN, 2);

        SeniorAppointmentAssigner.assign(campaign, List.of(bestLogistics, otherLogistics));

        verify(bestLogistics).setDepartmentHead(true);
        verify(otherLogistics, never()).setDepartmentHead(anyBoolean());
    }

    @Test
    void aDepartmentOfOneGetsNoHead() {
        // Titling a lone technician the head of a department they are the entirety of adds nothing.
        Campaign campaign = campaign();
        Person loneMekTech = junior(PersonnelRole.MEK_TECH);
        withSkill(loneMekTech, SkillType.S_TECH_MEK, 8);

        SeniorAppointmentAssigner.assign(campaign, List.of(loneMekTech));

        verify(loneMekTech, never()).setDepartmentHead(anyBoolean());
        // The branch-wide post still goes to them - they are the whole technical staff.
        verify(loneMekTech).setHeadTechnician(true);
    }

    @Test
    void doctorsGetNoDepartmentHeadBecauseTheChiefMedicalOfficerHeadsMedicine() {
        Campaign campaign = campaign();
        Person seniorDoctor = junior(PersonnelRole.DOCTOR);
        withSkill(seniorDoctor, SkillType.S_SURGERY, 8);
        Person otherDoctor = junior(PersonnelRole.DOCTOR);
        withSkill(otherDoctor, SkillType.S_SURGERY, 3);

        SeniorAppointmentAssigner.assign(campaign, List.of(seniorDoctor, otherDoctor));

        verify(seniorDoctor).setChiefMedicalOfficer(true);
        verify(seniorDoctor, never()).setDepartmentHead(anyBoolean());
        verify(otherDoctor, never()).setDepartmentHead(anyBoolean());
    }

    @Test
    void aHeadIsPromotedAboveTheirStaffRatherThanLevelWithThem() {
        // The reported problem: the chief shared a rank with one of the people they run.
        Campaign campaign = campaign();
        Person chosen = junior(PersonnelRole.DOCTOR);
        withSkill(chosen, SkillType.S_SURGERY, 9);
        when(chosen.getRankNumeric()).thenReturn(30);
        when(chosen.getRankSystem()).thenReturn(sldf());

        Person seniorStaff = junior(PersonnelRole.DOCTOR);
        withSkill(seniorStaff, SkillType.S_SURGERY, 2);
        when(seniorStaff.getRankNumeric()).thenReturn(34);

        SeniorAppointmentAssigner.assign(campaign, List.of(chosen, seniorStaff));

        verify(chosen).setChiefMedicalOfficer(true);
        ArgumentCaptor<Integer> promotedTo = ArgumentCaptor.forClass(Integer.class);
        verify(chosen).setRank(promotedTo.capture());
        assertTrue(promotedTo.getValue() > 34,
              "the chief should outrank their staff, not share a rank with them");
    }

    @Test
    void promotionSkipsRungsTheRankSystemLeavesBlank() {
        // SLDF names nothing at 37, so a chief stepping up from 36 must not stop there.
        Campaign campaign = campaign();
        Person chosen = junior(PersonnelRole.DOCTOR);
        withSkill(chosen, SkillType.S_SURGERY, 9);
        when(chosen.getRankNumeric()).thenReturn(30);
        when(chosen.getRankSystem()).thenReturn(sldf());

        Person seniorStaff = junior(PersonnelRole.DOCTOR);
        withSkill(seniorStaff, SkillType.S_SURGERY, 2);
        when(seniorStaff.getRankNumeric()).thenReturn(36);

        SeniorAppointmentAssigner.assign(campaign, List.of(chosen, seniorStaff));

        ArgumentCaptor<Integer> promotedTo = ArgumentCaptor.forClass(Integer.class);
        verify(chosen).setRank(promotedTo.capture());
        assertNotEquals(37, promotedTo.getValue().intValue(),
              "index 37 is blank in SLDF and must be skipped");
    }

    @Test
    void aHeadIsNeverPromotedPastTheUnitCommander() {
        // Support services do not outrank the officer commanding the force, however senior their own
        // staff happen to be.
        Campaign campaign = mock(Campaign.class);
        Person commander = mock(Person.class);
        when(commander.getRankNumeric()).thenReturn(34);
        LocalPersonnel roster = new LocalPersonnel();
        roster.put(UUID.randomUUID(), commander);
        stubPersonnel(campaign, roster);

        Person chosen = junior(PersonnelRole.DOCTOR);
        withSkill(chosen, SkillType.S_SURGERY, 9);
        when(chosen.getRankNumeric()).thenReturn(20);
        when(chosen.getRankSystem()).thenReturn(sldf());

        Person seniorStaff = junior(PersonnelRole.DOCTOR);
        withSkill(seniorStaff, SkillType.S_SURGERY, 2);
        when(seniorStaff.getRankNumeric()).thenReturn(33);

        SeniorAppointmentAssigner.assign(campaign, List.of(chosen, seniorStaff));

        verify(chosen).setChiefMedicalOfficer(true);
        ArgumentCaptor<Integer> promotedTo = ArgumentCaptor.forClass(Integer.class);
        verify(chosen).setRank(promotedTo.capture());
        assertTrue(promotedTo.getValue() <= 34,
              "the CMO should not be promoted past the commander at 34, but reached "
                    + promotedTo.getValue());
    }

    @Test
    void aHeadStaysPutRatherThanPassingTheCommander() {
        // Staff already level with the commander leaves no room to step into, so the head keeps the
        // rank they have instead of breaching the cap.
        Campaign campaign = mock(Campaign.class);
        Person commander = mock(Person.class);
        when(commander.getRankNumeric()).thenReturn(35);
        LocalPersonnel roster = new LocalPersonnel();
        roster.put(UUID.randomUUID(), commander);
        stubPersonnel(campaign, roster);

        Person chosen = junior(PersonnelRole.DOCTOR);
        withSkill(chosen, SkillType.S_SURGERY, 9);
        when(chosen.getRankNumeric()).thenReturn(35);
        when(chosen.getRankSystem()).thenReturn(sldf());

        Person seniorStaff = junior(PersonnelRole.DOCTOR);
        withSkill(seniorStaff, SkillType.S_SURGERY, 2);
        when(seniorStaff.getRankNumeric()).thenReturn(35);

        SeniorAppointmentAssigner.assign(campaign, List.of(chosen, seniorStaff));

        verify(chosen).setChiefMedicalOfficer(true);
        verify(chosen, never()).setRank(anyInt());
    }

    @Test
    void noSupportPostRisesAboveColonel() {
        // A general officer commanding the force does not entitle their chief administrator to keep
        // climbing: Colonel is the ceiling for the post itself.
        Campaign campaign = mock(Campaign.class);
        Person generalOfficer = mock(Person.class);
        when(generalOfficer.getRankNumeric()).thenReturn(45);
        LocalPersonnel roster = new LocalPersonnel();
        roster.put(UUID.randomUUID(), generalOfficer);
        stubPersonnel(campaign, roster);

        Person chosen = junior(PersonnelRole.ADMINISTRATOR_COMMAND);
        withSkill(chosen, SkillType.S_ADMIN, 9);
        when(chosen.getRankNumeric()).thenReturn(20);
        when(chosen.getRankSystem()).thenReturn(sldf());

        Person seniorStaff = junior(PersonnelRole.ADMINISTRATOR_COMMAND);
        withSkill(seniorStaff, SkillType.S_ADMIN, 2);
        when(seniorStaff.getRankNumeric()).thenReturn(38);

        SeniorAppointmentAssigner.assign(campaign, List.of(chosen, seniorStaff));

        verify(chosen).setChiefAdministrator(true);
        ArgumentCaptor<Integer> promotedTo = ArgumentCaptor.forClass(Integer.class);
        verify(chosen, atLeast(0)).setRank(promotedTo.capture());
        for (Integer rank : promotedTo.getAllValues()) {
            assertTrue(rank <= COLONEL, "no support post should exceed Colonel, got " + rank);
        }
    }

    @Test
    void theLowerOfTheTwoCapsWins() {
        // A Captain commanding a small unit caps their chief medical officer well below Colonel.
        Campaign campaign = mock(Campaign.class);
        Person captain = mock(Person.class);
        when(captain.getRankNumeric()).thenReturn(34);
        LocalPersonnel roster = new LocalPersonnel();
        roster.put(UUID.randomUUID(), captain);
        stubPersonnel(campaign, roster);

        Person chosen = junior(PersonnelRole.DOCTOR);
        withSkill(chosen, SkillType.S_SURGERY, 9);
        when(chosen.getRankNumeric()).thenReturn(20);
        when(chosen.getRankSystem()).thenReturn(sldf());

        Person seniorStaff = junior(PersonnelRole.DOCTOR);
        withSkill(seniorStaff, SkillType.S_SURGERY, 2);
        when(seniorStaff.getRankNumeric()).thenReturn(33);

        SeniorAppointmentAssigner.assign(campaign, List.of(chosen, seniorStaff));

        ArgumentCaptor<Integer> promotedTo = ArgumentCaptor.forClass(Integer.class);
        verify(chosen).setRank(promotedTo.capture());
        assertTrue(promotedTo.getValue() <= 34,
              "the commander's rank is the lower cap here, got " + promotedTo.getValue());
    }

    @Test
    void thePostsBuildAPyramidRatherThanABlockOfSeniorOfficers() {
        // The whole point of reserving field grade for posts. Staff all arrive at Captain, because the
        // generator creates the cohort at one experience level; the appointment pass is what produces
        // the spread above them.
        Campaign campaign = mock(Campaign.class);
        Person commander = mock(Person.class);
        when(commander.getRankNumeric()).thenReturn(COLONEL);
        LocalPersonnel roster = new LocalPersonnel();
        roster.put(UUID.randomUUID(), commander);
        stubPersonnel(campaign, roster);

        List<Person> staff = new ArrayList<>();
        Person bestLogistics = null;
        for (int index = 0; index < 2; index++) {
            Person person = junior(PersonnelRole.ADMINISTRATOR_LOGISTICS);
            withSkill(person, SkillType.S_ADMIN, index == 0 ? 8 : 3);
            when(person.getRankNumeric()).thenReturn(CAPTAIN);
            when(person.getRankSystem()).thenReturn(sldf());
            staff.add(person);
            if (index == 0) {
                bestLogistics = person;
            }
        }
        for (int index = 0; index < 2; index++) {
            Person person = junior(PersonnelRole.ADMINISTRATOR_HR);
            withSkill(person, SkillType.S_ADMIN, 2);
            when(person.getRankNumeric()).thenReturn(CAPTAIN);
            when(person.getRankSystem()).thenReturn(sldf());
            staff.add(person);
        }

        SeniorAppointmentAssigner.assign(campaign, staff);

        // The strongest logistics administrator heads that department and is raised above Captain.
        verify(bestLogistics).setDepartmentHead(true);
        ArgumentCaptor<Integer> logisticsRank = ArgumentCaptor.forClass(Integer.class);
        verify(bestLogistics, atLeastOnce()).setRank(logisticsRank.capture());
        int headRank = logisticsRank.getAllValues().get(logisticsRank.getAllValues().size() - 1);
        assertTrue(headRank > CAPTAIN, "a department head should sit above their staff, got " + headRank);
        assertTrue(headRank <= COLONEL, "and no higher than Colonel, got " + headRank);
    }

    /**
     * A rank system shaped like the Clan one: the profession names two rungs of its own, and every rung
     * above those belongs to another profession's column. Built here rather than read from the shipped
     * data so the test does not depend on data staging.
     */
    private static RankSystem twoRungProfessionLadder(Profession profession, int firstRung,
          int secondRung, int size) {
        RankSystem rankSystem = mock(RankSystem.class);
        List<Rank> ranks = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            Rank rank = mock(Rank.class);
            boolean namedForProfession = (index == firstRung) || (index == secondRung);
            when(rank.isEmpty(profession)).thenReturn(!namedForProfession);
            when(rank.indicatesAlternativeSystem(profession)).thenReturn(false);
            ranks.add(rank);
        }
        when(rankSystem.getRanks()).thenReturn(ranks);
        for (int index = 0; index < size; index++) {
            when(rankSystem.getRank(index)).thenReturn(ranks.get(index));
        }
        return rankSystem;
    }

    private static Campaign campaignCommandedAtRank(int commanderRank) {
        Campaign campaign = mock(Campaign.class);
        Person commander = mock(Person.class);
        when(commander.getRankNumeric()).thenReturn(commanderRank);
        LocalPersonnel roster = new LocalPersonnel();
        roster.put(UUID.randomUUID(), commander);
        stubPersonnel(campaign, roster);
        return campaign;
    }

    @Test
    void aDepartmentHeadStepsUpWithinTheirOwnProfessionsRanks() {
        Campaign campaign = campaignCommandedAtRank(COLONEL);
        RankSystem ladder = twoRungProfessionLadder(Profession.TECH, 4, 5, 20);

        Person bestTechnician = junior(PersonnelRole.MEK_TECH);
        withSkill(bestTechnician, SkillType.S_TECH_MEK, 8);
        when(bestTechnician.getRankNumeric()).thenReturn(4);
        when(bestTechnician.getRankSystem()).thenReturn(ladder);

        Person otherTechnician = junior(PersonnelRole.MEK_TECH);
        withSkill(otherTechnician, SkillType.S_TECH_MEK, 2);
        when(otherTechnician.getRankNumeric()).thenReturn(4);
        when(otherTechnician.getRankSystem()).thenReturn(ladder);

        SeniorAppointmentAssigner.assign(campaign, List.of(bestTechnician, otherTechnician));

        verify(bestTechnician).setDepartmentHead(true);
        // This technician heads both their department and the technical branch, so the promotion is
        // computed twice; every promotion must land on the one rung above them, never past it.
        ArgumentCaptor<Integer> promotedTo = ArgumentCaptor.forClass(Integer.class);
        verify(bestTechnician, atLeastOnce()).setRank(promotedTo.capture());
        for (Integer rank : promotedTo.getAllValues()) {
            assertEquals(5, rank, "a technician should step to the technician rung, not past it");
        }
    }

    @Test
    void aHeadNeverStepsIntoAnotherProfessionsRanks() {
        // The Clan system names two technician rungs and warrior ranks above them. Walking past the top
        // technician rung would retitle a technician as a warrior, so the search has to stop.
        Campaign campaign = campaignCommandedAtRank(COLONEL);
        RankSystem ladder = twoRungProfessionLadder(Profession.TECH, 4, 5, 20);

        Person bestTechnician = junior(PersonnelRole.MEK_TECH);
        withSkill(bestTechnician, SkillType.S_TECH_MEK, 8);
        when(bestTechnician.getRankNumeric()).thenReturn(5);
        when(bestTechnician.getRankSystem()).thenReturn(ladder);

        Person otherTechnician = junior(PersonnelRole.MEK_TECH);
        withSkill(otherTechnician, SkillType.S_TECH_MEK, 2);
        when(otherTechnician.getRankNumeric()).thenReturn(5);
        when(otherTechnician.getRankSystem()).thenReturn(ladder);

        SeniorAppointmentAssigner.assign(campaign, List.of(bestTechnician, otherTechnician));

        verify(bestTechnician).setDepartmentHead(true);
        verify(bestTechnician, never()).setRank(anyInt());
    }
}
