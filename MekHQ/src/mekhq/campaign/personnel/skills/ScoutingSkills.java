/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.skills;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.ObjectUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.log.PerformanceLogger;
import mekhq.campaign.personnel.Person;

public class ScoutingSkills {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AdvancedScoutingCampaignOptionsChangedConfirmationDialog";

    /**
     * Unmodifiable list of all skill type strings considered to be scouting skills.
     */
    public static final List<String> SCOUTING_SKILLS = List.of(SkillType.S_COMMUNICATIONS,
          SkillType.S_PERCEPTION, SkillType.S_SENSOR_OPERATIONS, SkillType.S_STEALTH, SkillType.S_TRACKING);

    public static final String DEFAULT_UNSKILLED_CHECK = SkillType.S_SENSOR_OPERATIONS;

    public static @Nullable String getBestScoutingSkill(Person person) {
        String bestSkill = null;
        int highestLevel = -1;

        SkillModifierData skillModifierData = person.getSkillModifierData();
        for (String skillName : SCOUTING_SKILLS) {
            if (person.hasSkill(skillName)) {
                int skillLevel = person.getSkill(skillName).getTotalSkillLevel(skillModifierData);

                if (skillLevel > highestLevel) {
                    highestLevel = skillLevel;
                    bestSkill = skillName;
                }
            }
        }

        return bestSkill;
    }

    /**
     * Gives roughly a quarter of the campaign's combat personnel level 1 in a random scouting skill, skipping anyone
     * who already has that skill at level 1 or higher. Used when Advanced Scouting is turned on.
     *
     * @param campaign the campaign whose personnel gain skills
     * @param isSilent {@code true} to skip the daily report and personnel log entries
     */
    public static void processFreeSkills(Campaign campaign, boolean isSilent) {
        List<Person> personnel = campaign.getPlayerForce().getHumanResources().getPersonnelFilteringOutDeparted();
        boolean logSkillGain = campaign.getCampaignOptions().get(CampaignOption.PERSONNEL_LOG_SKILL_GAIN);
        LocalDate today = campaign.getLocalDate();
        for (Person person : personnel) {
            if (!person.isCombat() || randomInt(4) != 0) {
                continue;
            }

            String skillName = ObjectUtility.getRandomItem(SCOUTING_SKILLS);
            int bonus = 0;
            if (person.hasSkill(skillName)) {
                Skill skill = person.getSkill(skillName);
                if (person.getSkill(skillName).getLevel() >= 1) {
                    continue;
                }
                bonus = skill.getBonus();
            }

            person.addSkill(skillName, 1, bonus);

            if (!isSilent) {
                PerformanceLogger.improvedSkill(logSkillGain,
                      person,
                      today,
                      skillName,
                      1);
                campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE, "improved.format",
                      person.getHyperlinkedName(),
                      SkillType.getType(skillName).getName()));
            }
            campaign.getPlayerForce().getHumanResources().personUpdated(campaign, person);
        }
    }
}
