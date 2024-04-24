package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class KillAwards {
    /**
     * This function loops through Kill Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param mission the mission just completed
     * @param person the person to check award eligibility for
     * @param awards the awards to be processed (should only include awards where item == Kill)
     */
    public static Map<Integer, List<Object>> KillAwardProcessor(Campaign campaign, Mission mission, UUID person, List<Award> awards) {
        int formationDepth;
        String killDepth;

        int killCount = 0;
        int killsNeeded;

        List<Award> individualAwards = new ArrayList<>();

        List<Award> groupAwards = new ArrayList<>();
        // the int corresponds to formationDepth
        List<Award> groupAwards1 = new ArrayList<>();
        List<Award> groupAwards2 = new ArrayList<>();
        List<Award> groupAwards3 = new ArrayList<>();
        List<Award> groupAwards4 = new ArrayList<>();
        List<Award> groupAwards5 = new ArrayList<>();
        List<Award> groupAwards6 = new ArrayList<>();
        List<Award> groupAwards7 = new ArrayList<>();
        List<Award> groupAwards8 = new ArrayList<>();

        Award bestAward = new Award();
        List<Award>  eligibleAwards = new ArrayList<>();

        int maximumDepth;
        int forceId;
        int forceDepth;

        try {
            maximumDepth = Force.getMaximumDepth(campaign.getForce(0), null);
            forceId = campaign.getPerson(person).getUnit().getForceId();
            forceDepth = Force.getDepth(campaign.getForce(forceId));
        } catch (Exception e) {
            LogManager.getLogger().info("AutoAwards failed to fill essential values for {}, using defaults",
                    campaign.getPerson(person).getFullName()   );
            forceId = -1;
            maximumDepth = 0;
            forceDepth = 0;
        }

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                // this allows us to convert non-IS formations into IS equivalents
                formationDepth = getFormation(campaign, award);

                // we skip, if an invalid formationDepth has been provided
                if (formationDepth == -1) {
                    continue;
                }

                // we also skip if the unit isn't assigned to a Force and this isn't an Individual Kill Award
                if ((formationDepth != 0) && (forceId == -1)) {
                    continue;
                }

                List<String> validOptions = Arrays.asList("scenario", "mission", "lifetime");

                if (validOptions.contains(award.getRange().toLowerCase())) {
                    killDepth = award.getRange().toLowerCase();
                } else {
                    LogManager.getLogger().warn("Award {} from the {} set has invalid range value {}",
                            award.getName(), award.getSet(), award.getRange());
                    continue;
                }

                // scenario kill awards are handled by their own class
                if (killDepth.equals("scenario")) {
                    continue;
                }

                try {
                    killsNeeded = award.getQty();
                } catch (Exception e) {
                    LogManager.getLogger().warn("Award {} from the {} set has invalid range qty {}",
                            award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                // with all the parameters validated we can begin processing the award
                switch (formationDepth) {
                    // individual
                    case 0:
                        killCount = getIndividualKills(campaign, mission, person, killDepth.equals("mission"));

                        break;
                    // lance
                    case 1:
                        // if the maximum depth is <= than the depth we're searching, we can just cheat
                        // and get all kills
                        if (maximumDepth <= 1) {
                            killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (forceDepth == maximumDepth) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(campaign, mission, forceId, killDepth.equals("mission"));
                        }

                        break;
                    // company
                    case 2:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 2) {
                            killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        } else if (forceDepth == maximumDepth - 1) {
                            killCount = getAllForceKills(campaign, mission, forceId, killDepth.equals("mission"));
                        }

                        break;
                    // battalion
                    case 3:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 3) {
                            killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        } else if (forceDepth == maximumDepth - 2) {
                            killCount = getAllForceKills(campaign, mission, forceId, killDepth.equals("mission"));
                        }

                        break;
                    // regiment
                    case 4:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 4) {
                            killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        } else if (forceDepth == maximumDepth - 3) {
                            killCount = getAllForceKills(campaign, mission, forceId, killDepth.equals("mission"));
                        }

                        break;
                    // brigade
                    case 5:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 5) {
                            killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        } else if (forceDepth == maximumDepth - 4) {
                            killCount = getAllForceKills(campaign, mission, forceId, killDepth.equals("mission"));
                        }

                        break;
                    // division
                    case 6:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 6) {
                            killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        } else if (forceDepth == maximumDepth - 5) {
                            killCount = getAllForceKills(campaign, mission, forceId, killDepth.equals("mission"));
                        }

                        break;
                    // corps
                    case 7:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 7) {
                            killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        } else if (forceDepth == maximumDepth - 6) {
                            killCount = getAllForceKills(campaign, mission, forceId, killDepth.equals("mission"));
                        }

                        break;
                    // army
                    case 8:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 8) {
                            killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        } else if (forceDepth == maximumDepth - 7) {
                            killCount = getAllForceKills(campaign, mission, forceId, killDepth.equals("mission"));
                        }

                        break;
                    default:
                        throw new IllegalStateException("Unexpected value in formationDepth: " + formationDepth);
                }

                if (killCount >= killsNeeded) {
                    if (formationDepth == 0) {
                        individualAwards.add(award);
                    } else {
                        groupAwards.add(award);
                    }
                }
            }
        }

        // At the point, all Awards have been processed, and we just need to communicate eligibility
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
                // we need to filter groupAwards into discrete Award Groups, as otherwise Forces become ineligible to
                // Awards they should be eligible for, if they are eligible for a 'better' Award from another Award Group
                // By removing each Award, as they're filtered, we can ensure all Awards have been removed
                for (Award award : groupAwards) {
                    switch (getFormation(campaign, award)) {
                        case 1:
                            groupAwards1.add(award);
                            break;
                        case 2:
                            groupAwards2.add(award);
                            break;
                        case 3:
                            groupAwards3.add(award);
                            break;
                        case 4:
                            groupAwards4.add(award);
                            break;
                        case 5:
                            groupAwards5.add(award);
                            break;
                        case 6:
                            groupAwards6.add(award);
                            break;
                        case 7:
                            groupAwards7.add(award);
                            break;
                        case 8:
                            groupAwards8.add(award);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value in getFormation: " + getFormation(campaign, award));
                    }
                }

                // As mentioned previously, the int after 'groupAwards' corresponds to formationDepth
                List<List<Award>> allGroupAwards = Arrays.asList(groupAwards1, groupAwards2,
                        groupAwards3, groupAwards4, groupAwards5, groupAwards6, groupAwards7,
                        groupAwards8);

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
         * This function uses switches to translate non-IS formations into the IS equivalent. It also
         * validates award size, returning 'invalid' if a malformed size is provided.
         * @param award the award providing the formation
         */
        private static int getFormation(Campaign campaign, Award award){
            int formationDepth;

            switch (award.getSize().toLowerCase()) {
                case "individual":
                    formationDepth = 0;
                    break;
                case "lance":
                    formationDepth = 1;
                    break;
                case "company":
                    formationDepth = 2;
                    break;
                case "battalion":
                    formationDepth = 3;
                    break;
                case "regiment":
                    formationDepth = 4;
                    break;
                case "brigade":
                    formationDepth = 5;
                    break;
                case "division":
                    formationDepth = 6;
                    break;
                case "corps":
                    formationDepth = 7;
                    break;
                case "army":
                    formationDepth = 8;
                    break;
                default:
                    LogManager.getLogger().warn("Award {} from the {} set has invalid size value {}", award.getName(), award.getSet(), award.getSize());
                    formationDepth = -1;
            }

            switch (formationDepth) {
                case -1:
                    return -1;
                case 0:
                    if (campaign.getCampaignOptions().isEnableIndividualKillAwards()) {
                        return formationDepth;
                    } else {
                        return -1;
                    }
                // default will catch anything over 0
                default:
                    if (campaign.getCampaignOptions().isEnableFormationKillAwards()) {
                        return formationDepth;
                    } else {
                        return -1;
                    }
            }
        }

        /**
         * This function gathers kills from all the force and all sub-forces
         * @param campaign the campaign being processed
         * @param mission the mission just completed
         * @param forceId the id for the force we want to parse
         * @param filterOtherMissionKills true if we should only count kills from the mission just completed
         */
        private static int getAllForceKills(Campaign campaign, Mission mission, int forceId, boolean filterOtherMissionKills){
            int killCount = 0;

            // this grabs the kills for any loose units that exist outside of lances
            Vector<Force> subForces = campaign.getForce(forceId).getSubForces();

            killCount += getForceKills(campaign, mission, forceId, filterOtherMissionKills);

            if (!subForces.isEmpty()) {
                killCount += subForces.stream().mapToInt(subforce
                        -> getAllForceKills(campaign, mission, subforce
                        .getId(), filterOtherMissionKills)).sum();
            }

            return killCount;
        }

        /**
         * This function gathers kills from the entire force
         * @param campaign the campaign being processed
         * @param mission the mission just completed
         * @param forceId the id for the force we want to parse
         * @param filterOtherMissionKills true if we should only count kills from the mission just completed
         */
        private static int getForceKills(Campaign campaign, Mission mission, int forceId, boolean filterOtherMissionKills){
            int killCount;

            Vector<UUID> units = campaign.getForce(forceId).getUnits();

            if (!units.isEmpty()) {
                killCount = units.stream().mapToInt(unit -> getIndividualKills(campaign, mission,
                        campaign.getUnit(unit).getCommander().getId(), filterOtherMissionKills)).sum();

                return killCount;
            } else {
                return 0;
            }
        }

        /**
         * This function gathers kills from individual personnel
         * @param campaign the campaign being processed
         * @param mission the mission just completed
         * @param commander the unit commander whose kills are being counted
         * @param filterOtherMissionKills true if we should only count kills from the mission just completed
         */
        private static int getIndividualKills(Campaign campaign, Mission mission, UUID commander, boolean filterOtherMissionKills){
            List<Kill> allKills;

            try {
                allKills = campaign.getKillsFor(campaign.getPerson(commander).getId());
            } catch (Exception e) {
                return 0;
            }

            if (filterOtherMissionKills) {
                allKills.removeIf(kill -> kill.getMissionId() != mission.getId());
            }

            return allKills.size();
        }
    }
