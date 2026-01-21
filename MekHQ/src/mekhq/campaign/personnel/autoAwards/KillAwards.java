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

import static mekhq.campaign.force.FormationLevel.ARMY;
import static mekhq.campaign.force.FormationLevel.BATTALION;
import static mekhq.campaign.force.FormationLevel.BRIGADE;
import static mekhq.campaign.force.FormationLevel.COMPANY;
import static mekhq.campaign.force.FormationLevel.CORPS;
import static mekhq.campaign.force.FormationLevel.DIVISION;
import static mekhq.campaign.force.FormationLevel.LANCE;
import static mekhq.campaign.force.FormationLevel.NONE;
import static mekhq.campaign.force.FormationLevel.REGIMENT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;

public class KillAwards {
    private static final MMLogger LOGGER = MMLogger.create(KillAwards.class);

    /**
     * This function loops through Kill Awards, checking whether the person is eligible to receive each type of award
     *
     * @param campaign the campaign to be processed
     * @param mission  the mission just completed
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where item == Kill)
     * @param killData the pre-processed list of kills mapped to Force ID
     */
    public static Map<Integer, List<Object>> KillAwardProcessor(Campaign campaign, Mission mission, UUID person,
          List<Award> awards, Map<Integer, List<Kill>> killData) {
        List<Award> individualAwards = new ArrayList<>();

        List<Award> groupAwards = new ArrayList<>();
        // the int corresponds to force depth (distance from origin force)
        List<Award> awardsDepth0 = new ArrayList<>();
        List<Award> awardsDepth1 = new ArrayList<>();
        List<Award> awardsDepth2 = new ArrayList<>();
        List<Award> awardsDepth3 = new ArrayList<>();
        List<Award> awardsDepth4 = new ArrayList<>();
        List<Award> awardsDepth5 = new ArrayList<>();
        List<Award> awardsDepth6 = new ArrayList<>();
        List<Award> awardsDepth7 = new ArrayList<>();

        Award bestAward = new Award();
        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            int killsNeeded;
            String awardScope;

            if (award.canBeAwarded(campaign.getPerson(person))) {
                List<String> validOptions = Arrays.asList("scenario", "mission", "lifetime");

                if (validOptions.contains(award.getRange().toLowerCase())) {
                    awardScope = award.getRange().toLowerCase();
                } else {
                    LOGGER.warn("Award {} from the {} set has invalid range value {}. Skipping",
                          award.getName(), award.getSet(), award.getRange());
                    continue;
                }

                // scenario kill awards are handled by their own class
                if (awardScope.equals("scenario")) {
                    continue;
                }

                try {
                    killsNeeded = award.getQty();
                } catch (Exception e) {
                    LOGGER.warn("Award {} from the {} set has invalid range qty {}. Skipping",
                          award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                FormationLevel awardDepth = getFormation(award);
                List<Integer> killCount = new ArrayList<>();

                // with all the parameters validated, we can begin processing the award
                if ((awardDepth.isNone()) && (awardScope.equalsIgnoreCase("lifetime"))) {
                    killCount.add(campaign.getKillsFor(person).size());
                } else if ((!awardDepth.isNone()) && (awardScope.equalsIgnoreCase("lifetime"))) {
                    LOGGER.warn(
                          "Award {} from the {} set has a invalid combination: range value {} with size {}. Skipping",
                          award.getName(), award.getSet(), award.getRange(), award.getSize());
                    continue;
                }

                if (awardScope.equalsIgnoreCase("mission")) {
                    List<Kill> killCredits = campaign.getKillsFor(person).stream()
                                                   .filter(kill -> kill.getMissionId() == mission.getId())
                                                   .toList();

                    // -1 corresponds to 'individual', so we only care about the pilot's personal
                    // kills
                    if (awardDepth == NONE) {
                        killCount.add(killCredits.size());

                        // otherwise, we need to identify all relevant kills across the TO&E
                    } else {
                        FormationLevel maximumDepth = campaign.getForce(0).getFormationLevel();

                        // in the event that the depth of the award exceeds the highest depth of the
                        // origin force
                        // (the one named after the campaign), we can cheat and just total all kills
                        if (maximumDepth.getDepth() < awardDepth.getDepth()) {
                            Set<String> identifiers = new HashSet<>();
                            List<Kill> sanitizedKills = new ArrayList<>();

                            for (Integer force : killData.keySet()) {
                                if (force != -1) { // a value of -1 implies no force was recorded for that kill
                                    for (Kill kill : killData.get(force)) {
                                        String awardIdentifier = kill.getAwardIdentifier();
                                        if (!identifiers.contains(awardIdentifier)) {
                                            identifiers.add(awardIdentifier);
                                            sanitizedKills.add(kill);
                                        }
                                    }
                                }
                            }

                            killCount.add(sanitizedKills.size());

                            // if we can't cheat, we need to read the TO&E and gather a list of appropriate
                            // kill counts.
                        } else {
                            List<Integer> forceCredits = new ArrayList<>();

                            for (Kill kill : killCredits) {
                                if (!forceCredits.contains(kill.getForceId())) {
                                    forceCredits.add(kill.getForceId());
                                }
                            }

                            // first, we build a list of all the forces the character has at least one kill
                            // in
                            int originForce = -1; // this number is used as a 'force id missing' proxy

                            // if the characters' current force isn't in that list, we add it
                            // this will fail if the character doesn't have a unit,
                            // but that's ok, in that case we just use a default value
                            try {
                                originForce = campaign.getPerson(person).getUnit().getForceId();
                            } catch (Exception ignored) {}

                            if ((originForce != -1) && (!forceCredits.contains(originForce))) {
                                forceCredits.add(originForce);
                            }

                            // next, we need to cycle through each force the character has credits in,
                            // walking through the TO&E and gathering any associated kills
                            for (int forceId : forceCredits) {
                                originForce = forceId;
                                List<Kill> temporaryKills = new ArrayList<>();

                                try {
                                    // Get the current formation depth of the origin force
                                    int depth = campaign.getForce(originForce).getFormationLevel().getDepth();

                                    // Continue the loop until the depth of the original force is not smaller than
                                    // the award depth
                                    while (depth < awardDepth.getDepth()) {
                                        // Get the ID of the origin force's parent force
                                        int parentForce = campaign.getForce(originForce).getParentForce().getId();

                                        // If the depth is greater or equal to the maximum depth, exit the loop
                                        if (depth >= maximumDepth.getDepth()) {
                                            break;
                                        }

                                        // Set the origin force to its parent force
                                        originForce = parentForce;
                                        // Update the depth to the depth of the new origin force
                                        depth = campaign.getForce(originForce).getFormationLevel().getDepth();
                                    }

                                    Formation originNode = campaign.getForce(originForce);
                                    temporaryKills = walkToeForKills(killData, originNode);
                                } catch (Exception e) {
                                    LOGGER.warn("Could not walk toe for force {}. Exception: {} Stacktrace: {}",
                                          originForce, e.getMessage(), e.getStackTrace());
                                    temporaryKills.addAll(killData.get(forceId));
                                }

                                Set<String> identifiers = new HashSet<>();
                                List<Kill> sanitizedKills = new ArrayList<>();

                                for (Kill kill : temporaryKills) {
                                    String awardIdentifier = kill.getAwardIdentifier();

                                    if (!identifiers.contains(awardIdentifier)) {
                                        identifiers.add(awardIdentifier);
                                        sanitizedKills.add(kill);
                                    }
                                }

                                int kills = sanitizedKills.size();
                                if (kills >= killsNeeded) {
                                    killCount.add(kills);
                                    break;
                                }
                            }
                        }
                    }

                    // now check whether any of the entries in killCount meet or beat the score
                    // required for the award;
                    for (Integer kill : killCount) {
                        if (kill >= killsNeeded) {
                            if (awardDepth.isNone()) {
                                individualAwards.add(award);
                            } else {
                                groupAwards.add(award);
                            }

                            break;
                        }
                    }
                }
            }
        }

        // At the point, all Awards have been processed, and we just need to communicate
        // eligibility
        int rollingQty = 0;

        if (!individualAwards.isEmpty()) {
            // if isIssueBestAwardOnly we need to do some filtering
            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
                for (Award a : individualAwards) {
                    if (a.getQty() > rollingQty) {
                        rollingQty = a.getQty();
                        bestAward = a;
                    }
                }

                eligibleAwards.add(bestAward);
            } else {
                eligibleAwards.addAll(individualAwards);
            }
        }

        if (!groupAwards.isEmpty()) {
            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
                // we need to filter groupAwards into discrete Award Groups.
                // otherwise, Forces become ineligible for Awards they should be entitled to
                // if they are eligible for a 'better' Award from another Award Group.
                // by removing each Award, as they're filtered, we can ensure all Awards have
                // been removed
                for (Award award : groupAwards) {
                    List<Award> targetList;

                    switch (getFormation(award).getDepth()) {
                        case 0 -> targetList = awardsDepth0;
                        case 1 -> targetList = awardsDepth1;
                        case 2 -> targetList = awardsDepth2;
                        case 3 -> targetList = awardsDepth3;
                        case 4 -> targetList = awardsDepth4;
                        case 5 -> targetList = awardsDepth5;
                        case 6 -> targetList = awardsDepth6;
                        case 7 -> targetList = awardsDepth7;
                        default -> throw new IllegalStateException("Unexpected value in getFormation: " +
                                                                         getFormation(award));
                    }

                    targetList.add(award);
                }

                // As mentioned previously, the int after 'groupAwards' corresponds to
                // formationDepth
                List<List<Award>> allGroupAwards = Arrays.asList(awardsDepth0, awardsDepth1, awardsDepth2,
                      awardsDepth3, awardsDepth4, awardsDepth5, awardsDepth6, awardsDepth7);

                for (List<Award> awardGroup : allGroupAwards) {
                    if (!awardGroup.isEmpty()) {
                        for (Award award : awardGroup) {
                            rollingQty = 0;

                            if (award.getQty() > rollingQty) {
                                bestAward = award;
                            }
                        }

                        eligibleAwards.add(bestAward);
                    }
                }
            } else {
                eligibleAwards.addAll(groupAwards);
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }

    /**
     * Traverses the graph of Forces, starting from an origin Force, to collect associated kills from each eligible
     * Force node in the graph. The traversal uses a depth-first search approach to visit each Force node.
     *
     * @param killData   the map containing kill records wherein the key is the Force ID and the value is a list of
     *                   associated Kill objects.
     * @param originNode the initial Force node from which the traversal begins.
     *
     * @return a list of Kill objects that are associated with the traversed Force nodes
     */
    private static List<Kill> walkToeForKills(Map<Integer, List<Kill>> killData, Formation originNode) {
        List<Kill> kills = new ArrayList<>();

        Stack<Formation> stack = new Stack<>();
        // we add visited nodes to a set, so we don't run the risk of re-evaluating
        // previously visited nodes
        Set<Integer> visitedForces = new HashSet<>();
        stack.push(originNode);

        while (!stack.isEmpty()) {
            Formation currentNode = stack.pop();

            if (!visitedForces.contains(currentNode.getId())) {
                if (killData.containsKey(currentNode.getId())) {
                    kills.addAll(killData.get(currentNode.getId()));
                }

                for (Formation subFormation : currentNode.getSubForces()) {
                    stack.push(subFormation);
                }

                visitedForces.add(currentNode.getId());
            }
        }

        return kills;
    }

    /**
     * @param award the award to determine the FormationLevel for
     *
     * @return the FormationLevel enum based on the size of the award.
     */
    private static FormationLevel getFormation(Award award) {
        return switch (award.getSize().toLowerCase()) {
            case "individual" -> NONE;
            case "lance" -> LANCE;
            case "company" -> COMPANY;
            case "battalion" -> BATTALION;
            case "regiment" -> REGIMENT;
            case "brigade" -> BRIGADE;
            case "division" -> DIVISION;
            case "corps" -> CORPS;
            case "army" -> ARMY;
            default -> {
                LOGGER.warn("Award {} from the {} set has invalid size value {}",
                      award.getName(),
                      award.getSet(),
                      award.getSize());
                yield NONE;
            }
        };
    }
}
