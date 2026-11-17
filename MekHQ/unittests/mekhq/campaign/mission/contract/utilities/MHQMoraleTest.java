/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.ADVANCING;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.CRITICAL;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.DOMINATING;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.OVERWHELMING;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.ROUTED;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.STALEMATE;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.WEAKENED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.ScenarioStatus;
import mekhq.campaign.mission.scenarios.ScenarioType;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MHQMoraleTest {
    private static final LocalDate TODAY = LocalDate.of(3151, 1, 1);
    private static final int DECISIVE_VICTORY_MODIFIER = 2;
    private static final int VICTORY_MODIFIER = 1;
    private static final int DECISIVE_DEFEAT_MODIFIER = -2;
    private static final int DEFEAT_MODIFIER = -1;

    @ParameterizedTest
    @MethodSource("adjustedSkillLevels")
    void testGetReliability_normal(int adjustedSkillLevel) {
        Faction mockFaction = mock(Faction.class);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(adjustedSkillLevel, mockFaction);
        int expectedReliability = MHQMorale.getReliabilityModifier(adjustedSkillLevel);

        assertEquals(expectedReliability, actualReliability,
              String.format("Expected %d but got %d",
                    adjustedSkillLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("adjustedSkillLevels")
    void testGetReliability_clan(int adjustedSkillLevel) {
        Faction mockFaction = mock(Faction.class);
        when(mockFaction.isClan()).thenReturn(true);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(adjustedSkillLevel, mockFaction);

        int adjustedQuality = Math.min(SkillLevel.LEGENDARY.getAdjustedValue(), adjustedSkillLevel + 1);
        int expectedReliability = MHQMorale.getReliabilityModifier(adjustedQuality);
        expectedReliability--;

        assertEquals(expectedReliability, actualReliability,
              String.format("Reliability should match expected level: expected %d but got %d",
                    adjustedSkillLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("adjustedSkillLevels")
    void testGetReliability_rebel(int adjustedSkillLevel) {
        Faction mockFaction = mock(Faction.class);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(true);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(adjustedSkillLevel, mockFaction);

        int expectedReliability = MHQMorale.getReliabilityModifier(adjustedSkillLevel);
        expectedReliability++;

        assertEquals(expectedReliability, actualReliability,
              String.format("Expected %d but got %d",
                    adjustedSkillLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("adjustedSkillLevels")
    void testGetReliability_minor(int adjustedSkillLevel) {
        Faction mockFaction = mock(Faction.class);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(true);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(adjustedSkillLevel, mockFaction);

        int expectedReliability = MHQMorale.getReliabilityModifier(adjustedSkillLevel);
        expectedReliability++;

        assertEquals(expectedReliability, actualReliability,
              String.format("Expected %d but got %d",
                    adjustedSkillLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("adjustedSkillLevels")
    void testGetReliability_mercenary(int adjustedSkillLevel) {
        Faction mockFaction = mock(Faction.class);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(true);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(adjustedSkillLevel, mockFaction);

        int expectedReliability = MHQMorale.getReliabilityModifier(adjustedSkillLevel);
        expectedReliability++;

        assertEquals(expectedReliability, actualReliability,
              String.format("Expected %d but got %d",
                    adjustedSkillLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("adjustedSkillLevels")
    void testGetReliability_pirate(int adjustedSkillLevel) {
        Faction mockFaction = mock(Faction.class);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(true);

        int actualReliability = MHQMorale.getReliability(adjustedSkillLevel, mockFaction);

        int expectedReliability = MHQMorale.getReliabilityModifier(adjustedSkillLevel);
        expectedReliability++;

        assertEquals(expectedReliability, actualReliability,
              String.format("Expected %d but got %d",
                    adjustedSkillLevel,
                    actualReliability));
    }

    private static Stream<Integer> adjustedSkillLevels() {
        return Stream.of(
              SkillLevel.NONE.getAdjustedValue(),
              SkillLevel.ULTRA_GREEN.getAdjustedValue(),
              SkillLevel.GREEN.getAdjustedValue(),
              SkillLevel.REGULAR.getAdjustedValue(),
              SkillLevel.VETERAN.getAdjustedValue(),
              SkillLevel.ELITE.getAdjustedValue(),
              SkillLevel.HEROIC.getAdjustedValue(),
              SkillLevel.LEGENDARY.getAdjustedValue()
        );
    }

    @ParameterizedTest
    @MethodSource("performanceModifierTestCases")
    void testPerformanceModifier(int expectedModifier, int daysToSubtract, int decisiveVictories, int victories,
          int pyrrhicVictories, int decisiveDefeats, int defeats, int fleetInBeing, int refusedEngagements) {
        AbstractContract mockContract = mock(AbstractContract.class);
        List<Scenario> scenarioList = buildScenarioArray(daysToSubtract, decisiveVictories, victories,
              pyrrhicVictories, decisiveDefeats, defeats, fleetInBeing, refusedEngagements);
        when(mockContract.getScenarios()).thenReturn(scenarioList);

        int actualPerformanceModifier = getPerformanceModifier(mockContract);

        assertEquals(expectedModifier, actualPerformanceModifier,
              String.format("Expected %d but got %d",
                    expectedModifier, actualPerformanceModifier));
    }

    /**
     * Rates the contract's last month and looks up the modifier for that rating, as the morale check does.
     */
    private static int getPerformanceModifier(AbstractContract contract) {
        return MHQMorale.getPerformanceModifier(MHQMorale.getPerformanceOutcome(TODAY, contract),
              DECISIVE_VICTORY_MODIFIER, VICTORY_MODIFIER, DECISIVE_DEFEAT_MODIFIER, DEFEAT_MODIFIER);
    }

    private static Stream<Arguments> performanceModifierTestCases() {
        return Stream.of(
              // expectedModifier, daysToSubtract, decisiveV, victories, pyrrhicV, decisiveD, defeats, fleetInBeing, refusedEng
              // 4 - 2: two to one, but only 2 points ahead, so not decisive
              Arguments.of(VICTORY_MODIFIER, 7, 1, 2, 0, 0, 2, 0, 0),
              // 5 - 2: 3 points ahead is decisive
              Arguments.of(DECISIVE_VICTORY_MODIFIER, 7, 2, 1, 0, 0, 2, 0, 0),
              Arguments.of(VICTORY_MODIFIER, 7, 1, 1, 0, 0, 2, 0, 0),
              Arguments.of(VICTORY_MODIFIER, 7, 1, 1, 1, 0, 2, 0, 0),
              Arguments.of(DECISIVE_DEFEAT_MODIFIER, 7, 0, 1, 0, 1, 2, 0, 0),
              Arguments.of(DEFEAT_MODIFIER, 7, 0, 2, 0, 1, 1, 0, 0),
              Arguments.of(DECISIVE_DEFEAT_MODIFIER, 7, 0, 1, 0, 0, 0, 0, 2),
              Arguments.of(DEFEAT_MODIFIER, 7, 0, 2, 0, 1, 0, 1, 0),
              Arguments.of(0, 7, 3, 5, 1, 2, 4, 1, 1)
        );
    }

    @Test
    void testPerformanceModifier_decisiveVictoryAllDefeatsExpired() {
        AbstractContract mockContract = mock(AbstractContract.class);
        List<Scenario> scenarioList = buildScenarioArray(7, 0, 3, 0, 0, 0, 0, 0);
        scenarioList.addAll(buildScenarioArray(357, 0, 1, 0, 0, 2, 0, 0));
        when(mockContract.getScenarios()).thenReturn(scenarioList);

        int expectedPerformanceModifier = DECISIVE_VICTORY_MODIFIER;
        int actualPerformanceModifier = getPerformanceModifier(mockContract);

        assertEquals(expectedPerformanceModifier, actualPerformanceModifier,
              String.format("Expected %d but got %d",
                    expectedPerformanceModifier, actualPerformanceModifier));
    }

    @Test
    void testPerformanceModifier_scenariosWithNullDateAreIgnored() {
        AbstractContract mockContract = mock(AbstractContract.class);
        List<Scenario> scenarioList = new ArrayList<>();

        // Scenario with null date – should be ignored
        Scenario nullDateScenario = mock(Scenario.class);
        when(nullDateScenario.getDate()).thenReturn(null);
        when(nullDateScenario.getStatus()).thenReturn(ScenarioStatus.DECISIVE_DEFEAT); // would count as 2 defeats
        scenarioList.add(nullDateScenario);

        // Scenario within last month – should be counted
        Scenario recentVictory = mock(Scenario.class);
        when(recentVictory.getDate()).thenReturn(TODAY.minusDays(7));
        when(recentVictory.getStatus()).thenReturn(ScenarioStatus.VICTORY);
        scenarioList.add(recentVictory);

        when(mockContract.getScenarios()).thenReturn(scenarioList);

        int expectedModifier = VICTORY_MODIFIER; // only the recent victory should count
        int actualModifier = getPerformanceModifier(mockContract);

        assertEquals(expectedModifier, actualModifier,
              String.format("Expected %d but got %d",
                    expectedModifier, actualModifier));
    }

    private static List<Scenario> buildScenarioArray(int daysToSubtract, int decisiveVictories, int victories,
          int pyrrhicVictories, int decisiveDefeats, int defeats, int fleetInBeing, int refusedEngagements) {
        List<Scenario> scenarioList = new ArrayList<>();

        addScenarios(scenarioList, daysToSubtract, ScenarioStatus.DECISIVE_VICTORY, decisiveVictories);
        addScenarios(scenarioList, daysToSubtract, ScenarioStatus.VICTORY, victories);
        addScenarios(scenarioList, daysToSubtract, ScenarioStatus.PYRRHIC_VICTORY, pyrrhicVictories);
        addScenarios(scenarioList, daysToSubtract, ScenarioStatus.DECISIVE_DEFEAT, decisiveDefeats);
        addScenarios(scenarioList, daysToSubtract, ScenarioStatus.DEFEAT, defeats);
        addScenarios(scenarioList, daysToSubtract, ScenarioStatus.FLEET_IN_BEING, fleetInBeing);
        addScenarios(scenarioList, daysToSubtract, ScenarioStatus.REFUSED_ENGAGEMENT, refusedEngagements);

        return scenarioList;
    }

    private static void addScenarios(List<Scenario> scenarioList, int daysToSubtract,
          ScenarioStatus status, int count) {
        if (count > 0) {
            Scenario mockScenario = mock(Scenario.class);
            when(mockScenario.getDate()).thenReturn(TODAY.minusDays(daysToSubtract));
            when(mockScenario.getStatus()).thenReturn(status);

            for (int i = 0; i < count; i++) {
                scenarioList.add(mockScenario);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("outcomeTestCases")
    void eachPerformanceOutcomeUsesItsOwnModifier(MHQMorale.PerformanceOutcome outcome, int expectedModifier) {
        int actualModifier = MHQMorale.getPerformanceModifier(outcome,
              DECISIVE_VICTORY_MODIFIER,
              VICTORY_MODIFIER,
              DECISIVE_DEFEAT_MODIFIER,
              DEFEAT_MODIFIER);

        assertEquals(expectedModifier, actualModifier);
    }

    private static Stream<Arguments> outcomeTestCases() {
        return Stream.of(
              Arguments.of(MHQMorale.PerformanceOutcome.DECISIVE_VICTORY, DECISIVE_VICTORY_MODIFIER),
              Arguments.of(MHQMorale.PerformanceOutcome.VICTORY, VICTORY_MODIFIER),
              Arguments.of(MHQMorale.PerformanceOutcome.DRAW, 0),
              Arguments.of(MHQMorale.PerformanceOutcome.DEFEAT, DEFEAT_MODIFIER),
              Arguments.of(MHQMorale.PerformanceOutcome.DECISIVE_DEFEAT, DECISIVE_DEFEAT_MODIFIER)
        );
    }

    @Test
    void aPlainVictoryIsRatedAsSuchEvenWhenTheOptionsGiveItTheDecisiveModifier() {
        // 1 decisive victory + 1 victory = 3 victory points against 2 defeat points: a victory, but not decisive
        AbstractContract mockContract = mock(AbstractContract.class);
        when(mockContract.getScenarios()).thenReturn(buildScenarioArray(7, 1, 1, 0, 0, 2, 0, 0));

        MHQMorale.PerformanceOutcome outcome = MHQMorale.getPerformanceOutcome(TODAY, mockContract);

        assertEquals(MHQMorale.PerformanceOutcome.VICTORY, outcome);
        // With both victory options set to 1, the modifier alone could not tell the two apart
        assertEquals(1, MHQMorale.getPerformanceModifier(outcome, 1, 1, -1, -1));
    }

    @Test
    void eachScenarioStatusAddsItsPointsToTheTally() {
        AbstractContract mockContract = mock(AbstractContract.class);
        // decisiveV, victories, pyrrhicV, decisiveD, defeats, fleetInBeing, refusedEngagements
        when(mockContract.getScenarios()).thenReturn(buildScenarioArray(7, 1, 1, 1, 1, 1, 1, 1));

        MHQMorale.PerformanceTally tally = MHQMorale.getPerformanceTally(TODAY, mockContract);

        assertEquals(2 + 1 + 1, tally.victoryPoints());
        assertEquals(1 + 2 + 1 + 1 + 2, tally.defeatPoints());
    }

    @ParameterizedTest
    @MethodSource("tallyOutcomeCases")
    void aTallyIsRatedByItsMargin(int victoryPoints, int defeatPoints, MHQMorale.PerformanceOutcome expected) {
        assertEquals(expected, new MHQMorale.PerformanceTally(victoryPoints, defeatPoints).outcome());
    }

    private static Stream<Arguments> tallyOutcomeCases() {
        return Stream.of(
              Arguments.of(0, 0, MHQMorale.PerformanceOutcome.DRAW),
              Arguments.of(2, 2, MHQMorale.PerformanceOutcome.DRAW),
              // A lone win in a quiet month is not decisive, however lopsided the ratio
              Arguments.of(1, 0, MHQMorale.PerformanceOutcome.VICTORY),
              Arguments.of(4, 2, MHQMorale.PerformanceOutcome.VICTORY),
              Arguments.of(3, 2, MHQMorale.PerformanceOutcome.VICTORY),
              Arguments.of(3, 0, MHQMorale.PerformanceOutcome.DECISIVE_VICTORY),
              // Sustained success against some setbacks is decisive, though short of two to one
              Arguments.of(9, 5, MHQMorale.PerformanceOutcome.DECISIVE_VICTORY),
              Arguments.of(2, 3, MHQMorale.PerformanceOutcome.DEFEAT),
              Arguments.of(2, 4, MHQMorale.PerformanceOutcome.DEFEAT),
              Arguments.of(0, 1, MHQMorale.PerformanceOutcome.DEFEAT),
              Arguments.of(0, 3, MHQMorale.PerformanceOutcome.DECISIVE_DEFEAT),
              Arguments.of(5, 9, MHQMorale.PerformanceOutcome.DECISIVE_DEFEAT)
        );
    }

    @Test
    void aDrawIsRatedAsSuchEvenWhenAnOptionIsZero() {
        AbstractContract mockContract = mock(AbstractContract.class);
        when(mockContract.getScenarios()).thenReturn(buildScenarioArray(7, 0, 1, 0, 0, 1, 0, 0));

        assertEquals(MHQMorale.PerformanceOutcome.DRAW, MHQMorale.getPerformanceOutcome(TODAY, mockContract));
    }

    @ParameterizedTest
    @MethodSource("moraleRallyingCases")
    void testGetMoraleOutcome_rallying(ContractMoraleLevel startLevel, ContractMoraleLevel expectedLevel) {
        AbstractContract mockContract = mock(AbstractContract.class);
        when(mockContract.getMoraleLevel()).thenReturn(startLevel);

        MHQMorale.MoraleOutcome outcome = MHQMorale.getMoraleOutcome(mockContract, MHQMorale.RALLYING_TARGET_NUMBER);

        assertEquals(MHQMorale.MoraleOutcome.RALLYING, outcome);

        if (expectedLevel != startLevel) {
            verify(mockContract).changeMorale(expectedLevel);
        } else {
            verify(mockContract, never()).changeMorale(any(ContractMoraleLevel.class));
        }
    }

    private static Stream<Arguments> moraleRallyingCases() {
        return Stream.of(
              Arguments.of(ROUTED, CRITICAL),
              Arguments.of(CRITICAL, WEAKENED),
              Arguments.of(WEAKENED, STALEMATE),
              Arguments.of(STALEMATE, ADVANCING),
              Arguments.of(ADVANCING, DOMINATING),
              Arguments.of(DOMINATING, OVERWHELMING),
              Arguments.of(OVERWHELMING, OVERWHELMING)
        );
    }

    @ParameterizedTest
    @MethodSource("moraleWaveringCases")
    void testGetMoraleOutcome_wavering(ContractMoraleLevel startLevel, ContractMoraleLevel expectedLevel) {
        AbstractContract mockContract = mock(AbstractContract.class);
        when(mockContract.getMoraleLevel()).thenReturn(startLevel);

        MHQMorale.MoraleOutcome outcome = MHQMorale.getMoraleOutcome(mockContract, MHQMorale.WAVERING_TARGET_NUMBER);

        assertEquals(MHQMorale.MoraleOutcome.WAVERING, outcome);

        if (expectedLevel != startLevel) {
            verify(mockContract).changeMorale(expectedLevel);
        } else {
            verify(mockContract, never()).changeMorale(any(ContractMoraleLevel.class));
        }
    }

    private static Stream<Arguments> moraleWaveringCases() {
        return Stream.of(
              Arguments.of(ROUTED, ROUTED),
              Arguments.of(CRITICAL, ROUTED),
              Arguments.of(WEAKENED, CRITICAL),
              Arguments.of(STALEMATE, WEAKENED),
              Arguments.of(ADVANCING, STALEMATE),
              Arguments.of(DOMINATING, ADVANCING),
              Arguments.of(OVERWHELMING, DOMINATING)
        );
    }

    @ParameterizedTest
    @MethodSource("moraleNoChangeCases")
    void testGetMoraleOutcome_noChange(ContractMoraleLevel startLevel, ContractMoraleLevel expectedLevel) {
        AbstractContract mockContract = mock(AbstractContract.class);
        when(mockContract.getMoraleLevel()).thenReturn(startLevel);

        MHQMorale.MoraleOutcome outcome = MHQMorale.getMoraleOutcome(mockContract, MHQMorale.NO_CHANGE_TARGET_NUMBER);

        assertEquals(MHQMorale.MoraleOutcome.UNCHANGED, outcome);

        if (expectedLevel != startLevel) {
            verify(mockContract).changeMorale(expectedLevel);
        } else {
            verify(mockContract, never()).changeMorale(any(ContractMoraleLevel.class));
        }
    }

    private static Stream<Arguments> moraleNoChangeCases() {
        return Stream.of(
              Arguments.of(ROUTED, ROUTED),
              Arguments.of(CRITICAL, CRITICAL),
              Arguments.of(WEAKENED, WEAKENED),
              Arguments.of(STALEMATE, STALEMATE),
              Arguments.of(ADVANCING, ADVANCING),
              Arguments.of(DOMINATING, DOMINATING),
              Arguments.of(OVERWHELMING, OVERWHELMING)
        );
    }

    // Scenarios that shift morale directly

    private static AbstractContract contractOfType(ContractObjectiveType objectiveType) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        when(contract.getMoraleLevel()).thenReturn(STALEMATE);
        return contract;
    }

    @Test
    void aRiotShiftsMoraleOnARiotDutyContract() {
        assertTrue(MHQMorale.isScenarioOutcomeAffectingMorale(ScenarioType.RIOT,
              contractOfType(ContractObjectiveType.RIOT_DUTY)));
    }

    @Test
    void aRiotElsewhereLeavesMoraleToTheMoraleCheck() {
        assertFalse(MHQMorale.isScenarioOutcomeAffectingMorale(ScenarioType.RIOT,
              contractOfType(ContractObjectiveType.RETAINER)), "a disrupted parade on a Retainer contract");
        assertFalse(MHQMorale.isScenarioOutcomeAffectingMorale(ScenarioType.RIOT, null));
    }

    @Test
    void otherScenarioTypesStillFollowTheirOwnFlags() {
        AbstractContract retainer = contractOfType(ContractObjectiveType.RETAINER);

        assertTrue(MHQMorale.isScenarioOutcomeAffectingMorale(ScenarioType.AFFECTS_MORALE_BOTH_WAYS, retainer));
        assertFalse(MHQMorale.isScenarioOutcomeAffectingMorale(ScenarioType.NONE, retainer));
    }

    @Test
    void winningARiotOnARiotDutyContractLowersMorale() {
        AbstractContract riotDuty = contractOfType(ContractObjectiveType.RIOT_DUTY);

        MHQMorale.processMoraleChangeFromScenario(mock(Campaign.class), riotDuty, ScenarioStatus.VICTORY,
              ScenarioType.RIOT);

        verify(riotDuty).changeMorale(WEAKENED);
    }

    @Test
    void winningARiotElsewhereLeavesMoraleAlone() {
        AbstractContract retainer = contractOfType(ContractObjectiveType.RETAINER);

        MHQMorale.processMoraleChangeFromScenario(mock(Campaign.class), retainer, ScenarioStatus.VICTORY,
              ScenarioType.RIOT);

        verify(retainer, never()).changeMorale(any(ContractMoraleLevel.class));
    }

    @Test
    void winningAgainstAnAlreadyRoutedGarrisonEnemyDoesNotRepeatTheRout() {
        Campaign campaign = mock(Campaign.class);
        AbstractContract garrison = contractOfType(ContractObjectiveType.GARRISON_DUTY);
        when(garrison.getMoraleLevel()).thenReturn(ROUTED);

        MHQMorale.processMoraleChangeFromScenario(campaign, garrison, ScenarioStatus.VICTORY,
              ScenarioType.AFFECTS_MORALE_BOTH_WAYS);

        // The rout's end date is not pushed back, and its prisoners are not handled a second time
        verify(garrison, never()).changeMorale(any(LocalDate.class));
        verify(campaign, never()).getPlayerForce();
    }
}
