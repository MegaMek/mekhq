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
 */
package mekhq.campaign.personnel.generator;

import java.util.Arrays;
import java.util.List;

import megamek.common.Compute;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;

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

        // roll lesdership skills
        if (primaryRole.isCombat()) {
            int leadershipSkillLevel = Utilities.generateExpLevel(rskillPrefs.getCommandSkillsModifier(expLvl));
            if (leadershipSkillLevel > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_TACTICS, leadershipSkillLevel, rskillPrefs.randomizeSkill(), bonus);
            }

            leadershipSkillLevel = Utilities.generateExpLevel(rskillPrefs.getCommandSkillsModifier(expLvl));
            if (leadershipSkillLevel > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_STRATEGY, leadershipSkillLevel, rskillPrefs.randomizeSkill(), bonus);
            }

            leadershipSkillLevel = Utilities.generateExpLevel(rskillPrefs.getCommandSkillsModifier(expLvl));
            if (leadershipSkillLevel > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_LEADER, leadershipSkillLevel, rskillPrefs.randomizeSkill(), bonus);
            }
        }

        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        // roll artillery skill
        if (campaignOptions.isUseArtillery()
                && (primaryRole.isMekWarrior() || primaryRole.isVehicleGunner() || primaryRole.isSoldier())
                && Utilities.rollProbability(rskillPrefs.getArtilleryProb())) {
            generateArtillerySkill(person, bonus);
        }

        // roll Negotiation skill
        if (campaignOptions.isAdminsHaveNegotiation()
                && (primaryRole.isAdministrator())) {
            addSkill(person, SkillType.S_NEG, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
        }

        // roll Scrounge skill
        if (campaignOptions.isAdminsHaveScrounge()
                && (primaryRole.isAdministrator())) {
            addSkill(person, SkillType.S_SCROUNGE, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
        }

        // roll Administration skill
        if (campaignOptions.isTechsUseAdministration() && (person.isTech() || primaryRole.isVesselCrew())) {
            addSkill(person, SkillType.S_ADMIN, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
        }

        if (campaignOptions.isDoctorsUseAdministration() && (primaryRole.isDoctor())) {
            addSkill(person, SkillType.S_ADMIN, expLvl, rskillPrefs.randomizeSkill(), bonus, mod);
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
