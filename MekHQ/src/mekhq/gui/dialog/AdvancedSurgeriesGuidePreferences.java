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
package mekhq.gui.dialog;

import java.util.LinkedHashMap;
import java.util.Map;

import megamek.client.ui.preferences.PreferenceElement;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

/**
 * The {@link AdvancedSurgeriesDialog}'s tutorial settings: whether the player has seen its guide, and whether the guide
 * opens every time the dialog does.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while MekHQ is
 * closed) shows the guide again.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class AdvancedSurgeriesGuidePreferences {
    private static final MMLogger LOGGER = MMLogger.create(AdvancedSurgeriesGuidePreferences.class);

    static final String GUIDE_SEEN = "guideSeen";
    static final String GUIDE_ALWAYS_OPEN = "guideAlwaysOpen";

    private static AdvancedSurgeriesGuidePreferences instance;

    private final Map<String, Flag> flags = new LinkedHashMap<>();

    /**
     * @param node the preferences node to store the settings under
     */
    AdvancedSurgeriesGuidePreferences(final PreferencesNode node) {
        register(node, GUIDE_SEEN, false);
        register(node, GUIDE_ALWAYS_OPEN, false);
    }

    /**
     * @return the settings, stored in MekHQ's preferences file
     */
    public static synchronized AdvancedSurgeriesGuidePreferences getInstance() {
        if (instance == null) {
            instance = new AdvancedSurgeriesGuidePreferences(MekHQ.getMHQPreferences()
                                                                   .forClass(AdvancedSurgeriesGuidePreferences.class));
        }
        return instance;
    }

    private void register(final PreferencesNode node, final String name, final boolean defaultValue) {
        try {
            Flag flag = new Flag(name, defaultValue);
            flags.put(name, flag);
            // Managing the flag loads any value saved in an earlier session and saves it again on exit.
            node.manage(flag);
        } catch (Exception exception) {
            LOGGER.error(exception, "Failed to register Advanced Surgery tutorial preference {}", name);
        }
    }

    private boolean get(final String name) {
        Flag flag = flags.get(name);
        return flag != null && flag.value;
    }

    private void set(final String name, final boolean value) {
        Flag flag = flags.get(name);
        if (flag != null) {
            flag.value = value;
        }
    }

    /**
     * @return {@code true} once the player has seen the guide
     */
    public boolean isGuideSeen() {
        return get(GUIDE_SEEN);
    }

    public void setGuideSeen(final boolean seen) {
        set(GUIDE_SEEN, seen);
    }

    /**
     * @return {@code true} if the guide opens every time the dialog does, rather than only the first time
     */
    public boolean isGuideAlwaysOpen() {
        return get(GUIDE_ALWAYS_OPEN);
    }

    public void setGuideAlwaysOpen(final boolean alwaysOpen) {
        set(GUIDE_ALWAYS_OPEN, alwaysOpen);
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
