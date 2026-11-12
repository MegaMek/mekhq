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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

/**
 * Spare parts, training munitions and fuel are each their own peacetime operating cost option.
 */
class PeacetimeCostPartsTest {
    private CampaignOptions campaignOptions;

    @BeforeEach
    void beforeEach() {
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
    }

    @Test
    void nothingIsChargedByDefault() {
        assertFalse(campaignOptions.isChargingPeacetimeSpareParts());
        assertFalse(campaignOptions.isChargingPeacetimeTrainingMunitions());
        assertFalse(campaignOptions.isChargingPeacetimeFuel());
        assertFalse(campaignOptions.isChargingPeacetimeCost());
    }

    @Test
    void eachCostIsChargedOnItsOwn() {
        campaignOptions.set(CampaignOption.PAY_FOR_TRAINING_MUNITIONS, true);

        assertFalse(campaignOptions.isChargingPeacetimeSpareParts());
        assertTrue(campaignOptions.isChargingPeacetimeTrainingMunitions());
        assertFalse(campaignOptions.isChargingPeacetimeFuel());
        assertTrue(campaignOptions.isChargingPeacetimeCost(), "Any one cost counts as charging peacetime costs");
    }

    @Test
    void noCostIsChargedAlongsideHotSpotsUpkeep() {
        campaignOptions.set(CampaignOption.PAY_FOR_PEACETIME_SPARE_PARTS, true);
        campaignOptions.set(CampaignOption.PAY_FOR_TRAINING_MUNITIONS, true);
        campaignOptions.set(CampaignOption.PAY_FOR_PEACETIME_FUEL, true);
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, true);

        assertFalse(campaignOptions.isChargingPeacetimeSpareParts());
        assertFalse(campaignOptions.isChargingPeacetimeTrainingMunitions());
        assertFalse(campaignOptions.isChargingPeacetimeFuel());
        assertFalse(campaignOptions.isChargingPeacetimeCost());
        assertTrue(campaignOptions.isAnyPeacetimeCostSelected(), "Hot Spots doesn't change the stored choices");
    }

    @Test
    void oldSavesThatUsedPeacetimeCostsChargeEveryCost() throws Exception {
        CampaignOptions loaded = unmarshalTags("<usePeacetimeCost>true</usePeacetimeCost>");

        assertTrue(loaded.get(CampaignOption.PAY_FOR_PEACETIME_SPARE_PARTS));
        assertTrue(loaded.get(CampaignOption.PAY_FOR_TRAINING_MUNITIONS));
        assertTrue(loaded.get(CampaignOption.PAY_FOR_PEACETIME_FUEL));
    }

    @Test
    void oldSavesWithoutPeacetimeCostsChargeNothing() throws Exception {
        CampaignOptions loaded = unmarshalTags("<usePeacetimeCost>false</usePeacetimeCost>");

        assertFalse(loaded.isAnyPeacetimeCostSelected());
    }

    @Test
    void eachCostSurvivesSaveAndLoad() throws Exception {
        campaignOptions.set(CampaignOption.PAY_FOR_PEACETIME_FUEL, true);

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            CampaignOptionsMarshaller.writeCampaignOptionsToXML(campaignOptions, printWriter, 0);
        }
        String xml = stringWriter.toString();
        assertFalse(xml.contains("usePeacetimeCost"), "New saves no longer write the old switch");

        CampaignOptions loaded = unmarshal(xml);
        assertFalse(loaded.get(CampaignOption.PAY_FOR_PEACETIME_SPARE_PARTS));
        assertFalse(loaded.get(CampaignOption.PAY_FOR_TRAINING_MUNITIONS));
        assertTrue(loaded.get(CampaignOption.PAY_FOR_PEACETIME_FUEL));
    }

    private static CampaignOptions unmarshal(String xml) throws Exception {
        Node campaignOptionsNode = DocumentBuilderFactory.newInstance()
                                         .newDocumentBuilder()
                                         .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                                         .getDocumentElement();
        return CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(campaignOptionsNode,
              new Version("0.51.01"));
    }

    private static CampaignOptions unmarshalTags(String innerTags) throws Exception {
        return unmarshal("<campaignOptions>" + innerTags + "</campaignOptions>");
    }
}
