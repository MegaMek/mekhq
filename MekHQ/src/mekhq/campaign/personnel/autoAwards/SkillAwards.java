/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.autoAwards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;

public class SkillAwards {
    private static final MMLogger LOGGER = MMLogger.create(SkillAwards.class);

    /**
     * This function loops through Skill Awards, checking whether the person is eligible to receive each type of award
     *
     * @param campaign the current campaign
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where item == Skill)
     */
    public static Map<Integer, List<Object>> SkillAwardsProcessor(Campaign campaign, UUID person, List<Award> awards) {
        int requiredSkillLevel;
        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            try {
                requiredSkillLevel = award.getQty();
            } catch (Exception e) {
                LOGGER.warn("Award {} from the {} set has an invalid qty value {}",
                      award.getName(),
                      award.getSet(),
                      award.getQty());
                continue;
            }

            if (award.canBeAwarded(campaign.getPerson(person))) {
                // this allows the user to specify multiple skills to be checked against,
                // where all skill levels need to be met.
                // if the user puts two ',' next to each other (creating an empty skill) the
                // system will just treat it as an invalid
                // skill and break the current loop iteration
                List<String> skills = Arrays.asList(award.getRange().replaceAll("\\s", "").split(","));

                boolean hasRequiredSkillLevel = true;

                if (!skills.isEmpty()) {
                    for (String skill : skills) {
                        if (processSkills(campaign, award, person, skill) < requiredSkillLevel) {
                            hasRequiredSkillLevel = false;
                            // this break ensures that all required skills must be met/exceeded for Award
                            // eligibility
                            break;
                        }
                    }

                    if (hasRequiredSkillLevel) {
                        eligibleAwards.add(award);
                    }
                }
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }

    /**
     * This function uses switches to feed the relevant skill/s into getSkillLevel()
     *
     * @param campaign the current campaign
     * @param award    the award being processed, this is for error logging
     * @param person   the person whose skill levels we want
     * @param skill    the skill we're checking
     */
    private static int processSkills(Campaign campaign, Award award, UUID person, String skill) {
        List<String> relevantSkills;

        switch (skill.toLowerCase()) {
            // These first couple of cases are for those instances where the users want to
            // check against multiple skills, but only needs one passing grade
            case "piloting":
                relevantSkills = Arrays.asList(SkillTypeNew.S_PILOT_MEK.name(),
                      SkillTypeNew.S_PILOT_AERO.name(),
                      SkillTypeNew.S_PILOT_GVEE.name(),
                      SkillTypeNew.S_PILOT_VTOL.name(),
                      SkillTypeNew.S_PILOT_NVEE.name(),
                      SkillTypeNew.S_PILOT_JET.name(),
                      SkillTypeNew.S_PILOT_SPACE.name());
                break;

            case "accuracy":
                relevantSkills = Arrays.asList(SkillTypeNew.S_GUN_MEK.name(),
                      SkillTypeNew.S_GUN_AERO.name(),
                      SkillTypeNew.S_GUN_VEE.name(),
                      SkillTypeNew.S_GUN_JET.name(),
                      SkillTypeNew.S_GUN_SPACE.name(),
                      SkillTypeNew.S_GUN_BA.name(),
                      SkillTypeNew.S_GUN_PROTO.name(),
                      SkillTypeNew.S_ARTILLERY.name(),
                      SkillTypeNew.S_SMALL_ARMS.name(),
                      SkillTypeNew.S_ANTI_MEK.name());
                break;

            case "command":
                relevantSkills = Arrays.asList(SkillTypeNew.S_LEADER.name(),
                      SkillTypeNew.S_TACTICS.name(),
                      SkillTypeNew.S_STRATEGY.name());
                break;

            case "admin":
                relevantSkills = Arrays.asList(SkillTypeNew.S_ADMIN.name(), SkillTypeNew.S_NEGOTIATION.name());
                break;

            case "techwithmedical":
                relevantSkills = Arrays.asList(SkillTypeNew.S_TECH_MEK.name(),
                      SkillTypeNew.S_TECH_AERO.name(),
                      SkillTypeNew.S_TECH_MECHANIC.name(),
                      SkillTypeNew.S_TECH_VESSEL.name(),
                      SkillTypeNew.S_TECH_BA.name(),
                      SkillTypeNew.S_ASTECH.name(),
                      SkillTypeNew.S_SURGERY.name(),
                      SkillTypeNew.S_MEDTECH.name());
                break;

            case "tech":
                relevantSkills = Arrays.asList(SkillTypeNew.S_TECH_MEK.name(),
                      SkillTypeNew.S_TECH_AERO.name(),
                      SkillTypeNew.S_TECH_MECHANIC.name(),
                      SkillTypeNew.S_TECH_VESSEL.name(),
                      SkillTypeNew.S_TECH_BA.name(),
                      SkillTypeNew.S_ASTECH.name());
                break;

            case "medical":
                relevantSkills = Arrays.asList(SkillTypeNew.S_SURGERY.name(), SkillTypeNew.S_MEDTECH.name());
                break;

            case "assistant":
                relevantSkills = Arrays.asList(SkillTypeNew.S_ASTECH.name(), SkillTypeNew.S_MEDTECH.name());
                break;

            case "piloting/mek":
                relevantSkills = List.of(SkillTypeNew.S_PILOT_MEK.name());
                break;

            case "piloting/aerospace":
                relevantSkills = List.of(SkillTypeNew.S_PILOT_AERO.name());
                break;

            case "piloting/groundvehicle":
                relevantSkills = List.of(SkillTypeNew.S_PILOT_GVEE.name());
                break;

            case "piloting/vtol":
                relevantSkills = List.of(SkillTypeNew.S_PILOT_VTOL.name());
                break;

            case "piloting/naval":
                relevantSkills = List.of(SkillTypeNew.S_PILOT_NVEE.name());
                break;

            case "piloting/aircraft":
                relevantSkills = List.of(SkillTypeNew.S_PILOT_JET.name());
                break;

            case "piloting/spacecraft":
                relevantSkills = List.of(SkillTypeNew.S_PILOT_SPACE.name());
                break;

            case "gunnery/mek":
                relevantSkills = List.of(SkillTypeNew.S_GUN_MEK.name());
                break;

            case "gunnery/aerospace":
                relevantSkills = List.of(SkillTypeNew.S_GUN_AERO.name());
                break;

            case "gunnery/vehicle":
                relevantSkills = List.of(SkillTypeNew.S_GUN_VEE.name());
                break;

            case "gunnery/aircraft":
                relevantSkills = List.of(SkillTypeNew.S_GUN_JET.name());
                break;

            case "gunnery/spacecraft":
                relevantSkills = List.of(SkillTypeNew.S_GUN_SPACE.name());
                break;

            case "gunnery/battlesuit":
                relevantSkills = List.of(SkillTypeNew.S_GUN_BA.name());
                break;

            case "gunnery/protomek":
                relevantSkills = List.of(SkillTypeNew.S_GUN_PROTO.name());
                break;

            case "tech/mek":
                relevantSkills = List.of(SkillTypeNew.S_TECH_MEK.name());
                break;

            case "tech/mechanic":
                relevantSkills = List.of(SkillTypeNew.S_TECH_MECHANIC.name());
                break;

            case "tech/aero":
                relevantSkills = List.of(SkillTypeNew.S_TECH_AERO.name());
                break;

            case "tech/ba":
                relevantSkills = List.of(SkillTypeNew.S_TECH_BA.name());
                break;

            case "tech/vessel":
                relevantSkills = List.of(SkillTypeNew.S_TECH_VESSEL.name());
                break;

            case "artillery":
                relevantSkills = List.of(SkillTypeNew.S_ARTILLERY.name());
                break;

            case "smallarms":
                relevantSkills = List.of(SkillTypeNew.S_SMALL_ARMS.name());
                break;

            case "antimek":
                relevantSkills = List.of(SkillTypeNew.S_ANTI_MEK.name());
                break;
            case "astech":
                relevantSkills = List.of(SkillTypeNew.S_ASTECH.name());
                break;

            case "doctor":
                relevantSkills = List.of(SkillTypeNew.S_SURGERY.name());
                break;

            case "medtech":
                relevantSkills = List.of(SkillTypeNew.S_MEDTECH.name());
                break;

            case "hyperspacenavigation":
                relevantSkills = List.of(SkillTypeNew.S_NAVIGATION.name());
                break;

            case "administration":
                relevantSkills = List.of(SkillTypeNew.S_ADMIN.name());
                break;

            case "tactics":
                relevantSkills = List.of(SkillTypeNew.S_TACTICS.name());
                break;

            case "strategy":
                relevantSkills = List.of(SkillTypeNew.S_STRATEGY.name());
                break;

            case "negotiation":
                relevantSkills = List.of(SkillTypeNew.S_NEGOTIATION.name());
                break;

            case "leadership":
                relevantSkills = List.of(SkillTypeNew.S_LEADER.name());
                break;

            default:
                LOGGER.warn("Award {} from the {} set has invalid skill {}", award.getName(), award.getSet(), skill);

                // this treats the malformed Skill as if a Person was untrained
                return -1;
        }

        return getSkillLevel(campaign, relevantSkills, person);
    }

    /**
     * This function loops through all relevant skills, calculating the max skill level. If all skills are untrained,
     * the function will default to -1.
     *
     * @param campaign       the current campaign
     * @param relevantSkills the list of Skills to check
     * @param personId       the person whose Skill Levels are being checked
     */
    private static int getSkillLevel(Campaign campaign, List<String> relevantSkills, UUID personId) {
        Person person = campaign.getPerson(personId);

        int[] skillLevels = new int[relevantSkills.size()];

        for (int i = 0; i < relevantSkills.size(); i++) {
            if (person.hasSkill(relevantSkills.get(i))) {
                skillLevels[i] = person.getSkill(relevantSkills.get(i)).getLevel();
            } else {
                skillLevels[i] = -1;
            }
        }

        // IntelliJ's NPE warning here was related to the OptionalInt and not the stream or skillsLevels.
        OptionalInt maxSkill = Arrays.stream(skillLevels).max();
        if (maxSkill.isPresent()) {
            return maxSkill.getAsInt();
        } else {
            return -1;
        }
    }
}
