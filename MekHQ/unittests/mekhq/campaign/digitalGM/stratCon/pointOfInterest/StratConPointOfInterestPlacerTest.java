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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConPointOfInterestPlacer}: the placement rules, random placement, and placing points of
 * interest as strategic objectives.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPointOfInterestPlacerTest {
    private static final String TERRAIN = "Plains";
    private static final String OCEAN = "Sea";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);

    private static final String TYPE_ID = "UnitTestPlacerType";

    private StratConPointOfInterestDefinition definition;
    private StratConTrackState track;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setDefaultOwner(ForceAlignment.Opposing);
        definition.setLifespanDays(7);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

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
    }

    @Test
    void placingOnAChosenHexUsesTheDefinition() {
        StratConPointOfInterest pointOfInterest = StratConPointOfInterestPlacer.place(track,
              TYPE_ID,
              new StratConCoords(2, 1),
              TODAY);

        assertNotNull(pointOfInterest);
        assertEquals(new StratConCoords(2, 1), pointOfInterest.getCoords());
        assertEquals(ForceAlignment.Opposing, pointOfInterest.getOwner());
        assertEquals(TODAY.plusDays(7), pointOfInterest.getExpiryDate());
        assertEquals(List.of(pointOfInterest), track.getPointsOfInterest());
    }

    @Test
    void unknownTypesAreNotPlaced() {
        assertNull(StratConPointOfInterestPlacer.place(track, "NoSuchType", new StratConCoords(0, 0), TODAY));
        assertTrue(track.getPointsOfInterest().isEmpty());
    }

    @Test
    void hexesOutsideTheSectorAreNotEligible() {
        assertFalse(StratConPointOfInterestPlacer.canPlace(track, definition, new StratConCoords(4, 0)));
        assertNull(StratConPointOfInterestPlacer.place(track, TYPE_ID, new StratConCoords(4, 0), TODAY));
    }

    @Test
    void landOnlyTypesStayOffWater() {
        track.setTerrainTile(new StratConCoords(0, 0), OCEAN);

        assertFalse(StratConPointOfInterestPlacer.canPlace(track, definition, new StratConCoords(0, 0)));

        definition.setLandOnly(false);
        assertTrue(StratConPointOfInterestPlacer.canPlace(track, definition, new StratConCoords(0, 0)));
    }

    @Test
    void typesAvoidingCitiesStayOutOfThem() {
        track.addCity(new StratConCoords(1, 1));
        assertTrue(StratConPointOfInterestPlacer.canPlace(track, definition, new StratConCoords(1, 1)));

        definition.setAvoidCities(true);
        assertFalse(StratConPointOfInterestPlacer.canPlace(track, definition, new StratConCoords(1, 1)));
    }

    @Test
    void occupyingTypesNeedAFreeHex() {
        track.addFacility(new StratConCoords(1, 1), new StratConFacility());
        assertTrue(StratConPointOfInterestPlacer.canPlace(track, definition, new StratConCoords(1, 1)),
              "a point of interest that shares its hex may join a facility");

        definition.setOccupiesHex(true);
        assertFalse(StratConPointOfInterestPlacer.canPlace(track, definition, new StratConCoords(1, 1)));
    }

    @Test
    void allowedTerrainMatchesTypeNamesAndCategoriesIgnoringCase() {
        StratConCoords coords = new StratConCoords(0, 0);

        definition.setAllowedTerrainCategories(List.of("Desert"));
        assertFalse(StratConPointOfInterestPlacer.canPlace(track, definition, coords));

        definition.setAllowedTerrainCategories(List.of("plains"));
        assertTrue(StratConPointOfInterestPlacer.canPlace(track, definition, coords), "matched by terrain type name");

        String category = StratConBiomeManifest.getInstance().getTerrainCategory(TERRAIN).name();
        definition.setAllowedTerrainCategories(List.of(category.toLowerCase()));
        assertTrue(StratConPointOfInterestPlacer.canPlace(track, definition, coords), "matched by terrain category");
    }

    @Test
    void randomPlacementChoosesOnlyEligibleHexes() {
        StratConTrackState narrowTrack = new StratConTrackState();
        narrowTrack.setWidth(2);
        narrowTrack.setHeight(1);
        narrowTrack.setTerrainTile(new StratConCoords(0, 0), OCEAN);
        narrowTrack.setTerrainTile(new StratConCoords(1, 0), TERRAIN);

        for (int attempt = 0; attempt < 20; attempt++) {
            assertEquals(new StratConCoords(1, 0),
                  StratConPointOfInterestPlacer.findPlacementCoords(narrowTrack, definition));
        }
    }

    @Test
    void randomPlacementOfOccupyingTypesAvoidsDeployedForces() {
        definition.setOccupiesHex(true);
        StratConTrackState narrowTrack = new StratConTrackState();
        narrowTrack.setWidth(2);
        narrowTrack.setHeight(1);
        narrowTrack.setTerrainTile(new StratConCoords(0, 0), TERRAIN);
        narrowTrack.setTerrainTile(new StratConCoords(1, 0), TERRAIN);
        narrowTrack.assignForce(7, new StratConCoords(0, 0), TODAY, false);

        for (int attempt = 0; attempt < 20; attempt++) {
            assertEquals(new StratConCoords(1, 0),
                  StratConPointOfInterestPlacer.findPlacementCoords(narrowTrack, definition));
        }
    }

    @Test
    void randomPlacementReturnsNullWhenNothingIsEligible() {
        definition.setAllowedTerrainCategories(List.of("Desert"));

        assertNull(StratConPointOfInterestPlacer.findPlacementCoords(track, definition));
        assertNull(StratConPointOfInterestPlacer.place(track, TYPE_ID, null, TODAY));
    }

    @Test
    void placingAsAStrategicObjectiveAddsAnObjectiveTiedToIt() {
        StratConPointOfInterest pointOfInterest = StratConPointOfInterestPlacer.placeAsStrategicObjective(track,
              TYPE_ID,
              null,
              TODAY);

        assertNotNull(pointOfInterest);
        assertEquals(1, track.getStrategicObjectives().size());
        StratConStrategicObjective objective = track.getStrategicObjectives().get(0);
        assertEquals(StrategicObjectiveType.PointOfInterest, objective.getObjectiveType());
        assertEquals(pointOfInterest.getId(), objective.getPointOfInterestId());
        assertEquals(1, objective.getDesiredObjectiveCount());
        assertNull(objective.getObjectiveCoords(), "it follows its point of interest, not a hex");
    }

    @Test
    void aFailedPlacementAddsNoObjective() {
        assertNull(StratConPointOfInterestPlacer.placeAsStrategicObjective(track, "NoSuchType", null, TODAY));
        assertTrue(track.getStrategicObjectives().isEmpty());
    }
}
