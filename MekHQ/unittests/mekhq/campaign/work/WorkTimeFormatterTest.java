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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;

import mekhq.campaign.work.WorkTimeFormatter.CalendarSpan;
import org.junit.jupiter.api.Test;

/**
 * Checks that work times read plainly: work in hours and minutes, and a job that runs past one day in calendar days,
 * months and years from the campaign date.
 */
class WorkTimeFormatterTest {
    /** 3025 is not a leap year, so January has 31 days and February 28. */
    private static final LocalDate CAMPAIGN_DATE = LocalDate.of(3025, 1, 1);
    private static final int WORKING_DAY = 480;

    @Test
    void workIsSpelledOutInWorkingDays() {
        // The Wolverine WVR-6M kit refit: 2,750 minutes at 480 a day is 5 days and 350 minutes, about 5.7 days
        String minutes = NumberFormat.getIntegerInstance().format(2750) + " min";
        String about = new DecimalFormat("0.#").format(2750 / 480.0);
        String expected = minutes + " of work (45 h 50 min): 5 days 5 h 50 min at 8 h a day, about " + about
              + " working days";

        assertEquals(expected, WorkTimeFormatter.describeWorkingDays(2750, WORKING_DAY));
    }

    @Test
    void wholeWorkingDaysHaveNoRemainder() {
        String description = WorkTimeFormatter.describeWorkingDays(960, WORKING_DAY);

        assertTrue(description.contains(": 2 days at 8 h a day"), description);
    }

    @Test
    void workWithinOneDayIsShownAsWorkOnly() {
        String minutes = NumberFormat.getIntegerInstance().format(90) + " min";

        assertEquals(minutes + " of work (1 h 30 min)", WorkTimeFormatter.describeWorkingDays(90, WORKING_DAY));
    }

    @Test
    void workIsShownInHoursAndMinutes() {
        assertEquals("1 h 30 min", WorkTimeFormatter.formatWork(90, WorkTimeDisplay.AUTOMATIC));
        assertEquals("2 h", WorkTimeFormatter.formatWork(120, WorkTimeDisplay.AUTOMATIC));
        assertEquals("45 min", WorkTimeFormatter.formatWork(45, WorkTimeDisplay.AUTOMATIC));
        assertEquals("22 h 30 min", WorkTimeFormatter.formatWork(1350, WorkTimeDisplay.FULL));
    }

    @Test
    void minutesDisplayKeepsPlainMinutes() {
        String expected = NumberFormat.getIntegerInstance().format(1350) + " min";
        assertEquals(expected, WorkTimeFormatter.formatWork(1350, WorkTimeDisplay.MINUTES));
        assertEquals(expected, WorkTimeFormatter.formatDuration(1350, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.MINUTES));
    }

    @Test
    void jobWithinOneWorkingDayShowsItsWork() {
        assertEquals("8 h", WorkTimeFormatter.formatDuration(480, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        assertEquals("4 h", WorkTimeFormatter.formatDuration(240, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void jobRunningPastADayCountsEachDayItTouches() {
        // 600 minutes is a full 8-hour day and 2 hours of the next, so it runs over 2 days, not "1 day 2 h"
        assertEquals(2, WorkTimeFormatter.countWorkingDays(600, WORKING_DAY));
        assertEquals("2 days", WorkTimeFormatter.formatDuration(600, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        // With overtime (720 minutes a day) the same work fits in one day
        assertEquals("10 h", WorkTimeFormatter.formatDuration(600, 720, CAMPAIGN_DATE, WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void sixtyWorkingDaysCountOnTheCalendar() {
        // A K-F drive replacement: 60 working days from 1 January ends on 2 March, 2 months and 1 day away
        assertEquals(new CalendarSpan(0, 2, 1), WorkTimeFormatter.toCalendarSpan(60, CAMPAIGN_DATE));
        assertEquals("2 months 1 day", WorkTimeFormatter.formatDuration(60 * WORKING_DAY, WORKING_DAY,
              CAMPAIGN_DATE, WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void automaticShowsTheTwoLargestUnitsAndFullShowsAll() {
        // 401 days from 1 January 3025 ends on 6 February 3026
        int minutes = (400 * WORKING_DAY) + 70;
        assertEquals("1 year 1 month", WorkTimeFormatter.formatDuration(minutes, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        assertEquals("1 year 1 month 5 days", WorkTimeFormatter.formatDuration(minutes, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.FULL));
    }

    @Test
    void automaticDoesNotSkipPastAnEmptyUnit() {
        // 370 days is 1 year, 0 months and 5 days; Automatic stops at the empty month rather than jumping to days
        int minutes = 370 * WORKING_DAY;
        assertEquals("1 year", WorkTimeFormatter.formatDuration(minutes, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        assertEquals("1 year 5 days", WorkTimeFormatter.formatDuration(minutes, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.FULL));
    }

    @Test
    void withoutADateDaysAreNotTurnedIntoMonths() {
        assertEquals("60 days", WorkTimeFormatter.formatDuration(60 * WORKING_DAY, WORKING_DAY, null,
              WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void noWorkingDayShowsTheWorkOnly() {
        assertEquals("1 h 30 min", WorkTimeFormatter.formatDuration(90, 0, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        assertEquals("0 min", WorkTimeFormatter.formatWork(0, WorkTimeDisplay.AUTOMATIC));
    }
}
