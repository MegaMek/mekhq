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
package mekhq.service.mrms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.w3c.dom.Node;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Every Mass Repair entry point offers the same units, and the saved Mass Repair option list holds exactly one row
 * per category Mass Repair offers (issue #10218).
 */
class MRMSUnitRuleAndOptionRowsTest {
    private static final Version VERSION = new Version("0.51.01");

    @ParameterizedTest(name = "{0} can be Mass Repaired: {1}")
    @CsvSource({
          "LOCUST_LCT_1V, true",
          "MINOTAUR_PROTOMEK, true",
          "EPONA_PURSUIT_TANK_PRIME, true",
          "ELEMENTAL_BATTLE_ARMOR_LASER, true",
          "LEOPARD_DROPSHIP, false"
    })
    void massRepairAcceptsEveryUnitATechCanWorkOnButNotSelfCrewedOnes(UnitFixture fixture, boolean isExpected) {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, true);
        Unit unit = scenario.withUnit(fixture);

        assertEquals(isExpected, MRMSService.isMassRepairableUnit(unit));
        assertEquals(isExpected, MRMSService.isValidMRMSUnit(unit, new MRMSConfiguredOptions(campaign)),
              "The dialog and Instant Mass Repair agree with the right-click menu");
    }

    @Test
    void aNewCampaignHasOneRowPerCategoryMassRepairOffers() {
        List<PartRepairType> rowTypes = new ArrayList<>();
        for (MRMSOption option : new CampaignOptions().get(CampaignOption.MRMS_OPTIONS)) {
            rowTypes.add(option.getType());
        }

        assertEquals(PartRepairType.getMRMSValidTypes(), rowTypes);
    }

    @Test
    void loadingDropsRowsMassRepairDoesNotOfferAndFillsInMissingOnes() throws Exception {
        String xml = "<mrmsOptions>"
                           + row(PartRepairType.WEAPON, false)
                           + row(PartRepairType.MEK_LOCATION, true)
                           + row(PartRepairType.HEAT_SINK, true)
                           + "</mrmsOptions>";
        Node optionsNode = DocumentBuilderFactory.newInstance()
                                 .newDocumentBuilder()
                                 .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                                 .getDocumentElement();

        List<MRMSOption> loadedOptions = MRMSOption.parseListFromXML(optionsNode, VERSION);

        List<PartRepairType> validTypes = PartRepairType.getMRMSValidTypes();
        assertEquals(validTypes.size(), loadedOptions.size(), "One row for each category Mass Repair offers");
        for (MRMSOption option : loadedOptions) {
            assertTrue(validTypes.contains(option.getType()), option.getType() + " is not a Mass Repair category");
            boolean isSavedWeaponRow = option.getType() == PartRepairType.WEAPON;
            assertEquals(!isSavedWeaponRow, option.isActive(), "A saved row keeps its setting; a new row is on");
        }
    }

    @Test
    void settingsTakenForARunDoNotChangeWhenTheCampaignListChanges() {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(campaign);
        int rowsTaken = configuredOptions.getMRMSOptions().size();

        campaign.getCampaignOptions().get(CampaignOption.MRMS_OPTIONS).clear();

        assertEquals(rowsTaken, configuredOptions.getMRMSOptions().size());
        assertFalse(configuredOptions.getMRMSOptions().isEmpty());
    }

    private static String row(PartRepairType type, boolean isActive) {
        return "<mrmsOption><type>" + type.name() + "</type><active>" + (isActive ? 1 : 0)
                     + "</active><skillMin>0</skillMin><skillMax>5</skillMax><targetNumberPreferred>4"
                     + "</targetNumberPreferred><targetNumberMax>6</targetNumberMax><dailyTimeMin>0"
                     + "</dailyTimeMin></mrmsOption>";
    }
}
