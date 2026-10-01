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

import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * What the tech list tells a player about each tech for the selected task (issue #10220).
 *
 * <p>With a full AsTech team a Regular tech has a target number of 3 on a damaged medium laser and 6 on a damaged
 * actuator, which takes 120 minutes. A roll of 3 or better on 2d6 comes up 35 times in 36 (97%); 6 or better, 26 in
 * 36 (72%).</p>
 */
class TechTaskEstimateTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int ACTUATOR_REPAIR_MINUTES = 120;

    private Campaign campaign;
    private Unit locust;
    private Person tech;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        tech = scenario.withTech(EXP_REGULAR);
    }

    private EquipmentPart damagedMediumLaser() {
        EquipmentPart laser = null;
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                laser = equipmentPart;
            }
        }
        assertNotNull(laser, "The Locust LCT-1V fixture has a Medium Laser");
        laser.setHits(1);
        return laser;
    }

    private MekActuator damagedActuator() {
        MekActuator actuator = PartsScenario.unitParts(locust, MekActuator.class).getFirst();
        actuator.setHits(1);
        return actuator;
    }

    @Test
    void anEasyShortJobShowsItsTargetOddsAndFinishesToday() {
        EquipmentPart laser = damagedMediumLaser();

        TechTaskEstimate estimate = TechTaskEstimate.estimate(campaign, laser, tech);

        assertFalse(estimate.isImpossible());
        assertEquals(3, estimate.targetNumber());
        assertEquals(97, estimate.successPercent());
        assertEquals(laser.getTimeLeft(), estimate.minutesNeeded());
        assertEquals(0, estimate.daysToFinish());
    }

    @Test
    void aJobLongerThanTheTechsDayRunsIntoTomorrow() {
        MekActuator actuator = damagedActuator();
        tech.setMinutesLeft(30);

        TechTaskEstimate estimate = TechTaskEstimate.estimate(campaign, actuator, tech);

        assertEquals(6, estimate.targetNumber());
        assertEquals(72, estimate.successPercent());
        assertEquals(ACTUATOR_REPAIR_MINUTES, estimate.minutesNeeded());
        assertEquals(1, estimate.daysToFinish(), "30 minutes today, the other 90 tomorrow");
    }

    @Test
    void overtimeCountsTowardsFinishingToday() {
        MekActuator actuator = damagedActuator();
        tech.setMinutesLeft(30);
        campaign.setOvertime(true);

        TechTaskEstimate estimate = TechTaskEstimate.estimate(campaign, actuator, tech);

        assertEquals(0, estimate.daysToFinish(), "30 minutes plus overtime covers the 120");
    }

    @Test
    void aTaskBeyondEveryTechIsShownAsImpossibleWithItsReason() {
        EquipmentPart laser = damagedMediumLaser();
        laser.setSkillMin(EXP_LEGENDARY + 1);

        TechTaskEstimate estimate = TechTaskEstimate.estimate(campaign, laser, tech);

        assertTrue(estimate.isImpossible());
        assertEquals(0, estimate.successPercent());
        assertFalse(estimate.targetDetails().isBlank(), "The reason is given");
    }

    @Test
    void estimatingLeavesTheTaskUnassigned() {
        EquipmentPart laser = damagedMediumLaser();

        TechTaskEstimate.estimate(campaign, laser, tech);

        assertNull(laser.getTech());
    }
}
