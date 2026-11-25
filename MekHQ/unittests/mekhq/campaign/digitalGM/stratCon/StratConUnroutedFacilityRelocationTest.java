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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilitySupply;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a newly laid out sector's facilities are moved to where their side's supply can reach them.
 *
 * <p>The sector is a mainland down its left side, with a city and so a road out, and a two-hex island that no road or
 * supply route can reach.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConUnroutedFacilityRelocationTest {
    private static final int SIZE = 10;
    private static final int MAINLAND_WIDTH = 5;
    private static final StratConCoords CITY = new StratConCoords(2, 5);
    private static final StratConCoords ISLAND = new StratConCoords(7, 4);
    private static final StratConCoords ISLAND_NEIGHBOR = new StratConCoords(7, 5);

    private StratConTrackState track;
    private Campaign campaign;
    private CampaignOptions options;
    private AbstractContract contract;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setDisplayableName("Sector Test");
        track.setWidth(SIZE);
        track.setHeight(SIZE);
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                track.setTerrainTile(new StratConCoords(x, y), (x < MAINLAND_WIDTH) ? "Plains" : "Sea");
            }
        }
        track.setTerrainTile(ISLAND, "Plains");
        track.setTerrainTile(ISLAND_NEIGHBOR, "Plains");
        track.addCity(CITY);

        options = new CampaignOptions();
        options.set(CampaignOption.USE_SUPPLY_LINES, true);
        options.set(CampaignOption.USE_STRAT_CON_ALTERNATE_SECTOR_TERRAIN, true);
        campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3050, 3, 1));

        // A contract with no planet, so no facility is folded into the road network
        contract = mock(AbstractContract.class);
    }

    private StratConFacility place(StratConCoords coords, ForceAlignment owner, FacilityType facilityType) {
        StratConFacility facility = StratConTestData.facility(owner, facilityType);
        track.addFacility(coords, facility);
        return facility;
    }

    /** Lays the sector's roads and routes, then moves its facilities, as at the start of a contract. */
    private void layOutSector(Set<StratConFacility> fixedFacilities) {
        StratConContractInitializer.connectFacilitiesToRoads(track, contract, campaign);
        StratConContractInitializer.relocateUnroutedFacilities(track, contract, campaign, fixedFacilities);
    }

    private StratConCoords coordsOf(StratConFacility facility) {
        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            if (entry.getValue() == facility) {
                return entry.getKey();
            }
        }
        throw new AssertionError("The facility is no longer in the sector");
    }

    @Test
    void facilityNoRouteCanReach_isMovedToWhereOneCan() {
        for (ForceAlignment owner : new ForceAlignment[] { ForceAlignment.Allied, ForceAlignment.Opposing }) {
            track.getFacilities().clear();
            StratConFacility facility = place(ISLAND, owner, FacilityType.MekBase);

            layOutSector(Collections.emptySet());

            assertNull(track.getFacility(ISLAND));
            assertTrue(coordsOf(facility).getX() < MAINLAND_WIDTH);
            assertTrue(StratConFacilitySupply.getUnroutedFacilityHexes(track).isEmpty());
            assertTrue(StratConFacilitySupply.updateCutOffFacilities(track).isEmpty());
        }
    }

    @Test
    void movedFacility_takesItsObjectiveAndRevealedHexWithIt() {
        StratConFacility facility = place(ISLAND, ForceAlignment.Allied, FacilityType.MekBase);
        facility.setVisible(true);
        track.getRevealedCoords().add(ISLAND);
        StratConStrategicObjective objective = new StratConStrategicObjective();
        objective.setObjectiveCoords(ISLAND);
        objective.setObjectiveType(StrategicObjectiveType.AlliedFacilityControl);
        track.addStrategicObjective(objective);

        layOutSector(Collections.emptySet());

        StratConCoords destination = coordsOf(facility);
        assertEquals(destination, objective.getObjectiveCoords());
        assertSame(objective, track.getObjectivesByCoords().get(destination));
        assertTrue(track.getRevealedCoords().contains(destination));
    }

    @Test
    void movedHiddenFacility_staysHidden() {
        StratConFacility facility = place(ISLAND, ForceAlignment.Opposing, FacilityType.MekBase);
        facility.setVisible(false);
        track.getRevealedCoords().add(ISLAND);

        layOutSector(Collections.emptySet());

        assertFalse(facility.getVisible());
        assertFalse(track.getRevealedCoords().contains(coordsOf(facility)));
    }

    @Test
    void suppliedFacilities_areNotMoved() {
        StratConCoords mainland = new StratConCoords(4, 1);
        StratConFacility mainlandFacility = place(mainland, ForceAlignment.Allied, FacilityType.MekBase);
        StratConFacility depot = place(ISLAND, ForceAlignment.Allied, FacilityType.SupplyDepot);
        StratConFacility nextToDepot = place(ISLAND_NEIGHBOR, ForceAlignment.Allied, FacilityType.TankBase);

        layOutSector(Collections.emptySet());

        assertEquals(mainland, coordsOf(mainlandFacility));
        assertEquals(ISLAND, coordsOf(depot));
        assertEquals(ISLAND_NEIGHBOR, coordsOf(nextToDepot));
    }

    @Test
    void facilityAlreadyOnTheMap_isNotMoved() {
        StratConFacility existing = place(ISLAND, ForceAlignment.Allied, FacilityType.MekBase);
        StratConFacility added = place(ISLAND_NEIGHBOR, ForceAlignment.Allied, FacilityType.TankBase);

        layOutSector(Set.of(existing));

        assertEquals(ISLAND, coordsOf(existing));
        assertTrue(coordsOf(added).getX() < MAINLAND_WIDTH);
    }

    @Test
    void sideWithNoSourceOfSupply_keepsItsFacilityWhereItIs() {
        StratConFacility facility = place(ISLAND, ForceAlignment.Allied, FacilityType.MekBase);
        StratConContractInitializer.connectFacilitiesToRoads(track, contract, campaign);
        for (StratConCoords roadExit : track.getRoadExits()) {
            place(roadExit, ForceAlignment.Opposing, FacilityType.TankBase);
        }

        layOutSector(Collections.emptySet());

        assertEquals(ISLAND, coordsOf(facility));
    }

    @Test
    void withSupplyLinesOff_nothingIsMoved() {
        options.set(CampaignOption.USE_SUPPLY_LINES, false);
        StratConFacility facility = place(ISLAND, ForceAlignment.Allied, FacilityType.MekBase);

        layOutSector(Collections.emptySet());

        assertEquals(ISLAND, coordsOf(facility));
    }

    @Test
    void suppliableHexes_leaveOutTheSeaTheOtherSidesFacilitiesAndItsRoutes() {
        StratConCoords enemyHex = new StratConCoords(4, 1);
        place(enemyHex, ForceAlignment.Opposing, FacilityType.MekBase);
        StratConContractInitializer.connectFacilitiesToRoads(track, contract, campaign);

        Set<StratConCoords> suppliableHexes = StratConFacilitySupply.getSuppliableHexes(track, true);

        assertFalse(suppliableHexes.isEmpty());
        assertFalse(suppliableHexes.contains(ISLAND));
        assertFalse(suppliableHexes.contains(enemyHex));
        for (StratConCoords coords : suppliableHexes) {
            assertTrue(coords.getX() < MAINLAND_WIDTH);
            assertFalse(StratConFacilitySupply.isOnSupplyNetwork(track, coords, false));
        }
    }

    @Test
    void suppliableHexes_takeInTheReachOfASupplyDepot() {
        place(ISLAND, ForceAlignment.Allied, FacilityType.SupplyDepot);
        StratConContractInitializer.connectFacilitiesToRoads(track, contract, campaign);

        assertTrue(StratConFacilitySupply.getSuppliableHexes(track, true).contains(ISLAND_NEIGHBOR));
        assertFalse(StratConFacilitySupply.getSuppliableHexes(track, false).contains(ISLAND_NEIGHBOR));
    }
}
