/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.enums;

import java.util.ResourceBundle;

import megamek.common.compute.Compute;
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
    BLACK_MARKET("UnitMarketType.BLACK_MARKET.text"),
    CIVILIAN("UnitMarketType.CIVILIAN.text");
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

    public boolean isCivilianMarket() {
        return this == CIVILIAN;
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
                case 5:
                    return CIVILIAN;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(UnitMarketType.class)
              .error("Unable to parse {} into a UnitMarketType. Returning OPEN.", text);
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
     * @param modifier the modifier to adjust the price (a negative modifier decreases price, positive increases price)
     *
     * @return the calculated price
     *
     * @throws IllegalStateException if the roll value is unexpected
     */
    public static int getPricePercentage(int modifier) {
        int roll = Compute.d6(2);
        int value = switch (roll) {
            case 2 -> modifier + 3;
            case 3 -> modifier + 2;
            case 4, 5 -> modifier + 1;
            case 6, 7, 8 -> modifier;
            case 9, 10 -> modifier - 1;
            case 11 -> modifier - 2;
            case 12 -> modifier - 3;
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/market/unitMarket/AtBMonthlyUnitMarket.java/getPrice: "
                        + roll);
        };

        return 100 + (value * 5);
    }

    /**
     * Returns the quality of a unit based on the given market type.
     *
     * @param market the type of market
     *
     * @return the quality of the unit
     */
    public static PartQuality getQuality(Campaign campaign, UnitMarketType market) {

        if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
            return Unit.getRandomUnitQuality(switch (market) {
                case OPEN, MERCENARY -> 0;
                case EMPLOYER -> -1;
                case BLACK_MARKET -> Compute.d6(1) <= 2 ? -12 : 12; // forces A/F
                case FACTORY, CIVILIAN -> 12; // Forces F
            });
        } else {
            return switch (market) {
                case OPEN, MERCENARY -> PartQuality.QUALITY_C;
                case EMPLOYER -> PartQuality.QUALITY_B;
                case BLACK_MARKET -> Compute.d6(1) <= 2 ? PartQuality.QUALITY_A : PartQuality.QUALITY_F;
                case FACTORY, CIVILIAN -> PartQuality.QUALITY_F;
            };
        }
    }
}
