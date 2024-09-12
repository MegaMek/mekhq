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
package mekhq.campaign.personnel.education;

import megamek.common.Compute;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.enums.education.EducationStage;
import mekhq.campaign.personnel.randomEvents.enums.personalities.Intelligence;

import java.time.DayOfWeek;
import java.util.*;

/**
 * The EducationController class is responsible for managing the education process.
 * It provides methods to begin the education process, calculate education level, and enroll a person into an academy.
 */
public class EducationController {
    private static final MMLogger logger = MMLogger.create(EducationController.class);

    /**
     * Checks eligibility for enrollment in an academy.
     *
     * @param campaign the current options
     * @param person the person applying for enrollment
     * @param academySet the set of academies to search for the desired academy
     * @param academyNameInSet the name of the desired academy within the set
     * @return true if the person is eligible for enrollment, false otherwise
     */
    public static boolean makeEnrollmentCheck(Campaign campaign, Person person, String academySet, String academyNameInSet) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        Academy academy = findAcademyInSet(academySet, academyNameInSet);

        if (academy == null) {
            return false;
        }

        // these academies always accept applicants so personnel aren't locked out of education
        if (academy.isLocal() || academy.isHomeSchool()) {
            return true;
        }

        // has the character already failed to apply to this academy?
        if (person.getEduFailedApplications().contains(academy)) {
            campaign.addReport(String.format(resources.getString("secondApplication.text"),
                    person.getHyperlinkedFullTitle(),
                    academyNameInSet,
                    "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                    "</span>"));
            return false;
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        // Calculate the roll based on Intelligence if necessary
        int roll = Compute.d6(2);
        if (campaignOptions.isUseRandomPersonalities()) {
            roll += (person.getIntelligence().getIntelligenceScore() / 4);
        }

        // Calculate target number based on base target number and faculty skill
        int targetNumber = campaignOptions.getEntranceExamBaseTargetNumber() - academy.getFacultySkill();

        // If roll meets the target number, the application is successful
        if (roll >= targetNumber) {
            return true;
        } else {
            // Mark the academy in the person's list of failed applications preventing re-application
            person.addEduFailedApplications(academy);
            campaign.addReport(String.format(resources.getString("applicationFailure.text"),
                    person.getHyperlinkedFullTitle(),
                    "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                    "</span>",
                    academyNameInSet));

            ServiceLogger.eduFailedApplication(person, campaign.getLocalDate(), academyNameInSet);

            return false;
        }
    }

    /**
     * Finds the academy with the given name in the provided academy set.
     *
     * @param academySet The academy set to search in.
     * @param academyNameInSet The name of the academy to find.
     * @return The academy with the given name, or null if not found.
     */
    private static Academy findAcademyInSet(String academySet, String academyNameInSet) {
        Academy academy = getAcademy(academySet, academyNameInSet);

        if (academy == null) {
            logger.error("No academy found with name {} in set {}", academyNameInSet, academySet);
            return null;
        }

        return academy;
    }

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
     * @param isReEnrollment Whether the person is being re-enrolled.
     */
    public static void performEducationPreEnrollmentActions(Campaign campaign, Person person, String academySet, String academyNameInSet, int courseIndex,
                                                            String campus, String faction, boolean isReEnrollment) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        Academy academy = findAcademyInSet(academySet, academyNameInSet);

        if (academy == null) {
            return;
        }

        // pay tuition
        double tuition = academy.getTuitionAdjusted(person) * academy.getFactionDiscountAdjusted(campaign, person);

        if (tuition > 0) {
            if (campaign.getFinances().getBalance().isLessThan(Money.of(tuition))) {
                String reportMessage = "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>"
                        + String.format(resources.getString("insufficientFunds.text"), person.getFullTitle())
                        + "</span>";

                campaign.addReport(reportMessage);
                return;
            } else {
                campaign.getFinances().debit(TransactionType.EDUCATION,
                        campaign.getLocalDate(),
                        Money.of(tuition),
                        String.format(resources.getString("payment.text"),
                                person.getFullTitle()));
            }
        }

        // with tuition paid, we can enroll/re-enroll Person
        if (isReEnrollment) {
            reEnrollPerson(campaign, person, academy);
        } else {
            person.setEduCourseIndex(courseIndex);
            enrollPerson(campaign, person, academy, campus, faction, courseIndex);
        }

        // notify the user
        if (academy.isHomeSchool()) {
            campaign.addReport(String.format(resources.getString("homeSchool.text"),
                    person.getHyperlinkedFullTitle()));
        } else {
            campaign.addReport(String.format(resources.getString("offToSchool.text"),
                    person.getHyperlinkedFullTitle(),
                    person.getEduAcademyName(),
                    person.getEduJourneyTime()));
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
     * @param courseIndex The index of the course being taken.
     */
    public static void enrollPerson(Campaign campaign, Person person, Academy academy, String campus, String faction, Integer courseIndex) {
        // change status will wipe the academic information, so must always precede the setters
        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.STUDENT);

        if (academy.isHomeSchool()) {
            // if the student is being homeschooled, we skip the journey to the 'academy'
            person.setEduEducationStage(EducationStage.EDUCATION);
        } else {
            person.setEduEducationStage(EducationStage.JOURNEY_TO_CAMPUS);
        }

        person.setEduAcademySet(academy.getSet());
        person.setEduAcademyNameInSet(academy.getName());
        person.setEduEducationTime(academy.getDurationDays());
        person.setEduAcademyFaction(faction);
        person.setEduCourseIndex(courseIndex);

        if (!academy.isHomeSchool()) {
            if (academy.isLocal()) {
                person.setEduJourneyTime(2);
                person.setEduAcademySystem(campaign.getCurrentSystem().getId());
            } else {
                person.setEduJourneyTime(campaign.getSimplifiedTravelTime(campaign.getSystemById(campus)));
                person.setEduAcademySystem(campus);
            }

            for (Person child : person.getGenealogy().getChildren()) {
                if ((child.getStatus().isActive()) && (child.isChild(campaign.getLocalDate()))) {
                    person.addEduTagAlong(child.getId());
                    child.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ON_LEAVE);
                }
            }
        }

        // this should already be 0, but we reset it just in case
        person.setEduDaysOfTravel(0);

        // if the academy is Local, we need to generate a name,
        // otherwise we use the listed name or the campaign name
        if (academy.isHomeSchool()) {
            person.setEduAcademyName(campaign.getName());
        } else if (academy.isLocal()) {
            person.setEduAcademyName(generateName(academy, campus));
        } else {
            person.setEduAcademyName(person.getEduAcademyNameInSet() + " (" + campaign.getSystemById(campus).getName(campaign.getLocalDate()) + ')');
        }

        // we have this all the way at the bottom as a bit of insurance.
        // when troubleshooting, if the log isn't getting entered, we know something went wrong when enrolling.
        ServiceLogger.eduEnrolled(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));
    }


    /**
     * Re-enrolls a person into a campaign and updates their education information.
     *
     * @param campaign    The campaign in which the person is to be re-enrolled.
     * @param person      The person to be re-enrolled.
     * @param academy     The academy or school that the person is being enrolled into.
     */
    public static void reEnrollPerson(Campaign campaign, Person person, Academy academy) {
        if (academy.isHomeSchool()) {
            // if the student is being homeschooled, we skip the journey to the 'academy'
            person.setEduEducationStage(EducationStage.EDUCATION);
        } else {
            person.setEduEducationStage(EducationStage.JOURNEY_TO_CAMPUS);
        }

        person.setEduEducationTime(academy.getDurationDays());

        if (!academy.isHomeSchool()) {
            if ((academy.isLocal()) || (!person.getEduEducationStage().isJourneyFromCampus())) {
                person.setEduJourneyTime(2);
                person.setEduAcademySystem(campaign.getCurrentSystem().getId());
            } else {
                person.setEduJourneyTime(Math.max(2, person.getEduDaysOfTravel()));
            }
        }

        // reset days of travel
        person.setEduDaysOfTravel(0);

        // we have this all the way at the bottom as a bit of insurance.
        // when troubleshooting, if the log isn't getting entered, we know something went wrong when enrolling.
        ServiceLogger.eduReEnrolled(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));
        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }

    /**
     * Generates a name for a local academy or clan education facility.
     *
     * @param academy     The academy to which the person is applying.
     * @param campus      The campus of the academy.
     * @return The generated name.
     */
    public static String generateName(Academy academy, String campus) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        if (academy.isPrepSchool()) {
            if (Compute.d6(1) <= 3) {
                return campus + ' ' + generateTypeChild(resources) + ' ' + resources.getString("conjoinerOf.text") + ' ' + generateSuffix(resources);
            } else {
                return generateTypeChild(resources) + ' ' + resources.getString("conjoinerOf.text") + ' ' + campus;
            }
        }

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

    /**
     * Generates a military academy prefix randomly selected from the resource bundle.
     *
     * @param resources the resource bundle containing the military prefix texts
     * @return a randomly generated military prefix
     * @throws IllegalStateException if an unexpected roll occurs
     */
    private static String generateMilitaryPrefix(ResourceBundle resources) {
        return switch (Compute.d6(1)) {
            case 1 -> resources.getString("prefixCombinedArms.text");
            case 2 -> resources.getString("prefixCombinedForces.text");
            case 3 -> resources.getString("prefixMilitary.text");
            case 4 -> resources.getString("prefixWar.text");
            case 5 -> resources.getString("prefixWarFighting.text");
            case 6 -> resources.getString("prefixCombat.text");
            default -> throw new IllegalStateException("Unexpected roll in generateMilitaryPrefix");
        };
    }

    /**
     * This method generates an academy suffix based on a random roll.
     *
     * @param resources The ResourceBundle containing the suffix texts.
     * @return The generated suffix.
     * @throws IllegalStateException if the random roll is unexpected.
     */
    private static String generateSuffix(ResourceBundle resources) {
        return switch (Compute.d6(1)) {
            case 1 -> resources.getString("suffixTechnology.text");
            case 2 -> resources.getString("suffixTechnologyAdvanced.text");
            case 3 -> resources.getString("suffixScience.text");
            case 4 -> resources.getString("suffixScienceAdvanced.text");
            case 5 -> resources.getString("suffixStudies.text");
            case 6 -> resources.getString("suffixHigherLearning.text");
            default -> throw new IllegalStateException("Unexpected roll in generateSuffix()");
        };
    }

    /**
     * Generates a random educational institution type from the provided ResourceBundle.
     *
     * @param resources the ResourceBundle containing the localized strings for the educational institution types
     * @return a randomly selected educational institution type
     * @throws IllegalStateException if the generated roll is unexpected
     */
    private static String generateTypeAdult(ResourceBundle resources) {
        return switch (Compute.d6(1)) {
            case 1 -> resources.getString("typeAcademy.text");
            case 2 -> resources.getString("typeCollege.text");
            case 3 -> resources.getString("typeInstitute.text");
            case 4 -> resources.getString("typeUniversity.text");
            case 5 -> resources.getString("typePolytechnic.text");
            case 6 -> resources.getString("typeSchool.text");
            default -> throw new IllegalStateException("Unexpected roll in generateTypeAdult");
        };
    }


    /**
     * Generates a random educational institution type from the provided ResourceBundle.
     *
     * @param resources the ResourceBundle containing the localized strings for the educational institution types
     * @return a randomly selected educational institution type
     * @throws IllegalStateException if the generated roll is unexpected
     */
    private static String generateTypeChild(ResourceBundle resources) {
        return switch (Compute.d6(1)) {
            case 1 -> resources.getString("typeAcademy.text");
            case 2 -> resources.getString("typePreparatorySchool.text");
            case 3 -> resources.getString("typeInstitute.text");
            case 4 -> resources.getString("typeSchoolBoarding.text");
            case 5 -> resources.getString("typeFinishingSchool.text");
            case 6 -> resources.getString("typeSchool.text");
            default -> throw new IllegalStateException("Unexpected roll in generateTypeChild");
        };
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

        EducationStage educationStage = person.getEduEducationStage();

        // is person in transit to the institution?
        if (educationStage.isJourneyToCampus()) {
            journeyToAcademy(campaign, person, resources);
            return false;
        }

        // is the person on campus and undergoing education
        if (educationStage.isEducation()) {
            return ongoingEducation(campaign, person, academy, ageBypass, resources);
        }

        // if education has concluded and the journey home hasn't started, we begin the journey
        if ((educationStage.isGraduating()) || (educationStage.isDroppingOut())) {
            beginJourneyHome(campaign, person, academy, resources);
            return false;
        }

        // if we reach this point it means Person is already in transit, so we continue their journey
        if (educationStage.isJourneyFromCampus()) {
            processJourneyHome(campaign, person);
            return false;
        }

        logger.error("Failed to process education stage: {}", educationStage);
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
        person.incrementEduDaysOfTravel();

        // has Person just arrived?
        if (person.getEduDaysOfTravel() >= person.getEduJourneyTime()) {
            campaign.addReport(String.format(resources.getString("arrived.text"), person.getHyperlinkedFullTitle()));
            person.setEduEducationStage(EducationStage.EDUCATION);
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
        int daysOfEducation = person.getEduEducationTime();

        if (academy.isPrepSchool()) {
            if ((person.getAge(campaign.getLocalDate()) >= academy.getAgeMax()) || (ageBypass)) {
                graduationPicker(campaign, person, academy, resources);

                person.setEduEducationStage(EducationStage.GRADUATING);

                return true;
            }

            checkForEvents(campaign, person, academy, resources);

            return false;
        } else {
            person.setEduEducationTime(daysOfEducation - 1);

            checkForEvents(campaign, person, academy, resources);

            // we use 2 as that would be the value prior the day's decrement
            if (daysOfEducation < 2) {
                if (graduationPicker(campaign, person, academy, resources)) {
                    return person.getEduEducationStage().isGraduating();
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Checks for any events based on the current date of the campaign.
     *
     * @param campaign   the campaign to check events for
     * @param person     the person involved in the campaign
     * @param academy    the academy related to the campaign
     * @param resources  the resource bundle for localized messages
     */
    private static void checkForEvents(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
            processNewWeekChecks(campaign, academy, person, resources);
        }

        if (campaign.getLocalDate().getDayOfYear() == 1) {
            processNewYearChecks(campaign, academy, person, resources);
        }
    }

    /**
     * Picks the appropriate graduation method based on the given campaign, person, academy, and resources.
     *
     * @param campaign the campaign to use for calculations
     * @param person the person to determine graduation for
     * @param academy the academy to determine graduation from
     * @param resources the resources to use for graduation
     * @return true, if the person successfully graduates, otherwise, false
     */
    private static boolean graduationPicker(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        if (academy.isPrepSchool()) {
            graduateChild(campaign, person, academy, resources);
            return true;
        } else {
            return graduateAdult(campaign, person, academy, resources);
        }
    }

    /**
     * Begins the journey for a person to return home from an academy.
     *
     * @param campaign   the current campaign
     * @param person     the person returning from the academy
     * @param academy    the academy being attended
     * @param resources  the resource bundle containing localized strings
     */
    private static void beginJourneyHome(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        // if the student is being homeschooled, they skip the journey home.
        if (academy.isHomeSchool()) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);

            return;
        }

        int travelTime = Math.max(2, campaign.getSimplifiedTravelTime(campaign.getSystemById(person.getEduAcademySystem())));

        campaign.addReport(String.format(resources.getString("returningFromSchool.text"), person.getHyperlinkedFullTitle(), travelTime));

        person.setEduJourneyTime(travelTime);
        person.setEduDaysOfTravel(0);
        person.setEduEducationStage(EducationStage.JOURNEY_FROM_CAMPUS);
    }

    /**
     * Processes the journey home for a person.
     *
     * @param campaign         the campaign the person is in
     * @param person           the person whose journey home is being processed
     */
    private static void processJourneyHome(Campaign campaign, Person person) {
        // has the journey time changed?
        int travelTime = Math.max(2, campaign.getSimplifiedTravelTime(campaign.getSystemById(person.getEduAcademySystem())));

        // if so, update the journey time
        if (travelTime != person.getEduJourneyTime()) {
            person.setEduJourneyTime(travelTime);
        }

        person.incrementEduDaysOfTravel();

        // has the person arrived home?
        if (person.getEduDaysOfTravel() >= person.getEduJourneyTime()) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);

            for (UUID tagAlong : person.getEduTagAlongs()) {
                campaign.getPerson(tagAlong).changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
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
     * @param resources       The resource bundle used for localized strings.
     */
    private static void processNewWeekChecks(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if (!academy.isHomeSchool()) {
            // has the system been depopulated? Nominally similar to destruction, but here we use actual system data, so it's more dynamic.
            if (campaign.getSystemById(person.getEduAcademySystem()).getPopulation(campaign.getLocalDate()) == 0) {
                if (checkForAcademyDestruction(campaign, academy, person, resources)) {
                    return;
                }
            }

            // is the academy faction at war with person faction, or the campaign faction?
            if (checkForAcademyFactionConflict(campaign, academy, person, resources)) {
                return;
            }
        }

        // does person want to drop out?
        if (checkForDropout(campaign, academy, person, resources)) {
            return;
        }

        // was there a training accident?
        checkForTrainingAccidents(campaign, academy, person, resources);
    }

    /**
     * Processes the new year checks for a specific person in a campaign.
     *
     * @param campaign        The campaign in which the person is participating.
     * @param academy         The academy where the person is receiving education.
     * @param person          The person whose new month checks need to be processed.
     * @param resources       The resource bundle used for localized strings.
     */
    private static void processNewYearChecks(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if (academy.isHomeSchool()) {
            return;
        }

        // It's unlikely we'll ever get canonical destruction or closure dates for all the academies,
        // so no need to check these more than once a year
        if (campaign.getLocalDate().getDayOfYear() == 1) {
            // time to check whether the academy is still standing.
            if (checkForAcademyDestruction(campaign, academy, person, resources)) {
                return;
            }

            // is the academy still open?
            checkForAcademyClosure(campaign, academy, person, resources);
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
        if (academy.isMilitary()) {
            int militaryDiceSize = campaign.getCampaignOptions().getMilitaryAcademyAccidents();
            int roll;

            if (militaryDiceSize > 1) {
                roll = Compute.randomInt(militaryDiceSize);

                if (academy.isHomeSchool()) {
                    int secondRoll = Compute.randomInt(militaryDiceSize);

                    if (secondRoll < roll) {
                        roll = secondRoll;
                    }
                }
            } else {
                roll = -1;
            }

            if (roll == 0) {
                if ((!person.isChild(campaign.getLocalDate())) || (campaign.getCampaignOptions().isAllAges())) {
                    if (Compute.d6(2) >= 5) {
                        processTrainingInjury(campaign, academy, person, resources);
                    } else {
                        String resultString = String.format(resources.getString("eventTrainingAccidentKilled.text"),
                                "<span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>",
                                "</span>");

                        String reportMessage = String.format(resources.getString("eventTrainingAccident.text"),
                                person.getHyperlinkedFullTitle(),
                                resultString);

                        campaign.addReport(reportMessage);

                        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACCIDENTAL);
                    }
                } else {
                    processTrainingInjury(campaign, academy, person, resources);
                }
            }
        }
    }

    /**
     * Processes a training injury for a student
     *
     * @param campaign   the campaign in which the training injury occurred
     * @param academy    the academy where the person is training
     * @param person     the person who suffered the training injury
     * @param resources  the ResourceBundle for localized strings
     */
    private static void processTrainingInjury(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        int roll = Compute.d6(3);

        String resultString = String.format(resources.getString("eventTrainingAccidentWounded.text"),
                "<span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>",
                "</span>",
                roll);

        String reportMessage = String.format(resources.getString("eventTrainingAccident.text"),
                person.getHyperlinkedFullTitle(),
                resultString);

        campaign.addReport(reportMessage);

        if (!academy.isPrepSchool()) {
            person.setEduEducationTime(person.getEduEducationTime() + roll);
        }
    }

    /**
     * Checks if a person should drop out of an education campaign based on their characteristics and chance factors.
     *
     * @param campaign The campaign the person is enrolled in.
     * @param academy The academy the person is attending.
     * @param person The person being evaluated.
     * @param resources The resource bundle containing localization strings.
     * @return true if the person should drop out, false otherwise.
     */
    private static boolean checkForDropout(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        int roll;
        int diceSize;

        int adultDiceSize = campaign.getCampaignOptions().getAdultDropoutChance();
        int childDiceSize = campaign.getCampaignOptions().getChildrenDropoutChance();

        if (person.isChild(campaign.getLocalDate())) {
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
            if ((diceSize == 1) || (roll == 0)) {
                // we add this limiter to avoid a bad play experience when someone drops out in the final stretch
                if (person.getEduEducationTime() >= 10) {
                    if (academy.isReeducationCamp()) {
                        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MISSING);
                    } else {
                        reportDropOut(campaign, person, academy, resources);

                        ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));
                        person.setEduEducationStage(EducationStage.DROPPING_OUT);
                        addFacultyXp(campaign, person, academy, 0);
                    }
                } else {
                    String reportMessage = String.format(resources.getString("dropOutRejected.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                }

                return true;
            } else if ((roll < (diceSize / 20)) && (!academy.isPrepSchool())) {
                // might as well scare the player
                String reportMessage = String.format(resources.getString("dropOutRejected.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
                return true;
            }
        }
        return false;
    }

    /**
     * This method processes a forced drop out for a person.
     *
     * @param campaign the campaign to add the drop-out report to
     * @param person the person who is forced to drop out
     * @param academy the academy where the person was studying
     */
    public static void processForcedDropOut(Campaign campaign, Person person, Academy academy) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education", MekHQ.getMHQOptions().getLocale());

        reportDropOut(campaign, person, academy, resources);

        ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));
        person.setEduEducationStage(EducationStage.DROPPING_OUT);

        addFacultyXp(campaign, person, academy, 0);
    }

    /**
     * Generates a dropout report and adds it to the campaign's reports.
     *
     * @param campaign  the campaign object to add the report to
     * @param person    the person object representing the dropout
     * @param academy   the academy object the person initially enrolled in
     * @param resources the resource bundle containing localized strings
     */
    private static void reportDropOut(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        String personTitle = person.getHyperlinkedFullTitle();
        String negativeHexColor = MekHQ.getMHQOptions().getFontColorNegativeHexColor();

        String reportText = academy.isHomeSchool() ? "dropOutHomeSchooled.text" : "dropOut.text";

        String coloredOpen = String.format("<span color='%s'>", negativeHexColor);
        String coloredClose = "</span>";

        String report = String.format(resources.getString(reportText), personTitle, coloredOpen, coloredClose);

        campaign.addReport(report);
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
            String reportMessage = String.format(resources.getString("eventWarExpelled.text"),
                    person.getHyperlinkedFullTitle(),
                    "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                    "</span>");

            campaign.addReport(reportMessage);

            person.setEduEducationStage(EducationStage.DROPPING_OUT);

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
     */
    private static void checkForAcademyClosure(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if (campaign.getLocalDate().getYear() >= academy.getClosureYear()) {
            String reportMessage = String.format(resources.getString("eventClosure.text"),
                    person.getHyperlinkedFullTitle(),
                    "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                    "</span>");

            campaign.addReport(reportMessage);
            person.setEduEducationStage(EducationStage.DROPPING_OUT);
        }
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
        // we assume that if the system's population has been depleted, the academy has been destroyed too.
        if ((campaign.getLocalDate().getYear() >= academy.getDestructionYear())
                || (campaign.getSystemById(person.getEduAcademySystem()).getPopulation(campaign.getLocalDate()) == 0)) {
            if ((!person.isChild(campaign.getLocalDate())) || (campaign.getCampaignOptions().isAllAges())) {
                if (Compute.d6(2) >= 5) {
                    String reportMessage = String.format(resources.getString("eventDestruction.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                            "</span>",
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);

                    person.setEduEducationStage(EducationStage.DROPPING_OUT);
                } else {
                    String reportMessage = String.format(resources.getString("eventDestructionKilled.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                            "</span>",
                            "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);

                    person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MISSING);
                }
            } else {
                String reportMessage = String.format(resources.getString("eventDestruction.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                        "</span>",
                        "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);

                person.setEduEducationStage(EducationStage.DROPPING_OUT);
            }
            return true;
        }

        return false;
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

        if (academy.isHomeSchool()) {
            int secondRoll = Compute.randomInt(100);

            if (secondRoll < graduationRoll) {
                graduationRoll = secondRoll;
            }

            if (graduationRoll >= 90) {
                graduationRoll = 89;
            }
        }

        if (campaign.getCampaignOptions().isUseRandomPersonalities()) {
            graduationRoll += person.getIntelligence().getIntelligenceScore();
        }

        // qualification failed
        if (graduationRoll < 5) {
            if (academy.isHomeSchool()) {
                String reportMessage = String.format(resources.getString("graduatedFailedHomeSchooled.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
            } else {
                String reportMessage = String.format(resources.getString("graduatedFailed.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
            }

            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

            improveSkills(campaign, person, academy, false);
            addFacultyXp(campaign, person, academy, 0);

            person.setEduEducationStage(EducationStage.DROPPING_OUT);

            return true;
        }

        // class resits required
        if (graduationRoll < 20) {
            roll = Compute.d6(3);

            String reportMessage = String.format(resources.getString("graduatedClassNeeded.text"),
                    person.getHyperlinkedFullTitle(),
                    "<span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>",
                    "</span>",
                    roll);

            campaign.addReport(reportMessage);

            person.setEduEducationTime(roll);

            return false;
        }

        if (graduationRoll >= 99) {
            if (Compute.d6(1) >= 5) {
                if (academy.isHomeSchool()) {
                    String reportMessage = String.format(resources.getString("graduatedHomeSchooled.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                } else {
                    String reportMessage = String.format(resources.getString("graduatedTop.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>",
                            ' ' + resources.getString(graduationEventPicker()),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                }
            } else {
                if (academy.isHomeSchool()) {
                    String reportMessage = String.format(resources.getString("graduatedHomeSchooled.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                } else {
                    String reportMessage = String.format(resources.getString("graduatedTop.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>",
                            "",
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                }
            }

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedTopLog.text"), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

            processGraduation(campaign, person, academy, 2, resources);

            person.setEduEducationStage(EducationStage.GRADUATING);

            person.changeLoyalty(academy.getDurationDays() / 300);

            return true;
        }

        // graduated with honors
        if (graduationRoll >= 90) {
            if (Compute.d6(1) >= 5) {

                if (academy.isHomeSchool()) {
                    String reportMessage = String.format(resources.getString("graduatedHomeSchooled.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                } else {
                    String reportMessage = String.format(resources.getString("graduatedHonors.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>",
                            ' ' + resources.getString(graduationEventPicker()),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                }
            } else {
                if (academy.isHomeSchool()) {
                    String reportMessage = String.format(resources.getString("graduatedHomeSchooled.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                } else {
                    String reportMessage = String.format(resources.getString("graduatedHonors.text"),
                            person.getHyperlinkedFullTitle(),
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>",
                            "",
                            "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                            "</span>");

                    campaign.addReport(reportMessage);
                }
            }

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedHonorsLog.text"), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

            processGraduation(campaign, person, academy, 1, resources);

            person.setEduEducationStage(EducationStage.GRADUATING);

            person.changeLoyalty(academy.getDurationDays() / 300);

            return true;
        }

        // default graduation
        if (Compute.d6(1) >= 5) {
            if (academy.isHomeSchool()) {
                String reportMessage = String.format(resources.getString("graduatedHomeSchooled.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
            } else {
                String reportMessage = String.format(resources.getString("graduated.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                        "</span>",
                        ' ' + resources.getString(graduationEventPicker()));

                campaign.addReport(reportMessage);
            }
        } else {
            if (academy.isHomeSchool()) {
                String reportMessage = String.format(resources.getString("graduatedHomeSchooled.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
            } else {
                String reportMessage = String.format(resources.getString("graduated.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                        "</span>",
                        "");

                campaign.addReport(reportMessage);
            }
        }

        ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

        processGraduation(campaign, person, academy, 0, resources);

        person.setEduEducationStage(EducationStage.GRADUATING);

        person.changeLoyalty(academy.getDurationDays() / 300);

        return true;
    }

    /**
     * This method generates a report for individuals who have completed either a Master's or Doctorate degree.
     *
     * @param campaign       the campaign to add the report to
     * @param person         the person who completed the degree
     * @param education the education level taught by the academy
     * @param resources      the resource bundle containing localized strings
     */
    private static void reportMastersOrDoctorateGain(Campaign campaign, Person person, Academy academy,
                                                     int education, ResourceBundle resources) {
        EducationLevel educationLevel = EducationLevel.parseFromInt(education);

        String qualification = academy.getQualifications().get(person.getEduCourseIndex());
        String personName = person.getHyperlinkedFullTitle();

        if (educationLevel.isPostGraduate()) {
            ServiceLogger.eduGraduatedMasters(
                    person,
                    campaign.getLocalDate(),
                    person.getEduAcademyName(),
                    qualification
            );

            generatePostGradGraduationReport(
                    campaign,
                    personName,
                    resources.getString("graduatedMasters.text"),
                    qualification,
                    resources
            );

        } else if (educationLevel.isDoctorate()) {
            ServiceLogger.eduGraduatedDoctorate(
                    person,
                    campaign.getLocalDate(),
                    person.getEduAcademyName(),
                    qualification
            );

            person.setPreNominal("Dr");

            generatePostGradGraduationReport(
                    campaign,
                    personName,
                    resources.getString("graduatedDoctorate.text"),
                    qualification,
                    resources
            );
        }
    }

    /**
     * Generates a post-graduate graduation report and publishes it to the daily report.
     *
     * @param campaign The campaign to which the report will be added.
     * @param personName The person's name (normally hyperlinked full title)
     * @param graduationText The text to be included in the graduation report.
     * @param qualification The qualification just completed
     */
    private static void generatePostGradGraduationReport(Campaign campaign, String personName,
                                                         String graduationText, String qualification, ResourceBundle resources) {
        campaign.addReport(String.format(resources.getString("graduatedPostGradReport.text"),
                personName,
                "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                graduationText,
                "</span>",
                qualification));
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

        if (academy.isHomeSchool()) {
            int secondRoll = Compute.randomInt(100);

            if (secondRoll < graduationRoll) {
                graduationRoll = secondRoll;
            }
        }

        if (campaign.getCampaignOptions().isUseRandomPersonalities()) {
            graduationRoll += Intelligence.parseToInt(person.getIntelligence()) - 12;
        }

        if (graduationRoll < 30) {
            if (academy.isHomeSchool()) {
                String reportMessage = String.format(resources.getString("graduatedBarelyHomeSchooled.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
            } else {
                String reportMessage = String.format(resources.getString("graduatedBarely.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
            }
        } else {
            if (academy.isHomeSchool()) {
                String reportMessage = String.format(resources.getString("graduatedHomeSchooled.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
            } else {
                String reportMessage = String.format(resources.getString("graduatedChild.text"),
                        person.getHyperlinkedFullTitle(),
                        "<span color='" + MekHQ.getMHQOptions().getFontColorPositiveHexColor() + "'>",
                        "</span>");

                campaign.addReport(reportMessage);
            }
        }

        ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

        processGraduation(campaign, person, academy, 0, resources);
    }

    /**
     * Adjusts the loyalty of a person based on a random roll.
     * If the roll is 1, the loyalty is decreased by 1.
     * If the roll is 4 or greater, the loyalty is increased by 1.
     *
     * @param person the person whose loyalty is to be adjusted
     */
    private static void adjustLoyalty(Person person) {
        int roll = Compute.d6(1);

        if (roll == 1) {
            person.setLoyalty(person.getLoyalty() - 1);
        } else if (roll >= 4) {
            person.setLoyalty(person.getLoyalty() + 1);
        }
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

        addFacultyXp(campaign, person, academy, bonusCount);

        if ((campaign.getCampaignOptions().isEnableBonuses()) && (bonusCount > 0)) {
            addBonus(campaign, person, academy, bonusCount, resources);
        }

        int educationLevel = academy.getEducationLevel(person);

        if (EducationLevel.parseToInt(person.getEduHighestEducation()) < educationLevel) {
            person.setEduHighestEducation(EducationLevel.parseFromInt(educationLevel));
        }

        if (academy.isReeducationCamp()) {
            if (campaign.getCampaignOptions().isUseReeducationCamps()) {
                person.setOriginFaction(campaign.getFaction());
            }

            // brainwashed personnel should have higher than average loyalty, so they roll 4d6 and drop the lowest roll
            List<Integer> rolls = new ArrayList<>();

            for (int roll = 0; roll < 4; roll++) {
                rolls.add(Compute.d6(1));
            }

            Collections.sort(rolls);

            person.setLoyalty(rolls.get(1) + rolls.get(2) + rolls.get(3));
        } else {
            adjustLoyalty(person);
        }

        if (!academy.isMilitary()) {
            reportMastersOrDoctorateGain(campaign, person, academy, educationLevel, resources);
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
        String[] curriculum = Arrays.stream(academy.getCurriculums().get(person.getEduCourseIndex()).split(","))
                .map(String::trim)
                .toArray(String[]::new);

        int educationLevel = Math.min(Math.max(academy.getEducationLevel(person) + academy.getBaseAcademicSkillLevel(), 0), 5);

        if (!isGraduating) {
            educationLevel--;

            if (educationLevel < 0) {
                return;
            }
        }

        for (String skill : curriculum) {
            if (skill.equalsIgnoreCase("none")) {
                return;
            }

            if (skill.equalsIgnoreCase("xp")) {
                person.awardXP(campaign, campaign.getCampaignOptions().getCurriculumXpRate());
            } else {
                updateSkill(person, educationLevel, skill);
            }
        }
    }

    /**
     * Updates the skill level of a person
     *
     * @param person         the person whose skill level should be updated
     * @param educationLevel the new education level for the skill
     * @param skill          the skill to be updated
     */
    private static void updateSkill(Person person, int educationLevel, String skill) {
        String skillParsed = Academy.skillParser(skill);

        if (person.hasSkill(skillParsed)) {
            int bonus = person.getSkill(skillParsed).getBonus();
            adjustSkillLevel(person, skillParsed, educationLevel, bonus);
        } else {
            person.addSkill(skillParsed, 0, 0);
            adjustSkillLevel(person, skillParsed, educationLevel, 0);
        }
    }

    /**
     * Adjusts the skill level of a person until the target level is reached.
     *
     * @param person        The person whose skill level needs adjustment.
     * @param skillParsed   The name of the skill to adjust.
     * @param targetLevel   The desired target level of the skill.
     * @param bonus         The bonus to apply when increasing the skill level.
     */
    private static void adjustSkillLevel(Person person, String skillParsed, int targetLevel, int bonus) {
        int skillLevel = 0;

        while (person.getSkill(skillParsed).getExperienceLevel() < targetLevel) {
            person.addSkill(skillParsed, skillLevel++, bonus);
        }
    }

    /**
     * Adds faculty XP to a person based on faculty skill and academy duration.
     *
     * @param campaign the campaign the person is participating in
     * @param person the person receiving the bonus XP
     * @param academy the academy attended by the person
     * @param bonusCount the number of extra bonus XP to be added (based on graduation level)
     */
    private static void addFacultyXp(Campaign campaign, Person person, Academy academy, Integer bonusCount) {
        int academyDuration;

        if (academy.isPrepSchool()) {
            academyDuration = (person.getAge(campaign.getLocalDate()) - academy.getAgeMin()) * 300;
        } else {
            academyDuration = academy.getDurationDays() - person.getEduEducationTime();
        }

        double bonusPercentage = (double) bonusCount / 5;

        if (EducationLevel.parseToInt(person.getEduHighestEducation()) < academy.getEducationLevel(person)) {
            int xpRate = Math.max(1, (12 - academy.getFacultySkill()) * (academyDuration / 600));

            xpRate *= campaign.getCampaignOptions().getFacultyXpRate();

            int bonusAmount = (int) Math.max(bonusCount, xpRate * bonusPercentage);
            person.awardXP(campaign, xpRate + bonusAmount);
        } else {
            int bonusAmount = (int) Math.max(bonusCount, 1 * bonusPercentage);
            person.awardXP(campaign, 1 + bonusAmount);
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
                .toList();

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
                   person.awardXP(campaign, campaign.getCampaignOptions().getCurriculumXpRate());

                    campaign.addReport(String.format(resources.getString("bonusXp.text"),
                            person.getFirstName(),
                            campaign.getCampaignOptions().getCurriculumXpRate()));
                }
            } catch (Exception e) {
                // if we get this, it means the 'skill' was XP or None
                if (curriculum.get(roll).equalsIgnoreCase("xp")) {
                    person.awardXP(campaign, campaign.getCampaignOptions().getCurriculumXpRate());

                    campaign.addReport(String.format(resources.getString("bonusXp.text"),
                            person.getFirstName(),
                            campaign.getCampaignOptions().getCurriculumXpRate()));
                }
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
        Collections.shuffle(graduationEventTable);

        return graduationEventTable.get(Compute.randomInt(graduationEventTable.size()));
    }

    /**
     * Returns a list of graduation event table entries. The list contains various graduation event table entries such as addresses, admirer
     * messages, events, gifts, pet costumes, recognitions, speeches, and surprises.
     *
     * @return a list of graduation event table entries
     */
    private static List<String> graduationEventTable() {
        return Arrays.asList(
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
                "surpriseStandard.text",
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
