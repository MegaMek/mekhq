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
package mekhq.campaign;

import static megamek.release.ReleaseChannel.CORE;
import static megamek.release.ReleaseChannel.EXPERIMENTAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;

import megamek.Version;
import megamek.release.ReleaseChannel;
import mekhq.campaign.io.CampaignXmlParser;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class CampaignVersionChannelXmlTest {
    private static final Version RELEASE = new Version("0.51.02.33");

    private static Element parsedRoot(Version version, ReleaseChannel channel) throws Exception {
        String xml = Campaign.campaignOpenTag(version, channel) + "</campaign>";
        return MHQXMLUtility.newSafeDocumentBuilder()
                     .parse(new InputSource(new StringReader(xml)))
                     .getDocumentElement();
    }

    @Test
    void stampedChannelsSurviveTheRoundTrip() throws Exception {
        assertEquals(CORE, CampaignXmlParser.parseVersionChannel(parsedRoot(RELEASE, CORE)));
        assertEquals(EXPERIMENTAL, CampaignXmlParser.parseVersionChannel(parsedRoot(RELEASE, EXPERIMENTAL)));
    }

    @Test
    void developmentBuildLeavesTheChannelOut() throws Exception {
        Element root = parsedRoot(RELEASE, null);
        assertFalse(root.hasAttribute("channel"));
        assertNull(CampaignXmlParser.parseVersionChannel(root));
    }

    @Test
    void versionAttributeIsEscaped() throws Exception {
        Version branchBuild = new Version("0", "51", "02", "feature/R&D \"<test>\"");
        assertEquals(branchBuild.toString(), parsedRoot(branchBuild, CORE).getAttribute("version"));
    }
}
