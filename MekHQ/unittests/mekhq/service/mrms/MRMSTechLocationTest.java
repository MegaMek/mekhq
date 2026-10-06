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
package mekhq.service.mrms;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.location.LocationNode.LocationManager;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Mass Repair only assigns techs who are where the task is. A tech at another base would be refused by the repair
 * itself, and picking them again on every pass froze the game (issue #10214).
 */
class MRMSTechLocationTest {
    private static final Duration FREEZE_LIMIT = Duration.ofSeconds(10);

    private Campaign campaign;
    private Unit locust;
    private Person tech;
    private EquipmentPart damagedLaser;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, true);
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        tech = scenario.withTech(EXP_REGULAR);
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                damagedLaser = equipmentPart;
            }
        }
        assertNotNull(damagedLaser, "The Locust LCT-1V fixture has a Medium Laser");
        damagedLaser.setHits(1);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private void runMassRepair() {
        FixedDieRolls.everyDieShows(6);
        assertTimeoutPreemptively(FREEZE_LIMIT,
              () -> MRMSService.mrmsUnits(campaign, List.of(locust), new MRMSConfiguredOptions(campaign)));
    }

    @Test
    void aTechAtAnotherBaseIsNotAssignedAndMassRepairFinishes() {
        PlayerBase otherBase = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        LocationManager.setLocation(tech, otherBase);
        int minutesBefore = tech.getMinutesLeft();

        runMassRepair();

        assertEquals(1, damagedLaser.getHits(), "Nobody at the Locust's location could repair it");
        assertEquals(minutesBefore, tech.getMinutesLeft(), "The tech elsewhere spends no time");
    }

    @Test
    void aTechAtTheUnitsLocationRepairsIt() {
        runMassRepair();

        assertEquals(0, damagedLaser.getHits());
    }
}
