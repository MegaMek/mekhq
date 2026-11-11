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
 * The crew assignment dialog's tutorial settings: whether each page's guide has been seen, and whether guides open by
 * themselves on a page's first visit.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while MekHQ is
 * closed) resets every guide.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewAssignmentGuidePreferences {
    static final String GUIDE_AUTO_OPEN = "guideAutoOpen";
    static final String GUIDE_SEEN_PREFIX = "guideSeen.";

    private static CrewAssignmentGuidePreferences instance;

    private final PreferenceFlags flags;

    CrewAssignmentGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(GUIDE_AUTO_OPEN, true);
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
}
