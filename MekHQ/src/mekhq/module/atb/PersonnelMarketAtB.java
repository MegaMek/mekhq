/*
 * Copyright (c) 2018-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.module.atb;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * Method for generating market personnel according to AtB rules.
 *
 * @author Neoancient
 */
public class PersonnelMarketAtB implements PersonnelMarketMethod {

    @Override
    public String getModuleName() {
        return "Against the Bot";
    }

    @Override
    public List<Person> generatePersonnelForDay(Campaign c) {
        if (c.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            List<Person> retVal = new ArrayList<>();
            Person p = null;

            int roll = Compute.d6(2);
            if (roll == 2) {
                switch (Compute.randomInt(4)) {
                case 0:
                    p = c.newPerson(PersonnelRole.ADMINISTRATOR_COMMAND);
                    break;
                case 1:
                    p = c.newPerson(PersonnelRole.ADMINISTRATOR_HR);
                    break;
                case 2:
                    p = c.newPerson(PersonnelRole.ADMINISTRATOR_LOGISTICS);
                    break;
                case 3:
                    p = c.newPerson(PersonnelRole.ADMINISTRATOR_TRANSPORT);
                    break;
                }
            } else if (roll == 3 || roll == 11) {
                int r = Compute.d6();
                if ((r == 1) && (c.getGameYear() > (c.getFaction().isClan() ? 2870 : 3050))) {
                    p = c.newPerson(PersonnelRole.BA_TECH);
                } else if (r < 4) {
                    p = c.newPerson(PersonnelRole.MECHANIC);
                } else if (r == 4 && c.getCampaignOptions().getUseAero()) {
                    p = c.newPerson(PersonnelRole.AERO_TECH);
                } else {
                    p = c.newPerson(PersonnelRole.MECH_TECH);
                }
            } else if (roll == 4 || roll == 10) {
                p = c.newPerson(PersonnelRole.MECHWARRIOR);
            } else if (roll == 5 && c.getCampaignOptions().getUseAero()) {
                p = c.newPerson(PersonnelRole.AEROSPACE_PILOT);
            } else if (roll == 5 && c.getFaction().isClan()) {
                p = c.newPerson(PersonnelRole.MECHWARRIOR);
            } else if (roll == 5) {
                int r = Compute.d6(2);
                if (r == 2) {
                    p = c.newPerson(PersonnelRole.VTOL_PILOT);
                    //Frequency based on frequency of VTOLs in Xotl 3028 Merc/General
                } else if (r <= 5) {
                    p = c.newPerson(PersonnelRole.GROUND_VEHICLE_DRIVER);
                } else {
                    p = c.newPerson(PersonnelRole.VEHICLE_GUNNER);
                }
            } else if (roll == 6 || roll == 8) {
                if (c.getFaction().isClan() && (c.getGameYear() > 2870) && (Compute.d6(2) > 3)) {
                    p = c.newPerson(PersonnelRole.BATTLE_ARMOUR);
                } else if (!c.getFaction().isClan() && (c.getGameYear() > 3050) && (Compute.d6(2) > 11)) {
                    p = c.newPerson(PersonnelRole.BATTLE_ARMOUR);
                } else {
                    p = c.newPerson(PersonnelRole.SOLDIER);
                }
            } else if (roll == 12) {
                p = c.newPerson(PersonnelRole.DOCTOR);
            }

            if (null != p) {
                retVal.add(p);

                if (p.getPrimaryRole().isGroundVehicleDriver()) {
                    /* Replace driver with 1-6 crew with equal
                     * chances of being drivers or gunners */
                    retVal.remove(p);
                    for (int i = 0; i < Compute.d6(); i++) {
                        retVal.add(c.newPerson((Compute.d6() < 4) ? PersonnelRole.GROUND_VEHICLE_DRIVER
                                : PersonnelRole.VEHICLE_GUNNER));
                    }
                }

                Person adminHR = c.findBestInRole(PersonnelRole.ADMINISTRATOR_HR, SkillType.S_ADMIN);
                int adminHRExp = (adminHR == null) ? SkillType.EXP_ULTRA_GREEN : adminHR.getSkill(SkillType.S_ADMIN).getExperienceLevel();
                int gunneryMod = 0;
                int pilotingMod = 0;
                switch (adminHRExp) {
                    case SkillType.EXP_ULTRA_GREEN:
                        gunneryMod = -1;
                        pilotingMod = -1;
                        break;
                    case SkillType.EXP_GREEN:
                        if (Compute.d6() < 4) {
                            gunneryMod = -1;
                        } else {
                            pilotingMod = -1;
                        }
                        break;
                    case SkillType.EXP_VETERAN:
                        if (Compute.d6() < 4) {
                            gunneryMod = 1;
                        } else {
                            pilotingMod = 1;
                        }
                        break;
                    case SkillType.EXP_ELITE:
                        gunneryMod = 1;
                        pilotingMod = 1;
                        break;
                    default:
                        break;
                }

                switch (p.getPrimaryRole()) {
                    case MECHWARRIOR:
                        adjustSkill(p, SkillType.S_GUN_MECH, gunneryMod);
                        adjustSkill(p, SkillType.S_PILOT_MECH, pilotingMod);
                        break;
                    case GROUND_VEHICLE_DRIVER:
                        adjustSkill(p, SkillType.S_PILOT_GVEE, pilotingMod);
                        break;
                    case NAVAL_VEHICLE_DRIVER:
                        adjustSkill(p, SkillType.S_PILOT_NVEE, pilotingMod);
                        break;
                    case VTOL_PILOT:
                        adjustSkill(p, SkillType.S_PILOT_VTOL, pilotingMod);
                        break;
                    case VEHICLE_GUNNER:
                        adjustSkill(p, SkillType.S_GUN_VEE, gunneryMod);
                        break;
                    case AEROSPACE_PILOT:
                        adjustSkill(p, SkillType.S_GUN_AERO, gunneryMod);
                        adjustSkill(p, SkillType.S_PILOT_AERO, pilotingMod);
                        break;
                    case PROTOMECH_PILOT:
                        adjustSkill(p, SkillType.S_GUN_PROTO, gunneryMod);
                        break;
                    case BATTLE_ARMOUR:
                        adjustSkill(p, SkillType.S_GUN_BA, gunneryMod);
                        adjustSkill(p, SkillType.S_ANTI_MECH, pilotingMod);
                        break;
                    case SOLDIER:
                        adjustSkill(p, SkillType.S_SMALL_ARMS, gunneryMod);
                        adjustSkill(p, SkillType.S_ANTI_MECH, pilotingMod);
                        break;
                    default:
                        break;
                }
            }
            return retVal;
        }
        return null;
    }

    @Override
    public List<Person> removePersonnelForDay(Campaign c, List<Person> current) {
        if (c.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            return current;
        }
        return null;
    }

    /**
     * Adjust a recruit's skill based on HR admin skill
     * @param p         The recruit
     * @param skillName The name of the skill to adjust
     * @param mod       The amount to adjust the skill
     */
    public void adjustSkill (Person p, String skillName, int mod) {
        if (p.getSkill(skillName) == null) {
            return;
        }
        if (mod > 0) {
            p.improveSkill(skillName);
        }
        if (mod < 0) {
            int lvl = p.getSkill(skillName).getLevel() + mod;
            p.getSkill(skillName).setLevel(Math.max(lvl, 0));
        }
    }

}
