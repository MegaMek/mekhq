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
package mekhq.gui.dialog.markets.personnelMarket;

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.preferences.PreferenceFlags;

/**
 * The personnel market's tutorial settings: whether its guide has been seen, and whether it opens by itself the first
 * time the market is opened.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class PersonnelMarketGuidePreferences {
    static final String GUIDE_AUTO_OPEN = "guideAutoOpen";
    static final String GUIDE_SEEN = "guideSeen";

    private static PersonnelMarketGuidePreferences instance;

    private final PreferenceFlags flags;

    /**
     * @param node the preferences node to store the settings under
     */
    PersonnelMarketGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(GUIDE_AUTO_OPEN, true);
        flags.register(GUIDE_SEEN, false);
    }

    /**
     * @return the settings, stored in MekHQ's preferences file
     */
    static synchronized PersonnelMarketGuidePreferences getInstance() {
        if (instance == null) {
            instance = new PersonnelMarketGuidePreferences(MekHQ.getMHQPreferences()
                                                                 .forClass(PersonnelMarketGuidePreferences.class));
        }
        return instance;
    }

    /** @return {@code true} if the guide opens by itself the first time the market is opened */
    boolean isGuideAutoOpen() {
        return flags.get(GUIDE_AUTO_OPEN);
    }

    void setGuideAutoOpen(final boolean autoOpen) {
        flags.set(GUIDE_AUTO_OPEN, autoOpen);
    }

    /** @return {@code true} if the player has already seen the guide */
    boolean isGuideSeen() {
        return flags.get(GUIDE_SEEN);
    }

    void setGuideSeen(final boolean seen) {
        flags.set(GUIDE_SEEN, seen);
    }

    /** @return {@code true} if the guide should open by itself: it is unseen and opening by itself is turned on */
    boolean shouldAutoOpen() {
        return isGuideAutoOpen() && !isGuideSeen();
    }
}
