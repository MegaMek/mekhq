package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Kill;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.*;

public class KillAwards {
    Campaign campaign;
    Mission mission;
    Person person;

    /**
     * This function loops through Kill Awards, checking whether the person is eligible to receive each type of award
     * @param c the campaign to be processed
     * @param m the mission just completed
     * @param awards the awards to be processed (should only include awards where item == Kill)
     * @param p the person to check award eligibility for
     */
    public KillAwards(Campaign c, Mission m, List<Award> awards, Person p) {
        campaign = c;
        mission = m;
        person = p;

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

        // mhq uses a forceId of -1 to signify that the unit is not assigned to a force
        int forceId = -1;
        // a maximumDepth of 0 just means the only entry in the TOE is the origin force
        int maximumDepth = 0;

        for (Award award : awards) {
            if (award.canBeAwarded(person)) {
                // this allows us to convert non-IS formations into IS equivalents
                formationDepth = getFormation(award);

                if (formationDepth == -1) {
                    continue;
                }
                if (formationDepth != 0) {
                    try {
                        forceId = person.getUnit().getForceId();
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);
                    } catch (Exception e) {
                        // if person is not assigned to a unit, there is no need to count their kills
                        continue;
                    }

                    if (forceId == -1) {
                        // likewise, if the unit isn't assigned to a force, we ignore their kills
                        continue;
                    }
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
                        killCount = getIndividualKills(person, killDepth.equals("mission"));

                        break;
                    // lance
                    case 1:
                        // if the maximum depth is <= than the depth we're searching, we can just cheat
                        // and get all kills
                        if (maximumDepth <= 1) {
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                        }

                        break;
                    // company
                    case 2:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 2) {
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 1) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                        }

                        break;
                    // battalion
                    case 3:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 3) {
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 2) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                        }

                        break;
                    // regiment
                    case 4:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 4) {
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 3) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                        }

                        break;
                    // brigade
                    case 5:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 5) {
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 4) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                        }

                        break;
                    // division
                    case 6:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 6) {
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 5) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                        }

                        break;
                    // corps
                    case 7:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 7) {
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 6) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                        }

                        break;
                    // army
                    case 8:
                        maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                        if (maximumDepth <= 8) {
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                            // otherwise, check to make sure force is of the type we're looking for
                        } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 7) {
                            // if it is, get all the kills for the force and any child forces
                            killCount = getAllForceKills(0, killDepth.equals("mission"));
                        }

                        break;
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
                    if (getFormation(award) == 1) {
                        groupAwards1.add(award);
                    } else if (getFormation(award) == 2) {
                        groupAwards2.add(award);
                    } else if (getFormation(award) == 3) {
                        groupAwards3.add(award);
                    } else if (getFormation(award) == 4) {
                        groupAwards4.add(award);
                    } else if (getFormation(award) == 5) {
                        groupAwards5.add(award);
                    } else if (getFormation(award) == 6) {
                        groupAwards6.add(award);
                    } else if (getFormation(award) == 7) {
                        groupAwards7.add(award);
                    } else if (getFormation(award) == 8) {
                        groupAwards8.add(award);
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

        // with everything processed and filtered, we can finally announce Award eligibility
        if (!eligibleAwards.isEmpty()) {
            for (Award award : eligibleAwards) {
                announceEligibility(award);
            }
        }
    }

        /**
         * This function uses switches to translate non-IS formations into their IS equivalent. It also
         * validates award size, returning 'invalid' if a malformed size is provided.
         * @param award the award providing the formation
         */
        private int getFormation (Award award){
            switch (award.getSize().toLowerCase()) {
                case "individual":
                    return 0;
                case "lance":
                    return 1;
                case "company":
                    return 2;
                case "battalion":
                    return 3;
                case "regiment":
                    return 4;
                case "brigade":
                    return 5;
                case "division":
                    return 6;
                case "corps":
                    return 7;
                case "army":
                    return 8;
                default:
                    LogManager.getLogger().warn("Award {} from the {} set has invalid size value {}", award.getName(), award.getSet(), award.getSize());
                    return -1;
            }
        }

        /**
         * This function gathers kills from all the force and all subforces
         * @param forceId the Id for the force we want to parse
         * @param filterOtherMissionKills true if we should only count kills from the mission just completed
         */
        private int getAllForceKills ( int forceId, boolean filterOtherMissionKills){
            int killCount = 0;

            // this grabs the kills for any loose units that exist outside of lances
            Vector<Force> subforces = campaign.getForce(forceId).getSubForces();

            killCount += getForceKills(forceId, filterOtherMissionKills);

            if (!subforces.isEmpty()) {
                for (Force subforce : subforces) {
                    killCount += getAllForceKills(subforce.getId(), filterOtherMissionKills);
                }
            }

            return killCount;
        }

        /**
         * This function gathers kills from the entire force
         * @param forceId the Id for the force we want to parse
         * @param filterOtherMissionKills true if we should only count kills from the mission just completed
         */
        private int getForceKills ( int forceId, boolean filterOtherMissionKills){
            int killCount;

            Vector<UUID> units = campaign.getForce(forceId).getUnits();

            if (!units.isEmpty()) {
                killCount = units.stream().mapToInt(unit -> getIndividualKills(campaign.getUnit(unit).getCommander(), filterOtherMissionKills)).sum();

                return killCount;
            } else {
                return 0;
            }
        }

        /**
         * This function gathers kills from individual personnel
         * @param commander the unit commander whose kills are being counted
         * @param filterOtherMissionKills true if we should only count kills from the mission just completed
         */
        private int getIndividualKills (Person commander,boolean filterOtherMissionKills){
            List<Kill> allKills;

            try {
                allKills = campaign.getKillsFor(commander.getId());
            } catch (Exception e) {
                return 0;
            }

            if (filterOtherMissionKills) {
                allKills.removeIf(kill -> kill.getMissionId() != mission.getId());
            }

            return allKills.size();
        }

        /**
         * This function announced Award eligibility to the Daily Report pane
         * @param award the award to be announced
         */
        private void announceEligibility (Award award){
            final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                    MekHQ.getMHQOptions().getLocale());

            // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
            campaign.addReport(person.getHyperlinkedName() + ' ' + MessageFormat.format(resource.getString(
                    "EligibleForAwardReport.format"), award.getName(), award.getSet()));
        }
    }
