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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.MaplessStratConGM;
import mekhq.campaign.digitalGM.stratCon.SinglesStratConGM;
import mekhq.campaign.digitalGM.stratCon.StratConDigitalGM;
import mekhq.campaign.digitalGM.stratCon.StratConPlayType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.w3c.dom.Node;

/**
 * IO round-trip coverage for the one piece of persisted state that drives the digital-GM refactor: the StratCon
 * {@link StratConPlayType} stored in {@link CampaignOptions}. The GMs themselves are stateless and never serialized, so
 * this play type is what a loaded campaign uses to pick which GM activates.
 *
 * <p>Covers three save/load concerns:</p>
 * <ul>
 *     <li><b>New saves save + load correctly</b> &mdash; every play type survives a marshal/unmarshal round trip and
 *     restores the same derived option flags.</li>
 *     <li><b>Loads select the right GM</b> &mdash; after a round trip, exactly the matching GM reports itself
 *     enabled.</li>
 *     <li><b>Existing (pre-50.10) saves load correctly</b> &mdash; the legacy {@code useStratCon} /
 *     {@code useMaplessStratCon} booleans migrate to the correct play type.</li>
 * </ul>
 *
 * @author Illiani
 */
class StratConPlayTypePersistenceTest {

    private static final Version VERSION = new Version("0.50.10");

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

    private static Campaign campaignWith(CampaignOptions options) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    // region New saves: marshal -> unmarshal preserves the play type and its derived flags

    @ParameterizedTest
    @EnumSource(StratConPlayType.class)
    void playTypeSurvivesFullRoundTrip(StratConPlayType playType) throws Exception {
        CampaignOptions original = new CampaignOptions();
        original.setStratConPlayType(playType);

        String xml = marshal(original);
        // Guard the write side too: the lookup name must appear in the serialized form
        assertTrue(xml.contains("<stratConPlayType>" + playType.getLookupName() + "</stratConPlayType>"),
              "serialized options should contain the play type lookup name");

        CampaignOptions loaded = unmarshal(xml);

        assertEquals(playType, loaded.getStratConPlayType(), "play type must survive the round trip");
        // Derived flags the GMs and rules read must be restored consistently
        assertEquals(playType != StratConPlayType.DISABLED, loaded.isUseStratCon());
        assertEquals(playType == StratConPlayType.MAPLESS || playType == StratConPlayType.SINGLES,
              loaded.isUseStratConMaplessMode());
        assertEquals(playType == StratConPlayType.SINGLES, loaded.isUseStratConSinglesMode());
    }

    // endregion

    // region Loads select the right GM: exactly the matching GM is enabled after a round trip

    @ParameterizedTest
    @EnumSource(StratConPlayType.class)
    void correctGmIsEnabledAfterRoundTrip(StratConPlayType playType) throws Exception {
        CampaignOptions original = new CampaignOptions();
        original.setStratConPlayType(playType);

        Campaign campaign = campaignWith(unmarshal(marshal(original)));
        CampaignOptions options = campaign.getCampaignOptions();

        assertEquals(playType == StratConPlayType.NORMAL, new StratConDigitalGM().isEnabled(options));
        assertEquals(playType == StratConPlayType.MAPLESS, new MaplessStratConGM().isEnabled(options));
        assertEquals(playType == StratConPlayType.SINGLES, new SinglesStratConGM().isEnabled(options));
    }

    // endregion

    // region Existing (pre-50.10) saves: legacy boolean flags migrate to the correct play type

    private static String legacyCampaignOptions(String innerTags) {
        return "<campaignOptions>" + innerTags + "</campaignOptions>";
    }

    @Test
    void legacyUseStratConTrueMigratesToNormal() throws Exception {
        CampaignOptions loaded = unmarshal(legacyCampaignOptions("<useStratCon>true</useStratCon>"));

        assertEquals(StratConPlayType.NORMAL, loaded.getStratConPlayType());
    }

    @Test
    void legacyMaplessMigratesToMapless() throws Exception {
        CampaignOptions loaded = unmarshal(legacyCampaignOptions(
              "<useStratCon>true</useStratCon><useMaplessStratCon>true</useMaplessStratCon>"));

        assertEquals(StratConPlayType.MAPLESS, loaded.getStratConPlayType());
    }

    @Test
    void legacyUseStratConFalseStaysDisabled() throws Exception {
        CampaignOptions loaded = unmarshal(legacyCampaignOptions("<useStratCon>false</useStratCon>"));

        assertEquals(StratConPlayType.DISABLED, loaded.getStratConPlayType());
        assertFalse(loaded.isUseStratCon());
    }

    @Test
    void saveWithNoStratConTagsDefaultsToDisabled() throws Exception {
        CampaignOptions loaded = unmarshal(legacyCampaignOptions(""));

        assertEquals(StratConPlayType.DISABLED, loaded.getStratConPlayType());
    }

    // endregion
}
