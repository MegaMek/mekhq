/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.generator;

import java.util.Objects;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillUtilities;

/**
 * Represents a class which can generate new Special Abilities for a {@link Person}.
 */
public abstract class AbstractSpecialAbilityGenerator {

    private RandomSkillPreferences randomSkillPreferences = new RandomSkillPreferences();

    /**
     * Gets the {@link RandomSkillPreferences}.
     *
     * @return The {@link RandomSkillPreferences} to use.
     */
    public RandomSkillPreferences getSkillPreferences() {
        return randomSkillPreferences;
    }

    /**
     * Sets the {@link RandomSkillPreferences}.
     *
     * @param skillPreferences A {@link RandomSkillPreferences} to use.
     */
    public void setSkillPreferences(RandomSkillPreferences skillPreferences) {
        randomSkillPreferences = Objects.requireNonNull(skillPreferences);
    }

    /**
     * Generates special abilities for the {@link Person} given their experience level.
     *
     * @param campaign The {@link Campaign} the person is a part of
     * @param person   The {@link Person} to add special abilities.
     * @param expLvl   The experience level of the person (e.g. {@link SkillUtilities#SKILL_LEVEL_GREEN}).
     *
     * @return A value indicating whether a special ability was assigned.
     */
    public abstract boolean generateSpecialAbilities(Campaign campaign, Person person, int expLvl);
}
