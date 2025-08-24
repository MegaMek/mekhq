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
import static mekhq.campaign.personnel.advancedCharacterBuilder.LifePathCategory.GENERAL_DARK_CASTE;
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
