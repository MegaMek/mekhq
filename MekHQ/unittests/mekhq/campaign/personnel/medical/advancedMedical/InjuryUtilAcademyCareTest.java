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
package mekhq.campaign.personnel.medical.advancedMedical;

import static mekhq.campaign.personnel.medical.MedicalController.ACADEMY_MEDICAL_CARE_TARGET_NUMBER;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests that academy medical staff treat a student's injuries under Advanced Medical, so they are not left with the
 * permanent-injury risk of untreated injuries, and that a failed treatment never worsens an injury.
 */
class InjuryUtilAcademyCareTest {
    private static final int HEAL_WAITING_PERIOD = 1;
    private static final int INJURY_TIME = 5;

    private Campaign campaign;
    private Person student;
    private Injury injury;

    @BeforeEach
    void beforeEach() {
        CampaignOptions campaignOptions = mock(CampaignOptions.class);
        when(campaignOptions.get(CampaignOption.HEAL_WAITING_PERIOD)).thenReturn(HEAL_WAITING_PERIOD);
        campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

        injury = mock(Injury.class);
        when(injury.getTime()).thenReturn(INJURY_TIME);
        when(injury.getName()).thenReturn("Broken arm");

        student = mock(Person.class);
        when(student.getInjuries()).thenReturn(List.of(injury));
        when(student.getHyperlinkedName()).thenReturn("Student");
    }

    @Test
    void successfulRoll_marksInjuryTreated() {
        resolveWithRoll(ACADEMY_MEDICAL_CARE_TARGET_NUMBER);

        verify(injury).setWorkedOn(true);
        verify(student).setDaysToWaitForHealing(HEAL_WAITING_PERIOD);
        verify(injury).setTime(INJURY_TIME - 1);
        verify(student).decrementDaysToWaitForHealing();
    }

    @Test
    void failedRoll_leavesInjuryUntreatedWithoutWorsening() {
        resolveWithRoll(ACADEMY_MEDICAL_CARE_TARGET_NUMBER - 1);

        verify(injury, never()).setWorkedOn(true);
        verify(student).setDaysToWaitForHealing(HEAL_WAITING_PERIOD);
        verify(injury, times(1)).setTime(anyInt());
        verify(injury).setTime(INJURY_TIME - 1);
    }

    @Test
    void waitingPeriodRunning_noTreatmentRoll() {
        when(student.getDaysToWaitForHealing()).thenReturn(2);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            InjuryUtil.resolveDailyHealingUnderAcademyCare(campaign, student);

            compute.verify(() -> Compute.d6(anyInt()), never());
        }

        verify(injury, never()).setWorkedOn(true);
        verify(injury).setTime(INJURY_TIME - 1);
    }

    @Test
    void treatedInjury_isNotRolledAgain() {
        when(injury.isWorkedOn()).thenReturn(true);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            InjuryUtil.resolveDailyHealingUnderAcademyCare(campaign, student);

            compute.verify(() -> Compute.d6(anyInt()), never());
        }

        verify(student, never()).setDaysToWaitForHealing(anyInt());
    }

    private void resolveWithRoll(int roll) {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(roll);

            InjuryUtil.resolveDailyHealingUnderAcademyCare(campaign, student);
        }
    }
}
