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

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.preferences.PreferenceFlags;

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
    static final String GUIDE_SEEN = "guideSeen";
    static final String GUIDE_ALWAYS_OPEN = "guideAlwaysOpen";

    private static AdvancedSurgeriesGuidePreferences instance;

    private final PreferenceFlags flags;

    /**
     * @param node the preferences node to store the settings under
     */
    AdvancedSurgeriesGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(GUIDE_SEEN, false);
        flags.register(GUIDE_ALWAYS_OPEN, false);
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

    /**
     * @return {@code true} once the player has seen the guide
     */
    public boolean isGuideSeen() {
        return flags.get(GUIDE_SEEN);
    }

    public void setGuideSeen(final boolean seen) {
        flags.set(GUIDE_SEEN, seen);
    }

    /**
     * @return {@code true} if the guide opens every time the dialog does, rather than only the first time
     */
    public boolean isGuideAlwaysOpen() {
        return flags.get(GUIDE_ALWAYS_OPEN);
    }

    public void setGuideAlwaysOpen(final boolean alwaysOpen) {
        flags.set(GUIDE_ALWAYS_OPEN, alwaysOpen);
    }
}
