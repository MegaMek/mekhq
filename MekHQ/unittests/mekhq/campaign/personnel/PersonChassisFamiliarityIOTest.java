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
package mekhq.campaign.personnel;

import static mekhq.campaign.personnel.familiarity.Familiarity.FAMILIARITY_THREE_HUNDRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Round-trip and read-side coverage for {@link Person}'s {@code chassisFamiliarity} map: the values written by
 * {@link Person#writeToXML} survive {@link Person#generateInstanceFromXML}, and the parser applies its filters (drops
 * non-positive values, clamps to the 300 hard cap).
 */
class PersonChassisFamiliarityIOTest {
    private static Person parsePerson(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        Campaign campaign = mockCampaign();
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn("MERC");
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 1));
        when(campaign.getVersion()).thenReturn(new Version("0.51.1"));

        return Person.generateInstanceFromXML(doc.getDocumentElement(), campaign, new Version("0.51.1"));
    }

    @Test
    void testWriteReadRoundTrip() throws Exception {
        // Build and serialize against the same campaign so getRankSystem() is non-null and matches the player force's
        // (the writer only emits a rankSystem override when they differ).
        Campaign campaign = mockCampaign();
        Person person = new Person("Given", "Sur", campaign, "MERC");
        person.setChassisFamiliarity("Atlas", 150);
        person.setChassisFamiliarity("Locust", 25);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        person.writeToXML(pw, 0, campaign);
        pw.flush();
        String xml = sw.toString();

        // Feed the writer's own <chassisFamiliarity> block straight back through the reader. Parsing the entire
        // serialized person would depend on unrelated fields surviving a mocked campaign; isolating the block keeps this
        // a focused test of the writer<->reader contract for this field.
        String open = "<chassisFamiliarity>";
        String close = "</chassisFamiliarity>";
        int start = xml.indexOf(open);
        int end = xml.indexOf(close) + close.length();
        assertTrue(start >= 0 && end > start, "writer should emit a chassisFamiliarity block");
        String block = xml.substring(start, end);

        Person loaded = parsePerson("<person><givenName>x</givenName>" + block + "</person>");
        assertEquals(150, loaded.getChassisFamiliarity("Atlas"));
        assertEquals(25, loaded.getChassisFamiliarity("Locust"));
        assertEquals(0, loaded.getChassisFamiliarity("Phoenix Hawk"), "chassis never recorded should read as 0");
    }

    @Test
    void testParseDropsNonPositiveAndClampsToCap() throws Exception {
        String xml = """
              <person>
                <givenName>Reader</givenName>
                <surname>Test</surname>
                <chassisFamiliarity>
                  <familiarity><chassis>Atlas</chassis><value>200</value></familiarity>
                  <familiarity><chassis>Overcap</chassis><value>9999</value></familiarity>
                  <familiarity><chassis>Zeroed</chassis><value>0</value></familiarity>
                  <familiarity><chassis>Negative</chassis><value>-40</value></familiarity>
                </chassisFamiliarity>
              </person>
              """;

        Person loaded = parsePerson(xml);
        assertEquals(200, loaded.getChassisFamiliarity("Atlas"));
        assertEquals(FAMILIARITY_THREE_HUNDRED, loaded.getChassisFamiliarity("Overcap"),
              "values above the 300 hard cap are clamped on load");
        assertEquals(0, loaded.getChassisFamiliarity("Zeroed"), "a zero value is not recorded");
        assertEquals(0, loaded.getChassisFamiliarity("Negative"), "a negative value is not recorded");
        assertFalse(loaded.getChassisFamiliarity().containsKey("Zeroed"));
    }
}
