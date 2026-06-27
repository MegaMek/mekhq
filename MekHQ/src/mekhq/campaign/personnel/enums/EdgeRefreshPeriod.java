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
package mekhq.campaign.personnel.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.DayOfWeek;
import java.time.LocalDate;

import megamek.logging.MMLogger;

/**
 * Defines the frequency at which Edge points are refreshed for a campaign.
 *
 * <p>Each constant represents a distinct refresh cadence, from never refreshing to refreshing on an annual basis.
 * The {@link #shouldRefresh(EdgeRefreshPeriod, LocalDate)} method determines whether a refresh should occur on any
 * given date according to the selected period.
 *
 * <p>Labels are loaded from the {@code mekhq.resources.EdgeRefreshPeriod} resource bundle, keyed by {@code
 * <CONSTANT_NAME>.label} (e.g. {@code DAILY.label}).
 *
 * @author Illiani
 * @since 0.51.0
 */
public enum EdgeRefreshPeriod {
    /**
     * Edge is never automatically refreshed.
     */
    NEVER("NEVER"),
    /**
     * Edge refreshes at the start of each new day.
     */
    DAILY("DAILY"),
    /**
     * Edge refreshes every Monday.
     */
    WEEKLY("WEEKLY"),
    /**
     * Edge refreshes on the first day of each new month
     */
    MONTHLY("MONTHLY"),
    /**
     * Edge refreshes on the first day of the year ({@link LocalDate#getDayOfYear()} equals {@code 1}).
     */
    ANNUALLY("ANNUALLY");

    private final String lookupKey;
    private final String label;
    private final String tooltip;

    EdgeRefreshPeriod(String lookupKey) {
        this.lookupKey = lookupKey;
        this.label = generateLabel();
        this.tooltip = generateTooltip();
    }

    private final String RESOURCE_BUNDLE = "mekhq.resources.EdgeRefreshPeriod";

    /**
     * Returns the lookup key for this refresh period.
     *
     * @return the lookup key string; never {@code null}
     *
     * @author Illiani
     * @since 0.51.0
     */
    public String getLookupKey() {
        return lookupKey;
    }

    /**
     * Returns the human-readable, localized label for this refresh period.
     *
     * @return the localized label string; never {@code null}
     *
     * @author Illiani
     * @since 0.51.0
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the human-readable, localized tooltip for this refresh period.
     *
     * @return the localized tooltip string; never {@code null}
     *
     * @author Illiani
     * @since 0.51.0
     */
    public String getTooltip() {
        return tooltip;
    }

    /**
     * Loads the localized label from the resource bundle using the lookup key.
     *
     * @return the resolved label string for this constant
     *
     * @author Illiani
     * @since 0.51.0
     */
    private String generateLabel() {
        final String RESOURCE_KEY = lookupKey + ".label";
        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Loads the localized tooltip from the resource bundle using the lookup key.
     *
     * @return the resolved tooltip string for this constant
     *
     * @author Illiani
     * @since 0.51.0
     */
    private String generateTooltip() {
        final String RESOURCE_KEY = lookupKey + ".tooltip";
        return getTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Determines whether Edge should be refreshed on the given date for the specified refresh period.
     *
     * <p>The evaluation rules per period are:</p>
     * <ul>
     *     <li>{@link #NEVER}    – always {@code false}</li>
     *     <li>{@link #DAILY}    – always {@code true}</li>
     *     <li>{@link #WEEKLY}   – {@code true} when {@code today} is a Monday</li>
     *     <li>{@link #MONTHLY}  – {@code true} when {@code today} is the 1st day of the month</li>
     *     <li>{@link #ANNUALLY} – {@code true} when {@code today} is the 1st day of the year</li>
     * </ul>
     *
     * @param period the configured refresh period; must not be {@code null}
     * @param today  the date to evaluate; must not be {@code null}
     *
     * @return {@code true} if a refresh should occur on {@code today}, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static boolean shouldRefresh(EdgeRefreshPeriod period, LocalDate today) {
        return switch (period) {
            case NEVER -> false;
            case DAILY -> true;
            case WEEKLY -> today.getDayOfWeek() == DayOfWeek.MONDAY;
            case MONTHLY -> today.getDayOfMonth() == 1;
            case ANNUALLY -> today.getDayOfYear() == 1;
        };
    }

    /**
     * Parses an {@code EdgeRefreshPeriod} from a string.
     *
     * <p>Resolution is attempted in the following order:</p>
     * <ol>
     *     <li>Case-insensitive name match (spaces normalized to underscores), e.g. {@code "weekly"} or {@code "per
     *     week"} → {@link #WEEKLY}.</li>
     *     <li>Ordinal index, e.g. {@code "2"} → {@link #WEEKLY}.</li>
     *     <li>If both attempts fail, logs an error and returns {@link #WEEKLY} as the default.</li>
     * </ol>
     *
     * @param text the string to parse; must not be {@code null}
     *
     * @return the matching {@code EdgeRefreshPeriod}, or {@link #WEEKLY} if unresolvable
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static EdgeRefreshPeriod fromString(String text) {
        try {
            return EdgeRefreshPeriod.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {}

        try {
            return EdgeRefreshPeriod.values()[Integer.parseInt(text)];
        } catch (Exception ignored) {}

        MMLogger logger = MMLogger.create(EdgeRefreshPeriod.class);
        logger.error("Unknown EdgeRefreshPeriod ordinal: {} - returning {}.", text, WEEKLY);

        return WEEKLY;
    }

    /**
     * Returns the localized label for this refresh period.
     *
     * @return the same value as {@link #getLabel()}; never {@code null}
     *
     * @author Illiani
     * @since 0.51.0
     */
    @Override
    public String toString() {
        return label;
    }
}
