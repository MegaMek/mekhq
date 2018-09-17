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
import java.util.Date;

import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.log.*;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class LogEntryTest {

    @Test
    public void testNullDescriptionBecomesEmpty() {
        Assert.assertEquals("", new HistoricalLogEntry(null, null).getDesc()); //$NON-NLS-1$
    }

    @Test
    public void testXmlMarshalling() throws Exception {
        checkMarshalling(new PersonalLogEntry(null, null));
        checkMarshalling(new AwardLogEntry(new Date(0l), ""));  //$NON-NLS-1$//$NON-NLS-2$
        checkMarshalling(new CustomLogEntry(new Date(0l), "Description"));  //$NON-NLS-1$//$NON-NLS-2$
        checkMarshalling(new ServiceLogEntry(new Date(0l), "<desc>Some description</desc>")); //$NON-NLS-1$ //$NON-NLS-2$
        checkMarshalling(new MedicalLogEntry(new Date(0l), "Some <em>xml-fragment</em> description")); //$NON-NLS-1$ //$NON-NLS-2$
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
        
        Assert.assertEquals(le, LogEntryFactory.getInstance().generateInstanceFromXML(node));
    }

}
