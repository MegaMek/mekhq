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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SkillType#createDefaultTypes()}, which presets rely on for skills they do not list.
 *
 * @author Illiani
 * @since 0.51.01
 */
class SkillTypeDefaultTypesTest {
    @AfterEach
    void resetSkillTypes() {
        SkillType.initializeTypes();
    }

    @Test
    void createDefaultTypes_coversTheSameSkillsAsInitializeTypes() {
        SkillType.initializeTypes();

        assertEquals(SkillType.getSkillHash().keySet(), SkillType.createDefaultTypes().keySet());
    }

    @Test
    void createDefaultTypes_returnsNewInstancesEachCall() {
        SkillType.initializeTypes();
        final Map<String, SkillType> first = SkillType.createDefaultTypes();
        final Map<String, SkillType> second = SkillType.createDefaultTypes();

        for (final String skillName : first.keySet()) {
            assertNotSame(first.get(skillName), second.get(skillName), skillName);
            assertNotSame(SkillType.getSkillHash().get(skillName), first.get(skillName), skillName);
        }
    }

    @Test
    void createDefaultTypes_ignoresEditsToTheActiveSkills() {
        SkillType.initializeTypes();
        final String skillName = SkillType.skillList[0];
        final int defaultTarget = SkillType.getType(skillName).getTarget();
        SkillType.getType(skillName).setTarget(defaultTarget + 1);

        assertEquals(defaultTarget, SkillType.createDefaultTypes().get(skillName).getTarget());
    }
}
