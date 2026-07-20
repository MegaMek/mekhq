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
package mekhq.gui.campaignOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import mekhq.MHQOptions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Exhaustive round-trip test for {@link MHQOptionsModel}, in the same spirit as the campaign-option model tests
 * (for example {@code PersonnelOptionsModelTest}): every field is mutated to a value that differs from its default, run
 * through {@link MHQOptionsModel#applyTo(MHQOptions)} and a fresh re-read, and asserted equal. This catches a field
 * that loads from one option but saves to another - the exact bug the campaign-option tests were written for.
 *
 * <p>
 * Unlike {@code CampaignOptions}, {@link MHQOptions} is not an isolated value object: it is a proxy over the JVM-wide
 * {@link java.util.prefs.Preferences#userRoot()} store, and {@code applyTo}'s two non-{@code MHQOptions} escapees (the
 * GUI scale in {@link GUIPreferences} and the user directory in {@link PreferenceManager}) have real side effects
 * (a look-and-feel rescale and a settings file written to disk). To keep this test hermetic, those two singletons are
 * mocked so no rescale or disk write happens, and the two escapee fields ({@code guiScaleValue} and {@code userDir})
 * are excluded from the mutate/verify pass because they cannot round-trip through the mocked singletons. The shared
 * preferences store is snapshotted through the model itself before the test mutates it and restored afterwards.
 * </p>
 */
class MHQOptionsModelTest {
    /** Escapee fields backed by mocked global singletons rather than the options store; not part of the round-trip. */
    private static final String[] EXCLUDED_FIELDS = { "guiScaleValue", "userDir" };

    @Test
    void applyToRoundTripsEveryField() throws ReflectiveOperationException {
        GUIPreferences guiPreferences = mock(GUIPreferences.class);
        when(guiPreferences.getGUIScale()).thenReturn(1.0f);
        ClientPreferences clientPreferences = mock(ClientPreferences.class);
        when(clientPreferences.getUserDir()).thenReturn("test-user-dir");
        PreferenceManager preferenceManager = mock(PreferenceManager.class);

        try (MockedStatic<GUIPreferences> guiStatic = mockStatic(GUIPreferences.class);
              MockedStatic<PreferenceManager> preferenceStatic = mockStatic(PreferenceManager.class)) {
            guiStatic.when(GUIPreferences::getInstance).thenReturn(guiPreferences);
            preferenceStatic.when(PreferenceManager::getInstance).thenReturn(preferenceManager);
            preferenceStatic.when(PreferenceManager::getClientPreferences).thenReturn(clientPreferences);

            MHQOptions options = new MHQOptions();
            // MHQOptions proxies the shared Preferences.userRoot() store; capture the current real state so it can be
            // restored after the test mutates it.
            MHQOptionsModel original = new MHQOptionsModel(options);
            try {
                MHQOptionsModel model = new MHQOptionsModel(options);
                mutateEveryField(model);

                model.applyTo(options);
                MHQOptionsModel roundTripped = new MHQOptionsModel(options);

                assertEveryFieldMatches(model, roundTripped);
            } finally {
                original.applyTo(options);
            }
        }
    }

    /**
     * Mutates every non-excluded, non-static field of {@code model} to a value that differs from its current one:
     * booleans are flipped, enums advance to a different constant, numbers are bumped, strings get a suffix, and the
     * homogeneous map groups have each value flipped (booleans) or shifted (colours).
     */
    private static void mutateEveryField(MHQOptionsModel model) throws ReflectiveOperationException {
        for (Field field : MHQOptionsModel.class.getDeclaredFields()) {
            if (isExcluded(field)) {
                continue;
            }
            field.setAccessible(true);
            Class<?> type = field.getType();
            Object value = field.get(model);
            if (type == boolean.class) {
                field.setBoolean(model, !field.getBoolean(model));
            } else if (type == int.class) {
                field.setInt(model, field.getInt(model) + 1);
            } else if (type == double.class) {
                field.setDouble(model, field.getDouble(model) + 1.0);
            } else if (type.isEnum()) {
                Object[] constants = type.getEnumConstants();
                int ordinal = (value == null) ? -1 : ((Enum<?>) value).ordinal();
                field.set(model, constants[(ordinal + 1) % constants.length]);
            } else if (type == String.class) {
                field.set(model, value + "-changed");
            } else if (value instanceof Map<?, ?> map) {
                mutateMap(map);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void mutateMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return;
        }
        Object sample = map.values().iterator().next();
        if (sample instanceof Boolean) {
            ((Map<String, Boolean>) map).replaceAll((key, current) -> !current);
        } else if (sample instanceof Color) {
            ((Map<String, Color>) map).replaceAll((key, current) ->
                  new Color((current.getRed() + 1) % 256, current.getGreen(), current.getBlue()));
        }
    }

    /**
     * Asserts that every non-excluded, non-static field of {@code expected} equals the matching field of
     * {@code actual}. Maps compare by contents through {@link Map#equals}, so a cross-wired option surfaces as a
     * mismatch named after the field.
     */
    private static void assertEveryFieldMatches(MHQOptionsModel expected, MHQOptionsModel actual)
          throws ReflectiveOperationException {
        for (Field field : MHQOptionsModel.class.getDeclaredFields()) {
            if (isExcluded(field)) {
                continue;
            }
            field.setAccessible(true);
            assertEquals(field.get(expected), field.get(actual), field.getName());
        }
    }

    private static boolean isExcluded(Field field) {
        if (Modifier.isStatic(field.getModifiers())) {
            return true;
        }
        for (String excluded : EXCLUDED_FIELDS) {
            if (excluded.equals(field.getName())) {
                return true;
            }
        }
        return false;
    }
}
