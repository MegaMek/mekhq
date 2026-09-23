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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.ObjectiveParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.PointOfInterestParameters;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConDataCacheBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConLookoutPointBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPotentialLeadBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConVulnerableInfrastructureBehavior;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
        return StratConDataCacheBehavior.TYPE_ID.equals(pointOfInterest.getTypeId());
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
    @SuppressWarnings("unchecked") // ArgumentCaptor cannot name a generic List type without an unchecked conversion
    void theRolledScheduleIsKeptOnTheContractAndMatchesTheCaches() {
        AbstractContract contract = contract(ContractObjectiveType.ESPIONAGE, 3);

        List<StratConScheduledPointOfInterest> scheduled = schedule(contract, true, true);

        ArgumentCaptor<List<Integer>> scheduleCaptor = ArgumentCaptor.forClass(List.class);
        verify(contract).setPointOfInterestSchedule(scheduleCaptor.capture());
        List<Integer> monthlyCounts = scheduleCaptor.getValue();
        assertEquals(LENGTH_IN_MONTHS, monthlyCounts.size(), "a three-month contract uses the three-month table");

        int scheduledCount = 0;
        for (int monthlyCount : monthlyCounts) {
            scheduledCount += monthlyCount;
        }
        assertEquals(scheduledCount, scheduled.size(), "one cache per slot in the rolled schedule");
        assertEquals(3 * SCALE, scheduledCount, "each roll of a three-track column places three");
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
        verify(contract, never()).setPointOfInterestSchedule(any());
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
        List<StratConScheduledPointOfInterest> scheduled = schedule(contract(ContractObjectiveType.GARRISON_DUTY, 1),
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
            assertEquals(StratConVulnerableInfrastructureBehavior.TYPE_ID, pointOfInterest.getTypeId());
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
            assertEquals(StratConPotentialLeadBehavior.TYPE_ID, pointOfInterest.getTypeId());
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
            assertEquals(StratConLookoutPointBehavior.TYPE_ID, pointOfInterest.getTypeId());
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

    // Which contracts use special points of interest - and so get no Essential scenarios

    @Test
    void eachContractTypeWithSpecialMechanicsGetsItsOwnSpecialPointOfInterest() {
        assertEquals(StratConDataCacheBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(contract(ContractObjectiveType.ESPIONAGE,
                    1), true));
        assertEquals(StratConVulnerableInfrastructureBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.GUERRILLA_WARFARE, 1), true));
        assertEquals(StratConPotentialLeadBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.MOLE_HUNTING, 1), true));
        assertEquals(StratConLookoutPointBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(
                    contract(ContractObjectiveType.OBSERVATION_RAID, 1), true));
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(
              contract(ContractObjectiveType.GARRISON_DUTY, 1), true));
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

    @Test
    void aContractWithoutAnObjectiveTypeGetsNoSpecialPointsOfInterest() {
        assertNull(StratConContractInitializer.getSpecialPointOfInterestTypeId(contract(null, 1), true));
    }
}
