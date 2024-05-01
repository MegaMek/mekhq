package mekhq.campaign.personnel.education;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import org.apache.logging.log4j.LogManager;

import java.time.DayOfWeek;
import java.util.*;

/**
 * The EducationController class is responsible for managing the education process.
 * It provides methods to begin the education process, calculate education level, and enroll a person into an academy.
 */
public class EducationController {
    /**
     * Begins the education process, including finance checks.
     *
     * @param campaign    The campaign object in which the education process will occur.
     * @param person      The person who will be enrolled in the education process.
     * @param academyName The name of the academy where the person will be enrolled.
     * @param courseIndex The index of the course in the academy where the person will be enrolled.
     * @param campus      The campus where the person will be enrolled. Can be null if the academy is local.
     * @param faction     The faction of the campus.
     */
    public static void beginEducation(Campaign campaign, Person person, String academyName, int courseIndex, @Nullable String campus, String faction) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        Academy academy = getAcademy(person);

        if (academy == null) {
            LogManager.getLogger().error("No academy found with name {}", academyName);
            return;
        }

        int tuition = academy.getTuitionAdjusted(academy.getEducationLevelMin(), getEducationLevel(person, academy));

        // check there is enough money in the campaign & if so, make a debit
        if (getBalance(campaign) < tuition) {
            campaign.addReport(resources.getString("insufficientFunds.text").replaceAll("0", person.getHyperlinkedFullTitle()));
            return;
        } else {
            campaign.getFinances().debit(TransactionType.EDUCATION, campaign.getLocalDate(), Money.of(tuition), resources.getString("payment.text").replaceAll("0", person.getFullName()));
        }

        // with the checks done, and tuition paid, we can enroll Person
        person.setEduCourseIndex(courseIndex);

        if (academy.isLocal()) {
            enrollPerson(campaign, person, academy, null, faction);
        } else {
            enrollPerson(campaign, person, academy, campus, faction);
        }

        campaign.addReport(resources.getString("offToSchool.text").replaceAll("0", person.getFullName()).replaceAll("1", person.getEduAcademyName()).replaceAll("2", String.valueOf(person.getEduDaysOfTravelToAcademy())));
    }

    /**
     * Returns the balance of the given campaign.
     *
     * @param campaign the campaign for which to get the balance
     * @return the balance of the campaign
     */
    private static int getBalance(Campaign campaign) {
        String balance = String.valueOf(campaign.getFinances().getBalance()).replaceAll("CSB ", "");

        if (balance.contains(".")) {
            balance = balance.substring(0, balance.indexOf('.'));
        }

        return Integer.parseInt(balance);
    }

    /**
     * Calculates the education level of a person based on their highest prior education level and
     * the range of education levels offered by an academy.
     *
     * @param person  The person whose education level needs to be determined.
     * @param academy The academy that provides the education.
     * @return The education level of the person.
     * It is calculated as the difference between the person's highest prior education level and the minimum education level offered by the
     * academy, unless the person's highest education level exceeds the maximum education level offered by the academy, in which case the education
     * level is set to the difference between the maximum and minimum education levels.
     */
    private static int getEducationLevel(Person person, Academy academy) {
        int educationLevel = 0;

        if (person.getEduHighestEducation() >= academy.getEducationLevelMax()) {
            educationLevel = academy.getEducationLevelMax() - academy.getEducationLevelMin();
        } else {
            educationLevel += person.getEduHighestEducation() - academy.getEducationLevelMin();
        }

        return educationLevel;
    }

    /**
     * Enrolls a person into an academy and assigns them to a campus.
     * Sets the person's various education-related properties.
     *
     * @param campaign The campaign the person is being enrolled in.
     * @param person   The person being enrolled.
     * @param academy  The academy the person is being enrolled into.
     * @param campus   The campus where the person will be assigned.
     *                 This parameter can be null if the academy is local.
     * @param faction  The faction of the academy the person is being enrolled into.
     */
    public static void enrollPerson(Campaign campaign, Person person, Academy academy, @Nullable String campus, String faction) {
        // if 'campus' is null and the academy isn't local, we need to abort
        if ((campus == null) && (!academy.isLocal())) {
            LogManager.getLogger().error("Non-Local Academy {} with null campus", academy.getName());
            return;
        }

        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.STUDENT);

        person.setEduAcademySet(academy.getSet());
        person.setEduAcademyNameInSet(academy.getName());
        person.setEduDaysOfEducation(academy.getDurationDays());
        person.setEduAcademyFaction(faction);

        if (academy.isLocal()) {
            person.setEduDaysOfTravelToAcademy(2);
            person.setEduAcademySystem(campaign.getCurrentSystem().getName(campaign.getLocalDate()));
        } else {
            person.setEduDaysOfTravelToAcademy(Campaign.getSimplifiedTravelTime(campaign, campaign.getSystemByName(campus)));
            person.setEduAcademySystem(campaign.getSystemByName(campus).getName(campaign.getLocalDate()));
        }

        person.setEduAcademyNameInSet(academy.getName());

        // if the academy is local, we need to generate a name, otherwise we use the listed name
        if (academy.isLocal()) {
            person.setEduAcademyName(generateName(academy.isMilitary(), campaign.getCurrentSystem().getName(campaign.getLocalDate())));
        } else {
            person.setEduAcademyName(person.getEduAcademyNameInSet() + " (" + campus + ')');
        }

        // we have this all the way at the bottom as a bit of insurance.
        // when troubleshooting, if the log isn't getting entered, we know something went wrong when enrolling.
        ServiceLogger.eduEnrolled(person, campaign.getLocalDate(), person.getEduAcademyName());
    }

    /**
     * Generates a name for an academy.
     *
     * @param isMilitary a boolean value indicating if the institution is military or not
     * @param campus     the name of the campus
     * @return the generated name for the educational institution
     * @throws IllegalStateException if there is an unexpected roll value
     */
    public static String generateName(Boolean isMilitary, String campus) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        String type;
        int roll = Compute.d6(1);
        switch (roll) {
            case 1:
                type = resources.getString("typeAcademy.text");
                break;
            case 2:
                type = resources.getString("typeCollege.text");
                break;
            case 3:
                type = resources.getString("typeInstitute.text");
                break;
            case 4:
                type = resources.getString("typeUniversity.text");
                break;
            case 5:
                type = resources.getString("typePolytechnic.text");
                break;
            case 6:
                type = resources.getString("typeSchool.text");
                break;
            default:
                throw new IllegalStateException("Unexpected roll in generateName() (type): " + roll);
        }

        String suffix;
        boolean systemNameInSuffix = false;
        roll = Compute.d6(1);
        if (roll <= 3) {
            suffix = campus;
            systemNameInSuffix = true;
        } else {
            roll = Compute.d6(1);
            switch (roll) {
                case 1:
                    suffix = resources.getString("suffixTechnology.text");
                    break;
                case 2:
                    suffix = resources.getString("suffixTechnologyAdvanced.text");
                    break;
                case 3:
                    suffix = resources.getString("suffixScience.text");
                    break;
                case 4:
                    suffix = resources.getString("suffixScienceAdvanced.text");
                    break;
                case 5:
                    suffix = resources.getString("suffixStudies.text");
                    break;
                case 6:
                    suffix = resources.getString("suffixHigherLearning.text");
                    break;
                default:
                    throw new IllegalStateException("Unexpected roll in generateName() (suffix): " + roll);
            }
        }

        String prefix = null;
        roll = Compute.d6(1);
        if (!systemNameInSuffix) {
            prefix = campus;
        } else if (isMilitary) {
            switch (roll) {
                case 1:
                    prefix = resources.getString("prefixCombinedArms.text");
                    break;
                case 2:
                    prefix = resources.getString("prefixCombinedForces.text");
                    break;
                case 3:
                    prefix = resources.getString("prefixMilitary.text");
                    break;
                case 4:
                    prefix = resources.getString("prefixWar.text");
                    break;
                case 5:
                    prefix = resources.getString("prefixWarFighting.text");
                    break;
                case 6:
                    prefix = resources.getString("prefixCombat.text");
                    break;
                default:
                    throw new IllegalStateException("Unexpected roll in generateName() (suffix): " + roll);
            }
        }

        StringBuilder name = new StringBuilder();

        if (suffix != null) {
            name.append(suffix).append(' ').append(resources.getString("conjoinerOf.text")).append(' ');
        }

        name.append(type);

        if (prefix != null) {
            name.append(' ').append(resources.getString("conjoinerOf.text")).append(' ').append(prefix);
        }

        return name.toString();
    }

    /**
     * Process a new day in the campaign, updating the education status of each student.
     *
     * @param campaign the campaign object containing the students
     */
    public static void processNewDay(Campaign campaign) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        for (Person person : campaign.getStudents()) {
            Academy academy = getAcademy(person);

            // is person in transit to the institution?
            if (journeyToAcademy(campaign, person, resources)) {
                continue;
            }

            // is person on campus and undergoing education?
            Integer daysOfEducation = ongoingEducation(campaign, person, academy, resources);

            if (daysOfEducation == null) {
                continue;
            }

            // if education has concluded and the journey home hasn't started, we begin the journey
            Integer daysOfTravelFrom = beginJourneyHome(campaign, person, daysOfEducation, resources);

            if (daysOfTravelFrom == null) {
                continue;
            }

            // if we reach this point it means Person is already in transit, so we continue their journey
            processJourneyHome(campaign, person, daysOfEducation, daysOfTravelFrom, resources);
        }
    }

    /**
     * Processes a person's journey to campus.
     *
     * @param campaign  The campaign the person is part of.
     * @param person    The person for whom the journey is being processed.
     * @param resources The resource bundle containing localized strings.
     * @return True if the person's journey to campus was processed, false otherwise.
     */
    private static boolean journeyToAcademy(Campaign campaign, Person person, ResourceBundle resources) {
        int daysOfTravelTo = person.getEduDaysOfTravelToAcademy();

        if (daysOfTravelTo > 0) {
            person.setEduDaysOfTravelToAcademy(daysOfTravelTo - 1);

            // has Person just arrived?
            if ((daysOfTravelTo - 1) == 0) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("arrived.text"));
            }

            return true;
        }
        return false;
    }

    /**
     * Processes a person's ongoing education.
     *
     * @param campaign  The campaign the person is part of.
     * @param person    The person for whom the days of education are being processed.
     * @param academy   the academy the person is attending.
     * @param resources The resource bundle containing localized strings.
     * @return The remaining days of education for the person.
     */
    private static Integer ongoingEducation(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int daysOfEducation = person.getEduDaysOfEducation();

        if (!academy.isPrepSchool()) {
            if (daysOfEducation > 0) {
                person.setEduDaysOfEducation(daysOfEducation - 1);

                if ((daysOfEducation - 1) == 0) {
                    graduateAdult(campaign, person, academy, resources);
                }

                if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                    processNewWeekChecks(campaign, academy, person, daysOfEducation, resources);
                }
                return null;
            }
        } else {
            if (person.getAge(campaign.getLocalDate()) > academy.getAgeMax()) {
                if (academy.isClan()) {
                    graduateClan(campaign, person, academy, resources);
                } else {
                    graduateChild(campaign, person, academy, resources);
                }
                person.setEduDaysOfEducation(0);
            }

            if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                // 11 is used as it still allows for drop-outs
                processNewWeekChecks(campaign, academy, person, daysOfEducation, resources);
            }
            return null;
        }
        return daysOfEducation;
    }

    /**
     * This method begins the journey home for a person after their education.
     *
     * @param campaign        The campaign the person is part of.
     * @param person          The person for whom the journey is being processed.
     * @param daysOfEducation the number education days the person has remaining
     * @param resources       the resource bundle for localization
     * @return the number of travel days from the academy, or null if the person has no education days and no
     * previous travel days
     */
    private static Integer beginJourneyHome(Campaign campaign, Person person, Integer daysOfEducation, ResourceBundle resources) {
        int daysOfTravelFrom = person.getEduDaysOfTravelFromAcademy();

        if ((daysOfEducation == 0) && (daysOfTravelFrom == 0)) {
            int travelTime = Campaign.getSimplifiedTravelTime(campaign, campaign.getSystemByName(person.getEduAcademySystem()));

            // We use a minimum of 2 days travel to avoid awkward grammar in the report.
            // This can be hand waved as being the time it takes for Person to get from campus and
            // recover from their education.
            if (travelTime < 2) {
                travelTime = 2;
            }

            person.setEduDaysOfTravelFromAcademy(travelTime);

            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("returningFromSchool.text"));

            return null;
        }
        return daysOfTravelFrom;
    }

    /**
     * Processes the journey home for a person.
     *
     * @param campaign         the campaign the person is in
     * @param person           the person whose journey home is being processed
     * @param daysOfEducation  the number education days remaining
     * @param daysOfTravelFrom the number of days it takes for the person to travel from the campaign location to the unit
     * @param resources        the ResourceBundle containing localized text resources
     */
    private static void processJourneyHome(Campaign campaign, Person person, Integer daysOfEducation, Integer daysOfTravelFrom, ResourceBundle resources) {
        if ((daysOfEducation < 1) && (daysOfTravelFrom > 0)) {
            int travelTime = Campaign.getSimplifiedTravelTime(campaign, campaign.getSystemByName(person.getEduAcademySystem()));

            // if true, it means the unit has moved since we last ran, so we need to adjust
            if (travelTime > daysOfEducation) {
                person.setEduDaysOfTravelFromAcademy(travelTime);
            } else {
                person.setEduDaysOfEducation(daysOfTravelFrom - 1);
            }

            if ((daysOfTravelFrom - 1) < 1) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("returned.text"));
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
            }
        }
    }

    /**
     * Returns the academy where the given person is enrolled.
     *
     * @param person the person whose academy is being retrieved
     * @return the academy where the person is enrolled,
     * or null if the person is not enrolled in any academy
     */
    private static Academy getAcademy(Person person) {
        List<String> setNames = AcademyFactory.getInstance().getAllSetNames();

        return setNames.stream().filter(setName -> setName.equalsIgnoreCase(person.getEduAcademySet())).map(setName -> AcademyFactory.getInstance().getAllAcademiesForSet(setName)).flatMap(Collection::stream).filter(academy -> String.valueOf(academy.getName()).equals(person.getEduAcademyNameInSet())).findFirst().orElse(null);
    }

    /**
     * Processes the new week checks for a specific person in a campaign.
     *
     * @param campaign        The campaign in which the person is participating.
     * @param academy         The academy where the person is receiving education.
     * @param person          The person whose new week checks need to be processed.
     * @param daysOfEducation The number of days the person has received education.
     * @param resources       The resource bundle used for obtaining localized strings.
     */
    private static void processNewWeekChecks(Campaign campaign, Academy academy, Person person, int daysOfEducation, ResourceBundle resources) {
        if (campaign.getCampaignOptions().isEnableRandomXp()) {
            if (Compute.d6(2) >= academy.getFacultySkill()) {
                person.awardXP(campaign, campaign.getCampaignOptions().getRandomXpRate());
            }
        }

        if (daysOfEducation > 0) {
            // time to check whether the academy is still standing.
            if (checkForAcademyDestruction(campaign, academy, person, resources)) {
                return;
            }

            // is the academy still open?
            if (checkForAcademyClosure(campaign, academy, person, resources)) {
                return;
            }

            // is the academy faction at war with person faction, or the campaign faction?
            if (checkForAcademyFactionConflict(campaign, academy, person, resources)) {
                return;
            }

            // does person want to drop out?
            if (checkForDropout(campaign, academy, person, daysOfEducation, resources)) {
                return;
            }

            // was there a training accident?
            if (checkForTrainingAccidents(campaign, academy, person, resources)) {
                return;
            }
        }
    }

    /**
     * Checks for training accidents for a person in a campaign.
     *
     * @param campaign   the campaign the person is part of
     * @param academy    the academy the person belongs to
     * @param person     the person to check for training accidents
     * @param resources  the resource bundle for getting localized strings
     * @return true if a training accident occurs, otherwise false
     */
    private static boolean checkForTrainingAccidents(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if ((academy.isClan()) || (academy.isMilitary())) {
            int roll;
            int diceSize;

            int warriorDiceSize = campaign.getCampaignOptions().getWarriorCasteAccidents();
            int otherCasteDiceSize = campaign.getCampaignOptions().getOtherCasteAccidents();
            int militaryDiceSize = campaign.getCampaignOptions().getMilitaryAcademyAccidents();

            if (academy.isClan()) {
                if (person.getEduCourseIndex() <= 6) {
                    if (warriorDiceSize > 1) {
                        roll = Compute.randomInt(warriorDiceSize);
                    } else {
                        // this is to avoid throwing an exception if dice size is 0
                        roll = -1;
                    }

                    diceSize = warriorDiceSize;
                } else {
                    if (otherCasteDiceSize > 1) {
                        roll = Compute.randomInt(otherCasteDiceSize);
                    } else {
                        roll = -1;
                    }

                    diceSize = otherCasteDiceSize;
                }
            } else {
                if (militaryDiceSize > 1) {
                    roll = Compute.randomInt(militaryDiceSize);
                } else {
                    roll = -1;
                }

                diceSize = militaryDiceSize;
            }

            if (diceSize > 0) {
                if (roll == 0) {
                    if ((!person.isChild(campaign.getLocalDate())) || (campaign.getCampaignOptions().isAllAges())) {
                        if (Compute.d6(2) >= 5) {
                            roll = Compute.d6(3);
                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventTrainingAccident.text")
                                    .replaceAll("0", String.valueOf(roll)));
                            person.setEduDaysOfEducation(person.getEduDaysOfEducation() + roll);
                        } else {
                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventTrainingAccidentKilled.text"));
                            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACCIDENTAL);
                        }
                    } else {
                        roll = Compute.d6(3);
                        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventTrainingAccident.text")
                                .replaceAll("0", String.valueOf(roll)));
                        person.setEduDaysOfEducation(person.getEduDaysOfEducation() + roll);
                    }

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a person should drop out of an education campaign based on their characteristics and chance factors.
     *
     * @param campaign The campaign the person is enrolled in.
     * @param academy The academy the person is attending.
     * @param person The person being evaluated.
     * @param daysOfEducation The number of days the person has been in education.
     * @param resources The resource bundle containing localization strings.
     * @return true if the person should drop out, false otherwise.
     */
    private static boolean checkForDropout(Campaign campaign, Academy academy, Person person, int daysOfEducation, ResourceBundle resources) {
        int roll;
        int diceSize;

        int adultDiceSize = campaign.getCampaignOptions().getAdultDropoutChance();
        int childDiceSize = campaign.getCampaignOptions().getChildrenDropoutChance();
        int clanDiceSize = campaign.getCampaignOptions().getWarriorCasteDropOutChance();

        if (academy.isClan()) {
            if (clanDiceSize > 1) {
                roll = Compute.randomInt(clanDiceSize);
            } else {
                // this is to avoid an exception for dice sizes of 0
                roll = -1;
            }

            diceSize = clanDiceSize;
        } else if (person.isChild(campaign.getLocalDate())) {
            if (childDiceSize > 1) {
                roll = Compute.randomInt(childDiceSize);
            } else {
                roll = -1;
            }

            diceSize = childDiceSize;
        } else {
            if (adultDiceSize > 1) {
                roll = Compute.randomInt(adultDiceSize);
            } else {
                roll = -1;
            }

            diceSize = adultDiceSize;
        }

        // we add this limiter to avoid a bad play experience when someone drops out in the final stretch
        if (diceSize > 0) {
            if (roll == 0) {
                if (daysOfEducation >= 10) {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOut.text"));

                    // if it's a Sibko we wash them out, instead
                    if (academy.isClan()) {
                        switch (person.getEduCourseIndex()) {
                            case 0:
                            case 1:
                            case 2:
                            case 3:
                            case 4:
                            case 5:
                            case 6:
                                if (processWarriorCasteWashout(campaign, person, resources)) {
                                    return true;
                                }
                                break;
                            case 7:
                                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedScientist.text"));

                                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                                        .replaceAll("0", resources.getString("graduatedScientist.text")) + resources.getString("graduatedWarriorLabour.text"));
                                person.setEduCourseIndex(10);

                                break;
                            case 8:
                                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedMerchant.text"));

                                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                                        .replaceAll("0", resources.getString("graduatedMerchant.text")) + resources.getString("graduatedWarriorLabour.text"));
                                person.setEduCourseIndex(10);

                                break;
                            case 9:
                                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedTechnician.text"));

                                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                                        .replaceAll("0", resources.getString("graduatedTechnician.text")) + resources.getString("graduatedWarriorLabour.text"));
                                person.setEduCourseIndex(10);

                                break;
                            case 10:
                                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedLabour.text"));

                                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                                        .replaceAll("0", resources.getString("graduatedLabour.text")) + resources.getString("washoutLabour.text"));

                                person.setEduDaysOfEducation(0);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected Clan Sibko Course Index: " + person.getEduCourseIndex());
                        }
                    // if it isn't a Sibko, we just drop them out
                    } else {
                        ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName());

                        person.setEduDaysOfEducation(0);
                    }
                } else {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));
                }
            } else if ((roll < (diceSize / 20)) && (!academy.isPrepSchool())) {
                // might as well scare the player
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));
            }
        }
        return false;
    }

    /**
     * Checks for a conflict between the academy faction and the person's faction (or their campaign faction)
     *
     * @param campaign the campaign in which the conflict is being checked
     * @param academy the academy for which the conflict is being checked
     * @param person the person whose faction is being tested for conflict
     * @param resources the resource bundle for localized strings
     * @return true if a faction conflict is found and resolved, false otherwise
     */
    private static boolean checkForAcademyFactionConflict(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if (academy.isFactionConflict(campaign, person)) {
            if (Compute.d6(2) >= 5) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventWar.text"));
                person.setEduDaysOfEducation(person.getEduDaysOfEducation() + Compute.d6(1));
            } else {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventWarExpelled.text"));
                person.setEduDaysOfEducation(0);
            }

            return true;
        }
        return false;
    }

    /**
     * Checks if an academy has been closed.
     *
     * @param campaign   the campaign the academy is in
     * @param academy    the academy being checked for closure
     * @param person     the person enrolled in the academy
     * @param resources  the resource bundle for localization.
     * @return true if the academy has been closed, false otherwise
     */
    private static boolean checkForAcademyClosure(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if (campaign.getLocalDate().getYear() >= academy.getClosureYear()) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventClosure.text"));
            person.setEduDaysOfEducation(0);

            return true;
        }
        return false;
    }

    /**
     * Checks if an academy has been destroyed.
     *
     * @param campaign   the campaign the academy is in
     * @param academy    the academy being checked for destruction
     * @param person     the person enrolled in the academy
     * @param resources  the resource bundle containing localized text
     * @return true if the academy has been destroyed, false otherwise
     */
    private static boolean checkForAcademyDestruction(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if (campaign.getLocalDate().getYear() >= academy.getDestructionYear()) {
            if ((!person.isChild(campaign.getLocalDate())) || (campaign.getCampaignOptions().isAllAges())) {
                if (Compute.d6(2) >= 5) {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventDestruction.text"));
                    person.setEduDaysOfEducation(0);
                } else {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventDestructionKilled.text"));
                    person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.HOMICIDE);
                }
            } else {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventDestruction.text"));
                person.setEduDaysOfEducation(0);
            }
            return true;
        }

        return false;
    }

    /**
     * Process the washout from the warrior caste.
     *
     * @param campaign   the campaign in which the washout is being processed
     * @param person     the person who is being washed out
     * @param resources  the resource bundle containing the necessary text resources
     * @return true if the washout is successful, false otherwise
     */
    private static boolean processWarriorCasteWashout(Campaign campaign, Person person, ResourceBundle resources) {
        ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedWarrior.text"));

        int totalOptionCount = campaign.getCampaignOptions().getFallbackLabour() + campaign.getCampaignOptions().getFallbackMerchant()
                + campaign.getCampaignOptions().getFallbackScientist() + campaign.getCampaignOptions().getFallbackTechnician();
        int rollingOptionCount = campaign.getCampaignOptions().getFallbackLabour();


        int roll = Compute.randomInt(totalOptionCount);

        if (roll < rollingOptionCount) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                    .replaceAll("0", resources.getString("graduatedWarrior.text")) + resources.getString("graduatedWarriorScience.text"));
            person.setEduCourseIndex(7);

            return true;
        } else {
            rollingOptionCount += campaign.getCampaignOptions().getFallbackScientist();
        }

        if (roll < rollingOptionCount) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                    .replaceAll("0", resources.getString("graduatedWarrior.text")) + resources.getString("graduatedWarriorMerchant.text"));
            person.setEduCourseIndex(8);

            return true;
        } else {
            rollingOptionCount += campaign.getCampaignOptions().getFallbackMerchant();
        }

        if (roll < rollingOptionCount) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                    .replaceAll("0", resources.getString("graduatedWarrior.text")) + resources.getString("graduatedWarriorTechnician.text"));
            person.setEduCourseIndex(9);

            return true;
        } else {
            rollingOptionCount += campaign.getCampaignOptions().getFallbackTechnician();
        }

        if (roll < rollingOptionCount) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                    .replaceAll("0", resources.getString("graduatedWarrior.text")) + resources.getString("graduatedWarriorLabour.text"));
            person.setEduCourseIndex(10);

            return true;
        }

        return false;
    }


    /**
     * Graduates a person from an academy.
     *
     * @param campaign the campaign the person belongs to
     * @param person the person being graduated
     * @param academy the academy from which the person is being graduated
     */
    private static void graduateAdult(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int graduationRoll = Compute.randomInt(100);
        int roll;
        // TODO link graduation to autoAwards (this can't be done till autoAwards have been merged)

        // qualification failed
        if (graduationRoll < 5) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedFailed.text"));
            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName());

            improveSkills(person, academy, false);

            return;
        }

        // class resits required
        if (graduationRoll < 20) {
            roll = Compute.d6(3);
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedClassNeeded.text")
                    .replace("0", String.valueOf(roll)));

            person.setEduDaysOfEducation(roll);

            return;
        }

        // this covers the code that is identical for both child and adult students
        commonGraduation(campaign, person, academy, resources, graduationRoll);
    }

    /**
     * Processes the types of graduation that are identical for both adult & child students.
     *
     * @param campaign the campaign where the graduation is taking place
     * @param person the person who is graduating
     * @param academy the academy where the person is studying
     * @param resources the resource bundle containing the strings for reporting and logging
     * @param graduationRoll the roll number for determining the graduation type
     */
    private static void commonGraduation(Campaign campaign, Person person, Academy academy, ResourceBundle resources, int graduationRoll) {
        // graduated top of the class
        if (graduationRoll == 99) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTop.text")
                    .replace("0", graduationEventPicker()));

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedTopLog.text"), person.getEduAcademyName());

            improveSkills(person, academy, true);

            if (campaign.getCampaignOptions().isEnableBonuses()) {
                addBonus(campaign, person, academy, 2, resources);
            }

            person.setEduHighestEducation(getEducationLevel(person, academy));

            return;
        }

        // graduated with honors
        if (graduationRoll >= 90) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedHonors.text")
                    .replace("0", graduationEventPicker()));

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedHonorsLog.text"), person.getEduAcademyName());

            improveSkills(person, academy, true);

            if (campaign.getCampaignOptions().isEnableBonuses()) {
                addBonus(campaign, person, academy, 1, resources);
            }

            person.setEduHighestEducation(getEducationLevel(person, academy));

            return;
        }

        // default graduation
        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduated.text")
                .replace("0", graduationEventPicker()));

        ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName());

        improveSkills(person, academy, true);

        person.setEduHighestEducation(getEducationLevel(person, academy));
    }

    /**
     * Graduates a child from a Prep School.
     *
     * @param campaign the campaign the person belongs to
     * @param person the person being graduated
     * @param academy the Prep School from which the person is being graduated
     */
    private static void graduateChild(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int graduationRoll = Compute.randomInt(100);
        // TODO link graduation to autoAwards (this can't be done till autoAwards have been merged)

        // qualification failed
        if (graduationRoll < 30) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedBarely.text"));
            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName());

            improveSkills(person, academy, false);

            return;
        }

        // graduated top of the class
        commonGraduation(campaign, person, academy, resources, graduationRoll);
    }

    /**
     * Graduates a child from a Sibko.
     *
     * @param campaign the campaign the person belongs to
     * @param person the person being graduated
     * @param academy the Prep School from which the person is being graduated
     */
    private static void graduateClan(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int graduationRoll = Compute.randomInt(100);
        // TODO link graduation to autoAwards (this can't be done till autoAwards have been merged)

        // Warrior Caste
        // [0]MechWarrior, [1]ProtoMech, [2]AreoSpace, [3]Space, [4]BA, [5]CI, [6]Vehicle
        if (person.getEduCourseIndex() <= 6) {
            // killed during The Blooding (only if life fire & all ages are enabled)
            // we probably don't need to also check All Ages here, but we do so as insurance.
            // we don't want child fatalities ever to occur (even as a bug) in a campaign that otherwise has them disabled
            if ((graduationRoll < 30) && (campaign.getCampaignOptions().isLiveFireBlooding()) && (campaign.getCampaignOptions().isAllAges())) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorKilled.text"));
                ServiceLogger.eduClanWarriorFailed(person, campaign.getLocalDate());
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.KIA);
                return;
            }

            // survived The Blooding, but failed to place.
            // process washout into fallback Caste.
            if (graduationRoll < 50) {
                ServiceLogger.eduClanWarriorFailed(person, campaign.getLocalDate());

                int totalOptionCount = campaign.getCampaignOptions().getFallbackLabour() + campaign.getCampaignOptions().getFallbackMerchant()
                        + campaign.getCampaignOptions().getFallbackScientist() + campaign.getCampaignOptions().getFallbackTechnician();
                int rollingOptionCount = campaign.getCampaignOptions().getFallbackLabour();

                int roll = Compute.randomInt(totalOptionCount);
                boolean proceed = true;

                if (roll < rollingOptionCount) {
                    campaign.addReport(person.getHyperlinkedName() + ' '
                            + resources.getString("graduatedWarriorFailed.text")
                            + resources.getString("graduatedWarriorScience.text"));
                    person.setEduCourseIndex(7);

                    proceed = false;
                } else {
                    rollingOptionCount += campaign.getCampaignOptions().getFallbackScientist();
                }

                if ((proceed) && (roll < rollingOptionCount)) {
                    campaign.addReport(person.getHyperlinkedName() + ' '
                            + resources.getString("graduatedWarriorFailed.text")
                            + resources.getString("graduatedWarriorMerchant.text"));
                    person.setEduCourseIndex(8);

                    proceed = false;
                } else {
                    rollingOptionCount += campaign.getCampaignOptions().getFallbackMerchant();
                }

                if ((proceed) && (roll < rollingOptionCount)) {
                    campaign.addReport(person.getHyperlinkedName() + ' '
                            + resources.getString("graduatedWarriorFailed.text")
                            + resources.getString("graduatedWarriorTechnician.text"));
                    person.setEduCourseIndex(9);

                    proceed = false;
                } else {
                    rollingOptionCount += campaign.getCampaignOptions().getFallbackTechnician();
                }

                if ((proceed) && (roll < rollingOptionCount)) {
                    campaign.addReport(person.getHyperlinkedName() + ' '
                            + resources.getString("graduatedWarriorFailed.text")
                            + resources.getString("graduatedWarriorLabour.text"));
                    person.setEduCourseIndex(10);

                    proceed = false;
                }

                if (proceed) {
                    LogManager.getLogger().error("Invalid caste fallback roll {}, graduation canceled", roll);
                    return;
                }
            }

            // one kill
            if ((graduationRoll >= 50) && (graduationRoll < 90)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorOneKill.text"));
                ServiceLogger.eduClanWarrior(person, campaign.getLocalDate(), resources.getString("graduatedWarriorOneKillLog.text"));

                improveSkills(person, academy, true);

                person.setEduHighestEducation(2);

                return;
            }

            // two kills
            if ((graduationRoll >= 90) && (graduationRoll < 99)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorTwoKills.text"));
                ServiceLogger.eduClanWarrior(person, campaign.getLocalDate(), resources.getString("graduatedWarriorTwoKillsLog.text"));

                improveSkills(person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 1, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }

            // three kills
            if (graduationRoll == 99) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorThreeKills.text"));
                ServiceLogger.eduClanWarrior(person, campaign.getLocalDate(), resources.getString("graduatedWarriorThreeKillsLog.text"));

                improveSkills(person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 2, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }
        }

        // [7]Scientist Caste
        graduationRoll = Compute.randomInt(100);
        if (person.getEduCourseIndex() == 7) {
            if (graduationRoll < 25) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialFailed.text")
                        .replaceAll("0", resources.getString("graduatedScience.text")));

                ServiceLogger.eduClanFailed(person, campaign.getLocalDate(), resources.getString("graduatedScience.text"));

                person.setEduCourseIndex(10);
            }

            if ((graduationRoll >= 25) && (graduationRoll < 90)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedClanBarely.text"))
                        .replaceAll("1", resources.getString("graduatedScience.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedClanBarely.text"),
                        resources.getString("graduatedScience.text"));

                improveSkills(person, academy, true);

                person.setEduHighestEducation(2);

                return;
            }

            if ((graduationRoll >= 90) && (graduationRoll < 99)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedEasily.text"))
                        .replaceAll("1", resources.getString("graduatedScience.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedEasily.text"),
                        resources.getString("graduatedScience.text"));

                improveSkills(person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 1, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }

            if (graduationRoll == 99) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedEffortlessly.text"))
                        .replaceAll("1", resources.getString("graduatedScience.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedEffortlessly.text"),
                        resources.getString("graduatedScience.text"));

                improveSkills(person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 2, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }
        }

        // [8]Merchant Caste
        graduationRoll = Compute.randomInt(100);
        if (person.getEduCourseIndex() == 8) {
            if (graduationRoll < 25) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialFailed.text")
                        .replaceAll("0", resources.getString("graduatedMerchant.text")));

                ServiceLogger.eduClanFailed(person, campaign.getLocalDate(), resources.getString("graduatedMerchant.text"));

                person.setEduCourseIndex(10);
            }

            if ((graduationRoll >= 25) && (graduationRoll < 90)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedClanBarely.text"))
                        .replaceAll("1", resources.getString("graduatedMerchant.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedClanBarely.text"),
                        resources.getString("graduatedMerchant.text"));

                improveSkills(person, academy, true);

                person.setEduHighestEducation(2);

                return;
            }

            if ((graduationRoll >= 90) && (graduationRoll < 99)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedEasily.text"))
                        .replaceAll("1", resources.getString("graduatedMerchant.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedEasily.text"),
                        resources.getString("graduatedMerchant.text"));

                improveSkills(person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 1, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }

            if (graduationRoll == 99) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedEffortlessly.text"))
                        .replaceAll("1", resources.getString("graduatedMerchant.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedEffortlessly.text"),
                        resources.getString("graduatedMerchant.text"));

                improveSkills(person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 2, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }
        }

        // [9]Technician Caste
        graduationRoll = Compute.randomInt(100);
        if (person.getEduCourseIndex() == 9) {
            if (graduationRoll < 25) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialFailed.text")
                        .replaceAll("0", resources.getString("graduatedTechnician.text")));

                ServiceLogger.eduClanFailed(person, campaign.getLocalDate(), resources.getString("graduatedTechnician.text"));

                person.setEduCourseIndex(10);
            }

            if ((graduationRoll >= 25) && (graduationRoll < 90)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedClanBarely.text"))
                        .replaceAll("1", resources.getString("graduatedTechnician.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedClanBarely.text"),
                        resources.getString("graduatedTechnician.text"));

                improveSkills(person, academy, true);

                person.setEduHighestEducation(2);

                return;
            }

            if ((graduationRoll >= 90) && (graduationRoll < 99)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedEasily.text"))
                        .replaceAll("1", resources.getString("graduatedTechnician.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedEasily.text"),
                        resources.getString("graduatedTechnician.text"));

                improveSkills(person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 1, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }

            if (graduationRoll == 99) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedEffortlessly.text"))
                        .replaceAll("1", resources.getString("graduatedTechnician.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedEffortlessly.text"),
                        resources.getString("graduatedTechnician.text"));

                improveSkills(person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 2, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }
        }

        // [10]Laborer Caste
        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedLabour.text"));
        ServiceLogger.eduClanLabour(person, campaign.getLocalDate());

        improveSkills(person, academy, true);

        person.setEduHighestEducation(2);
    }

    /**
     * Improves the skills of a given person based on the curriculum of the course they are attending.
     *
     * @param person The Person whose skills are being improved.
     * @param academy The academy the person is attending.
     * @param isGraduating A boolean value indicating whether the person is graduating from the academy or not.
     */
    private static void improveSkills(Person person, Academy academy, Boolean isGraduating) {
        String[] curriculum = academy.getCurriculums().get(person.getEduCourseIndex())
                .replaceAll(", ", ",").split(",");

        int education = academy.getBaseAcademicSkillLevel() + getEducationLevel(person, academy);

        if (!isGraduating) {
            education--;

            if (education < 0) {
                return;
            }
        }

        for (String skill : curriculum) {
            String skillParsed = Academy.skillParser(skill);
            int bonus;

            if ((person.hasSkill(skillParsed)) && (person.getSkillLevel(skillParsed) < education)) {
                bonus = person.getSkill(skillParsed).getBonus();
                person.addSkill(skillParsed, education, bonus);
            } else {
                person.addSkill(skillParsed, education, 0);
            }
        }
    }

    /**
     * Adds bonus to a number of skills based on the course curriculum.
     *
     * @param person The person to whom the bonus will be added.
     * @param academy The academy from which the person is studying.
     * @param bonusCount The number of bonuses to be added.
     * @param resources      The resource bundle used for obtaining localized string
     */
    private static void addBonus(Campaign campaign, Person person, Academy academy, int bonusCount, ResourceBundle resources) {
        List<String> curriculum = Arrays.asList(academy.getCurriculums().get(person.getEduCourseIndex())
                .replaceAll(", ", ",").split(","));

        // we remove the chance someone rolls the same skill multiple times, so they don't lose their bonus
        List<Integer> oldRolls = new ArrayList<>();

        for (int i = 0; i < bonusCount; i++) {
            int roll = Compute.randomInt(curriculum.size());

            if (!oldRolls.contains(roll)) {
                oldRolls.add(roll);
                String skillParsed = Academy.skillParser(curriculum.get(roll));

                // if the skill already has a bonus, they don't get to improve it further.
                // this is to discourage people from farming academia
                if (person.getSkill(skillParsed).getBonus() < 1) {
                    int skillLevel = person.getSkillLevel(skillParsed);
                    int bonus = person.getSkill(skillParsed).getBonus() + 1;

                    person.addSkill(skillParsed, skillLevel, bonus);

                    campaign.addReport(resources.getString("bonusAdded.text")
                            .replaceAll("0", person.getFirstName()));
                }
            } else {
                // this ensures the loop is redone
                bonusCount--;
            }
        }
    }

    /**
     * Picks a random graduation event from the graduation event table.
     *
     * @return a string representing the chosen graduation event
     */
    private static String graduationEventPicker() {
        int roll = Compute.d6();

        List<String> graduationEventTable = graduationEventTable();

        if (roll >= 5) {
            roll = Compute.randomInt(graduationEventTable.size());

            // We add 1 to ensure we cannot roll index 0 (uneventful.text)
            return graduationEventTable.get(roll + 1);
        } else {
            return graduationEventTable.get(0);
        }
    }

    /**
     * Returns a list of graduation event table entries. The list contains various graduation event table entries such as addresses, admirer
     * messages, events, gifts, pet costumes, recognitions, speeches, and surprises.
     *
     * @return a list of graduation event table entries
     */
    private static List<String> graduationEventTable() {
        return Arrays.asList(
                // this should always be the first entry
                "uneventful.text",
                // positive events
                "addressEncouragement.text",
                "addressFriendship.text",
                "addressFuturism.text",
                "addressGrowth.text",
                "addressHope.text",
                "addressHumorous.text",
                "addressLegacy.text",
                "addressResilience.text",
                "addressUncertain.text",
                "addressWisdom.text",
                "addressWisdomCelebrity.text",
                "addressWisdomElder.text",
                "addressWisdomFigure.text",
                "addressWisdomVeteran.text",
                "admirerArtwork.text",
                "admirerLetter.text",
                "admirerPoem.text",
                "admirerSpecial.text",
                "eventAlumni.text",
                "eventBalloons.text",
                "eventBells.text",
                "eventBillboard.text",
                "eventBonfire.text",
                "eventCandles.text",
                "eventCheerName.text",
                "eventCheerStage.text",
                "eventCoins.text",
                "eventCulturalPerformance.text",
                "eventDanceFlashMob.text",
                "eventDanceRoutine.text",
                "eventDanceTraditional.text",
                "eventDoves.text",
                "eventFireDance.text",
                "eventFireworks.text",
                "eventFoodTruck.text",
                "eventFundraiser.text",
                "eventGlitter.text",
                "eventHandshake.text",
                "eventKaraoke.text",
                "eventLetter.text",
                "eventLiveMusic.text",
                "eventMessage.text",
                "eventOutdoorGames.text",
                "eventPassingTorch.text",
                "eventProcession.text",
                "eventRoast.text",
                "eventSkywriting.text",
                "eventTimeCapsule.text",
                "eventVr.text",
                "giftAward.text",
                "giftCompass.text",
                "giftKeepsake.text",
                "giftLetter.text",
                "giftManual.text",
                "giftPersonalized.text",
                "giftPlantSapling.text",
                "giftPortrait.text",
                "giftRecommendation.text",
                "giftScrapbook.text",
                "giftSpecial.text",
                "giftSymbolic.text",
                "giftTravelVoucher.text",
                "giftWristwatch.text",
                "petCostume.text",
                "petGeneral.text",
                "petGown",
                "recognitionAcademic.text",
                "recognitionContributions.text",
                "recognitionCreativity.text",
                "recognitionDedication.text",
                "recognitionExtracurricular.text",
                "recognitionInnovation.text",
                "recognitionLeadership.text",
                "recognitionPublicOrder.text",
                "recognitionTalents.text",
                "speechAchievement.text",
                "speechCommunityInvolvement.text",
                "speechEmotional.text",
                "speechHumility.text",
                "speechHumorous.text",
                "speechInspiration.text",
                "speechMechCommander.text",
                "speechMotivation.text",
                "speechPromotion.text",
                "speechReflection.text",
                "speechTriumph.text",
                "surpriseArtist.text",
                "surpriseAwardsCeremony.text",
                "surpriseBand.text",
                "surpriseBandLocal.text",
                "surpriseChoir.text",
                "surpriseCommander.text",
                "surpriseDanceBattle.text",
                "surpriseMascot.text",
                "surpriseMotivationalSpeaker.text",
                "surprisePoet.text",
                "surpriseScholarship.text",
                "surpriseScholarshipAward.text",
                "surpriseSpeaker.text",
                "surpriseStandard.text",
                // negative events
                "eventAccident.text",
                "eventAirRaid.text",
                "eventAngryParent.text",
                "eventArrest.text",
                "eventAudioIssues.text",
                "eventBagCheckProcedure.text",
                "eventBooing.text",
                "eventCancellations.text",
                "eventCatererDelay.text",
                "eventCellPhoneRing.text",
                "eventChaos.text",
                "eventComplaints.text",
                "eventConflict.text",
                "eventCrowdedRestrooms.text",
                "eventDelayedStart.text",
                "eventDisappointment.text",
                "eventDisorganization.text",
                "eventElevatorMalfunction.text",
                "eventEmbarrassment.text",
                "eventEquipmentAdjustment.text",
                "eventEquipmentFailure.text",
                "eventEvacuation.text",
                "eventFailure.text",
                "eventFainting.text",
                "eventFighting.text",
                "eventFireAlarm.text",
                "eventFoodPoisoning.text",
                "eventFoodShortage.text",
                "eventForgottenSpeech.text",
                "eventGuestLateArrival.text",
                "eventGuestSpeakerCue.text",
                "eventHealthEmergency.text",
                "eventInadequateSeating.text",
                "eventInjury.text",
                "eventLackluster.text",
                "eventLateProgramDistribution.text",
                "eventLateSpeaker.text",
                "eventLateVendorSetup.text",
                "eventLeak.text",
                "eventLegalIssue.text",
                "eventLightingAdjustment.text",
                "eventLogisticalNightmare.text",
                "eventLostAndFound.text",
                "eventLostDirections.text",
                "eventMenuChange.text",
                "eventMiscommunication.text",
                "eventMissingEquipment.text",
                "eventNoShow.text",
                "eventOutburst.text",
                "eventOvercrowding.text",
                "eventOverflowParking.text",
                "eventOversight.text",
                "eventParkingDirection.text",
                "eventParkingIssues.text",
                "eventPhotographerDelay.text",
                "eventPhotographerObstruction.text",
                "eventPoorAttendance.text",
                "eventPowerOutage.text",
                "eventPowerStruggle.text",
                "eventPresentationGlitch.text",
                "eventProgramMisprint.text",
                "eventProtest.text",
                "eventPublicAddress.text",
                "eventPublicTransportDelay.text",
                "eventRain.text",
                "eventScheduleAdjustment.text",
                "eventSecurityBreach.text",
                "eventSecurityCheckDelay.text",
                "eventSecurityConcern.text",
                "eventSmokingViolation.text",
                "eventSpeakerIntroductionDelay.text",
                "eventSpeakerPreparation.text",
                "eventStagingError.text",
                "eventTechnicalAdjustments.text",
                "eventTechnicalFailure.text",
                "eventTechnicalGlitch.text",
                "eventTechnicalSupport.text",
                "eventTemperatureAdjustment.text",
                "eventTheft.text",
                "eventTicketMixup.text",
                "eventTicketScanningDelay.text",
                "eventTraffic.text",
                "eventTrafficControl.text",
                "eventTrafficJam.text",
                "eventTransportationIssue.text",
                "eventTransportationLogistics.text",
                "eventUnexpectedBreak.text",
                "eventUnexpectedGuest.text",
                "eventUnexpectedSpeaker.text",
                "eventUnplanned.text",
                "eventUnprepared.text",
                "eventUpset.text",
                "eventVandalism.text",
                "eventVenueChange.text",
                "eventVenueCleanup.text",
                "eventVenueNavigation.text",
                "eventVenueNoise.text",
                "eventVIPArrival.text",
                "eventVolunteerShortage.text",
                "eventWardrobeMalfunction.text",
                "eventWeatherDisruption.text",
                "eventWeatherForecastError.text",
                "speechApology.text",
                "speechBlunder.text",
                "speechControversial.text",
                "speechDisrespectful.text",
                "speechInsensitive.text",
                "speechMispronunciation.text",
                "speechRant.text",
                "speechRejection.text",
                "speechUninspiring.text",
                "surpriseArgument.text",
                "surpriseCancellation.text",
                "surpriseDisappointment.text",
                "surpriseEmbarrassment.text",
                "surpriseFailure.text",
                "surpriseInjury.text",
                "surpriseLaser.text",
                "surpriseMishap.text",
                "surpriseMisunderstanding.text",
                "surpriseRejection.text");
    }
}
