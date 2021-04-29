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

import java.util.ArrayList;
import java.util.List;

import megamek.common.Compute;
import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;

public class DefaultSkillGenerator extends AbstractSkillGenerator {

    @Override
    public void generateSkills(Person person, int expLvl) {
        PersonnelRole primaryRole = person.getPrimaryRole();
        PersonnelRole secondaryRole = person.getSecondaryRole();
        RandomSkillPreferences rskillPrefs = getSkillPreferences();

        int bonus = 0;
        int mod = 0;

        if (primaryRole.isLAMPilot()) {
            mod = -2;
        }

        generateDefaultSkills(person, primaryRole, expLvl, bonus, mod);

        if (!secondaryRole.isDependentOrNone()) {
            generateDefaultSkills(person, secondaryRole, expLvl, bonus, mod);
        }

        bonus = getPhenotypeBonus(person);

        // roll small arms skill
        if (!person.getSkills().hasSkill(SkillType.S_SMALL_ARMS)) {
            int sarmsLvl = Utilities.generateExpLevel(
                    (primaryRole.isSupport() || secondaryRole.isSupport(true))
                            ? rskillPrefs.getSupportSmallArmsBonus() : rskillPrefs.getCombatSmallArmsBonus());
            if (sarmsLvl > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_SMALL_ARMS, sarmsLvl, rskillPrefs.randomizeSkill(), bonus);
            }
        }

        // roll tactics skill
        if (!(primaryRole.isSupport() || secondaryRole.isSupport(true))) {
            int tacLvl = Utilities.generateExpLevel(rskillPrefs.getTacticsMod(expLvl));
            if (tacLvl > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_TACTICS, tacLvl, rskillPrefs.randomizeSkill(), bonus);
            }
        }

        // roll artillery skill
        if (getCampaignOptions(person).useArtillery()
                && (primaryRole.isMechWarrior() || primaryRole.isVehicleGunner() || primaryRole.isSoldier())
                && Utilities.rollProbability(rskillPrefs.getArtilleryProb())) {
            int artyLvl = Utilities.generateExpLevel(rskillPrefs.getArtilleryBonus());
            if (artyLvl > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_ARTILLERY, artyLvl, rskillPrefs.randomizeSkill(), bonus);
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
            addSkill(person, selSkill, secondLvl, rskillPrefs.randomizeSkill(), bonus);
        }
    }

    private CampaignOptions getCampaignOptions(Person person) {
        return person.getCampaign().getCampaignOptions();
    }
}
