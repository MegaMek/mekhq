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

import static mekhq.campaign.personnel.skills.SkillDeprecationTool.DEPRECATED_SKILLS;
import static mekhq.campaign.personnel.skills.SkillType.getRoleplaySkills;
import static mekhq.campaign.personnel.skills.SkillType.getUtilitySkills;

import java.util.Objects;

import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * Represents a class which can generate new {@link Skill} objects for a {@link Person}.
 */
public abstract class AbstractSkillGenerator {
    private RandomSkillPreferences randomSkillPreferences;

    protected AbstractSkillGenerator(final RandomSkillPreferences randomSkillPreferences) {
        this.randomSkillPreferences = randomSkillPreferences;
    }

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
     * Generates skills for a {@link Person} given their experience level.
     *
     * @param campaign The {@link Campaign} the person is a part of
     * @param person   The {@link Person} to add skills.
     * @param expLvl   The experience level of the person (e.g. {@link SkillType#EXP_GREEN}).
     */
    public abstract void generateSkills(Campaign campaign, Person person, int expLvl);

    public abstract void generateTraits(Person person);

    /**
     * Generates attributes for a specified person based on their phenotype.
     *
     * @param person The {@link Person} for whom attributes are to be generated.
     */
    public abstract void generateAttributes(Person person);

    /**
     * Generates the default skills for a {@link Person} based on their primary role.
     *
     * @param person       The {@link Person} to add default skills.
     * @param primaryRole  The primary role of the person
     * @param expLvl       The experience level of the person (e.g. {@link SkillType#EXP_GREEN}).
     * @param bonus        The bonus to use for the default skills.
     * @param rollModifier A roll modifier to apply to any randomization's.
     */
    protected void generateDefaultSkills(Person person, PersonnelRole primaryRole, int expLvl, int bonus,
          int rollModifier) {
        // For default skills, we just want the base skills, excluding any campaign option related supplementary
        // skills, such as artillery or admin for techs.
        for (String skillName : primaryRole.getSkillsForProfession()) {
            addSkill(person, skillName, expLvl, randomSkillPreferences.randomizeSkill(), bonus, rollModifier);
        }
    }

    public void generateArtillerySkill(final Person person) {
        generateArtillerySkill(person, getPhenotypeBonus(person));
    }

    protected void generateArtillerySkill(final Person person, final int bonus) {
        final int experienceLevel = Utilities.generateExpLevel(randomSkillPreferences.getArtilleryBonus());
        if (experienceLevel > SkillType.EXP_ULTRA_GREEN) {
            addSkill(person, SkillType.S_ARTILLERY, experienceLevel, randomSkillPreferences.randomizeSkill(), bonus);
        }
    }

    public void generateRoleplaySkills(final Person person) {
        for (SkillType skillType : getRoleplaySkills()) {
            if (DEPRECATED_SKILLS.contains(skillType)) {
                continue;
            }

            // No double-dipping
            if (person.hasSkill(skillType.getName())) {
                continue;
            }

            int roleplaySkillLevel = Utilities.generateExpLevel(randomSkillPreferences.getRoleplaySkillModifier());
            if (roleplaySkillLevel > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, skillType.getName(), roleplaySkillLevel, randomSkillPreferences.randomizeSkill(), 0);
            }
        }
    }

    public void generateUtilitySkills(final Person person) {
        for (SkillType skillType : getUtilitySkills()) {
            if (DEPRECATED_SKILLS.contains(skillType)) {
                continue;
            }

            // No double-dipping
            if (person.hasSkill(skillType.getName())) {
                continue;
            }

            int utilitySkillLevel = Utilities.generateExpLevel(randomSkillPreferences.getRoleplaySkillModifier());
            if (utilitySkillLevel > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, skillType.getName(), utilitySkillLevel, randomSkillPreferences.randomizeSkill(), 0);
            }
        }
    }

    public static void addSkill(Person person, String skillName, int level, int bonus) {
        person.addSkill(skillName, new Skill(skillName, level, bonus));
    }

    protected static void addSkill(Person person, String skillName, int experienceLevel, boolean randomizeLevel,
          int bonus) {
        addSkill(person, skillName, experienceLevel, randomizeLevel, bonus, 0);
    }

    protected static void addSkill(Person person, String skillName, int experienceLevel, boolean randomizeLevel,
          int bonus, int rollMod) {
        if (randomizeLevel) {
            person.addSkill(skillName, Skill.randomizeLevel(skillName, experienceLevel, bonus, rollMod));
        } else {
            person.addSkill(skillName, Skill.createFromExperience(skillName, experienceLevel, bonus));
        }
    }

    /**
     * Gets the clan phenotype bonus for a {@link Person}, if applicable.
     *
     * @param person A {@link Person} to calculate a phenotype bonus.
     *
     * @return The bonus to a {@link Skill} due to clan phenotypes matching the primary role.
     */
    protected int getPhenotypeBonus(Person person) {
        if (!person.isClanPersonnel()) {
            return 0;
        }

        switch (person.getPrimaryRole()) {
            case MEKWARRIOR:
            case LAM_PILOT:
                if (person.getPhenotype().isMekWarrior()) {
                    return 1;
                }
                break;
            case GROUND_VEHICLE_DRIVER:
            case NAVAL_VEHICLE_DRIVER:
            case VTOL_PILOT:
            case VEHICLE_GUNNER:
            case VEHICLE_CREW:
                if (person.getPhenotype().isVehicle()) {
                    return 1;
                }
                break;
            case AEROSPACE_PILOT:
            case CONVENTIONAL_AIRCRAFT_PILOT:
                if (person.getPhenotype().isAerospace()) {
                    return 1;
                }
                break;
            case PROTOMEK_PILOT:
                if (person.getPhenotype().isProtoMek()) {
                    return 1;
                }
            case BATTLE_ARMOUR:
                if (person.getPhenotype().isElemental()) {
                    return 1;
                }
                break;
            case VESSEL_PILOT:
            case VESSEL_GUNNER:
            case VESSEL_CREW:
            case VESSEL_NAVIGATOR:
                if (person.getPhenotype().isNaval()) {
                    return 1;
                }
            default:
                break;
        }

        return 0;
    }
}
