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

import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_ALTERNATE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.personnel.PersonnelOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests how the Fame trait (passed in as adjusted reputation) modifies the social skills in
 * {@link Skill#getSPAModifiers(PersonnelOptions, int)}, including the Alternate ID softening of negative Fame.
 *
 * @author Illiani
 * @since 0.51.01
 */
class SkillFameModifierTest {
    private PersonnelOptions options;

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        options = new PersonnelOptions();
    }

    private static int modifierFor(String skillName, PersonnelOptions options, int fame) {
        return new Skill(skillName).getSPAModifiers(options, fame);
    }

    @ParameterizedTest
    @CsvSource({
          SkillType.S_NEGOTIATION + ", 3",
          SkillType.S_NEGOTIATION + ", -4",
          SkillType.S_PROTOCOLS + ", 2",
          SkillType.S_PROTOCOLS + ", -2",
          SkillType.S_STREETWISE + ", 5",
          SkillType.S_STREETWISE + ", -5"
    })
    void fameIsAddedToSocialSkills(String skillName, int fame) {
        assertEquals(fame, modifierFor(skillName, options, fame));
    }

    @ParameterizedTest
    @ValueSource(ints = { -5, -1, 1, 5 })
    void fameDoesNotAffectOtherSkills(int fame) {
        assertEquals(0, modifierFor(SkillType.S_TACTICS, options, fame));
        assertEquals(0, modifierFor(SkillType.S_LEADER, options, fame));
    }

    @ParameterizedTest
    @CsvSource({ "-5, -3", "-3, -1", "-2, 0", "-1, 0" })
    void alternateIdSoftensNegativeFameWithoutMakingItPositive(int fame, int expected) {
        options.acquireAbility(LVL3_ADVANTAGES, ATOW_ALTERNATE_ID, true);
        assertEquals(expected, modifierFor(SkillType.S_NEGOTIATION, options, fame));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 5 })
    void alternateIdLeavesNonNegativeFameUnchanged(int fame) {
        options.acquireAbility(LVL3_ADVANTAGES, ATOW_ALTERNATE_ID, true);
        assertEquals(fame, modifierFor(SkillType.S_STREETWISE, options, fame));
    }

    @Test
    void zeroFameGivesNoModifier() {
        assertEquals(0, modifierFor(SkillType.S_PROTOCOLS, options, 0));
    }
}
