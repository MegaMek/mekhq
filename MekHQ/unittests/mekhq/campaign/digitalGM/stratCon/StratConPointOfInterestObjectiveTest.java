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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.PointOfInterestParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestPlacer;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/**
 * Tests for points of interest as strategic objectives, for scheduling them over a contract and placing them as their
 * days come, and for the contract definition fields that ask for them.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPointOfInterestObjectiveTest {
    private static final String TERRAIN = "Plains";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final String TYPE_ID = "UnitTestObjectiveType";
    private static final String OTHER_TYPE_ID = "UnitTestObjectiveOtherType";

    private StratConTrackState track;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        registerDefinition(TYPE_ID);
        registerDefinition(OTHER_TYPE_ID);

        track = new StratConTrackState();
        track.setWidth(4);
        track.setHeight(4);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                track.setTerrainTile(new StratConCoords(x, y), TERRAIN);
            }
        }
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(OTHER_TYPE_ID);
    }

    private static void registerDefinition(String typeId) {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(typeId);
        StratConPointOfInterestDefinitions.registerDefinition(definition);
    }

    private StratConPointOfInterest placeObjective() {
        StratConPointOfInterest pointOfInterest = StratConPointOfInterestPlacer.placeAsStrategicObjective(track,
              TYPE_ID,
              new StratConCoords(1, 1),
              TODAY);
        assertNotNull(pointOfInterest, "test setup: the objective should be placed");
        return pointOfInterest;
    }

    private StratConStrategicObjective objective() {
        return track.getStrategicObjectives().get(0);
    }

    // Objective state

    @Test
    void anActivePointOfInterestObjectiveIsInProgress() {
        placeObjective();

        assertFalse(objective().isObjectiveCompleted(track));
        assertFalse(objective().isObjectiveFailed(track));
        assertFalse(objective().isObjectiveResolved(track));
    }

    @Test
    void resolvingThePointOfInterestMeetsTheObjective() {
        StratConPointOfInterest pointOfInterest = placeObjective();

        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);

        assertEquals(PointOfInterestStatus.RESOLVED, pointOfInterest.getStatus());
        assertTrue(objective().isObjectiveCompleted(track));
        assertFalse(objective().isObjectiveFailed(track));
    }

    @Test
    void aResolvedObjectiveStaysMetAfterItsPointOfInterestIsRemoved() {
        StratConPointOfInterest pointOfInterest = placeObjective();
        StratConPointOfInterestRules.resolvePointOfInterest(track, pointOfInterest);

        track.removePointOfInterest(pointOfInterest.getId());

        assertTrue(objective().isObjectiveCompleted(track));
        assertFalse(objective().isObjectiveFailed(track));
    }

    @Test
    void settingTheStatusDirectlyAlsoMeetsTheObjectiveWhileItIsOnTheMap() {
        StratConPointOfInterest pointOfInterest = placeObjective();

        pointOfInterest.setStatus(PointOfInterestStatus.RESOLVED);

        assertTrue(objective().isObjectiveCompleted(track));
    }

    @Test
    void anUnresolvedPointOfInterestRemovedFromTheMapFailsTheObjective() {
        StratConPointOfInterest pointOfInterest = placeObjective();

        track.removePointOfInterest(pointOfInterest.getId());

        assertFalse(objective().isObjectiveCompleted(track));
        assertTrue(objective().isObjectiveFailed(track));
    }

    @Test
    void anExpiredPointOfInterestFailsTheObjective() {
        StratConPointOfInterest pointOfInterest = placeObjective();

        pointOfInterest.setStatus(PointOfInterestStatus.EXPIRED);

        assertTrue(objective().isObjectiveFailed(track));
        assertTrue(objective().isObjectiveResolved(track));
    }

    @Test
    void anObjectiveMarkedFailedStaysFailed() {
        StratConPointOfInterest pointOfInterest = placeObjective();
        objective().setCurrentObjectiveCount(StratConStrategicObjective.OBJECTIVE_FAILED);
        pointOfInterest.setStatus(PointOfInterestStatus.RESOLVED);

        assertTrue(objective().isObjectiveFailed(track));
        assertFalse(objective().isObjectiveCompleted(track), "an objective is never both failed and met");
    }

    @Test
    void pointOfInterestObjectivesAreNotTrackedByHex() {
        placeObjective();

        assertTrue(track.getObjectivesByCoords().isEmpty(),
              "several points of interest can share a hex, so their objectives never claim one");
    }

    // Scheduling over the contract

    private static ObjectiveParameters pointOfInterestObjective(double count, String... typeIds) {
        ObjectiveParameters objectiveParameters = new ObjectiveParameters();
        objectiveParameters.setObjectiveType(StrategicObjectiveType.PointOfInterest);
        objectiveParameters.setObjectiveCount(count);
        objectiveParameters.getObjectivePointsOfInterest().addAll(List.of(typeIds));
        return objectiveParameters;
    }

    private static PointOfInterestParameters ordinaryPointOfInterest(String typeId, double count) {
        PointOfInterestParameters pointOfInterestParameters = new PointOfInterestParameters();
        pointOfInterestParameters.setTypeId(typeId);
        pointOfInterestParameters.setCount(count);
        return pointOfInterestParameters;
    }

    private static StratConContractDefinition contractDefinition(List<ObjectiveParameters> objectives,
          List<PointOfInterestParameters> pointsOfInterest) {
        StratConContractDefinition definition = new StratConContractDefinition();
        definition.setObjectiveParameters(objectives);
        definition.setPointsOfInterest(pointsOfInterest);
        return definition;
    }

    private static int countStrategicObjectives(List<StratConScheduledPointOfInterest> pointsOfInterest) {
        int objectives = 0;
        for (StratConScheduledPointOfInterest pointOfInterest : pointsOfInterest) {
            if (pointOfInterest.isStrategicObjective()) {
                objectives++;
            }
        }
        return objectives;
    }

    @Test
    void requestedPointsOfInterestIncludeObjectivesAndOrdinaryOnes() {
        ObjectiveParameters otherObjective = new ObjectiveParameters();
        otherObjective.setObjectiveType(StrategicObjectiveType.AnyScenarioVictory);
        otherObjective.setObjectiveCount(4);
        StratConContractDefinition definition = contractDefinition(
              List.of(pointOfInterestObjective(2, TYPE_ID), otherObjective),
              List.of(ordinaryPointOfInterest(OTHER_TYPE_ID, 3)));

        List<StratConScheduledPointOfInterest> requested =
              StratConContractInitializer.getRequestedPointsOfInterest(definition, 1);

        assertEquals(5, requested.size(), "other objective types ask for no points of interest");
        assertEquals(2, countStrategicObjectives(requested));
        for (StratConScheduledPointOfInterest pointOfInterest : requested) {
            String expectedType = pointOfInterest.isStrategicObjective() ? TYPE_ID : OTHER_TYPE_ID;
            assertEquals(expectedType, pointOfInterest.getTypeId());
        }
    }

    @Test
    void negativeCountsScaleWithTheContract() {
        // An objective never scales below one; an ordinary entry may round down to none.
        StratConContractDefinition definition = contractDefinition(
              List.of(pointOfInterestObjective(-0.1, TYPE_ID)),
              List.of(ordinaryPointOfInterest(OTHER_TYPE_ID, -0.5), ordinaryPointOfInterest(TYPE_ID, -0.1)));

        List<StratConScheduledPointOfInterest> requested =
              StratConContractInitializer.getRequestedPointsOfInterest(definition, 3);

        assertEquals(1, countStrategicObjectives(requested), "max(1, 0.3) objectives");
        assertEquals(2, requested.size(), "plus (int) 1.5 ordinary, plus (int) 0.3 of the other type");
    }

    @Test
    void entriesWithoutTypesAreSkipped() {
        StratConContractDefinition definition = contractDefinition(
              List.of(pointOfInterestObjective(2)),
              List.of(ordinaryPointOfInterest(" ", 2), ordinaryPointOfInterest(null, 2)));

        assertTrue(StratConContractInitializer.getRequestedPointsOfInterest(definition, 1).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked") // ArgumentCaptor cannot name a generic List type without an unchecked conversion
    void acceptingAContractSchedulesItsPointsOfInterestAcrossItsMonths() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStartDate()).thenReturn(TODAY);
        when(contract.getLengthInMonths()).thenReturn(3);
        when(contract.getScale()).thenReturn(1);
        StratConCampaignState campaignState = new StratConCampaignState();
        StratConContractDefinition definition = contractDefinition(
              List.of(pointOfInterestObjective(1, TYPE_ID)),
              List.of(ordinaryPointOfInterest(OTHER_TYPE_ID, 4)));

        StratConContractInitializer.schedulePointsOfInterest(contract, definition, campaignState);

        List<StratConScheduledPointOfInterest> scheduled = campaignState.getScheduledPointsOfInterest();
        assertEquals(5, scheduled.size());
        assertEquals(1, countStrategicObjectives(scheduled));
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertNotNull(pointOfInterest.getSpawnDate());
            assertFalse(pointOfInterest.getSpawnDate().isBefore(TODAY));
            assertTrue(pointOfInterest.getSpawnDate().isBefore(TODAY.plusMonths(3)));
        }
        assertTrue(track.getPointsOfInterest().isEmpty(), "nothing is placed up front");

        ArgumentCaptor<List<Integer>> scheduleCaptor = ArgumentCaptor.forClass(List.class);
        verify(contract).setPointOfInterestSchedule(scheduleCaptor.capture());
        int scheduledCount = 0;
        for (int monthlyCount : scheduleCaptor.getValue()) {
            scheduledCount += monthlyCount;
        }
        assertEquals(5, scheduledCount, "the per-month schedule kept on the contract covers every point of interest");
    }

    @Test
    void aContractWithoutAStartDateSchedulesNothing() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStartDate()).thenReturn(null);
        StratConCampaignState campaignState = new StratConCampaignState();
        StratConContractDefinition definition = contractDefinition(List.of(),
              List.of(ordinaryPointOfInterest(TYPE_ID, 2)));

        StratConContractInitializer.schedulePointsOfInterest(contract, definition, campaignState);

        assertTrue(campaignState.getScheduledPointsOfInterest().isEmpty());
    }

    private static Campaign spawningCampaign(boolean maplessMode) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isUseStratConMaplessMode()).thenReturn(maplessMode);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        return campaign;
    }

    private AbstractContract contractWithTrack() {
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        return contract;
    }

    @Test
    void aScheduledObjectiveIsPlacedWithItsObjectiveWhenItsDayComes() {
        StratConScheduledPointOfInterest scheduled = new StratConScheduledPointOfInterest(TODAY, TYPE_ID, true);

        StratConPointOfInterest placed = StratConContractInitializer.spawnScheduledPointOfInterest(
              spawningCampaign(false),
              contractWithTrack(),
              scheduled);

        assertNotNull(placed);
        assertEquals(List.of(placed), track.getPointsOfInterest());
        assertEquals(1, track.getStrategicObjectives().size());
        assertEquals(placed.getId(), track.getStrategicObjectives().get(0).getPointOfInterestId());
    }

    @Test
    void aScheduledOrdinaryPointOfInterestAddsNoObjective() {
        StratConScheduledPointOfInterest scheduled = new StratConScheduledPointOfInterest(TODAY, TYPE_ID, false);

        assertNotNull(StratConContractInitializer.spawnScheduledPointOfInterest(spawningCampaign(false),
              contractWithTrack(),
              scheduled));
        assertTrue(track.getStrategicObjectives().isEmpty());
    }

    @Test
    void nothingIsPlacedInMaplessMode() {
        StratConScheduledPointOfInterest scheduled = new StratConScheduledPointOfInterest(TODAY, TYPE_ID, false);

        assertNull(StratConContractInitializer.spawnScheduledPointOfInterest(spawningCampaign(true),
              contractWithTrack(),
              scheduled));
        assertTrue(track.getPointsOfInterest().isEmpty());
    }

    @Test
    void scheduledPointsOfInterestAreDueOnTheirDayOrLater() {
        StratConScheduledPointOfInterest scheduled = new StratConScheduledPointOfInterest(TODAY, TYPE_ID, false);

        assertFalse(scheduled.isDue(TODAY.minusDays(1)));
        assertTrue(scheduled.isDue(TODAY));
        assertTrue(scheduled.isDue(TODAY.plusDays(5)), "a skipped day still catches up");
    }

    // Contract definitions

    @Test
    void contractDefinitionPointOfInterestFieldsRoundTrip(@TempDir Path tempDir) {
        StratConContractDefinition original = new StratConContractDefinition();

        ObjectiveParameters objectiveParameters = new ObjectiveParameters();
        objectiveParameters.setObjectiveType(StrategicObjectiveType.PointOfInterest);
        objectiveParameters.setObjectiveCount(2);
        objectiveParameters.getObjectivePointsOfInterest().add(TYPE_ID);
        original.setObjectiveParameters(List.of(objectiveParameters));

        PointOfInterestParameters pointOfInterestParameters = new PointOfInterestParameters();
        pointOfInterestParameters.setTypeId(OTHER_TYPE_ID);
        pointOfInterestParameters.setCount(-0.5);
        original.setPointsOfInterest(List.of(pointOfInterestParameters));

        File out = tempDir.resolve("PointOfInterestContract.json").toFile();
        original.Serialize(out);
        StratConContractDefinition reloaded = StratConContractDefinition.Deserialize(out);

        assertNotNull(reloaded);
        ObjectiveParameters reloadedObjective = reloaded.getObjectiveParameters().get(0);
        assertEquals(StrategicObjectiveType.PointOfInterest, reloadedObjective.getObjectiveType());
        assertEquals(List.of(TYPE_ID), reloadedObjective.getObjectivePointsOfInterest());
        assertEquals(1, reloaded.getPointsOfInterest().size());
        assertEquals(OTHER_TYPE_ID, reloaded.getPointsOfInterest().get(0).getTypeId());
        assertEquals(-0.5, reloaded.getPointsOfInterest().get(0).getCount());
    }

    @Test
    void contractDefinitionsWithoutPointsOfInterestHaveEmptyLists() {
        StratConContractDefinition definition = new StratConContractDefinition();
        definition.setPointsOfInterest(null);

        assertTrue(definition.getPointsOfInterest().isEmpty());
        assertTrue(new ObjectiveParameters().getObjectivePointsOfInterest().isEmpty());
    }
}
