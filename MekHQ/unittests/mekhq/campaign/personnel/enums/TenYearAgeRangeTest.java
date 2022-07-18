/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TenYearAgeRangeTest {
    //region Variable Declarations
    private static final TenYearAgeRange[] ranges = TenYearAgeRange.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsUnderOne() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.UNDER_ONE) {
                assertTrue(tenYearAgeRange.isUnderOne());
            } else {
                assertFalse(tenYearAgeRange.isUnderOne());
            }
        }
    }

    @Test
    public void testIsOneToFour() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.ONE_FOUR) {
                assertTrue(tenYearAgeRange.isOneToFour());
            } else {
                assertFalse(tenYearAgeRange.isOneToFour());
            }
        }
    }

    @Test
    public void testIsFiveToFourteen() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.FIVE_FOURTEEN) {
                assertTrue(tenYearAgeRange.isFiveToFourteen());
            } else {
                assertFalse(tenYearAgeRange.isFiveToFourteen());
            }
        }
    }

    @Test
    public void testIsFifteenToTwentyFour() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.FIFTEEN_TWENTY_FOUR) {
                assertTrue(tenYearAgeRange.isFifteenToTwentyFour());
            } else {
                assertFalse(tenYearAgeRange.isFifteenToTwentyFour());
            }
        }
    }

    @Test
    public void testIsTwentyFiveToThirtyFour() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.TWENTY_FIVE_THIRTY_FOUR) {
                assertTrue(tenYearAgeRange.isTwentyFiveToThirtyFour());
            } else {
                assertFalse(tenYearAgeRange.isTwentyFiveToThirtyFour());
            }
        }
    }

    @Test
    public void testIsThirtyFiveToFortyFour() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.THIRTY_FIVE_FORTY_FOUR) {
                assertTrue(tenYearAgeRange.isThirtyFiveToFortyFour());
            } else {
                assertFalse(tenYearAgeRange.isThirtyFiveToFortyFour());
            }
        }
    }

    @Test
    public void testIsFortyFiveToFiftyFour() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.FORTY_FIVE_FIFTY_FOUR) {
                assertTrue(tenYearAgeRange.isFortyFiveToFiftyFour());
            } else {
                assertFalse(tenYearAgeRange.isFortyFiveToFiftyFour());
            }
        }
    }

    @Test
    public void testIsFiftyFiveToSixtyFour() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.FIFTY_FIVE_SIXTY_FOUR) {
                assertTrue(tenYearAgeRange.isFiftyFiveToSixtyFour());
            } else {
                assertFalse(tenYearAgeRange.isFiftyFiveToSixtyFour());
            }
        }
    }

    @Test
    public void testIsSixtyFiveToSeventyFour() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.SIXTY_FIVE_SEVENTY_FOUR) {
                assertTrue(tenYearAgeRange.isSixtyFiveToSeventyFour());
            } else {
                assertFalse(tenYearAgeRange.isSixtyFiveToSeventyFour());
            }
        }
    }

    @Test
    public void testIsSeventyFiveToEightyFour() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.SEVENTY_FIVE_EIGHTY_FOUR) {
                assertTrue(tenYearAgeRange.isSeventyFiveToEightyFour());
            } else {
                assertFalse(tenYearAgeRange.isSeventyFiveToEightyFour());
            }
        }
    }

    @Test
    public void testIsEightyFiveOrOlder() {
        for (final TenYearAgeRange tenYearAgeRange : ranges) {
            if (tenYearAgeRange == TenYearAgeRange.EIGHTY_FIVE_OR_OLDER) {
                assertTrue(tenYearAgeRange.isEightyFiveOrOlder());
            } else {
                assertFalse(tenYearAgeRange.isEightyFiveOrOlder());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testDetermineAgeRange() {
        assertEquals(TenYearAgeRange.UNDER_ONE, TenYearAgeRange.determineAgeRange(0));
        assertEquals(TenYearAgeRange.ONE_FOUR, TenYearAgeRange.determineAgeRange(4));
        assertEquals(TenYearAgeRange.FIVE_FOURTEEN, TenYearAgeRange.determineAgeRange(5));
        assertEquals(TenYearAgeRange.FIFTEEN_TWENTY_FOUR, TenYearAgeRange.determineAgeRange(15));
        assertEquals(TenYearAgeRange.TWENTY_FIVE_THIRTY_FOUR, TenYearAgeRange.determineAgeRange(34));
        assertEquals(TenYearAgeRange.THIRTY_FIVE_FORTY_FOUR, TenYearAgeRange.determineAgeRange(35));
        assertEquals(TenYearAgeRange.FORTY_FIVE_FIFTY_FOUR, TenYearAgeRange.determineAgeRange(50));
        assertEquals(TenYearAgeRange.FIFTY_FIVE_SIXTY_FOUR, TenYearAgeRange.determineAgeRange(64));
        assertEquals(TenYearAgeRange.SIXTY_FIVE_SEVENTY_FOUR, TenYearAgeRange.determineAgeRange(65));
        assertEquals(TenYearAgeRange.SEVENTY_FIVE_EIGHTY_FOUR, TenYearAgeRange.determineAgeRange(84));
        assertEquals(TenYearAgeRange.EIGHTY_FIVE_OR_OLDER, TenYearAgeRange.determineAgeRange(85));
        assertEquals(TenYearAgeRange.EIGHTY_FIVE_OR_OLDER, TenYearAgeRange.determineAgeRange(100));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("TenYearAgeRange.UNDER_ONE.text"), TenYearAgeRange.UNDER_ONE.toString());
        assertEquals(resources.getString("TenYearAgeRange.EIGHTY_FIVE_OR_OLDER.text"), TenYearAgeRange.EIGHTY_FIVE_OR_OLDER.toString());
    }
}
