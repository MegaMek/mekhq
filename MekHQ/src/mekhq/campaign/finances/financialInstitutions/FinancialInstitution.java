/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.finances.financialInstitutions;

import java.io.PrintWriter;
import java.time.LocalDate;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class FinancialInstitution {
    private static final MMLogger LOGGER = MMLogger.create(FinancialInstitution.class);

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
            LOGGER.error("", ex);
            return null;
        }
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
