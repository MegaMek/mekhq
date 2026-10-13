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
package mekhq.campaign.work;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;

/**
 * Turns minutes of work into a countdown a player can read, such as {@code 2 months 5 days} or {@code 1 h 30 min}.
 *
 * <p>Whole working days are counted forward on the campaign calendar from today, so a month is a real calendar month
 * rather than a fixed 30 days. Work left over after the last whole day is shown in hours and minutes.</p>
 */
public final class WorkTimeFormatter {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";
    private static final int MINUTES_PER_HOUR = 60;

    private WorkTimeFormatter() {}

    /**
     * The parts of a countdown, from years down to minutes.
     *
     * @param years   whole years
     * @param months  whole months after the years
     * @param days    whole days after the months
     * @param hours   hours of work on the last, partly worked day
     * @param minutes minutes of work after those hours
     */
    public record WorkTimeBreakdown(int years, int months, int days, int hours, int minutes) {}

    /**
     * Splits work into whole working days, counted on the calendar from {@code today}, and the hours and minutes
     * left over.
     *
     * @param workMinutes   minutes of work left
     * @param minutesPerDay minutes the worker puts in each day; must be above zero
     * @param today         the campaign date the countdown starts from, or {@code null} to count whole days only
     *                      (no months or years)
     *
     * @return the countdown, from years down to minutes
     */
    public static WorkTimeBreakdown breakDown(int workMinutes, int minutesPerDay, @Nullable LocalDate today) {
        int minutesLeft = Math.max(0, workMinutes);
        int wholeDays = minutesLeft / minutesPerDay;
        int lastDayMinutes = minutesLeft % minutesPerDay;
        int hours = lastDayMinutes / MINUTES_PER_HOUR;
        int minutes = lastDayMinutes % MINUTES_PER_HOUR;
        if ((today == null) || (wholeDays == 0)) {
            return new WorkTimeBreakdown(0, 0, wholeDays, hours, minutes);
        }
        Period calendarSpan = Period.between(today, today.plusDays(wholeDays));
        return new WorkTimeBreakdown(calendarSpan.getYears(), calendarSpan.getMonths(), calendarSpan.getDays(), hours,
              minutes);
    }

    /**
     * Formats work as a countdown.
     *
     * @param workMinutes   minutes of work left
     * @param minutesPerDay minutes the worker puts in each day; zero or less shows plain minutes, since no countdown
     *                      can be made
     * @param today         the campaign date the countdown starts from, or {@code null} to count whole days only
     * @param display       which units to show
     *
     * @return the countdown text, for example {@code 2 months 5 days}
     */
    public static String format(int workMinutes, int minutesPerDay, @Nullable LocalDate today,
          WorkTimeDisplay display) {
        int minutesLeft = Math.max(0, workMinutes);
        if ((display == WorkTimeDisplay.MINUTES) || (minutesPerDay <= 0) || (minutesLeft == 0)) {
            return unitText("minutes", minutesLeft);
        }
        WorkTimeBreakdown breakdown = breakDown(minutesLeft, minutesPerDay, today);
        int[] amounts = { breakdown.years(), breakdown.months(), breakdown.days(), breakdown.hours(),
                          breakdown.minutes() };
        String[] unitKeys = { "years", "months", "days", "hours", "minutes" };

        int largestUnit = 0;
        while (amounts[largestUnit] == 0) {
            largestUnit++;
        }
        // Automatic shows the largest unit and the one just below it; Full shows every unit that applies
        int smallestUnit = (display == WorkTimeDisplay.FULL) ? (amounts.length - 1)
                                 : Math.min(largestUnit + 1, amounts.length - 1);
        List<String> shownUnits = new ArrayList<>();
        for (int unit = largestUnit; unit <= smallestUnit; unit++) {
            if (amounts[unit] > 0) {
                shownUnits.add(unitText(unitKeys[unit], amounts[unit]));
            }
        }
        return String.join(" ", shownUnits);
    }

    private static String unitText(String unitKey, int amount) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeFormatter." + unitKey, amount);
    }
}
