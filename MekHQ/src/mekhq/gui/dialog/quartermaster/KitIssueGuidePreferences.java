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
package mekhq.gui.dialog.quartermaster;

import java.util.List;

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.preferences.PreferenceFlags;

/**
 * The {@link IssueEquipmentDialog}'s tutorial settings: whether the player has seen each kit family's guide, and
 * whether guides open by themselves the first time a family's page is shown.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while MekHQ is
 * closed) shows the guides again.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class KitIssueGuidePreferences {
    static final String GUIDE_AUTO_OPEN = "guideAutoOpen";
    static final String GUIDE_SEEN_PREFIX = "guideSeen.";

    /** The armor kit family's guide. */
    static final String ARMOR_GUIDE = "armor";
    /** The equipment kit family's guide, shared by the primary and secondary slot pages. */
    static final String TOOLS_GUIDE = "tools";
    private static final List<String> GUIDES = List.of(ARMOR_GUIDE, TOOLS_GUIDE);

    private static KitIssueGuidePreferences instance;

    private final PreferenceFlags flags;

    /**
     * @param node the preferences node to store the settings under
     */
    KitIssueGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(GUIDE_AUTO_OPEN, true);
        for (String guide : GUIDES) {
            flags.register(GUIDE_SEEN_PREFIX + guide, false);
        }
    }

    /**
     * @return the settings, stored in MekHQ's preferences file
     */
    public static synchronized KitIssueGuidePreferences getInstance() {
        if (instance == null) {
            instance = new KitIssueGuidePreferences(MekHQ.getMHQPreferences().forClass(KitIssueGuidePreferences.class));
        }
        return instance;
    }

    /**
     * @return {@code true} if a kit family's guide opens by itself the first time its page is shown
     */
    public boolean isGuideAutoOpen() {
        return flags.get(GUIDE_AUTO_OPEN);
    }

    public void setGuideAutoOpen(final boolean autoOpen) {
        flags.set(GUIDE_AUTO_OPEN, autoOpen);
    }

    /**
     * @param guide a kit family's guide key, such as {@link #ARMOR_GUIDE}
     *
     * @return {@code true} once the player has seen that guide
     */
    boolean isGuideSeen(final String guide) {
        return flags.get(GUIDE_SEEN_PREFIX + guide);
    }

    void setGuideSeen(final String guide, final boolean seen) {
        flags.set(GUIDE_SEEN_PREFIX + guide, seen);
    }
}
