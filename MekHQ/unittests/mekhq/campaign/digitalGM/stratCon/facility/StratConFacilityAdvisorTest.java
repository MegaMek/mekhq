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
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenario.ScenarioState;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityAdvisor.OrderOption;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityAdvisor.UpcomingEvent;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityEffects.LocalModifiersEffect;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests the facility advisor: the order options it offers and the advice for those that cannot be given, the next-step
 * hints for a hex, and the list of upcoming events.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityAdvisorTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConFacilityOperations";
    private static final int FORMATION_ID = 7;
    private static final StratConCoords FACILITY_COORDS = new StratConCoords(3, 3);
    private static final StratConCoords ADJACENT_COORDS = FACILITY_COORDS.translate(0);
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
        when(campaign.getPlayerForce().getFormation(FORMATION_ID).getName()).thenReturn("Alpha Lance");

        contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.PLANETARY_ASSAULT);
        when(contract.getScale()).thenReturn(10);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        Faction enemyFaction = mock(Faction.class);
        when(contract.getEnemyFaction()).thenReturn(enemyFaction);
    }

    private StratConFacility placeFacility(ForceAlignment owner) {
        StratConFacility facility = StratConTestData.facility(owner,
              FacilityType.MekBase,
              new LocalModifiersEffect(List.of("MekGarrison.json")));
        track.addFacility(FACILITY_COORDS, facility);
        // Found by the player, as orders are only given on facilities they can see.
        facility.raiseIntel(FacilityIntel.LOCATED);
        track.getRevealedCoords().add(FACILITY_COORDS);
        return facility;
    }

    private void deployFormationAt(StratConCoords coords) {
        track.assignForce(FORMATION_ID, coords, TODAY, false);
    }

    private OrderOption option(FacilityOperation operation) {
        for (OrderOption option : StratConFacilityAdvisor.getOrderOptions(campaign, contract, track,
              FACILITY_COORDS)) {
            if (option.operation() == operation) {
                return option;
            }
        }
        throw new AssertionError("No option for " + operation);
    }

    private List<String> hints() {
        return StratConFacilityAdvisor.getHints(campaign, contract, track, FACILITY_COORDS);
    }

    @Nested
    class OrderOptions {
        @Test
        void withNoFormationNearbyEveryOrderSaysWhereToDeploy() {
            placeFacility(ForceAlignment.Opposing);

            OrderOption assault = option(FacilityOperation.ASSAULT);
            assertFalse(assault.isAvailable());
            assertEquals("contextMenu.noFormation", assault.reasonKey());
            assertEquals("guidance.noFormation.adjacent", assault.guidanceKey());
            assertTrue(assault.eligibleFormationIds().isEmpty());

            assertEquals("guidance.noFormation.adjacentOnly", option(FacilityOperation.SIEGE).guidanceKey());
        }

        @Test
        void aFormationNextToTheFacilityCanAssaultOrBesiegeIt() {
            placeFacility(ForceAlignment.Opposing);
            deployFormationAt(ADJACENT_COORDS);

            OrderOption siege = option(FacilityOperation.SIEGE);
            assertTrue(siege.isAvailable());
            assertNull(siege.guidanceKey());
            assertEquals(List.of(FORMATION_ID), siege.eligibleFormationIds());
            assertTrue(option(FacilityOperation.ASSAULT).isAvailable());
        }

        @Test
        void aFormationOnTheFacilityCannotBesiegeItAndIsToldToStepBack() {
            placeFacility(ForceAlignment.Opposing);
            deployFormationAt(FACILITY_COORDS);

            OrderOption siege = option(FacilityOperation.SIEGE);
            assertFalse(siege.isAvailable());
            assertEquals("guidance.noFormation.adjacentOnly", siege.guidanceKey());
            assertTrue(option(FacilityOperation.ASSAULT).isAvailable());
        }

        @Test
        void tooLittleSupportSaysHowToEarnMore() {
            placeFacility(ForceAlignment.Opposing);
            deployFormationAt(ADJACENT_COORDS);
            campaignState.setSupportPoints(0);

            OrderOption raid = option(FacilityOperation.RAID);
            assertEquals("reason.supportPoints", raid.reasonKey());
            assertEquals("guidance.supportPoints", raid.guidanceKey());
            // A free order is still possible.
            assertTrue(option(FacilityOperation.ASSAULT).isAvailable());
        }

        @Test
        void sabotageWithoutIntelPointsAtRecon() {
            when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.SABOTAGE);
            placeFacility(ForceAlignment.Opposing).setIntel(FacilityIntel.SCOUTED);
            deployFormationAt(FACILITY_COORDS);

            OrderOption sabotage = option(FacilityOperation.SABOTAGE);
            assertEquals("reason.needsDetailedIntel", sabotage.reasonKey());
            assertEquals("guidance.needsDetailedIntel", sabotage.guidanceKey());
        }

        @Test
        void aReasonNothingCanFixHasNoAdvice() {
            placeFacility(ForceAlignment.Opposing).setIntel(FacilityIntel.DETAILED);
            deployFormationAt(FACILITY_COORDS);

            OrderOption recon = option(FacilityOperation.RECON);
            assertEquals("reason.fullyScouted", recon.reasonKey());
            assertNull(recon.guidanceKey());
        }

        @Test
        void everyGuidanceKeyHasText() {
            for (FacilityOperation operation : FacilityOperation.values()) {
                for (String reasonKey : List.of("contextMenu.noFormation", "reason.supportPoints",
                      "reason.needsDetailedIntel", "reason.scenarioUnderway")) {
                    String guidanceKey = StratConFacilityAdvisor.getGuidanceKey(reasonKey, operation);
                    assertFalse(getTextAt(RESOURCE_BUNDLE, guidanceKey).startsWith("!"), guidanceKey);
                }
            }
        }
    }

    @Nested
    class Hints {
        @Test
        void noHintsWhenFacilityOperationsAreOff() {
            placeFacility(ForceAlignment.Opposing);
            when(campaign.getCampaignOptions().get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(false);

            assertTrue(hints().isEmpty());
        }

        @Test
        void anEnemyFacilityWithNoFormationNearbySaysToDeploy() {
            placeFacility(ForceAlignment.Opposing);

            assertEquals(List.of(getTextAt(RESOURCE_BUNDLE, "hint.deployToAct")), hints());
        }

        @Test
        void anEnemyFacilityWithAFormationNearbySaysHowToGiveOrders() {
            placeFacility(ForceAlignment.Opposing);
            deployFormationAt(ADJACENT_COORDS);

            assertEquals(List.of(getTextAt(RESOURCE_BUNDLE, "hint.ordersAvailable")), hints());
        }

        @Test
        void aSiegeShowsWhenTheFacilityShouldSurrenderOnceItsGarrisonIsKnown() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing);
            facility.setGarrison(2);
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.SIEGE, FORMATION_ID, FACILITY_COORDS,
                  TODAY, null));

            assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "hint.besieged", 1)), hints());

            // Begun today, the siege has its free first week before it bites: one week of nothing, then one step a
            // week for a garrison of two.
            facility.setIntel(FacilityIntel.DETAILED);
            assertEquals(List.of(getFormattedTextAt(RESOURCE_BUNDLE, "hint.besieged.detailed", 1, 3)), hints());
        }

        @Test
        void aTimedOrderShowsTheDaysLeft() {
            placeFacility(ForceAlignment.Opposing);
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.RECON, FORMATION_ID, FACILITY_COORDS,
                  TODAY.plusDays(4), null));

            List<String> hints = hints();
            assertTrue(hints.contains(getFormattedTextAt(RESOURCE_BUNDLE,
                  "hint.orderUnderway",
                  "Alpha Lance",
                  getTextAt(RESOURCE_BUNDLE, "operation.RECON"),
                  4L)), hints.toString());
        }

        @Test
        void anUnderstrengthFacilityYourSideHoldsSaysToReinforce() {
            placeFacility(ForceAlignment.Player).setGarrison(0);

            assertEquals(List.of(getTextAt(RESOURCE_BUNDLE, "hint.understrength")), hints());
        }

        @Test
        void aFullFacilityYourSideHoldsNeedsNothing() {
            placeFacility(ForceAlignment.Player);

            assertTrue(hints().isEmpty());
        }

        @Test
        void aCounterattackComesFirst() {
            placeFacility(ForceAlignment.Player).setGarrison(0);
            StratConScenario counterattack = new StratConScenario();
            counterattack.setCoords(FACILITY_COORDS);
            counterattack.setCounterattack(true);
            counterattack.setDeploymentDate(TODAY.plusDays(3));
            track.getScenarios().put(FACILITY_COORDS, counterattack);

            assertEquals(getFormattedTextAt(RESOURCE_BUNDLE,
                        "hint.counterattack",
                        StratConFacilityAdvisor.formatDate(TODAY.plusDays(3))),
                  hints().get(0));
        }

        @Test
        void aCounterattackYouHaveCommittedToNoLongerAsksYouToDeploy() {
            placeFacility(ForceAlignment.Player);
            StratConScenario counterattack = new StratConScenario();
            counterattack.setCoords(FACILITY_COORDS);
            counterattack.setCounterattack(true);
            counterattack.setDeploymentDate(TODAY.plusDays(3));
            counterattack.setCurrentState(ScenarioState.PRIMARY_FORCES_COMMITTED);
            track.getScenarios().put(FACILITY_COORDS, counterattack);

            assertEquals(List.of(getTextAt(RESOURCE_BUNDLE, "hint.counterattack.committed")), hints());
            assertEquals(1, StratConFacilityAdvisor.getUpcomingEvents(campaign, campaignState).size());
        }

        @Test
        void anEmptyHexWithNobodyOnItHasNoHints() {
            assertTrue(hints().isEmpty());
        }

        @Test
        void anEmptyHexWithAFormationOnItPointsAtBuild() {
            deployFormationAt(FACILITY_COORDS);

            assertEquals(List.of(getTextAt(RESOURCE_BUNDLE, "hint.emptyHexOrders")), hints());
        }
    }

    @Nested
    class SurrenderEstimate {
        @Test
        void oneBesiegerTakesAWeekPerGarrisonStep() {
            assertEquals(3, StratConFacilityAdvisor.getWeeksToSurrender(3, 1));
        }

        @Test
        void moreThanTwoBesiegersAddNothing() {
            assertEquals(2, StratConFacilityAdvisor.getWeeksToSurrender(3, 2));
            assertEquals(2, StratConFacilityAdvisor.getWeeksToSurrender(3, 5));
        }

        @Test
        void anEmptyGarrisonStillTakesOneWeeklyStep() {
            assertEquals(1, StratConFacilityAdvisor.getWeeksToSurrender(0, 1));
        }

        @Test
        void aSiegeStillInItsFreeFirstWeekAddsAWeek() {
            // Nobody bites next Monday; one besieger bites every Monday after.
            assertEquals(3, StratConFacilityAdvisor.getWeeksToSurrender(2, new int[] { 0, 1 }));
        }
    }

    @Nested
    class UpcomingEvents {
        @Test
        void eventsAreListedSoonestFirst() {
            placeFacility(ForceAlignment.Opposing);
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.BUILD, FORMATION_ID,
                  new StratConCoords(1, 1), TODAY.plusDays(14), null));
            track.getRoadCuts().add(new StratConRoadCut(new StratConCoords(5, 5), TODAY.plusDays(2)));

            List<UpcomingEvent> events = StratConFacilityAdvisor.getUpcomingEvents(campaign, campaignState);

            assertEquals(2, events.size());
            assertEquals(TODAY.plusDays(2), events.get(0).date());
            assertEquals(TODAY.plusDays(14), events.get(1).date());
            assertEquals(new StratConCoords(1, 1), events.get(1).coords());
        }

        @Test
        void aSiegeHasNoEndDateSoIsNotListed() {
            placeFacility(ForceAlignment.Opposing);
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.SIEGE, FORMATION_ID, FACILITY_COORDS,
                  TODAY, null));

            assertTrue(StratConFacilityAdvisor.getUpcomingEvents(campaign, campaignState).isEmpty());
        }

        @Test
        void onlyUnresolvedCounterattacksAreListed() {
            placeFacility(ForceAlignment.Player);
            StratConScenario counterattack = new StratConScenario();
            counterattack.setCoords(FACILITY_COORDS);
            counterattack.setCounterattack(true);
            counterattack.setDeploymentDate(TODAY.plusDays(3));
            track.getScenarios().put(FACILITY_COORDS, counterattack);

            List<UpcomingEvent> events = StratConFacilityAdvisor.getUpcomingEvents(campaign, campaignState);
            assertEquals(1, events.size());
            assertEquals(FACILITY_COORDS, events.get(0).coords());

            counterattack.setCurrentState(ScenarioState.COMPLETED);
            assertTrue(StratConFacilityAdvisor.getUpcomingEvents(campaign, campaignState).isEmpty());
        }

        @Test
        void anOrdinaryScenarioIsNotAFacilityEvent() {
            StratConScenario scenario = new StratConScenario();
            scenario.setCoords(FACILITY_COORDS);
            scenario.setDeploymentDate(TODAY.plusDays(3));
            track.getScenarios().put(FACILITY_COORDS, scenario);

            assertTrue(StratConFacilityAdvisor.getUpcomingEvents(campaign, campaignState).isEmpty());
        }
    }

    @Nested
    class ActivitySummary {
        @Test
        void aQuietFacilityHasNoActivity() {
            placeFacility(ForceAlignment.Opposing);

            assertEquals("", StratConFacilityAdvisor.getActivitySummary(track, FACILITY_COORDS));
        }

        @Test
        void aSiegeAndATimedOrderAreBothSummarised() {
            placeFacility(ForceAlignment.Opposing);
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.SIEGE, FORMATION_ID, FACILITY_COORDS,
                  TODAY, null));
            track.addFacilityOrder(new StratConFacilityOrder(FacilityOperation.RECON, FORMATION_ID + 1,
                  FACILITY_COORDS, TODAY.plusDays(7), null));

            String summary = StratConFacilityAdvisor.getActivitySummary(track, FACILITY_COORDS);
            assertEquals(getFormattedTextAt(RESOURCE_BUNDLE, "activity.besieged", 1) + ", "
                               + getFormattedTextAt(RESOURCE_BUNDLE, "activity.order",
                  getTextAt(RESOURCE_BUNDLE, "operation.RECON"),
                  StratConFacilityAdvisor.formatDate(TODAY.plusDays(7))), summary);
        }
    }
}
