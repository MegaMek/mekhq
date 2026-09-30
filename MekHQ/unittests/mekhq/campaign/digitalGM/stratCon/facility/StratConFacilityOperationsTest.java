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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTestDice;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests the facility orders: which orders a hex offers, when each can be given and by which formations, what the
 * orders that resolve without a fight do, how timed orders are kept and abandoned, and the choice after a capture.
 * Orders that start a scenario are left to play-testing, since building one needs a full campaign.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityOperationsTest {
    private static final int FORMATION_ID = 7;
    private static final StratConCoords FACILITY_COORDS = new StratConCoords(3, 3);
    private static final LocalDate TODAY = LocalDate.of(3050, 1, 1);

    private StratConCampaignState campaignState;
    private StratConTrackState track;
    private Campaign campaign;
    private AbstractContract contract;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        campaignState = new StratConCampaignState();
        track = new StratConTrackState();
        track.setWidth(8);
        track.setHeight(8);
        campaignState.addTrack(track);
        campaignState.setSupportPoints(10);

        campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(true);
        when(options.isUseStratConMaplessMode()).thenReturn(false);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID).isDeployed()).thenReturn(false);

        contract = contract(ContractObjectiveType.PLANETARY_ASSAULT);
    }

    private AbstractContract contract(ContractObjectiveType objectiveType) {
        AbstractContract newContract = mock(AbstractContract.class);
        when(newContract.getObjectiveType()).thenReturn(objectiveType);
        when(newContract.getScale()).thenReturn(10);
        when(newContract.getStratConCampaignState()).thenReturn(campaignState);
        when(newContract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        return newContract;
    }

    private StratConFacility placeFacility(ForceAlignment owner, FacilityType facilityType) {
        StratConFacility facility = StratConTestData.facility(owner,
              facilityType,
              new LocalModifiersEffect(List.of("MekGarrison.json")));
        track.addFacility(FACILITY_COORDS, facility);
        return facility;
    }

    private void deployFormationAt(StratConCoords coords) {
        track.assignForce(FORMATION_ID, coords, TODAY, false);
    }

    @Nested
    class OfferedOrders {
        @Test
        void anEnemyFacilityOffersTheFiveOrdersAgainstIt() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);

            assertEquals(List.of(FacilityOperation.RECON,
                        FacilityOperation.RAID,
                        FacilityOperation.SABOTAGE,
                        FacilityOperation.ASSAULT,
                        FacilityOperation.SIEGE),
                  StratConFacilityOperations.getOperationsFor(track, FACILITY_COORDS));
        }

        @Test
        void aFacilityThePlayerHoldsOffersFortifyAndReinforce() {
            placeFacility(ForceAlignment.Player, FacilityType.MekBase);

            assertEquals(List.of(FacilityOperation.FORTIFY, FacilityOperation.REINFORCE),
                  StratConFacilityOperations.getOperationsFor(track, FACILITY_COORDS));
        }

        @Test
        void anEmptyHexOffersBuild() {
            assertEquals(List.of(FacilityOperation.BUILD),
                  StratConFacilityOperations.getOperationsFor(track, FACILITY_COORDS));
        }

        @Test
        void everyOrderHasASupportPointCost() {
            for (FacilityOperation operation : FacilityOperation.values()) {
                assertTrue(StratConFacilityOperations.getSupportPointCost(operation) >= 0, operation.name());
            }
        }

        @Test
        void theOptionTurnsOrdersOff() {
            when(campaign.getCampaignOptions().get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(false);

            assertFalse(StratConFacilityOperations.isEnabled(campaign));
        }
    }

    @Nested
    class Reach {
        @Test
        void aFormationOnTheHexIsInReachOfEveryOrderButSiege() {
            for (FacilityOperation operation : FacilityOperation.values()) {
                assertEquals(operation != FacilityOperation.SIEGE,
                      StratConFacilityOperations.isInReach(FACILITY_COORDS, FACILITY_COORDS, operation),
                      operation.name());
            }
        }

        @Test
        void aFormationNextToTheFacilityCanActAgainstItButNotFortifyIt() {
            StratConCoords adjacent = FACILITY_COORDS.translate(0);

            assertTrue(StratConFacilityOperations.isInReach(adjacent, FACILITY_COORDS, FacilityOperation.RAID));
            assertFalse(StratConFacilityOperations.isInReach(adjacent, FACILITY_COORDS, FacilityOperation.FORTIFY));
            assertFalse(StratConFacilityOperations.isInReach(adjacent, FACILITY_COORDS, FacilityOperation.BUILD));
        }

        @Test
        void aFormationTwoHexesAwayIsOutOfReach() {
            StratConCoords farAway = FACILITY_COORDS.translate(0).translate(0);

            assertFalse(StratConFacilityOperations.isInReach(farAway, FACILITY_COORDS, FacilityOperation.RECON));
        }

        @Test
        void onlyFormationsInReachAndNotBusyAreEligible() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            deployFormationAt(FACILITY_COORDS.translate(1));

            assertEquals(List.of(FORMATION_ID),
                  StratConFacilityOperations.getEligibleFormationIds(campaign,
                        track,
                        FACILITY_COORDS,
                        FacilityOperation.RAID));

            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.RECON,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  TODAY.plusDays(7),
                  null));

            assertTrue(StratConFacilityOperations.getEligibleFormationIds(campaign,
                  track,
                  FACILITY_COORDS,
                  FacilityOperation.RAID).isEmpty());
        }
    }

    @Nested
    class Availability {
        private String reason(FacilityOperation operation) {
            return StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  operation);
        }

        @Test
        void anOrderNeedsItsSupportPoints() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            campaignState.setSupportPoints(0);

            assertEquals("reason.supportPoints", reason(FacilityOperation.RAID));
        }

        @Test
        void reconIsPointlessOnceEverythingIsKnown() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            assertNull(reason(FacilityOperation.RECON));

            facility.setIntel(FacilityIntel.DETAILED);

            assertEquals("reason.fullyScouted", reason(FacilityOperation.RECON));
        }

        @Test
        void aBatchallForbidsRaidsAndSabotageButNotAssaults() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            when(contract.isBatchallAccepted()).thenReturn(true);

            assertEquals("reason.batchall", reason(FacilityOperation.RAID));
            assertEquals("reason.batchall", reason(FacilityOperation.SABOTAGE));
            assertNull(reason(FacilityOperation.ASSAULT));
        }

        @Test
        void sabotageNeedsACovertContractOrSpecialMechanics() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setIntel(FacilityIntel.DETAILED);
            assertEquals("reason.sabotageContract", reason(FacilityOperation.SABOTAGE));

            contract = contract(ContractObjectiveType.ESPIONAGE);
            assertNull(reason(FacilityOperation.SABOTAGE));

            contract = contract(ContractObjectiveType.PLANETARY_ASSAULT);
            campaignState.setContractsUseSpecialMechanics(true);
            assertNull(reason(FacilityOperation.SABOTAGE));
        }

        @Test
        void sabotageNeedsDetailedIntelAndATargetNotYetCrippled() {
            contract = contract(ContractObjectiveType.SABOTAGE);
            StratConFacility facility = placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setIntel(FacilityIntel.SCOUTED);
            assertEquals("reason.needsDetailedIntel", reason(FacilityOperation.SABOTAGE));

            facility.setIntel(FacilityIntel.DETAILED);
            facility.setCondition(FacilityCondition.CRIPPLED);
            assertEquals("reason.alreadyCrippled", reason(FacilityOperation.SABOTAGE));
        }

        @Test
        void fortifyStopsAtAStrongholdAndReinforceAtAFullGarrison() {
            StratConFacility facility = placeFacility(ForceAlignment.Player, FacilityType.MekBase);
            assertEquals("reason.garrisonFull", reason(FacilityOperation.REINFORCE));
            assertNull(reason(FacilityOperation.FORTIFY));

            facility.setTier(FacilityTier.STRONGHOLD);

            assertEquals("reason.maximumTier", reason(FacilityOperation.FORTIFY));
            assertNull(reason(FacilityOperation.REINFORCE));
        }

        @Test
        void nothingCanBeBuiltOnACity() {
            track.addCity(FACILITY_COORDS);

            assertEquals("reason.hexOccupied", reason(FacilityOperation.BUILD));
        }
    }

    @Nested
    class ImmediateOrders {
        @Test
        void fortifyRaisesTheTierAndPays() {
            StratConFacility facility = placeFacility(ForceAlignment.Player, FacilityType.MekBase);
            deployFormationAt(FACILITY_COORDS);

            assertTrue(StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  FacilityOperation.FORTIFY,
                  null));

            assertEquals(FacilityTier.STRONGHOLD, facility.getTier());
            assertEquals(10 - StratConFacilityOperations.FORTIFY_COST, campaignState.getSupportPoints());
        }

        @Test
        void reinforceFillsTheGarrison() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied, FacilityType.MekBase);
            facility.setGarrison(0);
            deployFormationAt(FACILITY_COORDS);

            assertTrue(StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  FacilityOperation.REINFORCE,
                  null));

            assertEquals(facility.getGarrisonMaximum(), facility.getGarrison());
        }

        @Test
        void anOrderWithNoFormationInReachIsNotGivenAndCostsNothing() {
            placeFacility(ForceAlignment.Player, FacilityType.MekBase);

            assertFalse(StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  FacilityOperation.FORTIFY,
                  null));

            assertEquals(10, campaignState.getSupportPoints());
        }

        @Test
        void successfulSabotageDropsTheConditionTwoSteps() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);

            StratConFacilityOperations.resolveSabotage(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  StratConFacilityOperations.SABOTAGE_TARGET_NUMBER);

            assertEquals(FacilityCondition.CRIPPLED, facility.getCondition());
        }
    }

    @Nested
    class TimedOrders {
        @Test
        void reconAndBuildAreKeptAndHoldTheFormation() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            deployFormationAt(FACILITY_COORDS.translate(2));

            assertTrue(StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  FacilityOperation.RECON,
                  null));

            StratConFacilityOrder order = track.getFacilityOrder(FORMATION_ID);
            assertNotNull(order);
            assertEquals(TODAY.plusDays(StratConFacilityOperations.RECON_DAYS), order.getCompletionDate());
            assertTrue(track.getStickyForces().contains(FORMATION_ID));
        }

        @Test
        void aSuccessfulReconRaisesIntelOneStep() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setIntel(FacilityIntel.LOCATED);
            StratConFacilityOrder order = new StratConFacilityOrder(FacilityOperation.RECON,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  TODAY,
                  null);

            StratConFacilityOperations.resolveRecon(campaign,
                  contract,
                  track,
                  order,
                  StratConFacilityOperations.RECON_TARGET_NUMBER);

            assertEquals(FacilityIntel.SCOUTED, facility.getIntel());
        }

        @Test
        void aReconIsAbandonedWhenTheFormationLeaves() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            StratConFacilityOrder order = new StratConFacilityOrder(FacilityOperation.RECON,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  TODAY.plusDays(3),
                  null);
            track.addFacilityOrder(order);
            deployFormationAt(FACILITY_COORDS);
            assertTrue(StratConFacilityOperations.isOrderStillPossible(track, order));

            track.unassignFormation(FORMATION_ID);
            StratConFacilityOperations.processOrders(track, campaign);

            assertTrue(track.getFacilityOrders().isEmpty());
        }

        @Test
        void aBuildIsInterruptedWhenAFacilityAppearsOnItsHex() {
            StratConFacilityOrder order = new StratConFacilityOrder(FacilityOperation.BUILD,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  TODAY.plusDays(3),
                  "MekBase");
            deployFormationAt(FACILITY_COORDS);
            assertTrue(StratConFacilityOperations.isOrderStillPossible(track, order));

            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);

            assertFalse(StratConFacilityOperations.isOrderStillPossible(track, order));
        }

        @Test
        void ordersRoundTripThroughTheSave() throws JAXBException {
            StratConFacilityOrder order = new StratConFacilityOrder(FacilityOperation.BUILD,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  TODAY.plusDays(14),
                  "MekBase");

            Marshaller marshaller = JAXBContext.newInstance(StratConFacilityOrder.class).createMarshaller();
            StringWriter writer = new StringWriter();
            marshaller.marshal(new JAXBElement<>(new QName("facilityOrder"), StratConFacilityOrder.class, order),
                  writer);
            StratConFacilityOrder loaded = JAXBContext.newInstance(StratConFacilityOrder.class)
                                                 .createUnmarshaller()
                                                 .unmarshal(new StreamSource(new StringReader(writer.toString())),
                                                       StratConFacilityOrder.class)
                                                 .getValue();

            assertEquals(FacilityOperation.BUILD, loaded.getOperation());
            assertEquals(FORMATION_ID, loaded.getFormationId());
            assertEquals(FACILITY_COORDS, loaded.getTargetCoords());
            assertEquals(TODAY.plusDays(14), loaded.getCompletionDate());
            assertEquals("MekBase", loaded.getDefinitionId());
        }
    }

    @Nested
    class Capture {
        @Test
        void holdingAFacilityMakesItThePlayers() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied, FacilityType.MekBase);

            StratConFacilityOperations.resolveCapture(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityCaptureChoice.HOLD);

            assertEquals(ForceAlignment.Player, facility.getOwner());
            assertTrue(facility.isOwnerAlliedToPlayer());
        }

        @Test
        void handingAFacilityOverLeavesItAlliedAtFullGarrison() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied, FacilityType.MekBase);
            facility.setGarrison(0);

            StratConFacilityOperations.resolveCapture(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityCaptureChoice.HAND_OVER);

            assertEquals(ForceAlignment.Allied, facility.getOwner());
            assertEquals(facility.getGarrisonMaximum(), facility.getGarrison());
        }

        @Test
        void razingACivilianFacilityRaisesEscalationMoreThanAMilitaryOne() {
            contract = contract(ContractObjectiveType.SABOTAGE);
            campaignState.setContractsUseSpecialMechanics(true);

            try (MockedStatic<Compute> ignored = StratConTestDice.loadDice()) {
                placeFacility(ForceAlignment.Allied, FacilityType.MekBase);
                StratConFacilityOperations.resolveCapture(campaign,
                      contract,
                      track,
                      FACILITY_COORDS,
                      FacilityCaptureChoice.RAZE);
                assertNull(track.getFacility(FACILITY_COORDS));
                assertEquals(StratConTestDice.PIPS_PER_DIE, campaignState.getEscalation());

                placeFacility(ForceAlignment.Allied, FacilityType.IndustrialFacility);
                StratConFacilityOperations.resolveCapture(campaign,
                      contract,
                      track,
                      FACILITY_COORDS,
                      FacilityCaptureChoice.RAZE);
                assertEquals(StratConTestDice.PIPS_PER_DIE * 4, campaignState.getEscalation());
            }
        }

        @Test
        void withoutAGuiTheCaptureIsHeld() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied, FacilityType.MekBase);
            when(campaign.getGUI()).thenReturn(null);

            assertEquals(FacilityCaptureChoice.HOLD, StratConFacilityOperations.askCaptureChoice(campaign, facility));
        }
    }

    @Nested
    class AwkwardCases {
        private StratConFacilityOrder recon(LocalDate completionDate) {
            StratConFacilityOrder order = new StratConFacilityOrder(FacilityOperation.RECON,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  completionDate,
                  null);
            track.addFacilityOrder(order);
            track.addStickyForce(FORMATION_ID);
            return order;
        }

        @Test
        void aReconDueOnAFacilityThePlayerHasSinceTakenIsAbandoned() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            deployFormationAt(FACILITY_COORDS);
            recon(TODAY);
            facility.setOwner(ForceAlignment.Player);

            StratConFacilityOperations.processOrders(track, campaign);

            assertTrue(track.getFacilityOrders().isEmpty());
            assertFalse(track.getStickyForces().contains(FORMATION_ID));
        }

        @Test
        void aReconDueOnAFacilityRazedMeanwhileIsAbandoned() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            deployFormationAt(FACILITY_COORDS);
            recon(TODAY);
            track.removeFacility(FACILITY_COORDS);

            StratConFacilityOperations.processOrders(track, campaign);

            assertTrue(track.getFacilityOrders().isEmpty());
        }

        @Test
        void anOrderIsKeptUntilItsDayAndNotADayLonger() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setIntel(FacilityIntel.LOCATED);
            deployFormationAt(FACILITY_COORDS);
            recon(TODAY.plusDays(1));

            StratConFacilityOperations.processOrders(track, campaign);

            assertEquals(1, track.getFacilityOrders().size());
            assertEquals(FacilityIntel.LOCATED, facility.getIntel());
        }

        @Test
        void aBuildOfADefinitionNoLongerLoadedBuildsNothing() {
            deployFormationAt(FACILITY_COORDS);
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.BUILD,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  TODAY,
                  "NoSuchFacility"));
            when(campaign.getActiveContracts()).thenReturn(List.of(contract));

            StratConFacilityOperations.processOrders(track, campaign);

            assertTrue(track.getFacilityOrders().isEmpty());
            assertNull(track.getFacility(FACILITY_COORDS));
        }

        @Test
        void aSecondBuildCannotBeStartedOnAHexAlreadyBeingBuiltOn() {
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.BUILD,
                  FORMATION_ID + 1,
                  FACILITY_COORDS,
                  TODAY.plusDays(5),
                  "MekBase"));

            assertEquals("reason.hexOccupied", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityOperation.BUILD));
        }

        @Test
        void aBuildWithNothingChosenIsNotGivenAndCostsNothing() {
            deployFormationAt(FACILITY_COORDS);

            assertFalse(StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  FacilityOperation.BUILD,
                  null));
            assertEquals(10, campaignState.getSupportPoints());
        }

        @Test
        void aFormationAlreadyInAFightIsNotEligible() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            deployFormationAt(FACILITY_COORDS);
            when(campaign.getPlayerForce().getFormation(FORMATION_ID).isDeployed()).thenReturn(true);

            assertTrue(StratConFacilityOperations.getEligibleFormationIds(campaign,
                  track,
                  FACILITY_COORDS,
                  FacilityOperation.RAID).isEmpty());
        }

        @Test
        void ordersAgainstAFacilityCannotBeGivenWhileAFightIsUnderWayThere() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            StratConScenario scenario = new StratConScenario();
            scenario.setCoords(FACILITY_COORDS);
            track.getScenarios().put(FACILITY_COORDS, scenario);

            for (FacilityOperation operation : List.of(FacilityOperation.RECON,
                  FacilityOperation.RAID,
                  FacilityOperation.ASSAULT)) {
                assertEquals("reason.scenarioUnderway", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                      contract,
                      track,
                      FACILITY_COORDS,
                      operation), operation.name());
            }
        }

        @Test
        void ordersForYourOwnFacilitiesCannotBeGivenOnTheEnemys() {
            placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);

            assertEquals("reason.notHeld", StratConFacilityOperations.getUnavailableReasonKey(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityOperation.REINFORCE));
        }

        @Test
        void sabotageOnADamagedFacilityStopsAtCrippled() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing, FacilityType.MekBase);
            facility.setCondition(FacilityCondition.DAMAGED);

            StratConFacilityOperations.resolveSabotage(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  12);

            assertEquals(FacilityCondition.CRIPPLED, facility.getCondition());
            assertEquals(facility, track.getFacility(FACILITY_COORDS));
        }

        @Test
        void fortifyingKeepsTheGarrisonItHad() {
            StratConFacility facility = placeFacility(ForceAlignment.Player, FacilityType.MekBase);
            facility.setTier(FacilityTier.OUTPOST);
            deployFormationAt(FACILITY_COORDS);

            StratConFacilityOperations.issueOrder(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FORMATION_ID,
                  FacilityOperation.FORTIFY,
                  null);

            assertEquals(FacilityTier.BASE, facility.getTier());
            assertEquals(FacilityTier.OUTPOST.getGarrisonMaximum(), facility.getGarrison());
        }

        @Test
        void holdingAFacilityWithNoGarrisonLeftMansItFully() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied, FacilityType.MekBase);
            facility.setGarrison(0);

            StratConFacilityOperations.resolveCapture(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityCaptureChoice.HOLD);

            assertEquals(facility.getGarrisonMaximum(), facility.getGarrison());
        }

        @Test
        void aCaptureWithNoFacilityLeftDoesNothing() {
            StratConFacilityOperations.resolveCapture(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityCaptureChoice.RAZE);

            assertEquals(0, campaignState.getEscalation());
        }

        @Test
        void razingAFacilityEndsTheSiegesOnItTheNextDay() {
            placeFacility(ForceAlignment.Allied, FacilityType.MekBase);
            deployFormationAt(FACILITY_COORDS.translate(0));
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.SIEGE,
                  FORMATION_ID,
                  FACILITY_COORDS,
                  TODAY,
                  null));
            track.addStickyForce(FORMATION_ID);

            StratConFacilityOperations.resolveCapture(campaign,
                  contract,
                  track,
                  FACILITY_COORDS,
                  FacilityCaptureChoice.RAZE);
            StratConFacilityOperations.processOrders(track, campaign);

            assertTrue(track.getFacilityOrders().isEmpty());
            assertFalse(track.getStickyForces().contains(FORMATION_ID));
        }
    }
}
