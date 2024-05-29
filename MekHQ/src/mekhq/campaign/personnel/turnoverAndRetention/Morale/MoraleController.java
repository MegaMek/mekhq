package mekhq.campaign.personnel.turnoverAndRetention.Morale;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.ForceReliabilityMethod;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;

import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

import static mekhq.campaign.personnel.turnoverAndRetention.Morale.Desertion.processDesertion;
import static mekhq.campaign.personnel.turnoverAndRetention.Morale.Mutiny.processMutiny;

public class MoraleController {
    /**
     * This method returns the Morale level as a string based on the campaign's current morale.
     *
     * @return The Morale level as a string.
     * @throws IllegalStateException if the value of 'Morale' is unexpected.
     */
    public static String getMoraleLevel(Campaign campaign) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        int morale = campaign.getMorale() / 10;

        switch (morale) {
            case 1:
                return resources.getString("moraleLevelUnbreakable.text");
            case 2:
                return resources.getString("moraleLevelVeryHigh.text");
            case 3:
                return resources.getString("moraleLevelHigh.text");
            case 4:
                return resources.getString("moraleLevelNormal.text");
            case 5:
                return resources.getString("moraleLevelLow.text");
            case 6:
                return resources.getString("moraleLevelVeryLow.text");
            case 7:
                return resources.getString("moraleLevelBroken.text");
            default:
                throw new IllegalStateException("Unexpected value in getMoraleLevel: " + morale);
        }
    }

    /**
     * Calculates the morale check target number based on the campaign's morale and the desertion flag.
     *
     * @param campaign    the current campaign
     * @param isDesertion a flag indicating whether the target number is for desertion or not
     * @return the calculated target number
     * @throws IllegalStateException if the morale value is unexpected
     */
    private static int getTargetNumber(Campaign campaign, boolean isDesertion) {
        int morale = campaign.getMorale() / 10;

        switch (morale) {
            case 1:
                if (isDesertion) {
                    return 0;
                } else {
                    return -3;
                }
            case 2:
                if (isDesertion) {
                    return 1;
                } else {
                    return -2;
                }
            case 3:
                if (isDesertion) {
                    return 1;
                } else {
                    return -1;
                }
            case 4:
                if (isDesertion) {
                    return 2;
                } else {
                    return 0;
                }
            case 5:
                if (isDesertion) {
                    return 4;
                } else {
                    return 2;
                }
            case 6:
                if (isDesertion) {
                    return 5;
                } else {
                    return 4;
                }
            case 7:
                if (isDesertion) {
                    return 8;
                } else {
                    return 7;
                }
            default:
                throw new IllegalStateException("Unexpected value in getTargetNumber: " + morale);
        }
    }

    /**
     * Returns the final morale modifier for a person.
     *
     * @param campaign the ongoing campaign
     * @param person the person the modifier is being calculated for
     * @param isDesertion whether the target number is for a desertion check
     * @param meanLoyalty the mean loyalty value
     * @return the morale check modifiers
     */
    private static Integer getMoraleCheckModifiers(Campaign campaign, Person person, boolean isDesertion, Integer meanLoyalty) {
        int modifier = 0;

        // Custom Modifier
        modifier += campaign.getCampaignOptions().getCustomMoraleModifier();

        // Experience Level Modifier
        if (campaign.getCampaignOptions().isUseMoraleModifierExperienceLevel()) {
            modifier += getExperienceModifier(campaign, person);
        }

        // Faction Modifier
        if (campaign.getCampaignOptions().isUseMoraleModifierFaction()) {
            if (person.getOriginFaction().isClan()) {
                modifier++;
            } else if (person.getOriginFaction().isMercenary()) {
                modifier--;
            }
        }

        // Profession Modifier
        if (campaign.getCampaignOptions().isUseMoraleModifierProfession()) {
            modifier += getProfessionModifier(person.getPrimaryRole(), person);

            if (person.getSecondaryRole() != null) {
                modifier += (modifier + getProfessionModifier(person.getSecondaryRole(), person)) / 2;
            }
        }

        // Force Reliability Modifier
        if (campaign.getCampaignOptions().isUseMoraleModifierForceReliability()) {
            modifier += getForceReliabilityModifier(campaign, isDesertion, meanLoyalty);
        }

        // Cabin Fever
        if (campaign.getCampaignOptions().isUseMoraleModifierCabinFever()) {
            modifier--;
        }

        // Marriage
        if ((campaign.getCampaignOptions().isUseMoraleModifierMarriage()) && (isDesertion)) {
            modifier += 2;
        }

        // Management Skill Modifier
        if (campaign.getCampaignOptions().isUseMoraleModifierCommanderLeadership()) {
            Person commander = campaign.getFlaggedCommander();

            if ((commander != null) && (campaign.getCampaignOptions().isUseManagementSkill())) {
                if (commander.hasSkill(SkillType.S_LEADER)) {
                    if ((commander.getSkill(SkillType.S_LEADER).getLevel() + campaign.getCampaignOptions().getManagementSkillPenalty()) > 0) {
                        modifier++;
                    }
                }
            }
        }

        // Loyalty Modifier
        if ((campaign.getCampaignOptions().isUseLoyaltyModifiers()) && (campaign.getCampaignOptions().isUseMoraleModifierLoyalty())) {
            modifier += getLoyaltyModifier(isDesertion, person.getLoyalty());
        }

        // Leadership Method Modifier
        switch (campaign.getCampaignOptions().getMoraleModifierLeadershipMethod()) {
            case REGULAR:
                break;
            case FAMILY:
                if (isDesertion) {
                    modifier--;
                } else {
                    modifier++;
                }
                break;
            case GREEN:
                if (isDesertion) {
                    modifier--;
                }
                break;
            case ELITE:
            case IRON_FIST:
                modifier++;
                break;
        }

        return modifier;
    }

    /**
     * Calculates the experience modifier based on the person's experience level.
     *
     * @param campaign the campaign that the person is participating in
     * @param person the person whose experience level is being evaluated
     * @return the experience modifier based on the person's experience level
     * @throws IllegalStateException if the person's experience level is an unexpected value
     */
    private static int getExperienceModifier(Campaign campaign, Person person) {
        switch (person.getExperienceLevel(campaign, false)) {
            case -1:
                return -2;
            case 0:
            case 1:
                return -1;
            case 2:
                return 0;
            case 3:
                return 1;
            case 4:
                return 2;
            default:
                throw new IllegalStateException("Unexpected value in getExperienceModifier: " + person.getExperienceLevel(campaign, false));
        }
    }

    /**
     * Calculates the force reliability modifier based on the desertion flag, and chosen reliability method.
     *
     * @param campaign    the ongoing campaign
     * @param isDesertion  whether the target number is for a desertion check
     * @param meanLoyalty the mean loyalty value
     * @return the force reliability modifier as an integer value
     */
    private static int getForceReliabilityModifier(Campaign campaign, boolean isDesertion, Integer meanLoyalty) {
        ForceReliabilityMethod reliabilityMethod = campaign.getCampaignOptions().getForceReliabilityMethod();

        switch (reliabilityMethod) {
            case UNIT_RATING:
                return getUnitRatingModifier(campaign.getUnitRatingMod(), isDesertion);
            case LOYALTY:
                if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
                    return getLoyaltyModifier(isDesertion, meanLoyalty);
                } else {
                    return getUnitRatingModifier(campaign.getUnitRatingMod(), isDesertion);
                }
            case OVERRIDE_C:
                return 0;
            case OVERRIDE_A:
                return 1;
            case OVERRIDE_B:
                if (isDesertion) {
                    return 1;
                } else {
                    return 0;
                }
            case OVERRIDE_D:
                if (!isDesertion) {
                    return -1;
                } else {
                    return 0;
                }
            case OVERRIDE_F:
                return -1;
        }
        return 0;
    }

    /**
     * Computes the loyalty morale modifier.
     *
     * @param isDesertion  whether the target number is for a desertion check
     * @param meanLoyalty  the mean loyalty value used to calculate the loyalty modifier
     * @return the loyalty modifier calculated based on the given parameters
     * @throws IllegalStateException if the meanLoyalty value is unexpected
     */
    private static int getLoyaltyModifier(boolean isDesertion, Integer meanLoyalty) {
        switch (meanLoyalty) {
            case -3:
                return -1;
            case -2:
            case -1:
                if (!isDesertion) {
                    return -1;
                } else {
                    return 0;
                }
            case 0:
            case 1:
                if (isDesertion) {
                    return 1;
                } else {
                    return 0;
                }
            case 2:
            case 3:
                return 1;
            default:
                throw new IllegalStateException("Unexpected value in getLoyaltyModifier: " + meanLoyalty);
        }
    }

    /**
     * Computes the unit rating morale modifier.
     *
     * @param unitRatingMod the unit rating modifier value
     * @param isDesertion   whether the target number is for a desertion check
     * @return the unit rating morale modifier
     * @throws IllegalStateException if the campaign has an unexpected value for unit rating
     */
    private static int getUnitRatingModifier(Integer unitRatingMod, boolean isDesertion) {
        switch(unitRatingMod) {
            case 0:
                return -1;
            case 1:
                if (!isDesertion) {
                    return -1;
                } else {
                    return 0;
                }
            case 2:
            case 3:
                return 0;
            case 4:
                if (isDesertion) {
                    return 1;
                } else {
                    return 0;
                }
            case 5:
                return 1;
            default:
                throw new IllegalStateException("Unexpected value in getUnitRatingModifier: " + unitRatingMod);
        }
    }

    /**
     * Returns the profession modifier based on the role and person.
     *
     * @param role   the personnel role of the person
     * @param person the person for whom the profession modifier is being calculated
     * @return the profession modifier as an Integer
     */
    private static Integer getProfessionModifier(PersonnelRole role, Person person) {
        if (role.isVesselCrew()) {
            if (person.getUnit() == null) {
                return -1;
            } else {
                Entity entity = person.getUnit().getEntity();

                if ((entity.isSmallCraft()) || (entity.isJumpShip())) {
                    return -1;
                } else if (entity.isDropShip()) {
                    return 0;
                } else if (entity.isWarShip()) {
                    return 2;
                }
            }
        } else if ((role.isMechWarrior()) || (role.isProtoMechPilot()) || (role.isAerospaceGrouping()) || (role.isMedicalStaff())) {
            return 1;
        } else if ((role.isSoldier()) || (role.isTech())) {
            return -1;
        } else if (role.isAdministrator()) {
            return -2;
        } else if (role.isVehicleCrew()) {
            if (person.getUnit() == null) {
                return 0;
            } else if (person.getUnit().getEntity().isSupportVehicle()) {
                return -2;
            }
        }

        return 0;
    }

    /**
     * Adds a morale report to the given Campaign.
     *
     * @param campaign  the Campaign for which to generate the report
     */
    private static void getMoraleReport(Campaign campaign, ResourceBundle resources) {
        StringBuilder moraleReport = new StringBuilder();

        if (getTargetNumber(campaign, true) > 2) {
            moraleReport.append(String.format(resources.getString("moraleReportLow.text"), getMoraleLevel(campaign)));
        } else {
            moraleReport.append(String.format(resources.getString("moraleReport.text"), getMoraleLevel(campaign)));
        }

        if (getTargetNumber(campaign, false) > 2) {
            moraleReport.append(' ').append(resources.getString("moraleReportMutiny.text"));
        }

        campaign.addReport(moraleReport.toString());
    }

    /**
     * Makes morale checks for personnel in a given campaign.
     *
     * @param campaign     the campaign in which to make the morale checks
     * @param isDesertion  a boolean indicating if the checks are for desertion (true) or mutiny (false)
     * @return true if someone has mutinied or deserted, false otherwise
     */
    public static boolean makeMoraleChecks(Campaign campaign, boolean isDesertion) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        // we start with cases that cause not check to be needed
        if ((isDesertion) && (!campaign.getCampaignOptions().isUseDesertions())) {
            return false;
        } else if ((isDesertion) && (!campaign.getLocation().isOnPlanet())) {
            return false;
        } else if ((!isDesertion) && (!campaign.getCampaignOptions().isUseMutinies())) {
            return false;
        }

        // Next, we gather essential information, such as the target number,
        // personnel list, unit list (if unit theft is enabled, and mean loyalty score
        int targetNumber = getTargetNumber(campaign, isDesertion);

        List<Person> filteredPersonnel = campaign.getActivePersonnel().stream()
                .filter(person -> (person.getPrisonerStatus().isFreeOrBondsman()) || (!isDesertion))
                .filter(person -> !person.isChild(campaign.getLocalDate()))
                .collect(Collectors.toList());

        int meanLoyalty = getMeanLoyalty(campaign, isDesertion);

        // finally, we perform the actual checks, building a list of deserters and mutineers
        HashMap<Person, Integer> deserters = new HashMap<>();
        HashMap<Person, Integer> mutineers = new HashMap<>();

        for (Person person : filteredPersonnel) {
            int modifier = getMoraleCheckModifiers(campaign, person, isDesertion, meanLoyalty);

            int firstRoll = Compute.d6(2) + modifier;
            int secondRoll = Compute.d6(2) + modifier;

            if (isDesertion) {
                // Bondsmen can mutiny, but they cannot desert
                if (person.getPrisonerStatus().isBondsman()) {
                    continue;
                }

                if ((firstRoll < targetNumber) && (secondRoll < targetNumber)) {
                    deserters.put(person, secondRoll);
                }
            } else {
                if (firstRoll < targetNumber) {
                    // we record secondRoll in case the mutiny turns into desertion
                    mutineers.put(person, secondRoll);
                }
            }
        }

        // the rolls made, we check whether a mutiny or desertion has occurred and if so, process it

        if (isDesertion) {
            if (!deserters.isEmpty()) {
                processDesertion(campaign, deserters, targetNumber, resources);

                return true;
            } else {
                return false;
            }
        } else {
            if (!mutineers.isEmpty()) {
                List<Person> loyalists = filteredPersonnel.stream()
                        .filter(person -> !mutineers.containsKey(person))
                        .collect(Collectors.toList());

                processMutiny(campaign, loyalists, mutineers, targetNumber, resources);

                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Calculates the mean loyalty of active personnel in a campaign.
     *
     * @param campaign the active personnel
     * @param isDesertion true if desertions should be included in the calculation, false otherwise
     * @return the mean loyalty of the active personnel in the campaign
     */
    private static int getMeanLoyalty(Campaign campaign, boolean isDesertion) {
        int loyalty = campaign.getActivePersonnel().stream()
                .filter(person -> (person.getPrisonerStatus().isFree()) || (!isDesertion))
                .filter(person -> person.isChild(campaign.getLocalDate()))
                .mapToInt(Person::getLoyalty)
                .sum();

        long personnel = campaign.getActivePersonnel().stream()
                .filter(person -> (person.getPrisonerStatus().isFree()) || (!isDesertion))
                .filter(person -> person.isChild(campaign.getLocalDate()))
                .count();

        if (personnel == 0) {
            return 0;
        } else {
            return (int) (loyalty / personnel);
        }
    }

    /**
     * Reclaims the original unit for a person in a campaign.
     * If the unit is no longer available, return an integer used to reduce effective morale.
     *
     * @param campaign The campaign the person is participating in
     * @param person The person whose original unit is to be reclaimed
     * @return {@code true} if the original unit is reclaimed successfully, {@code false} otherwise
     */
    static boolean reclaimOriginalUnit(Campaign campaign, Person person) {
        UUID originalUnitId = person.getOriginalUnitId();

        if (!campaign.getUnit(originalUnitId).isDeployed()) {
            try {
                campaign.removeUnit(person.getOriginalUnitId());
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Gets the percentage modifier based on a roll of two six-sided dice.
     *
     * @return the percentage modifier based on the dice roll:
     *         - 3 for a roll of 2
     *         - 2 for a roll of 3
     *         - 1 for a roll of 4 or 5
     *         - 0 for a roll of 6, 7, or 8
     *         - -1 for a roll of 9
     *         - -2 for a roll of 10 or 11
     *         - -3 for a roll of 12
     * @throws IllegalStateException if the roll is unexpected
     */
    static int getPercentageModifier() {
        int roll = Compute.d6(2);

        switch(roll) {
            case 2:
                return 3;
            case 3:
                return 2;
            case 4:
            case 5:
                return 1;
            case 6:
            case 7:
            case 8:
                return 0;
            case 9:
                return -1;
            case 10:
            case 11:
                return -2;
            case 12:
                return -3;
            default:
                throw new IllegalStateException("Unexpected value in getPercentageModifier: " + roll);
        }
    }

    /**
     * Processes the number of AWOL (Absent Without Leave) days for a person.
     * If the person has no AWOL days remaining, it randomly determines whether to add another d6 AWOL days
     * or change the person's status to ACTIVE.
     * Otherwise, it subtracts one AWOL day from the person's total.
     *
     * @param campaign the campaign in which the person belongs
     * @param person the person for whom AWOL days are being processed
     */
    public static void processAwolDays(Campaign campaign, Person person) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        int awolDays = person.getAwolDays();

        if (awolDays == 0) {
            if (Compute.d6(1) <= 2) {
                person.setAwolDays(awolDays + Compute.d6(1));
                campaign.addReport(person.getHyperlinkedFullTitle() + ' ' + resources.getString("desertionAwolExtended.text"));
            } else {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
            }
        } else if (awolDays > 0) {
            person.setAwolDays(awolDays - 1);
        } else {
            person.setAwolDays(awolDays - 1);
        }
    }

    /**
     * The method processes the morale change in a campaign.
     *
     * @param campaign the campaign to process the morale change for
     * @param steps the number of steps to change the morale by
     */
    public static void processMoraleChange(Campaign campaign, int steps) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        int change = campaign.getCampaignOptions().getMoraleStepSize() * steps;
        int oldMorale = campaign.getMorale();
        int newMorale = MathUtility.clamp(campaign.getMorale() + change, 10, 70);

        campaign.setMorale(newMorale);

        if ((oldMorale / 10) != (newMorale / 10)) {
            getMoraleReport(campaign, resources);

            if ((oldMorale >= 50) && (newMorale < 50)) {
                campaign.addReport(resources.getString("moraleReportRecovered.text"));
            }
        }
    }
}
