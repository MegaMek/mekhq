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

import static mekhq.campaign.personnel.PersonnelOptions.TECH_SPECIALIST_ELECTRONIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.personnel.PersonnelOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Tech Specialist SPAs: holding one improves the matching granular Tech/... specialist skill by +1 and
 * hinders every other specialist skill by -1, while non-specialist tech skills and unrelated skills are untouched.
 */
class TechSpecialistSpaTest {

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    private static int spaModifier(String skillName, PersonnelOptions options) {
        return new Skill(skillName, 4, 0).getSPAModifiers(options, 0);
    }

    @Test
    void aTechSpecialistBoostsItsOwnSkillAndHindersTheOthers() {
        PersonnelOptions options = new PersonnelOptions();
        options.getOption(TECH_SPECIALIST_ELECTRONIC).setValue(true);

        assertEquals(1, spaModifier(SkillType.S_TECH_ELECTRONIC, options),
              "the specialized skill gains +1");
        assertEquals(-1, spaModifier(SkillType.S_TECH_MECHANICAL, options),
              "another specialist skill is hindered by -1");
        assertEquals(-1, spaModifier(SkillType.S_TECH_WEAPONS, options),
              "every other specialist skill is hindered by -1");
    }

    @Test
    void aTechSpecialistDoesNotAffectGlobalOrUnrelatedSkills() {
        PersonnelOptions options = new PersonnelOptions();
        options.getOption(TECH_SPECIALIST_ELECTRONIC).setValue(true);

        assertEquals(0, spaModifier(SkillType.S_TECH_MEK, options),
              "a whole-unit global tech skill is not a specialist skill and is unaffected");
        assertEquals(0, spaModifier(SkillType.S_ADMIN, options),
              "an unrelated skill is unaffected");
    }

    @Test
    void withoutAnyTechSpecialistSpaThereIsNoModifier() {
        PersonnelOptions options = new PersonnelOptions();

        assertEquals(0, spaModifier(SkillType.S_TECH_ELECTRONIC, options));
        assertEquals(0, spaModifier(SkillType.S_TECH_MECHANICAL, options));
    }
}
