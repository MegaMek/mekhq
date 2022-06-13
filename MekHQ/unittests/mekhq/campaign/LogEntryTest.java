/*
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import mekhq.campaign.log.*;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LogEntryTest {
    @Test
    public void testNullDescriptionBecomesEmpty() {
        assertEquals("", new HistoricalLogEntry(null, null).getDesc());
    }

    @Test
    public void testXmlMarshalling() throws Exception {
        checkMarshalling(new PersonalLogEntry(null, null));
        checkMarshalling(new AwardLogEntry(LocalDate.ofYearDay(1, 1), ""));
        checkMarshalling(new CustomLogEntry(LocalDate.ofYearDay(1, 1), "Description"));
        checkMarshalling(new ServiceLogEntry(LocalDate.ofYearDay(1, 1), "<desc>Some description</desc>"));
        checkMarshalling(new MedicalLogEntry(LocalDate.ofYearDay(1, 1), "Some <em>xml-fragment</em> description"));
    }

    private static void checkMarshalling(LogEntry le) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (PrintWriter pw = new PrintWriter(baos)) {
                le.writeToXML(pw, 0);
            }

            Node node;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
                node = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(new InputSource(bais))
                        .getDocumentElement();
            }

            assertEquals(le, LogEntryFactory.getInstance().generateInstanceFromXML(node));
        }
    }
}
