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
package mekhq.campaign.campaignOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorCountMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Node;

/**
 * Round-trip and migration tests for {@link StratConSectorCountMethod}.
 *
 * <p>The migration half matters more than the round trip: this option replaced two independent booleans, and every
 * campaign saved before it existed carries those instead. Reading them wrongly would silently change how many sectors
 * an existing campaign's next contract generates.</p>
 */
class StratConSectorCountMethodPersistenceTest {

    private static final Version VERSION = new Version("0.51.01");

    private static String marshal(CampaignOptions options) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            CampaignOptionsMarshaller.writeCampaignOptionsToXML(options, printWriter, 0);
        }
        return stringWriter.toString();
    }

    private static CampaignOptions unmarshal(String xml) throws Exception {
        Node node = DocumentBuilderFactory.newInstance()
                          .newDocumentBuilder()
                          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        return CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);
    }

    /** Wraps option tags in the element the unmarshaller expects, so a hand-written legacy save can be fed in. */
    private static String saveContaining(String tags) {
        return "<campaignOptions>" + tags + "</campaignOptions>";
    }

    @ParameterizedTest
    @EnumSource(StratConSectorCountMethod.class)
    void everyMethodSurvivesARoundTrip(StratConSectorCountMethod method) throws Exception {
        CampaignOptions options = new CampaignOptions();
        options.setStratConSectorCountMethod(method);

        assertEquals(method, unmarshal(marshal(options)).getStratConSectorCountMethod());
    }

    @Test
    void newCampaignsDefaultToCondensed() {
        // The two booleans this replaced both defaulted to true, which is what CONDENSED means. A different default
        // here would change sector counts for everyone starting a campaign.
        assertEquals(StratConSectorCountMethod.CONDENSED, new CampaignOptions().getStratConSectorCountMethod());
    }

    @ParameterizedTest
    @CsvSource({ "false, false, LEGACY", "true, false, ALTERNATE", "true, true, CONDENSED", "false, true, CONDENSED" })
    void legacyBooleanPairMigratesToTheEquivalentMethod(boolean alternateCount, boolean condenseSectors,
          StratConSectorCountMethod expected) throws Exception {
        String save = saveContaining("<useStratConAlternateSectorCount>" +
                                           alternateCount +
                                           "</useStratConAlternateSectorCount>" +
                                           "<useStratConCondenseSectors>" +
                                           condenseSectors +
                                           "</useStratConCondenseSectors>");

        assertEquals(expected, unmarshal(save).getStratConSectorCountMethod());
    }

    @Test
    void legacyBooleanPairMigratesRegardlessOfTagOrder() throws Exception {
        // The unmarshaller handles one node at a time in document order, so the migration has to converge whichever
        // of the two it sees first.
        String condenseFirst = saveContaining("<useStratConCondenseSectors>false</useStratConCondenseSectors>" +
                                                    "<useStratConAlternateSectorCount>true" +
                                                    "</useStratConAlternateSectorCount>");

        assertEquals(StratConSectorCountMethod.ALTERNATE, unmarshal(condenseFirst).getStratConSectorCountMethod());
    }

    @Test
    void anExplicitMethodIsNotOverwrittenByLegacyTags() throws Exception {
        // Defensive: a save should never carry both, but if one did, the newer tag is the authoritative one.
        String both = saveContaining("<stratConSectorCountMethod>REGIMENTAL</stratConSectorCountMethod>" +
                                           "<useStratConAlternateSectorCount>false</useStratConAlternateSectorCount>" +
                                           "<useStratConCondenseSectors>false</useStratConCondenseSectors>");

        assertEquals(StratConSectorCountMethod.REGIMENTAL, unmarshal(both).getStratConSectorCountMethod());
    }

    @Test
    void anUnrecognizedMethodFallsBackToCondensed() {
        assertEquals(StratConSectorCountMethod.CONDENSED, StratConSectorCountMethod.fromLookupName("NOT_A_METHOD"));
    }
}
