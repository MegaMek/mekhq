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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.PointOfInterestParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestPlacer;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for points of interest as strategic objectives, for placing them at contract start, and for the contract
 * definition fields that ask for them.
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

    // Contract start

    @Test
    void contractStartPlacesTheRequestedPointsOfInterest() {
        StratConContractInitializer.initializeTrackPointsOfInterest(track,
              3,
              List.of(TYPE_ID, OTHER_TYPE_ID),
              false,
              TODAY);

        assertEquals(3, track.getPointsOfInterest().size());
        assertTrue(track.getStrategicObjectives().isEmpty());
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            assertTrue(List.of(TYPE_ID, OTHER_TYPE_ID).contains(pointOfInterest.getTypeId()));
        }
    }

    @Test
    void contractStartCanPlacePointsOfInterestAsObjectives() {
        StratConContractInitializer.initializeTrackPointsOfInterest(track, 2, List.of(TYPE_ID), true, TODAY);

        assertEquals(2, track.getPointsOfInterest().size());
        assertEquals(2, track.getStrategicObjectives().size());
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            assertEquals(StrategicObjectiveType.PointOfInterest, objective.getObjectiveType());
            assertNotNull(objective.getPointOfInterest(track));
        }
    }

    @Test
    void contractStartWithNoTypesToChooseFromPlacesNothing() {
        StratConContractInitializer.initializeTrackPointsOfInterest(track, 2, List.of(), true, TODAY);

        assertTrue(track.getPointsOfInterest().isEmpty());
        assertTrue(track.getStrategicObjectives().isEmpty());
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
