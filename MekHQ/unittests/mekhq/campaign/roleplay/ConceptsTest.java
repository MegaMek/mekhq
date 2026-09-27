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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import megamek.common.util.weightedMaps.WeightedIntMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ConceptsTest {
    @AfterEach
    void tearDown() {
        RandomOracleGenerator.resetForTesting();
    }

    private static RandomOracleGenerator generator() {
        Map<OracleTable, WeightedIntMap<String>> pools = new EnumMap<>(OracleTable.class);
        for (OracleTable table : List.of(OracleTable.THEMES_FEAR, OracleTable.CHARACTERS_MOTIVE,
              OracleTable.ADVENTURE_TWIST, OracleTable.ADVENTURE_CLUE)) {
            WeightedIntMap<String> pool = new WeightedIntMap<>();
            pool.add(1, table.name().toLowerCase());
            pools.put(table, pool);
        }
        return RandomOracleGenerator.createForTesting(pools);
    }

    @Test
    void rollsEachSelectedTableInOrder() {
        List<Concepts.Concept> concepts = Concepts.roll(
              List.of(OracleTable.CHARACTERS_MOTIVE, OracleTable.THEMES_FEAR), generator());

        assertEquals(2, concepts.size());
        assertEquals(OracleTable.CHARACTERS_MOTIVE, concepts.get(0).table());
        assertEquals("characters_motive", concepts.get(0).meaning());
        assertEquals("themes_fear", concepts.get(1).meaning());
    }

    @Test
    void skipsNullsAndDuplicatesAndCapsAtThree() {
        List<Concepts.Concept> concepts = Concepts.roll(Arrays.asList(null, OracleTable.THEMES_FEAR,
              OracleTable.THEMES_FEAR, OracleTable.CHARACTERS_MOTIVE, OracleTable.ADVENTURE_TWIST,
              OracleTable.ADVENTURE_CLUE), generator());

        assertEquals(List.of(OracleTable.THEMES_FEAR, OracleTable.CHARACTERS_MOTIVE, OracleTable.ADVENTURE_TWIST),
              concepts.stream().map(Concepts.Concept::table).toList());
    }

    @Test
    void emptyTableGivesNullMeaning() {
        List<Concepts.Concept> concepts = Concepts.roll(List.of(OracleTable.THEMES_TRUST), generator());
        assertNull(concepts.get(0).meaning());
    }

    @Test
    void labelsReadNaturally() {
        assertEquals("Environments: Natural Weather", OracleTable.ENVIRONMENTS_NATURAL_WEATHER.getLabel());
        assertEquals("Adventure: Dilemma", OracleTable.ADVENTURE_DILEMMA.getLabel());
    }
}
