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
 */
package mekhq.campaign.personnel;

import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.Compute.d6;
import static mekhq.campaign.personnel.SkillType.*;
import static mekhq.campaign.personnel.enums.PersonnelRole.*;
import static mekhq.campaign.personnel.generator.AbstractSkillGenerator.addSkill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import megamek.common.enums.SkillLevel;
import megamek.common.options.IOption;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.AbstractSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.DefaultSpecialAbilityGenerator;

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
     * <p>The loyalty score is determined by a dice roll and is influenced by the person's skill
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
     * @deprecated use {@link #overrideSkills(boolean, boolean, boolean, boolean, boolean, Person, PersonnelRole, SkillLevel)}
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public static void overrideSkills(boolean isAdminsHaveNegotiation, boolean isAdminsHaveScrounge, boolean isUseExtraRandom, Person person, PersonnelRole primaryRole, SkillLevel skillLevel) {
        overrideSkills(isAdminsHaveNegotiation,
              isAdminsHaveScrounge,
              false,
              false,
              isUseExtraRandom,
              person,
              primaryRole,
              skillLevel);
    }

    /**
     * Overrides the skills of a {@link Person} based on their role, experience level, and various conditions.
     * This method applies specific skill sets to the person depending on their {@link PersonnelRole}, optionally
     * including administrator and tech specific skills or applying randomization to skill levels.
     *
     * <p>Roles are mapped to corresponding skills, administrators and techs can be optionally given additional
     * skills like Negotiation and Scrounge. Additional randomization can also be applied to skill levels.</p>
     *
     * <p>For roles not explicitly defined, no skills are applied by default.</p>
     *
     * @param isAdminsHaveNegotiation Whether administrators should possess the Negotiation skill.
     * @param isAdminsHaveScrounge    Whether administrators should possess the Scrounge skill.
     * @param isDoctorsUseAdministration Whether doctors should possess the Administration skill in addition to medical skills.
     * @param isTechsUseAdministration   Whether techs should possess the Administration skill in addition to technical skills.
     * @param isUseExtraRandom        Whether additional randomization should be applied to adjust skill levels.
     * @param person                  The {@link Person} whose skills are being overridden and assigned.
     * @param primaryRole             The {@link PersonnelRole} of the person, which dictates the skill mapping.
     * @param skillLevel              The {@link SkillLevel} corresponding to the experience level of the person.
     *                                Determines specific values when adding skills to the person.
     */
    public static void overrideSkills(boolean isAdminsHaveNegotiation, boolean isAdminsHaveScrounge, boolean isDoctorsUseAdministration, boolean isTechsUseAdministration, boolean isUseExtraRandom, Person person, PersonnelRole primaryRole, SkillLevel skillLevel) {
        // Role-to-Skill Mapping
        Map<PersonnelRole, List<String>> roleSkills = Map.ofEntries(Map.entry(MEKWARRIOR,
                    List.of(S_PILOT_MEK, S_GUN_MEK)),
              Map.entry(LAM_PILOT, List.of(S_PILOT_MEK, S_GUN_MEK, S_PILOT_AERO, S_GUN_AERO)),
              Map.entry(GROUND_VEHICLE_DRIVER, List.of(S_PILOT_GVEE, S_GUN_VEE)),
              Map.entry(NAVAL_VEHICLE_DRIVER, List.of(S_PILOT_NVEE, S_GUN_VEE)),
              Map.entry(VTOL_PILOT, List.of(S_PILOT_VTOL, S_GUN_VEE)),
              Map.entry(VEHICLE_GUNNER, List.of(S_GUN_VEE)),
              Map.entry(VEHICLE_CREW, List.of(S_TECH_MECHANIC)),
              Map.entry(MECHANIC,
                    isTechsUseAdministration ? List.of(S_TECH_MECHANIC, S_ADMIN) : List.of(S_TECH_MECHANIC)),
              Map.entry(AEROSPACE_PILOT, List.of(S_PILOT_AERO, S_GUN_AERO)),
              Map.entry(CONVENTIONAL_AIRCRAFT_PILOT, List.of(S_PILOT_JET, S_GUN_JET)),
              Map.entry(PROTOMEK_PILOT, List.of(S_GUN_PROTO)),
              Map.entry(BATTLE_ARMOUR, List.of(S_GUN_BA, S_ANTI_MEK)),
              Map.entry(SOLDIER, List.of(S_SMALL_ARMS)),
              Map.entry(VESSEL_PILOT, List.of(S_PILOT_SPACE)),
              Map.entry(VESSEL_GUNNER, List.of(S_GUN_SPACE)),
              Map.entry(VESSEL_CREW,
                    isTechsUseAdministration ? List.of(S_TECH_VESSEL, S_ADMIN) : List.of(S_TECH_VESSEL)),
              Map.entry(VESSEL_NAVIGATOR, List.of(S_NAV)),
              Map.entry(MEK_TECH, isTechsUseAdministration ? List.of(S_TECH_MEK, S_ADMIN) : List.of(S_TECH_MEK)),
              Map.entry(AERO_TEK, isTechsUseAdministration ? List.of(S_TECH_AERO, S_ADMIN) : List.of(S_TECH_AERO)),
              Map.entry(BA_TECH, isTechsUseAdministration ? List.of(S_TECH_BA, S_ADMIN) : List.of(S_TECH_BA)),
              Map.entry(DOCTOR, isDoctorsUseAdministration ? List.of(S_DOCTOR, S_ADMIN) : List.of(S_DOCTOR)));

        // Add admin-specific logic
        if (primaryRole == ADMINISTRATOR_COMMAND ||
                  primaryRole == ADMINISTRATOR_LOGISTICS ||
                  primaryRole == ADMINISTRATOR_TRANSPORT ||
                  primaryRole == ADMINISTRATOR_HR) {
            List<String> adminSkills = new ArrayList<>();

            adminSkills.add(S_ADMIN);
            if (isAdminsHaveNegotiation) {
                adminSkills.add(S_NEG);
            }
            if (isAdminsHaveScrounge) {
                adminSkills.add(S_SCROUNGE);
            }

            addSkillsAndRandomize(person, adminSkills, skillLevel, isUseExtraRandom);
            return;
        }

        // Handle Normal Role Skills
        List<String> skills = roleSkills.getOrDefault(primaryRole, List.of());
        addSkillsAndRandomize(person, skills, skillLevel, isUseExtraRandom);
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
    private static void addSkillsAndRandomize(Person person, List<String> skills, SkillLevel skillLevel, boolean randomize) {
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
     * <p>Each skill's level may increase, decrease, or stay the same based on a dice roll.</p>
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
     * @param skill      the name of the skill to add.
     * @param skillLevel the {@link SkillLevel} used to set the skill's experience level.
     */
    private static void addSkillFixedExperienceLevel(Person person, String skill, SkillLevel skillLevel) {
        int bonus = 0;

        if (person.hasSkill(skill)) {
            bonus = person.getSkill(skill).getBonus();
        }

        addSkill(person, skill, skillLevel.getExperienceLevel(), bonus);
    }
}
