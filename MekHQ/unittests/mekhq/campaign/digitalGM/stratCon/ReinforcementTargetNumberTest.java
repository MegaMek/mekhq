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
package mekhq.campaign.digitalGM.stratCon;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.NO_ADMINISTRATION_REINFORCEMENT_MODIFIER;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.calculateReinforcementTargetNumber;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCommandRights;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the Administration modifier in {@link StratConRulesManager#calculateReinforcementTargetNumber}.
 */
class ReinforcementTargetNumberTest {
    private static final int BASE_TARGET_NUMBER = 7;

    /** A contract that adds no other modifiers: Stalemate morale, Regular enemy, House command rights. */
    private static AbstractContract neutralContract() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getEnemyForceSkill()).thenReturn(SkillLevel.REGULAR);
        when(contract.getCommandRights()).thenReturn(ContractCommandRights.HOUSE);
        return contract;
    }

    private static Person mockAdmin(Skill administration) {
        Person admin = mock(Person.class);
        when(admin.getSkill(S_ADMIN)).thenReturn(administration);
        when(admin.getSkillModifierData()).thenReturn(mock(SkillModifierData.class));
        when(admin.getOptions()).thenReturn(mock(PersonnelOptions.class));
        return admin;
    }

    @ParameterizedTest
    @CsvSource({
          // Administration experience level, expected modifier
          SkillType.EXP_ULTRA_GREEN + ", 2",
          SkillType.EXP_GREEN + ", 1",
          SkillType.EXP_REGULAR + ", 0",
          SkillType.EXP_VETERAN + ", -1",
          SkillType.EXP_ELITE + ", -2",
          SkillType.EXP_LEGENDARY + ", -4"
    })
    void testAdministrationModifier_regularAdminGivesNoModifier(int experienceLevel, int expectedModifier) {
        Skill administration = mock(Skill.class);
        when(administration.getExperienceLevel(any(SkillModifierData.class))).thenReturn(experienceLevel);

        assertEquals(BASE_TARGET_NUMBER + expectedModifier,
              calculateReinforcementTargetNumber(mockAdmin(administration), neutralContract(), BASE_TARGET_NUMBER)
                    .getValue());
    }

    @Test
    void testAdministrationModifier_noAdminIsWorseThanUltraGreen() {
        assertEquals(BASE_TARGET_NUMBER + NO_ADMINISTRATION_REINFORCEMENT_MODIFIER,
              calculateReinforcementTargetNumber(null, neutralContract(), BASE_TARGET_NUMBER).getValue());
    }

    @Test
    void testAdministrationModifier_adminWithoutSkillIsWorseThanUltraGreen() {
        assertEquals(BASE_TARGET_NUMBER + NO_ADMINISTRATION_REINFORCEMENT_MODIFIER,
              calculateReinforcementTargetNumber(mockAdmin(null), neutralContract(), BASE_TARGET_NUMBER).getValue());
    }
}
