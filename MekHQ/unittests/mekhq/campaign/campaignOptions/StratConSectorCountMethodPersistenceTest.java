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
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Node;

/**
 * Round-trip and default tests for {@link StratConSectorCountMethod}.
 *
 * <p>The default matters as much as the round trip: no released version ever wrote this tag, so every existing
 * campaign takes the declared default on load. The briefly-used boolean pair from unreleased branch builds is
 * deliberately ignored rather than migrated.</p>
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
        options.set(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD, method);

        assertEquals(method, unmarshal(marshal(options)).get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD));
    }

    @Test
    void campaignsDefaultToRegimental() {
        // The chosen default for new campaigns AND for every save from before this option existed, since absent tags
        // fall back to the declared default. Changing it changes sector counts for everyone.
        assertEquals(StratConSectorCountMethod.ALTERNATE_REGIMENTAL,
              new CampaignOptions().get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD));
    }

    @Test
    void oldSectorCountBooleansAreIgnoredNotMigrated() {
        // These tags were written only by unreleased builds of this branch, where the sector count was briefly a pair
        // of booleans. They are deliberately not migrated: such a save takes the modern default, whatever the booleans
        // said. If this fails because the tags produced LEGACY or ALTERNATE, someone has reintroduced migration.
        String save = saveContaining("<useStratConAlternateSectorCount>false</useStratConAlternateSectorCount>" +
                                           "<useStratConCondenseSectors>false</useStratConCondenseSectors>");

        try {
            assertEquals(StratConSectorCountMethod.ALTERNATE_REGIMENTAL,
                  unmarshal(save).get(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void anUnrecognizedMethodFallsBackToTheDefault() {
        // Must agree with the declared default: an unreadable tag and an absent one should land in the same place.
        assertEquals(StratConSectorCountMethod.ALTERNATE_REGIMENTAL,
              StratConSectorCountMethod.fromLookupName("NOT_A_METHOD"));
    }
}
