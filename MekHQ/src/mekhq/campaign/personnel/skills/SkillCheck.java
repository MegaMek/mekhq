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

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.personnel.Person;

import java.time.LocalDate;

/**
 * An implementation of {@link ActionCheck} for skill checks.
 *
 * <p>This class encapsulates the logic needed to resolve a skill check for a character's
 * specific {@link SkillType}, accounting for linked attributes, untrained penalties, and natural aptitudes.</p>
 *
 * @author Hokk
 * @since 0.51.01
 */
public class SkillCheck extends ActionCheck<SkillCheck> {

    private SkillType skillType;

    /**
     * Instantiates a skill check from raw values. Prefer the other constructor variants.
     *
     * @param person       the {@link Person} performing the skill check
     * @param skillType    {@link SkillType} skill to be checked
     * @param targetNumber target number for the skill check
     */
    public SkillCheck(Person person, SkillType skillType, TargetRoll targetNumber) {
        super(person, targetNumber);
        this.skillType = skillType;
    }

    /**
     * Please see {@link Person#checkSkill(String)} and use it instead.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public SkillCheck(Person person, String skillName) {
        super(person, SkillCheckUtility.determineTargetNumber(person, SkillType.getType(skillName)));
        this.skillType = SkillType.getType(skillName);
    }

    /**
     * Please see {@link Person#checkSkill(String, boolean, boolean, LocalDate)} and use it instead.
     */
    public SkillCheck(Person person, String skillName,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate date) {
        super(person, SkillCheckUtility.determineTargetNumber(
              person, SkillType.getType(skillName), isUseAgingEffects, isClanCampaign, date));
        this.skillType = SkillType.getType(skillName);
    }

    @Override
    protected SkillCheck getThis() {
        return this;
    }

    @Override
    protected boolean isCountUp() {
        return skillType.isCountUp();
    }

    @Override
    protected boolean hasNaturalAptitude() {
        Skill skill = person.getSkill(skillType.getName());
        return skill != null &&  skill.getHasNaturalAptitude();
    }

    @Override
    protected String getActionName() {
        return skillType.getName();
    }
}
