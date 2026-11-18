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
package mekhq.campaign.digitalGM.stratCon;

import static mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation.AllGroundTerrain;
import static mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation.LowAtmosphere;
import static mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation.Space;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.facility.FacilityOperation;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractGeneration.AbstractContractGeneration;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.scenarios.ScenarioMapParameters.MapLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for where and how StratCon scenarios spawn: how many come per point of Scale, which contracts fight Essential
 * scenarios, the objective an ignored scenario fails, the hexes an enemy scenario may spawn on, the catch-up of overdue
 * ordinary scenario dates, and the forces an external scenario is seeded with.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConScenarioSpawningTest {
    private static final String TERRAIN = "Plains";
    private static final LocalDate TODAY = LocalDate.of(3025, 3, 10);
    private static final int FORCE_ID = 7;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static StratConTrackState track(int width, int height) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                track.setTerrainTile(new StratConCoords(x, y), TERRAIN);
            }
        }
        return track;
    }

    private static StratConFacility facility(ForceAlignment owner) {
        return StratConTestData.facility(owner, FacilityType.SupplyDepot);
    }

    private static StratConStrategicObjective addObjective(StratConTrackState track, StratConCoords coords,
          StrategicObjectiveType objectiveType) {
        StratConStrategicObjective objective = new StratConStrategicObjective();
        objective.setObjectiveCoords(coords);
        objective.setObjectiveType(objectiveType);
        objective.setDesiredObjectiveCount(1);
        track.addStrategicObjective(objective);
        return objective;
    }

    // Ignored scenarios

    @Test
    void ignoredScenarioWithoutFacilityFailsItsObjective() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);

        assertTrue(StratConRulesManager.isIgnoredScenarioObjectiveFailed(track, coords, null, null));
    }

    @Test
    void ignoredSiegeFightNeverFailsAnObjective() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);
        addObjective(track, coords, StrategicObjectiveType.SpecificScenarioVictory);

        assertFalse(StratConRulesManager.isIgnoredScenarioObjectiveFailed(track, coords, null,
              FacilityOperation.SIEGE));
    }

    @Test
    void ignoredEssentialScenarioOnItsOwnFacilityFailsItsObjective() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);
        StratConFacility facility = facility(ForceAlignment.Opposing);
        track.addFacility(coords, facility);
        addObjective(track, coords, StrategicObjectiveType.SpecificScenarioVictory);

        assertTrue(StratConRulesManager.isIgnoredScenarioObjectiveFailed(track, coords, facility, null));
    }

    @Test
    void ignoredScenarioOnAnObjectiveFacilityLeavesTheFacilityObjectiveAlone() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);
        StratConFacility facility = facility(ForceAlignment.Opposing);
        track.addFacility(coords, facility);
        addObjective(track, coords, StrategicObjectiveType.HostileFacilityControl);

        assertFalse(StratConRulesManager.isIgnoredScenarioObjectiveFailed(track, coords, facility, null));
    }

    @Test
    void ignoredScenarioOnAFacilityWithNoObjectiveFailsNothing() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);
        StratConFacility facility = facility(ForceAlignment.Allied);
        track.addFacility(coords, facility);

        assertFalse(StratConRulesManager.isIgnoredScenarioObjectiveFailed(track, coords, facility, null));
    }

    @Test
    void processingAnIgnoredEssentialFacilityScenarioFailsItsObjective() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);
        track.addFacility(coords, facility(ForceAlignment.Opposing));
        StratConStrategicObjective objective = addObjective(track, coords,
              StrategicObjectiveType.SpecificScenarioVictory);

        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getId()).thenReturn(412);
        when(backingScenario.getForceIDs()).thenReturn(new ArrayList<>());
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(coords);
        scenario.setBackingScenario(backingScenario);
        scenario.setStrategicObjective(true);
        track.addScenario(scenario);

        StratConRulesManager.processIgnoredStratConScenario(scenario, track, new StratConCampaignState());

        assertFalse(track.getScenarios().containsKey(coords));
        assertEquals(StratConStrategicObjective.OBJECTIVE_FAILED, objective.getCurrentObjectiveCount());
        assertTrue(objective.isObjectiveFailed(track));
    }

    // Where an enemy scenario may spawn

    @Test
    void enemyScenarioNeverSpawnsOnAnEnemyFacilityHeldByPlayerForces() {
        StratConTrackState track = track(2, 1);
        StratConCoords facilityCoords = new StratConCoords(0, 0);
        track.addFacility(facilityCoords, facility(ForceAlignment.Opposing));
        track.assignForce(FORCE_ID, facilityCoords, TODAY, false);

        for (int attempt = 0; attempt < 20; attempt++) {
            assertEquals(new StratConCoords(1, 0),
                  StratConContractInitializer.getUnoccupiedCoords(track, true, true, true));
        }
    }

    @Test
    void enemyScenarioMaySpawnOnAnAlliedFacilityHeldByPlayerForces() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);
        track.addFacility(coords, facility(ForceAlignment.Allied));
        track.assignForce(FORCE_ID, coords, TODAY, false);

        assertEquals(coords, StratConContractInitializer.getUnoccupiedCoords(track, true, true, true));
    }

    @Test
    void playerForcesDoNotOpenAnAlliedFacilityWhenFacilitiesAreNotAllowed() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);
        track.addFacility(coords, facility(ForceAlignment.Allied));
        track.assignForce(FORCE_ID, coords, TODAY, false);

        assertNull(StratConContractInitializer.getUnoccupiedCoords(track, false, true, true));
    }

    @Test
    void enemyScenarioMaySpawnOnPlayerForcesInTheOpen() {
        StratConTrackState track = track(1, 1);
        StratConCoords coords = new StratConCoords(0, 0);
        track.assignForce(FORCE_ID, coords, TODAY, false);

        assertEquals(coords, StratConContractInitializer.getUnoccupiedCoords(track, false, true, false));
        assertNull(StratConContractInitializer.getUnoccupiedCoords(track, true, false, false));
    }

    // Overdue ordinary scenario dates

    @Test
    void scenarioDatesDueTodayAreTakenAndCounted() {
        List<LocalDate> dates = new ArrayList<>(List.of(TODAY, TODAY, TODAY.plusDays(1)));

        assertEquals(2, StratConScenarioTempo.takeDueScenarioDates(dates, TODAY));
        assertEquals(List.of(TODAY.plusDays(1)), dates);
    }

    @Test
    void overdueScenarioDatesAreCaughtUp() {
        List<LocalDate> dates = new ArrayList<>(List.of(TODAY.minusDays(1),
              TODAY.minusDays(StratConScenarioTempo.MAXIMUM_SCENARIO_CATCH_UP_DAYS)));

        assertEquals(2, StratConScenarioTempo.takeDueScenarioDates(dates, TODAY));
        assertTrue(dates.isEmpty());
    }

    @Test
    void staleScenarioDatesAreDiscardedWithoutBeingCounted() {
        List<LocalDate> dates = new ArrayList<>(List.of(
              TODAY.minusDays(StratConScenarioTempo.MAXIMUM_SCENARIO_CATCH_UP_DAYS + 1L),
              TODAY.minusMonths(6),
              TODAY.plusDays(3)));

        assertEquals(0, StratConScenarioTempo.takeDueScenarioDates(dates, TODAY));
        assertEquals(List.of(TODAY.plusDays(3)), dates);
    }

    @Test
    void futureScenarioDatesAreLeftAlone() {
        List<LocalDate> dates = new ArrayList<>(List.of(TODAY.plusDays(1), TODAY.plusDays(20)));

        assertEquals(0, StratConScenarioTempo.takeDueScenarioDates(dates, TODAY));
        assertEquals(2, dates.size());
    }

    // External scenario seed forces

    private static Map<MapLocation, List<Integer>> sortedForces(List<Integer> ground, List<Integer> air,
          List<Integer> space) {
        Map<MapLocation, List<Integer>> sorted = new HashMap<>();
        sorted.put(AllGroundTerrain, new ArrayList<>(ground));
        sorted.put(LowAtmosphere, new ArrayList<>(air));
        sorted.put(Space, new ArrayList<>(space));
        return sorted;
    }

    @Test
    void externalScenarioWithoutTemplateMaySeedWithAnyForce() {
        List<Integer> available = List.of(1, 2, 3);

        assertEquals(available,
              StratConRulesManager.getExternalScenarioCandidateForceIDs(available,
                    sortedForces(List.of(1), List.of(2), List.of(3)),
                    false));
    }

    @Test
    void externalScenarioWithTemplateSeedsOnlyWithSuitableForces() {
        List<Integer> available = List.of(1, 2, 3, 4);
        // A ground template has cleared the air and space forces
        Map<MapLocation, List<Integer>> sorted = sortedForces(List.of(4, 1), List.of(), List.of());

        assertEquals(List.of(1, 4),
              StratConRulesManager.getExternalScenarioCandidateForceIDs(available, sorted, true));
    }

    @Test
    void externalScenarioWithTemplateFallsBackToAnyForceWhenNoneSuits() {
        List<Integer> available = List.of(2, 3);

        assertEquals(available,
              StratConRulesManager.getExternalScenarioCandidateForceIDs(available,
                    sortedForces(List.of(), List.of(), List.of()),
                    true));
    }

    @Test
    void externalScenarioWithNoForcesHasNoCandidates() {
        assertTrue(StratConRulesManager.getExternalScenarioCandidateForceIDs(List.of(),
              sortedForces(List.of(), List.of(), List.of()),
              true).isEmpty());
    }

    // Spawn rates

    @ParameterizedTest
    @CsvSource({
          // exact scale, rate per scale, expected
          "2.0, 1, 2",
          "1.4, 3, 4",
          "2.5, 1, 3",
          "2.4, 1, 2",
          "0.0, 1, 1",
          "0.3, 3, 1",
          "2.0, 0, 0"
    })
    void theCountIsTheScaleTimesTheRateRoundedNormallyAndNeverBelowOne(double exactScale, double ratePerScale,
          int expected) {
        assertEquals(expected, StratConSpawnRates.getCount(exactScale, ratePerScale));
    }

    @Test
    void theMonthlyScheduleHasTheSameCountEveryMonth() {
        assertEquals(List.of(2, 2, 2), StratConSpawnRates.getMonthlySchedule(3, 2));
        assertEquals(List.of(4), StratConSpawnRates.getMonthlySchedule(0, 4), "a contract under a month gets one");
    }

    // Essential scenarios

    private static AbstractContract contract(ContractObjectiveType objectiveType, double exactScale) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        when(contract.getExactScale()).thenReturn(exactScale);
        when(contract.getLengthInMonths()).thenReturn(4);
        return contract;
    }

    @Test
    void essentialScenariosComeOnceAMonthPerPointOfScale() {
        AbstractContract contract = contract(ContractObjectiveType.PLANETARY_ASSAULT, 2.6);

        assertEquals(List.of(3, 3, 3, 3),
              AbstractContractGeneration.rollScenarioSchedule(mock(Campaign.class), contract, 4));
    }

    @Test
    void aContractWithEssentialScenariosInItsDefinitionFightsThem() {
        // Planetary Assault keeps its Essential scenarios alongside its strategic positions
        assertTrue(StratConContractInitializer.usesEssentialScenarios(
              contract(ContractObjectiveType.PLANETARY_ASSAULT, 1), true));
    }

    @Test
    void aContractWhoseNavPointsReplaceItsEssentialScenariosFightsNone() {
        AbstractContract contract = contract(ContractObjectiveType.ESPIONAGE, 1);

        assertFalse(StratConContractInitializer.usesEssentialScenarios(contract, true));
        assertTrue(StratConContractInitializer.usesEssentialScenarios(contract, false),
              "without special mechanics, its Essential scenarios come back");
    }

    @Test
    void aContractWhoseDefinitionHasNoEssentialScenariosFightsNone() {
        // Garrison Duty's definition has no specific-scenario objective to draw them from
        assertFalse(StratConContractInitializer.usesEssentialScenarios(
              contract(ContractObjectiveType.GARRISON_DUTY, 1), false));
    }

    // How strongly normal scenarios are drawn to the player's forces

    @Test
    void aHexWithFormationsWeighsOneMorePerFormation() {
        assertEquals(2, StratConContractInitializer.getPlayerForceHexWeight(1), "twice an empty hex");
        assertEquals(3, StratConContractInitializer.getPlayerForceHexWeight(2));
        assertEquals(5, StratConContractInitializer.getPlayerForceHexWeight(4));
    }

    @Test
    void aHexMarkedAsHoldingFormationsAlwaysWeighsMoreThanAnEmptyOne() {
        assertEquals(2, StratConContractInitializer.getPlayerForceHexWeight(0));
    }
}
