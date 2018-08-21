/*
 * Copyright (c) 2018 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class LogEntryTest {

    @Test
    public void testNullDescriptionBecomesEmpty() {
        Assert.assertEquals("", new LogEntry(null, null).getDesc()); //$NON-NLS-1$
    }

    @Test
    public void testXmlMarshalling() throws Exception {
        checkMarshalling(new LogEntry(null, null, null));
        checkMarshalling(new LogEntry(new Date(0l), "", ""));  //$NON-NLS-1$//$NON-NLS-2$
        checkMarshalling(new LogEntry(new Date(0l), "<desc>Some description</desc>", "<type>Some type</type>")); //$NON-NLS-1$ //$NON-NLS-2$
        checkMarshalling(new LogEntry(new Date(0l), "Some <em>xml-fragment</em> description", "Some <em>xml-fragment</em> type")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Test
    public void testDatesMarsalledIn24HFormat() throws Exception {
        // The date format in LogEntry used to be yyyy-MM-dd hh:mm:ss (with lowercase hh), which
        // caused pm times (eg: 14:00 / 2pm) to be converted to am ones during marshalling.
        // This test is to ensure we have no regressions.
        Instant todayAt2Pm = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).withHour(14).toInstant();
        checkMarshalling(new LogEntry(new Date(todayAt2Pm.toEpochMilli()), "", ""));  //$NON-NLS-1$//$NON-NLS-2$
    }

    private static void checkMarshalling(LogEntry le) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PrintWriter pw = new PrintWriter(baos);
        le.writeToXml(pw, 0);
        pw.close();

        Node node = DocumentBuilderFactory.newInstance()
                                          .newDocumentBuilder()
                                          .parse(new InputSource(new ByteArrayInputStream(baos.toByteArray())))
                                          .getDocumentElement();
        
        Assert.assertEquals(le, LogEntry.generateInstanceFromXML(node));
    }

}
