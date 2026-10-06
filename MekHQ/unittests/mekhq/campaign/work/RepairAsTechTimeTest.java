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
package mekhq.campaign.work;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * AsTechs are charged for every day they help with a repair, not only the day it finishes, and a self-crewed unit is
 * repaired by its own crew without drawing on the AsTech pool (issue #10209).
 */
class RepairAsTechTimeTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int FULL_ASTECH_POOL_MINUTES = 2880;
    private static final int ACTUATOR_REPAIR_MINUTES = 120;
    private static final int MINUTES_LEFT_ON_THE_FIRST_DAY = 30;
    /** Every die shows 3, a roll of 6, which meets the damaged actuator's target of 6. */
    private static final int PASSING_DIE_FACE = 3;

    private PartsScenario scenario;
    private Campaign campaign;
    private ForceHumanResources humanResources;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        humanResources = campaign.getPlayerForce().getHumanResources();
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        tech = scenario.withTech(EXP_REGULAR);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private MekActuator damagedLocustActuator() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        MekActuator actuator = PartsScenario.unitParts(locust, MekActuator.class).getFirst();
        actuator.setHits(1);
        return actuator;
    }

    @Test
    void aPartialDayIsChargedForTheMinutesWorked() {
        MekActuator actuator = damagedLocustActuator();
        tech.setMinutesLeft(MINUTES_LEFT_ON_THE_FIRST_DAY);

        FixedDieRolls.everyDieShows(PASSING_DIE_FACE);
        campaign.fixPart(actuator, tech);

        assertEquals(1, actuator.getHits(), "The repair carries over to tomorrow");
        assertEquals(FULL_ASTECH_POOL_MINUTES - (MINUTES_LEFT_ON_THE_FIRST_DAY * FULL_ASTECH_TEAM),
              humanResources.getAsTechPoolMinutes());
    }

    @Test
    void aJobSpreadOverTwoDaysIsChargedForItsWholeTime() {
        MekActuator actuator = damagedLocustActuator();
        tech.setMinutesLeft(MINUTES_LEFT_ON_THE_FIRST_DAY);
        FixedDieRolls.everyDieShows(PASSING_DIE_FACE);
        campaign.fixPart(actuator, tech);

        tech.resetMinutesLeft(false);
        campaign.fixPart(actuator, tech);

        assertEquals(0, actuator.getHits(), "The repair is finished on the second day");
        assertEquals(FULL_ASTECH_POOL_MINUTES - (ACTUATOR_REPAIR_MINUTES * FULL_ASTECH_TEAM),
              humanResources.getAsTechPoolMinutes(), "The AsTechs are charged once for each minute of the job");
    }

    @Test
    void aSelfCrewedDropShipIsRepairedWithoutTheAsTechPool() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        assertTrue(leopard.isSelfCrewed(), "A DropShip is repaired by its own crew");
        Part dropShipPart = PartsScenario.unitParts(leopard, Part.class).getFirst();
        RepairAsTechTime asTechTime = new RepairAsTechTime(humanResources, campaign.isOvertimeAllowed(),
              campaign.getCampaignOptions());

        int helpers = asTechTime.chargeForMinutesWorked(dropShipPart, ACTUATOR_REPAIR_MINUTES, false);

        assertEquals(0, helpers);
        assertEquals(FULL_ASTECH_POOL_MINUTES, humanResources.getAsTechPoolMinutes());
    }
}
