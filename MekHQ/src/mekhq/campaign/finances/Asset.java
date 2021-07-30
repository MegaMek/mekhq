/*
 * Asset.java
 *
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All Rights Reserved.
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
package mekhq.campaign.finances;

import java.io.PrintWriter;
import java.io.Serializable;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An Asset is a non-core (i.e. not part of the core company) investment that a user can use to
 * generate income on a schedule. It can also be used increase loan collateral and thus get bigger
 * loans.
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 * @author Windchild (modern version)
 */
public class Asset implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -7071958800358172014L;

    private String name;
    private Money value;
    private int financialTerm;
    private Money income;
    //endregion Variable Declarations

    //region Constructors
    public Asset() {
        setName("New Asset");
        setValue(Money.zero());
        setFinancialTerm(Finances.SCHEDULE_YEARLY);
        setIncome(Money.zero());
    }
    //endregion Constructors

    //region Getters/Setters
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Money getValue() {
        return value;
    }

    public void setValue(final Money value) {
        this.value = value;
    }

    public int getFinancialTerm() {
        return financialTerm;
    }

    public void setFinancialTerm(final int financialTerm) {
        this.financialTerm = financialTerm;
    }

    public Money getIncome() {
        return income;
    }

    public void setIncome(final Money income) {
        this.income = income;
    }
    //region Getters/Setters

    //region File I/O
    public void writeToXml(final PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "asset");
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "name", name);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "value", value.toXmlString());
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "financialTerm", financialTerm);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "income", income.toXmlString());
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "asset");
    }

    public static Asset generateInstanceFromXML(final Node wn) {
        final Asset retVal = new Asset();
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.setName(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("value")) {
                    retVal.setValue(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("financialTerm")) {
                    retVal.setFinancialTerm(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("income")) {
                    retVal.setIncome(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("schedule")) { // Legacy - 0.49.3 Removal
                    retVal.setFinancialTerm(Integer.parseInt(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
        return retVal;
    }
    //endregion File I/O
}
