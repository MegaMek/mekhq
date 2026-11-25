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
package mekhq.gui.dialog.reportDialogs;

import java.util.List;

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.preferences.PreferenceFlags;

/**
 * The Chaos Reputation report's tutorial settings: whether each guide has been seen, and whether a guide opens by
 * itself the first time the report is opened in its tracking mode.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign. Deleting this class's entry from that file (while MekHQ is
 * closed) resets the guides.</p>
 */
public final class ChaosReputationGuidePreferences {
    static final String GUIDE_AUTO_OPEN = "guideAutoOpen";
    static final String GUIDE_SEEN_PREFIX = "guideSeen.";

    /** The guide for reputation derived from personnel. */
    static final String PERSONNEL_GUIDE = "personnel";
    /** The guide for reputation tracked at the campaign level. */
    static final String CAMPAIGN_GUIDE = "campaign";
    private static final List<String> GUIDES = List.of(PERSONNEL_GUIDE, CAMPAIGN_GUIDE);

    private static ChaosReputationGuidePreferences instance;

    private final PreferenceFlags flags;

    /**
     * @param node the preferences node to store the settings under
     */
    ChaosReputationGuidePreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(GUIDE_AUTO_OPEN, true);
        for (String guide : GUIDES) {
            flags.register(GUIDE_SEEN_PREFIX + guide, false);
        }
    }

    /**
     * @return the settings, stored in MekHQ's preferences file
     */
    public static synchronized ChaosReputationGuidePreferences getInstance() {
        if (instance == null) {
            instance = new ChaosReputationGuidePreferences(MekHQ.getMHQPreferences()
                                                                 .forClass(ChaosReputationGuidePreferences.class));
        }
        return instance;
    }

    /**
     * @return {@code true} if a guide opens by itself the first time the report is opened in its tracking mode
     */
    public boolean isGuideAutoOpen() {
        return flags.get(GUIDE_AUTO_OPEN);
    }

    public void setGuideAutoOpen(final boolean autoOpen) {
        flags.set(GUIDE_AUTO_OPEN, autoOpen);
    }

    /**
     * @param guide a guide key, such as {@link #PERSONNEL_GUIDE}
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
