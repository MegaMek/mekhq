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
package mekhq.campaign.personnel;

import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.personnel.generator.AbstractSkillGenerator.addSkill;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import megamek.common.enums.SkillLevel;
import megamek.common.options.IOption;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.AbstractSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.DefaultSpecialAbilityGenerator;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * Utility class that provides methods for managing and modifying the skills, loyalty, and advantages of personnel in
 * the campaign, based on their roles and experience levels.
 */
public class PersonUtility {

    /**
     * Re-rolls the Special Piloting Abilities (SPAs) of a person based on their experience level.
     *
     * <p>This clears all existing SPAs for the person and generates new ones that align with the
     * specified experience level.</p>
     *
     * @param campaign   the current {@link Campaign} instance.
     * @param person     the {@link Person} whose SPAs are being re-rolled.
     * @param skillLevel the {@link SkillLevel} of the person, used to determine the new SPAs.
     */
    public static void reRollAdvantages(Campaign campaign, Person person, SkillLevel skillLevel) {
        Enumeration<IOption> options = new PersonnelOptions().getOptions(PersonnelOptions.LVL3_ADVANTAGES);

        for (IOption option : Collections.list(options)) {
            person.getOptions().getOption(option.getName()).clearValue();
        }

        int skillLevelValue = skillLevel.getExperienceLevel();
        if (skillLevelValue > 0) {
            AbstractSpecialAbilityGenerator specialAbilityGenerator = new DefaultSpecialAbilityGenerator();
            specialAbilityGenerator.setSkillPreferences(new RandomSkillPreferences());
            specialAbilityGenerator.generateSpecialAbilities(campaign, person, skillLevelValue);
        }
    }

    /**
     * Re-rolls the loyalty of a person based on their experience level.
     *
     * <p>The loyalty score is determined by a die roll and is influenced by the person's skill
     * level. A higher skill level generally corresponds to lower (worse) loyalty values.</p>
     *
     * @param person     the {@link Person} whose loyalty is being re-rolled.
     * @param skillLevel the {@link SkillLevel} of the person, which affects the loyalty value.
     */
    public static void reRollLoyalty(Person person, SkillLevel skillLevel) {
        int skillLevelValue = skillLevel.getExperienceLevel();

        if (skillLevelValue <= 0) {
            person.setLoyalty(d6(3) + 2);
        } else if (skillLevelValue == 1) {
            person.setLoyalty(d6(3) + 1);
        } else {
            person.setLoyalty(d6(3));
        }
    }

    /**
     * @deprecated use
     *       {@link #overrideSkills(boolean, boolean, boolean, boolean, boolean, Person, PersonnelRole, SkillLevel)}
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public static void overrideSkills(boolean isAdminsHaveNegotiation, boolean isDoctorsUseAdministration,
          boolean isTechsUseAdministration, boolean isUseExtraRandom, Person person, PersonnelRole primaryRole,
          SkillLevel skillLevel) {
        overrideSkills(isAdminsHaveNegotiation,
              isDoctorsUseAdministration,
              isTechsUseAdministration,
              false,
              isUseExtraRandom,
              person,
              primaryRole,
              skillLevel);

    }

    /**
     * Assigns and overrides the skills of a {@link Person} based on their role, experience level, and campaign-specific
     * settings.
     *
     * <p>This method determines the appropriate skill set for the given person by consulting their primary role
     * and campaign preferences. The chosen skills are then assigned to the person, with optional randomization of their
     * levels if specified.</p>
     *
     * @param isAdminsHaveNegotiation    if {@code true}, administrators are assigned the Negotiation skill.
     * @param isDoctorsUseAdministration if {@code true}, doctors are given the Administration skill.
     * @param isTechsUseAdministration   if {@code true}, technicians are given the Administration skill.
     * @param isUseArtillery             if {@code true}, roles that can use it are assigned Artillery skills.
     * @param isUseExtraRandom           if {@code true}, adds randomization to the assigned skill levels.
     * @param person                     the {@link Person} whose skills will be overridden.
     * @param primaryRole                the {@link PersonnelRole} used to determine which skills to assign.
     * @param skillLevel                 the {@link SkillLevel} to use as a baseline for assigned skills.
     */
    public static void overrideSkills(boolean isAdminsHaveNegotiation, boolean isDoctorsUseAdministration,
          boolean isTechsUseAdministration, boolean isUseArtillery, boolean isUseExtraRandom, Person person,
          PersonnelRole primaryRole, SkillLevel skillLevel) {
        List<String> skills = primaryRole.getSkillsForProfession(isAdminsHaveNegotiation,
              isDoctorsUseAdministration,
              isTechsUseAdministration,
              isUseArtillery);

        if (!skills.isEmpty()) {
            addSkillsAndRandomize(person, skills, skillLevel, isUseExtraRandom);
        }
    }

    /**
     * Adds specified skills to a person and optionally applies randomization to those skills.
     *
     * <p>The randomization process can slightly increase or decrease skill levels based on dice
     * rolls.</p>
     *
     * @param person     the {@link Person} to whom the skills are added.
     * @param skills     a list of skill names to add to the person.
     * @param skillLevel the {@link SkillLevel} to which the skills should be set.
     * @param randomize  {@code true} if the skill levels should be randomized after being added; {@code false}
     *                   otherwise.
     */
    private static void addSkillsAndRandomize(Person person, List<String> skills, SkillLevel skillLevel,
          boolean randomize) {
        for (String skill : skills) {
            addSkillFixedExperienceLevel(person, skill, skillLevel);
        }

        if (randomize) {
            randomizeSkills(person, skills);
        }
    }

    /**
     * Randomizes the skill levels of the given person within a specific range.
     *
     * <p>Each skill's level may increase, decrease, or stay the same based on a die roll.</p>
     *
     * @param person the {@link Person} whose skills are being randomized.
     * @param skills a list of skill names that should be randomized.
     */
    private static void randomizeSkills(Person person, List<String> skills) {
        for (String skillName : skills) {
            Skill skill = person.getSkill(skillName);

            if (skill == null) {
                continue;
            }

            int roll = d6(); // Roll once for the skill
            int adjustedLevel = skill.getLevel() + (roll == 6 ? 1 : roll == 1 ? -1 : 0);
            skill.setLevel(clamp(adjustedLevel, 0, 10));
        }
    }

    /**
     * Adds a specific skill to a person with a fixed experience level.
     *
     * <p>If the person already has the skill, their existing bonus value is retained.
     * Otherwise, the skill is added with the specified experience level.</p>
     *
     * @param person     the {@link Person} to whom the skill is being added.
     * @param skillName  the name of the skill to add.
     * @param skillLevel the {@link SkillLevel} used to set the skill's experience level.
     */
    private static void addSkillFixedExperienceLevel(Person person, String skillName, SkillLevel skillLevel) {
        SkillType skillType = SkillType.getType(skillName);
        int targetLevel = skillType.getLevelFromExperience(skillLevel.getAdjustedValue());

        int bonus = 0;
        Skill skill = person.getSkill(skillName);
        if (skill != null) {
            bonus = skill.getBonus();
        }

        addSkill(person, skillName, targetLevel, bonus);
    }
}
