/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.dailyReportLog;

import static mekhq.campaign.dailyReportLog.DailyReportLog.REPORT_LINEBREAK;
import static mekhq.campaign.enums.DailyReportType.AGGREGATE;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.campaign.enums.DailyReportType;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

class DailyReportLogTest {
    @Test
    void newLogStartsEmpty() {
        DailyReportLog log = new DailyReportLog();
        for (DailyReportType type : DailyReportType.values()) {
            assertTrue(log.getLines(type).isEmpty());
            assertEquals("", log.getHtml(type));
            assertTrue(log.fetchAndClearNew(type).isEmpty());
        }
    }

    @Test
    void addBuildsLinesHtmlAndDelta() {
        DailyReportLog log = new DailyReportLog();
        log.add(GENERAL, "first");
        log.add(GENERAL, "second");

        assertEquals(List.of("first", "second"), log.getLines(GENERAL));
        assertEquals("first" + REPORT_LINEBREAK + "second", log.getHtml(GENERAL));
    }

    @Test
    void addTargetsOnlyTheGivenChannel() {
        DailyReportLog log = new DailyReportLog();
        log.add(GENERAL, "general only");

        assertEquals(List.of("general only"), log.getLines(GENERAL));
        assertTrue(log.getLines(BATTLE).isEmpty());
    }

    @Test
    void fetchAndClearNewReturnsDeltaThenResets() {
        DailyReportLog log = new DailyReportLog();
        log.add(GENERAL, "a");
        log.add(GENERAL, "b");

        assertEquals(List.of("a", REPORT_LINEBREAK, "b"), log.fetchAndClearNew(GENERAL));
        assertTrue(log.fetchAndClearNew(GENERAL).isEmpty());

        log.add(GENERAL, "c");
        assertEquals(List.of(REPORT_LINEBREAK, "c"), log.fetchAndClearNew(GENERAL));
    }

    @Test
    void clearResetsEveryChannel() {
        DailyReportLog log = new DailyReportLog();
        log.add(GENERAL, "x");
        log.add(BATTLE, "y");

        log.clear();

        assertTrue(log.getLines(GENERAL).isEmpty());
        assertTrue(log.getLines(BATTLE).isEmpty());
        assertEquals("", log.getHtml(GENERAL));
    }

    @Test
    void beginNewDayAppendsToEveryChannel() {
        DailyReportLog log = new DailyReportLog();
        log.beginNewDay("<b>Day</b>");

        for (DailyReportType type : DailyReportType.values()) {
            assertEquals(List.of("<b>Day</b>"), log.getLines(type));
        }
    }

    @Test
    void xmlRoundTripPreservesPersistedChannels() throws Exception {
        DailyReportLog original = new DailyReportLog();
        original.add(GENERAL, "general line");
        original.add(BATTLE, "battle line with &amp; and <em>markup</em>");

        DailyReportLog restored = roundTrip(original);

        assertEquals(List.of("general line"), restored.getLines(GENERAL));
        assertEquals(original.getLines(BATTLE), restored.getLines(BATTLE));
        // HTML cache is rebuilt on load
        assertEquals(original.getHtml(BATTLE), restored.getHtml(BATTLE));
    }

    @Test
    void aggregateIsNotPersisted() throws Exception {
        DailyReportLog original = new DailyReportLog();
        original.add(AGGREGATE, "aggregate line");

        DailyReportLog restored = roundTrip(original);

        assertTrue(restored.getLines(AGGREGATE).isEmpty());
    }

    private static DailyReportLog roundTrip(DailyReportLog log) throws Exception {
        byte[] xml;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            try (PrintWriter pw = new PrintWriter(baos)) {
                log.writeToXML(pw, 0);
            }
            xml = baos.toByteArray();
        }

        Node node;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(xml)) {
            node = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                         .parse(new InputSource(bais))
                         .getDocumentElement();
        }

        DailyReportLog restored = new DailyReportLog();
        restored.readFromXML(node);
        return restored;
    }
}
