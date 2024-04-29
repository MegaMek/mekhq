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
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.apache.logging.log4j.LogManager;

import java.time.DayOfWeek;
import java.util.*;

/**
 * Controls the education module for a person in a campaign.
 */
public class EducationController {
    /**
     * Begins the education process for a person.
     *
     * @param campaign the campaign in which the education is taking place
     * @param person the person receiving the education
     * @param academyName the name of the academy to enroll in
     * @param courseIndex the index of the curriculum to enroll in
     */
    public static void beginEducation(Campaign campaign, Person person, String academyName, int courseIndex) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

        Academy academy = getAcademy(person);

        if (academy == null) {
            LogManager.getLogger().error("No academy found with name {}", academyName);
            return;
        }

        int tuition = academy.getTuitionAdjusted(academy.getEducationLevelMin(), getEducationLevel(person, academy));

        // check there is enough money in the campaign & if so, make a debit
        if (getBalance(campaign) < tuition) {
            campaign.addReport(resources.getString("insufficientFunds.text").replaceAll("0",
                    person.getHyperlinkedFullTitle()));
            return;
        } else {
            campaign.getFinances().debit(TransactionType.EDUCATION, campaign.getLocalDate(), Money.of(tuition),
                    resources.getString("payment.text").replaceAll("0", person.getFullName()));
        }

        // with the checks done, we can enroll Person
        person.setEduCourseIndex(courseIndex);
        enrollPerson(campaign, person, academy);

        campaign.addReport(resources.getString("offToSchool.text")
                .replaceAll("0", person.getFullName())
                .replaceAll("1", person.getEduAcademyName())
                .replaceAll("2", String.valueOf(person.getEduDaysOfTravelToAcademy())));
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
     * @param person The person whose education level needs to be determined.
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
     * If the academy is local, it generates a name for the academy.
     *
     * @param campaign The campaign the person is from.
     * @param person The person to be enrolled.
     * @param academy The academy to which the person is being enrolled.
     */
    public static void enrollPerson(Campaign campaign, Person person, Academy academy) {
        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.STUDENT);

        person.setEduAcademySet(academy.getSet());
        person.setEduAcademyNameInSet(academy.getName());
        person.setEduDaysOfEducation(academy.getDurationDays());

        // the following two blocks of code need to be processed in this order so that generateName()
        // can make useful names
        int travelTime = 2;
        String campus = campaign.getCurrentSystem().getName(campaign.getLocalDate());

        if (!academy.isLocal()) {
            for (String system : academy.getLocationSystems()) {
                int travelTimeNew = simplifiedTravelTime(campaign, null, system);

                if (travelTimeNew > travelTime) {
                    travelTime = travelTimeNew;
                    campus = system;
                }
            }
        }

        person.setEduDaysOfTravelToAcademy(travelTime);
        person.setEduAcademyNameInSet(academy.getName());
        person.setEduAcademySystem(campus);

        // if the academy is local, we need to generate a name, otherwise we use the listed name
        if (academy.isLocal()) {
            person.setEduAcademyName(generateName(academy.isMilitary(), campus));
        } else {
            person.setEduAcademyName(person.getEduAcademyNameInSet());
        }

        // we have this all the way at the bottom as a bit of insurance.
        // if the log isn't getting entered, we know something went wrong when troubleshooting.
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
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

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
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

        for (Person person : campaign.getStudents()) {
            // is person in transit to the institution?
            int daysOfTravelTo = person.getEduDaysOfTravelToAcademy();

            if (daysOfTravelTo > 0) {
                person.setEduDaysOfTravelToAcademy(daysOfTravelTo - 1);

                // has Person just arrived?
                if ((daysOfTravelTo - 1) == 0) {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("arrived.text"));
                }

                continue;
            }

            // is person on campus and undergoing education?
            int daysOfEducation = person.getEduDaysOfEducation();

            if (daysOfEducation > 0) {
                person.setEduDaysOfEducation(daysOfEducation - 1);

                Academy academy = getAcademy(person);

                if ((daysOfEducation - 1) == 0) {
                    graduatePerson(campaign, person, academy);
                }

                if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                    processNewWeekChecks(campaign, academy, person, daysOfEducation, resources);
                }
                continue;
            }

            // if education has concluded and the journey home hasn't started, we begin the journey
            int daysOfTravelFrom = person.getEduDaysOfTravelFromAcademy();

            if ((daysOfEducation == 0) && (daysOfTravelFrom == 0)) {
                int travelTime = simplifiedTravelTime(campaign, person, null);

                // We use a minimum of 2 days travel to avoid awkward grammar in the report. This can be hand waved
                // as being the time it takes for Person to get from campus and recover from their education.
                if (travelTime < 2) {
                    travelTime = 2;
                }

                person.setEduDaysOfTravelFromAcademy(travelTime);

                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("returningFromSchool.text"));

                continue;
            }

            // if we reach this point it means Person is already in transit.
            if ((daysOfEducation == 0) && (daysOfTravelFrom > 0)) {
                int travelTime = simplifiedTravelTime(campaign, person, null);

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
    }

    /**
     * Returns the academy where the given person is enrolled.
     *
     * @param person the person whose academy is being retrieved
     * @return the academy where the person is enrolled,
     *         or null if the person is not enrolled in any academy
     */
    private static Academy getAcademy(Person person) {
        List<String> setNames = AcademyFactory.getInstance().getAllSetNames();

        return setNames.stream()
                .filter(setName -> setName.equalsIgnoreCase(person.getEduAcademySet()))
                .map(setName -> AcademyFactory.getInstance()
                        .getAllAcademiesForSet(setName))
                .flatMap(Collection::stream)
                .filter(academy -> String.valueOf(academy.getName())
                        .equals(person.getEduAcademyNameInSet()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Processes the new week checks for a specific person in a campaign.
     *
     * @param campaign       The campaign in which the person is participating.
     * @param academy        The academy where the person is receiving education.
     * @param person         The person whose new week checks need to be processed.
     * @param daysOfEducation The number of days the person has received education.
     * @param resources      The resource bundle used for obtaining localized strings.
     */
    private static void processNewWeekChecks(Campaign campaign, Academy academy, Person person, int daysOfEducation, ResourceBundle resources) {
        if (Compute.d6(2) >= academy.getFacultySkill()) {
            person.awardXP(campaign, 1);
        }

        // time to check whether the academy is still standing
        if (campaign.getLocalDate().getYear() >= academy.getDestructionYear()) {
            if (Compute.d6(2) >= 5) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventDestruction.text"));
                person.setEduDaysOfEducation(0);
            } else {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventDestructionKilled.text"));
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.HOMICIDE);
            }
        }

        List<String> factions = campaign.getSystemByName(person.getEduAcademySystem()).getFactions(campaign.getLocalDate());

        for (String faction : factions) {
            if (RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(person.getOriginFaction(), Factions.getInstance().getFaction(faction), campaign.getLocalDate())) {
                if (Compute.d6(2) >= 5) {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventWar.text"));
                    person.setEduDaysOfEducation(person.getEduDaysOfEducation() + Compute.d6(1));
                } else {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("eventWarExpelled.text"));
                    person.setEduDaysOfEducation(0);
                }
            }
        }
        int roll = Compute.randomInt(100);

        // we add this limiter to avoid a bad play experience when someone drops out in the final stretch
        if (roll == 0) {
            if (daysOfEducation >= 10) {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOut.text"));

                ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName());

                person.setEduDaysOfEducation(0);
            } else {
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));
            }
        } else if (roll < 5) {
            // might as well scare the player a little
            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));
        }
    }

    /**
     * Graduates a person from an academy.
     *
     * @param campaign the campaign the person belongs to
     * @param person the person being graduated
     * @param academy the academy from which the person is being graduated
     */
    private static void graduatePerson(Campaign campaign, Person person, Academy academy) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

        int graduationRoll = Compute.randomInt(100);
        int roll;

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

        // graduated top of the class
        if (graduationRoll == 99) {
            roll = Compute.d6(1);
            person.awardXP(campaign, roll + 3);

            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTop.text")
                    .replace("0", graduationEventPicker()));

            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(),
                    resources.getString("graduatedTopLog.text"), person.getEduAcademyName());

            improveSkills(person, academy, true);
            addBonus(campaign, person, academy, 2);

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
            addBonus(campaign, person, academy, 1);

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
     * Improves the skills of a given person based on the curriculum of the course they are attending.
     *
     * @param person The Person whose skills are being improved.
     * @param academy The academy the person is attending.
     * @param isGraduating A boolean value indicating whether the person is graduating from the academy or not.
     */
    private static void improveSkills(Person person, Academy academy, Boolean isGraduating) {
        String[] curriculum = academy.getCurriculums().get(person.getEduCourseIndex())
                .replaceAll(", ", ",").split(",");

        int education = academy.getAcademicBaseSkillLevel() + getEducationLevel(person, academy);

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
     */
    private static void addBonus(Campaign campaign, Person person, Academy academy, int bonusCount) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

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
     * Calculates the simplified travel time for a person in a campaign.
     * The simplified travel time is calculated based on the number of jumps between the person's education academy's
     * parent system and the current system in the campaign.
     *
     * @param campaign the campaign in which the person is traveling
     * @param person the person who is traveling, can be null if location is provided
     * @param location the location system of the academy can be null if a person is provided
     * @return the simplified travel time in days
     */
    public static int simplifiedTravelTime(Campaign campaign, @Nullable Person person, @Nullable String location) {
        if (location == null) {
            location = person.getEduAcademySystem();
        }

        int jumps = campaign.calculateJumpPath(campaign.getSystemByName(location),
                campaign.getCurrentSystem()).getJumps();

        return jumps * 7;
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
