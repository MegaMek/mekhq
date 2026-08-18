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

import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_GUNNERY;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.COMBAT_PILOTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.*;
import mekhq.campaign.personnel.PersonnelOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Coverage for the combat Chassis Familiarity bonus gate in {@link Skill#getSPAModifiers(PersonnelOptions, int, int)}.
 *
 * <p>Every unit family the feature treats as eligible must actually receive the bonus, in both directions - Normal's
 * bonuses and Hard's penalties. The gate therefore has to admit each skill a unit can be driven or fought with, not
 * just the Mek/vehicle/aero ones: large craft use Piloting/Gunnery/Spacecraft, infantry use Anti-Mek with Small Arms
 * (or another infantry gunnery skill), Battle Armor uses Gunnery/Battle Armor, ProtoMeks use Gunnery/ProtoMek, and any
 * of them may fire Artillery.</p>
 */
class SkillChassisFamiliarityBonusTest {
    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    /**
     * The SPA modifier for {@code skillName} when the caller supplies {@code familiarityBonus}. A character with no
     * SPAs and no reputation contributes nothing else, so the result is the familiarity bonus or zero.
     */
    private static int modifierFor(String skillName, int familiarityBonus) {
        Skill skill = new Skill(skillName, 0, 0);
        return skill.getSPAModifiers(new PersonnelOptions(), 0, familiarityBonus);
    }

    private static void assertBonusApplied(String skillName) {
        assertEquals(1, modifierFor(skillName, 1), skillName + " should receive a Normal-mode bonus");
        assertEquals(-1, modifierFor(skillName, -1), skillName + " should receive a Hard-mode penalty");
    }

    private static void assertBonusIgnored(String skillName) {
        assertEquals(0, modifierFor(skillName, 1), skillName + " should not receive a familiarity bonus");
        assertEquals(0, modifierFor(skillName, -1), skillName + " should not receive a familiarity penalty");
    }

    /**
     * Regression: these seven were dropped by the old Mek/vehicle/aero-only gate, so large craft, infantry, Battle
     * Armor, ProtoMek and artillery crews accrued familiarity and were shown bonuses they never received.
     */
    @ParameterizedTest
    @ValueSource(strings = { SkillType.S_ANTI_MEK,
                             SkillType.S_SMALL_ARMS,
                             SkillType.S_GUN_BA,
                             SkillType.S_GUN_PROTO,
                             SkillType.S_PILOT_SPACE,
                             SkillType.S_GUN_SPACE,
                             SkillType.S_ARTILLERY })
    void previouslyDroppedCombatSkills_receiveTheBonus(String skillName) {
        assertBonusApplied(skillName);
    }

    /**
     * The families that already worked keep working.
     */
    @ParameterizedTest
    @ValueSource(strings = { SkillType.S_PILOT_MEK,
                             SkillType.S_GUN_MEK,
                             SkillType.S_PILOT_GVEE,
                             SkillType.S_PILOT_NVEE,
                             SkillType.S_PILOT_VTOL,
                             SkillType.S_GUN_VEE,
                             SkillType.S_PILOT_AERO,
                             SkillType.S_GUN_AERO,
                             SkillType.S_PILOT_JET,
                             SkillType.S_GUN_JET })
    void mekVehicleAndAeroSkills_receiveTheBonus(String skillName) {
        assertBonusApplied(skillName);
    }

    /**
     * A conventional infantry platoon's gunnery resolves to whichever of these its troopers are best with, so each is a
     * unit's gunnery skill in its own right.
     */
    @ParameterizedTest
    @ValueSource(strings = { SkillType.S_ARCHERY,
                             SkillType.S_DEMOLITIONS,
                             SkillType.S_MARTIAL_ARTS,
                             SkillType.S_MELEE_WEAPONS,
                             SkillType.S_THROWN_WEAPONS,
                             SkillType.S_SUPPORT_WEAPONS })
    void infantryGunneryAlternatives_receiveTheBonus(String skillName) {
        for (String infantryGunnerySkill : InfantryGunnerySkills.INFANTRY_GUNNERY_SKILLS) {
            if (infantryGunnerySkill.equals(skillName)) {
                assertBonusApplied(skillName);
                return;
            }
        }
        throw new AssertionError(skillName + " is no longer an infantry gunnery skill");
    }

    /**
     * Nothing a unit is not driven or fought with receives the bonus, however it is passed.
     */
    @ParameterizedTest
    @ValueSource(strings = { SkillType.S_ADMIN,
                             SkillType.S_TECH_MEK,
                             SkillType.S_SURGERY,
                             SkillType.S_NEGOTIATION,
                             SkillType.S_LEADER,
                             SkillType.S_STRATEGY })
    void nonCombatSkills_ignoreTheBonus(String skillName) {
        assertBonusIgnored(skillName);
    }

    /**
     * The rule in one assertion: a skill receives the bonus exactly when it is a combat piloting or gunnery skill. A
     * newly added combat skill is covered automatically; a newly added support skill stays excluded.
     */
    @Test
    void everyCombatSkill_andOnlyCombatSkills_receiveTheBonus() {
        for (String skillName : SkillType.getSkillList()) {
            SkillType skillType = SkillType.getType(skillName);
            boolean isCombatSkill = skillType.isSubTypeOf(COMBAT_PILOTING) || skillType.isSubTypeOf(COMBAT_GUNNERY);

            if (isCombatSkill) {
                assertBonusApplied(skillName);
            } else {
                assertBonusIgnored(skillName);
            }
        }
    }

    /**
     * Ties the gate back to the unit families the feature declares eligible: whatever skill the game picks to drive or
     * shoot an eligible unit with must receive the bonus.
     */
    @Test
    void everySkillAnEligibleUnitIsFoughtWith_receivesTheBonus() {
        Tank groundVehicle = mock(Tank.class);
        when(groundVehicle.getMovementMode()).thenReturn(EntityMovementMode.TRACKED);
        Tank navalVessel = mock(Tank.class);
        when(navalVessel.getMovementMode()).thenReturn(EntityMovementMode.NAVAL);
        VTOL vtol = mock(VTOL.class);
        when(vtol.getMovementMode()).thenReturn(EntityMovementMode.VTOL);

        Entity[] eligibleEntities = { mock(Mek.class),
                                      groundVehicle,
                                      navalVessel,
                                      vtol,
                                      mock(ConvFighter.class),
                                      mock(Dropship.class),
                                      mock(Jumpship.class),
                                      mock(Infantry.class),
                                      mock(BattleArmor.class),
                                      mock(ProtoMek.class) };

        for (Entity entity : eligibleEntities) {
            assertBonusApplied(SkillType.getDrivingSkillFor(entity));
            assertBonusApplied(SkillType.getGunnerySkillFor(entity));
        }
    }
}
