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

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A tech mothballing one unit used to stop maintaining every other unit they look after until the mothballing was
 * done (issue #5855). Maintenance comes first each day, and the mothballing uses only the minutes left over, as a refit
 * does. A Locust LCT-1V takes 180 minutes to maintain.
 */
class MaintenanceWhileMothballingTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int FULL_SHIFT_MINUTES = 480;
    private static final int LOCUST_MAINTENANCE_MINUTES = 180;
    private static final int DAILY_CALLS_BEFORE_THE_CYCLE_IS_DUE = 27;
    private static final int LONG_MOTHBALL_JOB_MINUTES = 2000;

    private final PartsScenario scenario = PartsScenario.create();
    private final Campaign campaign = scenario.getCampaign();
    private Unit maintainedLocust;
    private Unit mothballingLocust;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        tech = scenario.withTech(EXP_REGULAR);
        maintainedLocust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        mothballingLocust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        maintainedLocust.setTech(tech);
        mothballingLocust.setTech(tech);
        mothballingLocust.setMothballTime(LONG_MOTHBALL_JOB_MINUTES);
        assertTrue(tech.isMothballing(), "The tech is mothballing the second Locust");
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private void runOneMaintenanceCycle() {
        for (int day = 0; day < DAILY_CALLS_BEFORE_THE_CYCLE_IS_DUE; day++) {
            Maintenance.doMaintenance(campaign, maintainedLocust);
        }
        FixedDieRolls.everyDieShows(6);
        Maintenance.doMaintenance(campaign, maintainedLocust);
    }

    @Test
    void theOtherUnitIsStillMaintained() {
        runOneMaintenanceCycle();

        assertEquals(FULL_SHIFT_MINUTES - LOCUST_MAINTENANCE_MINUTES, tech.getMinutesLeft(),
              "The tech spent the maintenance time on the first Locust");
    }

    @Test
    void theMothballingUsesTheMinutesLeftOver() {
        runOneMaintenanceCycle();

        campaign.workOnMothballingOrActivation(mothballingLocust);

        assertEquals(0, tech.getMinutesLeft());
        assertEquals(LONG_MOTHBALL_JOB_MINUTES - (FULL_SHIFT_MINUTES - LOCUST_MAINTENANCE_MINUTES),
              mothballingLocust.getMothballTime());
    }
}
