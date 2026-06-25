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
 * MechWarrior, BattleMech, `Mech and AeroTek are registered trademarks
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
package mekhq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import megamek.Version;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Backward-compatibility tests for the {@code <skillTypes>} block in campaign preset files, loaded through the real
 * production entry point {@link CampaignPreset#parseFromXML(NodeList, Version)}.
 *
 * <p>Every campaign preset shipped with MekHQ (and every user-saved preset and campaign save) persists each customised
 * skill type inside a {@code <skillType>} element. The loader gate must keep accepting that element name, otherwise a
 * new campaign started from a preset silently loses all skill-type customisation.</p>
 *
 * <p>These tests build a minimal preset containing only a {@code <skillTypes>} block, so they do not require Faction or
 * System data to be initialised.</p>
 */
class CampaignPresetSkillTypeIOTest {

    private static final String SKILL_NAME = "Piloting/Mek";
    private static final int CUSTOM_TARGET = 99;

    /**
     * The historically-stable element name. Every existing preset and save on disk uses this tag, so the loader must
     * continue to accept it. Currently the loader only accepts {@code affectedSkill}, so this test is RED until the
     * gate accepts both names.
     */
    @Test
    void legacySkillTypeTagStillLoadsFromPreset() throws Exception {
        CampaignPreset preset = parsePreset(presetXml("skillType"));

        assertNotNull(preset, "Preset must parse.");
        assertTrue(preset.getSkills().containsKey(SKILL_NAME),
              "A legacy <skillType> node must still be loaded; existing presets and saves use this element name.");
        assertEquals(CUSTOM_TARGET, preset.getSkills().get(SKILL_NAME).getTarget(),
              "The customised target number from the legacy <skillType> node must survive loading.");
    }

    /**
     * Positive control: the current element name loads correctly. This isolates the failure above to backward
     * compatibility only - the new format itself works.
     */
    @Test
    void currentAffectedSkillTagLoadsFromPreset() throws Exception {
        CampaignPreset preset = parsePreset(presetXml("affectedSkill"));

        assertNotNull(preset, "Preset must parse.");
        assertTrue(preset.getSkills().containsKey(SKILL_NAME),
              "The current <affectedSkill> node must load.");
        assertEquals(CUSTOM_TARGET, preset.getSkills().get(SKILL_NAME).getTarget(),
              "The customised target number from the <affectedSkill> node must survive loading.");
    }

    /**
     * Positive control: a genuinely unknown skill element is skipped and the preset still parses. Confirms the gate's
     * reject-and-continue behaviour is intact, so the legacy-tag failure above is specifically a missing accepted name,
     * not a broken parser.
     */
    @Test
    void unrecognisedSkillElementIsSkippedAndPresetStillParses() throws Exception {
        CampaignPreset preset = parsePreset(presetXml("bogusSkillElement"));

        assertNotNull(preset, "Preset must still parse when a skill element is unrecognised.");
        assertTrue(preset.getSkills().isEmpty(), "An unrecognised skill element must not be loaded.");
    }

    private static String presetXml(String skillElementName) {
        return "<campaignPreset version=\"" + MHQConstants.VERSION + "\">"
              + "<title>Test Preset</title>"
              + "<skillTypes>"
              + "<" + skillElementName + ">"
              + "<name>" + SKILL_NAME + "</name>"
              + "<target>" + CUSTOM_TARGET + "</target>"
              + "</" + skillElementName + ">"
              + "</skillTypes>"
              + "</campaignPreset>";
    }

    private static CampaignPreset parsePreset(String xml) throws Exception {
        NodeList children = parseRootChildren(xml);
        return CampaignPreset.parseFromXML(children, MHQConstants.VERSION);
    }

    private static NodeList parseRootChildren(String xml) throws Exception {
        Node root = MHQXMLUtility.newSafeDocumentBuilder()
                          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        return root.getChildNodes();
    }
}
