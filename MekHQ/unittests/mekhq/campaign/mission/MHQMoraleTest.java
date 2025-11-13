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

import static mekhq.campaign.mission.MHQMorale.getReliabilityModifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import megamek.client.ratgenerator.ForceDescriptor;
import mekhq.campaign.enums.DragoonRating;
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
    @MethodSource("dragoonRatings")
    void testGetReliability_normal(int experienceLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getEnemyQuality()).thenReturn(experienceLevel);

        Faction mockFaction = mock(Faction.class);
        when(mockContract.getEnemy()).thenReturn(mockFaction);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(mockContract);
        int expectedReliability = getReliabilityModifier(experienceLevel);

        assertEquals(expectedReliability, actualReliability,
              String.format("Reliability should match experience level: expected %d but got %d",
                    experienceLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("dragoonRatings")
    void testGetReliability_clan(int experienceLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getEnemyQuality()).thenReturn(experienceLevel);

        Faction mockFaction = mock(Faction.class);
        when(mockContract.getEnemy()).thenReturn(mockFaction);
        when(mockFaction.isClan()).thenReturn(true);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(mockContract);

        int adjustedQuality = Math.min(DragoonRating.DRAGOON_ASTAR.getRating(), experienceLevel + 1);
        int expectedReliability = getReliabilityModifier(adjustedQuality);
        expectedReliability--;

        assertEquals(expectedReliability, actualReliability,
              String.format("Reliability should match experience level: expected %d but got %d",
                    experienceLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("dragoonRatings")
    void testGetReliability_rebel(int experienceLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getEnemyQuality()).thenReturn(experienceLevel);

        Faction mockFaction = mock(Faction.class);
        when(mockContract.getEnemy()).thenReturn(mockFaction);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(true);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(mockContract);

        int expectedReliability = getReliabilityModifier(experienceLevel);
        expectedReliability++;

        assertEquals(expectedReliability, actualReliability,
              String.format("Reliability should match experience level: expected %d but got %d",
                    experienceLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("dragoonRatings")
    void testGetReliability_minor(int experienceLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getEnemyQuality()).thenReturn(experienceLevel);

        Faction mockFaction = mock(Faction.class);
        when(mockContract.getEnemy()).thenReturn(mockFaction);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(true);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(mockContract);

        int expectedReliability = getReliabilityModifier(experienceLevel);
        expectedReliability++;

        assertEquals(expectedReliability, actualReliability,
              String.format("Reliability should match experience level: expected %d but got %d",
                    experienceLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("dragoonRatings")
    void testGetReliability_mercenary(int experienceLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getEnemyQuality()).thenReturn(experienceLevel);

        Faction mockFaction = mock(Faction.class);
        when(mockContract.getEnemy()).thenReturn(mockFaction);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(true);
        when(mockFaction.isPirate()).thenReturn(false);

        int actualReliability = MHQMorale.getReliability(mockContract);

        int expectedReliability = getReliabilityModifier(experienceLevel);
        expectedReliability++;

        assertEquals(expectedReliability, actualReliability,
              String.format("Reliability should match experience level: expected %d but got %d",
                    experienceLevel,
                    actualReliability));
    }

    @ParameterizedTest
    @MethodSource("dragoonRatings")
    void testGetReliability_pirate(int experienceLevel) {
        AtBContract mockContract = mock(AtBContract.class);
        when(mockContract.getEnemyQuality()).thenReturn(experienceLevel);

        Faction mockFaction = mock(Faction.class);
        when(mockContract.getEnemy()).thenReturn(mockFaction);
        when(mockFaction.isClan()).thenReturn(false);
        when(mockFaction.isRebel()).thenReturn(false);
        when(mockFaction.isMinorPower()).thenReturn(false);
        when(mockFaction.isMercenary()).thenReturn(false);
        when(mockFaction.isPirate()).thenReturn(true);

        int actualReliability = MHQMorale.getReliability(mockContract);

        int expectedReliability = getReliabilityModifier(experienceLevel);
        expectedReliability++;

        assertEquals(expectedReliability, actualReliability,
              String.format("Reliability should match experience level: expected %d but got %d",
                    experienceLevel,
                    actualReliability));
    }

    private static Stream<Integer> dragoonRatings() {
        return Stream.of(
              ForceDescriptor.RATING_0,  // DRAGOON_F
              ForceDescriptor.RATING_1,  // DRAGOON_D
              ForceDescriptor.RATING_2,  // DRAGOON_C
              ForceDescriptor.RATING_3,  // DRAGOON_B
              ForceDescriptor.RATING_4,  // DRAGOON_A
              ForceDescriptor.RATING_5   // DRAGOON_ASTAR
        );
    }

    @ParameterizedTest
    @MethodSource("performanceModifierTestCases")
    void testPerformanceModifier(int expectedModifier, int daysToSubtract, int decisiveVictories,
          int victories, int pyrrhicVictories, int decisiveDefeats,
          int defeats, int fleetInBeing, int refusedEngagements) {
        AtBContract mockContract = mock(AtBContract.class);
        List<Scenario> scenarioList = buildScenarioArray(daysToSubtract, decisiveVictories, victories,
              pyrrhicVictories, decisiveDefeats, defeats, fleetInBeing, refusedEngagements);
        when(mockContract.getScenarios()).thenReturn(scenarioList);

        int actualPerformanceModifier = MHQMorale.getPerformanceModifier(TODAY, mockContract,
              DECISIVE_VICTORY_MODIFIER, VICTORY_MODIFIER, DECISIVE_DEFEAT_MODIFIER, DEFEAT_MODIFIER);

        assertEquals(expectedModifier, actualPerformanceModifier,
              String.format("Performance modifier should match experience level: expected %d but got %d",
                    expectedModifier, actualPerformanceModifier));
    }

    private static Stream<Arguments> performanceModifierTestCases() {
        return Stream.of(
              // expectedModifier, daysToSubtract, decisiveV, victories, pyrrhicV, decisiveD, defeats, fleetInBeing, refusedEng
              Arguments.of(DECISIVE_VICTORY_MODIFIER, 7, 1, 2, 0, 0, 2, 0, 0),
              Arguments.of(VICTORY_MODIFIER, 7, 1, 1, 0, 0, 2, 0, 0),
              Arguments.of(VICTORY_MODIFIER, 7, 1, 1, 1, 0, 1, 0, 0),
              Arguments.of(DECISIVE_DEFEAT_MODIFIER, 7, 0, 1, 0, 1, 2, 0, 0),
              Arguments.of(DEFEAT_MODIFIER, 7, 0, 2, 0, 1, 1, 0, 0),
              Arguments.of(DECISIVE_DEFEAT_MODIFIER, 7, 0, 1, 0, 0, 0, 0, 2),
              Arguments.of(DEFEAT_MODIFIER, 7, 0, 2, 0, 1, 0, 1, 0),
              Arguments.of(0, 7, 3, 5, 1, 2, 3, 1, 1)
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
              String.format("Performance modifier should match experience level: expected %d but got %d",
                    expectedPerformanceModifier, actualPerformanceModifier));
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
}
