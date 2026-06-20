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
package mekhq.gui.campaignOptions.contents;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Reflection helpers that let the campaign-option model tests assert an <em>exhaustive</em> load/save round-trip
 * without hand-writing one assertion per field.
 *
 * <p>
 * The campaign-option models are plain data holders: their constructor copies every value out of {@code CampaignOptions}
 * (and friends) and {@code applyTo} writes every value back. The danger is a field that loads from one option but saves
 * to another (a real example was found where the Markets tab loaded {@code regionalMekVariations} but saved
 * {@code unitMarketRegionalMekVariations}). To catch that for <em>every</em> field, a test mutates each field to a value
 * that differs from its default, runs it through {@code applyTo} and a fresh re-read, and asserts the re-read model
 * equals the mutated one.
 * </p>
 *
 * <p>
 * {@link #mutateScalarFields} handles the bulk of that mutation automatically: it flips every {@code boolean} field,
 * advances every {@code enum} field to a different constant, and bumps every numeric field by one. Only the four
 * contract-percentage setters in {@code CampaignOptions} clamp their input, so those (and any field with by-name or
 * collaborator-triggering semantics) are passed as exclusions and mutated by the caller. {@link String}, array, and
 * collection fields are also left to the caller. {@link #assertAllFieldsMatch} then compares every field by reflection,
 * so a newly added field is covered the moment it exists.
 * </p>
 */
final class OptionsModelTestSupport {
    private OptionsModelTestSupport() {}

    /**
     * Mutates every scalar field of {@code target} to a value that differs from its current one: booleans are flipped,
     * enums advance to a different constant, and numeric fields are bumped by one. {@link String}, array, object, and
     * collection fields are left untouched (the caller mutates those), as is any field named in {@code excludedFields}.
     *
     * @param target         the model whose scalar fields should be mutated in place
     * @param excludedFields field names to leave alone (clamped numerics, by-name fields, collaborator-triggering enums)
     */
    static void mutateScalarFields(Object target, String... excludedFields) {
        Set<String> excluded = new HashSet<>(Arrays.asList(excludedFields));
        for (Field field : target.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || excluded.contains(field.getName())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Class<?> type = field.getType();
                if (type == boolean.class) {
                    field.setBoolean(target, !field.getBoolean(target));
                } else if (type.isEnum()) {
                    Object[] constants = type.getEnumConstants();
                    if (constants.length > 1) {
                        Object current = field.get(target);
                        int ordinal = (current == null) ? -1 : ((Enum<?>) current).ordinal();
                        field.set(target, constants[(ordinal + 1) % constants.length]);
                    }
                } else if (type == int.class) {
                    field.setInt(target, field.getInt(target) + 1);
                } else if (type == long.class) {
                    field.setLong(target, field.getLong(target) + 1);
                } else if (type == short.class) {
                    field.setShort(target, (short) (field.getShort(target) + 1));
                } else if (type == byte.class) {
                    field.setByte(target, (byte) (field.getByte(target) + 1));
                } else if (type == double.class) {
                    field.setDouble(target, field.getDouble(target) + 1.0);
                } else if (type == float.class) {
                    field.setFloat(target, field.getFloat(target) + 1.0f);
                }
            } catch (IllegalAccessException exception) {
                throw new AssertionError("Could not mutate field " + field.getName(), exception);
            }
        }
    }

    /**
     * Asserts that every field of {@code expected} equals the matching field of {@code actual}, skipping static fields
     * and any field named in {@code excludedFields}. Array fields are compared by contents; everything else by
     * {@link Object#equals}. A mismatch reports the field name so a cross-wired or dropped option is easy to find.
     *
     * @param expected       the mutated model that was written out
     * @param actual         the model re-read after {@code applyTo}
     * @param excludedFields field names to skip (transient flags, collections asserted separately, by-name fields)
     */
    static void assertAllFieldsMatch(Object expected, Object actual, String... excludedFields) {
        Set<String> excluded = new HashSet<>(Arrays.asList(excludedFields));
        for (Field field : expected.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || excluded.contains(field.getName())) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object expectedValue = field.get(expected);
                Object actualValue = field.get(actual);
                if (field.getType().isArray()) {
                    assertArrayContentsEqual(expectedValue, actualValue, field.getName());
                } else {
                    assertEquals(expectedValue, actualValue, field.getName());
                }
            } catch (IllegalAccessException exception) {
                throw new AssertionError("Could not read field " + field.getName(), exception);
            }
        }
    }

    private static void assertArrayContentsEqual(Object expected, Object actual, String fieldName) {
        if (expected instanceof int[] expectedArray) {
            assertArrayEquals(expectedArray, (int[]) actual, fieldName);
        } else if (expected instanceof double[] expectedArray) {
            assertArrayEquals(expectedArray, (double[]) actual, fieldName);
        } else if (expected instanceof boolean[] expectedArray) {
            assertArrayEquals(expectedArray, (boolean[]) actual, fieldName);
        } else if (expected instanceof long[] expectedArray) {
            assertArrayEquals(expectedArray, (long[]) actual, fieldName);
        } else {
            assertArrayEquals((Object[]) expected, (Object[]) actual, fieldName);
        }
    }
}
