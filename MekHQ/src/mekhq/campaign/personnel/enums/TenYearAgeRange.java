/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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

import java.util.ResourceBundle;

public enum TenYearAgeRange {
    //region Enum Declarations
    UNDER_ONE("TenYearAgeRange.UNDER_ONE.text", "TenYearAgeRange.UNDER_ONE.toolTipText"),
    ONE_FOUR("TenYearAgeRange.ONE_FOUR.text", "TenYearAgeRange.ONE_FOUR.toolTipText"),
    FIVE_FOURTEEN("TenYearAgeRange.FIVE_FOURTEEN.text", "TenYearAgeRange.FIVE_FOURTEEN.toolTipText"),
    FIFTEEN_TWENTY_FOUR("TenYearAgeRange.FIFTEEN_TWENTY_FOUR.text", "TenYearAgeRange.FIFTEEN_TWENTY_FOUR.toolTipText"),
    TWENTY_FIVE_THIRTY_FOUR("TenYearAgeRange.TWENTY_FIVE_THIRTY_FOUR.text", "TenYearAgeRange.TWENTY_FIVE_THIRTY_FOUR.toolTipText"),
    THIRTY_FIVE_FORTY_FOUR("TenYearAgeRange.THIRTY_FIVE_FORTY_FOUR.text", "TenYearAgeRange.THIRTY_FIVE_FORTY_FOUR.toolTipText"),
    FORTY_FIVE_FIFTY_FOUR("TenYearAgeRange.FORTY_FIVE_FIFTY_FOUR.text", "TenYearAgeRange.FORTY_FIVE_FIFTY_FOUR.toolTipText"),
    FIFTY_FIVE_SIXTY_FOUR("TenYearAgeRange.FIFTY_FIVE_SIXTY_FOUR.text", "TenYearAgeRange.FIFTY_FIVE_SIXTY_FOUR.toolTipText"),
    SIXTY_FIVE_SEVENTY_FOUR("TenYearAgeRange.SIXTY_FIVE_SEVENTY_FOUR.text", "TenYearAgeRange.SIXTY_FIVE_SEVENTY_FOUR.toolTipText"),
    SEVENTY_FIVE_EIGHTY_FOUR("TenYearAgeRange.SEVENTY_FIVE_EIGHTY_FOUR.text", "TenYearAgeRange.SEVENTY_FIVE_EIGHTY_FOUR.toolTipText"),
    EIGHTY_FIVE_OR_OLDER("TenYearAgeRange.EIGHTY_FIVE_OR_OLDER.text", "TenYearAgeRange.EIGHTY_FIVE_OR_OLDER.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    TenYearAgeRange(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isUnderOne() {
        return this == UNDER_ONE;
    }

    public boolean isOneToFour() {
        return this == ONE_FOUR;
    }

    public boolean isFiveToFourteen() {
        return this == FIVE_FOURTEEN;
    }

    public boolean isFifteenToTwentyFour() {
        return this == FIFTEEN_TWENTY_FOUR;
    }

    public boolean isTwentyFiveToThirtyFour() {
        return this == TWENTY_FIVE_THIRTY_FOUR;
    }

    public boolean isThirtyFiveToFortyFour() {
        return this == THIRTY_FIVE_FORTY_FOUR;
    }

    public boolean isFortyFiveToFiftyFour() {
        return this == FORTY_FIVE_FIFTY_FOUR;
    }

    public boolean isFiftyFiveToSixtyFour() {
        return this == FIFTY_FIVE_SIXTY_FOUR;
    }

    public boolean isSixtyFiveToSeventyFour() {
        return this == SIXTY_FIVE_SEVENTY_FOUR;
    }

    public boolean isSeventyFiveToEightyFour() {
        return this == SEVENTY_FIVE_EIGHTY_FOUR;
    }

    public boolean isEightyFiveOrOlder() {
        return this == EIGHTY_FIVE_OR_OLDER;
    }
    //endregion Boolean Comparison Methods

    public static TenYearAgeRange determineAgeRange(final int age) {
        if (age > 84) {
            return EIGHTY_FIVE_OR_OLDER;
        } else if (age > 74) {
            return SEVENTY_FIVE_EIGHTY_FOUR;
        } else if (age > 64) {
            return SIXTY_FIVE_SEVENTY_FOUR;
        } else if (age > 54) {
            return FIFTY_FIVE_SIXTY_FOUR;
        } else if (age > 44) {
            return FORTY_FIVE_FIFTY_FOUR;
        } else if (age > 34) {
            return THIRTY_FIVE_FORTY_FOUR;
        } else if (age > 24) {
            return TWENTY_FIVE_THIRTY_FOUR;
        } else if (age > 14) {
            return FIFTEEN_TWENTY_FOUR;
        } else if (age > 4) {
            return FIVE_FOURTEEN;
        } else if (age > 0) {
            return ONE_FOUR;
        } else {
            return UNDER_ONE;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
