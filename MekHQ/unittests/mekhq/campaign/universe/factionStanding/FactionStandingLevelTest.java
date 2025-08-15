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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.FALLBACK_LABEL_SUFFIX_CLAN;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.FALLBACK_LABEL_SUFFIX_INNER_SPHERE;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.FALLBACK_LABEL_SUFFIX_PERIPHERY;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_0;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_8;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.getFallbackSuffix;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.getPolarityOfModifier;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import megamek.common.universe.FactionTag;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FactionStandingLevelTest {
    private final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingLevel";
    private static final List<String> SUPPORTED_FACTIONS = List.of("CC", "DC", "LA", "FS", "FWL", "TH",
          "ROS", "SL", "FC", "RWR", "TC", "MOC", "OA", "MH", "TD", "CS", "WOB", "innerSphere", "periphery", "clan");

    @Test
    void test_allStandingLevelsAreExclusive() {
        List<Integer> standingLevels = new ArrayList<>();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            int currentLevel = standingLevel.getStandingLevel();
            assertFalse(standingLevels.contains(currentLevel),
                  "The standing level of " + standingLevel.name() + " is not exclusive.");
            standingLevels.add(currentLevel);
        }
    }

    @Test
    void test_allStandingLevelsAreSequential() {
        int lastStandingLevel = STANDING_LEVEL_0.getStandingLevel() - 1;
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            int currentLevel = standingLevel.getStandingLevel();
            int expectedLevel = lastStandingLevel + 1;
            assertEquals(expectedLevel, currentLevel,
                  "The standing level of " + standingLevel.name() + " should be " + expectedLevel + ".");
            lastStandingLevel = currentLevel;
        }
    }

    @Test
    void test_allRegardValuesArePossible() {
        int minimumRegard = (int) STANDING_LEVEL_0.getMinimumRegard();
        int maximumRegard = (int) STANDING_LEVEL_8.getMaximumRegard();
        for (int regard = minimumRegard; regard <= maximumRegard; ++regard) {
            FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(regard);
            assertNotNull(factionStanding, "Faction Standing Level is null for " + regard + " regard.");
        }
    }

    @Test
    void test_allRegardValuesAreSequential() {
        int minimumRegard = (int) STANDING_LEVEL_0.getMinimumRegard();
        int maximumRegard = (int) STANDING_LEVEL_8.getMaximumRegard();

        int lastStandingLevel = STANDING_LEVEL_0.getStandingLevel();
        for (int regard = minimumRegard; regard <= maximumRegard; ++regard) {
            FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(regard);
            int currentStandingLevel = factionStanding.getStandingLevel();
            assertTrue(currentStandingLevel >= lastStandingLevel,
                  "The standing level for " +
                        regard +
                        " regard should be greater than or equal to " +
                        lastStandingLevel +
                        ".");
            lastStandingLevel = currentStandingLevel;
        }
    }

    @Test
    void test_negotiationModifiersAlwaysImprove() {
        int lastNegotiationModifier = STANDING_LEVEL_0.getNegotiationModifier();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            int currentNegotiationModifier = standingLevel.getNegotiationModifier();
            assertTrue(currentNegotiationModifier >= lastNegotiationModifier,
                  "The negotiation modifier of " +
                        standingLevel.name() +
                        " should be greater than or equal to " +
                        lastNegotiationModifier +
                        ".");
            lastNegotiationModifier = currentNegotiationModifier;
        }
    }

    @Test
    void test_resupplyWeightModifiersAlwaysImprove() {
        double lastResupplyModifier = STANDING_LEVEL_0.getResupplyWeightModifier();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            double currentResupplyModifier = standingLevel.getResupplyWeightModifier();
            assertTrue(currentResupplyModifier >= lastResupplyModifier,
                  "The resupply weight modifier of " +
                        standingLevel.name() +
                        " should be greater than or equal to " +
                        lastResupplyModifier +
                        ".");
            lastResupplyModifier = currentResupplyModifier;
        }
    }

    @Test
    void test_commandCircuitAccessAlwaysImproves() {
        boolean lastAccess = false;
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            boolean currentAccess = standingLevel.hasCommandCircuitAccess();
            if (lastAccess) {
                assertTrue(currentAccess, "The command circuit access of " + standingLevel.name() + " should be true.");
            }
            lastAccess = currentAccess;
        }
    }

    @Test
    void test_outlawStatusAlwaysImproves() {
        boolean lastStatus = false;

        FactionStandingLevel[] levels = FactionStandingLevel.values();
        for (int i = levels.length - 1; i >= 0; i--) {
            FactionStandingLevel standingLevel = levels[i];
            boolean currentStatus = standingLevel.isOutlawed();
            if (lastStatus) {
                assertTrue(currentStatus, "The outlaw status of " + standingLevel.name() + " should be true.");
            }
            lastStatus = currentStatus;
        }
    }

    @Test
    void test_batchallAllowedStatusAlwaysImproves() {
        boolean lastAllowance = false;
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            boolean currentAllowance = standingLevel.isBatchallAllowed();
            if (lastAllowance) {
                assertTrue(currentAllowance, "The batchall allowance of " + standingLevel.name() + " should be true.");
            }
            lastAllowance = currentAllowance;
        }
    }

    @Test
    void test_recruitmentTicketsAlwaysImprove() {
        int lastTickets = STANDING_LEVEL_0.getRecruitmentTickets();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            int currentTickets = standingLevel.getRecruitmentTickets();
            assertTrue(currentTickets >= lastTickets,
                  "The recruitment tickets of " +
                        standingLevel.name() +
                        " should be greater than or equal to " +
                        lastTickets +
                        ".");
            lastTickets = currentTickets;
        }
    }

    @Test
    void test_recruitmentRollsAlwaysImprove() {
        double lastRolls = STANDING_LEVEL_0.getRecruitmentRollsModifier();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            double currentRolls = standingLevel.getRecruitmentRollsModifier();
            assertTrue(currentRolls >= lastRolls,
                  "The recruitment roll modifier for " +
                        standingLevel.name() +
                        " should be greater than or equal to " +
                        lastRolls +
                        ".");
            lastRolls = currentRolls;
        }
    }

    @Test
    void test_barrackCostMultipliersAlwaysImprove() {
        double lastMultiplier = STANDING_LEVEL_0.getBarrackCostsMultiplier();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            double currentMultiplier = standingLevel.getBarrackCostsMultiplier();
            assertTrue(currentMultiplier <= lastMultiplier,
                  "The barrack cost modifier for " + standingLevel.name() + " should be less than or equal to " +
                        lastMultiplier + ".");
            lastMultiplier = currentMultiplier;
        }
    }

    @Test
    void test_contractPayMultipliersAlwaysImprove() {
        double lastMultiplier = STANDING_LEVEL_0.getContractPayMultiplier();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            double currentMultiplier = standingLevel.getContractPayMultiplier();
            assertTrue(currentMultiplier >= lastMultiplier,
                  "The barrack cost modifier for " + standingLevel.name() + " should be greater than or equal to " +
                        lastMultiplier + ".");
            lastMultiplier = currentMultiplier;
        }
    }

    @Test
    void test_contractStartSupportPointModifiersAlwaysImprove() {
        int lastModifier = STANDING_LEVEL_0.getSupportPointModifierContractStart();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            int currentRolls = standingLevel.getSupportPointModifierContractStart();
            assertTrue(currentRolls >= lastModifier,
                  "The contract start support point modifier for " +
                        standingLevel.name() +
                        " should be greater than" +
                        " or equal to " +
                        lastModifier +
                        ".");
            lastModifier = currentRolls;
        }
    }

    @Test
    void test_periodicStartSupportPointModifiersAlwaysImprove() {
        int lastModifier = STANDING_LEVEL_0.getSupportPointModifierPeriodic();
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            int currentRolls = standingLevel.getSupportPointModifierPeriodic();
            assertTrue(currentRolls >= lastModifier,
                  "The periodic support point modifier for " + standingLevel.name() + " should be greater than" +
                        " or equal to " + lastModifier + ".");
            lastModifier = currentRolls;
        }
    }

    static Stream<Arguments> labelsProvider() {
        return Stream.of(FactionStandingLevel.values())
                     .flatMap(level -> SUPPORTED_FACTIONS.stream()
                                             .map(faction -> Arguments.of(level, faction)));
    }

    @ParameterizedTest
    @MethodSource("labelsProvider")
    void test_allLabelsAreValid(FactionStandingLevel standingLevel, String factionCode) {
        String key = "factionStandingLevel." + standingLevel.name() + '.' + factionCode + ".label";
        String label = getTextAt(RESOURCE_BUNDLE, key);

        assertTrue(isResourceKeyValid(label), "The label " + label + " is not valid for " + factionCode + ".");
    }

    @ParameterizedTest
    @MethodSource("labelsProvider")
    void test_allDescriptionsAreValid(FactionStandingLevel standingLevel, String factionCode) {
        String key = "factionStandingLevel." + standingLevel.name() + '.' + factionCode + ".description";
        String description = getTextAt(RESOURCE_BUNDLE, key);

        assertTrue(isResourceKeyValid(description),
              "The description " + description + " is not valid for " + factionCode + ".");
    }

    @Test
    void test_getPolarityOfModifier_positive() {
        String polarity = getPolarityOfModifier(1);
        assertTrue(polarity.contains("+"), "Positive value should be positive.");
    }

    @Test
    void test_getPolarityOfModifier_negative() {
        String polarity = getPolarityOfModifier(-1);
        assertFalse(polarity.contains("+"), "Positive value should not be positive.");
    }

    @Test
    void test_getPolarityOfModifier_zero() {
        String polarity = getPolarityOfModifier(0);
        assertTrue(polarity.contains("+"), "Zero value should be positive.");
    }

    @Test
    void test_fallbackSuffix_clan() {
        Faction faction = new Faction();
        Set<FactionTag> tags = new HashSet<>();
        tags.add(FactionTag.CLAN);
        faction.setTags(tags);

        String fallbackSuffix = getFallbackSuffix(faction);
        assertEquals(FALLBACK_LABEL_SUFFIX_CLAN, fallbackSuffix, "Clan suffix should be returned.");
    }

    @Test
    void test_fallbackSuffix_periphery() {
        Faction faction = new Faction();
        Set<FactionTag> tags = new HashSet<>();
        tags.add(FactionTag.PERIPHERY);
        faction.setTags(tags);

        String fallbackSuffix = getFallbackSuffix(faction);
        assertEquals(FALLBACK_LABEL_SUFFIX_PERIPHERY, fallbackSuffix, "Periphery suffix should be returned.");
    }

    @Test
    void test_fallbackSuffix_other() {
        Faction faction = new Faction();

        String fallbackSuffix = getFallbackSuffix(faction);
        assertEquals(FALLBACK_LABEL_SUFFIX_INNER_SPHERE, fallbackSuffix, "Inner Sphere suffix should be returned.");
    }

    private static Stream<Arguments> fromStringTestCases() {
        return Stream.of(
              Arguments.of("STANDING_LEVEL_1", FactionStandingLevel.STANDING_LEVEL_1),
              Arguments.of("standing_level_2", FactionStandingLevel.STANDING_LEVEL_2),
              Arguments.of("standing level 3", FactionStandingLevel.STANDING_LEVEL_3),
              Arguments.of("0", FactionStandingLevel.STANDING_LEVEL_0),
              Arguments.of("1", FactionStandingLevel.STANDING_LEVEL_1),
              Arguments.of("not_a_level", FactionStandingLevel.STANDING_LEVEL_4),
              Arguments.of("99", FactionStandingLevel.STANDING_LEVEL_4),
              Arguments.of("-1", FactionStandingLevel.STANDING_LEVEL_4),
              Arguments.of(null, FactionStandingLevel.STANDING_LEVEL_4)
        );
    }

    @ParameterizedTest
    @MethodSource("fromStringTestCases")
    void test_fromString(String input, FactionStandingLevel expected) {
        assertEquals(expected, FactionStandingLevel.fromString(input));
    }
}
