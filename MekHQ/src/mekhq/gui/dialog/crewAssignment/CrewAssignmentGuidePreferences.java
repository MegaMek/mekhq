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
package mekhq.gui.dialog.crewAssignment;

import java.util.Locale;

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.preferences.PreferenceFlags;

/**
 * The crew assignment dialog's per-player settings: whether each page's guide has been seen, whether guides open by
 * themselves on a page's first visit, and whether the filter rail is hidden.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while MekHQ is
 * closed) resets every guide and shows the filter rail again.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewAssignmentGuidePreferences {
    static final String GUIDE_AUTO_OPEN = "guideAutoOpen";
    static final String GUIDE_SEEN_PREFIX = "guideSeen.";
    static final String FILTERS_HIDDEN = "filtersHidden";

    private static CrewAssignmentGuidePreferences instance;

    private final PreferenceFlags flags;

    CrewAssignmentGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(GUIDE_AUTO_OPEN, true);
        flags.register(FILTERS_HIDDEN, false);
        for (CrewAssignmentPage page : CrewAssignmentPage.values()) {
            flags.register(seenKey(page), false);
        }
    }

    static synchronized CrewAssignmentGuidePreferences getInstance() {
        if (instance == null) {
            instance = new CrewAssignmentGuidePreferences(MekHQ.getMHQPreferences()
                                                                .forClass(CrewAssignmentGuidePreferences.class));
        }
        return instance;
    }

    private static String seenKey(final CrewAssignmentPage page) {
        return GUIDE_SEEN_PREFIX + page.name().toLowerCase(Locale.ROOT);
    }

    boolean isGuideAutoOpen() {
        return flags.get(GUIDE_AUTO_OPEN);
    }

    void setGuideAutoOpen(final boolean autoOpen) {
        flags.set(GUIDE_AUTO_OPEN, autoOpen);
    }

    boolean isGuideSeen(final CrewAssignmentPage page) {
        return flags.get(seenKey(page));
    }

    void setGuideSeen(final CrewAssignmentPage page, final boolean seen) {
        flags.set(seenKey(page), seen);
    }

    /**
     * @return {@code true} if the player hid the filter rail; it is shown until they do
     *
     * @author Illiani
     * @since 0.51.01
     */
    boolean isFiltersHidden() {
        return flags.get(FILTERS_HIDDEN);
    }

    /**
     * @param hidden {@code true} to hide the filter rail on every page, now and the next time the dialog opens
     *
     * @author Illiani
     * @since 0.51.01
     */
    void setFiltersHidden(final boolean hidden) {
        flags.set(FILTERS_HIDDEN, hidden);
    }
}
