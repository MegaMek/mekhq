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

import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.market.enums.UnitMarketType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
public class UnitMarketOffer {
    //region Variable Declarations
    private UnitMarketType marketType;
    private int unitType;
    private MechSummary unit;
    private int percent;
    //endregion Variable Declarations

    //region Constructors
    private UnitMarketOffer() {

    }

    public UnitMarketOffer(final UnitMarketType marketType, final int unitType,
                           final MechSummary unit, final int percent) {
        setMarketType(marketType);
        setUnitType(unitType);
        setUnit(unit);
        setPercent(percent);
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

    public MechSummary getUnit() {
        return unit;
    }

    public void setUnit(final MechSummary unit) {
        this.unit = unit;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(final int percent) {
        this.percent = percent;
    }
    //endregion Getters/Setters

    //region File I/O
    public void writeToXML(final PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "offer");
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "market", getMarketType().name());
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "unitType", getUnitType());
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "unit", getUnit().getName());
        MekHqXmlUtil.writeSimpleXMLTag(pw1, indent, "percent", getPercent());
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "offer");
    }

    public static UnitMarketOffer generateInstanceFromXML(final Node wn) {
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
                    retVal.setUnit(MechSummaryCache.getInstance().getMech(wn3.getTextContent().trim()));
                } else if (wn3.getNodeName().equalsIgnoreCase("pct") // Legacy, 0.49.X removal
                        || wn3.getNodeName().equalsIgnoreCase("percent")) {
                    retVal.setPercent(Integer.parseInt(wn3.getTextContent().trim()));
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }

        return retVal;
    }
    //endregion File I/O
}
