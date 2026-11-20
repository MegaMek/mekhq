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

import java.util.Locale;

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.preferences.PreferenceFlags;

/**
 * Remembers, for each preset screen, whether its guide has been shown and whether the player wants it to open every
 * time. Kept in the MekHQ preferences file, like the Oracle Console's guide settings.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class PresetGuidePreferences {
    /** The screens with a preset guide. */
    public enum GuideScreen {
        /** The campaign upgrade console, shown when an older save is loaded. */
        UPGRADE,
        /** The preset picker, shown when a new campaign is started. */
        NEW_CAMPAIGN
    }

    static final String GUIDE_SEEN_PREFIX = "guideSeen.";
    static final String GUIDE_ALWAYS_OPEN_PREFIX = "guideAlwaysOpen.";

    private static PresetGuidePreferences instance;

    private final PreferenceFlags flags;

    PresetGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        for (GuideScreen screen : GuideScreen.values()) {
            flags.register(seenKey(screen), false);
            flags.register(alwaysOpenKey(screen), false);
        }
    }

    /**
     * @return the shared preferences, backed by the MekHQ preferences file
     */
    public static synchronized PresetGuidePreferences getInstance() {
        if (instance == null) {
            instance = new PresetGuidePreferences(MekHQ.getMHQPreferences().forClass(PresetGuidePreferences.class));
        }
        return instance;
    }

    private static String seenKey(final GuideScreen screen) {
        return GUIDE_SEEN_PREFIX + screen.name().toLowerCase(Locale.ROOT);
    }

    private static String alwaysOpenKey(final GuideScreen screen) {
        return GUIDE_ALWAYS_OPEN_PREFIX + screen.name().toLowerCase(Locale.ROOT);
    }

    /**
     * @param screen the screen
     *
     * @return {@code true} if the screen's guide should open by itself: the first time ever, or every time if the
     *       player asked for that
     */
    public boolean shouldOpenGuide(final GuideScreen screen) {
        return !isGuideSeen(screen) || isGuideAlwaysOpen(screen);
    }

    public boolean isGuideSeen(final GuideScreen screen) {
        return flags.get(seenKey(screen));
    }

    public void setGuideSeen(final GuideScreen screen, final boolean seen) {
        flags.set(seenKey(screen), seen);
    }

    public boolean isGuideAlwaysOpen(final GuideScreen screen) {
        return flags.get(alwaysOpenKey(screen));
    }

    public void setGuideAlwaysOpen(final GuideScreen screen, final boolean alwaysOpen) {
        flags.set(alwaysOpenKey(screen), alwaysOpen);
    }
}
