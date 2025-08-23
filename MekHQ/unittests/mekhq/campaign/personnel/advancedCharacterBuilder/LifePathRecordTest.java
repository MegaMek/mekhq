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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage.REAL_LIFE;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePath.fromRawEntry;
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory.GENERAL_DARK_CASTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LifePathRecordTest {
    @Nested
    class fromRawEntryTests {
        String entry = "FACTION_CODE::SL::0";
        List<String> entryData;
        Map<Integer, List<String>> entryDataMap;

        @BeforeEach
        public void setUpClass() {
            List<String> entryData = List.of(entry);
            entryDataMap = new HashMap<>();
            entryDataMap.put(0, entryData);
        }

        @Test
        void testFromRawEntry_allValid() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingID() {
            fromRawEntry(
                  null,
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingSource() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  null,
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingVersion() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  null,
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingName() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  null,
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingFlavorText() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  null,
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingAge() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  null,
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_negativeAge() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "-6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingXPDiscount() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  null,
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_negativeXPDiscount() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "-3",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingXPCost() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  null,
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_negativeXPCost() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "-2",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingLifeStage() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  null,
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingCategories() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  null,
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingRequirements() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  null,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingExclusions() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  null,
                  List.of(entry),
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingFixedXPAwards() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  null,
                  entryDataMap,
                  "1");
        }

        @Test
        void testFromRawEntry_missingSelectableXPAwards() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  null,
                  "0");
        }

        @Test
        void testFromRawEntry_missingPickCount() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  null);
        }

        @Test
        void testFromRawEntry_negativePickCount() {
            fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  "-1");
        }

        @Test
        void testFromRawEntry_impossiblePickCount() {
            LifePath record = fromRawEntry(
                  "00000000-0000-0000-0000-000000000000",
                  "ATOW",
                  "0.50.07",
                  "Test Life Path",
                  "Once upon a time...",
                  "6",
                  "5",
                  "24",
                  List.of(REAL_LIFE.getLookupName()),
                  List.of(GENERAL_DARK_CASTE.getLookupName()),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  String.valueOf(entryDataMap.size() + 1));
            assertEquals(record.pickCount(), entryDataMap.size());
        }
    }

    @Nested
    class CanonConstructorTests {
        LifePathEntryData entry = new LifePathEntryData("FACTION_CODE", "SL", 0);
        List<LifePathEntryData> entryData;
        Map<Integer, List<LifePathEntryData>> entryDataMap;

        @BeforeEach
        public void setUpClass() {
            List<LifePathEntryData> entryData = List.of(entry);
            entryDataMap = new HashMap<>();
            entryDataMap.put(0, entryData);
        }

        @Test
        void testCanonConstructor_allValid() {
            new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1);
        }

        @Test
        void testCanonConstructor_missingID() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  null,
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingSource() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  null,
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingVersion() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  null,
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingName() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  null,
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingFlavorText() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  null,
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_negativeAge() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  -6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_negativeXPDiscount() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  -5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_negativeXPCost() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  -24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingLifeStage() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  null,
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingCategories() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  null,
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingRequirements() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  null,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingExclusions() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  null,
                  List.of(entry),
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_missingFixedXPAwards() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  null,
                  entryDataMap,
                  1));
        }

        @Test
        void testCanonConstructor_negativePickCount() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  -1));
        }

        @Test
        void testCanonConstructor_impossiblePickCount() {
            assertThrows(IllegalArgumentException.class, () -> new LifePath(
                  UUID.fromString("00000000-0000-0000-0000-000000000000"),
                  "ATOW",
                  new Version("0.50.07"),
                  "Test Life Path",
                  "Once upon a time...",
                  6,
                  5,
                  24,
                  List.of(REAL_LIFE),
                  List.of(GENERAL_DARK_CASTE),
                  entryDataMap,
                  List.of(entry),
                  List.of(entry),
                  entryDataMap,
                  entryDataMap.size() + 1));
        }
    }
}
