package mekhq.campaign.personnel.education;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controls the education module for a person in a campaign.
 */
public class EducationController {
    /**
     * Controls the education module for a person in a campaign.
     *
     * @param campaign the campaign to which the person belongs
     * @param person the person whose education is being controlled
     * @param origin the origin of the education (e.g., "Basic Training", "Local Academy", etc.)
     * @throws IllegalStateException if the origin value is unexpected
     */
    public static void educationController(Campaign campaign, Person person, String academySet, String academyName, int courseIndex) {

    }

    /**
     * Returns the balance of the given campaign.
     *
     * @param campaign the campaign for which to get the balance
     * @return the balance of the campaign
     */
    public static int getBalance(Campaign campaign) {
        String balance = String.valueOf(campaign.getFinances().getBalance()).replaceAll("CSB ", "");

        if (balance.contains(".")) {
            balance = balance.substring(0, balance.indexOf('.'));
        }

        return Integer.parseInt(balance);
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

                // has Person just arrived
                if ((daysOfTravelTo - 1) == 0) {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("arrived.text"));
                }

                continue;
            }

            // is person on campus and undergoing education?
            int daysOfEducation = person.getEduDaysOfEducation();

            if (daysOfEducation > 0) {
                person.setEduDaysOfEducation(daysOfEducation - 1);

                if ((daysOfEducation - 1) == 0) {
                    int roll;

                    switch (Compute.d6(2)) {
                        case 2:
                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedFailed.text"));

                            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName());
                            break;
                        case 3:
                            roll = Compute.d6(3);
                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedClassNeeded.text")
                                    .replace("0", String.valueOf(roll)));
                            person.setEduDaysOfEducation(roll);
                            break;
                        case 11:
                            roll = Compute.d6(1);
                            person.awardXP(campaign, roll);

                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedHonors.text")
                                    .replace("0", graduationEventPicker())
                                    .replace("1", String.valueOf(roll)));

                            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(), resources.getString("graduatedHonorsLog.text"),
                                    person.getEduAcademyName());
                            break;
                        case 12:
                            roll = Compute.d6(1);
                            person.awardXP(campaign, roll + 3);

                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduatedTop.text")
                                    .replace("0", graduationEventPicker())
                                    .replace("1", String.valueOf(roll + 3)));

                            ServiceLogger.eduGraduatedPlus(person, campaign.getLocalDate(), resources.getString("graduatedTopLog.text"),
                                    person.getEduAcademyName());
                            break;
                        default:
                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("graduated.text")
                                    .replace("0", graduationEventPicker()));

                            ServiceLogger.eduGraduated(person, campaign.getLocalDate(), person.getEduAcademyName());
                    }
                }

                if (campaign.getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY) {
                    int roll = Compute.d6(2);

                    if (roll > 8) {
                        person.awardXP(campaign, 1);
                    }

                    roll = Compute.randomInt(100);

                    // we add this limiter to avoid a bad play experience when someone drops out in the final stretch
                    if (roll == 0) {
                        if (daysOfEducation >= 10) {
                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOut.text"));

                            ServiceLogger.eduFailed(person, campaign.getLocalDate(), person.getEduAcademyName());

                            person.setEduDaysOfEducation(0);
                            continue;
                        } else {
                            campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));
                        }
                    } else if (roll < 5) {
                        // might as well scare the player a little
                        campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("dropOutRejected.text"));
                    }
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

                if ((daysOfTravelFrom - 1) == 0) {
                    campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("returned.text"));
                    person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
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
     * @param location the location system of the academy, can be null if person is provided
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
