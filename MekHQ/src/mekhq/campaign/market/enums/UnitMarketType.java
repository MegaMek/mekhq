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

import megamek.common.Compute;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import org.apache.logging.log4j.LogManager;

import java.util.HashMap;
import java.util.ResourceBundle;

public enum UnitMarketType {
    //region Enum Declarations
    OPEN("UnitMarketType.OPEN.text"),
    EMPLOYER("UnitMarketType.EMPLOYER.text"),
    MERCENARY("UnitMarketType.MERCENARY.text"),
    FACTORY("UnitMarketType.FACTORY.text"),
    BLACK_MARKET("UnitMarketType.BLACK_MARKET.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    //endregion Variable Declarations

    //region Constructors
    UnitMarketType(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Market",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
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
    //endregion Boolean Comparison Methods

    //region File I/O
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

        LogManager.getLogger().error("Unable to parse " + text + " into a UnitMarketType. Returning OPEN.");
        return OPEN;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }



    /**
     * Returns the quality of a unit based on the given market type.
     *
     * @param market the type of market
     * @return the quality of the unit
     */
    public static int getQuality(Campaign campaign, UnitMarketType market) {
        HashMap<String, Integer> qualityAndModifier = new HashMap<>();

        switch(market) {
            case OPEN:
            case MERCENARY:
                qualityAndModifier.put("quality", Part.QUALITY_C);
                qualityAndModifier.put("modifier", 0);
                break;
            case EMPLOYER:
                qualityAndModifier.put("quality", Part.QUALITY_B);
                qualityAndModifier.put("modifier", -1);
                break;
            case BLACK_MARKET:
                if (Compute.d6(1) <= 2) {
                    qualityAndModifier.put("quality", Part.QUALITY_A);
                    // this is to force a result of 0 (A)
                    qualityAndModifier.put("modifier", -12);
                } else {
                    qualityAndModifier.put("quality", Part.QUALITY_F);
                    // this is to force a result of 5 (F)
                    qualityAndModifier.put("modifier", 12);
                }
                break;
            case FACTORY:
                qualityAndModifier.put("quality", Part.QUALITY_F);
                // this is to force a result of 5 (F)
                qualityAndModifier.put("modifier", 12);
                break;
        }

        if (campaign.getCampaignOptions().isUseRandomUnitQualities()) {
            return Unit.getRandomUnitQuality(qualityAndModifier.get("modifier"));
        } else {
            return qualityAndModifier.get("quality");
        }
    }
}
