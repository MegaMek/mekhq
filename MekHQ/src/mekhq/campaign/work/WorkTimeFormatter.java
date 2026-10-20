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

import jakarta.annotation.Nullable;

/**
 * Turns minutes of work into text a player can read.
 *
 * <ul>
 *   <li>Work itself is shown in hours and minutes, for example {@code 22 h 30 min}.</li>
 *   <li>How long a job runs is shown the same way when it fits in one working day. A longer job is shown in calendar
 *   days, months and years, counting each day it runs into as a whole day, for example {@code 3 days} or
 *   {@code 2 months 1 day}. Days are counted forward from the campaign date, so a month is a real calendar month.</li>
 * </ul>
 */
public final class WorkTimeFormatter {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";
    private static final int MINUTES_PER_HOUR = 60;

    private WorkTimeFormatter() {}

    /**
     * A stretch of calendar time.
     *
     * @param years  whole years
     * @param months whole months after the years
     * @param days   days after the months
     */
    public record CalendarSpan(int years, int months, int days) {}

    /**
     * @param workMinutes   minutes of work
     * @param minutesPerDay minutes the worker puts in each day; must be above zero
     *
     * @return how many days the work runs into, counting a part day as a whole day
     */
    public static int countWorkingDays(int workMinutes, int minutesPerDay) {
        return (int) Math.ceil((double) Math.max(0, workMinutes) / minutesPerDay);
    }

    /**
     * @param days  days counted forward from {@code today}
     * @param today the campaign date, or {@code null} to count days only (no months or years)
     *
     * @return the days as years, months and days on the calendar
     */
    public static CalendarSpan toCalendarSpan(int days, @Nullable LocalDate today) {
        if (today == null) {
            return new CalendarSpan(0, 0, days);
        }
        Period period = Period.between(today, today.plusDays(days));
        return new CalendarSpan(period.getYears(), period.getMonths(), period.getDays());
    }

    /**
     * Formats an amount of work, for example {@code 22 h 30 min}, or {@code 1,350 min} for the Minutes display.
     *
     * @param workMinutes minutes of work
     * @param display     the player's chosen display
     *
     * @return the work as text
     */
    public static String formatWork(int workMinutes, WorkTimeDisplay display) {
        int minutes = Math.max(0, workMinutes);
        if ((display == WorkTimeDisplay.MINUTES) || (minutes < MINUTES_PER_HOUR)) {
            return unitText("minutes", minutes);
        }
        int hours = minutes / MINUTES_PER_HOUR;
        int minutesAfterHours = minutes % MINUTES_PER_HOUR;
        String hoursText = unitText("hours", hours);
        if (minutesAfterHours == 0) {
            return hoursText;
        }
        return hoursText + ' ' + unitText("minutes", minutesAfterHours);
    }

    /**
     * Formats how long a job runs: its work in hours and minutes when it fits in one working day, otherwise the
     * calendar days it runs into, for example {@code 3 days} or {@code 2 months 1 day}.
     *
     * @param workMinutes   minutes of work
     * @param minutesPerDay minutes the worker puts in each day; zero or less shows the work only, since no days can
     *                      be counted
     * @param today         the campaign date the days are counted from, or {@code null} to count days only
     * @param display       the player's chosen display; Automatic shows the largest calendar unit and the one below
     *                      it, Full shows every calendar unit, Minutes shows plain minutes
     *
     * @return how long the job runs, as text
     */
    public static String formatDuration(int workMinutes, int minutesPerDay, @Nullable LocalDate today,
          WorkTimeDisplay display) {
        boolean isWithinOneDay = (minutesPerDay <= 0) || (workMinutes <= minutesPerDay);
        if ((display == WorkTimeDisplay.MINUTES) || isWithinOneDay) {
            return formatWork(workMinutes, display);
        }
        CalendarSpan span = toCalendarSpan(countWorkingDays(workMinutes, minutesPerDay), today);
        int[] amounts = { span.years(), span.months(), span.days() };
        String[] unitKeys = { "years", "months", "days" };

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

    /**
     * Spells out an amount of work in working days, for example {@code 2,750 min of work (45 h 50 min): 5 days 5 h 50
     * min at 8 h a day, about 5.7 working days}. Work that fits in one day is shown as work alone.
     *
     * @param workMinutes   minutes of work
     * @param minutesPerDay minutes worked each day; must be above zero
     *
     * @return the work as working days, as text
     */
    public static String describeWorkingDays(int workMinutes, int minutesPerDay) {
        int minutes = Math.max(0, workMinutes);
        String minutesText = formatWork(minutes, WorkTimeDisplay.MINUTES);
        String hoursText = formatWork(minutes, WorkTimeDisplay.AUTOMATIC);
        if (minutes <= minutesPerDay) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeFormatter.workOnly", minutesText, hoursText);
        }
        int wholeDays = minutes / minutesPerDay;
        int minutesAfterDays = minutes % minutesPerDay;
        String daysText = unitText("days", wholeDays);
        if (minutesAfterDays > 0) {
            daysText = daysText + ' ' + formatWork(minutesAfterDays, WorkTimeDisplay.AUTOMATIC);
        }
        double workingDays = (double) minutes / minutesPerDay;
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeFormatter.workingDays", minutesText, hoursText, daysText,
              formatWork(minutesPerDay, WorkTimeDisplay.AUTOMATIC), workingDays);
    }

    private static String unitText(String unitKey, int amount) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "WorkTimeFormatter." + unitKey, amount);
    }
}
