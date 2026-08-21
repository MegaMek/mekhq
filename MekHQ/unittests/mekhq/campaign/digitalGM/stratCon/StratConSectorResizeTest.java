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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer.ResizeImpact;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorCountMethod;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for GM sector resizing ({@link StratConContractInitializer#resizeTrack}): ground outside the new bounds is
 * discarded, but its occupants are moved back inside rather than destroyed.
 */
class StratConSectorResizeTest {

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static final String TERRAIN = "Plains";
    private static final String OCEAN = "Sea";

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

    private static Campaign campaign() {
        CampaignOptions options = new CampaignOptions();
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
        return campaign;
    }

    /** A campaign with improved sizing on, which is what enables shape profiles (and so the regeneration re-roll). */
    private static Campaign improvedCampaign() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD, StratConSectorCountMethod.ALTERNATE);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
        return campaign;
    }

    /**
     * A contract on a world held by neither party, so no facility is folded into the road network. The factions are
     * stubbed because regeneration consults them for the Ares Conventions before it generates anything.
     */
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

    @Test
    void resize_isRefusedWhenThereIsNowhereToPutTheDisplaced() {
        // A 1x1 sector has one hex, and the surviving facility already occupies it - so the other two have nowhere to
        // go. Squeezing them out would destroy them, so the resize is refused instead.
        StratConTrackState track = track(8, 8);
        track.addFacility(new StratConCoords(0, 0), new StratConFacility());
        track.addFacility(new StratConCoords(7, 7), new StratConFacility());
        track.addFacility(new StratConCoords(6, 6), new StratConFacility());

        assertFalse(StratConContractInitializer.previewResize(track, 1, 1).fits());
        assertFalse(resize(track, 1, 1), "an impossible resize should be refused");
    }

    @Test
    void refusedResize_leavesTheSectorCompletelyUntouched() {
        StratConTrackState track = track(8, 8);
        track.addFacility(new StratConCoords(0, 0), new StratConFacility());
        track.addFacility(new StratConCoords(7, 7), new StratConFacility());
        track.addFacility(new StratConCoords(6, 6), new StratConFacility());

        resize(track, 1, 1);

        assertEquals(8, track.getWidth(), "a refused resize must not change the width");
        assertEquals(8, track.getHeight(), "a refused resize must not change the height");
        assertEquals(3, track.getFacilities().size(), "a refused resize must not lose facilities");
        assertFalse(track.getTerrainTile(new StratConCoords(7, 7)).isEmpty(),
              "a refused resize must not trim terrain");
    }

    @Test
    void noOccupantIsEverLeftOutsideTheSector() {
        // The defect this guards: relocation used to give up when it ran out of room, leaving facilities keyed at
        // coordinates outside the sector - invisible and unreachable, but still counted.
        StratConTrackState track = track(8, 8);
        for (int x = 4; x < 8; x++) {
            track.addFacility(new StratConCoords(x, 7), new StratConFacility());
        }

        assertTrue(resize(track, 5, 5), "a 5x5 sector has room for four displaced facilities");

        for (StratConCoords coords : track.getFacilities().keySet()) {
            assertFalse(track.isOutOfBounds(coords), "facility left outside the sector at " + coords);
        }
        for (StratConCoords coords : track.getScenarios().keySet()) {
            assertFalse(track.isOutOfBounds(coords), "scenario left outside the sector at " + coords);
        }
        assertEquals(4, track.getFacilities().size(), "no facility should have been lost");
    }

    @Test
    void theCallerLaysTheRoadsThatGenerationNoLongerDoes() {
        // StratConSectorGenerator.generate() stopped laying roads, because the network has to span facilities seeded
        // after it returns. This pins the other half of that bargain: connectFacilitiesToRoads must actually build one,
        // or every sector would come back road-free and nothing would say so.
        StratConTrackState track = track(12, 12);
        track.addCity(new StratConCoords(1, 1));
        track.addCity(new StratConCoords(10, 10));

        StratConContractInitializer.connectFacilitiesToRoads(track, contract(), improvedCampaign());

        assertFalse(track.getRoads().isEmpty(), "the caller should lay a road network connecting the sector's cities");
        assertTrue(track.getRoads().contains(new StratConCoords(1, 1)),
              "each city should sit on the network it is connected by");
        assertTrue(track.getRoads().contains(new StratConCoords(10, 10)));
    }

    @Test
    void regeneration_reRollsTheSectorShape() {
        // Regeneration used to leave width and height exactly as the contract first set them, so every regenerate
        // handed back the same proportions. It now re-rolls the shape while preserving the sector's area.
        Set<String> shapes = new HashSet<>();
        for (int roll = 0; roll < 60; roll++) {
            StratConTrackState track = track(12, 12);
            StratConContractInitializer.regenerateTrack(track, contract(), improvedCampaign());
            shapes.add(track.getWidth() + "x" + track.getHeight());
        }

        assertTrue(shapes.size() > 1, "regeneration always produced the same shape: " + shapes);
    }

    @Test
    void regeneration_keepsRoughlyTheSameAmountOfSector() {
        // Re-rolling the shape must not quietly grow or shrink the sector: regeneration changes the terrain and
        // climate, not how much ground there is to fight over.
        for (int roll = 0; roll < 30; roll++) {
            StratConTrackState track = track(12, 12);
            StratConContractInitializer.regenerateTrack(track, contract(), improvedCampaign());

            int area = track.getSize();
            assertTrue((area >= (144 * 0.7)) && (area <= (144 * 1.3)),
                  "regeneration changed the sector's area to " + area + " (" + track.getWidth() + 'x' +
                        track.getHeight() + ')');
        }
    }

    @Test
    void previewResize_reportsRemainingCapacity() {
        StratConTrackState track = track(8, 8);
        track.addFacility(new StratConCoords(0, 0), new StratConFacility());

        // 4x4 = 16 hexes, one of them holding the surviving facility.
        assertEquals(15, StratConContractInitializer.previewResize(track, 4, 4).freeHexes());
    }

    @Test
    void previewResize_countsWhatWouldBeDisplaced() {
        StratConTrackState track = track(8, 8);
        track.addFacility(new StratConCoords(7, 7), new StratConFacility());
        track.assignForce(42, new StratConCoords(6, 6), LocalDate.of(3151, 1, 1), false);

        ResizeImpact impact = StratConContractInitializer.previewResize(track, 4, 4);

        assertEquals(1, impact.facilities(), "the corner facility falls outside a 4x4 sector");
        assertEquals(1, impact.forces(), "the deployed force falls outside a 4x4 sector");
        assertFalse(impact.isEmpty());
    }

    @Test
    void previewResize_reportsNothingWhenGrowing() {
        StratConTrackState track = track(6, 6);
        track.addFacility(new StratConCoords(5, 5), new StratConFacility());

        assertTrue(StratConContractInitializer.previewResize(track, 10, 10).isEmpty(),
              "growing a sector displaces nothing");
    }

    @Test
    void shrink_dropsGroundOutsideTheNewBounds() {
        StratConTrackState track = track(8, 8);
        track.addCity(new StratConCoords(7, 7));

        resize(track, 4, 4);

        assertEquals(4, track.getWidth());
        assertEquals(4, track.getHeight());
        assertTrue(track.getTerrainTile(new StratConCoords(7, 7)).isEmpty(), "terrain outside the sector should go");
        assertFalse(track.isCity(new StratConCoords(7, 7)), "a city outside the sector should go");
    }

    @Test
    void shrink_movesFacilitiesBackInsideRatherThanDestroyingThem() {
        StratConTrackState track = track(8, 8);
        StratConFacility facility = new StratConFacility();
        facility.setDisplayableName("Test Base");
        track.addFacility(new StratConCoords(7, 7), facility);

        resize(track, 4, 4);

        assertEquals(1, track.getFacilities().size(), "the facility should survive the resize");
        StratConCoords moved = track.getFacilities().keySet().iterator().next();
        assertFalse(track.isOutOfBounds(moved), "the facility should have been moved inside the new bounds");
    }

    @Test
    void shrink_keepsAFacilityAndItsScenarioOnTheSameHex() {
        // A facility scenario is created on its facility's hex, and capture/destruction later resolves the facility
        // from the scenario's coords. Relocating the two separately would split the pair and break that resolution, so
        // a displaced facility and its co-located scenario must land on one hex.
        StratConTrackState track = track(8, 8);
        StratConCoords shared = new StratConCoords(7, 7);
        track.addFacility(shared, new StratConFacility());
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(shared);
        track.addScenario(scenario);

        resize(track, 4, 4);

        StratConCoords movedScenario = scenario.getCoords();
        assertFalse(track.isOutOfBounds(movedScenario), "the scenario should have been moved inside the new bounds");
        assertNotNull(track.getFacility(movedScenario),
              "the facility must stay on the scenario's hex so the battle can still resolve it");
        assertEquals(1, track.getFacilities().size(), "the facility should not have been duplicated or lost");
        assertEquals(1, track.getScenarios().size(), "the scenario should not have been duplicated or lost");
    }

    @Test
    void shrink_recallsForcesLeftOutside() {
        StratConTrackState track = track(8, 8);
        track.assignForce(42, new StratConCoords(7, 7), LocalDate.of(3151, 1, 1), false);

        resize(track, 4, 4);

        assertFalse(track.getAssignedForceCoords().containsKey(42), "a force left off the map should be recalled");
    }

    @Test
    void flooding_carriesADeployedForceAshoreWithItsFacility() {
        // The parallel of shrink_movesFacilitiesBackInsideRatherThanDestroyingThem: a flooded facility is relocated,
        // and the force garrisoning it must travel with it rather than be left at sea.
        StratConTrackState track = track(8, 8);
        StratConCoords flooded = new StratConCoords(3, 3);
        track.addFacility(flooded, new StratConFacility());
        track.assignForce(42, flooded, LocalDate.of(3151, 1, 1), false);
        track.setTerrainTile(flooded, OCEAN);

        StratConContractInitializer.applyTerrainChange(track, contract(), campaign());

        StratConCoords moved = track.getAssignedForceCoords().get(42);
        assertNotNull(moved, "a force on a flooded facility should stay deployed, not be dropped");
        assertFalse(StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(moved)),
              "a force was left standing on water at " + moved);
        assertTrue(track.getFacilities().containsKey(moved),
              "the force should have travelled with the facility it garrisoned");
    }

    @Test
    void flooding_keepsAFacilityAndItsScenarioOnTheSameHex() {
        // The ocean parallel of shrink_keepsAFacilityAndItsScenarioOnTheSameHex: when their shared hex floods, the
        // facility and its co-located scenario must be carried ashore together, not to two separate hexes.
        StratConTrackState track = track(8, 8);
        StratConCoords shared = new StratConCoords(3, 3);
        track.addFacility(shared, new StratConFacility());
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(shared);
        track.addScenario(scenario);
        track.setTerrainTile(shared, OCEAN);

        StratConContractInitializer.applyTerrainChange(track, contract(), campaign());

        StratConCoords movedScenario = scenario.getCoords();
        assertFalse(StratConBiomeManifest.isOceanTerrain(track.getTerrainTile(movedScenario)),
              "the scenario should have been moved off the water");
        assertNotNull(track.getFacility(movedScenario),
              "the facility must stay on the scenario's hex so the battle can still resolve it");
        assertEquals(1, track.getFacilities().size(), "the facility should not have been duplicated or lost");
        assertEquals(1, track.getScenarios().size(), "the scenario should not have been duplicated or lost");
    }

    @Test
    void flooding_recallsAForceLeftStandingOnOpenWater() {
        // Nothing to carry this one ashore, so it comes home rather than being stranded at sea.
        StratConTrackState track = track(8, 8);
        StratConCoords flooded = new StratConCoords(5, 5);
        track.assignForce(42, flooded, LocalDate.of(3151, 1, 1), false);
        track.setTerrainTile(flooded, OCEAN);

        StratConContractInitializer.applyTerrainChange(track, contract(), campaign());

        assertFalse(track.getAssignedForceCoords().containsKey(42),
              "a force on a hex that flooded should be recalled");
        assertFalse(track.getAssignedCoordForces().containsKey(flooded),
              "the flooded hex should not still be listed as holding forces");
    }

    @Test
    void flooding_leavesForcesOnDryGroundAlone() {
        StratConTrackState track = track(8, 8);
        StratConCoords dry = new StratConCoords(1, 1);
        track.assignForce(42, dry, LocalDate.of(3151, 1, 1), false);
        track.setTerrainTile(new StratConCoords(5, 5), OCEAN);

        StratConContractInitializer.applyTerrainChange(track, contract(), campaign());

        assertEquals(dry, track.getAssignedForceCoords().get(42), "a force on dry ground should not be disturbed");
    }

    @Test
    void shrink_keepsForcesThatAreStillInside() {
        StratConTrackState track = track(8, 8);
        track.assignForce(42, new StratConCoords(1, 1), LocalDate.of(3151, 1, 1), false);

        resize(track, 4, 4);

        assertEquals(new StratConCoords(1, 1),
              track.getAssignedForceCoords().get(42),
              "a force still inside the sector should stay where it is");
    }

    @Test
    void grow_givesEveryNewHexTerrain() {
        StratConTrackState track = track(4, 4);

        resize(track, 8, 6);

        assertEquals(8, track.getWidth());
        assertEquals(6, track.getHeight());
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 6; y++) {
                assertFalse(track.getTerrainTile(new StratConCoords(x, y)).isEmpty(),
                      "new hex " + x + ',' + y + " should have been given terrain");
            }
        }
    }

    @Test
    void grow_leavesExistingHexesOnTheirOriginalCoordinates() {
        // Growing at the right and bottom edges must not shift anything: StratCon hexes sit on a parity-offset grid,
        // so moving a hex to a different column would silently change which hexes it borders.
        StratConTrackState track = track(4, 4);
        track.setTerrainTile(new StratConCoords(2, 2), "Desert");
        track.addCity(new StratConCoords(1, 3));

        resize(track, 9, 7);

        assertEquals("Desert", track.getTerrainTile(new StratConCoords(2, 2)), "existing terrain should not move");
        assertTrue(track.isCity(new StratConCoords(1, 3)), "an existing city should not move");
    }
}
