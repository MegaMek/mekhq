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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

import mekhq.campaign.personnel.enums.InjuryLevel;
import mekhq.campaign.personnel.medical.BodyLocation;

/**
 * Test class for {@link InjuryType} register method
 */
class InjuryTypeTest {

    /**
     * Helper class to create test injury types
     */
    private static class TestInjuryType extends InjuryType {
        public TestInjuryType(String name) {
            this.simpleName = name;
            this.recoveryTime = 10;
            this.fluffText = "Test injury: " + name;
            this.level = InjuryLevel.MINOR;
            this.allowedLocations = EnumSet.of(BodyLocation.GENERIC);
        }
    }

    @Test
    void testRegisterSuccessWithValidId() {
        // Create a unique injury type
        InjuryType testInjury = new TestInjuryType("testSuccessWithId");
        int uniqueId = 999999;
        String uniqueKey = "test:success_with_id_" + System.nanoTime();

        // Register should complete without throwing an exception
        InjuryType.register(uniqueId, uniqueKey, testInjury);

        // Verify it can be retrieved by ID
        InjuryType retrieved = InjuryType.byId(uniqueId);
        assertNotNull(retrieved, "Injury type should be retrievable by ID");
        assertSame(testInjury, retrieved, "Retrieved injury type should be the same instance");

        // Verify it can be retrieved by key
        InjuryType retrievedByKey = InjuryType.byKey(uniqueKey);
        assertNotNull(retrievedByKey, "Injury type should be retrievable by key");
        assertSame(testInjury, retrievedByKey, "Retrieved injury type should be the same instance");
    }

    @Test
    void testRegisterSuccessWithNegativeId() {
        // Create a unique injury type
        InjuryType testInjury = new TestInjuryType("testSuccessWithNegativeId");
        String uniqueKey = "test:success_negative_id_" + System.nanoTime();

        // Register with negative ID (should skip ID registration)
        InjuryType.register(-1, uniqueKey, testInjury);

        // Verify it can be retrieved by key
        InjuryType retrievedByKey = InjuryType.byKey(uniqueKey);
        assertNotNull(retrievedByKey, "Injury type should be retrievable by key");
        assertSame(testInjury, retrievedByKey, "Retrieved injury type should be the same instance");
    }

    @Test
    void testRegisterThrowsOnNullInjuryType() {
        // Attempting to register null injury type should throw NullPointerException
        assertThrows(NullPointerException.class, () -> {
            InjuryType.register(888888, "test:null_injury", null);
        }, "Register should throw NullPointerException when injury type is null");
    }

    @Test
    void testRegisterThrowsOnNullKey() {
        // Create a unique injury type
        InjuryType testInjury = new TestInjuryType("testNullKey");

        // Attempting to register with null key should throw NullPointerException
        assertThrows(NullPointerException.class, () -> {
            InjuryType.register(777777, null, testInjury);
        }, "Register should throw NullPointerException when key is null");
    }

    @Test
    void testRegisterThrowsOnEmptyKey() {
        // Create a unique injury type
        InjuryType testInjury = new TestInjuryType("testEmptyKey");

        // Attempting to register with empty key should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            InjuryType.register(666666, "", testInjury);
        }, "Register should throw IllegalArgumentException when key is empty");

        assertEquals("Injury type key can't be an empty string.", exception.getMessage());
    }

    @Test
    void testRegisterThrowsOnDuplicateId() {
        // Create two unique injury types
        InjuryType firstInjury = new TestInjuryType("testDuplicateId1");
        InjuryType secondInjury = new TestInjuryType("testDuplicateId2");
        int duplicateId = 555555;
        String firstKey = "test:duplicate_id_first_" + System.nanoTime();
        String secondKey = "test:duplicate_id_second_" + System.nanoTime();

        // Register the first injury type
        InjuryType.register(duplicateId, firstKey, firstInjury);

        // Attempting to register second injury type with same ID should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            InjuryType.register(duplicateId, secondKey, secondInjury);
        }, "Register should throw IllegalArgumentException when ID is already registered");

        assertEquals("Injury type ID " + duplicateId + " is already registered.", exception.getMessage());
    }

    @Test
    void testRegisterThrowsOnDuplicateKey() {
        // Create two unique injury types
        InjuryType firstInjury = new TestInjuryType("testDuplicateKey1");
        InjuryType secondInjury = new TestInjuryType("testDuplicateKey2");
        String duplicateKey = "test:duplicate_key_" + System.nanoTime();

        // Register the first injury type
        InjuryType.register(444444, duplicateKey, firstInjury);

        // Attempting to register second injury type with same key should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            InjuryType.register(444443, duplicateKey, secondInjury);
        }, "Register should throw IllegalArgumentException when key is already registered");

        assertEquals("Injury type key \"" + duplicateKey + "\" is already registered.", exception.getMessage());
    }

    @Test
    void testRegisterThrowsOnDuplicateInjuryType() {
        // Create a unique injury type
        InjuryType testInjury = new TestInjuryType("testDuplicateInjuryType");
        String firstKey = "test:duplicate_injtype_first_" + System.nanoTime();
        String secondKey = "test:duplicate_injtype_second_" + System.nanoTime();

        // Register the injury type with the first key
        InjuryType.register(333333, firstKey, testInjury);

        // Attempting to register the same injury type instance again should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            InjuryType.register(333332, secondKey, testInjury);
        }, "Register should throw IllegalArgumentException when injury type instance is already registered");

        assertEquals("Injury type " + testInjury.getSimpleName() + " is already registered", exception.getMessage());
    }

    @Test
    void testRegisterWithoutIdParameter() {
        // Create a unique injury type
        InjuryType testInjury = new TestInjuryType("testRegisterWithoutId");
        String uniqueKey = "test:without_id_" + System.nanoTime();

        // Register using the overloaded method that doesn't take an ID
        InjuryType.register(uniqueKey, testInjury);

        // Verify it can be retrieved by key
        InjuryType retrievedByKey = InjuryType.byKey(uniqueKey);
        assertNotNull(retrievedByKey, "Injury type should be retrievable by key");
        assertSame(testInjury, retrievedByKey, "Retrieved injury type should be the same instance");
    }

    @Test
    void testByKeyReturnsNullForUnregisteredKey() {
        // Attempting to retrieve an injury type with a non-existent key should return null
        InjuryType result = InjuryType.byKey("test:nonexistent_key_" + System.nanoTime());
        assertEquals(null, result, "byKey should return null for unregistered keys");
    }

    @Test
    void testByIdReturnsNullForUnregisteredId() {
        // Attempting to retrieve an injury type with a non-existent ID should return null
        InjuryType result = InjuryType.byId(999998);
        assertEquals(null, result, "byId should return null for unregistered IDs");
    }

    @Test
    void testByKeyWithNumericStringFallsBackToId() {
        // Create a unique injury type and register it with a numeric ID
        InjuryType testInjury = new TestInjuryType("testNumericKeyFallback");
        int testId = 111111;
        String uniqueKey = "test:numeric_fallback_" + System.nanoTime();

        InjuryType.register(testId, uniqueKey, testInjury);

        // Verify it can be retrieved by passing the ID as a string to byKey
        InjuryType retrieved = InjuryType.byKey(String.valueOf(testId));
        assertNotNull(retrieved, "Injury type should be retrievable by numeric string key");
        assertSame(testInjury, retrieved, "Retrieved injury type should be the same instance");
    }
}