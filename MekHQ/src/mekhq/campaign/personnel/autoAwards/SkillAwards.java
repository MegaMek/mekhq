/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.personnel.autoAwards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

public class SkillAwards {
    private static final MMLogger logger = MMLogger.create(SkillAwards.class);

    /**
     * This function loops through Skill Awards, checking whether the person is
     * eligible to receive each type of award
     *
     * @param campaign the current campaign
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where
     *                 item == Skill)
     */
    public static Map<Integer, List<Object>> SkillAwardsProcessor(Campaign campaign, UUID person, List<Award> awards) {
        int requiredSkillLevel;
        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            try {
                requiredSkillLevel = award.getQty();
            } catch (Exception e) {
                logger.warn("Award {} from the {} set has an invalid qty value {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            if (award.canBeAwarded(campaign.getPerson(person))) {
                // this allows the user to specify multiple skills to be checked against,
                // where all skill levels need to be met.
                // if the user puts two ',' next to each other (creating an empty skill) the
                // system will just treat it as an invalid
                // skill and break the current loop iteration
                List<String> skills = Arrays.asList(award.getRange()
                        .replaceAll("\\s", "")
                        .split(","));

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
                relevantSkills = Arrays.asList(SkillType.S_PILOT_MEK, SkillType.S_PILOT_AERO, SkillType.S_PILOT_GVEE,
                        SkillType.S_PILOT_VTOL, SkillType.S_PILOT_NVEE, SkillType.S_PILOT_JET, SkillType.S_PILOT_SPACE);
                break;

            case "accuracy":
                relevantSkills = Arrays.asList(SkillType.S_GUN_MEK, SkillType.S_GUN_AERO, SkillType.S_GUN_VEE,
                        SkillType.S_GUN_JET, SkillType.S_GUN_SPACE, SkillType.S_GUN_BA, SkillType.S_GUN_PROTO,
                        SkillType.S_ARTILLERY, SkillType.S_SMALL_ARMS, SkillType.S_ANTI_MEK);
                break;

            case "command":
                relevantSkills = Arrays.asList(SkillType.S_LEADER, SkillType.S_TACTICS, SkillType.S_STRATEGY);
                break;

            case "admin":
                relevantSkills = Arrays.asList(SkillType.S_ADMIN, SkillType.S_NEG, SkillType.S_SCROUNGE);
                break;

            case "techwithmedical":
                relevantSkills = Arrays.asList(SkillType.S_TECH_MEK, SkillType.S_TECH_AERO, SkillType.S_TECH_MECHANIC,
                        SkillType.S_TECH_VESSEL, SkillType.S_TECH_BA, SkillType.S_ASTECH, SkillType.S_DOCTOR,
                        SkillType.S_MEDTECH);
                break;

            case "tech":
                relevantSkills = Arrays.asList(SkillType.S_TECH_MEK, SkillType.S_TECH_AERO, SkillType.S_TECH_MECHANIC,
                        SkillType.S_TECH_VESSEL, SkillType.S_TECH_BA, SkillType.S_ASTECH);
                break;

            case "medical":
                relevantSkills = Arrays.asList(SkillType.S_DOCTOR, SkillType.S_MEDTECH);
                break;

            case "assistant":
                relevantSkills = Arrays.asList(SkillType.S_ASTECH, SkillType.S_MEDTECH);
                break;

            case "piloting/mech": // Remove below after Milestone Release post 0.49.19
            case "piloting/mek":
                relevantSkills = List.of(SkillType.S_PILOT_MEK);
                break;

            case "piloting/aerospace":
                relevantSkills = List.of(SkillType.S_PILOT_AERO);
                break;

            case "piloting/groundvehicle":
                relevantSkills = List.of(SkillType.S_PILOT_GVEE);
                break;

            case "piloting/vtol":
                relevantSkills = List.of(SkillType.S_PILOT_VTOL);
                break;

            case "piloting/naval":
                relevantSkills = List.of(SkillType.S_PILOT_NVEE);
                break;

            case "piloting/aircraft":
                relevantSkills = List.of(SkillType.S_PILOT_JET);
                break;

            case "piloting/spacecraft":
                relevantSkills = List.of(SkillType.S_PILOT_SPACE);
                break;

            case "gunnery/mech": // Remove below after Milestone Release post 0.49.19
            case "gunnery/mek":
                relevantSkills = List.of(SkillType.S_GUN_MEK);
                break;

            case "gunnery/aerospace":
                relevantSkills = List.of(SkillType.S_GUN_AERO);
                break;

            case "gunnery/vehicle":
                relevantSkills = List.of(SkillType.S_GUN_VEE);
                break;

            case "gunnery/aircraft":
                relevantSkills = List.of(SkillType.S_GUN_JET);
                break;

            case "gunnery/spacecraft":
                relevantSkills = List.of(SkillType.S_GUN_SPACE);
                break;

            case "gunnery/battlesuit":
                relevantSkills = List.of(SkillType.S_GUN_BA);
                break;

            case "gunnery/protomech": // Remove below after Milestone Release post 0.49.19
            case "gunnery/protomek":
                relevantSkills = List.of(SkillType.S_GUN_PROTO);
                break;

            case "tech/mech": // Remove below after Milestone Release post 0.49.19
            case "tech/mek":
                relevantSkills = List.of(SkillType.S_TECH_MEK);
                break;

            case "tech/mechanic":
                relevantSkills = List.of(SkillType.S_TECH_MECHANIC);
                break;

            case "tech/aero":
                relevantSkills = List.of(SkillType.S_TECH_AERO);
                break;

            case "tech/ba":
                relevantSkills = List.of(SkillType.S_TECH_BA);
                break;

            case "tech/vessel":
                relevantSkills = List.of(SkillType.S_TECH_VESSEL);
                break;

            case "artillery":
                relevantSkills = List.of(SkillType.S_ARTILLERY);
                break;

            case "smallarms":
                relevantSkills = List.of(SkillType.S_SMALL_ARMS);
                break;

            case "antimech": // Remove below after Milestone Release post 0.49.19
            case "antimek":
                relevantSkills = List.of(SkillType.S_ANTI_MEK);
                break;
            case "astech":
                relevantSkills = List.of(SkillType.S_ASTECH);
                break;

            case "doctor":
                relevantSkills = List.of(SkillType.S_DOCTOR);
                break;

            case "medtech":
                relevantSkills = List.of(SkillType.S_MEDTECH);
                break;

            case "hyperspacenavigation":
                relevantSkills = List.of(SkillType.S_NAV);
                break;

            case "administration":
                relevantSkills = List.of(SkillType.S_ADMIN);
                break;

            case "tactics":
                relevantSkills = List.of(SkillType.S_TACTICS);
                break;

            case "strategy":
                relevantSkills = List.of(SkillType.S_STRATEGY);
                break;

            case "negotiation":
                relevantSkills = List.of(SkillType.S_NEG);
                break;

            case "leadership":
                relevantSkills = List.of(SkillType.S_LEADER);
                break;

            case "scrounge":
                relevantSkills = List.of(SkillType.S_SCROUNGE);
                break;

            default:
                logger.warn("Award {} from the {} set has invalid skill {}", award.getName(),
                        award.getSet(), skill);

                // this treats the malformed Skill as if a Person was untrained
                return -1;
        }

        return getSkillLevel(campaign, relevantSkills, person);
    }

    /**
     * This function loops through all relevant skills, calculating the max skill
     * level.
     * If all skills are untrained, the function will default to -1.
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

        // IntelliJ's NPE warning here is a false-positive. The code doesn't allow
        // skillLevels to be empty.
        return Arrays.stream(skillLevels).max().getAsInt();
    }
}
