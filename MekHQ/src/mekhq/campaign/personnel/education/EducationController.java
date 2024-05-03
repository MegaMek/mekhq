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
import java.time.LocalDate;
import java.util.*;

/**
 * The EducationController class is responsible for managing the education process.
 * It provides methods to begin the education process, calculate education level, and enroll a person into an academy.
 */
public class EducationController {
    /**
     * Begins the education process for a Person in a Campaign.
     *
     * @param campaign The Campaign in which the education process is taking place.
     * @param person The Person who is enrolling for education.
     * @param academySet The set name of the academy.
     * @param academyNameInSet The name of the academy within the set.
     * @param courseIndex The index of the course in the academy.
     * @param campus The campus location (can be null if the academy is local).
     * @param faction The faction of the person.
     */
    public static void beginEducation(Campaign campaign, Person person, String academySet, String academyNameInSet, int courseIndex, @Nullable String campus, String faction) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        Academy academy = getAcademy(academySet, academyNameInSet);

        if (academy == null) {
            LogManager.getLogger().error("No academy found with name {} in set {}", academyNameInSet, academySet);
            return;
        }

        double tuition = academy.getTuitionAdjusted(person) * academy.getFactionDiscountAdjusted(campaign, person);
        LogManager.getLogger().info(tuition);
        LogManager.getLogger().info(campaign.getFinances().getBalance());

        // check there is enough money in the campaign & if so, make a debit
        if (campaign.getFinances().getBalance().isLessThan(Money.of(tuition))) {
            campaign.addReport(resources.getString("insufficientFunds.text").replaceAll("0", person.getHyperlinkedFullTitle()));
            return;
        } else {
            campaign.getFinances().debit(TransactionType.EDUCATION, campaign.getLocalDate(), Money.of(tuition), resources.getString("payment.text").replaceAll("0", person.getFullName()));
        }

        // with the checks done, and tuition paid, we can enroll Person
        person.setEduCourseIndex(courseIndex);

        enrollPerson(campaign, person, academy, campus, faction, courseIndex);

        campaign.addReport(resources.getString("offToSchool.text").replaceAll("0", person.getFullName()).replaceAll("1", person.getEduAcademyName()).replaceAll("2", String.valueOf(person.getEduDaysOfTravelToAcademy())));
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
     * @param courseIndex The index of the course being taken
     *                 This parameter can be null if the academy is not a Clan Sibko.
     */
    public static void enrollPerson(Campaign campaign, Person person, Academy academy, @Nullable String campus, String faction, @Nullable Integer courseIndex) {
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
            person.setEduDaysOfTravelToAcademy(campaign.getSimplifiedTravelTime(campaign.getSystemByName(campus)));
            person.setEduAcademySystem(campaign.getSystemByName(campus).getName(campaign.getLocalDate()));
        }

        person.setEduAcademyNameInSet(academy.getName());

        // if the academy is Local or Clan, we need to generate a name, otherwise we use the listed name
        if ((academy.isLocal()) || (academy.isClan())) {
            person.setEduAcademyName(generateName(campaign, person, academy, courseIndex, campaign.getCurrentSystem().getName(campaign.getLocalDate())));
        } else {
            person.setEduAcademyName(person.getEduAcademyNameInSet() + " (" + campus + ')');
        }

        // we have this all the way at the bottom as a bit of insurance.
        // when troubleshooting, if the log isn't getting entered, we know something went wrong when enrolling.
        ServiceLogger.eduEnrolled(person, campaign.getLocalDate(), person.getEduAcademyName());
    }

    /**
     * Generates a name for a local academy.
     *
     * @param person      The person for whom the name is being generated (nullable, if not Clan Sibko).
     * @param academy     The academy to which the person is applying.
     * @param courseIndex The index of the course (nullable, if not Clan Sibko).
     * @param campus      The campus of the academy.
     * @return The generated name.
     */
    public static String generateName(@Nullable Campaign campaign, @Nullable Person person, Academy academy, @Nullable Integer courseIndex, String campus) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        if (academy.isClan()) {
            if (academy.isLocal()) {
                return generateSibkoCode(campaign, person, courseIndex, resources) + " (REDACTED)";
            } else {
                return generateSibkoCode(campaign, person, courseIndex, resources) + " (" + campus + ')';
            }
        } else {
            if (Compute.d6(1) <= 3) {
                if (academy.isMilitary()) {
                    return campus + ' ' + generateMilitaryPrefix(resources) + ' ' + generateTypeAdult(resources) + ' ' + resources.getString("conjoinerOf.text") + generateSuffix(resources);
                } else {
                    return campus + ' ' + generateTypeAdult(resources) + ' ' + resources.getString("conjoinerOf.text") + generateSuffix(resources);
                }
            } else {
                if (academy.isMilitary()) {
                    return generateMilitaryPrefix(resources) + ' ' + generateTypeAdult(resources) + ' ' + resources.getString("conjoinerOf.text") + campus;
                } else {
                    return generateTypeAdult(resources) + ' ' + resources.getString("conjoinerOf.text") + campus;
                }
            }
        }
    }

    /**
     * Generates a Sibko code based on the given campaign, person, course index, and resources.
     *
     * @param campaign The current campaign.
     * @param person The individual.
     * @param courseIndex The index of the course the person is enrolled in.
     * @param resources The resource bundle containing the caste text values.
     * @return The generated Sibko code.
     * @throws IllegalStateException if the course index is unexpected.
     */
    private static String generateSibkoCode(Campaign campaign, Person person, Integer courseIndex, ResourceBundle resources) {
        LocalDate birthDate = person.getRecruitment();
        String caste;

        switch (courseIndex) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                caste = resources.getString("graduatedWarrior.text");
                break;
            case 7:
                caste = resources.getString("graduatedScientist.text");
                break;
            case 8:
                caste = resources.getString("graduatedMerchant.text");
                break;
            case 9:
                caste = resources.getString("graduatedTechnician.text");
                break;
            case 10:
                caste = resources.getString("graduatedLabour.text");
                break;
            default:
                throw new IllegalStateException("Unexpected caste value in generateName: " + courseIndex);
        }

        return (caste + ' ' + birthDate.getYear() + birthDate.getMonth().getValue() + birthDate.getDayOfMonth() + campaign.getFaction().getShortName());
    }

    /**
     * Generates a military academy prefix randomly selected from the resource bundle.
     *
     * @param resources the resource bundle containing the military prefix texts
     * @return a randomly generated military prefix
     * @throws IllegalStateException if an unexpected roll occurs
     */
    private static String generateMilitaryPrefix(ResourceBundle resources) {
        switch (Compute.d6(1)) {
            case 1:
                return resources.getString("prefixCombinedArms.text");
            case 2:
                return resources.getString("prefixCombinedForces.text");
            case 3:
                return resources.getString("prefixMilitary.text");
            case 4:
                return resources.getString("prefixWar.text");
            case 5:
                return resources.getString("prefixWarFighting.text");
            case 6:
                return resources.getString("prefixCombat.text");
            default:
                throw new IllegalStateException("Unexpected roll in generateMilitaryPrefix");
        }
    }

    /**
     * This method generates a academy suffix based on a random roll.
     *
     * @param resources The ResourceBundle containing the suffix texts.
     * @return The generated suffix.
     * @throws IllegalStateException if the random roll is unexpected.
     */
    private static String generateSuffix(ResourceBundle resources) {
        switch (Compute.d6(1)) {
            case 1:
                return resources.getString("suffixTechnology.text");
            case 2:
                return resources.getString("suffixTechnologyAdvanced.text");
            case 3:
                return resources.getString("suffixScience.text");
            case 4:
                return resources.getString("suffixScienceAdvanced.text");
            case 5:
                return resources.getString("suffixStudies.text");
            case 6:
                return resources.getString("suffixHigherLearning.text");
            default:
                throw new IllegalStateException("Unexpected roll in generateSuffix()");
        }
    }

    /**
     * Generates a random educational institution type from the provided ResourceBundle.
     *
     * @param resources the ResourceBundle containing the localized strings for the educational institution types
     * @return a randomly selected educational institution type
     * @throws IllegalStateException if the generated roll is unexpected
     */
    private static String generateTypeAdult(ResourceBundle resources) {
        switch (Compute.d6(1)) {
            case 1:
                return resources.getString("typeAcademy.text");
            case 2:
                return resources.getString("typeCollege.text");
            case 3:
                return resources.getString("typeInstitute.text");
            case 4:
                return resources.getString("typeUniversity.text");
            case 5:
                return resources.getString("typePolytechnic.text");
            case 6:
                return resources.getString("typeSchool.text");
            default:
                throw new IllegalStateException("Unexpected roll in generateTypeAdult");
        }
    }

    /**
     * Process a new day in the campaign, updating the education status of each student.
     *
     * @param campaign the campaign object containing the students
     */
    public static void processNewDay(Campaign campaign) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        for (Person person : campaign.getStudents()) {
            Academy academy = getAcademy(person.getEduAcademySet(), person.getEduAcademyNameInSet());

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
            Integer daysOfTravelFrom = beginJourneyHome(campaign, person, academy, daysOfEducation, resources);

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
    private static Integer beginJourneyHome(Campaign campaign, Person person, Academy academy, Integer daysOfEducation, ResourceBundle resources) {
        int daysOfTravelFrom = person.getEduDaysOfTravelFromAcademy();

        int travelTime;

        if ((daysOfEducation == 0) && (daysOfTravelFrom == 0)) {
            if ((academy.isClan()) && (academy.isLocal())) {
                try {
                    travelTime = campaign.getSimplifiedTravelTime(campaign.getFaction().getStartingPlanet(campaign, campaign.getLocalDate()));
                } catch (Exception e) {
                    // not all Clans have a starting planet, so we abstract it
                    travelTime = 100;
                }
            } else {
                travelTime = campaign.getSimplifiedTravelTime(campaign.getSystemByName(person.getEduAcademySystem()));
            }

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
            int travelTime = campaign.getSimplifiedTravelTime(campaign.getSystemByName(person.getEduAcademySystem()));

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
     * Retrieves an Academy object based on the provided parameters.
     *
     * @param academySetName The academy set name to match.
     * @param academyNameInSet The academy name in the set to match.
     * @return The Academy that matches the provided parameters or null if no match is found.
     */
    private static Academy getAcademy(String academySetName, String academyNameInSet) {
        List<String> setNames = AcademyFactory.getInstance().getAllSetNames();

        return setNames.stream().filter(setName -> setName.equalsIgnoreCase(academySetName))
                .map(setName -> AcademyFactory.getInstance().getAllAcademiesForSet(setName))
                .flatMap(Collection::stream).filter(academy -> String.valueOf(academyNameInSet)
                        .equals(academyNameInSet)).findFirst().orElse(null);
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
                                person.setEduAcademyName(generateSibkoCode(campaign, person, 10, resources));

                                break;
                            case 8:
                                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedMerchant.text"));

                                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                                        .replaceAll("0", resources.getString("graduatedMerchant.text")) + resources.getString("graduatedWarriorLabour.text"));
                                person.setEduCourseIndex(10);
                                person.setEduAcademyName(generateSibkoCode(campaign, person, 10, resources));

                                break;
                            case 9:
                                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedTechnician.text"));

                                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                                        .replaceAll("0", resources.getString("graduatedTechnician.text")) + resources.getString("graduatedWarriorLabour.text"));
                                person.setEduCourseIndex(10);
                                person.setEduAcademyName(generateSibkoCode(campaign, person, 10, resources));

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
            person.setEduAcademyName(generateSibkoCode(campaign, person, 7, resources));

            return true;
        } else {
            rollingOptionCount += campaign.getCampaignOptions().getFallbackScientist();
        }

        if (roll < rollingOptionCount) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                    .replaceAll("0", resources.getString("graduatedWarrior.text")) + resources.getString("graduatedWarriorMerchant.text"));
            person.setEduCourseIndex(8);
            person.setEduAcademyName(generateSibkoCode(campaign, person, 8, resources));

            return true;
        } else {
            rollingOptionCount += campaign.getCampaignOptions().getFallbackMerchant();
        }

        if (roll < rollingOptionCount) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                    .replaceAll("0", resources.getString("graduatedWarrior.text")) + resources.getString("graduatedWarriorTechnician.text"));
            person.setEduCourseIndex(9);
            person.setEduAcademyName(generateSibkoCode(campaign, person, 9, resources));

            return true;
        } else {
            rollingOptionCount += campaign.getCampaignOptions().getFallbackTechnician();
        }

        if (roll < rollingOptionCount) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("washout.text")
                    .replaceAll("0", resources.getString("graduatedWarrior.text")) + resources.getString("graduatedWarriorLabour.text"));
            person.setEduCourseIndex(10);
            person.setEduAcademyName(generateSibkoCode(campaign, person, 10, resources));

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

            improveSkills(campaign, person, academy, false);

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

        if (graduationRoll == 99) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTop.text")
                    .replace("0", graduationEventPicker()));

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedTopLog.text"), person.getEduAcademyName());

            improveSkills(campaign, person, academy, true);

            if (campaign.getCampaignOptions().isEnableBonuses()) {
                addBonus(campaign, person, academy, 2, resources);
            }

            int educationLevel = academy.getEducationLevel(person);
            person.setEduHighestEducation(educationLevel);

            if (educationLevel == 3) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedMasters.text"));

                ServiceLogger.eduGraduatedMasters(person, campaign.getLocalDate(), person.getEduAcademyName());

            } else if (educationLevel == 4) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedDoctorate.text"));

                ServiceLogger.eduGraduatedDoctorate(person, campaign.getLocalDate(), person.getEduAcademyName());

                person.setPreNominal("Dr");
            }

            return;
        }

        // graduated with honors
        if (graduationRoll >= 90) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedHonors.text")
                    .replace("0", graduationEventPicker()));

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedHonorsLog.text"), person.getEduAcademyName());

            improveSkills(campaign, person, academy, true);

            if (campaign.getCampaignOptions().isEnableBonuses()) {
                addBonus(campaign, person, academy, 1, resources);
            }

            person.setEduHighestEducation(academy.getEducationLevel(person));

            return;
        }

        // default graduation
        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduated.text")
                .replace("0", graduationEventPicker()));

        ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName());

        improveSkills(campaign, person, academy, true);

        person.setEduHighestEducation(academy.getEducationLevel(person));
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

            improveSkills(campaign, person, academy, false);

            return;
        }

        // default graduation
        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduated.text")
                .replace("0", graduationEventPicker()));

        ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName());

        improveSkills(campaign, person, academy, true);

        person.setEduHighestEducation(academy.getEducationLevel(person));
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
                    person.setEduAcademyName(generateSibkoCode(campaign, person, 7, resources));

                    proceed = false;
                } else {
                    rollingOptionCount += campaign.getCampaignOptions().getFallbackScientist();
                }

                if ((proceed) && (roll < rollingOptionCount)) {
                    campaign.addReport(person.getHyperlinkedName() + ' '
                            + resources.getString("graduatedWarriorFailed.text")
                            + resources.getString("graduatedWarriorMerchant.text"));
                    person.setEduCourseIndex(8);
                    person.setEduAcademyName(generateSibkoCode(campaign, person, 8, resources));

                    proceed = false;
                } else {
                    rollingOptionCount += campaign.getCampaignOptions().getFallbackMerchant();
                }

                if ((proceed) && (roll < rollingOptionCount)) {
                    campaign.addReport(person.getHyperlinkedName() + ' '
                            + resources.getString("graduatedWarriorFailed.text")
                            + resources.getString("graduatedWarriorTechnician.text"));
                    person.setEduCourseIndex(9);
                    person.setEduAcademyName(generateSibkoCode(campaign, person, 9, resources));

                    proceed = false;
                } else {
                    rollingOptionCount += campaign.getCampaignOptions().getFallbackTechnician();
                }

                if ((proceed) && (roll < rollingOptionCount)) {
                    campaign.addReport(person.getHyperlinkedName() + ' '
                            + resources.getString("graduatedWarriorFailed.text")
                            + resources.getString("graduatedWarriorLabour.text"));
                    person.setEduCourseIndex(10);
                    person.setEduAcademyName(generateSibkoCode(campaign, person, 10, resources));

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

                improveSkills(campaign, person, academy, true);

                person.setEduHighestEducation(2);

                return;
            }

            // two kills
            if ((graduationRoll >= 90) && (graduationRoll < 99)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorTwoKills.text"));
                ServiceLogger.eduClanWarrior(person, campaign.getLocalDate(), resources.getString("graduatedWarriorTwoKillsLog.text"));

                improveSkills(campaign, person, academy, true);

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

                improveSkills(campaign, person, academy, true);

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
                        .replaceAll("0", resources.getString("graduatedScientist.text")));

                ServiceLogger.eduClanFailed(person, campaign.getLocalDate(), resources.getString("graduatedScientist.text"));

                person.setEduCourseIndex(10);
            }

            if ((graduationRoll >= 25) && (graduationRoll < 90)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedClanBarely.text"))
                        .replaceAll("1", resources.getString("graduatedScientist.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedClanBarely.text"),
                        resources.getString("graduatedScientist.text"));

                improveSkills(campaign, person, academy, true);

                person.setEduHighestEducation(2);

                return;
            }

            if ((graduationRoll >= 90) && (graduationRoll < 99)) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedEasily.text"))
                        .replaceAll("1", resources.getString("graduatedScientist.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedEasily.text"),
                        resources.getString("graduatedScientist.text"));

                improveSkills(campaign, person, academy, true);

                if (campaign.getCampaignOptions().isEnableBonuses()) {
                    addBonus(campaign, person, academy, 1, resources);
                }

                person.setEduHighestEducation(2);

                return;
            }

            if (graduationRoll == 99) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text")
                        .replaceAll("0", resources.getString("graduatedEffortlessly.text"))
                        .replaceAll("1", resources.getString("graduatedScientist.text")));

                ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                        resources.getString("graduatedEffortlessly.text"),
                        resources.getString("graduatedScientist.text"));

                improveSkills(campaign, person, academy, true);

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

                improveSkills(campaign, person, academy, true);

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

                improveSkills(campaign, person, academy, true);

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

                improveSkills(campaign, person, academy, true);

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

                improveSkills(campaign, person, academy, true);

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

                improveSkills(campaign, person, academy, true);

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

                improveSkills(campaign, person, academy, true);

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

        improveSkills(campaign, person, academy, true);

        person.setEduHighestEducation(2);
    }

    /**
     * Improves the skills of a given person based on the curriculum of the course they are attending.
     *
     * @param person The Person whose skills are being improved.
     * @param academy The academy the person is attending.
     * @param isGraduating A boolean value indicating whether the person is graduating from the academy or not.
     */
    private static void improveSkills(Campaign campaign, Person person, Academy academy, Boolean isGraduating) {
        String[] curriculum = academy.getCurriculums().get(person.getEduCourseIndex())
                .replaceAll(", ", ",").split(",");

        int educationLevel = academy.getBaseAcademicSkillLevel() + academy.getEducationLevel(person);

        if (!isGraduating) {
            educationLevel--;

            if (educationLevel < 0) {
                return;
            }
        }

        for (String skill : curriculum) {
            if (skill.equalsIgnoreCase("bonus xp")) {
                person.awardXP(campaign, Compute.d6(educationLevel));
            } else {
                String skillParsed = Academy.skillParser(skill);
                int bonus;

                if ((person.hasSkill(skillParsed)) && (person.getSkillLevel(skillParsed) < educationLevel)) {
                    bonus = person.getSkill(skillParsed).getBonus();
                    person.addSkill(skillParsed, educationLevel, bonus);
                } else {
                    person.addSkill(skillParsed, educationLevel, 0);
                }
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

        for (int i = 0; i < bonusCount; i++) {
            int roll = Compute.randomInt(curriculum.size() - 1);

            try {
                String skillParsed = Academy.skillParser(curriculum.get(roll));

                // if 'person' already has a +1 bonus for the skill, we give them XP, instead
                if (person.getSkill(skillParsed).getBonus() < 1) {
                    int skillLevel = person.getSkillLevel(skillParsed);
                    int bonus = person.getSkill(skillParsed).getBonus() + 1;

                    person.addSkill(skillParsed, skillLevel, bonus);

                    campaign.addReport(resources.getString("bonusAdded.text")
                            .replaceAll("0", person.getFirstName()));
                } else {
                    person.awardXP(campaign, Compute.d6(2));
                }
            } catch (Exception e) {
                // if we get this, it means the 'skill' was Bonus XP
                person.awardXP(campaign, Compute.d6(2));
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
