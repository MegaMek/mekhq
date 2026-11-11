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
package mekhq.campaign.personnel.training;

import static mekhq.campaign.personnel.skills.SkillType.S_GUN_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_PILOT_MEK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.skills.SkillTrainingCategory;
import mekhq.campaign.personnel.training.TrainingPurchase.EdgeRefill;
import mekhq.campaign.personnel.training.TrainingPurchase.SkillImprovement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the small pieces the Training Console's engine is built on: counting the people a batch of purchases trains,
 * the fee categories skills are charged under, and an ability's Clan requirement on its own.
 */
class TrainingPurchaseTest {
    @Test
    void peopleAreCountedOnceHoweverManyPurchasesTheyHave() {
        TrainingFixture fixture = new TrainingFixture();
        Person first = fixture.person(0);
        Person second = fixture.person(0);

        int count = TrainingPurchase.peopleCount(List.of(new SkillImprovement(first, S_GUN_MEK),
              new SkillImprovement(first, S_PILOT_MEK), new EdgeRefill(first),
              new SkillImprovement(second, S_GUN_MEK)));

        assertEquals(2, count);
        assertEquals(0, TrainingPurchase.peopleCount(List.of()));
    }

    @ParameterizedTest
    @EnumSource(SkillTrainingCategory.class)
    void eachSkillCategoryHasTheFeeCategoryOfTheSameName(SkillTrainingCategory category) {
        assertEquals(category.name(), TrainingFeeCategory.of(category).name());
    }

    @Test
    void anAbilityWithoutAClanRequirementSuitsEveryone() {
        SpecialAbility ability = new SpecialAbility("no_clan_requirement");

        assertTrue(ability.isClanRequirementMet(true));
        assertTrue(ability.isClanRequirementMet(false));
    }
}
