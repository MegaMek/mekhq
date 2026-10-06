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
package mekhq.gui.roleplay;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import megamek.client.ui.preferences.PreferenceElement;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.MekHQ;

/**
 * The Oracle console's tutorial settings: whether the welcome banner has been dismissed, whether each page's guide has
 * been seen, and whether guides open by themselves on a page's first visit.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while MekHQ is
 * closed) resets every tutorial.</p>
 */
public final class OracleGuidePreferences {
    private static final MMLogger LOGGER = MMLogger.create(OracleGuidePreferences.class);

    static final String WELCOME_DISMISSED = "welcomeDismissed";
    static final String GUIDE_AUTO_OPEN = "guideAutoOpen";
    static final String GUIDE_SEEN_PREFIX = "guideSeen.";

    private static OracleGuidePreferences instance;

    private final Map<String, Flag> flags = new LinkedHashMap<>();

    /**
     * @param node the preferences node to store the settings under
     */
    OracleGuidePreferences(final PreferencesNode node) {
        register(node, WELCOME_DISMISSED, false);
        register(node, GUIDE_AUTO_OPEN, true);
        for (ConsolePage page : ConsolePage.values()) {
            register(node, seenKey(page), false);
        }
    }

    /**
     * @return the settings, stored in MekHQ's preferences file
     */
    public static synchronized OracleGuidePreferences getInstance() {
        if (instance == null) {
            instance = new OracleGuidePreferences(MekHQ.getMHQPreferences().forClass(OracleGuidePreferences.class));
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
            LOGGER.error(exception, "Failed to register Oracle tutorial preference {}", name);
        }
    }

    private static String seenKey(final ConsolePage page) {
        return GUIDE_SEEN_PREFIX + page.name().toLowerCase(Locale.ROOT);
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
     * @return {@code true} once the player has dismissed the welcome banner
     */
    public boolean isWelcomeDismissed() {
        return get(WELCOME_DISMISSED);
    }

    public void setWelcomeDismissed(final boolean dismissed) {
        set(WELCOME_DISMISSED, dismissed);
    }

    /**
     * @return {@code true} if a page's guide opens by itself the first time the page is visited
     */
    public boolean isGuideAutoOpen() {
        return get(GUIDE_AUTO_OPEN);
    }

    public void setGuideAutoOpen(final boolean autoOpen) {
        set(GUIDE_AUTO_OPEN, autoOpen);
    }

    /**
     * @param page a console page
     *
     * @return {@code true} if the player has already seen that page's guide
     */
    public boolean isGuideSeen(final ConsolePage page) {
        return get(seenKey(page));
    }

    public void setGuideSeen(final ConsolePage page, final boolean seen) {
        set(seenKey(page), seen);
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
