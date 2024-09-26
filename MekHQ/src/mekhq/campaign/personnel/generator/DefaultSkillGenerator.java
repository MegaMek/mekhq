/*
 * Copyright (C) 2019-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.Compute;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;

import java.util.Arrays;
import java.util.List;

public class DefaultSkillGenerator extends AbstractSkillGenerator {
    //region Constructors
    public DefaultSkillGenerator(final RandomSkillPreferences randomSkillPreferences) {
        super(randomSkillPreferences);
    }
    //endregion Constructors

    @Override
    public void generateSkills(final Campaign campaign, final Person person, final int expLvl) {
        PersonnelRole primaryRole = person.getPrimaryRole();
        PersonnelRole secondaryRole = person.getSecondaryRole();
        RandomSkillPreferences rskillPrefs = getSkillPreferences();

        int bonus = getPhenotypeBonus(person);
        int mod = 0;

        if (primaryRole.isLAMPilot()) {
            mod = -2;
        }

        generateDefaultSkills(person, primaryRole, expLvl, bonus, mod);

        if (!secondaryRole.isCivilian()) {
            generateDefaultSkills(person, secondaryRole, expLvl, bonus, mod);
        }

        // apply phenotype bonus only to primary skills
        bonus = 0;

        // roll small arms skill
        if (!person.getSkills().hasSkill(SkillType.S_SMALL_ARMS)) {
            int sarmsLvl = Utilities.generateExpLevel(
                    (primaryRole.isSupport(true) || secondaryRole.isSupport(true))
                            ? rskillPrefs.getSupportSmallArmsBonus() : rskillPrefs.getCombatSmallArmsBonus());

            if (primaryRole.isCivilian()) {
                sarmsLvl = 0;
            }

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
        if (campaign.getCampaignOptions().isUseArtillery()
                && (primaryRole.isMekWarrior() || primaryRole.isVehicleGunner() || primaryRole.isSoldier())
                && Utilities.rollProbability(rskillPrefs.getArtilleryProb())) {
            generateArtillerySkill(person, bonus);
        }

        // roll Negotiation skill
        if (campaign.getCampaignOptions().isAdminsHaveNegotiation()
                && (primaryRole.isAdministrator())) {
            addSkill(person, SkillType.S_NEG, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
        }

        // roll Scrounge skill
        if (campaign.getCampaignOptions().isAdminsHaveScrounge()
                && (primaryRole.isAdministrator())) {
            addSkill(person, SkillType.S_SCROUNGE, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
        }

        // roll random secondary skill
        if (Utilities.rollProbability(rskillPrefs.getSecondSkillProb())) {
            final List<String> possibleSkills = Arrays.stream(SkillType.skillList)
                    .filter(stype -> !person.getSkills().hasSkill(stype))
                    .toList();
            String selSkill = possibleSkills.get(Compute.randomInt(possibleSkills.size()));
            int secondLvl = Utilities.generateExpLevel(rskillPrefs.getSecondSkillBonus());
            addSkill(person, selSkill, secondLvl, rskillPrefs.randomizeSkill(), bonus);
        }
    }
}
