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
import mekhq.campaign.mission.scenarios.salvage.SalvageSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Node;

/**
 * Save/load coverage for the salvage options: the {@link SalvageSystem} that replaced the old "Use CamOps Salvage"
 * boolean, and the option to keep enemy camouflage on salvage.
 *
 * @author Illiani
 */
class SalvageSystemPersistenceTest {
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
    void newCampaignsDefaultToLegacy() {
        CampaignOptions options = new CampaignOptions();

        assertEquals(SalvageSystem.LEGACY, options.get(CampaignOption.SALVAGE_SYSTEM));
        assertFalse(options.get(CampaignOption.IS_KEEP_ENEMY_CAMOUFLAGE_ON_SALVAGE));
    }

    @ParameterizedTest
    @EnumSource(SalvageSystem.class)
    void salvageSystemSurvivesRoundTrip(SalvageSystem salvageSystem) throws Exception {
        CampaignOptions original = new CampaignOptions();
        original.set(CampaignOption.SALVAGE_SYSTEM, salvageSystem);

        String xml = marshal(original);
        assertTrue(xml.contains("<salvageSystem>" + salvageSystem.getLookupName() + "</salvageSystem>"),
              "serialized options should contain the salvage system's lookup name");

        assertEquals(salvageSystem, unmarshal(xml).get(CampaignOption.SALVAGE_SYSTEM));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void keepEnemyCamouflageSurvivesRoundTrip(boolean isKeepEnemyCamouflage) throws Exception {
        CampaignOptions original = new CampaignOptions();
        original.set(CampaignOption.IS_KEEP_ENEMY_CAMOUFLAGE_ON_SALVAGE, isKeepEnemyCamouflage);

        CampaignOptions loaded = unmarshal(marshal(original));

        assertEquals(isKeepEnemyCamouflage, loaded.get(CampaignOption.IS_KEEP_ENEMY_CAMOUFLAGE_ON_SALVAGE));
    }

    @Test
    void newSavesNoLongerWriteTheLegacyBoolean() {
        assertFalse(marshal(new CampaignOptions()).contains("isUseCamOpsSalvage"));
    }

    @Test
    void legacyCamOpsSalvageMigratesToCamOpsStrict() throws Exception {
        CampaignOptions loaded = unmarshalTags("<isUseCamOpsSalvage>true</isUseCamOpsSalvage>");

        assertEquals(SalvageSystem.CAM_OPS_STRICT, loaded.get(CampaignOption.SALVAGE_SYSTEM));
    }

    @Test
    void legacyNonCamOpsSalvageMigratesToLegacy() throws Exception {
        CampaignOptions loaded = unmarshalTags("<isUseCamOpsSalvage>false</isUseCamOpsSalvage>");

        assertEquals(SalvageSystem.LEGACY, loaded.get(CampaignOption.SALVAGE_SYSTEM));
    }

    @Test
    void saveWithoutAnySalvageTagDefaultsToLegacy() throws Exception {
        assertEquals(SalvageSystem.LEGACY, unmarshalTags("").get(CampaignOption.SALVAGE_SYSTEM));
    }

    @Test
    void unknownSalvageSystemFallsBackToLegacy() throws Exception {
        CampaignOptions loaded = unmarshalTags("<salvageSystem>NOT_A_SYSTEM</salvageSystem>");

        assertEquals(SalvageSystem.LEGACY, loaded.get(CampaignOption.SALVAGE_SYSTEM));
    }
}
