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
package mekhq.campaign.personnel.skills;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import megamek.Version;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

/**
 * Save-side tests for {@link SkillType#writeToXML(PrintWriter, int)}.
 *
 * <p>{@link SkillType#writeToXML} is the writer used both by campaign saves ({@code Campaign} {@code <skillTypes>}
 * block) and by campaign presets ({@code CampaignPreset} {@code <skillTypes>} block). The element name it emits is the
 * on-disk save format. Changing it breaks every reader - older MekHQ versions and the current loader gate - so the
 * element name must stay stable.</p>
 */
class SkillTypeXmlIOTest {

    @BeforeAll
    static void initializeSkillTypes() {
        SkillType.initializeTypes();
    }

    /**
     * The writer must emit the historically-stable {@code <skillType>} wrapper element; changing it would break
     * backwards compatibility with existing saves/presets and any loader gates that check element names.
     */
    @Test
    void writeToXmlUsesBackwardCompatibleElementName() throws Exception {
        Node written = writeSkillTypeAndReparse(SkillType.getType(SkillType.S_PILOT_MEK));

        assertEquals("skillType", written.getNodeName(),
              "writeToXML must emit the historically-stable <skillType> element so existing saves and presets, and "
                    + "the loader gate, can still read it.");
    }

    /**
     * Characterisation: writing a skill type and reading it back through the real reader preserves the field values.
     * The reader ({@link SkillType#generateInstanceFromXML}) keys off the child element names, so this round-trip is
     * symmetric regardless of the wrapper element name; it passes today and documents the working read/write pairing.
     */
    @Test
    void writeReadRoundTripPreservesTargetNumber() throws Exception {
        SkillType original = SkillType.getType(SkillType.S_PILOT_MEK);
        int originalTarget = original.getTarget();

        Node written = writeSkillTypeAndReparse(original);
        SkillType.generateInstanceFromXML(written, new Version());

        assertEquals(originalTarget, SkillType.getType(SkillType.S_PILOT_MEK).getTarget(),
              "Target number must survive a write -> read round-trip.");
    }

    /**
     * Characterisation: every field {@link SkillType#writeToXML} emits survives a write -> read round-trip. This is the
     * broad guard against a "written-but-not-read / read-but-not-written" tag mismatch in the skill-type save format.
     */
    @Test
    void writeReadRoundTripPreservesAllPersistedFields() throws Exception {
        SkillType original = SkillType.getType(SkillType.S_PILOT_MEK);

        int target = original.getTarget();
        boolean countUp = original.isCountUp();
        SkillSubType subType = original.getSubType();
        SkillAttribute firstAttribute = original.getFirstAttribute();
        SkillAttribute secondAttribute = original.getSecondAttribute();
        int greenLevel = original.getGreenLevel();
        int regularLevel = original.getRegularLevel();
        int veteranLevel = original.getVeteranLevel();
        int eliteLevel = original.getEliteLevel();
        int heroicLevel = original.getHeroicLevel();
        int legendaryLevel = original.getLegendaryLevel();
        Integer[] costs = original.getCosts();

        Node written = writeSkillTypeAndReparse(original);
        SkillType.generateInstanceFromXML(written, new Version());

        SkillType reloaded = SkillType.getType(SkillType.S_PILOT_MEK);
        assertEquals(target, reloaded.getTarget(), "target");
        assertEquals(countUp, reloaded.isCountUp(), "isCountUp");
        assertEquals(subType, reloaded.getSubType(), "subType");
        assertEquals(firstAttribute, reloaded.getFirstAttribute(), "firstAttribute");
        assertEquals(secondAttribute, reloaded.getSecondAttribute(), "secondAttribute");
        assertEquals(greenLevel, reloaded.getGreenLevel(), "greenLvl");
        assertEquals(regularLevel, reloaded.getRegularLevel(), "regLvl");
        assertEquals(veteranLevel, reloaded.getVeteranLevel(), "vetLvl");
        assertEquals(eliteLevel, reloaded.getEliteLevel(), "eliteLvl");
        assertEquals(heroicLevel, reloaded.getHeroicLevel(), "heroicLvl");
        assertEquals(legendaryLevel, reloaded.getLegendaryLevel(), "legendaryLvl");
        assertArrayEquals(costs, reloaded.getCosts(), "costs");
    }

    private static Node writeSkillTypeAndReparse(SkillType skillType) throws Exception {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            skillType.writeToXML(printWriter, 0);
        }

        return MHQXMLUtility.newSafeDocumentBuilder()
                     .parse(new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8)))
                     .getDocumentElement();
    }
}
