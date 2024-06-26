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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.enums.education.EducationStage;
import org.apache.logging.log4j.LogManager;

import java.time.DayOfWeek;
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

        campaign.addReport(String.format(resources.getString("offToSchool.text"),
                    person.getFullName(),
                    person.getEduAcademyName(),
                    person.getEduJourneyTime()));
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

        person.setEduEducationStage(EducationStage.JOURNEY_TO_CAMPUS);
        person.setEduAcademySet(academy.getSet());
        person.setEduAcademyNameInSet(academy.getName());
        person.setEduEducationTime(academy.getDurationDays());
        person.setEduAcademyFaction(faction);
        person.setEduCourseIndex(courseIndex);

        if (academy.isLocal()) {
            person.setEduJourneyTime(2);
            person.setEduAcademySystem(campaign.getCurrentSystem().getId());
        } else {
            person.setEduJourneyTime(campaign.getSimplifiedTravelTime(campaign.getSystemById(campus)));
            person.setEduAcademySystem(campus);
        }

        // this should already be 0, but we reset it just in case
        person.setEduDaysOfTravel(0);

        // if the academy is Local, we need to generate a name, otherwise we use the listed name
        if (academy.isLocal()) {
            person.setEduAcademyName(generateName(academy, campus));
        } else {
            person.setEduAcademyName(person.getEduAcademyNameInSet() + " (" + campaign.getSystemById(campus).getName(campaign.getLocalDate()) + ')');
        }

        // we have this all the way at the bottom as a bit of insurance.
        // when troubleshooting, if the log isn't getting entered, we know something went wrong when enrolling.
        ServiceLogger.eduEnrolled(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));
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
     * Generates a random educational institution type from the provided ResourceBundle.
     *
     * @param resources the ResourceBundle containing the localized strings for the educational institution types
     * @return a randomly selected educational institution type
     * @throws IllegalStateException if the generated roll is unexpected
     */
    private static String generateTypeChild(ResourceBundle resources) {
        switch (Compute.d6(1)) {
            case 1:
                return resources.getString("typeAcademy.text");
            case 2:
                return resources.getString("typePreparatorySchool.text");
            case 3:
                return resources.getString("typeInstitute.text");
            case 4:
                return resources.getString("typeSchoolBoarding.text");
            case 5:
                return resources.getString("typeFinishingSchool.text");
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

        EducationStage educationStage = person.getEduEducationStage();

        // is person in transit to the institution?
        if (educationStage == EducationStage.JOURNEY_TO_CAMPUS) {
            journeyToAcademy(campaign, person, resources);
            return false;
        }

        // is the person on campus and undergoing education
        if (educationStage == EducationStage.EDUCATION) {
            return ongoingEducation(campaign, person, academy, ageBypass, resources);
        }

        // if education has concluded and the journey home hasn't started, we begin the journey
        if ((educationStage == EducationStage.GRADUATING) || (educationStage == EducationStage.DROPPING_OUT)) {
            beginJourneyHome(campaign, person, resources);
            return false;
        }

        // if we reach this point it means Person is already in transit, so we continue their journey
        if (educationStage == EducationStage.JOURNEY_FROM_CAMPUS) {
            processJourneyHome(campaign, person);
            return false;
        }

        LogManager.getLogger().error("Failed to process education stage: {}", educationStage);
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
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("arrived.text"));
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

                if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                    processNewWeekChecks(campaign, academy, person, resources);
                }

                return true;
            }

            if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                processNewWeekChecks(campaign, academy, person, resources);
            }

            return false;
        } else {
            person.setEduEducationTime(daysOfEducation - 1);

            if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                processNewWeekChecks(campaign, academy, person, resources);
            }

            // we use 2 as that would be the value prior the day's decrement
            if (daysOfEducation < 2) {
                if (graduationPicker(campaign, person, academy, resources)) {
                    person.setEduEducationStage(EducationStage.GRADUATING);
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
     * @return true, if the person successfully graduates, otherwise, false
     */
    private static boolean graduationPicker(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        if (academy.isPrepSchool()) {
            graduateChild(campaign, person, academy, resources);
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
     * @param resources  the resource bundle containing localized strings
     */
    private static void beginJourneyHome(Campaign campaign, Person person, ResourceBundle resources) {
        int travelTime = Math.max(2, campaign.getSimplifiedTravelTime(campaign.getSystemById(person.getEduAcademySystem())));

        campaign.addReport(person.getHyperlinkedName() + ' '
                + String.format(resources.getString("returningFromSchool.text"), travelTime));

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
        if ((campaign.getCampaignOptions().isEnableRandomXp()) && (campaign.getLocalDate().getDayOfMonth() == 1)) {
            if (Compute.d6(2) >= academy.getFacultySkill()) {
                person.awardXP(campaign, campaign.getCampaignOptions().getRandomXpRate());
            }
        }

        if (person.getEduEducationStage() != EducationStage.GRADUATING) {
            // It's unlikely we'll ever get canonical destruction or closure dates for all the academies,
            // so no need to check these more than once a year
            if (campaign.getLocalDate().getDayOfYear() == 1) {
                // time to check whether the academy is still standing.
                if (checkForAcademyDestruction(campaign, academy, person, resources)) {
                    return;
                }

                // is the academy still open?
                if (checkForAcademyClosure(campaign, academy, person, resources)) {
                    return;
                }
            }

            // is the academy faction at war with person faction, or the campaign faction?
            if (checkForAcademyFactionConflict(campaign, academy, person, resources)) {
                return;
            }

            // does person want to drop out?
            if (checkForDropout(campaign, academy, person, resources)) {
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
        if (academy.isMilitary()) {
            int militaryDiceSize = campaign.getCampaignOptions().getMilitaryAcademyAccidents();
            int roll;

            if (militaryDiceSize > 1) {
                roll = Compute.randomInt(militaryDiceSize);
            } else {
                roll = -1;
            }

            if (roll == 0) {
                if ((!person.isChild(campaign.getLocalDate())) || (campaign.getCampaignOptions().isAllAges())) {
                    if (Compute.d6(2) >= 5) {
                        roll = Compute.d6(3);

                        campaign.addReport(person.getHyperlinkedName() + ' '
                                + String.format(resources.getString("eventTrainingAccident.text"), roll));

                        if (!academy.isPrepSchool()) {
                            person.setEduEducationTime(person.getEduEducationTime() + roll);
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
                        person.setEduEducationTime(person.getEduEducationTime() + roll);
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
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOut.text"));
                    ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));
                    person.setEduEducationStage(EducationStage.DROPPING_OUT);
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
     * @return true if the academy has been closed, false otherwise
     */
    private static boolean checkForAcademyClosure(Campaign campaign, Academy academy, Person person, ResourceBundle resources) {
        if (campaign.getLocalDate().getYear() >= academy.getClosureYear()) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventClosure.text"));
            person.setEduEducationStage(EducationStage.DROPPING_OUT);

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
                    person.setEduEducationStage(EducationStage.DROPPING_OUT);
                } else {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventDestructionKilled.text"));
                    person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MISSING);
                }
            } else {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventDestruction.text"));
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

        // qualification failed
        if (graduationRoll < 5) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedFailed.text"));
            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

            improveSkills(campaign, person, academy, false);

            return true;
        }

        // class resits required
        if (graduationRoll < 20) {
            roll = Compute.d6(3);
            campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedClassNeeded.text"), roll));

            person.setEduEducationTime(roll);

            return false;
        }

        if (graduationRoll == 99) {
            if (Compute.d6(1) > 5) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTop.text"),
                        ' ' + resources.getString(graduationEventPicker())));
            } else {
                campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedTop.text"), ""));
            }

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedTopLog.text"), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

            processGraduation(campaign, person, academy, 2, resources);

            if (!academy.isMilitary()) {
                reportMastersOrDoctorateGain(campaign, person, academy, resources);
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
                    resources.getString("graduatedHonorsLog.text"), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

            processGraduation(campaign, person, academy, 1, resources);

            if (!academy.isMilitary()) {
                reportMastersOrDoctorateGain(campaign, person, academy, resources);
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

        ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

        processGraduation(campaign, person, academy, 0, resources);

        if (!academy.isMilitary()) {
            reportMastersOrDoctorateGain(campaign, person, academy, resources);
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
    private static void reportMastersOrDoctorateGain(Campaign campaign, Person person, Academy academy, ResourceBundle resources) {
        if (person.getEduHighestEducation().isPostGraduate()) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedMasters.text"));

            ServiceLogger.eduGraduatedMasters(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

        } else if (person.getEduHighestEducation().isDoctorate()) {
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedDoctorate.text"));

            ServiceLogger.eduGraduatedDoctorate(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

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
            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName(), academy.getQualifications().get(person.getEduCourseIndex()));

            improveSkills(campaign, person, academy, false);

            return;
        }

        // default graduation
        campaign.addReport(person.getHyperlinkedName() + ' ' + String.format(resources.getString("graduatedChild.text"), person.getEduAcademyName()));

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

        if ((campaign.getCampaignOptions().isEnableBonuses()) && (bonusCount > 0)) {
            addBonus(campaign, person, academy, bonusCount, resources);
        }

        int educationLevel = academy.getEducationLevel(person);

        if (EducationLevel.parseToInt(person.getEduHighestEducation()) < educationLevel) {
            LogManager.getLogger().info(educationLevel);
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
            if (skill.equalsIgnoreCase("xp")) {
                person.awardXP(campaign, educationLevel * campaign.getCampaignOptions().getCurriculumXpRate());
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
                   person.awardXP(campaign, campaign.getCampaignOptions().getCurriculumXpRate());

                    campaign.addReport(String.format(resources.getString("bonusXp.text"),
                            person.getFirstName(), campaign.getCampaignOptions().getCurriculumXpRate()));
                }
            } catch (Exception e) {
                // if we get this, it means the 'skill' was XP
                person.awardXP(campaign, campaign.getCampaignOptions().getCurriculumXpRate());

                campaign.addReport(String.format(resources.getString("bonusXp.text"),
                        person.getFirstName(), campaign.getCampaignOptions().getCurriculumXpRate()));
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
