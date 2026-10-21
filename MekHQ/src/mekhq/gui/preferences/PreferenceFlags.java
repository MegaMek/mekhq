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
package mekhq.gui.preferences;

import java.util.LinkedHashMap;
import java.util.Map;

import megamek.client.ui.preferences.PreferenceElement;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;

/**
 * A set of named on/off settings stored under one preferences node, such as a window's tutorial flags.
 *
 * <p>Each flag is saved in the preferences file as {@code true} or {@code false}. A flag that was never registered,
 * or failed to register, reads as {@code false} and ignores writes.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class PreferenceFlags {
    private static final MMLogger LOGGER = MMLogger.create(PreferenceFlags.class);

    private final PreferencesNode node;
    private final Map<String, Flag> flags = new LinkedHashMap<>();

    /**
     * @param node the preferences node to store the flags under
     */
    public PreferenceFlags(final PreferencesNode node) {
        this.node = node;
    }

    /**
     * Adds a flag, loading any value saved in an earlier session; it is saved again on exit.
     *
     * @param name         the flag's name in the preferences file
     * @param defaultValue the value before anything has been saved
     */
    public void register(final String name, final boolean defaultValue) {
        try {
            Flag flag = new Flag(name, defaultValue);
            flags.put(name, flag);
            node.manage(flag);
        } catch (Exception exception) {
            LOGGER.error(exception, "Failed to register preference {}", name);
        }
    }

    /**
     * @param name the flag's name
     *
     * @return the flag's value, or {@code false} if it isn't registered
     */
    public boolean get(final String name) {
        Flag flag = flags.get(name);
        return flag != null && flag.value;
    }

    /**
     * @param name  the flag's name
     * @param value the new value, ignored if the flag isn't registered
     */
    public void set(final String name, final boolean value) {
        Flag flag = flags.get(name);
        if (flag != null) {
            flag.value = value;
        }
    }

    /** One on/off setting, saved in the preferences file as {@code true} or {@code false}. */
    private static final class Flag extends PreferenceElement {
        private boolean value;

        private Flag(final String name, final boolean defaultValue) throws Exception {
            super(name);
            this.value = defaultValue;
        }

        @Override
        protected String getValue() {
            return Boolean.toString(value);
        }

        @Override
        protected void initialize(final String saved) {
            value = MathUtility.parseBoolean(saved.strip());
        }

        @Override
        protected void dispose() {
            // Nothing to release: the flag is not tied to a component.
        }
    }
}
