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
import mekhq.campaign.universe.PlanetarySystem;
import org.apache.logging.log4j.LogManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    public static void beginEducation(Campaign campaign, Person person, String academySet, String academyNameInSet, int courseIndex, String campus, String faction) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        Academy academy = getAcademy(academySet, academyNameInSet);

        if (academy == null) {
            LogManager.getLogger().error("No academy found with name {} in set {}", academyNameInSet, academySet);
            return;
        }

        double tuition = academy.getTuitionAdjusted(person) * academy.getFactionDiscountAdjusted(campaign, person);

        if (tuition > 0) {
            // check there is enough money in the campaign & if so, make a debit
            if (campaign.getFinances().getBalance().isLessThan(Money.of(tuition))) {
                campaign.addReport(String.format(resources.getString("insufficientFunds.text"),
                        person.getHyperlinkedFullTitle()));
                return;
            } else {
                campaign.getFinances().debit(TransactionType.EDUCATION,
                        campaign.getLocalDate(),
                        Money.of(tuition),
                        String.format(resources.getString("payment.text"),
                                person.getFullName()));
            }
        }

        // with the checks done, and tuition paid, we can enroll Person
        person.setEduCourseIndex(courseIndex);

        enrollPerson(campaign, person, academy, campus, faction, courseIndex);

        if (academy.isClan()) {
            campaign.addReport(String.format(resources.getString("offToSchoolClan.text"),
                    person.getFullName(),
                    person.getEduAcademyName(),
                    person.getEduDaysOfTravelToAcademy()));
        } else {
            campaign.addReport(String.format(resources.getString("offToSchool.text"),
                    person.getFullName(),
                    person.getEduAcademyName(),
                    person.getEduDaysOfTravelToAcademy()));
        }
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
    public static void enrollPerson(Campaign campaign, Person person, Academy academy, String campus, String faction, Integer courseIndex) {
        // change status will wipe the academic information, so must always precede the setters
        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.STUDENT);

        person.setEduEducationStage(1);
        person.setEduAcademySet(academy.getSet());
        person.setEduAcademyNameInSet(academy.getName());
        person.setEduDaysOfEducation(academy.getDurationDays());
        person.setEduAcademyFaction(faction);
        person.setEduCourseIndex(courseIndex);

        if (academy.isLocal()) {
            person.setEduDaysOfTravelToAcademy(2);
            person.setEduAcademySystem(campaign.getCurrentSystem().getId());
        } else if (academy.isTrueborn()) {
            person.setEduDaysOfTravelToAcademy(2);

            person.setEduAcademySystem(campaign.getSystemByName(campus).getId());
        } else {
            person.setEduDaysOfTravelToAcademy(campaign.getSimplifiedTravelTime(campaign.getSystemById(campus)));
            person.setEduAcademySystem(campaign.getSystemById(campus).getName(campaign.getLocalDate()));
        }

        person.setEduAcademyNameInSet(academy.getName());

        // if the academy is Local or Clan, we need to generate a name, otherwise we use the listed name
        if ((academy.isLocal()) || (academy.isClan())) {
            person.setEduAcademyName(generateName(campaign, person, academy, courseIndex, campus));
        } else {
            person.setEduAcademyName(person.getEduAcademyNameInSet() + " (" + campus + ')');
        }

        // we have this all the way at the bottom as a bit of insurance.
        // when troubleshooting, if the log isn't getting entered, we know something went wrong when enrolling.
        ServiceLogger.eduEnrolled(person, campaign.getLocalDate(), person.getEduAcademyName());
    }

    /**
     * Generates a name for a local academy or clan education facility.
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
            return generateClanEducationCode(campaign, person, courseIndex, resources) + campus + ')';
        } else {
            if (Compute.d6(1) <= 3) {
                if (academy.isMilitary()) {
                    return campus + ' ' + generateMilitaryPrefix(resources) + ' ' + generateTypeAdult(resources) + ' ' + resources.getString("conjoinerOf.text") + ' ' + generateSuffix(resources);
                } else {
                    return campus + ' ' + generateTypeAdult(resources) + ' ' + resources.getString("conjoinerOf.text") + ' ' + generateSuffix(resources);
                }
            } else {
                if (academy.isMilitary()) {
                    return generateMilitaryPrefix(resources) + ' ' + generateTypeAdult(resources) + ' ' + resources.getString("conjoinerOf.text") + ' ' + campus;
                } else {
                    return generateTypeAdult(resources) + ' ' + resources.getString("conjoinerOf.text") + ' ' + campus;
                }
            }
        }
    }

    /**
     * Generates a Clan Education code based on the given campaign, person, and course index.
     *
     * @param campaign The current campaign.
     * @param person The individual.
     * @param courseIndex The index of the course the person is enrolled in.
     * @param resources The resource bundle containing the caste text values.
     * @return The generated Clan Education code.
     * @throws IllegalStateException if the course index is unexpected.
     */
    private static String generateClanEducationCode(Campaign campaign, Person person, Integer courseIndex, ResourceBundle resources) {
        LocalDate birthDate = person.getBirthday();
        String caste = "";

        if (person.getAge(campaign.getLocalDate()) >= 10) {
            switch (courseIndex) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    caste = resources.getString(getWarriorCasteSubCasteString(person.getEduCourseIndex()));
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
                    caste = resources.getString("graduatedLabor.text");
                    break;
                default:
                    throw new IllegalStateException("Unexpected caste value in generateName: " + courseIndex);
            }
        }

        if (person.getAge(campaign.getLocalDate()) < 10) {
            return "Creche " + birthDate.getYear() + birthDate.getMonth().getValue() + birthDate.getDayOfMonth() + campaign.getFaction().getShortName() + " (";
        } else if (person.getAge(campaign.getLocalDate()) < 20) {
            return (caste + ' ' + birthDate.getYear() + birthDate.getMonth().getValue() + birthDate.getDayOfMonth() + campaign.getFaction().getShortName()) + " (";
        } else {
            byte campHash = 0;

            for (char character : "camp_hash".toCharArray()) {
                campHash += (byte) character;
            }

            return "Education Camp " + campHash + "( ";
        }
    }

    /**
     * Returns the sub-caste string corresponding to the given course index.
     *
     * @param courseIndex the index of the course
     * @return the sub-caste string
     */
    private static String getWarriorCasteSubCasteString(int courseIndex) {
        String originCaste;

        switch (courseIndex) {
            case 0:
                originCaste = "graduatedWarriorMechWarrior.text";
                break;
            case 1:
                originCaste = "graduatedWarriorProtoMech.text";
                break;
            case 2:
                originCaste = "graduatedWarriorAerospace.text";
                break;
            case 3:
                originCaste = "graduatedWarriorSpace.text";
                break;
            case 4:
                originCaste = "graduatedWarriorBa.text";
                break;
            case 5:
                originCaste = "graduatedWarriorInfantry.text";
                break;
            case 6:
                originCaste = "graduatedWarriorVehicle.text";
                break;
            default:
                originCaste = "graduatedWarrior.text";
                break;
        }
        return originCaste;
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
     * This method generates an academy suffix based on a random roll.
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
     * Processes a new day for a person in a campaign.
     *
     * @param campaign   the campaign in which the person is participating
     * @param person     the person for whom the new day is being processed
     * @param ageBypass  a flag indicating whether graduation age restrictions should be bypassed
     * @return true if the new day was successfully processed, false otherwise
     */
    public static boolean processNewDay(Campaign campaign, Person person, boolean ageBypass) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());
        Academy academy = getAcademy(person.getEduAcademySet(), person.getEduAcademyNameInSet());

        int educationStage = person.getEduEducationStage();

        // is person in transit to the institution?
        if (educationStage == 1) {
            journeyToAcademy(campaign, person, resources);
            return false;
        }

        // is person on campus and undergoing education, or awaiting assignment to a sibko?
        if ((educationStage == 2) || (educationStage == 3)) {
            return ongoingEducation(campaign, person, academy, ageBypass, resources);
        }

        // if education has concluded and the journey home hasn't started, we begin the journey
        if (educationStage == 4) {
            beginJourneyHome(campaign, person, academy, resources);
            return false;
        }

        // if we reach this point it means Person is already in transit, so we continue their journey
        if (educationStage == 5) {
            processJourneyHome(campaign, person);
            return false;
        }

        LogManager.getLogger().error("Unexpected education stage: {}", educationStage);
        return false;
    }

    /**
     * Processes a person's journey to campus.
     *
     * @param campaign  The campaign the person is part of.
     * @param person    The person for whom the journey is being processed.
     * @param resources The resource bundle containing localized strings.
     */
    private static void journeyToAcademy(Campaign campaign, Person person, ResourceBundle resources) {
        int daysOfTravelTo = person.getEduDaysOfTravelToAcademy();

        person.setEduDaysOfTravelToAcademy(daysOfTravelTo - 1);

        // has Person just arrived?
        if ((daysOfTravelTo - 1) < 1) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("arrived.text"));
            person.setEduEducationStage(2);
        }
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
    private static boolean ongoingEducation(Campaign campaign, Person person, Academy academy, boolean ageBypass, ResourceBundle resources) {
        int daysOfEducation = person.getEduDaysOfEducation();

        if (academy.isPrepSchool()) {
            if ((person.getAge(campaign.getLocalDate()) >= academy.getAgeMax()) || (ageBypass)) {
                if (person.getEduEducationStage() == 2) {
                    graduationPicker(campaign, person, academy, resources);

                    person.setEduDaysOfEducation(0);
                    person.setEduEducationStage(3);
                } else {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedCreche.text"));
                }

                if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                    processNewWeekChecks(campaign, academy, person, 0, resources);
                }

                return true;
            }

            if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                processNewWeekChecks(campaign, academy, person, daysOfEducation, resources);
            }

            return false;
        } else {
            person.setEduDaysOfEducation(daysOfEducation - 1);

            if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                processNewWeekChecks(campaign, academy, person, daysOfEducation - 1, resources);
            }

            if ((daysOfEducation - 1) < 1) {
                if (graduationPicker(campaign, person, academy, resources)) {
                    person.setEduEducationStage(4);
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Picks the appropriate graduation method based on the given campaign, person, academy, and resources.
     *
     * @param campaign the campaign to use for calculations
     * @param person the person to determine graduation for
     * @param academy the academy to determine graduation from
     * @param resources the resources to use for graduation
     * @return true if the person successfully graduates; otherwise, false
     */
    private static boolean graduationPicker(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        if ((academy.isClan()) && (academy.isPrepSchool())) {
            if (person.getAge(campaign.getLocalDate()) < 10) {
                graduateClanCreche(campaign, person, academy, resources);
            } else {
                graduateClanSibko(campaign, person, academy, resources);
            }
        } else if (academy.isPrepSchool()) {
            graduateChild(campaign, person, academy, resources);
        } else if (academy.isClan()) {
            graduateReeducationCamp(campaign, person, academy, resources);
        } else {
            return graduateAdult(campaign, person, academy, resources);
        }

        return true;
    }

    /**
     * Begins the journey for a person to return home from an academy.
     *
     * @param campaign   the current campaign
     * @param person     the person returning from the academy
     * @param academy    the academy the person attended
     * @param resources  the resource bundle containing localized strings
     */
    private static void beginJourneyHome(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int travelTime = 0;

        if ((academy.isClan()) && (academy.isPrepSchool())) {
            // we do this to deliberately create an infinite loop, where the player is pestered
            // daily until the student is assigned to a Sibko
            person.setEduDaysOfTravelFromAcademy(travelTime);

            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("creche.text"));
        } else if ((academy.isClan()) && (academy.isTrueborn())) {
            try {
                travelTime = Math.max(2, campaign.getSimplifiedTravelTime(campaign.getFaction().getStartingPlanet(campaign, campaign.getLocalDate())));
            } catch (Exception e) {
                travelTime = Math.max(2, campaign.getSimplifiedTravelTime(campaign.getSystemById("Strana Mechty")));
            }
        } else {
            travelTime = Math.max(2, campaign.getSimplifiedTravelTime(campaign.getSystemById(person.getEduAcademySystem())));
        }

        campaign.addReport(person.getHyperlinkedName() + ' '
                + String.format(resources.getString("returningFromSchool.text"), travelTime));

        person.setEduDaysOfTravelFromAcademy(travelTime);
        person.setEduDaysOfTravel(0);
        person.setEduEducationStage(5);
    }

    /**
     * Processes the journey home for a person.
     *
     * @param campaign         the campaign the person is in
     * @param person           the person whose journey home is being processed
     */
    private static void processJourneyHome(Campaign campaign, Person person) {
        int travelTime;

        try {
            travelTime = Math.max(2, campaign.getSimplifiedTravelTime(campaign.getSystemById(person.getEduAcademySystem())));

            if (travelTime != person.getEduDaysOfTravel()) {
                person.setEduDaysOfTravelFromAcademy(travelTime);
            }
        } finally {
            person.setEduDaysOfTravel(person.getEduDaysOfTravel() + 1);
        }

        if ((travelTime - person.getEduDaysOfTravel()) < 1) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
        }
    }

    /**
     * Retrieves an Academy object based on the provided parameters.
     *
     * @param academySetName The academy set name to match.
     * @param academyNameInSet The academy name in the set to match.
     * @return The Academy that matches the provided parameters or null if no match is found.
     */
    public static Academy getAcademy(String academySetName, String academyNameInSet) {

        List<String> setNames = AcademyFactory.getInstance().getAllSetNames();

        return setNames.stream()
                .filter(set -> set.equals(academySetName))
                .map(set -> AcademyFactory.getInstance().getAllAcademiesForSet(set))
                .flatMap(Collection::stream)
                .filter(academy -> academy.getName().equals(academyNameInSet)).findFirst().orElse(null);
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
        if ((campaign.getCampaignOptions().isEnableRandomXp()) && (campaign.getLocalDate().getDayOfMonth() == 1)) {
            if (Compute.d6(2) >= academy.getFacultySkill()) {
                person.awardXP(campaign, campaign.getCampaignOptions().getRandomXpRate());
            }
        }

        if (daysOfEducation > 0) {
            // It's unlikely we'll ever get canonical destruction or closure dates for all the academies,
            // so no need to check these more than once a year
            if ((campaign.getLocalDate().getDayOfMonth() == 1) && (campaign.getLocalDate().getMonthValue() == 1)) {
                // time to check whether the academy is still standing.
                if (checkForAcademyDestruction(campaign, academy, person, resources)) {
                    return;
                }

                // is the academy still open?
                if (checkForAcademyClosure(campaign, academy, person, resources)) {
                    return;
                }

                // has the academy been moved alongside the faction capital?
                if ((academy.isClan()) && (!academy.isLocal())) {
                    PlanetarySystem location;

                    try {
                        location = campaign.getFaction().getStartingPlanet(campaign, campaign.getLocalDate());

                        if (!person.getEduAcademySystem().equalsIgnoreCase(location.getId())) {
                            person.setEduAcademySystem(location.getId());

                            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("eventAcademyMoved.text"),
                                    location.getName(campaign.getLocalDate())));
                        }
                    } catch (Exception e) {
                        // it'll only throw an exception if there isn't a starting planet for the faction,
                        // at which point there is no point checking for change
                    }
                }
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
            checkForTrainingAccidents(campaign, academy, person, resources);
        }
    }

    /**
     * Checks for training accidents for a person in a campaign.
     *
     * @param campaign   the campaign the person is part of
     * @param academy    the academy the person belongs to
     * @param person     the person to check for training accidents
     * @param resources  the resource bundle for getting localized strings
     */
    private static void checkForTrainingAccidents(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if ((academy.isClan()) || (academy.isMilitary())) {
            int roll;
            int diceSize;

            int warriorDiceSize = campaign.getCampaignOptions().getWarriorCasteAccidents();
            int otherCasteDiceSize = campaign.getCampaignOptions().getOtherCasteAccidents();
            int militaryDiceSize = campaign.getCampaignOptions().getMilitaryAcademyAccidents();

            if ((academy.isClan()) && (academy.isPrepSchool())) {
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
            } else if (academy.isClan()) {
                if (otherCasteDiceSize > 1) {
                    roll = Compute.randomInt(otherCasteDiceSize);
                } else {
                    roll = -1;
                }

                diceSize = otherCasteDiceSize;
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

                            campaign.addReport(person.getHyperlinkedName() + ' '
                                    + String.format(resources.getString("eventTrainingAccident.text"), roll));

                            if (!academy.isPrepSchool()) {
                                person.setEduDaysOfEducation(person.getEduDaysOfEducation() + roll);
                            }

                            // this checks to see if the personnel is in a Sibko
                            if ((academy.isClan()) && (academy.isPrepSchool()) && (person.getAge(campaign.getLocalDate()) >= 10)) {
                                if (person.getEduCourseIndex() <= 6) {
                                    processClanWashout(campaign, person, person.getEduCourseIndex(), resources);
                                }
                            }
                        } else {
                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventTrainingAccidentKilled.text"));
                            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACCIDENTAL);
                        }
                    } else {
                        roll = Compute.d6(3);

                        campaign.addReport(person.getHyperlinkedName() + ' '
                                + String.format(resources.getString("eventTrainingAccident.text"), roll));

                        if (!academy.isPrepSchool()) {
                            person.setEduDaysOfEducation(person.getEduDaysOfEducation() + roll);
                        }
                    }
                }
            }
        }
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
        int clanWarriorDiceSize = campaign.getCampaignOptions().getWarriorCasteDropOutChance();
        int clanOtherDiceSize = campaign.getCampaignOptions().getOtherCasteDropOutChance();

        if (academy.isClan()) {
            // we don't want creche aspirants washing out
            if (academy.isPrepSchool()) {
                return false;
            }

            if ((person.getAge(campaign.getLocalDate()) < 20) && (person.getEduCourseIndex() <= 6)) {
                if (clanWarriorDiceSize > 1) {
                    roll = Compute.randomInt(clanWarriorDiceSize);
                } else {
                    // this is to avoid an exception for dice sizes of 0
                    roll = -1;
                }

                diceSize = clanWarriorDiceSize;
            } else {
                if (clanOtherDiceSize > 1) {
                    roll = Compute.randomInt(clanOtherDiceSize);
                } else {
                    // this is to avoid an exception for dice sizes of 0
                    roll = -1;
                }

                diceSize = clanOtherDiceSize;
            }
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

        if (diceSize > 0) {
            if (roll == 0) {
                // we add this limiter to avoid a bad play experience when someone drops out in the final stretch
                if (daysOfEducation >= 10) {
                    // if it's a Sibko we wash them out, instead
                    if ((academy.isClan()) && (person.getAge(campaign.getLocalDate()) < 20)) {
                        if (person.getEduCourseIndex() == 10) {
                            // this reflects the unwillingness of people to washout from the Labor Caste
                            if (Compute.d6(1) < 3) {
                                processClanWashout(campaign, person, person.getEduCourseIndex(), resources);
                            } else {
                                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));
                            }
                        } else {
                            processClanWashout(campaign, person, person.getEduCourseIndex(), resources);
                        }
                        // if it isn't a Sibko, we assume it was a reeducation camp, so they flee.
                    } else if (academy.isClan()) {
                        ServiceLogger.eduClanFlee(person, campaign.getLocalDate());
                        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MIA);
                    } else {
                        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOut.text"));
                    }
                } else {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));
                }

                return true;
            } else if ((roll < (diceSize / 20)) && (!academy.isPrepSchool())) {
                // might as well scare the player
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));

                return true;
            }
        }
        return false;
    }

    /**
     * Process clan washout.
     *
     * @param campaign The ongoing campaign.
     * @param person The person being washed out.
     * @param courseIndex the caste the person is washing out of.
     * @param resources The resource bundle containing localized strings.
     * @throws IllegalStateException If the clan sibko course index is unexpected.
     */
    private static void processClanWashout(Campaign campaign, Person person, Integer courseIndex, ResourceBundle resources) {
        switch (courseIndex) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                processWarriorCasteWashout(campaign, person, resources);
                break;
            case 7:
                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedScientist.text"));

                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                        resources.getString("graduatedScientist.text"),
                        resources.getString("graduatedWarriorLabor.text")));
                person.setEduCourseIndex(10);
                person.setEduAcademyName(generateClanEducationCode(campaign, person, 10, resources));

                break;
            case 8:
                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedMerchant.text"));

                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                        resources.getString("graduatedMerchant.text"),
                        resources.getString("graduatedWarriorLabor.text")));
                person.setEduCourseIndex(10);
                person.setEduAcademyName(generateClanEducationCode(campaign, person, 10, resources));

                break;
            case 9:
                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedTechnician.text"));

                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                        resources.getString("graduatedTechnician.text"),
                        resources.getString("graduatedWarriorLabor.text")));
                person.setEduCourseIndex(10);
                person.setEduAcademyName(generateClanEducationCode(campaign, person, 10, resources));

                break;
            case 10:
                ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString("graduatedLabor.text"));


                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                        resources.getString("graduatedLabor.text"),
                        resources.getString("washoutLabor.text")));

                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MISSING);
                person.setEduDaysOfEducation(0);
                break;
            default:
                throw new IllegalStateException("Unexpected Clan Sibko Course Index: " + person.getEduCourseIndex());
        }
    }

    /**
     * Checks for a conflict between the academy faction and the person's faction
     *
     * @param campaign the campaign in which the conflict is being checked
     * @param academy the academy for which the conflict is being checked
     * @param person the person whose faction is being tested for conflict
     * @param resources the resource bundle for localized strings
     * @return true if a faction conflict is found and resolved, false otherwise
     */
    private static boolean checkForAcademyFactionConflict(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if (academy.isFactionConflict(campaign, person)) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventWarExpelled.text"));
            person.setEduDaysOfEducation(0);

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
                    person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MISSING);
                    person.setEduDaysOfEducation(0);
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
     */
    private static void processWarriorCasteWashout(Campaign campaign, Person person, ResourceBundle resources) {
        // log washout
        String originCaste = getWarriorCasteSubCasteString(person.getEduCourseIndex());

        ServiceLogger.eduClanWashout(person, campaign.getLocalDate(), resources.getString(originCaste));

        // check for second chance caste
        if (!campaign.getCampaignOptions().getSecondChanceCaste().isNone()) {
            int secondChanceCasteIndex = getSecondChanceCasteIndex(campaign);

            // a secondChanceCasteIndex of -1 means second chance caste is set to 'NONE', so we can skip the switch
            if ((secondChanceCasteIndex != -1) && (person.getEduCourseIndex() != secondChanceCasteIndex)) {
                switch (campaign.getCampaignOptions().getSecondChanceCaste()) {
                    case BA:
                        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                                resources.getString(originCaste),
                                resources.getString("graduatedWarriorBa.text")));
                        person.setEduCourseIndex(4);
                        person.setEduAcademyName(generateClanEducationCode(campaign, person, 4, resources));

                        return;
                    case INFANTRY:
                        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                                resources.getString(originCaste),
                                resources.getString("graduatedWarriorInfantry.text")));
                        person.setEduCourseIndex(5);
                        person.setEduAcademyName(generateClanEducationCode(campaign, person, 5, resources));

                        return;
                    case VEHICLE:
                        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                                resources.getString(originCaste),
                                resources.getString("graduatedWarriorVehicle.text")));
                        person.setEduCourseIndex(6);
                        person.setEduAcademyName(generateClanEducationCode(campaign, person, 6, resources));

                        return;
                    default:
                        throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/education/EducationController.java/processWarriorCasteWashout: "
                                + campaign.getCampaignOptions().getSecondChanceCaste());
                }
            }
        }

        // otherwise, process washout into a civilian caste
        int fallbackScientist = campaign.getCampaignOptions().getFallbackScientist();
        int fallbackMerchant = campaign.getCampaignOptions().getFallbackMerchant() + fallbackScientist;
        int fallbackTechnician = campaign.getCampaignOptions().getFallbackTechnician() + fallbackMerchant;
        int fallbackLabour = campaign.getCampaignOptions().getFallbackLabor() + fallbackTechnician;

        int roll = Compute.randomInt(fallbackLabour);

        if (roll < fallbackScientist) {

            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                    resources.getString(originCaste),
                    resources.getString("graduatedWarriorScientist.text")));
            person.setEduCourseIndex(7);
            person.setEduAcademyName(generateClanEducationCode(campaign, person, 7, resources));

            return;
        }

        if (roll < fallbackMerchant) {

            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                    resources.getString(originCaste),
                    resources.getString("graduatedWarriorMerchant.text")));
            person.setEduCourseIndex(8);
            person.setEduAcademyName(generateClanEducationCode(campaign, person, 8, resources));

            return;
        }

        if (roll < fallbackTechnician) {

            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                    resources.getString(originCaste),
                    resources.getString("graduatedWarriorTechnician.text")));
            person.setEduCourseIndex(9);
            person.setEduAcademyName(generateClanEducationCode(campaign, person, 9, resources));

            return;
        }

        // Labor

        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("washout.text"),
                resources.getString(originCaste),
                resources.getString("graduatedWarriorLabor.text")));
        person.setEduCourseIndex(10);
        person.setEduAcademyName(generateClanEducationCode(campaign, person, 10, resources));
    }


    /**
     * Checks if a person graduates from an academy as an adult.
     *
     * @param campaign    the campaign in which the graduation occurs
     * @param person      the person who is graduating
     * @param academy     the academy from which the person is graduating
     * @param resources   the ResourceBundle containing localized messages
     * @return true if the person completed their education, false otherwise
     */
    private static boolean graduateAdult(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int graduationRoll = Compute.randomInt(100);
        int roll;

        // qualification failed
        if (graduationRoll < 5) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedFailed.text"));
            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName());

            improveSkills(campaign, person, academy, false);

            return true;
        }

        // class resits required
        if (graduationRoll < 20) {
            roll = Compute.d6(3);
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedClassNeeded.text"), roll));

            person.setEduDaysOfEducation(roll);

            return false;
        }

        if (graduationRoll == 99) {
            if (Compute.d6(1) > 5) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTop.text"),
                        ' ' + resources.getString(graduationEventPicker())));
            } else {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTop.text"));
            }

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedTopLog.text"), person.getEduAcademyName());

            processGraduation(campaign, person, academy, 2, resources);

            if (!academy.isMilitary()) {
                reportMastersOrDoctorateGain(campaign, person, resources);
            } else if (!Objects.equals(academy.getPromotion(), "None")) {
                campaign.addReport(person.getHyperlinkedName() + ' ' +
                        String.format(resources.getString("graduatedRank.text"), academy.getPromotion()));
            }

            return true;
        }

        // graduated with honors
        if (graduationRoll >= 90) {
            if (Compute.d6(1) > 5) {

                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedHonors.text"),
                        ' ' + resources.getString(graduationEventPicker())));
            } else {
                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedHonors.text"), ""));
            }

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedHonorsLog.text"), person.getEduAcademyName());

            processGraduation(campaign, person, academy, 1, resources);

            if (!academy.isMilitary()) {
                reportMastersOrDoctorateGain(campaign, person, resources);
            } else if (!Objects.equals(academy.getPromotion(), "None")) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedRank.text"),
                        academy.getPromotion()));
            }

            return true;
        }

        // default graduation
        if (Compute.d6(1) > 5) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduated.text"),
                    ' ' + resources.getString(graduationEventPicker())));
        } else {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduated.text"), ""));
        }

        ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName());

        processGraduation(campaign, person, academy, 0, resources);

        if (!academy.isMilitary()) {
            reportMastersOrDoctorateGain(campaign, person, resources);
        } else if (!Objects.equals(academy.getPromotion(), "None")) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedRank.text"),
                    academy.getPromotion()));
        }
        return true;
    }

    /**
     * This method generates a report for individuals who have completed either a Master's or Doctorate degree.
     *
     * @param campaign   the campaign to add the report to
     * @param person     the person who completed the degree
     * @param resources  the resource bundle containing localized strings
     */
    private static void reportMastersOrDoctorateGain(Campaign campaign, Person person, ResourceBundle resources) {
        if (person.getEduHighestEducation() == 3) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedMasters.text"));

            ServiceLogger.eduGraduatedMasters(person, campaign.getLocalDate(), person.getEduAcademyName());

        } else if (person.getEduHighestEducation() == 4) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedDoctorate.text"));

            ServiceLogger.eduGraduatedDoctorate(person, campaign.getLocalDate(), person.getEduAcademyName());

            person.setPreNominal("Dr");
        }
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

        // qualification failed
        if (graduationRoll < 30) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedBarely.text"));
            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName());

            improveSkills(campaign, person, academy, false);

            return;
        }

        // default graduation
        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduated.text"), ""));

        ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName());

        processGraduation(campaign, person, academy, 0, resources);
    }

    /**
     * Graduates a person from the Clan Creche in a campaign.
     * This method adds a report to the campaign, logs the event using the ServiceLogger,
     * improves the person's skills, and sets their highest education level to the education level
     * obtained from the Creche.
     *
     * @param campaign   the campaign where the graduation is taking place
     * @param person     the person being graduated
     * @param academy    the Creche responsible for the graduation
     * @param resources  the ResourceBundle containing localized text
     */
    private static void graduateClanCreche(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedCreche.text"));

        ServiceLogger.eduClanCreche(person, campaign.getLocalDate());

        processGraduation(campaign, person, academy, 0, resources);
    }

    /**
     * Graduates a child from a Sibko.
     *
     * @param campaign the campaign the person belongs to
     * @param person the person being graduated
     * @param academy the Sibko from which the person is being graduated
     */
    private static void graduateClanSibko(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        // Warrior Caste
        // [0]MechWarrior, [1]ProtoMech, [2]Aerospace, [3]Space, [4]BA, [5]CI, [6]Vehicle
        if (person.getEduCourseIndex() <= 6) {
            graduateWarriorCaste(campaign, person, academy, resources);

            return;
        }

        // [7]Scientist Caste
        if (person.getEduCourseIndex() == 7) {
            graduateScientistCaste(campaign, person, academy, resources);

            return;
        }

        // [8]Merchant Caste
        if (person.getEduCourseIndex() == 8) {
            graduateMerchantCaste(campaign, person, academy, resources);

            return;
        }

        // [9]Technician Caste
        if (person.getEduCourseIndex() == 9) {
            graduateTechnicianCaste(campaign, person, academy, resources);

            return;
        }

        // [10]Laborer Caste
        graduateLaborCaste(campaign, person, academy, resources);
    }

    /**
     * Graduates a person from the academy and reports based on the graduation roll.
     *
     * @param campaign the current campaign.
     * @param person the person to be graduated.
     * @param academy the academy where the warrior is being graduated from.
     * @param resources the ResourceBundle containing localized text.
     */
    private static void graduateWarriorCaste(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int graduationRoll = Compute.randomInt(100);

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
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorFailed.text"));

            processClanWashout(campaign, person, person.getEduCourseIndex(), resources);
            graduateClanSibko(campaign, person, academy, resources);

            return;
        }

        // one kill
        if (graduationRoll < 90) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorOneKill.text"));
            ServiceLogger.eduClanWarrior(person, campaign.getLocalDate(), resources.getString("graduatedWarriorOneKillLog.text"));

            processGraduation(campaign, person, academy, 0, resources);

            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedRankClan.text"),
                    resources.getString("graduatedRankClanWarrior.text"), resources.getString("graduatedWeightLight.text")));
            return;
        }

        // two kills
        if (graduationRoll < 99) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorTwoKills.text"));
            ServiceLogger.eduClanWarrior(person, campaign.getLocalDate(), resources.getString("graduatedWarriorTwoKillsLog.text"));

            processGraduation(campaign, person, academy, 1, resources);

            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedRankClan.text"),
                    resources.getString("graduatedRankStarCommander.text"), resources.getString("graduatedWeightMedium.text")));
            return;
        }

        // three kills
        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedWarriorThreeKills.text"));
        ServiceLogger.eduClanWarrior(person, campaign.getLocalDate(), resources.getString("graduatedWarriorThreeKillsLog.text"));

        processGraduation(campaign, person, academy, 2, resources);

        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedRankClan.text"),
                resources.getString("graduatedRankStarCaptain.text"), resources.getString("graduatedWeightHeavy.text")));
    }

    /**
     * Graduates a person from a reeducation camp based on their education course.
     *
     * @param campaign   the campaign the person is part of
     * @param person     the person being graduated
     * @param academy    the academy for reeducation
     * @param resources  the resource bundle for localized messages
     */
    private static void graduateReeducationCamp(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        // [0]Scientist Caste
        if (person.getEduCourseIndex() == 0) {
            graduateScientistCaste(campaign, person, academy, resources);
        }

        // [1]Merchant Caste
        if (person.getEduCourseIndex() == 1) {
            graduateMerchantCaste(campaign, person, academy, resources);
        }

        // [2]Technician Caste
        if (person.getEduCourseIndex() == 2) {
            graduateTechnicianCaste(campaign, person, academy, resources);
        }

        // [3]Laborer Caste
        if (person.getEduCourseIndex() == 3) {
            graduateLaborCaste(campaign, person, academy, resources);
        }
    }

    /**
     * Graduates a person from the labor caste.
     * Updates the person's education level and adds a report to the campaign.
     * Triggers a service logger to record the event.
     *
     * @param campaign   The campaign in which the graduation occurs.
     * @param person     The person who is being graduated.
     * @param academy    The academy used for improving skills and determining education level.
     * @param resources  The resource bundle containing localized strings.
     */
    private static void graduateLaborCaste(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                "", resources.getString("graduatedLabor.text")));
        ServiceLogger.eduClanLabor(person, campaign.getLocalDate());

        processGraduation(campaign, person, academy, 0, resources);
    }

    /**
     * Graduates a person from the Technician caste.
     * Updates the person's education level and adds a report to the campaign.
     * Triggers a service logger to record the event.
     *
     * @param campaign   The campaign in which the graduation occurs.
     * @param person     The person who is being graduated.
     * @param academy    The academy used for improving skills and determining education level.
     * @param resources  The resource bundle containing localized strings.
     */
    private static void graduateTechnicianCaste(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int graduationRoll = Compute.randomInt(100);

        if (graduationRoll < 25) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialFailed.text"),
                    resources.getString("graduatedTechnician.text")));

            ServiceLogger.eduClanFailed(person, campaign.getLocalDate(), resources.getString("graduatedTechnician.text"));

            processClanWashout(campaign, person, person.getEduCourseIndex(), resources);
            graduateClanSibko(campaign, person, academy, resources);

            return;
        }

        if (graduationRoll < 90) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                    resources.getString("graduatedClanBarely.text"),
                    resources.getString("graduatedTechnician.text")));

            ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                    resources.getString("graduatedClanBarely.text"),
                    resources.getString("graduatedTechnician.text"));

            processGraduation(campaign, person, academy, 0, resources);

            return;
        }

        if (graduationRoll < 99) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                    resources.getString("graduatedEasily.text"), resources.getString("graduatedTechnician.text")));

            ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                    resources.getString("graduatedEasily.text"),
                    resources.getString("graduatedTechnician.text"));

            processGraduation(campaign, person, academy, 1, resources);

            return;
        }

        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                resources.getString("graduatedEffortlessly.text"), resources.getString("graduatedTechnician.text")));

        ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                resources.getString("graduatedEffortlessly.text"),
                resources.getString("graduatedTechnician.text"));

        processGraduation(campaign, person, academy, 2, resources);
    }

    /**
     * Graduates a person from the Merchant caste.
     * Updates the person's education level and adds a report to the campaign.
     * Triggers a service logger to record the event.
     *
     * @param campaign   The campaign in which the graduation occurs.
     * @param person     The person who is being graduated.
     * @param academy    The academy used for improving skills and determining education level.
     * @param resources  The resource bundle containing localized strings.
     */
    private static void graduateMerchantCaste(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int graduationRoll = Compute.randomInt(100);

        if (graduationRoll < 25) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialFailed.text"),
                    resources.getString("graduatedMerchant.text")));

            ServiceLogger.eduClanFailed(person, campaign.getLocalDate(), resources.getString("graduatedMerchant.text"));

            processClanWashout(campaign, person, person.getEduCourseIndex(), resources);
            graduateClanSibko(campaign, person, academy, resources);

            return;
        }

        if (graduationRoll < 90) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTrialPassed.text"),
                    resources.getString("graduatedClanBarely.text"), resources.getString("graduatedMerchant.text"));

            ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                    resources.getString("graduatedClanBarely.text"),
                    resources.getString("graduatedMerchant.text"));

            processGraduation(campaign, person, academy, 0, resources);

            return;
        }

        if (graduationRoll < 99) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                    resources.getString("graduatedEasily.text"), resources.getString("graduatedMerchant.text")));

            ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                    resources.getString("graduatedEasily.text"),
                    resources.getString("graduatedMerchant.text"));

            processGraduation(campaign, person, academy, 1, resources);

            return;
        }

        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                resources.getString("graduatedEffortlessly.text"), resources.getString("graduatedMerchant.text")));

        ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                resources.getString("graduatedEffortlessly.text"),
                resources.getString("graduatedMerchant.text"));

        processGraduation(campaign, person, academy, 2, resources);
    }

    /**
     * Graduates a person from the Scientist caste.
     * Updates the person's education level and adds a report to the campaign.
     * Triggers a service logger to record the event.
     *
     * @param campaign   The campaign in which the graduation occurs.
     * @param person     The person who is being graduated.
     * @param academy    The academy used for improving skills and determining education level.
     * @param resources  The resource bundle containing localized strings.
     */
    private static void graduateScientistCaste(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        int graduationRoll = Compute.randomInt(100);

        if (graduationRoll < 25) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialFailed.text"),
                    resources.getString("graduatedScientist.text")));

            ServiceLogger.eduClanFailed(person, campaign.getLocalDate(), resources.getString("graduatedScientist.text"));

            processClanWashout(campaign, person, person.getEduCourseIndex(), resources);
            graduateClanSibko(campaign, person, academy, resources);

            return;
        }

        if (graduationRoll < 90) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                    resources.getString("graduatedClanBarely.text"),
                    resources.getString("graduatedScientist.text")));

            ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                    resources.getString("graduatedClanBarely.text"),
                    resources.getString("graduatedScientist.text"));

            processGraduation(campaign, person, academy, 0, resources);

            return;
        }

        if (graduationRoll < 99) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                    resources.getString("graduatedEasily.text"), resources.getString("graduatedScientist.text")));

            ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                    resources.getString("graduatedEasily.text"),
                    resources.getString("graduatedScientist.text"));

            processGraduation(campaign, person, academy, 1, resources);

            return;
        }

        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTrialPassed.text"),
                resources.getString("graduatedEffortlessly.text"), resources.getString("graduatedScientist.text")));

        ServiceLogger.eduClanPassed(person, campaign.getLocalDate(),
                resources.getString("graduatedEffortlessly.text"),
                resources.getString("graduatedScientist.text"));

        processGraduation(campaign, person, academy, 2, resources);
    }

    /**
     * Graduates a person from an academy and potentially applies a skill bonus.
     *
     * @param campaign      the campaign associated with the person's education
     * @param person        the person who is graduating with a bonus
     * @param academy       the academy from which the person is graduating
     * @param bonusCount    the number of bonuses to be added
     * @param resources     the resource bundle for retrieving localized messages
     */
    private static void processGraduation(Campaign campaign, Person person, Academy academy, Integer bonusCount, ResourceBundle resources) {
        improveSkills(campaign, person, academy, true);

        if ((campaign.getCampaignOptions().isEnableBonuses()) && (bonusCount > 0)) {
            addBonus(campaign, person, academy, bonusCount, resources);
        }

        int educationLevel = academy.getEducationLevel(person);

        if (person.getEduHighestEducation() < educationLevel) {
            person.setEduHighestEducation(educationLevel);
        }

        if ((academy.isReeducationCamp()) && (campaign.getCampaignOptions().isUseReeducationCamps())) {
            if (!Objects.equals(person.getOriginFaction(), campaign.getFaction())) {
                person.setOriginFaction(campaign.getFaction());
            }
        }
    }

    /**
     * Improves the skills of a given person based on the curriculum of the course they are attending.
     *
     * @param person The Person whose skills are being improved.
     * @param academy The academy the person is attending.
     * @param isGraduating A boolean value indicating whether the person is graduating from the academy or not.
     */
    private static void improveSkills(Campaign campaign, Person person, Academy academy, Boolean isGraduating) {
        String[] curriculum = academy.getCurriculums().get(person.getEduCourseIndex()).split(",");

        curriculum = Arrays.stream(curriculum)
                .map(String::trim)
                .toArray(String[]::new);

        int educationLevel = academy.getEducationLevel(person) + academy.getBaseAcademicSkillLevel();

        if (educationLevel > 10) {
            educationLevel = 10;
        } else if (educationLevel < 0) {
            educationLevel = 0;
        }

        if (!isGraduating) {
            educationLevel--;

            if (educationLevel < 0) {
                return;
            }
        }

        for (String skill : curriculum) {
            if (skill.equalsIgnoreCase("bonus xp")) {
                person.awardXP(campaign, Compute.d6(Math.max(1, educationLevel)));
            } else {
                String skillParsed = Academy.skillParser(skill);
                int bonus;

                if ((person.hasSkill(skillParsed)) && (person.getSkill(skillParsed).getExperienceLevel() < educationLevel)) {
                    bonus = person.getSkill(skillParsed).getBonus();

                    int skillLevel = 0;
                    while (person.getSkill(skillParsed).getExperienceLevel() < (educationLevel + bonus)) {
                        person.addSkill(skillParsed, skillLevel, bonus);
                        skillLevel++;
                    }
                } else if (!person.hasSkill(skillParsed)) {
                    int skillLevel = 0;
                    person.addSkill(skillParsed, skillLevel, 0);

                    while (person.getSkill(skillParsed).getExperienceLevel() < educationLevel) {
                        person.addSkill(skillParsed, skillLevel, 0);
                        skillLevel++;
                    }
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
     * @param resources      The resource bundle used for getting localized strings
     */
    private static void addBonus(Campaign campaign, Person person, Academy academy, int bonusCount, ResourceBundle resources) {
        List<String> curriculum = Arrays.asList(academy.getCurriculums().get(person.getEduCourseIndex()).split(","));

        curriculum = curriculum.stream()
                .map(String::trim)
                .collect(Collectors.toList());

        for (int i = 0; i < bonusCount; i++) {
            int roll = Compute.randomInt(curriculum.size());

            try {
                String skillParsed = Academy.skillParser(curriculum.get(roll));

                // if 'person' already has a +1 bonus for the skill, we give them XP, instead
                if (person.getSkill(skillParsed).getBonus() < 1) {
                    int skillLevel = person.getSkill(skillParsed).getLevel();
                    int bonus = person.getSkill(skillParsed).getBonus() + 1;

                    person.addSkill(skillParsed, skillLevel, bonus);

                    campaign.addReport(String.format(resources.getString("bonusAdded.text"),
                            person.getFirstName()));
                } else {
                    roll = Compute.d6(2);
                    person.awardXP(campaign, roll);

                    campaign.addReport(String.format(resources.getString("bonusXp.text"),
                            person.getFirstName(), roll));
                }
            } catch (Exception e) {
                // if we get this, it means the 'skill' was Bonus XP
                person.awardXP(campaign, Compute.d6(2));

                campaign.addReport(String.format(resources.getString("bonusXp.text"),
                        person.getFirstName(), roll));
            }
        }
    }

    /**
     * Picks a random graduation event from the graduation event table.
     *
     * @return a string representing the chosen graduation event
     */
    private static String graduationEventPicker() {
        List<String> graduationEventTable = graduationEventTable();

        return graduationEventTable.get(Compute.randomInt(graduationEventTable.size()));
    }

    /**
     * Retrieves the course index value associated with the SecondChanceCaste assigned in campaign options.
     *
     * @return The course index of the chosen SecondChanceCaste.
     * Returns -1 if NONE; 4 if BA; 5 if INFANTRY; 6 if VEHICLE.
     */
    private static int getSecondChanceCasteIndex(Campaign campaign) {
        switch (campaign.getCampaignOptions().getSecondChanceCaste()) {
            case NONE:
                return -1;
            case BA:
                return 4;
            case INFANTRY:
                return 5;
            case VEHICLE:
                return 6;
        }

        return -1;
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
                // the events
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
                "eventAccident.text",
                "eventAirRaid.text",
                "eventAlumni.text",
                "eventAngryParent.text",
                "eventArrest.text",
                "eventAudioIssues.text",
                "eventBagCheckProcedure.text",
                "eventBalloons.text",
                "eventBells.text",
                "eventBillboard.text",
                "eventBonfire.text",
                "eventBooing.text",
                "eventCancellations.text",
                "eventCandles.text",
                "eventCatererDelay.text",
                "eventCellPhoneRing.text",
                "eventChaos.text",
                "eventCheerName.text",
                "eventCheerStage.text",
                "eventCoins.text",
                "eventComplaints.text",
                "eventConflict.text",
                "eventCrowdedRestrooms.text",
                "eventCulturalPerformance.text",
                "eventDanceFlashMob.text",
                "eventDanceRoutine.text",
                "eventDanceTraditional.text",
                "eventDelayedStart.text",
                "eventDisappointment.text",
                "eventDisorganization.text",
                "eventDoves.text",
                "eventElevatorMalfunction.text",
                "eventEmbarrassment.text",
                "eventEquipmentAdjustment.text",
                "eventEquipmentFailure.text",
                "eventEvacuation.text",
                "eventFailure.text",
                "eventFainting.text",
                "eventFighting.text",
                "eventFireAlarm.text",
                "eventFireDance.text",
                "eventFireworks.text",
                "eventFoodPoisoning.text",
                "eventFoodShortage.text",
                "eventFoodTruck.text",
                "eventForgottenSpeech.text",
                "eventFundraiser.text",
                "eventGlitter.text",
                "eventGuestLateArrival.text",
                "eventGuestSpeakerCue.text",
                "eventHandshake.text",
                "eventHealthEmergency.text",
                "eventInadequateSeating.text",
                "eventInjury.text",
                "eventKaraoke.text",
                "eventLackluster.text",
                "eventLateProgramDistribution.text",
                "eventLateSpeaker.text",
                "eventLateVendorSetup.text",
                "eventLeak.text",
                "eventLegalIssue.text",
                "eventLetter.text",
                "eventLightingAdjustment.text",
                "eventLiveMusic.text",
                "eventLogisticalNightmare.text",
                "eventLostAndFound.text",
                "eventLostDirections.text",
                "eventMenuChange.text",
                "eventMessage.text",
                "eventMiscommunication.text",
                "eventMissingEquipment.text",
                "eventNoShow.text",
                "eventOutburst.text",
                "eventOutdoorGames.text",
                "eventOvercrowding.text",
                "eventOverflowParking.text",
                "eventOversight.text",
                "eventParkingDirection.text",
                "eventParkingIssues.text",
                "eventPassingTorch.text",
                "eventPhotographerDelay.text",
                "eventPhotographerObstruction.text",
                "eventPoorAttendance.text",
                "eventPowerOutage.text",
                "eventPowerStruggle.text",
                "eventPresentationGlitch.text",
                "eventProcession.text",
                "eventProgramMisprint.text",
                "eventProtest.text",
                "eventPublicAddress.text",
                "eventPublicTransportDelay.text",
                "eventRain.text",
                "eventRoast.text",
                "eventScheduleAdjustment.text",
                "eventSecurityBreach.text",
                "eventSecurityCheckDelay.text",
                "eventSecurityConcern.text",
                "eventSkywriting.text",
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
                "eventTicketMixUp.text",
                "eventTicketScanningDelay.text",
                "eventTimeCapsule.text",
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
                "eventVr.text",
                "eventWardrobeMalfunction.text",
                "eventWeatherDisruption.text",
                "eventWeatherForecastError.text",
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
                "speechApology.text",
                "speechBlunder.text",
                "speechCommunityInvolvement.text",
                "speechControversial.text",
                "speechDisrespectful.text",
                "speechEmotional.text",
                "speechHumility.text",
                "speechHumorous.text",
                "speechInsensitive.text",
                "speechInspiration.text",
                "speechMechCommander.text",
                "speechMispronunciation.text",
                "speechMotivation.text",
                "speechPromotion.text",
                "speechRant.text",
                "speechReflection.text",
                "speechRejection.text",
                "speechTriumph.text",
                "speechUninspiring.text",
                "surpriseArgument.text",
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
                "surpriseStandard.text");
    }
}
