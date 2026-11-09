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
package mekhq.campaign;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that Optimize Assignments gives the most injured patients to the best surgeons. Previously doctors were ranked
 * by their role experience level, which averages in Administration when doctors use it, so a Surgery 1 doctor with
 * good Administration was assigned patients before a Surgery 3 doctor.
 */
class OptimizeInfirmaryAssignmentsTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 6, 1);

    private Campaign campaign;
    private ForceHumanResources humanResources;
    private int healingWaitingPeriod;

    @BeforeEach
    void beforeEach() {
        CampaignOptions campaignOptions = new CampaignOptions();
        healingWaitingPeriod = campaignOptions.get(CampaignOption.HEAL_WAITING_PERIOD);

        campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.isOnContractAndPlanetside()).thenReturn(false);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        humanResources = mock(ForceHumanResources.class);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
    }

    @Test
    void betterSurgeonIsAssignedFirstDespiteLowerExperienceLevel() {
        // Mirrors the bug report: Surgery 1 with strong Administration rates as the more experienced doctor
        Person weakSurgeon = doctor(1, EXP_VETERAN);
        Person strongSurgeon = doctor(3, EXP_REGULAR);
        Person severePatient = patient(5);
        Person minorPatient = patient(1);
        setUpInfirmary(List.of(weakSurgeon, strongSurgeon), List.of(minorPatient, severePatient));

        new OptimizeInfirmaryAssignments(campaign);

        verify(severePatient).setDoctorId(strongSurgeon.getId(), healingWaitingPeriod);
        verify(minorPatient).setDoctorId(weakSurgeon.getId(), healingWaitingPeriod);
    }

    @Test
    void equalSurgerySkillFallsBackToExperienceLevel() {
        Person lessExperiencedDoctor = doctor(2, EXP_REGULAR);
        Person moreExperiencedDoctor = doctor(2, EXP_VETERAN);
        Person severePatient = patient(5);
        Person minorPatient = patient(1);
        setUpInfirmary(List.of(lessExperiencedDoctor, moreExperiencedDoctor), List.of(minorPatient, severePatient));

        new OptimizeInfirmaryAssignments(campaign);

        verify(severePatient).setDoctorId(moreExperiencedDoctor.getId(), healingWaitingPeriod);
        verify(minorPatient).setDoctorId(lessExperiencedDoctor.getId(), healingWaitingPeriod);
    }

    @Test
    void doctorWithoutSurgeryIsAssignedLast() {
        Person unskilledDoctor = doctorWithoutSurgery(EXP_VETERAN);
        Person surgeon = doctor(0, EXP_REGULAR);
        Person severePatient = patient(5);
        Person minorPatient = patient(1);
        setUpInfirmary(List.of(unskilledDoctor, surgeon), List.of(minorPatient, severePatient));

        new OptimizeInfirmaryAssignments(campaign);

        verify(severePatient).setDoctorId(surgeon.getId(), healingWaitingPeriod);
        verify(minorPatient).setDoctorId(unskilledDoctor.getId(), healingWaitingPeriod);
    }

    private void setUpInfirmary(List<Person> doctors, List<Person> patients) {
        when(humanResources.getDoctors()).thenReturn(new ArrayList<>(doctors));
        when(humanResources.getPatients()).thenReturn(new ArrayList<>(patients));
    }

    /**
     * @param surgeryLevel    the doctor's total Surgery skill level
     * @param experienceLevel the doctor's role experience level
     *
     * @return a doctor who can take exactly one patient
     */
    private static Person doctor(int surgeryLevel, int experienceLevel) {
        Person doctor = doctorWithoutSurgery(experienceLevel);
        Skill surgerySkill = mock(Skill.class);
        when(surgerySkill.getTotalSkillLevel(any())).thenReturn(surgeryLevel);
        when(doctor.getSkill(S_SURGERY)).thenReturn(surgerySkill);
        return doctor;
    }

    /**
     * @param experienceLevel the doctor's role experience level
     *
     * @return a doctor with no Surgery skill who can take exactly one patient
     */
    private static Person doctorWithoutSurgery(int experienceLevel) {
        Person doctor = mock(Person.class);
        UUID doctorId = UUID.randomUUID();
        when(doctor.getId()).thenReturn(doctorId);
        when(doctor.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        when(doctor.getExperienceLevel(any(CampaignOptions.class), anyBoolean(), any(LocalDate.class), anyBoolean(),
              anyBoolean())).thenReturn(experienceLevel);
        when(doctor.getDoctorMedicalCapacity(anyBoolean(), anyInt())).thenReturn(1);
        return doctor;
    }

    /**
     * @param severity the patient's injury severity
     *
     * @return a free (non-prisoner) patient with the given injury severity
     */
    private static Person patient(int severity) {
        Person patient = mock(Person.class);
        when(patient.getNonPermanentInjurySeverity()).thenReturn(severity);
        when(patient.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        return patient;
    }
}
