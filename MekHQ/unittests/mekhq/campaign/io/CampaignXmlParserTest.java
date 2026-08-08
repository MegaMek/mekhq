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
package mekhq.campaign.io;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import megamek.Version;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOptionsUnmarshaller;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Tests the save-load reputation migration in {@link CampaignXmlParser}: Chaos Reputation defaults on for new
 * campaigns, but existing campaigns (whose saves predate the option) must not be silently switched over.
 */
class CampaignXmlParserTest {
    private static final Version VERSION = new Version("0.50.10");

    /** Parses a {@code <campaignOptions>} element with the supplied inner tags, mirroring on-disk save content. */
    private static Node campaignOptionsNode(final String innerXml) {
        final String xml = "<campaignOptions>" + innerXml + "</campaignOptions>";
        final Document document = assertDoesNotThrow(() -> MHQXMLUtility.parseDocument(
              new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
        return document.getElementsByTagName("campaignOptions").item(0);
    }

    @Test
    void existingCampaignWithoutTag_keepsCamOpsReputation() {
        // A save predating the option has no <useChaosReputation> tag; the parsed value falls back to the (true)
        // default, which must be overridden so the campaign is not flipped onto Chaos Reputation.
        Node node = campaignOptionsNode("<manualUnitRatingModifier>0</manualUnitRatingModifier>");
        CampaignOptions options = CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);
        assertTrue(options.get(CampaignOption.USE_CHAOS_REPUTATION), "default should start enabled before migration");

        CampaignXmlParser.preserveLegacyReputationForExistingCampaigns(node, options);

        assertFalse(options.get(CampaignOption.USE_CHAOS_REPUTATION),
              "a save without the tag must keep CamOps reputation");
    }

    @Test
    void newFormatSaveWithTagEnabled_keepsChaosReputation() {
        Node node = campaignOptionsNode("<useChaosReputation>true</useChaosReputation>");
        CampaignOptions options = CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);

        CampaignXmlParser.preserveLegacyReputationForExistingCampaigns(node, options);

        assertTrue(options.get(CampaignOption.USE_CHAOS_REPUTATION), "an explicit tag value must be honored");
    }

    @Test
    void newFormatSaveWithTagDisabled_staysDisabled() {
        Node node = campaignOptionsNode("<useChaosReputation>false</useChaosReputation>");
        CampaignOptions options = CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);

        CampaignXmlParser.preserveLegacyReputationForExistingCampaigns(node, options);

        assertFalse(options.get(CampaignOption.USE_CHAOS_REPUTATION), "an explicit tag value must be honored");
    }
}
