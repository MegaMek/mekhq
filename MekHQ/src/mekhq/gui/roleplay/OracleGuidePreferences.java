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

import java.util.Locale;

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.preferences.PreferenceFlags;

/**
 * The Oracle console's tutorial settings: whether the welcome banner has been dismissed, whether each page's guide has
 * been seen, and whether guides open by themselves on a page's first visit.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while MekHQ is
 * closed) resets every tutorial.</p>
 */
public final class OracleGuidePreferences {
    static final String WELCOME_DISMISSED = "welcomeDismissed";
    static final String GUIDE_AUTO_OPEN = "guideAutoOpen";
    static final String GUIDE_SEEN_PREFIX = "guideSeen.";

    private static OracleGuidePreferences instance;

    private final PreferenceFlags flags;

    /**
     * @param node the preferences node to store the settings under
     */
    OracleGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(WELCOME_DISMISSED, false);
        flags.register(GUIDE_AUTO_OPEN, true);
        for (ConsolePage page : ConsolePage.values()) {
            flags.register(seenKey(page), false);
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

    private static String seenKey(final ConsolePage page) {
        return GUIDE_SEEN_PREFIX + page.name().toLowerCase(Locale.ROOT);
    }

    /**
     * @return {@code true} once the player has dismissed the welcome banner
     */
    public boolean isWelcomeDismissed() {
        return flags.get(WELCOME_DISMISSED);
    }

    public void setWelcomeDismissed(final boolean dismissed) {
        flags.set(WELCOME_DISMISSED, dismissed);
    }

    /**
     * @return {@code true} if a page's guide opens by itself the first time the page is visited
     */
    public boolean isGuideAutoOpen() {
        return flags.get(GUIDE_AUTO_OPEN);
    }

    public void setGuideAutoOpen(final boolean autoOpen) {
        flags.set(GUIDE_AUTO_OPEN, autoOpen);
    }

    /**
     * @param page a console page
     *
     * @return {@code true} if the player has already seen that page's guide
     */
    public boolean isGuideSeen(final ConsolePage page) {
        return flags.get(seenKey(page));
    }

    public void setGuideSeen(final ConsolePage page, final boolean seen) {
        flags.set(seenKey(page), seen);
    }
}
