/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static mekhq.campaign.mission.MHQMorale.NO_CHANGE_TARGET_NUMBER;
import static mekhq.campaign.mission.MHQMorale.RALLYING_TARGET_NUMBER;
import static mekhq.campaign.mission.MHQMorale.WAVERING_TARGET_NUMBER;
import static mekhq.campaign.mission.MHQMorale.getMoraleOutcome;
import static mekhq.campaign.mission.MHQMorale.getReliabilityModifier;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.ADVANCING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.CRITICAL;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.DOMINATING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.OVERWHELMING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.ROUTED;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.WEAKENED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import megamek.client.ratgenerator.ForceDescriptor;
import mekhq.campaign.enums.DragoonRating;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.enums.ScenarioStatus;
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
        int expectedReliability = getReliabilityModifier(adjustedSkillLevel);

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
        int expectedReliability = getReliabilityModifier(adjustedQuality);
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

        int expectedReliability = getReliabilityModifier(adjustedSkillLevel);
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

        int expectedReliability = getReliabilityModifier(adjustedSkillLevel);
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

        int expectedReliability = getReliabilityModifier(adjustedSkillLevel);
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

        int expectedReliability = getReliabilityModifier(adjustedSkillLevel);
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
        AtBContract mockContract = mock(AtBContract.class);
        List<Scenario> scenarioList = buildScenarioArray(daysToSubtract, decisiveVictories, victories,
              pyrrhicVictories, decisiveDefeats, defeats, fleetInBeing, refusedEngagements);
        when(mockContract.getScenarios()).thenReturn(scenarioList);

        int actualPerformanceModifier = MHQMorale.getPerformanceModifier(TODAY, mockContract,
              DECISIVE_VICTORY_MODIFIER, VICTORY_MODIFIER, DECISIVE_DEFEAT_MODIFIER, DEFEAT_MODIFIER);

        assertEquals(expectedModifier, actualPerformanceModifier,
              String.format("Expected %d but got %d",
                    expectedModifier, actualPerformanceModifier));
    }

    private static Stream<Arguments> performanceModifierTestCases() {
        return Stream.of(
              // expectedModifier, daysToSubtract, decisiveV, victories, pyrrhicV, decisiveD, defeats, fleetInBeing, refusedEng
              Arguments.of(DECISIVE_VICTORY_MODIFIER, 7, 1, 2, 0, 0, 2, 0, 0),
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
        AtBContract mockContract = mock(AtBContract.class);
        List<Scenario> scenarioList = buildScenarioArray(7, 0, 1, 0, 0, 0, 0, 0);
        scenarioList.addAll(buildScenarioArray(357, 0, 1, 0, 0, 2, 0, 0));
        when(mockContract.getScenarios()).thenReturn(scenarioList);

        int expectedPerformanceModifier = DECISIVE_VICTORY_MODIFIER;
        int actualPerformanceModifier = MHQMorale.getPerformanceModifier(TODAY, mockContract,
              DECISIVE_VICTORY_MODIFIER, VICTORY_MODIFIER, DECISIVE_DEFEAT_MODIFIER, DEFEAT_MODIFIER);

        assertEquals(expectedPerformanceModifier, actualPerformanceModifier,
              String.format("Expected %d but got %d",
                    expectedPerformanceModifier, actualPerformanceModifier));
    }

    @Test
    void testPerformanceModifier_scenariosWithNullDateAreIgnored() {
        AtBContract mockContract = mock(AtBContract.class);
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

        int expectedModifier = DECISIVE_VICTORY_MODIFIER; // only the recent victory should count
        int actualModifier = MHQMorale.getPerformanceModifier(
              TODAY, mockContract,
              DECISIVE_VICTORY_MODIFIER, VICTORY_MODIFIER,
              DECISIVE_DEFEAT_MODIFIER, DEFEAT_MODIFIER);

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
    void testGetOutcome(MHQMorale.PerformanceOutcome expectedOutcome, int scoreModifier) {
        MHQMorale.PerformanceOutcome actualPerformanceOutcome = MHQMorale.getOutcome(
              DECISIVE_VICTORY_MODIFIER,
              VICTORY_MODIFIER,
              DECISIVE_DEFEAT_MODIFIER,
              DEFEAT_MODIFIER,
              scoreModifier);

        assertEquals(expectedOutcome, actualPerformanceOutcome,
              String.format("Performance outcome should match expected: expected %s but got %s",
                    expectedOutcome,
                    actualPerformanceOutcome));
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

    @ParameterizedTest
    @MethodSource("moraleRallyingCases")
    void testGetMoraleOutcome_rallying(AtBMoraleLevel startLevel, AtBMoraleLevel expectedLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getMoraleLevel()).thenReturn(startLevel);

        MHQMorale.MoraleOutcome outcome = getMoraleOutcome(mockContract, RALLYING_TARGET_NUMBER);

        assertEquals(MHQMorale.MoraleOutcome.RALLYING, outcome);

        if (expectedLevel != startLevel) {
            verify(mockContract).setMoraleLevel(expectedLevel);
        } else {
            verify(mockContract, never()).setMoraleLevel(any());
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
    void testGetMoraleOutcome_wavering(AtBMoraleLevel startLevel, AtBMoraleLevel expectedLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getMoraleLevel()).thenReturn(startLevel);

        MHQMorale.MoraleOutcome outcome = getMoraleOutcome(mockContract, WAVERING_TARGET_NUMBER);

        assertEquals(MHQMorale.MoraleOutcome.WAVERING, outcome);

        if (expectedLevel != startLevel) {
            verify(mockContract).setMoraleLevel(expectedLevel);
        } else {
            verify(mockContract, never()).setMoraleLevel(any());
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
    void testGetMoraleOutcome_noChange(AtBMoraleLevel startLevel, AtBMoraleLevel expectedLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getMoraleLevel()).thenReturn(startLevel);

        MHQMorale.MoraleOutcome outcome = getMoraleOutcome(mockContract, NO_CHANGE_TARGET_NUMBER);

        assertEquals(MHQMorale.MoraleOutcome.UNCHANGED, outcome);

        if (expectedLevel != startLevel) {
            verify(mockContract).setMoraleLevel(expectedLevel);
        } else {
            verify(mockContract, never()).setMoraleLevel(any());
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
}
