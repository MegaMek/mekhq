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
package mekhq.campaign.market.unitMarket;

import java.io.PrintWriter;

import megamek.Version;
import megamek.common.units.Entity;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UnitMarketOffer {
    private static final MMLogger logger = MMLogger.create(UnitMarketOffer.class);
    // region Variable Declarations
    private UnitMarketType marketType;
    private int unitType;
    private MekSummary unit;
    private int percent;
    private int transitDuration;
    // endregion Variable Declarations

    // region Constructors
    private UnitMarketOffer() {

    }

    public UnitMarketOffer(final UnitMarketType marketType, final int unitType,
          final MekSummary unit, final int percent, final int transitDuration) {
        setMarketType(marketType);
        setUnitType(unitType);
        setUnit(unit);
        setPercent(percent);
        setTransitDuration(transitDuration);
    }
    // endregion Constructors

    // region Getters/Setters
    public UnitMarketType getMarketType() {
        return marketType;
    }

    public void setMarketType(final UnitMarketType marketType) {
        this.marketType = marketType;
    }

    public int getUnitType() {
        return unitType;
    }

    public void setUnitType(final int unitType) {
        this.unitType = unitType;
    }

    public MekSummary getUnit() {
        return unit;
    }

    public void setUnit(final MekSummary unit) {
        this.unit = unit;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(final int percent) {
        this.percent = percent;
    }

    public int getTransitDuration() {
        return transitDuration;
    }

    public void setTransitDuration(final int transitDuration) {
        this.transitDuration = transitDuration;
    }
    // endregion Getters/Setters

    /**
     * @return the Entity offered in this UnitMarketOffer
     */
    public @Nullable Entity getEntity() {
        try {
            return new MekFileParser(getUnit().getSourceFile(), getUnit().getEntryName()).getEntity();
        } catch (Exception e) {
            logger.error("Unable to load entity: " + getUnit().getSourceFile()
                               + ": " + getUnit().getEntryName() + ". Returning null.", e);
            return null;
        }
    }

    /**
     * @return the final price of this Offer
     */
    public Money getPrice() {
        return Money.of((double) getUnit().getCost()).multipliedBy(getPercent()).dividedBy(100);
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "offer");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "market", getMarketType().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitType", getUnitType());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unit", getUnit().getName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "percent", getPercent());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transitDuration", getTransitDuration());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "offer");
    }

    public static @Nullable UnitMarketOffer generateInstanceFromXML(final Node wn,
          final Campaign campaign,
          final Version version) {
        UnitMarketOffer retVal = new UnitMarketOffer();
        NodeList nl = wn.getChildNodes();

        try {
            for (int i = 0; i < nl.getLength(); i++) {
                Node wn3 = nl.item(i);
                if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                if (wn3.getNodeName().equalsIgnoreCase("market")) {
                    retVal.setMarketType(UnitMarketType.parseFromString(wn3.getTextContent().trim()));
                } else if (wn3.getNodeName().equalsIgnoreCase("unitType")) {
                    retVal.setUnitType(Integer.parseInt(wn3.getTextContent().trim()));
                } else if (wn3.getNodeName().equalsIgnoreCase("unit")) {
                    final String unitName = wn3.getTextContent().trim();
                    retVal.setUnit(MekSummaryCache.getInstance().getMek(unitName));
                    if (retVal.getUnit() == null) {
                        logger.error(
                              "Failed to find unit with name " + unitName + ", removing the offer from the market.");
                        return null;
                    }
                } else if (wn3.getNodeName().equalsIgnoreCase("percent")) {
                    retVal.setPercent(Integer.parseInt(wn3.getTextContent().trim()));
                } else if (wn3.getNodeName().equalsIgnoreCase("transitDuration")) {
                    retVal.setTransitDuration(Integer.parseInt(wn3.getTextContent().trim()));
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }

        return retVal;
    }
    // endregion File I/O
}
