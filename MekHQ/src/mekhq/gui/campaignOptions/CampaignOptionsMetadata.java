/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.Version;
import megamek.common.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.MissingResourceException;

import java.util.Set;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * Metadata for campaign options, tracking when options were added and their special characteristics.
 *
 * @param version the version when this campaign option was added, or null if not recently added
 * @param flags   special flags indicating characteristics of this option (custom, documented, etc.), or empty set if
 *                none
 */
public record CampaignOptionsMetadata(
      @Nullable Version version,
      Set<CampaignOptionFlag> flags
) {
    /**
     * Indicates when a campaign option was added, displayed as a colored star badge.
     */
    private enum AddedSinceBadge {
        /** Added since the last development release - shown as a filled purple star (★) */
        DEVELOPMENT("development"),

        /** Added since the last milestone release - shown as a hollow green star (☆) */
        MILESTONE("milestone");

        private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignOptionsDialog";

        private final String key;

        AddedSinceBadge(String key) {
            this.key = key;
        }

        /**
         * Gets the color for this badge from the properties file.
         *
         * @return the color hex code, or "#000000" if not found
         */
        String getColor() {
            try {
                return getTextAt(RESOURCE_BUNDLE, "badge." + key + ".color");
            } catch (MissingResourceException e) {
                return "#000000";
            }
        }

        /**
         * Gets the symbol for this badge from the properties file.
         *
         * @return the symbol, or "?" if not found
         */
        String getSymbol() {
            try {
                return getTextAt(RESOURCE_BUNDLE, "badge." + key + ".symbol");
            } catch (MissingResourceException e) {
                return "?";
            }
        }
    }

    /**
     * Canonical constructor that ensures flags is never null.
     * <br><br>
     * Should not be used directly, use {@link CampaignOptionsUtilities#getMetadata(Version, CampaignOptionFlag...)}
     * so don't create unnecessary duplicate records.
     */
    public CampaignOptionsMetadata {
        flags = (flags == null) ? EnumSet.noneOf(CampaignOptionFlag.class) : (flags.isEmpty() ?
                                                                                    Collections.emptySet() :
                                                                                    EnumSet.copyOf(flags));
    }

    /**
     * Checks if this campaign option has a specific flag.
     *
     * @param flag the flag to check for
     *
     * @return true if the flag is present
     */
    public boolean hasFlag(CampaignOptionFlag flag) {
        return flags.contains(flag);
    }

    /**
     * Determines which badge to display based on the version.
     * <p>
     * - If the version equals the current running version: DEVELOPMENT badge (added since last development release)
     * - If the version is after the last milestone: MILESTONE badge (added since last milestone release)
     * - Otherwise: no badge (older feature)
     *
     * @return the appropriate badge type, or null if no version is set or feature is not recent
     */
    private AddedSinceBadge getBadgeType() {
        if (version == null) {
            return null;
        }

        // If this version equals the current running version → added since last development
        if (version.equals(megamek.SuiteConstants.VERSION)) {
            return AddedSinceBadge.DEVELOPMENT;
        }

        // If this version is after the last milestone → added since last milestone
        if (version.compareTo(megamek.SuiteConstants.LAST_MILESTONE) > 0) {
            return AddedSinceBadge.MILESTONE;
        }

        // Old feature, no badge
        return null;
    }

    /**
     * Gets the formatted HTML badge for the "added since" indicator, if present.
     *
     * @return an HTML-formatted colored star badge, or empty string if not recently added
     */
    String getAddedSinceBadgeHtml() {
        AddedSinceBadge badge = getBadgeType();
        if (badge == null) {
            return "";
        }
        return String.format(" <span style=\"color:%s;\">%s</span>",
              badge.getColor(), badge.getSymbol());
    }
}
