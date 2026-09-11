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

import static mekhq.campaign.personnel.skills.SkillModifierData.IGNORE_AGE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Map;

import mekhq.campaign.personnel.PersonnelOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the equipment-kit bonus carried by {@link SkillModifierData} feeds into {@link Skill}'s final skill
 * value, so a kit's bonus reaches every check made through the {@code SkillModifierData} path — but is kept out of the
 * skill level and experience rating, which a kit must not inflate.
 */
class SkillEquipmentKitBonusTest {

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    private static SkillModifierData modifierData(Map<String, Integer> kitBonuses) {
        return new SkillModifierData(new PersonnelOptions(), new Attributes(), 0, new ArrayList<>(), IGNORE_AGE,
              kitBonuses);
    }

    private static int delta(Skill skill, int bonus) {
        int base = skill.getFinalSkillValue(modifierData(Map.of()));
        int boosted = skill.getFinalSkillValue(modifierData(Map.of(skill.getType().getName(), bonus)));
        return skill.getType().isCountUp() ? (boosted - base) : (base - boosted);
    }

    @Test
    void aMatchingKitBonusEasesTheSkillByThatMuch() {
        Skill surgery = new Skill(SkillType.S_SURGERY, 4, 0);
        assertEquals(2, delta(surgery, 2), "a +2 medical-kit bonus should ease Surgery by 2");
    }

    @Test
    void administrationTakesItsOwnBonus() {
        Skill admin = new Skill(SkillType.S_ADMIN, 4, 0);
        assertEquals(1, delta(admin, 1), "a +1 computer-kit bonus should ease Administration by 1");
    }

    @Test
    void aSkillNotInTheKitMapIsUnaffected() {
        Skill negotiation = new Skill(SkillType.S_NEGOTIATION, 4, 0);
        // The person carries a Survival kit only; Negotiation must not move.
        int base = negotiation.getFinalSkillValue(modifierData(Map.of()));
        int other = negotiation.getFinalSkillValue(modifierData(Map.of(SkillType.S_SURVIVAL, 2)));
        assertEquals(base, other, "a kit for another skill should not change Negotiation");
    }

    @Test
    void anEmptyKitMapChangesNothing() {
        Skill surgery = new Skill(SkillType.S_SURGERY, 4, 0);
        assertEquals(0, delta(surgery, 0));
    }

    @Test
    void aKitBonusDoesNotChangeTheTotalSkillLevel() {
        // A kit modifies the skill check (final skill value) only; the underlying skill level, used for a character's
        // experience rating, must be untouched by any kit they happen to carry.
        Skill surgery = new Skill(SkillType.S_SURGERY, 4, 0);
        int base = surgery.getTotalSkillLevel(modifierData(Map.of()));
        int withKit = surgery.getTotalSkillLevel(modifierData(Map.of(surgery.getType().getName(), 2)));
        assertEquals(base, withKit, "a medical-kit bonus must not raise the Surgery skill level");
    }

    @Test
    void aKitBonusDoesNotChangeTheExperienceLevel() {
        Skill surgery = new Skill(SkillType.S_SURGERY, 4, 0);
        int base = surgery.getExperienceLevel(modifierData(Map.of()));
        int withKit = surgery.getExperienceLevel(modifierData(Map.of(surgery.getType().getName(), 2)));
        assertEquals(base, withKit, "a medical-kit bonus must not raise the Surgery experience level");
    }
}
