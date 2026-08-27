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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RandomOperationNameGeneratorTest {

    private static WeightedIntMap<String> pool(final String... words) {
        WeightedIntMap<String> map = new WeightedIntMap<>();
        for (String word : words) {
            map.add(1, word);
        }
        return map;
    }

    @BeforeEach
    void reset() {
        RandomOperationNameGenerator.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        RandomOperationNameGenerator.resetForTesting();
    }

    // --- nounFilePaths: the 20-arm switch must cover every objective with a distinct, well-formed base/user pair ---

    @ParameterizedTest
    @EnumSource(ContractObjectiveType.class)
    void nounFilePathsReturnsAWellFormedPairForEveryObjective(final ContractObjectiveType objectiveType) {
        String[] paths = RandomOperationNameGenerator.nounFilePaths(objectiveType);

        assertNotNull(paths, "no noun paths for " + objectiveType);
        assertEquals(2, paths.length, "expected {base, user} for " + objectiveType);
        assertNotNull(paths[0]);
        assertNotNull(paths[1]);
        assertFalse(paths[0].isBlank());
        assertFalse(paths[1].isBlank());
        assertNotEquals(paths[0], paths[1], "base and user paths must differ for " + objectiveType);
        assertTrue(paths[0].contains("randomOperationNameGenerator"),
              "base path should live under the generator's data dir for " + objectiveType);
        assertTrue(paths[1].contains("userdata"), "user override path should live under userdata for " + objectiveType);
    }

    @Test
    void everyObjectiveMapsToADistinctNounFile() {
        Set<String> basePaths = new HashSet<>();
        Set<String> userPaths = new HashSet<>();
        for (ContractObjectiveType objectiveType : ContractObjectiveType.values()) {
            String[] paths = RandomOperationNameGenerator.nounFilePaths(objectiveType);
            assertTrue(basePaths.add(paths[0]), "duplicate base noun file mapped for " + objectiveType);
            assertTrue(userPaths.add(paths[1]), "duplicate user noun file mapped for " + objectiveType);
        }
        assertEquals(ContractObjectiveType.values().length, basePaths.size());
    }

    // --- generation over seeded pools ---

    @Test
    void generateDescriptorReturnsAWordFromTheDescriptorPool() {
        RandomOperationNameGenerator generator = RandomOperationNameGenerator.createForTesting(
              pool("RED", "IRON", "SILENT"), Map.of());

        Set<String> allowed = Set.of("RED", "IRON", "SILENT");
        for (int i = 0; i < 50; i++) {
            assertTrue(allowed.contains(generator.generateDescriptor()));
        }
    }

    @Test
    void generateNounReturnsAWordFromThatObjectivesPool() {
        Map<ContractObjectiveType, WeightedIntMap<String>> nouns =
              new EnumMap<>(ContractObjectiveType.class);
        nouns.put(ContractObjectiveType.GARRISON_DUTY, pool("BULWARK", "BASTION"));
        RandomOperationNameGenerator generator = RandomOperationNameGenerator.createForTesting(pool("RED"), nouns);

        Set<String> allowed = Set.of("BULWARK", "BASTION");
        for (int i = 0; i < 50; i++) {
            assertTrue(allowed.contains(generator.generateNoun(ContractObjectiveType.GARRISON_DUTY)));
        }
    }

    @Test
    void generateNounFallsBackToUndefinedWhenTheObjectivePoolIsEmpty() {
        Map<ContractObjectiveType, WeightedIntMap<String>> nouns =
              new EnumMap<>(ContractObjectiveType.class);
        nouns.put(ContractObjectiveType.GARRISON_DUTY, pool()); // present but empty
        nouns.put(ContractObjectiveType.UNDEFINED, pool("FALLBACK"));
        RandomOperationNameGenerator generator = RandomOperationNameGenerator.createForTesting(pool("RED"), nouns);

        assertEquals("FALLBACK", generator.generateNoun(ContractObjectiveType.GARRISON_DUTY));
    }

    @Test
    void generateNounFallsBackToUndefinedWhenTheObjectiveHasNoPoolAtAll() {
        Map<ContractObjectiveType, WeightedIntMap<String>> nouns =
              new EnumMap<>(ContractObjectiveType.class);
        nouns.put(ContractObjectiveType.UNDEFINED, pool("FALLBACK"));
        RandomOperationNameGenerator generator = RandomOperationNameGenerator.createForTesting(pool("RED"), nouns);

        assertEquals("FALLBACK", generator.generateNoun(ContractObjectiveType.PIRATE_HUNTING));
    }

    @Test
    void generateReturnsNullWhenPoolsAreUnavailable() {
        // Null descriptor pool and no noun pools at all (not even UNDEFINED).
        RandomOperationNameGenerator generator = RandomOperationNameGenerator.createForTesting(null, Map.of());

        assertNull(generator.generateDescriptor());
        assertNull(generator.generateNoun(ContractObjectiveType.GARRISON_DUTY));
    }

    @Test
    void weightingBiasesTheDrawTowardHeavierWords() {
        WeightedIntMap<String> descriptor = new WeightedIntMap<>();
        descriptor.add(99, "COMMON");
        descriptor.add(1, "RARE");
        RandomOperationNameGenerator generator = RandomOperationNameGenerator.createForTesting(descriptor, Map.of());

        int common = 0;
        for (int i = 0; i < 400; i++) {
            if ("COMMON".equals(generator.generateDescriptor())) {
                common++;
            }
        }
        assertTrue(common > 200, "heavily weighted word should dominate; got " + common + "/400");
    }

    @Test
    void resetForTestingClearsSeededState() {
        RandomOperationNameGenerator.createForTesting(pool("RED"), Map.of());
        RandomOperationNameGenerator.resetForTesting();
        // After reset the descriptor pool is gone; a fresh instance would reload from files (not exercised here).
        assertNull(RandomOperationNameGenerator.getWeightedDescriptor());
    }
}
