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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests sieges: who can give the order, the weekly garrison loss and its cost, surrender, the fights that keep or
 * break a siege, and lifting one.
 *
 * <p>Every besieged facility here is cut off from its supply lines, so neither a sortie nor a relief force is ever
 * rolled, and no scenario has to be generated.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilitySiegeTest {
    private static final LocalDate TODAY = LocalDate.of(3050, 3, 6);
    private static final StratConCoords FACILITY_COORDS = new StratConCoords(10, 10);
    private static final int FORMATION_ID = 7;
    private static final int SECOND_FORMATION_ID = 8;
    private static final int THIRD_FORMATION_ID = 9;

    private StratConTrackState track;
    private Campaign campaign;
    private CampaignOptions options;
    private StratConCampaignState campaignState;
    private AbstractContract contract;
    private StratConFacility facility;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        track = new StratConTrackState();
        track.setWidth(20);
        track.setHeight(20);

        campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        campaignState.setSupportPoints(5);
        contract = mock(AbstractContract.class);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);

        campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(true);
        when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(0.0);
        when(options.get(CampaignOption.USE_FATIGUE)).thenReturn(false);
        when(options.get(CampaignOption.FATIGUE_RATE)).thenReturn(1);
        when(options.isUseStratConMaplessMode()).thenReturn(false);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getGUI()).thenReturn(null);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        for (int formationId : List.of(FORMATION_ID, SECOND_FORMATION_ID, THIRD_FORMATION_ID)) {
            when(campaign.getPlayerForce().getFormation(formationId).isDeployed()).thenReturn(false);
        }

        facility = StratConTestData.facility(ForceAlignment.Opposing,
              FacilityType.MekBase,
              new LocalModifiersEffect(List.of("MekGarrison.json")));
        // A Stronghold's garrison is large enough to take two steps without surrendering.
        facility.setTier(FacilityTier.STRONGHOLD);
        track.addFacility(FACILITY_COORDS, facility);
        // Cut off, so the garrison can neither sortie nor be relieved.
        track.getCutOffFacilities().add(FACILITY_COORDS);
    }

    /** Starts a siege a full week ago, so this Monday's step charges and counts it. */
    private void besiege(int formationId, int direction) {
        besiege(formationId, direction, TODAY.minusDays(StratConFacilitySiege.SIEGE_FIRST_WEEK_DAYS));
    }

    private void besiege(int formationId, int direction, LocalDate startDate) {
        track.assignForce(formationId, FACILITY_COORDS.translate(direction), TODAY, false);
        track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.SIEGE,
              formationId,
              FACILITY_COORDS,
              startDate,
              null));
        track.addStickyForce(formationId);
    }

    /**
     * Puts a fight over the siege on a besieger's hex, with that formation in it, as a sortie or relief force would
     * be.
     */
    private StratConScenario siegeFight(int formationId, boolean isSortie) {
        List<Integer> forceIds = new ArrayList<>(List.of(formationId));
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getId()).thenReturn(99);
        when(backingScenario.getForceIDs()).thenReturn(forceIds);

        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(track.getAssignedForceCoords().get(formationId));
        scenario.setBackingScenario(backingScenario);
        scenario.setFacilityOperation(FacilityOperation.SIEGE);
        scenario.setSiegeCoords(FACILITY_COORDS);
        scenario.setSiegeSortie(isSortie);
        track.addScenario(scenario);
        return scenario;
    }

    @Nested
    class Order {
        @Test
        void anEnemyFacilityOffersSiegeButOneYourSideHoldsDoesNot() {
            assertTrue(StratConFacilityOperations.getOperationsFor(track, FACILITY_COORDS)
                             .contains(FacilityOperation.SIEGE));

            facility.setOwner(ForceAlignment.Player);

            assertFalse(StratConFacilityOperations.getOperationsFor(track, FACILITY_COORDS)
                              .contains(FacilityOperation.SIEGE));
        }

        @Test
        void onlyAFormationNextToTheFacilityCanBesiegeIt() {
            assertTrue(StratConFacilityOperations.isInReach(FACILITY_COORDS.translate(2),
                  FACILITY_COORDS,
                  FacilityOperation.SIEGE));
            assertFalse(StratConFacilityOperations.isInReach(FACILITY_COORDS,
                  FACILITY_COORDS,
                  FacilityOperation.SIEGE));
        }

        @Test
        void givingTheOrderPaysTheFirstWeekAndHoldsTheFormation() {
            track.assignForce(FORMATION_ID, FACILITY_COORDS.translate(0), TODAY, false);

            assertTrue(StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  FacilityOperation.SIEGE,
                  null));

            assertEquals(5 - StratConFacilityOperations.SIEGE_COST, campaignState.getSupportPoints());
            assertTrue(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
            assertTrue(track.getStickyForces().contains(FORMATION_ID));
        }

        @Test
        void aSiegeHasNoSetEndAndIsKeptWhileTheFormationStaysInReach() {
            besiege(FORMATION_ID, 0);
            when(campaign.getLocalDate()).thenReturn(TODAY.plusDays(60));

            StratConFacilityOperations.processOrders(track, campaign);

            assertTrue(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void aSiegeIsAbandonedWhenTheFormationLeaves() {
            besiege(FORMATION_ID, 0);
            track.unassignFormation(FORMATION_ID);

            StratConFacilityOperations.processOrders(track, campaign);

            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
            assertFalse(track.getStickyForces().contains(FORMATION_ID));
        }

        @Test
        void liftingASiegeEndsItAndFreesTheFormation() {
            besiege(FORMATION_ID, 0);

            assertTrue(StratConFacilitySiege.liftSiege(campaign, track, FORMATION_ID));

            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
            assertFalse(track.getStickyForces().contains(FORMATION_ID));
            assertFalse(StratConFacilitySiege.liftSiege(campaign, track, FORMATION_ID));
        }
    }

    @Nested
    class Weekly {
        @Test
        void eachFormationTakesAGarrisonStepUpToTwo() {
            assertEquals(0, StratConFacilitySiege.getWeeklyGarrisonLoss(0));
            assertEquals(1, StratConFacilitySiege.getWeeklyGarrisonLoss(1));
            assertEquals(2, StratConFacilitySiege.getWeeklyGarrisonLoss(2));
            assertEquals(2, StratConFacilitySiege.getWeeklyGarrisonLoss(3));
        }

        @Test
        void aWeekOfSiegeCostsAGarrisonStepAndASupportPointPerFormation() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.processSieges(track, campaign);

            assertEquals(garrison - 1, facility.getGarrison());
            assertEquals(5 - StratConFacilitySiege.SIEGE_WEEKLY_COST, campaignState.getSupportPoints());
        }

        @Test
        void aSiegeUnderAWeekOldIsNeitherChargedNorCounted() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();
            besiege(FORMATION_ID, 0, TODAY.minusDays(1));

            StratConFacilitySiege.processSieges(track, campaign);

            assertEquals(garrison, facility.getGarrison());
            assertEquals(5, campaignState.getSupportPoints());
            assertTrue(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void threeFormationsStillTakeOnlyTwoSteps() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();
            besiege(FORMATION_ID, 0);
            besiege(SECOND_FORMATION_ID, 1);
            besiege(THIRD_FORMATION_ID, 2);

            StratConFacilitySiege.processSieges(track, campaign);

            assertEquals(garrison - 2, facility.getGarrison());
            assertEquals(5 - (3 * StratConFacilitySiege.SIEGE_WEEKLY_COST), campaignState.getSupportPoints());
        }

        @Test
        void aFormationThePlayerCannotPayForLiftsItsSiege() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();
            campaignState.setSupportPoints(0);
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.processSieges(track, campaign);

            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
            assertEquals(garrison, facility.getGarrison());
        }

        @Test
        void aBesiegedFacilityGetsNoUpkeep() {
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(1.0);
            track.getCutOffFacilities().clear();
            facility.setGarrison(0);
            besiege(FORMATION_ID, 0);

            StratConEnemyFacilityActivity.applyWeeklyUpkeep(track, campaign);

            assertEquals(0, facility.getGarrison());
        }

        @Test
        void aCutOffBesiegedFacilitySurrendersWithoutAFight() {
            facility.setGarrison(1);
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.processSieges(track, campaign);

            assertEquals(ForceAlignment.Player, facility.getOwner());
            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
            assertTrue(track.getScenarios().isEmpty());
            assertFalse(track.getStickyForces().contains(FORMATION_ID));
        }

        @Test
        void aCutOffGarrisonNeverSorties() {
            facility.setGarrison(facility.getGarrisonMaximum());

            assertNull(StratConFacilitySiege.trySortie(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  List.of(FORMATION_ID),
                  0));
        }

        @Test
        void aCutOffFacilityIsNeverRelieved() {
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(3.0);
            besiege(FORMATION_ID, 0);

            assertNull(StratConFacilitySiege.tryRelief(campaign, contract, track, FACILITY_COORDS, FORMATION_ID));
        }
    }

    @Nested
    class Fights {
        @Test
        void losingASortieBreaksEverySiegeOnTheFacility() {
            besiege(FORMATION_ID, 0);
            besiege(SECOND_FORMATION_ID, 1);

            StratConFacilitySiege.resolveSiegeScenario(campaign, contract, track, FACILITY_COORDS, true, false);

            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void losingToAReliefForceBreaksTheSiege() {
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.resolveSiegeScenario(campaign, contract, track, FACILITY_COORDS, false, false);

            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void winningASortieCostsTheGarrisonAStep() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.resolveSiegeScenario(campaign, contract, track, FACILITY_COORDS, true, true);

            assertEquals(garrison - 1, facility.getGarrison());
            assertTrue(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void beatingAReliefForceKeepsTheSiegeAndChangesNothingElse() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.resolveSiegeScenario(campaign, contract, track, FACILITY_COORDS, false, true);

            assertEquals(garrison, facility.getGarrison());
            assertTrue(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void winningASortieAgainstTheLastOfTheGarrisonTakesTheFacility() {
            facility.setGarrison(1);
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.resolveSiegeScenario(campaign, contract, track, FACILITY_COORDS, true, true);

            assertEquals(ForceAlignment.Player, facility.getOwner());
            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void aFightOverASiegeAlreadyOverChangesNothing() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();

            StratConFacilitySiege.resolveSiegeScenario(campaign, contract, track, FACILITY_COORDS, true, true);

            assertEquals(garrison, facility.getGarrison());
        }
    }

    @Nested
    class AwkwardCases {
        @Test
        void aFormationOnTheFacilityItselfCannotBesiegeIt() {
            track.assignForce(FORMATION_ID, FACILITY_COORDS, TODAY, false);

            assertFalse(StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  FacilityOperation.SIEGE,
                  null));
            assertEquals(5, campaignState.getSupportPoints());
            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void aSiegeCannotBeOrderedWithoutTheSupportForIt() {
            campaignState.setSupportPoints(StratConFacilityOperations.SIEGE_COST - 1);

            assertEquals("reason.supportPoints", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityOperation.SIEGE));
        }

        @Test
        void aSiegeCannotBeOrderedWhileAFightIsUnderWayOnTheFacility() {
            StratConScenario scenario = new StratConScenario();
            scenario.setCoords(FACILITY_COORDS);
            track.getScenarios().put(FACILITY_COORDS, scenario);

            assertEquals("reason.scenarioUnderway", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityOperation.SIEGE));
        }

        @Test
        void aBesiegingFormationCannotTakeAnotherOrder() {
            besiege(FORMATION_ID, 0);

            assertTrue(StratConFacilityOperations.getEligibleFormationIds(campaign,
                  track,
                  FACILITY_COORDS,
                  FacilityOperation.RAID).isEmpty());
        }

        @Test
        void liftingASiegeLeavesAnyOtherOrderAlone() {
            track.assignForce(FORMATION_ID, FACILITY_COORDS, TODAY, false);
            StratConFacilityOrder recon = new StratConFacilityOrder(FacilityOperation.RECON,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  TODAY.plusDays(3),
                  null);
            track.addFacilityOrder(recon);

            assertFalse(StratConFacilitySiege.liftSiege(campaign, track, FORMATION_ID));
            assertEquals(recon, track.getFacilityOrder(FORMATION_ID));
        }

        @Test
        void aSiegeSixDaysOldIsNotYetCountedButOneAWeekOldIs() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();
            besiege(FORMATION_ID, 0, TODAY.minusDays(StratConFacilitySiege.SIEGE_FIRST_WEEK_DAYS - 1));
            besiege(SECOND_FORMATION_ID, 1, TODAY.minusDays(StratConFacilitySiege.SIEGE_FIRST_WEEK_DAYS));

            StratConFacilitySiege.processSieges(track, campaign);

            assertEquals(garrison - 1, facility.getGarrison());
            assertEquals(5 - StratConFacilitySiege.SIEGE_WEEKLY_COST, campaignState.getSupportPoints());
        }

        @Test
        void whenSupportRunsOutPartWayOnlyTheFormationsPaidForStay() {
            facility.setGarrison(facility.getGarrisonMaximum());
            int garrison = facility.getGarrison();
            campaignState.setSupportPoints(StratConFacilitySiege.SIEGE_WEEKLY_COST);
            besiege(FORMATION_ID, 0);
            besiege(SECOND_FORMATION_ID, 1);

            StratConFacilitySiege.processSieges(track, campaign);

            assertEquals(0, campaignState.getSupportPoints());
            assertEquals(1, StratConFacilitySiege.getSieges(track, FACILITY_COORDS).size());
            assertEquals(garrison - 1, facility.getGarrison());
            assertFalse(track.getStickyForces().contains(SECOND_FORMATION_ID));
        }

        @Test
        void aFacilityTakenByOtherMeansIsNotChargedForAndItsSiegesEndNextDay() {
            besiege(FORMATION_ID, 0);
            facility.setOwner(ForceAlignment.Player);

            StratConFacilitySiege.processSieges(track, campaign);
            assertEquals(5, campaignState.getSupportPoints());

            StratConFacilityOperations.processOrders(track, campaign);
            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
            assertFalse(track.getStickyForces().contains(FORMATION_ID));
        }

        @Test
        void aRazedFacilitysSiegesEndWithoutCharge() {
            besiege(FORMATION_ID, 0);
            track.removeFacility(FACILITY_COORDS);

            StratConFacilitySiege.processSieges(track, campaign);
            StratConFacilityOperations.processOrders(track, campaign);

            assertEquals(5, campaignState.getSupportPoints());
            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
        }

        @Test
        void aSurrenderedFacilityIsHeldAtFullGarrison() {
            facility.setGarrison(1);
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.processSieges(track, campaign);

            assertEquals(ForceAlignment.Player, facility.getOwner());
            assertEquals(facility.getGarrisonMaximum(), facility.getGarrison());
        }

        @Test
        void aGarrisonAlreadyEmptySurrendersAtTheFirstWeeklyStep() {
            facility.setGarrison(0);
            besiege(FORMATION_ID, 0);

            StratConFacilitySiege.processSieges(track, campaign);

            assertEquals(ForceAlignment.Player, facility.getOwner());
        }

        @Test
        void aBesiegerKeepsItsPlaceAfterAWonFightIsCleared() {
            besiege(FORMATION_ID, 0);
            StratConCoords besiegerCoords = track.getAssignedForceCoords().get(FORMATION_ID);
            StratConScenario fight = siegeFight(FORMATION_ID, false);

            StratConFacilitySiege.resolveSiegeScenario(campaign, contract, track, FACILITY_COORDS, false, true);
            track.removeScenario(fight);
            StratConFacilityOperations.processOrders(track, campaign);

            assertTrue(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
            assertEquals(besiegerCoords, track.getAssignedForceCoords().get(FORMATION_ID));
            assertTrue(track.getStickyForces().contains(FORMATION_ID));
        }

        @Test
        void aBesiegerThatLostIsSentHomeWhenTheFightIsCleared() {
            besiege(FORMATION_ID, 0);
            StratConScenario fight = siegeFight(FORMATION_ID, true);

            StratConFacilitySiege.resolveSiegeScenario(campaign, contract, track, FACILITY_COORDS, true, false);
            track.removeScenario(fight);

            assertNull(track.getAssignedForceCoords().get(FORMATION_ID));
        }

        @Test
        void aFightOverASiegeLeftUnplayedBreaksItAsALossWould() {
            besiege(FORMATION_ID, 0);
            besiege(SECOND_FORMATION_ID, 1);
            StratConScenario fight = siegeFight(FORMATION_ID, false);

            StratConRulesManager.processIgnoredStratConScenario(fight,
                  track,
                  campaignState);

            assertFalse(StratConFacilitySiege.isBesieged(track, FACILITY_COORDS));
            assertNull(track.getAssignedForceCoords().get(FORMATION_ID));
            assertFalse(track.getStickyForces().contains(SECOND_FORMATION_ID));
        }

        @Test
        void aGarrisonDoesNotSortieWhileAFightOverTheSiegeIsUnderWay() {
            track.getCutOffFacilities().clear();
            besiege(FORMATION_ID, 0);
            besiege(SECOND_FORMATION_ID, 1);
            siegeFight(FORMATION_ID, false);

            assertTrue(StratConFacilitySiege.isSiegeFightUnderway(track, FACILITY_COORDS));
            assertNull(StratConFacilitySiege.trySortie(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  List.of(SECOND_FORMATION_ID),
                  0));
        }

        @Test
        void aRoutedGarrisonNeverSorties() {
            track.getCutOffFacilities().clear();
            track.setScenarioOdds(100);
            when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.ROUTED);
            besiege(FORMATION_ID, 0);

            assertNull(StratConFacilitySiege.trySortie(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  List.of(FORMATION_ID),
                  0));
        }

        @Test
        void aRollAboveTheOddsBringsNoSortie() {
            track.getCutOffFacilities().clear();
            track.setScenarioOdds(10);
            besiege(FORMATION_ID, 0);

            assertNull(StratConFacilitySiege.trySortie(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  List.of(FORMATION_ID),
                  11));
        }

        @Test
        void noReliefComesWithoutEnemyActivity() {
            track.getCutOffFacilities().clear();
            besiege(FORMATION_ID, 0);

            assertNull(StratConFacilitySiege.tryRelief(campaign, contract, track, FACILITY_COORDS, FORMATION_ID));
            assertNull(campaignState.getLastCounterattackDate());
        }

        @Test
        void aReliefForceWithNoBesiegerToFallOnDoesNotCountAsACounterattack() {
            track.getCutOffFacilities().clear();
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(3.0);
            campaignState.setLastCounterattackDate(TODAY.minusDays(100));

            assertNull(StratConFacilitySiege.tryRelief(campaign, contract, track, FACILITY_COORDS, FORMATION_ID));
            assertEquals(TODAY.minusDays(100), campaignState.getLastCounterattackDate());
        }
    }
}
