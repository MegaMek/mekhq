/*
 * Copyright (C) 2019 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.generator;

import java.util.Objects;

import mekhq.Utilities;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;

/**
 * Represents a class which can generate new {@link Skill} objects
 * for a {@link Person}.
 */
public abstract class AbstractSkillGenerator {

    private RandomSkillPreferences rskillPrefs = new RandomSkillPreferences();

    /**
     * Gets the {@link RandomSkillPreferences}.
     * @return The {@link RandomSkillPreferences} to use.
     */
    public RandomSkillPreferences getSkillPreferences() {
        return rskillPrefs;
    }

    /**
     * Sets the {@link RandomSkillPreferences}.
     * @param skillPreferences A {@link RandomSkillPreferences} to use.
     */
    public void setSkillPreferences(RandomSkillPreferences skillPreferences) {
        rskillPrefs = Objects.requireNonNull(skillPreferences);
    }

    /**
     * Generates skills for a {@link Person} given their experience level.
     * @param person The {@link Person} to add skills.
     * @param expLvl The experience level of the person (e.g. {@link SkillType#EXP_GREEN}).
     */
    public abstract void generateSkills(Person person, int expLvl);

    /**
     * Generates the default skills for a {@link Person} based on their primary role.
     * @param person The {@link Person} to add default skills.
     * @param primaryRole The primary role of the person (e.g. {@link Person#T_MECHWARRIOR}).
     * @param expLvl The experience level of the person (e.g. {@link SkillType#EXP_GREEN}).
     * @param bonus The bonus to use for the default skills.
     * @param rollModifier A roll modifier to apply to any randomizations.
     */
    protected void generateDefaultSkills(Person person, int primaryRole, int expLvl, int bonus, int rollModifier) {
        switch (primaryRole) {
            case (Person.T_MECHWARRIOR):
                addSkill(person, SkillType.S_PILOT_MECH, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_MECH, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_GVEE_DRIVER):
                addSkill(person, SkillType.S_PILOT_GVEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_NVEE_DRIVER):
                addSkill(person, SkillType.S_PILOT_NVEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_VTOL_PILOT):
                addSkill(person, SkillType.S_PILOT_VTOL, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_VEE_GUNNER):
                addSkill(person, SkillType.S_GUN_VEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_CONV_PILOT):
                addSkill(person, SkillType.S_PILOT_JET, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_JET, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_AERO_PILOT):
                addSkill(person, SkillType.S_PILOT_AERO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_AERO, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_PROTO_PILOT):
                addSkill(person, SkillType.S_GUN_PROTO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_BA):
                addSkill(person, SkillType.S_GUN_BA, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_ANTI_MECH, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_SMALL_ARMS, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_INFANTRY):
                if (Utilities.rollProbability(rskillPrefs.getAntiMekProb())) {
                    addSkill(person, SkillType.S_ANTI_MECH, expLvl,
                            rskillPrefs.randomizeSkill(), bonus, rollModifier);
                }
                addSkill(person, SkillType.S_SMALL_ARMS, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_SPACE_PILOT):
                addSkill(person, SkillType.S_PILOT_SPACE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_SPACE_CREW):
                addSkill(person, SkillType.S_TECH_VESSEL, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_SPACE_GUNNER):
                addSkill(person, SkillType.S_GUN_SPACE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_NAVIGATOR):
                addSkill(person, SkillType.S_NAV, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_MECH_TECH):
                addSkill(person, SkillType.S_TECH_MECH, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_MECHANIC):
            case Person.T_VEHICLE_CREW:
                addSkill(person, SkillType.S_TECH_MECHANIC, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_AERO_TECH):
                addSkill(person, SkillType.S_TECH_AERO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_BA_TECH):
                addSkill(person, SkillType.S_TECH_BA, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_ASTECH):
                addSkill(person, SkillType.S_ASTECH, 0, 0);
                break;
            case (Person.T_DOCTOR):
                addSkill(person, SkillType.S_DOCTOR, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case (Person.T_MEDIC):
                addSkill(person, SkillType.S_MEDTECH, 0, 0);
                break;
            case (Person.T_ADMIN_COM):
            case (Person.T_ADMIN_LOG):
            case (Person.T_ADMIN_TRA):
            case (Person.T_ADMIN_HR):
                addSkill(person, SkillType.S_ADMIN, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
        }
    }

    protected static void addSkill(Person person, String skillName, int level, int bonus) {
        person.addSkill(skillName, new Skill(skillName, level, bonus));
    }

    protected static void addSkill(Person person, String skillName, int experienceLevel, boolean randomizeLevel, int bonus) {
        addSkill(person, skillName, experienceLevel, randomizeLevel, bonus, 0);
    }

    protected static void addSkill(Person person, String skillName, int experienceLevel, boolean randomizeLevel, int bonus, int rollMod) {
        if (randomizeLevel) {
            person.addSkill(skillName, Skill.randomizeLevel(skillName, experienceLevel, bonus, rollMod));
        } else {
            person.addSkill(skillName, Skill.createFromExperience(skillName, experienceLevel, bonus));
        }
    }

    /**
     * Gets the clan phenotype bonus for a {@link Person}, if applicable.
     * @param person A {@link Person} to calculate a phenotype bonus.
     * @return The bonus to a {@link Skill} due to clan phenotypes matching
     *         the primary role.
     */
    protected int getPhenotypeBonus(Person person) {
        if (person.isClanner()) {
            // apply phenotype bonus only to primary skills
            switch (person.getPrimaryRole()) {
                case (Person.T_MECHWARRIOR):
                    if (person.getPhenotype() == Person.PHENOTYPE_MW) {
                        return 1;
                    }
                    break;
                case (Person.T_GVEE_DRIVER):
                case (Person.T_NVEE_DRIVER):
                case (Person.T_VTOL_PILOT):
                case (Person.T_VEE_GUNNER):
                    if (person.getPhenotype() == Person.PHENOTYPE_VEE) {
                        return 1;
                    }
                    break;
                case (Person.T_CONV_PILOT):
                case (Person.T_AERO_PILOT):
                case (Person.T_PROTO_PILOT):
                    if (person.getPhenotype() == Person.PHENOTYPE_AERO) {
                        return 1;
                    }
                    break;
                case (Person.T_BA):
                    if (person.getPhenotype() == Person.PHENOTYPE_BA) {
                        return 1;
                    }
                    break;
                default:
                    break;
            }
        }
        return 0;
    }
}
