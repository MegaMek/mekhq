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
import mekhq.campaign.GroundTransitLocation;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.location.LocationNode.LocationManager;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Maintenance can be paused on contract, off contract, and in transit, and its cost scales with the unit's situation.
 * The scenario campaign has no active contracts.
 */
class MaintenanceSituationTest {
    private static final double DELTA = 0.0001;

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Unit locust;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaignOptions = campaign.getCampaignOptions();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Person tech = scenario.withTech(EXP_ELITE);
        locust.setTech(tech);
    }

    private void sendLocustOverland() {
        LocationManager.setLocation(locust, new GroundTransitLocation(mock(PlanetarySystem.class), 5.0));
    }

    @Test
    void maintenanceProgressesOffContractByDefault() {
        Maintenance.doMaintenance(campaign, locust);

        assertTrue(locust.getDaysSinceMaintenance() > 0, "The cycle advances while off contract");
    }

    @Test
    void disablingOffContractMaintenancePausesTheCycle() {
        campaignOptions.set(CampaignOption.MAINTENANCE_OFF_CONTRACT, false);

        Maintenance.doMaintenance(campaign, locust);

        assertEquals(0.0, locust.getDaysSinceMaintenance(), DELTA, "The cycle does not advance while paused");
    }

    @Test
    void disablingOnContractMaintenanceDoesNotAffectAnOffContractCampaign() {
        campaignOptions.set(CampaignOption.MAINTENANCE_ON_CONTRACT, false);

        assertTrue(Maintenance.isMaintenanceEnabledForSituation(campaign, locust));
    }

    @Test
    void disablingInTransitMaintenancePausesOnlyTravellingUnits() {
        campaignOptions.set(CampaignOption.MAINTENANCE_IN_TRANSIT, false);
        assertTrue(Maintenance.isMaintenanceEnabledForSituation(campaign, locust), "A unit in the hangar is not "
              + "in transit");

        sendLocustOverland();

        assertTrue(Maintenance.isUnitInTransit(locust));
        assertFalse(Maintenance.isMaintenanceEnabledForSituation(campaign, locust));
    }

    @Test
    void immediateMaintenanceWhilePausedReturnsAtOnce() {
        Maintenance.doMaintenance(campaign, locust);
        assertTrue(locust.getDaysSinceMaintenance() > 0, "The unit is part way through its cycle");
        campaignOptions.set(CampaignOption.MAINTENANCE_OFF_CONTRACT, false);

        assertTimeoutPreemptively(Duration.ofSeconds(5),
              () -> Maintenance.performImmediateMaintenance(campaign, locust));
    }

    @Test
    void noSituationalMultiplierAppliesOffContractOnPlanet() {
        campaignOptions.set(CampaignOption.IN_TRANSIT_MAINTENANCE_COST_MULTIPLIER, 0.5);

        assertEquals(1.0, Maintenance.getSituationalMaintenanceCostMultiplier(campaign, locust), DELTA);
    }

    @Test
    void inTransitMultiplierAppliesToATravellingMek() {
        campaignOptions.set(CampaignOption.IN_TRANSIT_MAINTENANCE_COST_MULTIPLIER, 0.5);

        sendLocustOverland();

        assertEquals(0.5, Maintenance.getSituationalMaintenanceCostMultiplier(campaign, locust), DELTA);
    }
}
