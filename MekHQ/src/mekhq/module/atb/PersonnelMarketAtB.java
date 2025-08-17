/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.module.atb;

import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
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
    public List<Person> generatePersonnelForDay(Campaign campaign) {
        if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            List<Person> potentialRecruits = new ArrayList<>();
            Person recruit = null;

            int roll = Compute.d6(2);
            if (roll == 2) {
                recruit = switch (Compute.randomInt(4)) {
                    case 0 -> campaign.newPerson(PersonnelRole.ADMINISTRATOR_COMMAND);
                    case 1 -> campaign.newPerson(PersonnelRole.ADMINISTRATOR_HR);
                    case 2 -> campaign.newPerson(PersonnelRole.ADMINISTRATOR_LOGISTICS);
                    case 3 -> campaign.newPerson(PersonnelRole.ADMINISTRATOR_TRANSPORT);
                    default -> null;
                };
            } else if (roll == 3 || roll == 11) {
                int secondaryRoll = Compute.d6();
                if ((secondaryRoll == 1) && (campaign.getGameYear() > (campaign.getFaction().isClan() ? 2870 : 3050))) {
                    recruit = campaign.newPerson(PersonnelRole.BA_TECH);
                } else if (secondaryRoll < 4) {
                    recruit = campaign.newPerson(PersonnelRole.MECHANIC);
                } else if (secondaryRoll == 4 && campaign.getCampaignOptions().isUseAero()) {
                    recruit = campaign.newPerson(PersonnelRole.AERO_TEK);
                } else {
                    recruit = campaign.newPerson(PersonnelRole.MEK_TECH);
                }
            } else if (roll == 4 || roll == 10) {
                recruit = campaign.newPerson(PersonnelRole.MEKWARRIOR);
            } else if (roll == 5 && campaign.getCampaignOptions().isUseAero()) {
                recruit = campaign.newPerson(PersonnelRole.AEROSPACE_PILOT);
            } else if (roll == 5 && campaign.getFaction().isClan()) {
                recruit = campaign.newPerson(PersonnelRole.MEKWARRIOR);
            } else if (roll == 5) {
                int secondaryRoll = Compute.d6(2);
                if (secondaryRoll == 2) {
                    recruit = campaign.newPerson(PersonnelRole.VTOL_PILOT);
                    // Frequency based on frequency of VTOLs in Xotl 3028 Merc/General
                } else if (secondaryRoll <= 5) {
                    recruit = campaign.newPerson(PersonnelRole.GROUND_VEHICLE_DRIVER);
                } else {
                    recruit = campaign.newPerson(PersonnelRole.VEHICLE_GUNNER);
                }
            } else if (roll == 6 || roll == 8) {
                if (campaign.getFaction().isClan() && (campaign.getGameYear() > 2870) && (Compute.d6(2) > 3)) {
                    recruit = campaign.newPerson(PersonnelRole.BATTLE_ARMOUR);
                } else if (!campaign.getFaction().isClan() && (campaign.getGameYear() > 3050) && (Compute.d6(2) > 11)) {
                    recruit = campaign.newPerson(PersonnelRole.BATTLE_ARMOUR);
                } else {
                    recruit = campaign.newPerson(PersonnelRole.SOLDIER);
                }
            } else if (roll == 12) {
                recruit = campaign.newPerson(PersonnelRole.DOCTOR);
            }

            if (null != recruit) {
                potentialRecruits.add(recruit);

                if (recruit.getPrimaryRole().isGroundVehicleDriver()) {
                    /*
                     * Replace driver with 1-6 crew with equal
                     * chances of being drivers or gunners
                     */
                    potentialRecruits.remove(recruit);
                    for (int i = 0; i < Compute.d6(); i++) {
                        potentialRecruits.add(campaign.newPerson((Compute.d6() < 4) ?
                                                                       PersonnelRole.GROUND_VEHICLE_DRIVER
                                                                       : PersonnelRole.VEHICLE_GUNNER));
                    }
                }

                Person adminHR = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_HR, S_ADMIN);
                int adminExperienceLevel = EXP_NONE;
                if (adminHR != null && adminHR.hasSkill(S_ADMIN)) {
                    Skill adminSkill = adminHR.getSkill(S_ADMIN);
                    adminExperienceLevel = adminSkill.getExperienceLevel(adminHR.getOptions(),
                          adminHR.getATOWAttributes());
                }

                int gunneryMod = 0;
                int pilotingMod = 0;
                switch (adminExperienceLevel) {
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
                    case SkillType.EXP_ELITE, SkillType.EXP_HEROIC, SkillType.EXP_LEGENDARY:
                        gunneryMod = 1;
                        pilotingMod = 1;
                        break;
                    default:
                        break;
                }

                switch (recruit.getPrimaryRole()) {
                    case MEKWARRIOR:
                        adjustSkill(recruit, SkillType.S_GUN_MEK, gunneryMod);
                        adjustSkill(recruit, SkillType.S_PILOT_MEK, pilotingMod);
                        break;
                    case GROUND_VEHICLE_DRIVER:
                        adjustSkill(recruit, SkillType.S_PILOT_GVEE, pilotingMod);
                        break;
                    case NAVAL_VEHICLE_DRIVER:
                        adjustSkill(recruit, SkillType.S_PILOT_NVEE, pilotingMod);
                        break;
                    case VTOL_PILOT:
                        adjustSkill(recruit, SkillType.S_PILOT_VTOL, pilotingMod);
                        break;
                    case VEHICLE_GUNNER:
                        adjustSkill(recruit, SkillType.S_GUN_VEE, gunneryMod);
                        break;
                    case AEROSPACE_PILOT:
                        adjustSkill(recruit, SkillType.S_GUN_AERO, gunneryMod);
                        adjustSkill(recruit, SkillType.S_PILOT_AERO, pilotingMod);
                        break;
                    case PROTOMEK_PILOT:
                        adjustSkill(recruit, SkillType.S_GUN_PROTO, gunneryMod);
                        break;
                    case BATTLE_ARMOUR:
                        adjustSkill(recruit, SkillType.S_GUN_BA, gunneryMod);
                        adjustSkill(recruit, SkillType.S_ANTI_MEK, pilotingMod);
                        break;
                    case SOLDIER:
                        adjustSkill(recruit, SkillType.S_SMALL_ARMS, gunneryMod);
                        adjustSkill(recruit, SkillType.S_ANTI_MEK, pilotingMod);
                        break;
                    default:
                        break;
                }
            }
            return potentialRecruits;
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
     *
     * @param p         The recruit
     * @param skillName The name of the skill to adjust
     * @param mod       The amount to adjust the skill
     */
    public void adjustSkill(Person p, String skillName, int mod) {
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
