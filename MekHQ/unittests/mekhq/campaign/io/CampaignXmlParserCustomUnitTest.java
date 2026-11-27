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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.loaders.MekSummary;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.PlayerForce;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;

/**
 * Tests that loading a campaign writes a custom design to the customs folder only when the unit list does not already
 * have it (issue #11307).
 */
class CampaignXmlParserCustomUnitTest {
    private static final String FORCE_NAME = "Helena's Valkyries";
    private static final String DESIGN_NAME = "Harasser Missile Platform VAL-1";
    private static final String CUSTOM_XML = "<custom><name>" + DESIGN_NAME + "</name>"
                                                   + "<blk><![CDATA[<UnitType>\nTank\n</UnitType>]]></blk></custom>";

    @TempDir
    File tempDirectory;

    private Campaign campaign;
    private Node customNode;
    private String customsDirectory;
    private File campaignCopy;

    @BeforeEach
    void setUp() throws Exception {
        campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(playerForce.getName()).thenReturn(FORCE_NAME);
        when(campaign.getPlayerForce()).thenReturn(playerForce);

        customNode = DocumentBuilderFactory.newInstance()
                           .newDocumentBuilder()
                           .parse(new ByteArrayInputStream(CUSTOM_XML.getBytes(StandardCharsets.UTF_8)))
                           .getDocumentElement();

        customsDirectory = new File(tempDirectory, "customs").getPath();
        campaignCopy = new File(new File(customsDirectory, FORCE_NAME), DESIGN_NAME + ".blk");
    }

    @Test
    void designAlreadyInUnitListIsNotCopied() {
        MekSummary savedElsewhere = new MekSummary();
        savedElsewhere.setSourceFile(new File(tempDirectory, DESIGN_NAME + ".blk"));

        boolean wroteFile = CampaignXmlParser.processCustom(campaign, customNode, customsDirectory,
              name -> DESIGN_NAME.equals(name) ? savedElsewhere : null);

        assertFalse(wroteFile);
        assertFalse(campaignCopy.exists());
        verify(campaign, never()).addCustom(anyString());
    }

    @Test
    void designMissingFromUnitListIsWrittenToCampaignCustoms() {
        boolean wroteFile = CampaignXmlParser.processCustom(campaign, customNode, customsDirectory, name -> null);

        assertTrue(wroteFile);
        assertTrue(campaignCopy.exists());
        verify(campaign).addCustom(DESIGN_NAME);
    }
}
