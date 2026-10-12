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

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.LocationNode.LocationManager;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A maintenance check that goes wrong posts a notice to the General tab of the daily log; a routine check posts
 * nothing there. The full report stays on the Technical tab either way.
 */
class MaintenanceGeneralNoticeTest {
    private static final int DAILY_CALLS_TO_DUE = 28;
    private static final String NOTICE_MARKER = "Maintenance problem:";

    private Campaign campaign;
    private Unit locust;
    private Person tech;

    @BeforeEach
    void setUp() {
        // With the daily log tabs disabled everything lands on General anyway, so there is nothing to test. This is a
        // user preference, so it is read rather than changed.
        assumeFalse(MekHQ.getMHQOptions().getUnifiedDailyReport(), "Daily report tabs are disabled on this machine");

        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        tech = scenario.withTech(EXP_ELITE);
        locust.setTech(tech);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private void runUntilMaintenanceIsDone() {
        FixedDieRolls.everyDieShows(4);
        for (int day = 0; day < DAILY_CALLS_TO_DUE; day++) {
            Maintenance.doMaintenance(campaign, locust);
        }
        assertEquals(0.0, locust.getDaysSinceMaintenance(), "The maintenance cycle came due and was checked");
    }

    private String generalTab() {
        return String.join("\n", campaign.getDailyReportLog().getLines(GENERAL));
    }

    @Test
    void aTechAwayFromTheUnitIsReportedOnTheGeneralTab() {
        PlayerBase otherBase = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        LocationManager.setLocation(tech, otherBase);

        runUntilMaintenanceIsDone();

        String generalTab = generalTab();
        assertTrue(generalTab.contains(NOTICE_MARKER), generalTab);
        assertTrue(generalTab.contains("the assigned tech is at a different location"), generalTab);
    }

    @Test
    void aRoutineCheckPostsNothingToTheGeneralTab() {
        runUntilMaintenanceIsDone();

        String generalTab = generalTab();
        assertFalse(generalTab.contains(NOTICE_MARKER), generalTab);
    }
}
