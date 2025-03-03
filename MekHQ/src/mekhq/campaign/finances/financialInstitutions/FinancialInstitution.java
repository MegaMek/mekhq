/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.finances.financialInstitutions;

import java.io.PrintWriter;
import java.time.LocalDate;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;

public class FinancialInstitution {
    private static final MMLogger logger = MMLogger.create(FinancialInstitution.class);

    // region Variable Declarations
    private String name;
    private LocalDate foundationDate;
    private LocalDate shutterDate;
    // endregion Variable Declarations

    // region Constructors
    public FinancialInstitution() {
        setName("");
        setFoundationDate(null);
        setShutterDate(null);
    }
    // endregion Constructors

    // region Getters/Setters
    public void setName(final String name) {
        this.name = name;
    }

    public @Nullable LocalDate getFoundationDate() {
        return foundationDate;
    }

    public void setFoundationDate(final @Nullable LocalDate foundationDate) {
        this.foundationDate = foundationDate;
    }

    public @Nullable LocalDate getShutterDate() {
        return shutterDate;
    }

    public void setShutterDate(final @Nullable LocalDate shutterDate) {
        this.shutterDate = shutterDate;
    }
    // endregion Getters/Setters

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "institution");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "name", toString());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "foundationDate", getFoundationDate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "shutterDate", getShutterDate());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "institution");
    }

    public static @Nullable FinancialInstitution parseFromXML(final NodeList nl) {
        final FinancialInstitution financialInstitution = new FinancialInstitution();
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                switch (wn.getNodeName()) {
                    case "name":
                        financialInstitution.setName(wn.getTextContent().trim());
                        break;
                    case "foundationDate":
                        financialInstitution.setFoundationDate(MHQXMLUtility.parseDate(wn.getTextContent().trim()));
                        break;
                    case "shutterDate":
                        financialInstitution.setShutterDate(MHQXMLUtility.parseDate(wn.getTextContent().trim()));
                        break;
                    default:
                        break;
                }
            }
            return financialInstitution;
        } catch (Exception ex) {
            logger.error("", ex);
            return null;
        }
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
