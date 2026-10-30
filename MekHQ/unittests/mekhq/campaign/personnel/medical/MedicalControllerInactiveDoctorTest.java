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
package mekhq.campaign.personnel.medical;

import static mekhq.campaign.enums.DailyReportType.MEDICAL;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests that the daily medical step releases patients whose doctor is no longer active. That used to happen only when
 * the Infirmary tab redrew, so a multi-day advance or a detached Infirmary tab could leave patients with a doctor who
 * had died or left.
 */
class MedicalControllerInactiveDoctorTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 6, 1);

    private Campaign campaign;
    private ForceHumanResources humanResources;
    private int naturalHealingWaitingPeriod;

    @BeforeEach
    void beforeEach() {
        CampaignOptions campaignOptions = new CampaignOptions();
        naturalHealingWaitingPeriod = campaignOptions.get(CampaignOption.NATURAL_HEALING_WAITING_PERIOD);

        campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        humanResources = mock(ForceHumanResources.class);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelStatus.class, names = { "KIA", "RETIRED", "AWOL", "MIA" })
    void patientOfInactiveDoctorIsReleased(PersonnelStatus doctorStatus) {
        Person doctor = doctorWithStatus(doctorStatus);
        Person patient = patientOf(doctor);

        new MedicalController(campaign).processMedicalEvents(patient, false, false, TODAY);

        verify(patient).setDoctorId(null, naturalHealingWaitingPeriod);
        verify(campaign).addReport(eq(MEDICAL), anyString());
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelStatus.class, names = { "ACTIVE", "CAMP_FOLLOWER" })
    void patientOfActiveDoctorKeepsThem(PersonnelStatus doctorStatus) {
        Person doctor = doctorWithStatus(doctorStatus);
        Person patient = patientOf(doctor);

        new MedicalController(campaign).processMedicalEvents(patient, false, false, TODAY);

        verify(patient, never()).setDoctorId(isNull(), anyInt());
    }

    /**
     * @param status the doctor's personnel status
     *
     * @return a qualified doctor with spare capacity, registered with the campaign
     */
    private Person doctorWithStatus(PersonnelStatus status) {
        Person doctor = mock(Person.class);
        UUID doctorId = UUID.randomUUID();
        when(doctor.getId()).thenReturn(doctorId);
        when(doctor.getStatus()).thenReturn(status);
        when(doctor.isDoctor()).thenReturn(true);
        when(doctor.getDoctorMedicalCapacity(anyBoolean(), anyInt())).thenReturn(25);
        when(doctor.getHyperlinkedFullTitle()).thenReturn("Doctor");
        when(humanResources.getPerson(doctorId)).thenReturn(doctor);
        return doctor;
    }

    /**
     * @param doctor the doctor treating the patient
     *
     * @return a patient assigned to {@code doctor} who needs no further healing today
     */
    private Person patientOf(Person doctor) {
        Person patient = mock(Person.class);
        UUID doctorId = doctor.getId();
        when(patient.getDoctorId()).thenReturn(doctorId);
        when(patient.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(patient.getHyperlinkedFullTitle()).thenReturn("Patient");
        return patient;
    }
}
