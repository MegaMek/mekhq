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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer.ResizeImpact;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.mission.AtBContract;
import org.junit.jupiter.api.Test;

/**
 * Tests for GM sector resizing ({@link StratConContractInitializer#resizeTrack}): ground outside the new bounds is
 * discarded, but its occupants are moved back inside rather than destroyed.
 */
class StratConSectorResizeTest {
    private static final String TERRAIN = "Plains";

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
        CampaignOptions options = mock(CampaignOptions.class);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3151, 1, 1));
        return campaign;
    }

    /** A contract on a world held by neither party, so no facility is folded into the road network. */
    private static AtBContract contract() {
        return mock(AtBContract.class);
    }

    private static void resize(StratConTrackState track, int width, int height) {
        StratConContractInitializer.resizeTrack(track, width, height, contract(), campaign());
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
    void shrink_recallsForcesLeftOutside() {
        StratConTrackState track = track(8, 8);
        track.assignForce(42, new StratConCoords(7, 7), LocalDate.of(3151, 1, 1), false);

        resize(track, 4, 4);

        assertFalse(track.getAssignedForceCoords().containsKey(42), "a force left off the map should be recalled");
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
