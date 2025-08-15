/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum TenYearAgeRange {
    //region Enum Declarations
    UNDER_ONE("TenYearAgeRange.UNDER_ONE.text"),
    ONE_FOUR("TenYearAgeRange.ONE_FOUR.text"),
    FIVE_FOURTEEN("TenYearAgeRange.FIVE_FOURTEEN.text"),
    FIFTEEN_TWENTY_FOUR("TenYearAgeRange.FIFTEEN_TWENTY_FOUR.text"),
    TWENTY_FIVE_THIRTY_FOUR("TenYearAgeRange.TWENTY_FIVE_THIRTY_FOUR.text"),
    THIRTY_FIVE_FORTY_FOUR("TenYearAgeRange.THIRTY_FIVE_FORTY_FOUR.text"),
    FORTY_FIVE_FIFTY_FOUR("TenYearAgeRange.FORTY_FIVE_FIFTY_FOUR.text"),
    FIFTY_FIVE_SIXTY_FOUR("TenYearAgeRange.FIFTY_FIVE_SIXTY_FOUR.text"),
    SIXTY_FIVE_SEVENTY_FOUR("TenYearAgeRange.SIXTY_FIVE_SEVENTY_FOUR.text"),
    SEVENTY_FIVE_EIGHTY_FOUR("TenYearAgeRange.SEVENTY_FIVE_EIGHTY_FOUR.text"),
    EIGHTY_FIVE_OR_OLDER("TenYearAgeRange.EIGHTY_FIVE_OR_OLDER.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    //endregion Variable Declarations

    //region Constructors
    TenYearAgeRange(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    //endregion Constructors

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
