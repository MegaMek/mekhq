/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import java.util.HashMap;
import java.util.Map;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;

class AdvancementOptionsModel {
    private static final int[] EXPERIENCE_LEVELS = new int[] { SkillType.EXP_ULTRA_GREEN,
                                                               SkillType.EXP_GREEN,
                                                               SkillType.EXP_REGULAR,
                                                               SkillType.EXP_VETERAN,
                                                               SkillType.EXP_ELITE,
                                                               SkillType.EXP_HEROIC,
                                                               SkillType.EXP_LEGENDARY };

    double xpCostMultiplier;
    int taskXP;
    int nTasksXP;
    int successXP;
    int mistakeXP;
    int scenarioXP;
    int killXP;
    int killsForXP;
    int vocationalXP;
    int vocationalXPFrequency;
    int vocationalXPTargetNumber;
    int missionXpFail;
    int missionXpSuccess;
    int missionXpOutstandingSuccess;
    int contractNegotiationXP;
    int adminWeeklyXP;
    int adminWeeklyXPPeriod;
    boolean randomizeSkill;
    int[] phenotypeProbabilities;
    int[] specialAbilityBonus;
    int[] commandSkillsModifier;
    int[] utilitySkillsModifier;
    int roleplaySkillsModifier;
    int combatSmallArmsBonus;
    int supportSmallArmsBonus;
    int artilleryProb;
    int artilleryBonus;
    int antiMekProb;
    int secondSkillProb;
    int secondSkillBonus;
    Map<PersonnelRole, Integer> recruitmentBonuses;

    AdvancementOptionsModel(CampaignOptions options, RandomSkillPreferences skillPreferences) {
        xpCostMultiplier = options.getXpCostMultiplier();
        taskXP = options.getTaskXP();
        nTasksXP = options.getNTasksXP();
        successXP = options.getSuccessXP();
        mistakeXP = options.getMistakeXP();
        scenarioXP = options.getScenarioXP();
        killXP = options.getKillXPAward();
        killsForXP = options.getKillsForXP();
        vocationalXP = options.getVocationalXP();
        vocationalXPFrequency = options.getVocationalXPCheckFrequency();
        vocationalXPTargetNumber = options.getVocationalXPTargetNumber();
        missionXpFail = options.getMissionXpFail();
        missionXpSuccess = options.getMissionXpSuccess();
        missionXpOutstandingSuccess = options.getMissionXpOutstandingSuccess();
        contractNegotiationXP = options.getContractNegotiationXP();
        adminWeeklyXP = options.getAdminXP();
        adminWeeklyXPPeriod = options.getAdminXPPeriod();
        randomizeSkill = skillPreferences.randomizeSkill();
        phenotypeProbabilities = options.getPhenotypeProbabilities().clone();
        specialAbilityBonus = new int[EXPERIENCE_LEVELS.length];
        commandSkillsModifier = new int[EXPERIENCE_LEVELS.length];
        utilitySkillsModifier = new int[EXPERIENCE_LEVELS.length];
        for (int experienceLevel : EXPERIENCE_LEVELS) {
            specialAbilityBonus[experienceLevel] = skillPreferences.getSpecialAbilityBonus(experienceLevel);
            commandSkillsModifier[experienceLevel] = skillPreferences.getCommandSkillsModifier(experienceLevel);
            utilitySkillsModifier[experienceLevel] = skillPreferences.getUtilitySkillsModifier(experienceLevel);
        }
        roleplaySkillsModifier = skillPreferences.getRoleplaySkillModifier();
        combatSmallArmsBonus = skillPreferences.getCombatSmallArmsBonus();
        supportSmallArmsBonus = skillPreferences.getSupportSmallArmsBonus();
        artilleryProb = skillPreferences.getArtilleryProb();
        artilleryBonus = skillPreferences.getArtilleryBonus();
        antiMekProb = skillPreferences.getAntiMekProb();
        secondSkillProb = skillPreferences.getSecondSkillProb();
        secondSkillBonus = skillPreferences.getSecondSkillBonus();
        recruitmentBonuses = new HashMap<>(skillPreferences.getRecruitmentBonuses());
    }

    void applyTo(CampaignOptions options, RandomSkillPreferences skillPreferences) {
        options.setXpCostMultiplier(xpCostMultiplier);
        options.setTaskXP(taskXP);
        options.setNTasksXP(nTasksXP);
        options.setSuccessXP(successXP);
        options.setMistakeXP(mistakeXP);
        options.setScenarioXP(scenarioXP);
        options.setKillXPAward(killXP);
        options.setKillsForXP(killsForXP);
        options.setVocationalXP(vocationalXP);
        options.setVocationalXPCheckFrequency(vocationalXPFrequency);
        options.setVocationalXPTargetNumber(vocationalXPTargetNumber);
        options.setMissionXpFail(missionXpFail);
        options.setMissionXpSuccess(missionXpSuccess);
        options.setMissionXpOutstandingSuccess(missionXpOutstandingSuccess);
        options.setContractNegotiationXP(contractNegotiationXP);
        options.setAdminXP(adminWeeklyXP);
        options.setAdminXPPeriod(adminWeeklyXPPeriod);

        skillPreferences.setRandomizeSkill(randomizeSkill);
        for (int i = 0; i < phenotypeProbabilities.length; i++) {
            options.setPhenotypeProbability(i, phenotypeProbabilities[i]);
        }
        skillPreferences.setAntiMekProb(antiMekProb);
        skillPreferences.setArtilleryProb(artilleryProb);
        skillPreferences.setArtilleryBonus(artilleryBonus);
        skillPreferences.setSecondSkillProb(secondSkillProb);
        skillPreferences.setSecondSkillBonus(secondSkillBonus);

        for (int experienceLevel : EXPERIENCE_LEVELS) {
            skillPreferences.setCommandSkillsMod(experienceLevel, commandSkillsModifier[experienceLevel]);
            skillPreferences.setUtilitySkillsMod(experienceLevel, utilitySkillsModifier[experienceLevel]);
            skillPreferences.setSpecialAbilityBonus(experienceLevel, specialAbilityBonus[experienceLevel]);
        }
        skillPreferences.setRoleplaySkillModifier(roleplaySkillsModifier);
        skillPreferences.setCombatSmallArmsBonus(combatSmallArmsBonus);
        skillPreferences.setSupportSmallArmsBonus(supportSmallArmsBonus);

        for (Map.Entry<PersonnelRole, Integer> recruitmentBonus : recruitmentBonuses.entrySet()) {
            skillPreferences.addRecruitmentBonus(recruitmentBonus.getKey(), recruitmentBonus.getValue());
        }
    }
}