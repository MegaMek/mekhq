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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConSupplyInterdictionBehavior;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConHexGeometry;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests supply lines: how each side's supply routes are laid and kept, when a facility is cut off, what being cut off
 * costs, cutting a route, and the Interdict Supply order that does it.
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

    /** Brings the routes up to date and checks supply, as the daily step does. */
    private Set<StratConCoords> checkSupply() {
        StratConFacilitySupply.reconcileNetworks(track);
        return StratConFacilitySupply.updateCutOffFacilities(track);
    }

    /** Floods every hex but the road and its far end, so nothing can go around the road. */
    private void floodAllButTheRoad() {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (!track.isRoad(coords) && !coords.equals(roadEnd)) {
                    track.setTerrainTile(coords, "Sea");
                }
            }
        }
    }

    @Nested
    class Routes {
        @Test
        void aFacilityAtTheEndOfARoadLeavingTheSectorIsOnTheSupplyLines() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);

            assertTrue(checkSupply().isEmpty());
            assertTrue(facility.isNetworked());
        }

        @Test
        void aRouteKeepsToTheRoad() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);

            checkSupply();

            assertEquals(List.of(roadEnd, road[2], road[1], road[0]),
                  track.getEnemySupplyNetwork().routeFrom(roadEnd));
        }

        @Test
        void aFacilityOffTheRoadsIsStillJoinedToTheSupplyLines() {
            StratConCoords faraway = new StratConCoords(2, 2);
            StratConFacility facility = place(faraway, ForceAlignment.Opposing, FacilityType.MekBase);

            assertTrue(checkSupply().isEmpty());
            assertTrue(facility.isNetworked());
            assertTrue(track.getEnemySupplyNetwork().isRouted(faraway));
        }

        @Test
        void aFacilityNoRouteCanReachIsCutOff() {
            StratConCoords island = new StratConCoords(2, 2);
            for (StratConCoords neighbor : StratConHexGeometry.neighbors(track, island)) {
                track.setTerrainTile(neighbor, "Sea");
            }
            StratConFacility facility = place(island, ForceAlignment.Opposing, FacilityType.MekBase);

            assertEquals(Set.of(island), checkSupply());
            assertFalse(facility.isNetworked());
        }

        @Test
        void aSupplyDepotLinksItsSidesFacilitiesWithinThreeHexesStraightToItself() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.SupplyDepot);
            StratConCoords nearby = roadEnd.translate(ROAD_DIRECTION).translate(ROAD_DIRECTION)
                                          .translate(ROAD_DIRECTION);
            int otherWay = ROAD_DIRECTION + 2;
            StratConCoords tooFar = roadEnd.translate(otherWay).translate(otherWay).translate(otherWay)
                                          .translate(otherWay);
            place(nearby, ForceAlignment.Opposing, FacilityType.MekBase);
            place(tooFar, ForceAlignment.Opposing, FacilityType.TankBase);

            checkSupply();

            StratConSupplyNetwork network = track.getEnemySupplyNetwork();
            assertEquals(roadEnd, network.nextHex(nearby));
            assertNotEquals(roadEnd, network.nextHex(tooFar));
            assertTrue(network.isRouted(tooFar), "beyond the depot's reach, a route is laid as for any facility");
        }

        @Test
        void aDepotOfTheOtherSideSuppliesNothingOfYours() {
            place(roadEnd, ForceAlignment.Player, FacilityType.SupplyDepot);
            StratConCoords nearby = roadEnd.translate(ROAD_DIRECTION + 1).translate(ROAD_DIRECTION + 1);
            place(nearby, ForceAlignment.Opposing, FacilityType.MekBase);

            checkSupply();

            assertFalse(track.getEnemySupplyNetwork().routeFrom(nearby).contains(roadEnd));
        }

        @Test
        void aCommandCenterIsASourceOfItsOwnSupply() {
            track.getRoadExits().clear();
            StratConCoords commandCenter = road[0].translate(ROAD_DIRECTION + 3);
            place(commandCenter, ForceAlignment.Opposing, FacilityType.CommandCenter);
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);

            assertTrue(checkSupply().isEmpty());
            assertTrue(facility.isNetworked());
            assertTrue(track.getEnemySupplyNetwork().isRoot(commandCenter));
        }

        @Test
        void theEmployersAndThePlayersFacilitiesShareSupplyLines() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            StratConCoords beyond = roadEnd.translate(ROAD_DIRECTION);
            StratConFacility playerFacility = place(beyond, ForceAlignment.Player, FacilityType.TankBase);

            checkSupply();

            assertTrue(playerFacility.isNetworked());
            assertEquals(roadEnd, track.getPlayerSupplyNetwork().nextHex(beyond));
        }

        @Test
        void aSectorWithoutRoadsKeepsNoRoutesAndCutsNothingOff() {
            track.getRoads().clear();
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setNetworked(true);

            assertTrue(checkSupply().isEmpty());
            assertTrue(track.getEnemySupplyNetwork().getLinks().isEmpty());
        }

        @Test
        void aFacilityThatTurnsUpLaterIsJoinedWithoutTouchingTheRoads() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            checkSupply();
            Set<StratConCoords> roadsBefore = new HashSet<>(track.getRoads());

            StratConCoords later = new StratConCoords(3, 15);
            place(later, ForceAlignment.Opposing, FacilityType.TankBase);
            StratConFacilitySupply.processSupply(track, campaign, false);

            assertTrue(track.getEnemySupplyNetwork().isRouted(later));
            assertEquals(roadsBefore, track.getRoads());
        }

        @Test
        void anOldSaveWithNoRoutesHasThemLaidOnItsFirstCheck() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            assertTrue(track.getEnemySupplyNetwork().getLinks().isEmpty());

            StratConFacilitySupply.processSupply(track, campaign, false);

            assertTrue(track.getEnemySupplyNetwork().isRouted(roadEnd));
        }

        @Test
        void rebuildingLaysTheRoutesAfresh() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            track.getEnemySupplyNetwork().addLink(new StratConCoords(1, 1), new StratConCoords(1, 2));

            StratConFacilitySupply.rebuildNetworks(track);

            assertFalse(track.getEnemySupplyNetwork().contains(new StratConCoords(1, 1)));
            assertTrue(track.getEnemySupplyNetwork().isRouted(roadEnd));
        }
    }

    @Nested
    class Blocking {
        @Test
        void aCutAnywhereOnTheRouteCutsOffEverythingBeyondIt() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            StratConCoords beyond = roadEnd.translate(ROAD_DIRECTION);
            place(beyond, ForceAlignment.Opposing, FacilityType.TankBase);
            checkSupply();

            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));

            assertEquals(Set.of(roadEnd, beyond), checkSupply());
        }

        @Test
        void noRouteIsLaidAroundACut() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            checkSupply();

            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));
            checkSupply();

            assertTrue(track.getEnemySupplyNetwork().routeFrom(roadEnd).contains(road[1]));
        }

        @Test
        void aRoadCutDoesNotBlockThePlayersSide() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            checkSupply();

            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));

            assertTrue(checkSupply().isEmpty());
        }

        @Test
        void aRouteGoesAroundAFacilityTheOtherSideTakesOnIt() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            checkSupply();

            place(road[1], ForceAlignment.Player, FacilityType.TankBase);

            assertFalse(checkSupply().contains(roadEnd));
            assertFalse(track.getEnemySupplyNetwork().routeFrom(roadEnd).contains(road[1]));
        }

        @Test
        void aFacilityTheOtherSideTakesWithNoWayAroundCutsOffEverythingBeyondIt() {
            floodAllButTheRoad();
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            checkSupply();

            place(road[1], ForceAlignment.Player, FacilityType.TankBase);

            assertTrue(checkSupply().contains(roadEnd));
        }

        @Test
        void aFacilityThatChangesSidesLeavesItsOldSidesRoutesForItsNewSidesOnes() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            checkSupply();

            facility.setOwner(ForceAlignment.Player);
            assertFalse(facility.isNetworked());
            checkSupply();

            assertFalse(track.getEnemySupplyNetwork().contains(roadEnd));
            assertTrue(track.getPlayerSupplyNetwork().isRouted(roadEnd));
            assertTrue(facility.isNetworked());
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
            when(options.get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(true);
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setGarrison(0);
            track.getCutOffFacilities().add(roadEnd);

            StratConEnemyFacilityActivity.applyMonthlyUpkeep(track, campaign);

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
            StratConCampaignState campaignState = new StratConCampaignState();
            campaignState.addTrack(track);
            campaignState.setSupportPoints(5);
            contract = mock(AbstractContract.class);
            when(contract.getStratConCampaignState()).thenReturn(campaignState);

            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            checkSupply();
            track.getRevealedCoords().add(road[1]);
            track.getRevealedCoords().add(road[2]);
        }

        @Test
        void aScoutedHexOnAnEnemyRouteOffersInterdictSupplyAndAnOpenHexDoesNot() {
            assertTrue(StratConFacilityOperations.getOperationsFor(track, road[1])
                             .contains(FacilityOperation.INTERDICT));

            StratConCoords openHex = new StratConCoords(1, 1);
            track.getRevealedCoords().add(openHex);
            assertFalse(StratConFacilityOperations.getOperationsFor(track, openHex)
                              .contains(FacilityOperation.INTERDICT));
        }

        @Test
        void anUnscoutedHexOnAnEnemyRouteGivesNothingAway() {
            track.getRevealedCoords().remove(road[1]);

            assertFalse(StratConFacilityOperations.getOperationsFor(track, road[1])
                              .contains(FacilityOperation.INTERDICT));
            assertEquals("reason.notSupplyRoute", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract, track, road[1], FacilityOperation.INTERDICT));
        }

        @Test
        void aRoadNoEnemyRouteUsesIsNotAnEnemySupplyRoute() {
            track.getEnemySupplyNetwork().clear();

            assertEquals("reason.notSupplyRoute", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract, track, road[1], FacilityOperation.INTERDICT));
        }

        @Test
        void itCanBeGivenOnlyWhileSupplyLinesAreInUseAndTheRouteIsNotCut() {
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

    private void interdict(StratConCoords coords) {
        track.addPointOfInterest(new StratConPointOfInterest(StratConSupplyInterdictionBehavior.TYPE_ID, coords));
    }

    private int countInterdictions() {
        int count = 0;
        for (StratConPointOfInterest pointOfInterest : track.getPointsOfInterest()) {
            if (StratConSupplyInterdictionBehavior.TYPE_ID.equals(pointOfInterest.getTypeId())) {
                count++;
            }
        }
        return count;
    }

    private static StratConPointOfInterestDefinition interdictionDefinition() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(StratConSupplyInterdictionBehavior.TYPE_ID);
        definition.setDisplayableName("Supply Interdiction");
        definition.setOccupiesHex(true);
        definition.setLandOnly(true);
        definition.setDefaultOwner(ForceAlignment.Opposing);
        definition.setLifespanDays(7);
        definition.setRemoveOnExpiry(true);
        definition.setBehaviorId(StratConSupplyInterdictionBehavior.BEHAVIOR_ID);
        return definition;
    }

    @Nested
    class Interdictions {
        @Test
        void anInterdictionCutsOffThePlayersFacilitiesBeyondIt() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            StratConCoords beyond = roadEnd.translate(ROAD_DIRECTION);
            place(beyond, ForceAlignment.Player, FacilityType.TankBase);
            checkSupply();

            interdict(road[1]);

            assertEquals(Set.of(roadEnd, beyond), checkSupply());
        }

        @Test
        void noRouteIsLaidAroundAnInterdiction() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            checkSupply();

            interdict(road[1]);
            checkSupply();

            assertTrue(track.getPlayerSupplyNetwork().routeFrom(roadEnd).contains(road[1]));
        }

        @Test
        void anInterdictionDoesNotBlockTheEnemy() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            checkSupply();

            interdict(road[1]);

            assertTrue(checkSupply().isEmpty());
        }

        @Test
        void onceTheInterdictionIsGoneTheRouteIsOpenAgain() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            checkSupply();
            StratConPointOfInterest interdiction = new StratConPointOfInterest(
                  StratConSupplyInterdictionBehavior.TYPE_ID, road[1]);
            track.addPointOfInterest(interdiction);
            assertTrue(checkSupply().contains(roadEnd));

            track.removePointOfInterest(interdiction.getId());

            assertTrue(checkSupply().isEmpty());
        }

        @Test
        void anInterdictionCanSitOnlyOnThePlayersRoutesAwayFromFacilitiesAndFormations() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            checkSupply();
            track.assignForce(1, road[2], TODAY, false);

            List<StratConCoords> candidates = StratConFacilitySupply.getInterdictionCandidates(track,
                  interdictionDefinition());

            assertEquals(Set.of(road[0], road[1]), new HashSet<>(candidates));
        }

        @Test
        void theEnemysOwnRoutesAreNoPlaceForAnInterdiction() {
            place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            checkSupply();

            assertTrue(StratConFacilitySupply.getInterdictionCandidates(track, interdictionDefinition()).isEmpty());
        }
    }

    @Nested
    class InterdictionSchedule {
        private StratConCampaignState campaignState;
        private AbstractContract contract;

        @BeforeEach
        void setUpSchedule() {
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(1.0);
            when(options.get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(true);
            when(options.get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION)).thenReturn(false);

            campaignState = new StratConCampaignState();
            campaignState.addTrack(track);
            contract = mock(AbstractContract.class);
            when(contract.getStratConCampaignState()).thenReturn(campaignState);
            when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
            when(contract.getScale()).thenReturn(6);
            when(contract.getEndingDate()).thenReturn(TODAY.plusYears(1));
            when(contract.getName()).thenReturn("Test Contract");

            StratConPointOfInterestDefinitions.registerDefinition(interdictionDefinition());

            place(roadEnd, ForceAlignment.Allied, FacilityType.MekBase);
            checkSupply();
        }

        @AfterEach
        void forgetDefinition() {
            StratConPointOfInterestDefinitions.unregisterDefinition(StratConSupplyInterdictionBehavior.TYPE_ID);
        }

        @Test
        void oneDueTodaySitsAcrossAPlayerSupplyRoute() {
            campaignState.setSupplyInterdictionsScheduledUntil(TODAY.plusDays(10));
            campaignState.getSupplyInterdictionDates().add(TODAY);

            StratConFacilitySupply.processSupplyInterdictions(campaign, contract, campaignState);

            assertEquals(1, countInterdictions());
            StratConPointOfInterest interdiction = track.getPointsOfInterest().getFirst();
            assertTrue(track.getPlayerSupplyNetwork().contains(interdiction.getCoords()));
            assertTrue(campaignState.getSupplyInterdictionDates().isEmpty());
        }

        @Test
        void theyComeAsOftenAsEngineers() {
            StratConFacilitySupply.processSupplyInterdictions(campaign, contract, campaignState);

            int expected = StratConEnemyFacilityActivity.getMonthlyEngineerCount(6, false, 1.0);
            assertEquals(expected, campaignState.getSupplyInterdictionDates().size() + countInterdictions());
            assertEquals(TODAY.plusMonths(1), campaignState.getSupplyInterdictionsScheduledUntil());
        }

        @Test
        void noneAreSentWhileSupplyLinesAreOff() {
            when(options.get(CampaignOption.USE_SUPPLY_LINES)).thenReturn(false);
            campaignState.getSupplyInterdictionDates().add(TODAY);

            StratConFacilitySupply.processSupplyInterdictions(campaign, contract, campaignState);

            assertEquals(0, countInterdictions());
            assertTrue(campaignState.getSupplyInterdictionDates().isEmpty());
        }

        @Test
        void noneAreSentByAnIdleEnemy() {
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(0.0);
            campaignState.getSupplyInterdictionDates().add(TODAY);

            StratConFacilitySupply.processSupplyInterdictions(campaign, contract, campaignState);

            assertEquals(0, countInterdictions());
            assertTrue(campaignState.getSupplyInterdictionDates().isEmpty());
        }

        @Test
        void noneArePlacedByARoutedEnemy() {
            when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.ROUTED);
            campaignState.setSupplyInterdictionsScheduledUntil(TODAY.plusDays(10));
            campaignState.getSupplyInterdictionDates().add(TODAY);

            StratConFacilitySupply.processSupplyInterdictions(campaign, contract, campaignState);

            assertEquals(0, countInterdictions());
            assertTrue(campaignState.getSupplyInterdictionDates().isEmpty());
        }
    }

    /** @return the report for a player side left with no source of supply, read from the bundle as the game does */
    private String unsourcedReport() {
        return getFormattedTextAt("mekhq.resources.StratConEnemyFacilityActivity",
              "report.unsourced",
              track.getDisplayableName());
    }

    @Nested
    class NoSupplySource {
        @BeforeEach
        void holdTheOnlyRoadOut() {
            // The enemy holds the hex where the only road leaves the sector, so it is no source for the player's side.
            place(road[0], ForceAlignment.Opposing, FacilityType.MekBase);
        }

        @Test
        void aPlayerSideWithNoSourceIsCutOffAndWarnedOnce() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.TankBase);

            StratConFacilitySupply.processSupply(track, campaign, false);
            StratConFacilitySupply.processSupply(track, campaign, false);

            assertTrue(StratConFacilitySupply.isCutOff(track, roadEnd));
            assertTrue(track.isPlayerSupplyUnsourced());
            verify(campaign, times(1)).addReport(any(), eq(unsourcedReport()));
        }

        @Test
        void theWarningComesAgainOnlyAfterASourceIsRegainedAndLostAgain() {
            place(roadEnd, ForceAlignment.Allied, FacilityType.TankBase);
            StratConFacilitySupply.processSupply(track, campaign, false);

            StratConCoords commandCenter = new StratConCoords(3, 15);
            place(commandCenter, ForceAlignment.Allied, FacilityType.CommandCenter);
            StratConFacilitySupply.processSupply(track, campaign, false);
            assertFalse(track.isPlayerSupplyUnsourced());
            assertFalse(StratConFacilitySupply.isCutOff(track, roadEnd));

            track.removeFacility(commandCenter);
            StratConFacilitySupply.processSupply(track, campaign, false);

            verify(campaign, times(2)).addReport(any(), eq(unsourcedReport()));
        }

        @Test
        void noWarningWithoutAnyFacilityOfThePlayersSide() {
            StratConFacilitySupply.processSupply(track, campaign, false);

            assertFalse(track.isPlayerSupplyUnsourced());
            verify(campaign, never()).addReport(any(), eq(unsourcedReport()));
        }

        @Test
        void noWarningWhileSupplyLinesAreOff() {
            when(options.get(CampaignOption.USE_SUPPLY_LINES)).thenReturn(false);
            place(roadEnd, ForceAlignment.Allied, FacilityType.TankBase);

            StratConFacilitySupply.processSupply(track, campaign, false);

            verify(campaign, never()).addReport(any(), eq(unsourcedReport()));
        }
    }

    @Test
    void noWarningWhileARoadOutIsOpenToThePlayersSide() {
        place(roadEnd, ForceAlignment.Allied, FacilityType.TankBase);

        StratConFacilitySupply.processSupply(track, campaign, false);

        assertFalse(track.isPlayerSupplyUnsourced());
        verify(campaign, never()).addReport(any(), eq(unsourcedReport()));
    }

    /** @return the report for a facility back on its supply lines, read from the resource bundle as the game does */
    private String reconnectedReport(StratConFacility facility, StratConCoords coords) {
        return getFormattedTextAt("mekhq.resources.StratConEnemyFacilityActivity",
              "report.reconnected",
              facility.getDisplayableName(),
              track.getDisplayableName(),
              coords.toBTString());
    }

    @Nested
    class AwkwardCases {
        @Test
        void aCutOffFacilityThatChangesSidesIsNotReportedAsBackOnItsSupplyLines() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setIntel(StratConFacility.FacilityIntel.LOCATED);
            StratConFacilitySupply.processSupply(track, campaign, false);
            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));
            StratConFacilitySupply.processSupply(track, campaign, false);
            assertTrue(StratConFacilitySupply.isCutOff(track, roadEnd));

            facility.setOwner(ForceAlignment.Player);
            clearInvocations(campaign);
            StratConFacilitySupply.processSupply(track, campaign, false);

            assertFalse(StratConFacilitySupply.isCutOff(track, roadEnd));
            verify(campaign, never()).addReport(any(), eq(reconnectedReport(facility, roadEnd)));
        }

        @Test
        void aFacilityGenuinelyReconnectedIsReported() {
            StratConFacility facility = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setIntel(StratConFacility.FacilityIntel.LOCATED);
            StratConFacilitySupply.processSupply(track, campaign, false);
            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(1)));
            StratConFacilitySupply.processSupply(track, campaign, false);
            assertTrue(StratConFacilitySupply.isCutOff(track, roadEnd));

            when(campaign.getLocalDate()).thenReturn(TODAY.plusDays(1));
            clearInvocations(campaign);
            StratConFacilitySupply.processSupply(track, campaign, false);

            assertFalse(StratConFacilitySupply.isCutOff(track, roadEnd));
            verify(campaign).addReport(any(), eq(reconnectedReport(facility, roadEnd)));
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
            facility.setCondition(FacilityCondition.CRIPPLED);
            track.getRoadCuts().add(new StratConRoadCut(road[1], TODAY.plusDays(5)));

            StratConFacilitySupply.processSupply(track, campaign, true);

            assertTrue(StratConFacilitySupply.isCutOff(track, roadEnd));
            assertEquals(FacilityCondition.CRIPPLED, facility.getCondition());
            assertEquals(facility, track.getFacility(roadEnd));
        }

        @Test
        void onlyCutOffFacilitiesStarve() {
            StratConFacility supplied = place(roadEnd, ForceAlignment.Opposing, FacilityType.MekBase);

            StratConFacilitySupply.processSupply(track, campaign, true);

            assertEquals(FacilityCondition.INTACT, supplied.getCondition());
        }
    }
}
