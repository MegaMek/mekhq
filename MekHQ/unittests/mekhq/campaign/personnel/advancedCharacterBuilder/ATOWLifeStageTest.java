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

import static mekhq.campaign.personnel.advancedCharacterBuilder.ATOWLifeStage.AFFILIATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ATOWLifeStageTest {
    @ParameterizedTest
    @EnumSource(ATOWLifeStage.class)
    void testForUniqueLookupNames(ATOWLifeStage lifeStage) {
        for (ATOWLifeStage lookup : ATOWLifeStage.values()) {
            if (lookup == lifeStage) {
                continue;
            }

            assertNotEquals(lifeStage.getLookupName(), lookup.getLookupName(), "Lookup names should be unique");
        }
    }

    @ParameterizedTest
    @EnumSource(ATOWLifeStage.class)
    void testForUniqueOrders(ATOWLifeStage lifeStage) {
        for (ATOWLifeStage lookup : ATOWLifeStage.values()) {
            if (lookup == lifeStage) {
                continue;
            }

            assertNotEquals(lifeStage.getOrder(), lookup.getOrder(), "Orders should be unique");
        }
    }

    @ParameterizedTest
    @EnumSource(ATOWLifeStage.class)
    void testLookupName_ValidOrder(ATOWLifeStage lifeStage) {
        assertNotNull(lifeStage, "Life stage was null.");
        assertEquals(lifeStage, ATOWLifeStage.fromLookupName(lifeStage.getLookupName()),
              "Failed to retrieve " + lifeStage.getLookupName() + " from lookup name.");
    }

    @ParameterizedTest
    @EnumSource(ATOWLifeStage.class)
    void testLookupName_ValidOrder_caseInsensitive(ATOWLifeStage lifeStage) {
        assertNotNull(lifeStage, "Life stage was null.");
        assertEquals(lifeStage, ATOWLifeStage.fromLookupName(lifeStage.getLookupName().toLowerCase()),
              "Failed to retrieve " + lifeStage.getLookupName() + " from lookup name.");
    }

    @Test
    void testFromLookupName_InvalidLookupName() {
        ATOWLifeStage result = ATOWLifeStage.fromLookupName("INVALID_NAME");
        assertNull(result);
    }

    @Test
    void testFromLookupName_CaseInsensitiveLookupName() {
        ATOWLifeStage result = ATOWLifeStage.fromLookupName(AFFILIATION.getLookupName().toUpperCase());
        assertNotNull(result);
        assertEquals(AFFILIATION, result);
    }

    @Test
    void testFromLookupName_NullInput() {
        ATOWLifeStage result = ATOWLifeStage.fromLookupName(null);
        assertNull(result);
    }

    @Test
    void testFromLookupName_EmptyString() {
        ATOWLifeStage result = ATOWLifeStage.fromLookupName("");
        assertNull(result);
    }

    @Test
    void testFromLookupName_WhitespaceString() {
        ATOWLifeStage result = ATOWLifeStage.fromLookupName("   ");
        assertNull(result);
    }

    @ParameterizedTest
    @EnumSource(ATOWLifeStage.class)
    void testFromOrder_ValidOrder(ATOWLifeStage lifeStage) {
        assertNotNull(lifeStage, "Life stage was null.");
        assertEquals(lifeStage, ATOWLifeStage.fromOrder(lifeStage.getOrder()),
              "Failed to retrieve " + lifeStage.getLookupName() + " from order.");
    }

    @Test
    void testFromOrder_InvalidOrder() {
        assertNull(ATOWLifeStage.fromOrder(-1));
        assertNull(ATOWLifeStage.fromOrder(5));
        assertNull(ATOWLifeStage.fromOrder(100));
    }

    @Test
    void testFromOrder_BoundaryValues() {
        assertNull(ATOWLifeStage.fromOrder(Integer.MIN_VALUE));
        assertNull(ATOWLifeStage.fromOrder(Integer.MAX_VALUE));
    }

    @ParameterizedTest
    @EnumSource(ATOWLifeStage.class)
    void testFromString_ValidLookupName(ATOWLifeStage lifeStage) {
        assertNotNull(lifeStage, "Life stage was null.");
        assertEquals(lifeStage, ATOWLifeStage.fromString(lifeStage.getLookupName()),
              "Failed to retrieve " + lifeStage.getLookupName() + " from string.");
    }

    @ParameterizedTest
    @EnumSource(ATOWLifeStage.class)
    void testFromString_ValidOrder(ATOWLifeStage lifeStage) {
        assertNotNull(lifeStage, "Life stage was null.");
        assertEquals(lifeStage, ATOWLifeStage.fromString(String.valueOf(lifeStage.getOrder())),
              "Failed to retrieve " + lifeStage.getOrder() + " from string.");
    }

    @Test
    void testFromString_InvalidInputs() {
        assertNull(ATOWLifeStage.fromString("INVALID_STRING"), "Expected null for invalid string.");
        assertNull(ATOWLifeStage.fromString("999"), "Expected null for out-of-range number.");
        assertNull(ATOWLifeStage.fromString("-1"), "Expected null for negative number.");
        assertNull(ATOWLifeStage.fromString(null), "Expected null for null input.");
        assertNull(ATOWLifeStage.fromString("   "), "Expected null for whitespace string.");
    }
}
