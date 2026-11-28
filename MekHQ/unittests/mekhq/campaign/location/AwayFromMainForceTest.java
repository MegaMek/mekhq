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
package mekhq.campaign.location;

import static mekhq.campaign.force.Formation.FORMATION_NONE;
import static mekhq.campaign.force.Formation.NO_ASSIGNED_SCENARIO;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Vector;

import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.GroundTransitLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.gm.StratConPlayType;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.utilities.MissionCompletionManager;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Scenarios are fought by the main force. A unit kept at a base or travelling somewhere can't deploy, a formation of
 * such units has nothing to send, and a spare on its way to another place can't be used before it lands.
 */
class AwayFromMainForceTest {
    private static final String UNIT_BUNDLE = "mekhq.resources.Unit";
    private static final int SCENARIO_ID = 77;

    private PartsScenario scenario;
    private Campaign campaign;
    private PlayerBase base;
    private GroundTransitLocation travelNode;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        travelNode = new GroundTransitLocation(mock(PlanetarySystem.class), 2d);
        assertTrue(travelNode.setParent(base), "travel node should attach under the base");
        campaign.getCampaignLocationManager().addLocation(travelNode);
    }

    private Unit unitWithTheMainForce() {
        return scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
    }

    private Unit unitAtTheBase() {
        Unit unit = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        campaign.getPlayerForce().getForceDetachment().getHangar().removeUnit(unit.getId());
        base.getBaseHangar().addUnit(unit);
        return unit;
    }

    private Unit unitOnTheWay() {
        Unit unit = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        assertTrue(unit.setParent(travelNode), "unit should board the travel node");
        return unit;
    }

    private MekSensor spare() {
        MekSensor spare = new MekSensor();
        spare.setCampaign(campaign);
        return spare;
    }

    @Test
    void aUnitWithTheMainForceIsNeitherAwayNorOnTheWay() {
        Unit unit = unitWithTheMainForce();

        assertFalse(LocationUtils.isAwayFromMainForce(unit, campaign));
        assertFalse(LocationUtils.isOnTheWay(unit));
    }

    @Test
    void aUnitAtABaseIsAwayButNotOnTheWay() {
        Unit unit = unitAtTheBase();

        assertTrue(LocationUtils.isAwayFromMainForce(unit, campaign));
        assertFalse(LocationUtils.isOnTheWay(unit));
    }

    @Test
    void aUnitTravellingToABaseIsAwayAndOnTheWay() {
        Unit unit = unitOnTheWay();

        assertTrue(LocationUtils.isAwayFromMainForce(unit, campaign));
        assertTrue(LocationUtils.isOnTheWay(unit));
    }

    @Test
    void anItemWithNoPlaceCountsAsWithTheMainForce() {
        MekSensor unplacedSpare = spare();

        assertFalse(LocationUtils.isAwayFromMainForce(unplacedSpare, campaign));
        assertFalse(LocationUtils.isOnTheWay(unplacedSpare));
    }

    @Test
    void aUnitWithTheMainForceIsNotHeldBackByItsLocation() {
        String problem = unitWithTheMainForce().checkDeployment();

        assertNotEquals(getTextAt(UNIT_BUNDLE, "Unit.checkDeployment.inTransit"), problem);
        assertNotEquals(getTextAt(UNIT_BUNDLE, "Unit.checkDeployment.awayFromForce"), problem);
    }

    @Test
    void aUnitAtABaseCannotDeploy() {
        assertEquals(getTextAt(UNIT_BUNDLE, "Unit.checkDeployment.awayFromForce"), unitAtTheBase().checkDeployment());
    }

    @Test
    void aUnitOnTheWayCannotDeploy() {
        assertEquals(getTextAt(UNIT_BUNDLE, "Unit.checkDeployment.inTransit"), unitOnTheWay().checkDeployment());
    }

    @Test
    void aUnitWhoseCrewIsElsewhereCannotDeploy() {
        Unit unit = unitWithTheMainForce();
        Person pilot = new Person("Test", "Pilot", campaign);
        pilot.setPrimaryRoleDirect(PersonnelRole.MEKWARRIOR);
        campaign.importPerson(pilot);
        unit.addPilotOrSoldier(pilot);
        assertTrue(pilot.setParent(travelNode), "the pilot should board the travel node");

        assertEquals(getFormattedTextAt(UNIT_BUNDLE, "Unit.checkDeployment.crewElsewhere", pilot.getFullTitle()),
              unit.checkDeployment());
    }

    @Test
    void aScenarioTakesOnlyUnitsWithTheMainForce() {
        Scenario battle = new Scenario("Battle");

        assertFalse(battle.canDeploy(unitAtTheBase(), campaign), "a unit at a base can't join a scenario");
        assertFalse(battle.canDeploy(unitOnTheWay(), campaign), "a unit on the way can't join a scenario");
    }

    @Test
    void deployingAFormationLeavesItsAwayUnitsBehind() {
        Unit withTheForce = unitWithTheMainForce();
        Unit atTheBase = unitAtTheBase();
        Formation lance = new Formation("Lance");
        lance.addUnit(withTheForce.getId());
        lance.addUnit(atTheBase.getId());

        lance.setScenarioId(SCENARIO_ID, campaign);

        assertEquals(SCENARIO_ID, withTheForce.getScenarioId(), "the unit with the main force deploys");
        assertEquals(NO_ASSIGNED_SCENARIO, atTheBase.getScenarioId(), "the unit at the base stays behind");
    }

    @Test
    void aFormationIsAwayOnlyWhenNoneOfItsUnitsAreWithTheMainForce() {
        Formation awayLance = new Formation("Away Lance");
        awayLance.addUnit(unitAtTheBase().getId());
        awayLance.addUnit(unitOnTheWay().getId());
        Formation mixedLance = new Formation("Mixed Lance");
        mixedLance.addUnit(unitAtTheBase().getId());
        mixedLance.addUnit(unitWithTheMainForce().getId());
        Formation emptyLance = new Formation("Empty Lance");

        assertTrue(awayLance.isAwayFromMainForce(campaign));
        assertFalse(mixedLance.isAwayFromMainForce(campaign));
        assertFalse(emptyLance.isAwayFromMainForce(campaign));
    }

    @Test
    void aScenarioRefusesAFormationWithNobodyWithTheMainForce() {
        Formation awayLance = new Formation("Away Lance");
        awayLance.addUnit(unitAtTheBase().getId());
        Vector<Formation> formations = new Vector<>();
        formations.add(awayLance);

        assertFalse(new Scenario("Battle").canDeployForces(formations, campaign));
    }

    @Test
    void enemySizingCountsOnlyUnitsWithTheMainForce() {
        Unit withTheForce = unitWithTheMainForce();
        Formation lance = new Formation("Lance");
        lance.addUnit(withTheForce.getId());
        lance.addUnit(unitAtTheBase().getId());
        lance.addUnit(unitOnTheWay().getId());
        Formation lanceWithoutTheAwayUnits = new Formation("Lance Without The Away Units");
        lanceWithoutTheAwayUnits.addUnit(withTheForce.getId());

        assertEquals(lanceWithoutTheAwayUnits.getTotalBV(campaign, true), lance.getTotalBV(campaign, true));
        assertEquals(1, lance.getUnitCountWithMainForce(campaign, false));
        assertEquals(1, lance.getUnitCountWithMainForce(campaign, true));
        assertEquals(3, lance.getTotalUnitCount(campaign, false), "on paper the lance keeps all its units");
    }

    @Test
    void aCombatTeamIsWeighedWithoutItsAwayUnits() {
        // Without StratCon only vehicles and fighters are weighed, so switch it on to weigh the Meks.
        campaign.getCampaignOptions().set(CampaignOption.STRAT_CON_PLAY_TYPE, StratConPlayType.NORMAL);
        Unit withTheForce = unitWithTheMainForce();
        Formation lance = registeredLance();
        lance.addUnit(withTheForce.getId());
        lance.addUnit(unitAtTheBase().getId());

        assertEquals(withTheForce.getEntity().getWeight(), CombatTeam.calculateTotalWeight(campaign, lance.getId()));
    }

    @Test
    void removingAFormationReleasesItsUnitsAtABase() {
        Unit atTheBase = unitAtTheBase();
        Formation lance = registeredLance();
        lance.addUnit(atTheBase.getId());
        atTheBase.setFormationId(lance.getId());

        campaign.getPlayerForce().removeFormation(lance, campaign);

        assertEquals(FORMATION_NONE, atTheBase.getFormationId());
    }

    @Test
    void cleanupOnLoadUndeploysUnitsAtABase() {
        Unit atTheBase = unitAtTheBase();
        atTheBase.setScenarioId(SCENARIO_ID);

        MissionCompletionManager.releaseCompletedContractDeployments(campaign);

        assertEquals(NO_ASSIGNED_SCENARIO, atTheBase.getScenarioId());
    }

    private Formation registeredLance() {
        Formation lance = new Formation("Lance");
        lance.setId(SCENARIO_ID);
        campaign.getPlayerForce().importFormation(lance);
        return lance;
    }

    @Test
    void aSpareOnTheWayIsNotOnHand() {
        MekSensor travellingSpare = spare();
        assertTrue(travellingSpare.setParent(travelNode), "spare should board the travel node");
        MekSensor spareAtTheBase = spare();
        assertTrue(spareAtTheBase.setParent(base), "spare should be kept at the base");

        assertFalse(travellingSpare.isOnHand());
        assertFalse(MissingPart.isAvailableAsReplacement(travellingSpare), "a spare on the way can't replace a part");
        assertTrue(spareAtTheBase.isOnHand());
        assertTrue(MissingPart.isAvailableAsReplacement(spareAtTheBase));
    }

    @Test
    void aSpareOnTheWayDoesNotStackWithSparesOnHand() {
        MekSensor travellingSpare = spare();
        assertTrue(travellingSpare.setParent(travelNode), "spare should board the travel node");
        MekSensor otherTravellingSpare = spare();
        assertTrue(otherTravellingSpare.setParent(travelNode), "spare should board the travel node");
        MekSensor spareAtTheBase = spare();
        assertTrue(spareAtTheBase.setParent(base), "spare should be kept at the base");

        assertFalse(spareAtTheBase.isSamePartTypeAndStatus(travellingSpare), "a new spare must not join a travelling stack");
        assertFalse(travellingSpare.isSamePartTypeAndStatus(spareAtTheBase));
        assertTrue(travellingSpare.isSamePartTypeAndStatus(otherTravellingSpare), "spares on the same trip stack");
    }
}
