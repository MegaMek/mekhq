/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.unitMarket;

import megamek.Version;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.enums.UnitMarketType;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

public class UnitMarketOffer {
    //region Variable Declarations
    private UnitMarketType marketType;
    private int unitType;
    private MekSummary unit;
    private int percent;
    private int transitDuration;
    //endregion Variable Declarations

    //region Constructors
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
    //endregion Constructors

    //region Getters/Setters
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
    //endregion Getters/Setters

    /**
     * @return the Entity offered in this UnitMarketOffer
     */
    public @Nullable Entity getEntity() {
        try {
            return new MekFileParser(getUnit().getSourceFile(), getUnit().getEntryName()).getEntity();
        } catch (Exception e) {
            LogManager.getLogger().error("Unable to load entity: " + getUnit().getSourceFile()
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

    //region File I/O
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
                        LogManager.getLogger().error("Failed to find unit with name " + unitName + ", removing the offer from the market.");
                        return null;
                    }
                } else if (wn3.getNodeName().equalsIgnoreCase("pct") // Legacy, 0.49.3 removal
                        || wn3.getNodeName().equalsIgnoreCase("percent")) {
                    retVal.setPercent(Integer.parseInt(wn3.getTextContent().trim()));
                } else if (wn3.getNodeName().equalsIgnoreCase("transitDuration")) {
                    retVal.setTransitDuration(Integer.parseInt(wn3.getTextContent().trim()));
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return null;
        }

        if (version.isLowerThan("0.49.3")) {
            retVal.setTransitDuration(campaign.getCampaignOptions().isInstantUnitMarketDelivery()
                    ? 0 : campaign.calculatePartTransitTime(Compute.d6(2) - 2));
        }

        return retVal;
    }
    //endregion File I/O
}
