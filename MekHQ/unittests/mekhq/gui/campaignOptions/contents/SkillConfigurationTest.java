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
import static org.junit.jupiter.api.Assertions.assertNotSame;

import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SkillConfiguration}, the per-skill DTO used by the Skills page. The class exists so a skill's
 * editable values can be read from a {@link SkillType}, copied for the table's Copy/Paste feature, and written back; the
 * tests pin down that round-trip and the deep-copy independence the Copy/Paste feature relies on.
 */
class SkillConfigurationTest {
    @BeforeAll
    static void initializeSkillTypes() {
        SkillType.initializeTypes();
    }

    private static SkillType skillTypeWithKnownValues() {
        SkillType skillType = new SkillType();
        skillType.setTarget(5);
        skillType.setGreenLevel(1);
        skillType.setRegularLevel(2);
        skillType.setVeteranLevel(3);
        skillType.setEliteLevel(4);
        skillType.setHeroicLevel(6);
        skillType.setLegendaryLevel(7);
        Integer[] costs = skillType.getCosts();
        for (int i = 0; i < costs.length; i++) {
            costs[i] = i + 10;
        }
        return skillType;
    }

    @Test
    void constructorCopiesEveryValueFromSkillType() {
        SkillType source = skillTypeWithKnownValues();

        SkillConfiguration configuration = new SkillConfiguration(source);

        assertEquals(5, configuration.targetNumber);
        assertEquals(1, configuration.greenLevel);
        assertEquals(2, configuration.regularLevel);
        assertEquals(3, configuration.veteranLevel);
        assertEquals(4, configuration.eliteLevel);
        assertEquals(6, configuration.heroicLevel);
        assertEquals(7, configuration.legendaryLevel);
        assertArrayEquals(source.getCosts(), configuration.costs);
        assertNotSame(source.getCosts(), configuration.costs, "costs must be a defensive copy, not the live array");
    }

    @Test
    void copyConstructorProducesAnIndependentCopy() {
        SkillConfiguration original = new SkillConfiguration(skillTypeWithKnownValues());

        SkillConfiguration copy = new SkillConfiguration(original);

        assertEquals(original.targetNumber, copy.targetNumber);
        assertArrayEquals(original.costs, copy.costs);
        assertNotSame(original.costs, copy.costs);

        copy.targetNumber = 888;
        copy.costs[0] = 999;
        assertEquals(5, original.targetNumber, "mutating the copy must not affect the original");
        assertEquals(10, original.costs[0], "mutating the copied costs must not affect the original");
    }

    @Test
    void copyFromOverwritesValuesAndLeavesSourceUntouched() {
        SkillConfiguration source = new SkillConfiguration(skillTypeWithKnownValues());
        SkillConfiguration destination = new SkillConfiguration(new SkillType());

        destination.copyFrom(source);

        assertEquals(source.targetNumber, destination.targetNumber);
        assertEquals(source.legendaryLevel, destination.legendaryLevel);
        assertArrayEquals(source.costs, destination.costs);
        assertNotSame(source.costs, destination.costs);

        destination.costs[0] = 999;
        assertEquals(10, source.costs[0], "copyFrom must deep-copy the costs array");
    }

    @Test
    void applyToWritesEveryValueBackIntoASkillType() {
        SkillConfiguration configuration = new SkillConfiguration(skillTypeWithKnownValues());
        SkillType target = new SkillType();

        configuration.applyTo(target);

        assertEquals(configuration.targetNumber, target.getTarget());
        assertEquals(configuration.greenLevel, target.getGreenLevel());
        assertEquals(configuration.regularLevel, target.getRegularLevel());
        assertEquals(configuration.veteranLevel, target.getVeteranLevel());
        assertEquals(configuration.eliteLevel, target.getEliteLevel());
        assertEquals(configuration.heroicLevel, target.getHeroicLevel());
        assertEquals(configuration.legendaryLevel, target.getLegendaryLevel());

        Integer[] targetCosts = target.getCosts();
        for (int i = 0; i < Math.min(configuration.costs.length, targetCosts.length); i++) {
            assertEquals(configuration.costs[i], targetCosts[i]);
        }
    }
}
