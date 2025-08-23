/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_GLASS_JAW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.BloodmarkLevel;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class BloodmarkTest {
    final static LocalDate CURRENT_DATE = LocalDate.of(3151, 1, 1);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Person target;

    @BeforeEach
    void beforeEach() {
        campaign = mock(Campaign.class);
        campaignOptions = mock(CampaignOptions.class);
        Faction campaignFaction = mock(Faction.class);
        Hangar campaignHangar = mock(Hangar.class);
        Warehouse campaignWarehouse = mock(Warehouse.class);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getFaction()).thenReturn(campaignFaction);
        when(campaignFaction.getShortName()).thenReturn("MERC");
        when(campaign.getHangar()).thenReturn(campaignHangar);
        when(campaign.getWarehouse()).thenReturn(campaignWarehouse);

        target = new Person(campaign);
    }

    @Test
    void testGetBloodhuntSchedule_NoBloodhuntDueToZeroFrequency() {
        BloodmarkLevel level = BloodmarkLevel.BLOODMARK_ZERO;

        List<LocalDate> result = Bloodmark.getBloodhuntSchedule(level, CURRENT_DATE, false);

        assertTrue(result.isEmpty(), "Expected no assassination schedule for BLOODMARK_ZERO level.");
    }

    @Test
    void testGetBloodhuntSchedule_BloodhuntSkippedByRoll() {
        BloodmarkLevel level = BloodmarkLevel.BLOODMARK_ONE;

        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.randomInt(level.getRollFrequency())).thenReturn(1);

            List<LocalDate> result = Bloodmark.getBloodhuntSchedule(level, CURRENT_DATE, false);

            assertTrue(result.isEmpty(), "Expected no assassination schedule when the bloodhunt roll is not zero.");
        }
    }

    @Test
    void testGetBloodhuntSchedule_NoAssassinationsDueToZeroDivisor() {
        BloodmarkLevel level = BloodmarkLevel.BLOODMARK_ONE;

        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.randomInt(level.getRollFrequency())).thenReturn(0);
            mockedCompute.when(() -> Compute.d6(1)).thenReturn(0); // Divisor should prevent attempts

            List<LocalDate> result = Bloodmark.getBloodhuntSchedule(level, CURRENT_DATE, false);

            assertTrue(result.isEmpty(), "Expected no assassination schedule due to zero divisor.");
        }
    }

    @Test
    void testGetBloodhuntSchedule_SingleAssassination() {
        BloodmarkLevel level = BloodmarkLevel.BLOODMARK_TWO;

        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.randomInt(level.getRollFrequency())).thenReturn(0);
            mockedCompute.when(() -> Compute.d6(1)).thenReturn(2, 3); // One attempt with 3-day lag

            List<LocalDate> result = Bloodmark.getBloodhuntSchedule(level, CURRENT_DATE, false);

            assertEquals(1, result.size(), "Expected one assassination attempt.");
            assertEquals(CURRENT_DATE.plusDays(3), result.get(0), "Incorrect date for assassination attempt.");
        }
    }

    @Test
    void testGetBloodhuntSchedule_MultipleAssassinations() {
        BloodmarkLevel level = BloodmarkLevel.BLOODMARK_THREE;

        try (MockedStatic<Compute> mockedCompute = mockStatic(Compute.class)) {
            mockedCompute.when(() -> Compute.randomInt(level.getRollFrequency())).thenReturn(0);
            mockedCompute.when(() -> Compute.d6(1)).thenReturn(6, 2, 3); // 2 assassination attempts

            List<LocalDate> result = Bloodmark.getBloodhuntSchedule(level, CURRENT_DATE, false);

            assertEquals(2, result.size(), "Expected two assassination attempts.");
            assertEquals(CURRENT_DATE.plusDays(2), result.get(0), "Incorrect date for first assassination attempt.");
            assertEquals(CURRENT_DATE.plusDays(2 + 3),
                  result.get(1),
                  "Incorrect date for second assassination attempt.");
        }
    }

    @Test
    void testCheckForAssassinationAttempt_returnsFalseIfBloodhuntScheduleIsEmpty() {
        boolean result = Bloodmark.checkForAssassinationAttempt(target, CURRENT_DATE, true);

        assertFalse(result);
    }

    @Test
    void testCheckForAssassinationAttempt_returnsFalseIfBloodhuntScheduleDoesNotContainToday() {
        LocalDate futureDate = CURRENT_DATE.plusDays(1);
        target.addBloodhuntDate(futureDate);

        boolean result = Bloodmark.checkForAssassinationAttempt(target, CURRENT_DATE, true);

        // Future bloodhunts should remain in the schedule
        assertTrue(target.getBloodhuntSchedule().contains(futureDate));

        assertFalse(result);
    }

    @Test
    void testCheckForAssassinationAttempt_returnsFalseIfNotCampaignPlanetside() {
        target.addBloodhuntDate(CURRENT_DATE);
        target.setStatus(PersonnelStatus.ACTIVE);

        boolean result = Bloodmark.checkForAssassinationAttempt(target, CURRENT_DATE, false);

        // removeBloodhuntDate should be called even if later checks fail
        assertFalse(target.getBloodhuntSchedule().contains(CURRENT_DATE));

        assertFalse(result);
    }

    @Test
    void testCheckForAssassinationAttempt_returnsTrueIfNotCampaignPlanetsideButTargetIsAbsent() {
        target.addBloodhuntDate(CURRENT_DATE);
        target.setStatus(PersonnelStatus.ON_LEAVE);

        boolean result = Bloodmark.checkForAssassinationAttempt(target, CURRENT_DATE, false);

        // removeBloodhuntDate should be called even if later checks fail
        assertFalse(target.getBloodhuntSchedule().contains(CURRENT_DATE));

        assertTrue(result);
    }

    @Test
    void testCheckForAssassinationAttempt_returnsFalseIfTargetIsDeployed() {
        target.addBloodhuntDate(CURRENT_DATE);
        target.setStatus(PersonnelStatus.ACTIVE);

        Unit mockUnit = mock(Unit.class);
        target.setUnit(mockUnit);
        when(mockUnit.getScenarioId()).thenReturn(1);

        boolean result = Bloodmark.checkForAssassinationAttempt(target, CURRENT_DATE, true);

        // removeBloodhuntDate should be called even if later checks fail
        assertFalse(target.getBloodhuntSchedule().contains(CURRENT_DATE));

        assertFalse(result);
    }

    @Test
    void testCheckForAssassinationAttempt_returnsTrueWhenAllCriteriaMet() {
        target.addBloodhuntDate(CURRENT_DATE);
        target.setStatus(PersonnelStatus.ACTIVE);

        Unit mockUnit = mock(Unit.class);
        target.setUnit(mockUnit);
        when(mockUnit.getScenarioId()).thenReturn(-1);

        boolean result = Bloodmark.checkForAssassinationAttempt(target, CURRENT_DATE, true);

        // removeBloodhuntDate should be called even if later checks fail
        assertFalse(target.getBloodhuntSchedule().contains(CURRENT_DATE));

        assertTrue(result);
    }

    @Test
    void testProcessWounds_AdvancedMedicalEnabled_JustWounded() {
        when(campaignOptions.isUseAdvancedMedical()).thenReturn(true);

        // Due to the way AM works, it's not reasonable to fully test this process. So instead we're setting up
        // the test so that the character doesn't qualify for death when the 'injuries' would be applied. However,
        // the new injuries are set to 0 to avoid NPEs
        for (int i = 0; i < 3; i++) {
            target.addInjury(new Injury());
        }

        Bloodmark.processWounds(campaign, target, CURRENT_DATE, 0);

        assertFalse(target.getStatus().isDead());
    }

    @Test
    void testProcessWounds_AdvancedMedicalEnabled_Killed() {
        when(campaignOptions.isUseAdvancedMedical()).thenReturn(true);

        // Due to the way AM works, it's not reasonable to fully test this process. So instead we're setting up
        // the test so that the character qualifies for death when the 'injuries' would be applied. However, the new
        // injuries are set to 0 to avoid NPEs
        for (int i = 0; i < 6; i++) {
            target.addInjury(new Injury());
        }

        Bloodmark.processWounds(campaign, target, CURRENT_DATE, 0);

        assertTrue(target.getStatus().isDead());
    }

    @Test
    void testProcessWounds_AdvancedMedicalDisabled_JustWounded() {
        when(campaignOptions.isUseAdvancedMedical()).thenReturn(false);
        target.setHits(0);

        Bloodmark.processWounds(campaign, target, CURRENT_DATE, 2);

        assertFalse(target.getStatus().isDead());
    }

    @Test
    void testProcessWounds_AdvancedMedicalDisabled_KilledDueToPriorHits() {
        when(campaignOptions.isUseAdvancedMedical()).thenReturn(false);
        target.setHits(4);

        Bloodmark.processWounds(campaign, target, CURRENT_DATE, 3);

        assertTrue(target.getStatus().isDead());
    }

    @Test
    void testProcessWounds_AdvancedMedicalDisabled_KilledFromNewHits() {
        when(campaignOptions.isUseAdvancedMedical()).thenReturn(false);
        target.setHits(0);

        Bloodmark.processWounds(campaign, target, CURRENT_DATE, 6);

        assertTrue(target.getStatus().isDead());
    }

    @Test
    void testAdjustmentWoundsForSPAs_NoSPAs_WoundsUnchanged() {
        assertEquals(3, Bloodmark.adjustmentWoundsForSPAs(target, 3));
    }

    @Test
    void testAdjustmentWoundsForSPAs_GlassJawOnly_WoundsDoubled() {
        PersonnelOptions options = target.getOptions();
        options.acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, FLAW_GLASS_JAW, true);
        assertEquals(8, Bloodmark.adjustmentWoundsForSPAs(target, 4));
    }

    @Test
    void testAdjustmentWoundsForSPAs_ToughnessOnly_WoundsReduced() {
        PersonnelOptions options = target.getOptions();
        options.acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, ATOW_TOUGHNESS, true);
        assertEquals(2, Bloodmark.adjustmentWoundsForSPAs(target, 2));
        assertEquals(4, Bloodmark.adjustmentWoundsForSPAs(target, 5)); // 5*.75=3.75->4
    }
}
