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

import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FinancialInstitutionTest {

    //region File I/O
    @Test
    public void testWriteToXML() throws IOException {
        final FinancialInstitution financialInstitution = new FinancialInstitution();
        financialInstitution.setName("Johnstone Banking Inc.");
        financialInstitution.setFoundationDate(LocalDate.of(3025, 1, 1));
        financialInstitution.setShutterDate(LocalDate.of(3025, 6, 1));

        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            financialInstitution.writeToXML(pw, 0);

            // Assert the written XML equals to the expected text, ignoring line ending differences
            assertEquals("<institution>\t<name>Johnstone Banking Inc.</name>\t<foundationDate>3025-01-01</foundationDate>\t<shutterDate>3025-06-01</shutterDate></institution>",
                    sw.toString().replaceAll("\\n|\\r\\n", ""));
        }
    }

    @Test
    public void testGenerateInstanceFromXML() throws Exception {
        final String text = "<institution>\n\t<name>Johnstone Banking Inc.</name>\n\t<foundationDate>3025-01-01</foundationDate>\n\t<shutterDate>3025-06-01</shutterDate>\n</institution>\n";

        final Document document;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            document = MHQXMLUtility.newSafeDocumentBuilder().parse(bais);
        }

        final Element element = document.getDocumentElement();
        element.normalize();

        assertTrue(element.hasChildNodes());
        final FinancialInstitution financialInstitution = FinancialInstitution.parseFromXML(element.getChildNodes());
        assertNotNull(financialInstitution);
        assertEquals("Johnstone Banking Inc.", financialInstitution.toString());
        assertEquals(LocalDate.of(3025, 1, 1), financialInstitution.getFoundationDate());
        assertEquals(LocalDate.of(3025, 6, 1), financialInstitution.getShutterDate());
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        final FinancialInstitution financialInstitutionA = new FinancialInstitution();
        financialInstitutionA.setName("Institution A");

        final FinancialInstitution financialInstitutionB = new FinancialInstitution();
        financialInstitutionB.setName("Institution B");

        assertEquals("Institution A", financialInstitutionA.toString());
        assertNotEquals("Institution B", financialInstitutionA.toString());
        assertNotEquals("Institution A", financialInstitutionB.toString());
    }
}
