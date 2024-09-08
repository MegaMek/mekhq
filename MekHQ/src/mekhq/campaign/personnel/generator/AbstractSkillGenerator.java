/*
 * Copyright (C) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.generator;

import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;

import java.util.Objects;

/**
 * Represents a class which can generate new {@link Skill} objects
 * for a {@link Person}.
 */
public abstract class AbstractSkillGenerator {
    private RandomSkillPreferences rskillPrefs;

    protected AbstractSkillGenerator(final RandomSkillPreferences randomSkillPreferences) {
        this.rskillPrefs = randomSkillPreferences;
    }

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
     * @param campaign The {@link Campaign} the person is a part of
     * @param person The {@link Person} to add skills.
     * @param expLvl The experience level of the person (e.g. {@link SkillType#EXP_GREEN}).
     */
    public abstract void generateSkills(final Campaign campaign, final Person person, final int expLvl);

    /**
     * Generates the default skills for a {@link Person} based on their primary role.
     * @param person The {@link Person} to add default skills.
     * @param primaryRole The primary role of the person
     * @param expLvl The experience level of the person (e.g. {@link SkillType#EXP_GREEN}).
     * @param bonus The bonus to use for the default skills.
     * @param rollModifier A roll modifier to apply to any randomizations.
     */
    protected void generateDefaultSkills(Person person, PersonnelRole primaryRole, int expLvl, int bonus, int rollModifier) {
        switch (primaryRole) {
            case MEKWARRIOR:
                addSkill(person, SkillType.S_PILOT_MEK, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_MEK, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case LAM_PILOT:
                addSkill(person, SkillType.S_PILOT_MEK, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_MEK, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_PILOT_AERO, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_AERO, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case GROUND_VEHICLE_DRIVER:
                addSkill(person, SkillType.S_PILOT_GVEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case NAVAL_VEHICLE_DRIVER:
                addSkill(person, SkillType.S_PILOT_NVEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case VTOL_PILOT:
                addSkill(person, SkillType.S_PILOT_VTOL, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case VEHICLE_GUNNER:
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case VEHICLE_CREW:
                addSkill(person, SkillType.S_TECH_MECHANIC, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case AEROSPACE_PILOT:
                addSkill(person, SkillType.S_PILOT_AERO, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_AERO, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case CONVENTIONAL_AIRCRAFT_PILOT:
                addSkill(person, SkillType.S_PILOT_JET, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_GUN_JET, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case PROTOMEK_PILOT:
                addSkill(person, SkillType.S_GUN_PROTO, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case BATTLE_ARMOUR:
                addSkill(person, SkillType.S_GUN_BA, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_ANTI_MEK, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                addSkill(person, SkillType.S_SMALL_ARMS, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case SOLDIER:
                if (Utilities.rollProbability(rskillPrefs.getAntiMekProb())) {
                    addSkill(person, SkillType.S_ANTI_MEK, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                }
                addSkill(person, SkillType.S_SMALL_ARMS, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case VESSEL_PILOT:
                addSkill(person, SkillType.S_PILOT_SPACE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case VESSEL_GUNNER:
                addSkill(person, SkillType.S_GUN_SPACE, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case VESSEL_CREW:
                addSkill(person, SkillType.S_TECH_VESSEL, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case VESSEL_NAVIGATOR:
                addSkill(person, SkillType.S_NAV, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case MEK_TECH:
                addSkill(person, SkillType.S_TECH_MEK, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case MECHANIC:
                addSkill(person, SkillType.S_TECH_MECHANIC, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case AERO_TECH:
                addSkill(person, SkillType.S_TECH_AERO, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case BA_TECH:
                addSkill(person, SkillType.S_TECH_BA, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case ASTECH:
                addSkill(person, SkillType.S_ASTECH, 0, 0);
                break;
            case DOCTOR:
                addSkill(person, SkillType.S_DOCTOR, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            case MEDIC:
                addSkill(person, SkillType.S_MEDTECH, 0, 0);
                break;
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_TRANSPORT:
            case ADMINISTRATOR_HR:
                addSkill(person, SkillType.S_ADMIN, expLvl, rskillPrefs.randomizeSkill(), bonus, rollModifier);
                break;
            default:
                break;
        }
    }

    public void generateArtillerySkill(final Person person) {
        generateArtillerySkill(person, getPhenotypeBonus(person));
    }

    protected void generateArtillerySkill(final Person person, final int bonus) {
        final int experienceLevel = Utilities.generateExpLevel(rskillPrefs.getArtilleryBonus());
        if (experienceLevel > SkillType.EXP_ULTRA_GREEN) {
            addSkill(person, SkillType.S_ARTILLERY, experienceLevel, rskillPrefs.randomizeSkill(), bonus);
        }
    }

    public static void addSkill(Person person, String skillName, int level, int bonus) {
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
