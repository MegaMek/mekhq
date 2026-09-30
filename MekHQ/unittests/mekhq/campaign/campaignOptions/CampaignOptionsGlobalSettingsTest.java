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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import megamek.Version;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.common.preference.ClientPreferences;
import megamek.common.preference.PreferenceManager;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Tests for the campaign options that MegaMek holds globally: percent female, the name generator faction and the
 * strategic view minimap theme. They must persist with the campaign options (so presets carry them), reading them must
 * never touch the globals, and {@link CampaignOptions#applyGlobalSettings()} must push them out. The globals are saved
 * and restored around each test so no state leaks into other tests.
 *
 * @author Illiani
 * @since 0.51.01
 */
class CampaignOptionsGlobalSettingsTest {
    private static final Version VERSION = new Version("0.51.01");
    private static final ClientPreferences CLIENT_PREFERENCES = PreferenceManager.getClientPreferences();

    private int originalPercentFemale;
    private String originalChosenFaction;
    private String originalStrategicViewTheme;

    @BeforeEach
    void rememberGlobals() {
        originalPercentFemale = RandomGenderGenerator.getPercentFemale();
        originalChosenFaction = RandomNameGenerator.getInstance().getChosenFaction();
        originalStrategicViewTheme = CLIENT_PREFERENCES.getStrategicViewTheme().getName();
    }

    @AfterEach
    void restoreGlobals() {
        RandomGenderGenerator.setPercentFemale(originalPercentFemale);
        RandomNameGenerator.getInstance().setChosenFaction(originalChosenFaction);
        CLIENT_PREFERENCES.setStrategicViewTheme(originalStrategicViewTheme);
    }

    private static CampaignOptions roundTrip(final CampaignOptions original) {
        final StringWriter stringWriter = new StringWriter();
        try (PrintWriter pw = new PrintWriter(stringWriter)) {
            CampaignOptionsMarshaller.writeCampaignOptionsToXML(original, pw, 0);
        }
        final Document document = assertDoesNotThrow(() -> MHQXMLUtility.parseDocument(
              new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8))));
        final Node node = document.getElementsByTagName("campaignOptions").item(0);
        return CampaignOptionsUnmarshaller.generateCampaignOptionsFromXml(node, VERSION);
    }

    @Test
    void globalSettings_surviveARoundTrip() {
        final CampaignOptions original = new CampaignOptions();
        original.set(CampaignOption.PERCENT_FEMALE, 73);
        original.set(CampaignOption.NAME_GENERATOR_FACTION, "FS");
        original.set(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME, "test.theme");

        final CampaignOptions reloaded = roundTrip(original);

        assertEquals(73, reloaded.get(CampaignOption.PERCENT_FEMALE));
        assertEquals("FS", reloaded.get(CampaignOption.NAME_GENERATOR_FACTION));
        assertEquals("test.theme", reloaded.get(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME));
    }

    @Test
    void readingOptions_leavesTheGlobalsAlone() {
        RandomGenderGenerator.setPercentFemale(11);
        RandomNameGenerator.getInstance().setChosenFaction("CC");
        CLIENT_PREFERENCES.setStrategicViewTheme("before.theme");

        final CampaignOptions original = new CampaignOptions();
        original.set(CampaignOption.PERCENT_FEMALE, 73);
        original.set(CampaignOption.NAME_GENERATOR_FACTION, "FS");
        original.set(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME, "test.theme");
        roundTrip(original);

        assertEquals(11, RandomGenderGenerator.getPercentFemale());
        assertEquals("CC", RandomNameGenerator.getInstance().getChosenFaction());
        assertEquals("before.theme", CLIENT_PREFERENCES.getStrategicViewTheme().getName());
    }

    @Test
    void applyGlobalSettings_pushesEveryGlobalOption() {
        final CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.PERCENT_FEMALE, 73);
        options.set(CampaignOption.NAME_GENERATOR_FACTION, "FS");
        options.set(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME, "test.theme");

        options.applyGlobalSettings();

        assertEquals(73, RandomGenderGenerator.getPercentFemale());
        assertEquals("FS", RandomNameGenerator.getInstance().getChosenFaction());
        assertEquals("test.theme", CLIENT_PREFERENCES.getStrategicViewTheme().getName());
    }
}
