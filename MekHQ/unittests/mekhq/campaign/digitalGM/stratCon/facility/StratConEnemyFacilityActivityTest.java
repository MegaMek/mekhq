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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.bind.JAXBContext;
import megamek.common.compute.Compute;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
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
import org.mockito.MockedStatic;

/**
 * Tests the enemy's facility activity: weekly upkeep, when counterattacks come and what losing or ignoring one costs,
 * and how often enemy engineers are sent. Building a counterattack's scenario needs a full campaign, so that is left to
 * play-testing.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConEnemyFacilityActivityTest {
    private static final StratConCoords FACILITY_COORDS = new StratConCoords(3, 3);
    private static final LocalDate TODAY = LocalDate.of(3050, 1, 3);

    private StratConCampaignState campaignState;
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
        contract = mock(AbstractContract.class);
        when(contract.getScale()).thenReturn(6);
        when(contract.getStartDate()).thenReturn(TODAY.minusDays(10));
        when(contract.getEndingDate()).thenReturn(TODAY.plusMonths(6));
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getEmployerForceSkill()).thenReturn(SkillLevel.REGULAR);
        when(contract.getEnemyForceSkill()).thenReturn(SkillLevel.REGULAR);

        campaignState = new StratConCampaignState(contract);
        track = new StratConTrackState();
        track.setWidth(8);
        track.setHeight(8);
        campaignState.addTrack(track);

        campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(1.0);
        when(options.get(CampaignOption.USE_FACILITY_OPERATIONS)).thenReturn(true);
        when(options.get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION)).thenReturn(false);
        when(options.isUseStratConMaplessMode()).thenReturn(false);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getGUI()).thenReturn(null);
    }

    private StratConFacility placeFacility(ForceAlignment owner) {
        StratConFacility facility = StratConTestData.facility(owner,
              FacilityType.MekBase,
              new LocalModifiersEffect(List.of("MekGarrison.json")));
        facility.setTier(FacilityTier.BASE);
        track.addFacility(FACILITY_COORDS, facility);
        return facility;
    }

    private StratConScenario placeScenario(boolean isCounterattack, boolean isCrisis) {
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getId()).thenReturn(99);
        when(backingScenario.getForceIDs()).thenReturn(new ArrayList<>());
        when(backingScenario.isCrisis()).thenReturn(isCrisis);

        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(FACILITY_COORDS);
        scenario.setBackingScenario(backingScenario);
        scenario.setCounterattack(isCounterattack);
        track.addScenario(scenario);
        return scenario;
    }

    @Nested
    class Activity {
        @Test
        void theOptionIsKeptWithinItsRange() {
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(7.0);
            assertEquals(StratConEnemyFacilityActivity.MAXIMUM_ACTIVITY,
                  StratConEnemyFacilityActivity.getActivity(campaign));

            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(-1.0);
            assertEquals(0, StratConEnemyFacilityActivity.getActivity(campaign));
        }

        @Test
        void maplessPlayHasNoActivity() {
            when(options.isUseStratConMaplessMode()).thenReturn(true);

            assertEquals(0, StratConEnemyFacilityActivity.getActivity(campaign));
        }
    }

    @Nested
    class MonthlyUpkeep {
        @Test
        void theWholeActivityIsAlwaysGivenAndItsFractionIsAChance() {
            assertEquals(1, StratConEnemyFacilityActivity.getUpkeepSteps(1.0, 99));
            assertEquals(0, StratConEnemyFacilityActivity.getUpkeepSteps(0.0, 0));
            assertEquals(2, StratConEnemyFacilityActivity.getUpkeepSteps(1.5, 49));
            assertEquals(1, StratConEnemyFacilityActivity.getUpkeepSteps(1.5, 50));
        }

        @Test
        void eachStepRepairsOneConditionStepAndReinforcesOneGarrisonStep() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing);
            facility.setCondition(FacilityCondition.CRIPPLED);
            facility.setGarrison(0);

            assertTrue(StratConEnemyFacilityActivity.applyUpkeep(facility, 1));

            assertEquals(FacilityCondition.DAMAGED, facility.getCondition());
            assertEquals(1, facility.getGarrison());
        }

        @Test
        void aFacilityInFullOrderIsNotChanged() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing);

            assertFalse(StratConEnemyFacilityActivity.applyUpkeep(facility, 2));
        }

        @Test
        void onlyEnemyFacilitiesWithNoFightUnderWayGetUpkeep() {
            StratConFacility allied = placeFacility(ForceAlignment.Allied);
            allied.setGarrison(0);

            StratConEnemyFacilityActivity.applyMonthlyUpkeep(track, campaign);
            assertEquals(0, allied.getGarrison());

            track.removeFacility(FACILITY_COORDS);
            StratConFacility enemy = placeFacility(ForceAlignment.Opposing);
            enemy.setGarrison(0);
            placeScenario(false, false);

            StratConEnemyFacilityActivity.applyMonthlyUpkeep(track, campaign);
            assertEquals(0, enemy.getGarrison());

            track.removeScenario(track.getScenario(FACILITY_COORDS));
            StratConEnemyFacilityActivity.applyMonthlyUpkeep(track, campaign);
            assertEquals(1, enemy.getGarrison());
        }

        @Test
        void noActivityMeansNoUpkeep() {
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(0.0);
            StratConFacility enemy = placeFacility(ForceAlignment.Opposing);
            enemy.setGarrison(0);

            StratConEnemyFacilityActivity.applyMonthlyUpkeep(track, campaign);

            assertEquals(0, enemy.getGarrison());
        }
    }

    @Nested
    class Counterattacks {
        @Test
        void aMonthWithoutOneGuaranteesTheNext() {
            assertTrue(StratConEnemyFacilityActivity.isCounterattack(1.0, 30, 99));
            assertFalse(StratConEnemyFacilityActivity.isCounterattack(1.0, 29, 99));
        }

        @Test
        void otherwiseTheChanceScalesWithActivity() {
            assertTrue(StratConEnemyFacilityActivity.isCounterattack(1.0, 0, 24));
            assertFalse(StratConEnemyFacilityActivity.isCounterattack(1.0, 0, 25));
            assertTrue(StratConEnemyFacilityActivity.isCounterattack(2.0, 0, 49));
        }

        @Test
        void noActivityMeansNoCounterattacks() {
            assertFalse(StratConEnemyFacilityActivity.isCounterattack(0, 365, 0));
            when(options.get(CampaignOption.ENEMY_FACILITY_ACTIVITY)).thenReturn(0.0);
            placeFacility(ForceAlignment.Player);

            assertEquals(0, StratConEnemyFacilityActivity.launchCounterattacks(campaign, contract, campaignState, 3));
        }

        @Test
        void onlyTheSideOfThePlayerIsCounterattackedAndNeverWhereAFightIsUnderWay() {
            placeFacility(ForceAlignment.Opposing);
            assertTrue(StratConEnemyFacilityActivity.getCounterattackTargets(campaignState).isEmpty());

            track.removeFacility(FACILITY_COORDS);
            placeFacility(ForceAlignment.Allied);
            assertEquals(1, StratConEnemyFacilityActivity.getCounterattackTargets(campaignState).size());

            placeScenario(false, false);
            assertTrue(StratConEnemyFacilityActivity.getCounterattackTargets(campaignState).isEmpty());
        }

        @Test
        void losingOneHandsTheFacilityToTheEnemy() {
            StratConFacility facility = placeFacility(ForceAlignment.Player);

            StratConEnemyFacilityActivity.resolveLostCounterattack(campaign, track, FACILITY_COORDS, facility);

            assertEquals(ForceAlignment.Opposing, facility.getOwner());
        }

        @Test
        void ignoringOneOnAFacilityThePlayerHoldsLosesItAndTheCrisisPoint() {
            StratConFacility facility = placeFacility(ForceAlignment.Player);
            StratConScenario scenario = placeScenario(true, true);

            StratConRulesManager.processIgnoredStratConScenario(scenario, track, campaignState);

            assertEquals(ForceAlignment.Opposing, facility.getOwner());
            assertEquals(-1, campaignState.getVictoryPoints());
            assertFalse(track.getScenarios().containsKey(FACILITY_COORDS));
        }

        @Test
        void anOrdinaryScenarioLeftUnplayedOnAFacilityNoLongerCostsIt() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied);
            StratConScenario scenario = placeScenario(false, false);

            StratConRulesManager.processIgnoredStratConScenario(scenario, track, campaignState);

            assertEquals(ForceAlignment.Allied, facility.getOwner());
        }

        @Test
        void anEmployerGarrisonThatHoldsOffScreenKeepsTheFacilityAndThePoint() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied);
            StratConScenario scenario = placeScenario(true, true);

            try (MockedStatic<Compute> dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
                dice.when(() -> Compute.d6(anyInt())).thenReturn(12);
                StratConRulesManager.processIgnoredStratConScenario(scenario, track, campaignState);
            }

            assertEquals(ForceAlignment.Allied, facility.getOwner());
            assertEquals(0, campaignState.getVictoryPoints());
            assertEquals(facility.getGarrisonMaximum() - 1, facility.getGarrison());
        }

        @Test
        void theOffScreenRollWeighsGarrisonTierSkillAndMorale() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied);
            // Base: garrison 2 + tier 2 = 4, so a roll of 5 just holds at even skill and Stalemate.
            assertTrue(StratConEnemyFacilityActivity.isOffScreenDefenseHeld(facility,
                  SkillLevel.REGULAR,
                  SkillLevel.REGULAR,
                  ContractMoraleLevel.STALEMATE,
                  5));
            assertFalse(StratConEnemyFacilityActivity.isOffScreenDefenseHeld(facility,
                  SkillLevel.REGULAR,
                  SkillLevel.REGULAR,
                  ContractMoraleLevel.STALEMATE,
                  4));
            assertFalse(StratConEnemyFacilityActivity.isOffScreenDefenseHeld(facility,
                  SkillLevel.REGULAR,
                  SkillLevel.REGULAR,
                  ContractMoraleLevel.ADVANCING,
                  5));
            assertFalse(StratConEnemyFacilityActivity.isOffScreenDefenseHeld(facility,
                  SkillLevel.GREEN,
                  SkillLevel.REGULAR,
                  ContractMoraleLevel.STALEMATE,
                  5));
        }
    }

    @Nested
    class EnemyEngineers {
        @Test
        void oneAMonthPerThreeScaleOrPerNineWithSupportPointsInScale() {
            assertEquals(2, StratConEnemyFacilityActivity.getMonthlyEngineerCount(6, false, 1.0));
            assertEquals(0, StratConEnemyFacilityActivity.getMonthlyEngineerCount(2, false, 1.0));
            assertEquals(1, StratConEnemyFacilityActivity.getMonthlyEngineerCount(9, true, 1.0));
            assertEquals(0, StratConEnemyFacilityActivity.getMonthlyEngineerCount(9, false, 0));
        }

        @Test
        void aMonthIsScheduledAtATimeWithinTheContract() {
            try (MockedStatic<Compute> dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
                dice.when(() -> Compute.randomInt(anyInt())).thenReturn(10);
                StratConEnemyFacilityActivity.processEnemyEngineers(campaign, contract, campaignState);
            }

            assertEquals(List.of(TODAY.plusDays(10), TODAY.plusDays(10)), campaignState.getEnemyEngineerDates());
            assertEquals(TODAY.plusMonths(1), campaignState.getEnemyEngineersScheduledUntil());

            // The next day, nothing more is scheduled until the month runs out.
            when(campaign.getLocalDate()).thenReturn(TODAY.plusDays(1));
            StratConEnemyFacilityActivity.processEnemyEngineers(campaign, contract, campaignState);
            assertEquals(2, campaignState.getEnemyEngineerDates().size());
        }

        @Test
        void thoseDueWhileTheEnemyIsRoutedNeverAppear() {
            when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.ROUTED);
            campaignState.setEnemyEngineersScheduledUntil(TODAY.plusDays(5));
            campaignState.getEnemyEngineerDates().add(TODAY);

            StratConEnemyFacilityActivity.processEnemyEngineers(campaign, contract, campaignState);

            assertTrue(campaignState.getEnemyEngineerDates().isEmpty());
        }

        @Test
        void theScheduleIsSaved() throws Exception {
            StratConCampaignState saved = new StratConCampaignState();
            saved.setLastCounterattackDate(TODAY);
            saved.setEnemyEngineersScheduledUntil(TODAY.plusMonths(1));
            saved.getEnemyEngineerDates().add(TODAY.plusDays(4));

            JAXBContext context = JAXBContext.newInstance(StratConCampaignState.class);
            StringWriter writer = new StringWriter();
            context.createMarshaller().marshal(saved, writer);
            StratConCampaignState loaded = context.createUnmarshaller()
                                                 .unmarshal(new StreamSource(new StringReader(writer.toString())),
                                                       StratConCampaignState.class)
                                                 .getValue();

            assertEquals(TODAY, loaded.getLastCounterattackDate());
            assertEquals(TODAY.plusMonths(1), loaded.getEnemyEngineersScheduledUntil());
            assertEquals(List.of(TODAY.plusDays(4)), loaded.getEnemyEngineerDates());
        }
    }

    @Nested
    class AwkwardCases {
        @Test
        void lowerActivityWaitsLongerBeforeACounterattackIsCertain() {
            assertFalse(StratConEnemyFacilityActivity.isCounterattack(0.5, 59, 99));
            assertTrue(StratConEnemyFacilityActivity.isCounterattack(0.5, 60, 99));
        }

        @Test
        void aContractWithNoStartDateCountsNoDaysSinceTheLastCounterattack() {
            when(contract.getStartDate()).thenReturn(null);

            assertEquals(0, StratConEnemyFacilityActivity.getDaysSinceLastCounterattack(contract,
                  campaignState,
                  TODAY));

            campaignState.setLastCounterattackDate(TODAY.minusDays(12));
            assertEquals(12, StratConEnemyFacilityActivity.getDaysSinceLastCounterattack(contract,
                  campaignState,
                  TODAY));
        }

        @Test
        void counterattacksCanFallOnAnySectorOfTheContract() {
            placeFacility(ForceAlignment.Player);
            StratConTrackState otherTrack = new StratConTrackState();
            otherTrack.setWidth(8);
            otherTrack.setHeight(8);
            campaignState.addTrack(otherTrack);
            otherTrack.addFacility(FACILITY_COORDS, StratConTestData.facility(ForceAlignment.Allied,
                  FacilityType.TankBase,
                  new LocalModifiersEffect(List.of("MekGarrison.json"))));

            assertEquals(2, StratConEnemyFacilityActivity.getCounterattackTargets(campaignState).size());
        }

        @Test
        void anEmployerGarrisonAlreadyEmptyNeverGoesBelowNothing() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied);
            facility.setGarrison(0);
            StratConScenario scenario = placeScenario(true, true);

            try (MockedStatic<Compute> dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
                dice.when(() -> Compute.d6(anyInt())).thenReturn(12);
                StratConRulesManager.processIgnoredStratConScenario(scenario, track, campaignState);
            }

            assertEquals(0, facility.getGarrison());
        }

        @Test
        void anEmployerGarrisonThatFailsOffScreenLosesTheFacilityAndThePoint() {
            StratConFacility facility = placeFacility(ForceAlignment.Allied);
            facility.setGarrison(0);
            StratConScenario scenario = placeScenario(true, true);

            try (MockedStatic<Compute> dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
                dice.when(() -> Compute.d6(anyInt())).thenReturn(2);
                StratConRulesManager.processIgnoredStratConScenario(scenario, track, campaignState);
            }

            assertEquals(ForceAlignment.Opposing, facility.getOwner());
            assertEquals(-1, campaignState.getVictoryPoints());
        }

        @Test
        void anIgnoredCounterattackOnAFacilityTheEnemyAlreadyRetookChangesNothing() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing);

            assertFalse(StratConEnemyFacilityActivity.resolveIgnoredCounterattack(track, facility, campaignState));
            assertEquals(ForceAlignment.Opposing, facility.getOwner());
        }

        @Test
        void aLostFacilityIsStillKnownInFullToThePlayer() {
            StratConFacility facility = placeFacility(ForceAlignment.Player);

            StratConEnemyFacilityActivity.resolveLostCounterattack(campaign, track, FACILITY_COORDS, facility);

            assertEquals(StratConFacility.FacilityIntel.DETAILED, facility.getIntel());
        }

        @Test
        void losingACounterattackOnAFacilityAlreadyLostDoesNotHandItBack() {
            StratConFacility facility = placeFacility(ForceAlignment.Opposing);

            StratConEnemyFacilityActivity.resolveLostCounterattack(campaign, track, FACILITY_COORDS, facility);

            assertEquals(ForceAlignment.Opposing, facility.getOwner());
        }

        @Test
        void aNegativeScaleSendsNoEngineers() {
            assertEquals(0, StratConEnemyFacilityActivity.getMonthlyEngineerCount(-4, false, 3.0));
            assertEquals(1, StratConEnemyFacilityActivity.getMonthlyEngineerCount(2, false, 1.5));
        }
    }

    @Test
    void aReliefForceIsNeverGuaranteed() {
        // A counterattack's chance is 25% at normal activity; a long quiet spell doesn't change that for relief.
        assertTrue(StratConEnemyFacilityActivity.isReliefSent(1.0, 24));
        assertFalse(StratConEnemyFacilityActivity.isReliefSent(1.0, 25));
        assertFalse(StratConEnemyFacilityActivity.isReliefSent(0, 0));
    }

    @Test
    void theQuietSpellCountsFromTheFirstDayCounterattacksCouldCome() {
        // A contract loaded partway through must not owe a counterattack for the days before.
        StratConEnemyFacilityActivity.launchCounterattacks(campaign, contract, campaignState, 0);

        assertEquals(TODAY, campaignState.getLastCounterattackDate());
    }
}
