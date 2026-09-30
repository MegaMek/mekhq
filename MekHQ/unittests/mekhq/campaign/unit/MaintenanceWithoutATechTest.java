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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.Duration;

import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.CampaignOption;
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
 * A unit whose tech cannot do the maintenance is checked as unmaintained, not with that tech's skill, and immediate
 * maintenance cannot hang when maintenance checks are off (issue #10211).
 */
class MaintenanceWithoutATechTest {
    private static final int DAILY_CALLS_TO_DUE = 28;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
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

    @Test
    void aTechAtAnotherBaseDoesNotMakeTheCheck() {
        PlayerBase otherBase = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        LocationManager.setLocation(tech, otherBase);
        int minutesBefore = tech.getMinutesLeft();

        runUntilMaintenanceIsDone();

        String report = locust.getLastMaintenanceReport();
        assertTrue(report.contains("Nobody performing maintenance"), report);
        assertTrue(report.contains("Unmaintained"), "The unmaintained target is used, not the Elite tech's skill");
        assertEquals(minutesBefore, tech.getMinutesLeft(), "A tech who is away spends no time");
    }

    @Test
    void aTechWhoCanDoTheWorkMakesTheCheckWithTheirOwnSkill() {
        runUntilMaintenanceIsDone();

        String report = locust.getLastMaintenanceReport();
        assertTrue(report.contains(tech.getFullTitle() + " performing maintenance"), report);
        assertFalse(report.contains("Unmaintained"), report);
    }

    @Test
    void immediateMaintenanceWithChecksOffReturnsAtOnce() {
        Maintenance.doMaintenance(campaign, locust);
        assertTrue(locust.getDaysSinceMaintenance() > 0, "The unit is part way through its cycle");
        campaign.getCampaignOptions().set(CampaignOption.CHECK_MAINTENANCE, false);

        assertTimeoutPreemptively(Duration.ofSeconds(5),
              () -> Maintenance.performImmediateMaintenance(campaign, locust));
    }
}
