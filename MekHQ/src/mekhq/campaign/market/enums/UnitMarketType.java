/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.enums;

import java.util.ResourceBundle;

import megamek.common.Compute;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;

public enum UnitMarketType {
    // region Enum Declarations
    OPEN("UnitMarketType.OPEN.text"),
    EMPLOYER("UnitMarketType.EMPLOYER.text"),
    MERCENARY("UnitMarketType.MERCENARY.text"),
    FACTORY("UnitMarketType.FACTORY.text"),
    BLACK_MARKET("UnitMarketType.BLACK_MARKET.text");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    UnitMarketType(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparison Methods
    public boolean isOpen() {
        return this == OPEN;
    }

    public boolean isEmployer() {
        return this == EMPLOYER;
    }

    public boolean isMercenary() {
        return this == MERCENARY;
    }

    public boolean isFactory() {
        return this == FACTORY;
    }

    public boolean isBlackMarket() {
        return this == BLACK_MARKET;
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    public static UnitMarketType parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return OPEN;
                case 1:
                    return EMPLOYER;
                case 2:
                    return MERCENARY;
                case 3:
                    return FACTORY;
                case 4:
                    return BLACK_MARKET;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(UnitMarketType.class)
                .error("Unable to parse " + text + " into a UnitMarketType. Returning OPEN.");
        return OPEN;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }

    /**
     * Calculates the price percentage based on a given modifier and d6 roll.
     *
     * @param modifier the modifier to adjust the price (a negative modifier
     *                 decreases price, positive increases price)
     * @return the calculated price
     * @throws IllegalStateException if the roll value is unexpected
     */
    public static int getPricePercentage(int modifier) {
        int roll = Compute.d6(2);
        int value;

        switch (roll) {
            case 2:
                value = modifier + 3;
                break;
            case 3:
                value = modifier + 2;
                break;
            case 4:
            case 5:
                value = modifier + 1;
                break;
            case 6:
            case 7:
            case 8:
                value = modifier;
                break;
            case 9:
            case 10:
                value = modifier - 1;
                break;
            case 11:
                value = modifier - 2;
                break;
            case 12:
                value = modifier - 3;
                break;
            default:
                throw new IllegalStateException(
                        "Unexpected value in mekhq/campaign/market/unitMarket/AtBMonthlyUnitMarket.java/getPrice: "
                                + roll);
        }

        return 100 + (value * 5);
    }

    /**
     * Returns the quality of a unit based on the given market type.
     *
     * @param market the type of market
     * @return the quality of the unit
     */
    public static PartQuality getQuality(Campaign campaign, UnitMarketType market) {

        if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
            return Unit.getRandomUnitQuality(switch(market) {
                case OPEN, MERCENARY -> 0;
                case EMPLOYER -> -1;
                case BLACK_MARKET -> Compute.d6(1) <= 2 ? -12 : 12; // forces A/F
                case FACTORY -> 12; // Forces F
            });
        } else {
            return switch(market) {
                case OPEN, MERCENARY -> PartQuality.QUALITY_C;
                case EMPLOYER -> PartQuality.QUALITY_B;
                case BLACK_MARKET -> Compute.d6(1) <= 2 ? PartQuality.QUALITY_A : PartQuality.QUALITY_F;
                case FACTORY -> PartQuality.QUALITY_F;
            };
        }
    }
}
