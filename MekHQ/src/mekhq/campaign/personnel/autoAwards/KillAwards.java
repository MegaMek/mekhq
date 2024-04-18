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
    /**
     * This function loops through Kill Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param campaign the mission just completed
     * @param awards the awards to be processed (should only include awards where item == Kill)
     * @param person the person to check award eligibility for
     */
    public KillAwards(Campaign campaign, Mission mission, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        int formationDepth;
        String killDepth;
        int killCount = 0;
        int killsNeeded;

        // mhq uses a forceId of -1 to signify that the unit is not assigned to a force
        int forceId = -1;
        // a maximumDepth of 0 just means the only entry in the TOE is the origin force
        int maximumDepth = 0;

        for (Award award : awards) {
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
                    killCount = getIndividualKills(campaign, mission, person, killDepth.equals("mission"));

                    break;
                // lance
                case 1:
                    // if the maximum depth is <= than the depth we're searching, we can just cheat and get all kills
                    if (maximumDepth <= 1) {
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    // otherwise, check to make sure force is of the type we're looking for
                    } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth) {
                        // if it is, get all the kills for the force and any child forces
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    }

                    break;
                // company
                case 2:
                    maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                    // if the maximum depth is <= than the depth we're searching, we can just cheat and get all kills
                    if (maximumDepth <= 2) {
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        // otherwise, check to make sure force is of the type we're looking for
                    } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 1) {
                        // if it is, get all the kills for the force and any child forces
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    }

                    break;
                // battalion
                case 3:
                    maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                    // if the maximum depth is <= than the depth we're searching, we can just cheat and get all kills
                    if (maximumDepth <= 3) {
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        // otherwise, check to make sure force is of the type we're looking for
                    } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 2) {
                        // if it is, get all the kills for the force and any child forces
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    }

                    break;
                // regiment
                case 4:
                    maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                    // if the maximum depth is <= than the depth we're searching, we can just cheat and get all kills
                    if (maximumDepth <= 4) {
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        // otherwise, check to make sure force is of the type we're looking for
                    } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 3) {
                        // if it is, get all the kills for the force and any child forces
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    }

                    break;
                // brigade
                case 5:
                    maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                    // if the maximum depth is <= than the depth we're searching, we can just cheat and get all kills
                    if (maximumDepth <= 5) {
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        // otherwise, check to make sure force is of the type we're looking for
                    } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 4) {
                        // if it is, get all the kills for the force and any child forces
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    }

                    break;
                // division
                case 6:
                    maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                    // if the maximum depth is <= than the depth we're searching, we can just cheat and get all kills
                    if (maximumDepth <= 6) {
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        // otherwise, check to make sure force is of the type we're looking for
                    } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 5) {
                        // if it is, get all the kills for the force and any child forces
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    }

                    break;
                // corps
                case 7:
                    maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                    // if the maximum depth is <= than the depth we're searching, we can just cheat and get all kills
                    if (maximumDepth <= 7) {
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        // otherwise, check to make sure force is of the type we're looking for
                    } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 6) {
                        // if it is, get all the kills for the force and any child forces
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    }

                    break;
                // army
                case 8:
                    maximumDepth = Force.getMaximumDepth(campaign.getForce(forceId), null);

                    // if the maximum depth is <= than the depth we're searching, we can just cheat and get all kills
                    if (maximumDepth <= 8) {
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                        // otherwise, check to make sure force is of the type we're looking for
                    } else if (Force.getDepth(campaign.getForce(forceId)) == maximumDepth - 7) {
                        // if it is, get all the kills for the force and any child forces
                        killCount = getAllForceKills(campaign, mission, 0, killDepth.equals("mission"));
                    }

                    break;
            }

            if (killCount >= killsNeeded) {
                // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                campaign.addReport(person.getHyperlinkedName() + ' ' +
                        MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                award.getName(), award.getSet()));
            }
        }
    }

    /**
     * This function uses switches to translate non-IS formations into their IS equivalent. It also
     * validates award size, returning 'invalid' if a malformed size is provided.
     * @param award the award providing the formation
     */
    private int getFormation(Award award) {
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
                LogManager.getLogger().warn("Award {} from the {} set has invalid size value {}",
                        award.getName(), award.getSet(), award.getSize());
                return -1;
        }
    }

    /**
     * This function gathers kills from all the force and all subforces
     * @param campaign the campaign being processed
     * @param mission the mission just completed
     * @param forceId the Id for the force we want to parse
     * @param filterOtherMissionKills true if we should only count kills from the mission just completed
     */
    private int getAllForceKills(Campaign campaign, Mission mission, int forceId, boolean filterOtherMissionKills) {
        int killCount = 0;

        // this grabs the kills for any loose units that exist outside of lances
        Vector<Force> subforces = campaign.getForce(forceId).getSubForces();

        killCount += getForceKills(campaign, mission, forceId, filterOtherMissionKills);

        if (!subforces.isEmpty()) {
            for (Force subforce : subforces) {
                killCount += getAllForceKills(campaign, mission, subforce.getId(), filterOtherMissionKills);
            }
        }

        return killCount;
    }

    /**
     * This function gathers kills from the entire force
     * @param campaign the campaign being processed
     * @param mission the mission just completed
     * @param forceId the Id for the force we want to parse
     * @param filterOtherMissionKills true if we should only count kills from the mission just completed
     */
    private int getForceKills(Campaign campaign, Mission mission, int forceId, boolean filterOtherMissionKills) {
        int killCount = 0;

        Vector<UUID> units = campaign.getForce(forceId).getUnits();

        if (!units.isEmpty()) {
            for (UUID unit : units) {
                killCount += getIndividualKills(campaign, mission, campaign.getUnit(unit).getCommander(), filterOtherMissionKills);
            }

            return killCount;
        } else {
            return 0;
        }
    }

    /**
     * This function gathers kills from individual personnel
     * @param campaign the campaign being processed
     * @param mission the mission just completed
     * @param person the person whose kills are being counted
     * @param filterOtherMissionKills true if we should only count kills from the mission just completed
     */
    private int getIndividualKills(Campaign campaign, Mission mission, Person person, boolean filterOtherMissionKills) {
        List<Kill> allKills = campaign.getKillsFor(person.getId());

        if (filterOtherMissionKills) {
            allKills.removeIf(kill -> kill.getMissionId() != mission.getId());
        }

        return allKills.size();
    }
}
