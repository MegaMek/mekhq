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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.utilities.MHQXMLUtility;

public class FinancialInstitutions {
    private static final MMLogger logger = MMLogger.create(FinancialInstitutions.class);

    // region Variable Declarations
    private static final List<FinancialInstitution> financialInstitutions = new ArrayList<>();
    // endregion Variable Declarations

    // region Constructors
    private FinancialInstitutions() {
        // This Class should never be constructed
    }
    // endregion Constructors

    // region Getters
    public static List<FinancialInstitution> getFinancialInstitutions() {
        return financialInstitutions;
    }
    // endregion Getters

    /**
     * @param today the day to generate a financial institution on
     * @return a random financial institution founded before today that has not been
     *         shuttered
     */
    public static FinancialInstitution randomFinancialInstitution(final LocalDate today) {
        return ObjectUtility.getRandomItem(getFinancialInstitutions().stream()
                .filter(financialInstitution -> ((financialInstitution.getFoundationDate() == null)
                        || financialInstitution.getFoundationDate().isBefore(today))
                        && ((financialInstitution.getShutterDate() == null)
                                || financialInstitution.getShutterDate().isAfter(today)))
                .collect(Collectors.toList()));
    }

    // region File I/O
    public static void initializeFinancialInstitutions() {
        getFinancialInstitutions().clear();
        getFinancialInstitutions()
                .addAll(loadFinancialInstitutionsFromFile(new File(MHQConstants.FINANCIAL_INSTITUTIONS_FILE_PATH)));
        getFinancialInstitutions().addAll(
                loadFinancialInstitutionsFromFile(new File(MHQConstants.USER_FINANCIAL_INSTITUTIONS_FILE_PATH)));
    }

    public static List<FinancialInstitution> loadFinancialInstitutionsFromFile(final @Nullable File file) {
        if (file == null) {
            return new ArrayList<>();
        }

        final Document xmlDoc;

        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MHQXMLUtility.newSafeDocumentBuilder().parse(is);
        } catch (Exception ex) {
            logger.error("", ex);
            return new ArrayList<>();
        }

        final Element element = xmlDoc.getDocumentElement();
        element.normalize();
        final NodeList nl = element.getChildNodes();
        return IntStream.range(0, nl.getLength())
                .mapToObj(nl::item)
                .filter(wn -> "institution".equals(wn.getNodeName()))
                .map(wn -> FinancialInstitution.parseFromXML(wn.getChildNodes()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    // endregion File I/O
}
