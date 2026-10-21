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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import mekhq.campaign.finances.enums.UnitCostModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Node;

/**
 * Save/load coverage for the {@link UnitCostModel} that replaced the old "Alternate Unit Cost" boolean.
 *
 * @author Illiani
 * @since 0.51.01
 */
class UnitCostModelPersistenceTest {
    private static final Version VERSION = new Version("0.51.01");

    private static String marshal(CampaignOptions options) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            CampaignOptionsMarshaller.writeCampaignOptionsToXML(options, printWriter, 0);
        }
        return stringWriter.toString();
    }

    private static CampaignOptions unmarshal(String xml) throws Exception {
        Node campaignOptionsNode = DocumentBuilderFactory.newInstance()
                                         .newDocumentBuilder()
                                         .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                                         .getDocumentElement();
        return CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(campaignOptionsNode, VERSION);
    }

    private static CampaignOptions unmarshalTags(String innerTags) throws Exception {
        return unmarshal("<campaignOptions>" + innerTags + "</campaignOptions>");
    }

    @Test
    void newCampaignsDefaultToCanon() {
        assertEquals(UnitCostModel.CANON, new CampaignOptions().get(CampaignOption.UNIT_COST_MODEL));
    }

    @ParameterizedTest
    @EnumSource(UnitCostModel.class)
    void unitCostModelSurvivesRoundTrip(UnitCostModel unitCostModel) throws Exception {
        CampaignOptions original = new CampaignOptions();
        original.set(CampaignOption.UNIT_COST_MODEL, unitCostModel);

        String xml = marshal(original);
        assertTrue(xml.contains("<unitCostModel>" + unitCostModel.name() + "</unitCostModel>"),
              "serialized options should contain the unit cost model's name");

        assertEquals(unitCostModel, unmarshal(xml).get(CampaignOption.UNIT_COST_MODEL));
    }

    @Test
    void newSavesNoLongerWriteTheLegacyBoolean() {
        assertFalse(marshal(new CampaignOptions()).contains("useAlternateUnitCost"));
    }

    @Test
    void legacyAlternateUnitCostMigratesToChaosCampaignStrict() throws Exception {
        CampaignOptions loaded = unmarshalTags("<useAlternateUnitCost>true</useAlternateUnitCost>");

        assertEquals(UnitCostModel.CHAOS_CAMPAIGN_STRICT, loaded.get(CampaignOption.UNIT_COST_MODEL));
    }

    @Test
    void legacyNonAlternateUnitCostMigratesToCanon() throws Exception {
        CampaignOptions loaded = unmarshalTags("<useAlternateUnitCost>false</useAlternateUnitCost>");

        assertEquals(UnitCostModel.CANON, loaded.get(CampaignOption.UNIT_COST_MODEL));
    }

    @Test
    void saveWithoutAnyUnitCostTagDefaultsToCanon() throws Exception {
        assertEquals(UnitCostModel.CANON, unmarshalTags("").get(CampaignOption.UNIT_COST_MODEL));
    }
}
