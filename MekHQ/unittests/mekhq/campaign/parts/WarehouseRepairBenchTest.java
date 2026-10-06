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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.LocationNode.LocationManager;
import mekhq.campaign.location.LocationUtils;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Repairs started from the Warehouse tab: filling an empty OmniPod uses any spare of its equipment and keeps the pod
 * until it is filled, and a tech at a base can repair that base's spares (issue #10210).
 */
class WarehouseRepairBenchTest {
    private static final int SUCCESSFUL_FACE = 6;

    private PartsScenario scenario;
    private Campaign campaign;
    private Person tech;
    private EquipmentPart mediumLaser;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        tech = scenario.withTech(SkillType.EXP_REGULAR);
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        for (EquipmentPart equipment : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if (equipment.getName().equals("Medium Laser")) {
                mediumLaser = equipment;
            }
        }
        assertNotNull(mediumLaser, "The Locust carries a medium laser");
    }

    @AfterEach
    void tearDown() {
        FixedDieRolls.restore();
    }

    private OmniPod emptyMediumLaserPodInStock() {
        OmniPod pod = new OmniPod(mediumLaser.clone(), campaign);
        scenario.withSpare(pod, 1);
        for (Part spare : scenario.getSpareParts()) {
            if (spare instanceof OmniPod podInStock) {
                return podInStock;
            }
        }
        throw new IllegalStateException("The pod did not reach the warehouse");
    }

    private int podsInStock() {
        int pods = 0;
        for (Part spare : scenario.getSpareParts()) {
            if (spare instanceof OmniPod) {
                pods += spare.getQuantity();
            }
        }
        return pods;
    }

    @Test
    void aSpareOfAnyQualityIsPoddedAndKeepsItsQuality() {
        EquipmentPart spareLaser = (EquipmentPart) mediumLaser.clone();
        spareLaser.setQuality(PartQuality.QUALITY_C);
        scenario.withSpare(spareLaser, 1);
        OmniPod pod = emptyMediumLaserPodInStock();
        FixedDieRolls.everyDieShows(SUCCESSFUL_FACE);

        campaign.fixWarehousePart(pod, tech);

        EquipmentPart poddedLaser = null;
        for (Part spare : scenario.getSpareParts()) {
            if ((spare instanceof EquipmentPart laser) && laser.isOmniPodded()) {
                poddedLaser = laser;
            }
        }
        assertNotNull(poddedLaser, "The quality C laser is now podded");
        assertEquals(PartQuality.QUALITY_C, poddedLaser.getQuality());
        assertEquals(0, podsInStock(), "The pod was used to pod the laser");
    }

    @Test
    void aPodJobThatRunsPastTodayKeepsThePod() {
        scenario.withSpare(mediumLaser.clone(), 1);
        OmniPod pod = emptyMediumLaserPodInStock();
        tech.setMinutesLeft(1);

        campaign.fixWarehousePart(pod, tech);

        assertEquals(1, podsInStock(), "The pod waits in the warehouse for the job to carry on");
    }

    @Test
    void aPodWithNoSpareToFillItIsNotLost() {
        OmniPod pod = emptyMediumLaserPodInStock();
        FixedDieRolls.everyDieShows(SUCCESSFUL_FACE);

        campaign.fixWarehousePart(pod, tech);

        assertEquals(1, podsInStock());
    }

    @Test
    void aTechAtABaseRepairsThatBasesSpare() {
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        LocationManager.setLocation(tech, base);
        EquipmentPart damagedSpare = (EquipmentPart) mediumLaser.clone();
        damagedSpare.setHits(1);
        base.getBaseWarehouse().addPart(damagedSpare, true);
        LocationManager.setLocation(damagedSpare, base.getBaseWarehouse());
        FixedDieRolls.everyDieShows(SUCCESSFUL_FACE);

        Part repaired = campaign.fixWarehousePart(damagedSpare, tech);

        assertEquals(0, repaired.getHits(), "The base tech repaired the base's spare");
        assertSame(base, LocationUtils.findEffectiveBase(repaired));
    }

    @Test
    void podSpaceIsWhereItsUnitIs() {
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        Unit epona = scenario.withUnit(UnitFixture.EPONA_PURSUIT_TANK_PRIME);
        LocationManager.setLocation(epona, base.getBaseHangar());

        PodSpace podSpace = new PodSpace(epona.getEntity().firstArmorIndex(), epona);

        assertSame(base, LocationUtils.findEffectiveBase(podSpace));
        assertFalse(LocationUtils.areSameEffectiveLocation(podSpace, campaign.getPlayerForce().getWarehouse()),
              "Pod space on a unit at a base is not with the main force");
    }
}
