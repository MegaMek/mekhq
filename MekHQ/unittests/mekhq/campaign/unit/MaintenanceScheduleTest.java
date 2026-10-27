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
package mekhq.campaign.unit;

import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A tech whose units need more minutes than one day holds, but fewer than a cycle holds, keeps every unit maintained:
 * the daily schedule check does one of them early instead of letting both fall due on the same day (#10654).
 */
class MaintenanceScheduleTest {
    private static final int CYCLE_DAYS = 7;
    /** Off contract the cycle advances at a quarter of a day per day, so a full cycle takes four times as long */
    private static final int DAYS_TO_RUN = CYCLE_DAYS * 4 * 3;

    private Campaign campaign;
    private Person tech;
    private Unit wolverine;
    private Unit longbow;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        campaignOptions.set(CampaignOption.CHECK_MAINTENANCE, true);
        campaignOptions.set(CampaignOption.MAINTENANCE_CYCLE_DAYS, CYCLE_DAYS);
        campaignOptions.set(CampaignOption.TECHS_USE_ADMINISTRATION, false);

        tech = scenario.withTech(EXP_ELITE);
        // At x4 time a medium Mek takes 240 minutes and an assault Mek 360: 600 together, more than a 480 minute day
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        longbow = scenario.withUnit(UnitFixture.LONGBOW_LGB_8C);
        for (Unit unit : List.of(wolverine, longbow)) {
            unit.setMaintenanceMultiplier(4);
            unit.setTech(tech);
        }
    }

    /** One new day as the campaign runs it: minutes reset, the schedule is checked, then each unit's maintenance */
    private int runOneDay() {
        int missedChecks = 0;
        tech.resetMinutesLeft(false);
        Maintenance.checkAndCorrectMaintenanceSchedule(campaign);
        for (Unit unit : List.of(wolverine, longbow)) {
            double daysBefore = unit.getDaysSinceMaintenance();
            Maintenance.doMaintenance(campaign, unit);
            boolean wasChecked = unit.getDaysSinceMaintenance() < daysBefore;
            if (wasChecked && unit.getLastMaintenanceReport().contains("Nobody")) {
                missedChecks++;
            }
        }
        return missedChecks;
    }

    @Test
    void unitsFallingDueTogetherAreStaggeredSoNeitherIsMissed() {
        assertEquals(480, tech.getDailyAvailableTechTime(false), "The tech has one full day of minutes");
        assertTrue(wolverine.getMaintenanceTime() + longbow.getMaintenanceTime() > 480,
              "Both units together need more than one day of the tech's minutes");

        int missedChecks = 0;
        for (int day = 0; day < DAYS_TO_RUN; day++) {
            missedChecks += runOneDay();
        }

        assertEquals(0, missedChecks, "Every maintenance check is made by the tech");
    }

    /** On a combat contract, Cadre Duty among them, the cycle runs at a full day per day (#10654) */
    @Test
    void unitsFallingDueTogetherOnACombatContractAreStaggeredSoNeitherIsMissed() {
        try (MockedStatic<Maintenance> maintenance = mockStatic(Maintenance.class, CALLS_REAL_METHODS)) {
            maintenance.when(() -> Maintenance.isOnCombatContract(any())).thenReturn(true);
            assertEquals(1.0, Maintenance.getDailyMaintenanceProgress(campaign), "The cycle runs at full pace");

            int missedChecks = 0;
            for (int day = 0; day < CYCLE_DAYS * 3; day++) {
                missedChecks += runOneDay();
            }

            assertEquals(0, missedChecks, "Every maintenance check is made by the tech");
        }
    }

    @Test
    void bothUnitsAreStillCheckedEveryCycle() {
        int checksBeforeEnd = 0;
        for (int day = 0; day < DAYS_TO_RUN; day++) {
            double wolverineBefore = wolverine.getDaysSinceMaintenance();
            double longbowBefore = longbow.getDaysSinceMaintenance();
            runOneDay();
            if (wolverine.getDaysSinceMaintenance() < wolverineBefore) {
                checksBeforeEnd++;
            }
            if (longbow.getDaysSinceMaintenance() < longbowBefore) {
                checksBeforeEnd++;
            }
        }

        assertTrue(checksBeforeEnd >= 4, "Each unit is checked at least twice in three cycles, got " + checksBeforeEnd);
    }
}
