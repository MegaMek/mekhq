/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.log.AwardLogEntry;
import mekhq.campaign.log.CustomLogEntry;
import mekhq.campaign.log.HistoricalLogEntry;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.LogEntryFactory;
import mekhq.campaign.log.MedicalLogEntry;
import mekhq.campaign.log.PersonalLogEntry;
import mekhq.campaign.log.ServiceLogEntry;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

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
