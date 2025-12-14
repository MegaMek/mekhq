/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.finances;

import static mekhq.campaign.enums.DailyReportType.FINANCES;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.enums.FinancialTerm;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An Asset is a non-core (i.e. not part of the core company) investment that a user can use to generate income on a
 * schedule. It can also be used increase loan collateral and thus get bigger loans.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @author Justin "Windchild" Bowen (modern version)
 */
public class Asset {
    private static final MMLogger logger = MMLogger.create(Asset.class);

    // region Variable Declarations
    private String name;
    private Money value;
    private FinancialTerm financialTerm;
    private Money income;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Finances",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Constructors
    public Asset() {
        setName("New Asset");
        setValue(Money.zero());
        setFinancialTerm(FinancialTerm.ANNUALLY);
        setIncome(Money.zero());
    }
    // endregion Constructors

    // region Getters/Setters
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

    public FinancialTerm getFinancialTerm() {
        return financialTerm;
    }

    public void setFinancialTerm(final FinancialTerm financialTerm) {
        this.financialTerm = financialTerm;
    }

    public Money getIncome() {
        return income;
    }

    public void setIncome(final Money income) {
        this.income = income;
    }
    // endregion Getters/Setters

    public void processNewDay(final Campaign campaign, final LocalDate yesterday,
          final LocalDate today, final Finances finances) {
        if (getFinancialTerm().endsToday(yesterday, today)) {
            finances.credit(TransactionType.MISCELLANEOUS, today, getIncome(),
                  String.format(resources.getString("AssetPayment.finances"), getName()));
            campaign.addReport(FINANCES, String.format(resources.getString("AssetPayment.report"),
                  getIncome().toAmountAndSymbolString(), getName()));
        }
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "asset");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", getName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "value", getValue());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "financialTerm", getFinancialTerm().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "income", getIncome());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "asset");
    }

    public static Asset generateInstanceFromXML(final Node wn) {
        final Asset asset = new Asset();
        final NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    asset.setName(MHQXMLUtility.unEscape(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("value")) {
                    asset.setValue(Money.fromXmlString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("financialTerm")) {
                    asset.setFinancialTerm(FinancialTerm.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("income")) {
                    asset.setIncome(Money.fromXmlString(wn2.getTextContent().trim()));
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        return asset;
    }
    // endregion File I/O
}
