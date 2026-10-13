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

import java.text.NumberFormat;
import java.time.LocalDate;

import mekhq.campaign.work.WorkTimeFormatter.WorkTimeBreakdown;
import org.junit.jupiter.api.Test;

/**
 * Checks that work times read as a countdown: short jobs in hours and minutes, long ones in calendar days, months and
 * years from the campaign date.
 */
class WorkTimeFormatterTest {
    /** 3025 is not a leap year, so January has 31 days and February 28. */
    private static final LocalDate CAMPAIGN_DATE = LocalDate.of(3025, 1, 1);
    private static final int WORKING_DAY = 480;

    @Test
    void jobShorterThanADayShowsHoursAndMinutes() {
        assertEquals("1 h 30 min", WorkTimeFormatter.format(90, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        assertEquals("2 h", WorkTimeFormatter.format(120, WORKING_DAY, CAMPAIGN_DATE, WorkTimeDisplay.AUTOMATIC));
        assertEquals("45 min", WorkTimeFormatter.format(45, WORKING_DAY, CAMPAIGN_DATE, WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void partlyWorkedLastDayShowsItsHours() {
        // 600 minutes is one full 8-hour day and 2 hours more
        assertEquals("1 day 2 h", WorkTimeFormatter.format(600, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void sixtyWorkingDaysCountOnTheCalendar() {
        // A K-F drive replacement: 60 working days from 1 January ends on 2 March, 2 months and 1 day away
        int kfDriveReplacement = 60 * WORKING_DAY;
        assertEquals(new WorkTimeBreakdown(0, 2, 1, 0, 0),
              WorkTimeFormatter.breakDown(kfDriveReplacement, WORKING_DAY, CAMPAIGN_DATE));
        assertEquals("2 months 1 day", WorkTimeFormatter.format(kfDriveReplacement, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void automaticShowsTheTwoLargestUnitsAndFullShowsAll() {
        // 400 working days from 1 January 3025 ends on 5 February 3026
        int minutes = (400 * WORKING_DAY) + 70;
        assertEquals("1 year 1 month", WorkTimeFormatter.format(minutes, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        assertEquals("1 year 1 month 4 days 1 h 10 min", WorkTimeFormatter.format(minutes, WORKING_DAY,
              CAMPAIGN_DATE, WorkTimeDisplay.FULL));
    }

    @Test
    void automaticDoesNotSkipPastAnEmptyUnit() {
        // 370 days is 1 year, 0 months and 5 days; Automatic stops at the empty month rather than jumping to days
        int minutes = 370 * WORKING_DAY;
        assertEquals("1 year", WorkTimeFormatter.format(minutes, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        assertEquals("1 year 5 days", WorkTimeFormatter.format(minutes, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.FULL));
    }

    @Test
    void minutesDisplayKeepsPlainMinutes() {
        String expected = NumberFormat.getIntegerInstance().format(28_800) + " min";
        assertEquals(expected, WorkTimeFormatter.format(28_800, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.MINUTES));
    }

    @Test
    void longerWorkingDayShortensTheCountdown() {
        // The same 1,440 minutes take 3 normal days, or 2 days with overtime (720 a day)
        assertEquals("3 days", WorkTimeFormatter.format(1440, WORKING_DAY, CAMPAIGN_DATE,
              WorkTimeDisplay.AUTOMATIC));
        assertEquals("2 days", WorkTimeFormatter.format(1440, 720, CAMPAIGN_DATE, WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void withoutADateWholeDaysAreNotTurnedIntoMonths() {
        assertEquals("60 days", WorkTimeFormatter.format(60 * WORKING_DAY, WORKING_DAY, null,
              WorkTimeDisplay.AUTOMATIC));
    }

    @Test
    void noWorkingDayOrNoWorkFallsBackToMinutes() {
        assertEquals("90 min", WorkTimeFormatter.format(90, 0, CAMPAIGN_DATE, WorkTimeDisplay.AUTOMATIC));
        assertEquals("0 min", WorkTimeFormatter.format(0, WORKING_DAY, CAMPAIGN_DATE, WorkTimeDisplay.AUTOMATIC));
    }
}
