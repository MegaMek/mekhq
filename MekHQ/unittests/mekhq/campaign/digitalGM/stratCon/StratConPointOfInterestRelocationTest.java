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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer.ResizeImpact;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for how points of interest take part in the sector's occupancy rules: the free-hex search, facility capacity,
 * sector resizing, and relocation off flooded ground.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPointOfInterestRelocationTest {
    private static final String TERRAIN = "Plains";
    private static final String OCEAN = "Sea";

    private static final String OCCUPYING_TYPE_ID = "UnitTestRelocationOccupying";
    private static final String LAND_ONLY_TYPE_ID = "UnitTestRelocationLandOnly";
    private static final String WATER_TOLERANT_TYPE_ID = "UnitTestRelocationWaterTolerant";
    private static final String CITY_AVOIDING_OCCUPYING_TYPE_ID = "UnitTestRelocationCityAvoidingOccupying";
    private static final String CITY_AVOIDING_TYPE_ID = "UnitTestRelocationCityAvoiding";

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void registerDefinitions() {
        registerDefinition(OCCUPYING_TYPE_ID, true, true);
        registerDefinition(LAND_ONLY_TYPE_ID, false, true);
        registerDefinition(WATER_TOLERANT_TYPE_ID, false, false);
        registerDefinition(CITY_AVOIDING_OCCUPYING_TYPE_ID, true, true);
        StratConPointOfInterestDefinitions.getDefinition(CITY_AVOIDING_OCCUPYING_TYPE_ID).setAvoidCities(true);
        registerDefinition(CITY_AVOIDING_TYPE_ID, false, true);
        StratConPointOfInterestDefinitions.getDefinition(CITY_AVOIDING_TYPE_ID).setAvoidCities(true);
    }

    @AfterEach
    void unregisterDefinitions() {
        StratConPointOfInterestDefinitions.unregisterDefinition(OCCUPYING_TYPE_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(LAND_ONLY_TYPE_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(WATER_TOLERANT_TYPE_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(CITY_AVOIDING_OCCUPYING_TYPE_ID);
        StratConPointOfInterestDefinitions.unregisterDefinition(CITY_AVOIDING_TYPE_ID);
    }

    private static void registerDefinition(String typeId, boolean occupiesHex, boolean landOnly) {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(typeId);
        definition.setOccupiesHex(occupiesHex);
        definition.setLandOnly(landOnly);
        StratConPointOfInterestDefinitions.registerDefinition(definition);
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

    private static StratConPointOfInterest addPointOfInterest(StratConTrackState track, String typeId, int x, int y) {
        StratConPointOfInterest pointOfInterest = new StratConPointOfInterest(typeId, new StratConCoords(x, y));
        assertTrue(track.addPointOfInterest(pointOfInterest), "test setup: the point of interest should be placed");
        return pointOfInterest;
    }

    private static Campaign campaign() {
        CampaignOptions options = new CampaignOptions();
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
        return campaign;
    }

    /** A contract on a world held by neither party, so no facility is folded into the road network. */
    private static AbstractContract contract() {
        Faction faction = mock(Faction.class);
        when(faction.isAresConventionsSignatory(anyInt())).thenReturn(false);

        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getEmployerFaction()).thenReturn(faction);
        when(contract.getEnemyFaction()).thenReturn(faction);
        return contract;
    }

    private static boolean resize(StratConTrackState track, int width, int height) {
        return StratConContractInitializer.resizeTrack(track, width, height, contract(), campaign());
    }

    private static boolean isOcean(StratConTrackState track, StratConCoords coords) {
        return StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(coords));
    }

    // Free-hex search and capacity

    @Test
    void unoccupiedCoordsNeverLandOnAnOccupyingPointOfInterest() {
        StratConTrackState track = track(2, 1);
        addPointOfInterest(track, OCCUPYING_TYPE_ID, 0, 0);

        for (int attempt = 0; attempt < 20; attempt++) {
            assertEquals(new StratConCoords(1, 0), StratConContractInitializer.getUnoccupiedCoords(track));
        }
    }

    @Test
    void unoccupiedCoordsMayShareAHexWithANonOccupyingPointOfInterest() {
        StratConTrackState track = track(1, 1);
        addPointOfInterest(track, LAND_ONLY_TYPE_ID, 0, 0);

        assertEquals(new StratConCoords(0, 0), StratConContractInitializer.getUnoccupiedCoords(track));
    }

    @Test
    void occupyingPointsOfInterestCountAgainstFacilityCapacity() {
        // 2x2 dry land holds round(4 * 0.5) = 2 facilities, and two occupying points of interest already fill that.
        StratConTrackState track = track(2, 2);
        addPointOfInterest(track, OCCUPYING_TYPE_ID, 0, 0);
        addPointOfInterest(track, OCCUPYING_TYPE_ID, 1, 1);

        StratConContractInitializer.initializeTrackFacilities(track, 3, ForceAlignment.Allied, false, List.of());

        assertTrue(track.getFacilities().isEmpty());
    }

    // Resize preview

    @Test
    void previewResizeCountsDisplacedPointsOfInterest() {
        StratConTrackState track = track(8, 8);
        addPointOfInterest(track, OCCUPYING_TYPE_ID, 7, 7);
        addPointOfInterest(track, LAND_ONLY_TYPE_ID, 6, 6);
        addPointOfInterest(track, WATER_TOLERANT_TYPE_ID, 1, 1);

        ResizeImpact impact = StratConContractInitializer.previewResize(track, 4, 4);

        assertEquals(2, impact.pointsOfInterest(), "both points of interest outside a 4x4 sector are displaced");
        assertEquals(1, impact.occupyingPointsOfInterest());
        assertEquals(1, impact.displacedOccupants(), "only the occupying one needs a free hex of its own");
        assertFalse(impact.isEmpty());
    }

    @Test
    void previewResizeFreeHexesExcludeOnlyOccupyingPointsOfInterest() {
        StratConTrackState track = track(8, 8);
        addPointOfInterest(track, OCCUPYING_TYPE_ID, 0, 0);
        addPointOfInterest(track, LAND_ONLY_TYPE_ID, 1, 1);

        // 4x4 = 16 hexes, one held by the occupying point of interest.
        assertEquals(15, StratConContractInitializer.previewResize(track, 4, 4).freeHexes());
    }

    @Test
    void resizeIsRefusedWhenAnOccupyingPointOfInterestHasNowhereToGo() {
        StratConTrackState track = track(8, 8);
        track.addFacility(new StratConCoords(0, 0), new StratConFacility());
        addPointOfInterest(track, OCCUPYING_TYPE_ID, 7, 7);

        assertFalse(StratConContractInitializer.previewResize(track, 1, 1).fits());
        assertFalse(resize(track, 1, 1));
        assertEquals(new StratConCoords(7, 7), track.getPointsOfInterest().get(0).getCoords(),
              "a refused resize leaves the point of interest where it was");
    }

    @Test
    void resizeIsNotRefusedForNonOccupyingPointsOfInterest() {
        StratConTrackState track = track(8, 8);
        track.addFacility(new StratConCoords(0, 0), new StratConFacility());
        addPointOfInterest(track, LAND_ONLY_TYPE_ID, 7, 7);

        assertTrue(StratConContractInitializer.previewResize(track, 1, 1).fits(),
              "a point of interest that can share a hex needs no free one");
    }

    // Shrinking

    @Test
    void shrinkMovesAnOccupyingPointOfInterestToAFreeHex() {
        // A 2x2 sector with three hexes holding facilities leaves exactly one free hex to receive it.
        StratConTrackState track = track(8, 8);
        track.addFacility(new StratConCoords(0, 0), new StratConFacility());
        track.addFacility(new StratConCoords(1, 0), new StratConFacility());
        track.addFacility(new StratConCoords(0, 1), new StratConFacility());
        StratConPointOfInterest occupying = addPointOfInterest(track, OCCUPYING_TYPE_ID, 7, 7);

        assertTrue(resize(track, 2, 2));

        assertEquals(new StratConCoords(1, 1), occupying.getCoords());
        assertEquals(List.of(occupying), track.getPointsOfInterest(new StratConCoords(1, 1)));
        assertEquals(3, track.getFacilities().size(), "no facility was disturbed");
    }

    @Test
    void shrinkPullsNonOccupyingPointsOfInterestToTheNearestEdge() {
        StratConTrackState track = track(8, 8);
        StratConPointOfInterest corner = addPointOfInterest(track, WATER_TOLERANT_TYPE_ID, 7, 6);
        StratConPointOfInterest bottom = addPointOfInterest(track, WATER_TOLERANT_TYPE_ID, 2, 7);
        StratConPointOfInterest inside = addPointOfInterest(track, WATER_TOLERANT_TYPE_ID, 1, 1);

        assertTrue(resize(track, 4, 4));

        assertEquals(new StratConCoords(3, 3), corner.getCoords());
        assertEquals(new StratConCoords(2, 3), bottom.getCoords());
        assertEquals(new StratConCoords(1, 1), inside.getCoords(), "a point of interest still inside stays put");
        assertEquals(3, track.getPointsOfInterest().size(), "nothing is discarded with the ground");
    }

    @Test
    void shrinkSendsALandOnlyPointOfInterestAshoreWhenItsEdgeHexIsOcean() {
        StratConTrackState track = track(8, 8);
        track.setTerrainTile(new StratConCoords(3, 3), OCEAN);
        StratConPointOfInterest landOnly = addPointOfInterest(track, LAND_ONLY_TYPE_ID, 7, 7);

        assertTrue(resize(track, 4, 4));

        assertFalse(track.isOutOfBounds(landOnly.getCoords()));
        assertFalse(isOcean(track, landOnly.getCoords()), "a land-only point of interest is not left on water");
    }

    @Test
    void shrinkMovesAnOccupyingPointOfInterestOnlyWhereItsTypeAllows() {
        // Three of the four hexes left are cities, which this type avoids; the fourth is the only place it may go.
        StratConTrackState track = track(8, 8);
        track.addCity(new StratConCoords(0, 0));
        track.addCity(new StratConCoords(1, 0));
        track.addCity(new StratConCoords(0, 1));
        StratConPointOfInterest cityAvoiding = addPointOfInterest(track, CITY_AVOIDING_OCCUPYING_TYPE_ID, 7, 7);

        assertTrue(resize(track, 2, 2));

        assertEquals(new StratConCoords(1, 1), cityAvoiding.getCoords());
    }

    @Test
    void shrinkPullsANonOccupyingPointOfInterestAwayFromAnEdgeHexItsTypeForbids() {
        StratConTrackState track = track(8, 8);
        track.addCity(new StratConCoords(3, 3));
        StratConPointOfInterest cityAvoiding = addPointOfInterest(track, CITY_AVOIDING_TYPE_ID, 7, 7);

        assertTrue(resize(track, 4, 4));

        assertFalse(track.isOutOfBounds(cityAvoiding.getCoords()));
        assertFalse(track.isCity(cityAvoiding.getCoords()), "its nearest edge hex is a city, which its type avoids");
    }

    @Test
    void floodingMovesAnOccupyingPointOfInterestOnlyWhereItsTypeAllows() {
        // A 2x2 sector: one hex floods, two are cities, so the last hex is the only one this type may move to.
        StratConTrackState track = track(2, 2);
        track.addCity(new StratConCoords(1, 0));
        track.addCity(new StratConCoords(0, 1));
        StratConPointOfInterest cityAvoiding = addPointOfInterest(track, CITY_AVOIDING_OCCUPYING_TYPE_ID, 0, 0);
        track.setTerrainTile(new StratConCoords(0, 0), OCEAN);

        StratConContractInitializer.applyTerrainChange(track, contract(), campaign());

        assertEquals(new StratConCoords(1, 1), cityAvoiding.getCoords());
    }

    // Flooding

    @Test
    void floodingMovesAnOccupyingPointOfInterestAshore() {
        StratConTrackState track = track(6, 6);
        StratConCoords flooded = new StratConCoords(2, 2);
        StratConPointOfInterest occupying = addPointOfInterest(track, OCCUPYING_TYPE_ID, 2, 2);
        track.setTerrainTile(flooded, OCEAN);

        StratConContractInitializer.applyTerrainChange(track, contract(), campaign());

        assertNotEquals(flooded, occupying.getCoords());
        assertFalse(isOcean(track, occupying.getCoords()));
        assertTrue(track.getPointsOfInterest(flooded).isEmpty());
    }

    @Test
    void floodingMovesALandOnlyPointOfInterestAshore() {
        StratConTrackState track = track(6, 6);
        StratConCoords flooded = new StratConCoords(2, 2);
        StratConPointOfInterest landOnly = addPointOfInterest(track, LAND_ONLY_TYPE_ID, 2, 2);
        track.setTerrainTile(flooded, OCEAN);

        StratConContractInitializer.applyTerrainChange(track, contract(), campaign());

        assertFalse(isOcean(track, landOnly.getCoords()));
    }

    @Test
    void floodingLeavesAWaterTolerantPointOfInterestInPlaceWhileItsFacilityMoves() {
        StratConTrackState track = track(6, 6);
        StratConCoords flooded = new StratConCoords(2, 2);
        track.addFacility(flooded, new StratConFacility());
        StratConPointOfInterest waterTolerant = addPointOfInterest(track, WATER_TOLERANT_TYPE_ID, 2, 2);
        track.setTerrainTile(flooded, OCEAN);

        StratConContractInitializer.applyTerrainChange(track, contract(), campaign());

        assertNull(track.getFacility(flooded), "the facility is carried ashore");
        assertEquals(flooded, waterTolerant.getCoords(), "the point of interest belongs to the ground, not the base");
    }
}
