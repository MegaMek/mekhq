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

import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.BELEAGUERED_FORCES;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.DATA_CACHE;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.LOOKOUT_POINT;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.POTENTIAL_LEAD;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.SECURITY_REVIEW;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.STRATEGIC_POSITION;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.VIP;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.VULNERABLE_INFRASTRUCTURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.PointOfInterestParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.*;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for scheduling data caches on Espionage contracts: the "Contracts Use Special Mechanics" option, the roll on
 * the Track Intensity Tables (made once per point of scale when "Multiply Track Intensity by Scale" is on), and the
 * caches' spawn dates.
 *
 * <p>With a single track, every row of the Track Intensity Tables' first column holds exactly one item, so each roll
 * schedules exactly one cache whatever the die shows.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConDataCacheSchedulingTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final String DEFINITION_TYPE_ID = "UnitTestSchedulingDefinitionType";
    private static final int LENGTH_IN_MONTHS = 3;
    private static final int SCALE = 3;

    private static AbstractContract contract(ContractObjectiveType objectiveType, int trackCount) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        when(contract.getStartDate()).thenReturn(TODAY);
        when(contract.getLengthInMonths()).thenReturn(LENGTH_IN_MONTHS);
        when(contract.getTrackCount()).thenReturn(trackCount);
        when(contract.getScale()).thenReturn(SCALE);
        return contract;
    }

    /** A contract definition asking for two point of interest objectives and one ordinary point of interest. */
    private static StratConContractDefinition definitionAskingForPointsOfInterest() {
        ObjectiveParameters objective = new ObjectiveParameters();
        objective.setObjectiveType(StrategicObjectiveType.PointOfInterest);
        objective.setObjectiveCount(2);
        objective.getObjectivePointsOfInterest().add(DEFINITION_TYPE_ID);

        PointOfInterestParameters ordinary = new PointOfInterestParameters();
        ordinary.setTypeId(DEFINITION_TYPE_ID);
        ordinary.setCount(1);

        StratConContractDefinition definition = new StratConContractDefinition();
        definition.setObjectiveParameters(List.of(objective));
        definition.setPointsOfInterest(List.of(ordinary));
        return definition;
    }

    private static List<StratConScheduledPointOfInterest> schedule(AbstractContract contract,
          boolean isMultiplyTrackIntensityByScale, boolean isContractsUseSpecialMechanics) {
        StratConCampaignState campaignState = new StratConCampaignState();
        StratConContractInitializer.schedulePointsOfInterest(contract,
              definitionAskingForPointsOfInterest(),
              campaignState,
              isMultiplyTrackIntensityByScale,
              isContractsUseSpecialMechanics);
        return campaignState.getScheduledPointsOfInterest();
    }

    private static boolean isDataCache(StratConScheduledPointOfInterest pointOfInterest) {
        return DATA_CACHE.getTypeId().equals(pointOfInterest.getTypeId());
    }

    @Test
    void anEspionageContractSchedulesOnlyDataCachesEachAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled = schedule(contract(ContractObjectiveType.ESPIONAGE, 1),
              true,
              true);

        assertFalse(scheduled.isEmpty());
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertTrue(isDataCache(pointOfInterest), "the definition's own points of interest are ignored");
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void theTrackIntensityTablesAreRolledOncePerPointOfScale() {
        List<StratConScheduledPointOfInterest> scheduled = schedule(contract(ContractObjectiveType.ESPIONAGE, 1),
              true,
              true);

        assertEquals(SCALE, scheduled.size(), "one roll per point of scale, one cache per roll with a single track");
    }

    @Test
    void theTrackIntensityTablesAreRolledOnceWithoutMultiplyingByScale() {
        List<StratConScheduledPointOfInterest> scheduled = schedule(contract(ContractObjectiveType.ESPIONAGE, 1),
              false,
              true);

        assertEquals(1, scheduled.size());
    }

    @Test
    void theRolledScheduleSetsHowManyCachesThereAre() {
        List<StratConScheduledPointOfInterest> scheduled = schedule(contract(ContractObjectiveType.ESPIONAGE, 3),
              true,
              true);

        assertEquals(3 * SCALE, scheduled.size(), "each roll of a three-track column places three");
    }

    @Test
    void dataCachesAppearWithinTheContract() {
        List<StratConScheduledPointOfInterest> scheduled = schedule(contract(ContractObjectiveType.ESPIONAGE, 3),
              true,
              true);

        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertNotNull(pointOfInterest.getSpawnDate());
            assertFalse(pointOfInterest.getSpawnDate().isBefore(TODAY));
            assertTrue(pointOfInterest.getSpawnDate().isBefore(TODAY.plusMonths(LENGTH_IN_MONTHS)));
        }
    }

    @Test
    void aContractWithoutTracksGetsNoDataCaches() {
        assertTrue(schedule(contract(ContractObjectiveType.ESPIONAGE, 0), true, true).isEmpty());
    }

    @Test
    void anEspionageContractWithoutAStartDateSchedulesNothing() {
        AbstractContract contract = contract(ContractObjectiveType.ESPIONAGE, 1);
        when(contract.getStartDate()).thenReturn(null);

        assertTrue(schedule(contract, true, true).isEmpty());
    }

    @Test
    void withoutSpecialMechanicsAnEspionageContractSchedulesItsDefinitionsPointsOfInterest() {
        List<StratConScheduledPointOfInterest> scheduled = schedule(contract(ContractObjectiveType.ESPIONAGE, 1),
              true,
              false);

        assertEquals(3, scheduled.size(), "two objectives and one ordinary point of interest, as the definition asks");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertFalse(isDataCache(pointOfInterest));
        }
    }

    @Test
    void otherContractTypesNeverGetDataCaches() {
        List<StratConScheduledPointOfInterest> scheduled = schedule(contract(ContractObjectiveType.UNDEFINED, 1),
              true,
              true);

        assertEquals(3, scheduled.size());
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertFalse(isDataCache(pointOfInterest));
        }
    }

    // Guerrilla Warfare: vulnerable infrastructure

    @Test
    void aGuerrillaWarfareContractSchedulesOnlyVulnerableInfrastructureEachAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.GUERRILLA_WARFARE, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(VULNERABLE_INFRASTRUCTURE.getTypeId(), pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void withoutSpecialMechanicsAGuerrillaWarfareContractSchedulesItsDefinitionsPointsOfInterest() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.GUERRILLA_WARFARE, 1), true, false);

        assertEquals(3, scheduled.size());
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(DEFINITION_TYPE_ID, pointOfInterest.getTypeId());
        }
    }

    // Mole Hunting: potential leads

    @Test
    void aMoleHuntingContractSchedulesOnlyPotentialLeadsEachAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.MOLE_HUNTING, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(POTENTIAL_LEAD.getTypeId(), pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void withoutSpecialMechanicsAMoleHuntingContractSchedulesItsDefinitionsPointsOfInterest() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.MOLE_HUNTING, 1), true, false);

        assertEquals(3, scheduled.size());
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(DEFINITION_TYPE_ID, pointOfInterest.getTypeId());
        }
    }

    // Observation Raid: lookout points

    @Test
    void anObservationRaidContractSchedulesOnlyLookoutPointsEachAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.OBSERVATION_RAID, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(LOOKOUT_POINT.getTypeId(), pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void withoutSpecialMechanicsAnObservationRaidContractSchedulesItsDefinitionsPointsOfInterest() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.OBSERVATION_RAID, 1), true, false);

        assertEquals(3, scheduled.size());
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(DEFINITION_TYPE_ID, pointOfInterest.getTypeId());
        }
    }

    // Relief Duty: beleaguered forces

    @Test
    void aReliefDutyContractSchedulesOnlyBeleagueredForcesEachAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.RELIEF_DUTY, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(BELEAGUERED_FORCES.getTypeId(), pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    // Which contracts use special points of interest, and which of those replace Essential scenarios

    @Test
    void eachContractTypeWithSpecialMechanicsGetsItsOwnSpecialPointOfInterest() {
        assertEquals(DATA_CACHE.getTypeId(),
              StratConContractInitializer.getSpecialPointOfInterestTypeId(contract(ContractObjectiveType.ESPIONAGE,
                    1), true));
        assertEquals(VULNERABLE_INFRASTRUCTURE.getTypeId(),
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.GUERRILLA_WARFARE, 1), true));
        assertEquals(POTENTIAL_LEAD.getTypeId(),
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.MOLE_HUNTING, 1), true));
        assertEquals(StratConAssassinationLeadBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.ASSASSINATION, 1), true));
        assertEquals(LOOKOUT_POINT.getTypeId(),
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.OBSERVATION_RAID, 1), true));
        assertEquals(BELEAGUERED_FORCES.getTypeId(),
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.RELIEF_DUTY, 1), true));
        assertEquals(StratConShowOfForceBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.GARRISON_DUTY, 1), true));
        assertEquals(StratConPirateCaptainBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.PIRATE_HUNTING, 1), true));
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(
              contract(ContractObjectiveType.UNDEFINED, 1), true));
    }

    @Test
    void withoutSpecialMechanicsNoContractGetsSpecialPointsOfInterest() {
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.ESPIONAGE, 1), false),
              "without special mechanics, an Espionage contract keeps its Essential scenarios");
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.GUERRILLA_WARFARE, 1), false),
              "without special mechanics, a Guerrilla Warfare contract keeps its Essential scenarios");
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.MOLE_HUNTING, 1), false),
              "without special mechanics, a Mole Hunting contract keeps its Essential scenarios");
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.OBSERVATION_RAID, 1), false),
              "without special mechanics, an Observation Raid contract keeps its Essential scenarios");
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class,
          names = { "ESPIONAGE", "GUERRILLA_WARFARE", "MOLE_HUNTING", "ASSASSINATION", "OBSERVATION_RAID",
                    "EXTRACTION_RAID", "RETAINER", "RIOT_DUTY", "SABOTAGE", "TERRORISM", "PIRATE_RAID",
                    "PIRATE_HUNTING", "DIVERSIONARY_RAID", "OBJECTIVE_RAID" })
    void specialPointsOfInterestReplaceEssentialScenarios(ContractObjectiveType objectiveType) {
        assertTrue(StratConContractInitializer.isReplacingEssentialScenarios(contract(objectiveType, 1), true));
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(contract(objectiveType, 1), false),
              "without special mechanics, the contract keeps its Essential scenarios");
    }

    @Test
    void oneAssassinationLeadPerPointOfScalePointsToTheRealTarget() {
        // The behavior told of the schedule is the one the type's definition names.
        StratConPointOfInterestDefinition realDefinition = new StratConPointOfInterestDefinition();
        realDefinition.setTypeId(StratConAssassinationLeadBehavior.TYPE_ID);
        realDefinition.setBehaviorId(StratConAssassinationLeadBehavior.BEHAVIOR_ID);
        StratConPointOfInterestDefinitions.registerDefinition(realDefinition);
        List<StratConScheduledPointOfInterest> scheduled;
        try {
            scheduled = schedule(contract(ContractObjectiveType.ASSASSINATION, 1), true, true);
        } finally {
            StratConPointOfInterestDefinitions.unregisterDefinition(StratConAssassinationLeadBehavior.TYPE_ID);
        }

        int realTargets = 0;
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertTrue(pointOfInterest.isStrategicObjective(), "every lead looks alike, so every one is an objective");
            if ("true".equals(pointOfInterest.getInitialState()
                                    .get(StratConAssassinationLeadBehavior.REAL_TARGET_STATE_KEY))) {
                realTargets++;
            }
        }
        assertEquals(Math.min(SCALE, scheduled.size()), realTargets);
    }

    @Test
    void aSpecialTypeWithoutADefinitionIsStillScheduledButNothingIsSettledForIt() {
        StratConCampaignState scheduleState = new StratConCampaignState();

        StratConContractInitializer.scheduleSpecialPointsOfInterest(contract(ContractObjectiveType.ASSASSINATION, 1),
              scheduleState,
              true,
              "UnitTestUndefinedSpecialType");

        assertFalse(scheduleState.getScheduledPointsOfInterest().isEmpty());
        for (StratConScheduledPointOfInterest pointOfInterest : scheduleState.getScheduledPointsOfInterest()) {
            assertTrue(pointOfInterest.getInitialState().isEmpty());
        }
    }

    @Test
    void pirateCaptainsAreNotObjectivesThemselves() {
        assertFalse(StratConContractInitializer.isSpecialPointOfInterestObjective(
              StratConPirateCaptainBehavior.TYPE_ID), "the fight with a captain is the objective, not the captain");
    }

    @Test
    void showsOfForceKeepEssentialScenariosAndAreNotObjectives() {
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(
              contract(ContractObjectiveType.GARRISON_DUTY, 1), true));
        assertFalse(StratConContractInitializer.isSpecialPointOfInterestObjective(StratConShowOfForceBehavior.TYPE_ID),
              "a show of force only calms Escalation");
    }

    @Test
    void beleagueredForcesDoNotReplaceEssentialScenarios() {
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(
              contract(ContractObjectiveType.RELIEF_DUTY, 1), true));
    }

    @Test
    void trainingManeuversDoNotReplaceEssentialScenarios() {
        assertEquals(StratConTrainingManeuversBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.CADRE_DUTY, 1), true));
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(
              contract(ContractObjectiveType.CADRE_DUTY, 1), true));
    }

    @Test
    void anExtractionRaidContractSchedulesOnlyVIPsEachAnObjective() {
        assertEquals(VIP.getTypeId(),
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.EXTRACTION_RAID, 1), true));

        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.EXTRACTION_RAID, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(VIP.getTypeId(), pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void aPirateRaidContractSchedulesOnlyPlunderTargetsEachAnObjective() {
        assertEquals(StratConPlunderTargetBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.PIRATE_RAID, 1), true));

        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.PIRATE_RAID, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(StratConPlunderTargetBehavior.TYPE_ID, pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void aTerrorismContractSchedulesOnlyCivilianInfrastructureEachAnObjective() {
        assertEquals(StratConCivilianInfrastructureBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.TERRORISM, 1), true));

        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.TERRORISM, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(StratConCivilianInfrastructureBehavior.TYPE_ID, pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void aSabotageContractSchedulesOnlySabotageTargetsEachAnObjective() {
        assertEquals(StratConSabotageTargetBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.SABOTAGE, 1), true));

        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.SABOTAGE, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(StratConSabotageTargetBehavior.TYPE_ID, pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void aRiotDutyContractSchedulesOnlyCivilDisobedienceEachAnObjective() {
        assertEquals(StratConCivilDisobedienceBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.RIOT_DUTY, 1), true));

        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.RIOT_DUTY, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(StratConCivilDisobedienceBehavior.TYPE_ID, pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void withTheOptionOffARiotDutyContractHasNoRiotsAndKeepsItsEssentialScenarios() {
        // The weekly riot event is gone, so with the option off a Riot Duty contract runs as an ordinary contract:
        // its definition's points of interest, no Civil Disobedience, and its Essential scenarios.
        AbstractContract contract = contract(ContractObjectiveType.RIOT_DUTY, 1);
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(contract, false));
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(contract, false));

        List<StratConScheduledPointOfInterest> scheduled = schedule(contract, true, false);

        assertFalse(scheduled.isEmpty(), "the definition's points of interest are still scheduled");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertFalse(StratConCivilDisobedienceBehavior.TYPE_ID.equals(pointOfInterest.getTypeId()),
                  "no Civil Disobedience with the option off");
        }
    }

    @Test
    void aRetainerContractSchedulesOnlyScheduledParadesEachAnObjective() {
        assertEquals(StratConScheduledParadeBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.RETAINER, 1), true));

        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.RETAINER, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(StratConScheduledParadeBehavior.TYPE_ID, pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void strategicPositionsDoNotReplaceEssentialScenarios() {
        assertEquals(STRATEGIC_POSITION.getTypeId(),
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.PLANETARY_ASSAULT, 1), true));
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(
              contract(ContractObjectiveType.PLANETARY_ASSAULT, 1), true));
    }

    @Test
    void aPlanetaryAssaultContractSchedulesOnlyStrategicPositionsEachAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.PLANETARY_ASSAULT, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(STRATEGIC_POSITION.getTypeId(), pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void securityReviewsDoNotReplaceEssentialScenarios() {
        assertEquals(SECURITY_REVIEW.getTypeId(),
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.SECURITY_DUTY, 1), true));
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(
              contract(ContractObjectiveType.SECURITY_DUTY, 1), true));
    }

    @Test
    void aSecurityDutyContractSchedulesOnlySecurityReviewsEachAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.SECURITY_DUTY, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(SECURITY_REVIEW.getTypeId(), pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void highProfileTargetsReplaceEssentialScenarios() {
        assertEquals(StratConHighProfileTargetBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.DIVERSIONARY_RAID, 1), true));
        assertTrue(StratConContractInitializer.isReplacingEssentialScenarios(
              contract(ContractObjectiveType.DIVERSIONARY_RAID, 1), true));
    }

    @Test
    void aDiversionaryRaidContractSchedulesOnlyHighProfileTargetsNoneAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.DIVERSIONARY_RAID, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(StratConHighProfileTargetBehavior.TYPE_ID, pointOfInterest.getTypeId());
            assertFalse(pointOfInterest.isStrategicObjective(), "a Diversionary Raid's objective is its Escalation");
        }
    }

    /** The special points of interest that are not strategic objectives; every other one is. */
    private static final Set<String> NON_OBJECTIVE_SPECIAL_POINTS_OF_INTEREST = Set.of(
          StratConHighProfileTargetBehavior.TYPE_ID,
          StratConShowOfForceBehavior.TYPE_ID,
          StratConPirateCaptainBehavior.TYPE_ID,
          StratConTargetIntelligenceBehavior.TYPE_ID);

    @ParameterizedTest
    @EnumSource(ContractObjectiveType.class)
    void eachContractTypesSpecialPointOfInterestIsAnObjectiveUnlessItIsOneOfTheFourThatAreNot(
          ContractObjectiveType objectiveType) {
        String typeId = StratConContractInitializer.getSpecialPointOfInterestTypeId(contract(objectiveType, 1), true);
        if (typeId == null) {
            return;
        }

        assertEquals(!NON_OBJECTIVE_SPECIAL_POINTS_OF_INTEREST.contains(typeId),
              StratConContractInitializer.isSpecialPointOfInterestObjective(typeId),
              objectiveType + "'s " + typeId);
    }

    @Test
    void aCadreDutyContractSchedulesOnlyTrainingManeuversEachAnObjective() {
        List<StratConScheduledPointOfInterest> scheduled =
              schedule(contract(ContractObjectiveType.CADRE_DUTY, 1), true, true);

        assertEquals(SCALE, scheduled.size(), "rolled like data caches: once per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertEquals(StratConTrainingManeuversBehavior.TYPE_ID, pointOfInterest.getTypeId());
            assertTrue(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void aContractWithoutSpecialPointsOfInterestKeepsItsEssentialScenarios() {
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(
              contract(ContractObjectiveType.UNDEFINED, 1), true));
        assertFalse(StratConContractInitializer.isReplacingEssentialScenarios(contract(null, 1), true));
    }

    @Test
    void aContractWithoutAnObjectiveTypeGetsNoSpecialPointsOfInterest() {
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(contract(null, 1), true));
    }
}
