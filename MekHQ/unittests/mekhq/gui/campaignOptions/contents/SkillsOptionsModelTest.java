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
package mekhq.gui.campaignOptions.contents;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SkillsOptionsModel}, which snapshots every {@link SkillType} plus the edge and attribute costs from
 * {@link CampaignOptions}. {@link SkillType#initializeTypes()} runs before each test so the global skill registry the
 * model edits is reset to defaults, keeping the (necessarily global) skill mutations isolated between tests.
 */
class SkillsOptionsModelTest {
    @BeforeEach
    void resetSkillTypes() {
        SkillType.initializeTypes();
    }

    @Test
    void constructorLoadsAConfigurationForEverySkill() {
        SkillsOptionsModel model = new SkillsOptionsModel(new CampaignOptions(), null);

        for (String skillName : SkillType.getSkillList()) {
            assertNotNull(model.getSkillConfiguration(skillName), "missing configuration for skill " + skillName);
        }
    }

    @Test
    void applyToRoundTripsEdgeAndAttributeCosts() {
        SkillsOptionsModel model = new SkillsOptionsModel(new CampaignOptions(), null);
        model.edgeCost = 99;
        model.attributeCost = 77;

        CampaignOptions destination = new CampaignOptions();
        model.applyTo(destination, null);
        SkillsOptionsModel roundTripped = new SkillsOptionsModel(destination, null);

        assertEquals(99, roundTripped.edgeCost);
        assertEquals(77, roundTripped.attributeCost);
    }

    @Test
    void applyToWritesSkillConfigurationBackToTheSkillType() {
        String skillName = SkillType.getSkillList()[0];
        SkillsOptionsModel model = new SkillsOptionsModel(new CampaignOptions(), null);
        SkillConfiguration configuration = model.getSkillConfiguration(skillName);
        assertNotNull(configuration);
        configuration.targetNumber = 12;

        model.applyTo(new CampaignOptions(), null);

        assertEquals(12, SkillType.getType(skillName).getTarget());
    }

    @Test
    void applyToRoundTripsSkillConfigurationCostsAndMilestones() {
        String skillName = SkillType.getSkillList()[0];
        SkillsOptionsModel model = new SkillsOptionsModel(new CampaignOptions(), null);
        SkillConfiguration configuration = model.getSkillConfiguration(skillName);
        assertNotNull(configuration);

        configuration.targetNumber = 11;
        configuration.greenLevel = 3;
        configuration.regularLevel = 4;
        configuration.veteranLevel = 5;
        configuration.eliteLevel = 6;
        configuration.heroicLevel = 7;
        configuration.legendaryLevel = 8;
        for (int level = 0; level < configuration.costs.length; level++) {
            configuration.costs[level] = level + 1;
        }

        // applyTo writes each configuration back to the global SkillType registry; a fresh model reads it back out.
        model.applyTo(new CampaignOptions(), null);
        SkillsOptionsModel reloaded = new SkillsOptionsModel(new CampaignOptions(), null);
        SkillConfiguration roundTripped = reloaded.getSkillConfiguration(skillName);
        assertNotNull(roundTripped);

        assertEquals(11, roundTripped.targetNumber);
        assertEquals(3, roundTripped.greenLevel);
        assertEquals(4, roundTripped.regularLevel);
        assertEquals(5, roundTripped.veteranLevel);
        assertEquals(6, roundTripped.eliteLevel);
        assertEquals(7, roundTripped.heroicLevel);
        assertEquals(8, roundTripped.legendaryLevel);
        assertArrayEquals(configuration.costs, roundTripped.costs);
    }
}
