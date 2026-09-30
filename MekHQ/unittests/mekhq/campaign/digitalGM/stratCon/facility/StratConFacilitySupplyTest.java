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
package mekhq.campaign.digitalGM.stratCon.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests supply lines: which facilities a side's supply reaches, when a facility is cut off, what being cut off costs,
 * cutting a road, and the Interdict Supply order that does it.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilitySupplyTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 3, 1);
    // A straight road of three hexes leaving the sector at ROAD[0], with a facility hex at its far end.
    private static final StratConCoords ORIGIN = new StratConCoords(10, 10);
    private static final int ROAD_DIRECTION = 1;

    private StratConTrackState track;
    private Campaign campaign;
    private CampaignOptions options;
    private StratConCoords[] road;
    private StratConCoords roadEnd;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setWidth(20);
        track.setHeight(20);

        road = new StratConCoords[3];
        StratConCoords coords = ORIGIN;
        for (int index = 0; index < road.length; index++) {
            road[index] = coords;
            track.getRoads().add(coords);
            coords = coords.translate(ROAD_DIRECTION);
        }
        roadEnd = coords;
        track.getRoadExits().add(road[0]);

        campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.USE_SUPPLY_LINES)).thenReturn(true);
        when(options.get(CampaignOption.USE_STRAT_CON_ALTERNATE_SECTOR_TERRAIN)).thenReturn(true);
        when(options.isUseStratConMaplessMode()).thenReturn(false);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getGUI()).thenReturn(null);
    }

    private StratConFacility place(StratConCoords coords, ForceAlignment owner, FacilityType facilityType) {
        StratConFacility facility = StratConTestData.facility(owner,
              facilityType,
              new LocalModifiersEffect(List.of("MekGarrison.json")));
        track.addFacility(coords, facility);
        return facility;
    }

    @Nested
    class Reach {
        @Test
        void aFacilityAtTheEndOfARoadLeavingTheSectorIsOnTheSupplyLines() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);

            assertTrue(StratConFacilitySupply.updateCutOffFacilities(track).isEmpty());
            assertTrue(facility.isNetworked());
        }

        @Test
        void cuttingTheRoadCutsOffAnEnemyFacilityThatWasOnIt() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            StratConFacilitySupply.updateCutOffFacilities(track);

            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));

            assertEquals(java.util.Set.of(roadEnd), StratConFacilitySupply.updateCutOffFacilities(track));
            assertTrue(facility.isNetworked());
        }

        @Test
        void aRoadCutDoesNotBlockThePlayersSide() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            StratConFacilitySupply.updateCutOffFacilities(track);

            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));

            assertTrue(StratConFacilitySupply.updateCutOffFacilities(track).isEmpty());
        }

        @Test
        void theOtherSidesFacilityOnTheRoadBlocksIt() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            StratConFacilitySupply.updateCutOffFacilities(track);

            track.getRoads().remove(road[1]);
            place(road[1], ForceAlignment.Player, FacilityType.TankBase);

            assertTrue(StratConFacilitySupply.updateCutOffFacilities(track).contains(roadEnd));
        }

        @Test
        void aFacilityNeverOnTheSupplyLinesSuppliesItself() {
            StratConCoords faraway = new StratConCoords(2, 2);
            StratConFacility facility = place(faraway, ForceAlignment.Opposing, FacilityType.MekBase);

            assertTrue(StratConFacilitySupply.updateCutOffFacilities(track).isEmpty());
            assertFalse(facility.isNetworked());
        }

        @Test
        void aSupplyDepotReachesItsSidesFacilitiesWithinThreeHexes() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.SupplyDepot);
            StratConCoords nearby = roadEnd.translate(ROAD_DIRECTION).translate(ROAD_DIRECTION)
                                          .translate(ROAD_DIRECTION);
            // Four hexes out the other way, so no supplied facility stands next to it to pass supply on.
            int otherWay = ROAD_DIRECTION + 2;
            StratConCoords tooFar = roadEnd.translate(otherWay).translate(otherWay).translate(otherWay)
                                          .translate(otherWay);
            StratConFacility reached = place(nearby, ForceAlignment.Opposing, FacilityType.MekBase);
            StratConFacility unreached = place(tooFar, ForceAlignment.Opposing, FacilityType.TankBase);

            StratConFacilitySupply.updateCutOffFacilities(track);

            assertTrue(reached.isNetworked());
            assertFalse(unreached.isNetworked());
        }

        @Test
        void aCommandCenterIsASourceOfItsOwnSupply() {
            track.getRoadExits().clear();
            place(road[0].translate(ROAD_DIRECTION + 3), ForceAlignment.Opposing, FacilityType.CommandCenter);
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);

            StratConFacilitySupply.updateCutOffFacilities(track);

            assertTrue(facility.isNetworked());
        }

        @Test
        void changingSidesTakesAFacilityOffTheSupplyLines() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            StratConFacilitySupply.updateCutOffFacilities(track);

            facility.setOwner(ForceAlignment.Player);

            assertFalse(facility.isNetworked());
        }

        @Test
        void aSectorWithoutRoadsCutsNothingOff() {
            track.getRoads().clear();
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setNetworked(true);

            assertTrue(StratConFacilitySupply.updateCutOffFacilities(track).isEmpty());
        }
    }

    @Nested
    class Effects {
        @Test
        void aCutOffFacilityDecaysAtTheStartOfTheMonth() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            StratConFacilitySupply.processSupply(track, campaign, false);
            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));

            StratConFacilitySupply.processSupply(track, campaign, true);

            assertTrue(StratConFacilitySupply.isCutOff(track, roadEnd));
            assertEquals(FacilityCondition.DAMAGED, facility.getCondition());
        }

        @Test
        void aRoadCutEndsOnItsDay() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY));

            StratConFacilitySupply.processSupply(track, campaign, false);

            assertTrue(track.getRoadCuts().isEmpty());
            assertFalse(StratConFacilitySupply.isCutOff(track, roadEnd));
        }

        @Test
        void withSupplyLinesOffNothingIsCutOff() {
            when(options.get(CampaignOption.USE_SUPPLY_LINES)).thenReturn(false);
            track.getCutOffFacilities().add(roadEnd);

            StratConFacilitySupply.processSupply(track, campaign, true);

            assertTrue(track.getCutOffFacilities().isEmpty());
        }

        @Test
        void theLegacyGeneratorNeverUsesSupplyLines() {
            when(options.get(CampaignOption.USE_STRAT_CON_ALTERNATE_SECTOR_TERRAIN)).thenReturn(false);

            assertFalse(StratConFacilitySupply.isSupplyLinesActive(campaign));
        }

        @Test
        void aCutOffEnemyFacilityGetsNoUpkeep() {
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(1.0);
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setGarrison(0);
            track.getCutOffFacilities().add(roadEnd);

            StratConEnemyFacilityActivity.applyWeeklyUpkeep(track, campaign);

            assertEquals(0, facility.getGarrison());
        }

        @Test
        void cuttingARoadLastsFourWeeks() {
            StratConFacilitySupply.cutRoad(campaign, track, road[1]);

            assertTrue(track.isRoadCut(road[1]));
            assertEquals(TODAY.plusDays(StratConFacilitySupply.ROAD_CUT_DAYS),
                  track.getRoadCuts().getFirst().getEndDate());
        }
    }

    @Nested
    class InterdictOrder {
        private AbstractContract contract;

        @BeforeEach
        void setUpContract() {
            mekhq.campaign.digitalGM.stratCon.StratConCampaignState campaignState =
                  new mekhq.campaign.digitalGM.stratCon.StratConCampaignState();
            campaignState.addTrack(track);
            campaignState.setSupportPoints(5);
            contract = mock(AbstractContract.class);
            when(contract.getStratConCampaignState()).thenReturn(campaignState);
        }

        @Test
        void aRoadHexOffersInterdictSupplyAndAnOpenHexDoesNot() {
            assertTrue(StratConFacilityOperations.getOperationsFor(track, road[1])
                             .contains(FacilityOperation.INTERDICT));
            assertFalse(StratConFacilityOperations.getOperationsFor(track, new StratConCoords(1, 1))
                              .contains(FacilityOperation.INTERDICT));
        }

        @Test
        void itCanBeGivenOnARoadOnlyWhileSupplyLinesAreInUseAndTheRoadIsNotCut() {
            assertNull(StratConFacilityOperations.getUnavailableReasonKey(campaign, contract, track, road[1],
                  FacilityOperation.INTERDICT));

            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));
            assertEquals("reason.alreadyCut", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract, track, road[1], FacilityOperation.INTERDICT));

            when(options.get(CampaignOption.USE_SUPPLY_LINES)).thenReturn(false);
            assertEquals("reason.supplyLinesOff", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract, track, road[2], FacilityOperation.INTERDICT));
        }
    }

    @Nested
    class AwkwardCases {
        @Test
        void aCutOffFacilityThatChangesSidesIsNotReportedAsBackOnItsSupplyLines() {
            StratConCoords offRoad = roadEnd.translate(ROAD_DIRECTION).translate(ROAD_DIRECTION);
            StratConFacility facility = place(offRoad, ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setNetworked(true);
            facility.setIntel(StratConFacility.FacilityIntel.LOCATED);
            StratConFacilitySupply.processSupply(track, campaign, false);
            assertTrue(StratConFacilitySupply.isCutOff(track, offRoad));

            facility.setOwner(ForceAlignment.Player);
            clearInvocations(campaign);
            StratConFacilitySupply.processSupply(track, campaign, false);

            assertFalse(StratConFacilitySupply.isCutOff(track, offRoad));
            verify(campaign, never()).addReport(any(), contains("back on its supply lines"));
        }

        @Test
        void aFacilityGenuinelyReconnectedIsReported() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase).setIntel(
                  StratConFacility.FacilityIntel.LOCATED);
            StratConFacilitySupply.processSupply(track, campaign, false);
            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(1)));
            StratConFacilitySupply.processSupply(track, campaign, false);
            assertTrue(StratConFacilitySupply.isCutOff(track, roadEnd));

            when(campaign.getLocalDate()).thenReturn(TODAY.plusDays(1));
            clearInvocations(campaign);
            StratConFacilitySupply.processSupply(track, campaign, false);

            assertFalse(StratConFacilitySupply.isCutOff(track, roadEnd));
            verify(campaign).addReport(any(), contains("back on its supply lines"));
        }

        @Test
        void cuttingARoadAlreadyCutStartsItsTimeAgainRatherThanAddingASecondCut() {
            StratConFacilitySupply.cutRoad(campaign, track, road[1]);
            when(campaign.getLocalDate()).thenReturn(TODAY.plusDays(10));

            StratConFacilitySupply.cutRoad(campaign, track, road[1]);

            assertEquals(1, track.getRoadCuts().size());
            assertEquals(TODAY.plusDays(10 + StratConFacilitySupply.ROAD_CUT_DAYS),
                  track.getRoadCuts().getFirst().getEndDate());
        }

        @Test
        void starvingNeverDestroysAFacility() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setNetworked(true);
            facility.setCondition(FacilityCondition.CRIPPLED);
            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));

            StratConFacilitySupply.processSupply(track, campaign, true);

            assertEquals(FacilityCondition.CRIPPLED, facility.getCondition());
            assertEquals(facility, track.getFacility(roadEnd));
        }

        @Test
        void onlyCutOffFacilitiesStarve() {
            StratConFacility supplied = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);

            StratConFacilitySupply.processSupply(track, campaign, true);

            assertEquals(FacilityCondition.INTACT, supplied.getCondition());
        }

        @Test
        void theEmployersAndThePlayersFacilitiesShareSupplyLines() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            StratConCoords beyond = roadEnd.translate(ROAD_DIRECTION);
            StratConFacility playerFacility = place(beyond, ForceAlignment.Player, FacilityType.TankBase);

            StratConFacilitySupply.updateCutOffFacilities(track);

            assertTrue(playerFacility.isNetworked());
        }

        @Test
        void aDepotOfTheOtherSideSuppliesNothingOfYours() {
            place(roadEnd, ForceAlignment.Player, FacilityType.SupplyDepot);
            StratConCoords nearby = roadEnd.translate(ROAD_DIRECTION + 1).translate(ROAD_DIRECTION + 1);
            StratConFacility enemy = place(nearby, ForceAlignment.Opposing, FacilityType.MekBase);

            StratConFacilitySupply.updateCutOffFacilities(track);

            assertFalse(enemy.isNetworked());
        }
    }
}
