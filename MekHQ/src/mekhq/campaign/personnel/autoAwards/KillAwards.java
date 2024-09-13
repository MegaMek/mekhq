/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.force.Force;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;

public class KillAwards {
    private static final MMLogger logger = MMLogger.create(KillAwards.class);

    /**
     * This function loops through Kill Awards, checking whether the person is
     * eligible to receive each type of award
     *
     * @param campaign the campaign to be processed
     * @param mission  the mission just completed
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where
     *                 item == Kill)
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
                    logger.warn("Award {} from the {} set has invalid range value {}. Skipping",
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
                    logger.warn("Award {} from the {} set has invalid range qty {}. Skipping",
                            award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                FormationLevel awardDepth = getFormation(award);
                List<Integer> killCount = new ArrayList<>();

                // with all the parameters validated, we can begin processing the award
                if ((awardDepth.isNone()) && (awardScope.equalsIgnoreCase("lifetime"))) {
                    killCount.add(campaign.getKillsFor(person).size());
                } else if ((!awardDepth.isNone()) && (awardScope.equalsIgnoreCase("lifetime"))) {
                    logger.warn(
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
                            killCount.add(killData.keySet().stream()
                                    .mapToInt(force -> force)
                                    .filter(force -> force != -1) // a value of -1 means no force was recorded for that
                                                                  // kill
                                    .map(force -> killData.get(force).size())
                                    .sum());

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
                            } catch (Exception ignored) {
                            }

                            if ((originForce != -1) && (!forceCredits.contains(originForce))) {
                                forceCredits.add(originForce);
                            }

                            // next, we need to cycle through each force the character has credits in,
                            // walking through the TO&E and gathering any associated kills
                            for (int forceId : forceCredits) {
                                originForce = forceId;
                                int kills;

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

                                    Force originNode = campaign.getForce(originForce);
                                    kills = walkToeForKills(killData, originNode, new HashSet<>(forceCredits));
                                } catch (Exception e) {
                                    kills = killData.get(forceId).size();
                                }

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
                        default ->
                            throw new IllegalStateException("Unexpected value in getFormation: " + getFormation(award));
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
     * Calculates the total number of kills from a given originNode in the killData
     * map.
     *
     * @param killData     the map of kill records mapped to Force ID
     * @param originNode   the Force node to start the traversal from
     * @param forceCredits the set of Force IDs eligible for kills
     * @return the total number of kills for the originNode
     */
    private static int walkToeForKills(Map<Integer, List<Kill>> killData, Force originNode, Set<Integer> forceCredits) {
        int kills = 0;

        Stack<Force> stack = new Stack<>();
        // we add visited nodes to a set, so we don't run the risk of re-evaluating
        // previously visited nodes
        Set<Integer> visitedForces = new HashSet<>();
        stack.push(originNode);

        while (!stack.isEmpty()) {
            Force currentNode = stack.pop();

            if (!visitedForces.contains(currentNode.getId())) {
                if (forceCredits.contains(currentNode.getId())) {
                    kills += killData.get(currentNode.getId()).size();
                }

                for (Force subForce : currentNode.getSubForces()) {
                    stack.push(subForce);
                }

                visitedForces.add(currentNode.getId());
            }
        }

        return kills;
    }

    /**
     * @return the FormationLevel enum based on the size of the award.
     *
     * @param award the award to determine the FormationLevel for
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
                logger.warn("Award {} from the {} set has invalid size value {}",
                        award.getName(),
                        award.getSet(),
                        award.getSize());
                yield NONE;
            }
        };
    }
}
