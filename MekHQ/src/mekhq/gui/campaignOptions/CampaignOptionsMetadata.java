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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.annotation.Nullable;
import megamek.SuiteConstants;
import megamek.Version;
import megamek.client.ui.settings.SettingsBadge;

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
     * How a campaign option's version badge is colored, relative to the last Core release.
     */
    private enum AddedSinceBadge {
        /** Added after the last Core release - the version is shown in purple. */
        SINCE_LAST_CORE("sinceLastCore"),

        /** Added in the last Core release or earlier - the version is shown in blue. */
        LAST_CORE_OR_EARLIER("lastCoreOrEarlier");

        private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignOptionsDialog";

        private final String key;

        AddedSinceBadge(String key) {
            this.key = key;
        }

        /**
         * Classifies an option's version against the last Core release.
         *
         * @param version  the version the option was added in
         * @param lastCore the last Core release
         *
         * @return {@link #SINCE_LAST_CORE} if the option is newer than the last Core release, otherwise
         *       {@link #LAST_CORE_OR_EARLIER}
         */
        static AddedSinceBadge of(Version version, Version lastCore) {
            return version.isHigherThan(lastCore) ? SINCE_LAST_CORE : LAST_CORE_OR_EARLIER;
        }

        /**
         * Gets the color for this badge from the properties file.
         *
         * @return the color, or the label foreground if the color cannot be read
         */
        @Nullable Color getColor() {
            try {
                return Color.decode(getTextAt(RESOURCE_BUNDLE, "badge." + key + ".color"));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /**
         * @return the localized legend description for this badge
         */
        String getDescription() {
            return getTextAt(RESOURCE_BUNDLE, "legend." + key);
        }

        /**
         * @param lastCore the last Core release
         *
         * @return the sample text shown for this badge in the legend, such as "&gt; 0.51.00"
         */
        String getLegendSample(Version lastCore) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "legend." + key + ".sample", formatVersionNumber(lastCore));
        }
    }

    /**
     * Builds the legend entries explaining the version badges, using the last Core release as the dividing line.
     *
     * @return the legend entries, newest first
     */
    static List<SettingsBadge> addedSinceLegendEntries() {
        Version lastCore = SuiteConstants.LAST_MILESTONE;
        List<SettingsBadge> entries = new ArrayList<>();
        for (AddedSinceBadge badge : AddedSinceBadge.values()) {
            entries.add(SettingsBadge.ofText(badge.getLegendSample(lastCore), badge.getColor(),
                  badge.getDescription()));
        }
        return entries;
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
     * Gets the formatted HTML badge for the "added since" indicator, if present.
     *
     * @return the HTML-formatted, colored version number, or an empty string if no version is set
     */
    String getAddedSinceBadgeHtml() {
        SettingsBadge badge = getAddedSinceSettingsBadge();
        return badge == null ? "" : badge.toHtml();
    }

    /**
     * @return this option's version as a colored text badge, or {@code null} when no version is set
     */
    @Nullable SettingsBadge getAddedSinceSettingsBadge() {
        return getAddedSinceSettingsBadge(SuiteConstants.LAST_MILESTONE);
    }

    /**
     * Builds this option's version badge: purple if the option was added after the given Core release, blue
     * otherwise.
     *
     * @param lastCore the last Core release
     *
     * @return this option's version as a colored text badge, or {@code null} when no version is set
     */
    @Nullable SettingsBadge getAddedSinceSettingsBadge(Version lastCore) {
        if (version == null) {
            return null;
        }
        AddedSinceBadge badge = AddedSinceBadge.of(version, lastCore);
        return SettingsBadge.ofText(formatVersionNumber(version), badge.getColor(), badge.getDescription());
    }

    /**
     * Formats a version as its release number alone, such as {@code 0.51.01} or {@code 0.49.19.1}.
     *
     * <p>{@link Version#toString()} also appends the build information (branch and git hash) on PR and nightly
     * builds, which says nothing about when an option was added, so it is left out here.</p>
     *
     * @param version the version to format
     *
     * @return the version number without any build information
     */
    private static String formatVersionNumber(Version version) {
        return String.format(Locale.ROOT, "%d.%02d.%02d%s",
              version.getMajor(),
              version.getMinor(),
              version.getPatch(),
              version.hasRevision() ? "." + version.getRevision() : "");
    }
}
