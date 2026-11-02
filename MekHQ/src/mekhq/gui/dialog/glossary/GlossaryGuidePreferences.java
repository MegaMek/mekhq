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
package mekhq.gui.dialog.glossary;

import java.util.Locale;

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.dialog.glossary.GlossaryConsole.Mode;
import mekhq.gui.preferences.PreferenceFlags;

/**
 * The tutorial settings of the glossary console and the guide viewer: whether the welcome banner has been dismissed,
 * whether each page's guide has been seen, and whether guides open by themselves on a page's first visit.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while MekHQ is
 * closed) resets every tutorial.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class GlossaryGuidePreferences {
    static final String WELCOME_DISMISSED = "welcomeDismissed";
    static final String GUIDE_AUTO_OPEN = "guideAutoOpen";
    static final String GUIDE_SEEN_PREFIX = "guideSeen.";
    static final String VIEWER_GUIDE_SEEN = "guideSeen.viewer";

    private static GlossaryGuidePreferences instance;

    private final PreferenceFlags flags;

    /**
     * @param node the preferences node to store the settings under
     */
    GlossaryGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(WELCOME_DISMISSED, false);
        flags.register(GUIDE_AUTO_OPEN, true);
        for (Mode mode : Mode.values()) {
            flags.register(seenKey(mode), false);
        }
        flags.register(VIEWER_GUIDE_SEEN, false);
    }

    /**
     * @return the settings, stored in MekHQ's preferences file
     */
    public static synchronized GlossaryGuidePreferences getInstance() {
        if (instance == null) {
            instance = new GlossaryGuidePreferences(MekHQ.getMHQPreferences()
                                                          .forClass(GlossaryGuidePreferences.class));
        }
        return instance;
    }

    private static String seenKey(final Mode mode) {
        return GUIDE_SEEN_PREFIX + mode.name().toLowerCase(Locale.ROOT);
    }

    /** @return {@code true} if the player has dismissed the overview's welcome banner */
    public boolean isWelcomeDismissed() {
        return flags.get(WELCOME_DISMISSED);
    }

    public void setWelcomeDismissed(final boolean dismissed) {
        flags.set(WELCOME_DISMISSED, dismissed);
    }

    /** @return {@code true} if a page's guide opens by itself the first time the page is shown */
    public boolean isGuideAutoOpen() {
        return flags.get(GUIDE_AUTO_OPEN);
    }

    public void setGuideAutoOpen(final boolean autoOpen) {
        flags.set(GUIDE_AUTO_OPEN, autoOpen);
    }

    /**
     * @param mode a page of the console
     *
     * @return {@code true} if the player has opened that page's guide before
     */
    boolean isGuideSeen(final Mode mode) {
        return flags.get(seenKey(mode));
    }

    void setGuideSeen(final Mode mode, final boolean seen) {
        flags.set(seenKey(mode), seen);
    }

    /** @return {@code true} if the player has opened the guide viewer's guide before */
    boolean isViewerGuideSeen() {
        return flags.get(VIEWER_GUIDE_SEEN);
    }

    void setViewerGuideSeen(final boolean seen) {
        flags.set(VIEWER_GUIDE_SEEN, seen);
    }
}
