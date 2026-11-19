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
 * Remembers whether the campaign upgrade console's guide has been shown, and whether the player wants it to open every
 * time they upgrade. Kept in the MekHQ preferences file, like the Oracle Console's guide settings.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class CampaignUpgradeGuidePreferences {
    static final String GUIDE_SEEN = "guideSeen";
    static final String GUIDE_ALWAYS_OPEN = "guideAlwaysOpen";

    private static CampaignUpgradeGuidePreferences instance;

    private final PreferenceFlags flags;

    CampaignUpgradeGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(GUIDE_SEEN, false);
        flags.register(GUIDE_ALWAYS_OPEN, false);
    }

    /**
     * @return the shared preferences, backed by the MekHQ preferences file
     */
    public static synchronized CampaignUpgradeGuidePreferences getInstance() {
        if (instance == null) {
            instance = new CampaignUpgradeGuidePreferences(
                  MekHQ.getMHQPreferences().forClass(CampaignUpgradeGuidePreferences.class));
        }
        return instance;
    }

    /**
     * @return {@code true} if the guide should open by itself when the console opens: the first time ever, or every
     *       time if the player asked for that
     */
    public boolean shouldOpenGuide() {
        return !isGuideSeen() || isGuideAlwaysOpen();
    }

    public boolean isGuideSeen() {
        return flags.get(GUIDE_SEEN);
    }

    public void setGuideSeen(final boolean seen) {
        flags.set(GUIDE_SEEN, seen);
    }

    public boolean isGuideAlwaysOpen() {
        return flags.get(GUIDE_ALWAYS_OPEN);
    }

    public void setGuideAlwaysOpen(final boolean alwaysOpen) {
        flags.set(GUIDE_ALWAYS_OPEN, alwaysOpen);
    }
}
