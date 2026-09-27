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
package mekhq.campaign.roleplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import megamek.common.util.weightedMaps.WeightedIntMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RandomOracleGeneratorTest {

    private static WeightedIntMap<String> pool(final String... meanings) {
        WeightedIntMap<String> map = new WeightedIntMap<>();
        for (String meaning : meanings) {
            map.add(1, meaning);
        }
        return map;
    }

    @BeforeEach
    void reset() {
        RandomOracleGenerator.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        RandomOracleGenerator.resetForTesting();
    }

    @Test
    void everyTableHasADistinctWellFormedPath() {
        Set<String> paths = new HashSet<>();
        for (OracleTable table : OracleTable.values()) {
            assertTrue(table.getFilePath().endsWith(".csv"), table.name());
            assertTrue(table.getUserFilePath().startsWith("userdata"), table.name());
            assertTrue(paths.add(table.getFilePath()), "Duplicate path for " + table.name());
        }
    }

    @Test
    void generateDrawsFromTheRequestedTable() {
        Map<OracleTable, WeightedIntMap<String>> pools = new EnumMap<>(OracleTable.class);
        pools.put(OracleTable.THEMES_FEAR, pool("dread"));
        pools.put(OracleTable.THEMES_TRUST, pool("faith"));

        RandomOracleGenerator generator = RandomOracleGenerator.createForTesting(pools);

        assertEquals("dread", generator.generate(OracleTable.THEMES_FEAR));
        assertEquals("faith", generator.generate(OracleTable.THEMES_TRUST));
    }

    @Test
    void generateReturnsNullForMissingTable() {
        RandomOracleGenerator generator = RandomOracleGenerator.createForTesting(new EnumMap<>(OracleTable.class));
        assertNull(generator.generate(OracleTable.ADVENTURE_TWIST));
    }

    @Test
    void loadMeaningsParsesRowsSkippingHeaderBlankAndMalformedLines(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("table.csv");
        Files.writeString(file, "Meaning,Weight\nvictory,3\n\ndefeat, 1\nbroken\ntoo,many,fields\n",
              StandardCharsets.UTF_8);

        Map<String, Integer> meanings = new HashMap<>();
        RandomOracleGenerator.loadMeanings(file.toFile(), meanings);

        assertEquals(Map.of("victory", 3, "defeat", 1), meanings);
    }

    @Test
    void loadMeaningsIgnoresMissingFile(@TempDir Path directory) {
        Map<String, Integer> meanings = new HashMap<>();
        RandomOracleGenerator.loadMeanings(new File(directory.toFile(), "missing.csv"), meanings);
        assertTrue(meanings.isEmpty());
    }

    @Test
    void userOverrideReplacesBaseWeight(@TempDir Path directory) throws IOException {
        Path base = directory.resolve("base.csv");
        Path user = directory.resolve("user.csv");
        Files.writeString(base, "Meaning,Weight\nvictory,1\ndefeat,1\n", StandardCharsets.UTF_8);
        Files.writeString(user, "Meaning,Weight\nvictory,5\n", StandardCharsets.UTF_8);

        Map<String, Integer> meanings = new HashMap<>();
        RandomOracleGenerator.loadMeanings(base.toFile(), meanings);
        RandomOracleGenerator.loadMeanings(user.toFile(), meanings);

        assertEquals(5, meanings.get("victory"));
        assertEquals(1, meanings.get("defeat"));
        assertFalse(meanings.containsKey("Meaning"));
    }

    @Test
    void aBadWeightSkipsOnlyThatLine(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("table.csv");
        Files.writeString(file, "Meaning,Weight\nvictory,3\ndefeat,lots\nstalemate,2\n", StandardCharsets.UTF_8);

        Map<String, Integer> meanings = new HashMap<>();
        RandomOracleGenerator.loadMeanings(file.toFile(), meanings);

        assertEquals(Map.of("victory", 3, "stalemate", 2), meanings);
    }

    @Test
    void anEmptyFileLoadsNothing(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("empty.csv");
        Files.writeString(file, "", StandardCharsets.UTF_8);

        Map<String, Integer> meanings = new HashMap<>();
        RandomOracleGenerator.loadMeanings(file.toFile(), meanings);

        assertTrue(meanings.isEmpty());
    }
}
