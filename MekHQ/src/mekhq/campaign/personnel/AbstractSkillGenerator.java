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
package mekhq.campaign.personnel;

import java.util.Objects;

import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;

public abstract class AbstractSkillGenerator {

    private final CampaignOptions options;

    private final RandomSkillPreferences rskillPrefs = new RandomSkillPreferences();

    protected AbstractSkillGenerator(CampaignOptions options) {
        this.options = Objects.requireNonNull(options);
    }

    protected CampaignOptions getCampaignOptions() {
        return options;
    }

    protected RandomSkillPreferences getRandomSkillPreferences() {
        return rskillPrefs;
    }

    public abstract void generateSkills(Person person, int expLvl);

    protected void generateDefaultSkills(Person person, int type, int expLvl, int bonus, int mod) {
        switch (type) {
            case (Person.T_MECHWARRIOR):
                addSkill(person, SkillType.S_PILOT_MECH, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                addSkill(person, SkillType.S_GUN_MECH, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_GVEE_DRIVER):
                addSkill(person, SkillType.S_PILOT_GVEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_NVEE_DRIVER):
                addSkill(person, SkillType.S_PILOT_NVEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_VTOL_PILOT):
                addSkill(person, SkillType.S_PILOT_VTOL, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                addSkill(person, SkillType.S_GUN_VEE, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_VEE_GUNNER):
                addSkill(person, SkillType.S_GUN_VEE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_CONV_PILOT):
                addSkill(person, SkillType.S_PILOT_JET, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                addSkill(person, SkillType.S_GUN_JET, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_AERO_PILOT):
                addSkill(person, SkillType.S_PILOT_AERO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                addSkill(person, SkillType.S_GUN_AERO, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_PROTO_PILOT):
                addSkill(person, SkillType.S_GUN_PROTO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_BA):
                addSkill(person, SkillType.S_GUN_BA, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                addSkill(person, SkillType.S_ANTI_MECH, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                addSkill(person, SkillType.S_SMALL_ARMS, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_INFANTRY):
                if (Utilities.rollProbability(rskillPrefs.getAntiMekProb())) {
                    addSkill(person, SkillType.S_ANTI_MECH, expLvl,
                            rskillPrefs.randomizeSkill(), bonus, mod);
                }
                addSkill(person, SkillType.S_SMALL_ARMS, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_SPACE_PILOT):
                addSkill(person, SkillType.S_PILOT_SPACE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_SPACE_CREW):
                addSkill(person, SkillType.S_TECH_VESSEL, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_SPACE_GUNNER):
                addSkill(person, SkillType.S_GUN_SPACE, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_NAVIGATOR):
                addSkill(person, SkillType.S_NAV, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_MECH_TECH):
                addSkill(person, SkillType.S_TECH_MECH, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_MECHANIC):
            case Person.T_VEHICLE_CREW:
                addSkill(person, SkillType.S_TECH_MECHANIC, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_AERO_TECH):
                addSkill(person, SkillType.S_TECH_AERO, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_BA_TECH):
                addSkill(person, SkillType.S_TECH_BA, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_ASTECH):
                addSkill(person, SkillType.S_ASTECH, 0, 0);
                break;
            case (Person.T_DOCTOR):
                addSkill(person, SkillType.S_DOCTOR, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
            case (Person.T_MEDIC):
                addSkill(person, SkillType.S_MEDTECH, 0, 0);
                break;
            case (Person.T_ADMIN_COM):
            case (Person.T_ADMIN_LOG):
            case (Person.T_ADMIN_TRA):
            case (Person.T_ADMIN_HR):
                addSkill(person, SkillType.S_ADMIN, expLvl,
                        rskillPrefs.randomizeSkill(), bonus, mod);
                break;
        }
    }

    protected static void addSkill(Person person, String skillName, int lvl, int bonus) {
        person.addSkill(skillName, new Skill(skillName, lvl, bonus));
    }

    protected static void addSkill(Person person, String skillName, int xpLvl, boolean random, int bonus) {
        person.addSkill(skillName, new Skill(skillName, xpLvl, random, bonus, 0));
    }

    protected static void addSkill(Person person, String skillName, int xpLvl, boolean random, int bonus, int rollMod) {
        person.addSkill(skillName, new Skill(skillName, xpLvl, random, bonus, rollMod));
    }

    protected static int getPhenotypeBonus(Person person) {
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
