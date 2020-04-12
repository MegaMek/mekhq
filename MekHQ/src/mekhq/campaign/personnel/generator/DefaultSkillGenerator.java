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

import java.util.ArrayList;
import java.util.List;

import megamek.common.Compute;
import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;

public class DefaultSkillGenerator extends AbstractSkillGenerator {

    @Override
    public void generateSkills(Person person, int expLvl) {
        int type = person.getPrimaryRole();
        int secondary = person.getSecondaryRole();
        RandomSkillPreferences rskillPrefs = getSkillPreferences();

        int bonus = 0;
        int mod = 0;

        if ((type == Person.T_MECHWARRIOR)
            && (secondary == Person.T_AERO_PILOT)) {
            mod = -2;
        }

        generateDefaultSkills(person, type, expLvl, bonus, mod);

        if (secondary != Person.T_NONE) {
            generateDefaultSkills(person, secondary, expLvl, bonus, mod);
        }

        bonus = getPhenotypeBonus(person);

        // roll small arms skill
        if (!person.getSkills().hasSkill(SkillType.S_SMALL_ARMS)) {
            int sarmsLvl = -12;
            if (Person.isSupportRole(type) || Person.isSupportRole(secondary)) {
                sarmsLvl = Utilities.generateExpLevel(rskillPrefs.getSupportSmallArmsBonus());
            } else {
                sarmsLvl = Utilities.generateExpLevel(rskillPrefs.getCombatSmallArmsBonus());
            }
            if (sarmsLvl > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_SMALL_ARMS, sarmsLvl,
                        rskillPrefs.randomizeSkill(), bonus);
            }
        }

        // roll tactics skill
        if (!(Person.isSupportRole(type) || Person.isSupportRole(secondary))) {
            int tacLvl = Utilities.generateExpLevel(rskillPrefs.getTacticsMod(expLvl));
            if (tacLvl > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_TACTICS, tacLvl,
                        rskillPrefs.randomizeSkill(), bonus);
            }
        }

        // roll artillery skill
        if (getCampaignOptions(person).useArtillery()
                && (type == Person.T_MECHWARRIOR || type == Person.T_VEE_GUNNER || type == Person.T_INFANTRY)
                && Utilities.rollProbability(rskillPrefs.getArtilleryProb())) {
            int artyLvl = Utilities.generateExpLevel(rskillPrefs.getArtilleryBonus());
            if (artyLvl > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_ARTILLERY, artyLvl,
                        rskillPrefs.randomizeSkill(), bonus);
            }
        }

        // roll random secondary skill
        if (Utilities.rollProbability(rskillPrefs.getSecondSkillProb())) {
            final List<String> possibleSkills = new ArrayList<>();
            for (String stype : SkillType.skillList) {
                if (!person.getSkills().hasSkill(stype)) {
                    possibleSkills.add(stype);
                }
            }
            String selSkill = possibleSkills.get(Compute.randomInt(possibleSkills.size()));
            int secondLvl = Utilities.generateExpLevel(rskillPrefs.getSecondSkillBonus());
            addSkill(person, selSkill, secondLvl, rskillPrefs.randomizeSkill(),
                    bonus);
        }
    }

    private CampaignOptions getCampaignOptions(Person person) {
        return person.getCampaign().getCampaignOptions();
    }
}
