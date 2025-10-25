/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;

/**
 * Utility class for working with infantry gunnery-related skills.
 *
 * <p>Provides a static list of relevant infantry gunnery skill types as well as a method to identify the
 * highest-level infantry gunnery skill possessed by a given {@link Person}.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class InfantryGunnerySkills {
    /**
     * Unmodifiable list of all skill type strings considered to be infantry gunnery skills.
     */
    public static final List<String> INFANTRY_GUNNERY_SKILLS = List.of(SkillType.S_ARCHERY, SkillType.S_SMALL_ARMS,
          SkillType.S_DEMOLITIONS, SkillType.S_MARTIAL_ARTS, SkillType.S_MELEE_WEAPONS, SkillType.S_THROWN_WEAPONS,
          SkillType.S_SUPPORT_WEAPONS);

    /**
     * Determines the name of the infantry gunnery skill with the highest total skill level for the given person.
     *
     * <p>Iterates through all skills in {@link #INFANTRY_GUNNERY_SKILLS}, and finds the skill that the person
     * possesses with the highest skill level (according to {@code getTotalSkillLevel}). If the person does not possess
     * any of these skills, {@code null} is returned.</p>
     *
     * <p>If {@code useSmallArmsOnly} is {@code true}, only the Small Arms skill is considered. Otherwise, all
     * available infantry gunnery skills are evaluated.</p>
     *
     * @param person           the {@link Person} whose infantry gunnery skills to search
     * @param useSmallArmsOnly {@code true} if only the Small Arms skill should be considered; {@code false} if all
     *                         infantry gunnery skills should be evaluated
     *
     * @return the skill name (as defined in {@link SkillType}) for the person's best infantry gunnery skill, or
     *       {@code null} if no such skill is present
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static @Nullable String getBestInfantryGunnerySkill(Person person, boolean useSmallArmsOnly) {
        if (useSmallArmsOnly) {
            String skillName = SkillType.S_SMALL_ARMS;
            return person.hasSkill(skillName) ? skillName : null;
        }

        return getGunnerySkill(person);
    }

    /**
     * Searches all infantry gunnery skills for the given person and returns the name of the skill with the highest
     * total skill level.
     *
     * @param person the {@link Person} to examine
     *
     * @return the name of the best infantry gunnery skill, or {@code null} if none is present
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getGunnerySkill(Person person) {
        PersonnelOptions options = person.getOptions();
        Attributes attributes = person.getATOWAttributes();

        int highestLevel = Integer.MIN_VALUE;
        String bestSkill = null;
        for (String skillName : INFANTRY_GUNNERY_SKILLS) {
            if (person.hasSkill(skillName)) {
                int skillLevel = person.getSkill(skillName).getTotalSkillLevel(options, attributes);

                if (skillLevel > highestLevel) {
                    highestLevel = skillLevel;
                    bestSkill = skillName;
                }
            }
        }

        return bestSkill;
    }
}
