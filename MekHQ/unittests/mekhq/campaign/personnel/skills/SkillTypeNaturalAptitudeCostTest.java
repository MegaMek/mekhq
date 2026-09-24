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

import static mekhq.campaign.personnel.skills.SkillType.ADVANCED_NATURAL_APTITUDE_COST;
import static mekhq.campaign.personnel.skills.SkillType.BASIC_NATURAL_APTITUDE_COST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the default Natural Aptitude XP costs, which follow each skill's complexity in the A Time of War Master Skills
 * List: Basic skills (SB, CB) cost 300 XP and Advanced skills (SA, CA) cost 500 XP.
 */
class SkillTypeNaturalAptitudeCostTest {

    @ParameterizedTest
    @ValueSource(strings = {
          // Simple-Basic
          SkillType.S_ACROBATICS, SkillType.S_ANIMAL_HANDLING, SkillType.S_ARCHERY, SkillType.S_CAREER_ANY,
          SkillType.S_ANTI_MEK, SkillType.S_COMMUNICATIONS, SkillType.S_DISGUISE, SkillType.S_MEDTECH,
          SkillType.S_NAVIGATION, SkillType.S_PERCEPTION, SkillType.S_RUNNING, SkillType.S_SMALL_ARMS,
          SkillType.S_SUPPORT_WEAPONS, SkillType.S_SWIMMING, SkillType.S_THROWN_WEAPONS,
          SkillType.S_ZERO_G_OPERATIONS,
          // Complex-Basic
          SkillType.S_ACTING, SkillType.S_APPRAISAL, SkillType.S_NEGOTIATION, SkillType.S_STREETWISE,
          // No A Time of War equivalent; treated as Basic
          SkillType.S_ASTECH
    })
    void basicSkillsCostTheBasicAmount(String skillName) {
        assertEquals(BASIC_NATURAL_APTITUDE_COST, SkillType.getDefaultNaturalAptitudeCost(skillName));
    }

    @ParameterizedTest
    @ValueSource(strings = {
          // Simple-Advanced
          SkillType.S_ADMIN, SkillType.S_ARTILLERY, SkillType.S_PILOT_GVEE, SkillType.S_PILOT_NVEE,
          SkillType.S_FORGERY, SkillType.S_GUN_MEK, SkillType.S_GUN_AERO, SkillType.S_GUN_JET, SkillType.S_GUN_VEE,
          SkillType.S_GUN_SPACE, SkillType.S_GUN_BA, SkillType.S_GUN_PROTO, SkillType.S_LANGUAGES,
          SkillType.S_LEADER, SkillType.S_MARTIAL_ARTS, SkillType.S_MELEE_WEAPONS, SkillType.S_PILOT_MEK,
          SkillType.S_PILOT_AERO, SkillType.S_PILOT_JET, SkillType.S_PILOT_VTOL, SkillType.S_PILOT_SPACE,
          SkillType.S_PILOT_PROTO, SkillType.S_SLEIGHT_OF_HAND, SkillType.S_SENSOR_OPERATIONS, SkillType.S_STEALTH,
          SkillType.S_TRACKING,
          // Complex-Advanced
          SkillType.S_ART_DANCING, SkillType.S_ART_OTHER, SkillType.S_COMPUTERS, SkillType.S_CRYPTOGRAPHY,
          SkillType.S_DEMOLITIONS, SkillType.S_ESCAPE_ARTIST, SkillType.S_INTEREST_HISTORY,
          SkillType.S_INTEREST_OTHER, SkillType.S_INTERROGATION, SkillType.S_INVESTIGATION, SkillType.S_PROTOCOLS,
          SkillType.S_SCIENCE_BIOLOGY, SkillType.S_SCIENCE_OTHER, SkillType.S_SECURITY_SYSTEMS_ELECTRONIC,
          SkillType.S_SECURITY_SYSTEMS_MECHANICAL, SkillType.S_STRATEGY, SkillType.S_SURGERY, SkillType.S_SURVIVAL,
          SkillType.S_TACTICS, SkillType.S_TECH_MEK, SkillType.S_TECH_VESSEL, SkillType.S_TECH_CYBERNETICS,
          SkillType.S_TRAINING
    })
    void advancedSkillsCostTheAdvancedAmount(String skillName) {
        assertEquals(ADVANCED_NATURAL_APTITUDE_COST, SkillType.getDefaultNaturalAptitudeCost(skillName));
    }

    @Test
    void unknownSkillIsTreatedAsBasic() {
        assertEquals(BASIC_NATURAL_APTITUDE_COST, SkillType.getDefaultNaturalAptitudeCost("Not A Skill"));
        assertEquals(BASIC_NATURAL_APTITUDE_COST, SkillType.getDefaultNaturalAptitudeCost(null));
    }

    @Test
    void newSkillTypesStartWithTheirDefaultCost() {
        assertEquals(ADVANCED_NATURAL_APTITUDE_COST, SkillType.createGunneryMek().getNaturalAptitudeCost());
        assertEquals(BASIC_NATURAL_APTITUDE_COST, SkillType.createAstech().getNaturalAptitudeCost());
    }
}
