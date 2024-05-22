package mekhq.campaign.personnel.turnoverAndRetention;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.ForceReliabilityMethod;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;

import java.util.*;

public class Morale {

    /**
     * This method returns the Morale level as a string based on the value of the 'Morale' variable.
     *
     * @return The Morale level as a string.
     * @throws IllegalStateException if the value of 'Morale' is unexpected.
     */
    public static String getMoraleLevel(Campaign campaign) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        double morale = campaign.getMorale();

        if ((morale >= 1) && (morale < 2)) {
            return resources.getString("moraleLevelUnbreakable.text");
        } else if (morale < 3) {
            return resources.getString("moraleLevelVeryHigh.text");
        } else if (morale < 4) {
            return resources.getString("moraleLevelHigh.text");
        } else if (morale < 5) {
            return resources.getString("moraleLevelNormal.text");
        } else if (morale < 6) {
            return resources.getString("moraleLevelLow.text");
        } else if (morale < 7) {
            return resources.getString("moraleLevelVeryLow.text");
        } else if (morale == 7) {
            return resources.getString("moraleLevelBroken.text");
        }

        throw new IllegalStateException("Unexpected value in getMoraleLevel(): " + campaign.getMorale());
    }

    /**
     * Returns the target number for a morale check based on the campaign morale level and desertion flag.
     *
     * @param campaign the current campaign
     * @param isDesertion whether the target number is for a desertion check
     * @return the base target number for morale
     * @throws IllegalStateException if the campaign morale level is unexpected
     */
    private static Double getTargetNumber(Campaign campaign, boolean isDesertion) {
        double morale = campaign.getMorale();

        if ((morale >= 1) && (morale < 4)) {
            return 0.0;
        } else if (morale < 5) {
            if (isDesertion) {
                return 2.0;
            } else {
                return 0.0;
            }
        } else if (morale < 7) {
            if (isDesertion) {
                return 5.0;
            } else {
                return 4.0;
            }
        } else if (morale == 7) {
            if (isDesertion) {
                return 8.0;
            } else {
                return 7.0;
            }
        }

        throw new IllegalStateException("Unexpected value in getTargetNumber: " + campaign.getMorale());
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

        // Iron Fist Modifier
        if (campaign.getCampaignOptions().isUseRuleWithIronFist()) {
            modifier++;
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
     * @param campaign the Campaign for which to generate the report
     */
    public static void getMoraleReport(Campaign campaign) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        StringBuilder moraleReport = new StringBuilder();

        if (getTargetNumber(campaign, true) >= 2) {
            moraleReport.append(String.format(resources.getString("moraleReportLow.text"), getMoraleLevel(campaign)));
        } else {
            moraleReport.append(String.format(resources.getString("moraleReport.text"), getMoraleLevel(campaign)));
        }

        if (getTargetNumber(campaign, false) >= 2) {
            moraleReport.append(' ').append(resources.getString("moraleReportMutiny.text"));
        }

        campaign.addReport(moraleReport.toString());
    }

    public static void makeMoraleChecks(Campaign campaign, boolean isDesertion) {
        if ((isDesertion) && (!campaign.getCampaignOptions().isUseDesertions())) {
            return;
        } else if ((isDesertion) && (!campaign.getLocation().isOnPlanet())) {
            return;
        } else if ((!isDesertion) && (!campaign.getCampaignOptions().isUseMutinies())) {
            return;
        }

        double targetNumber = getTargetNumber(campaign, isDesertion);

        if (targetNumber == 0) {
            return;
        }

        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        List<Person> filteredPersonnel = new ArrayList<>();
        int loyalty = 0;

        for (Person person : campaign.getActivePersonnel()) {
            if ((person.getPrisonerStatus().isFree()) || (!isDesertion)) {
                filteredPersonnel.add(person);
                loyalty += person.getLoyalty();
            }
        }

        if (campaign.getCampaignOptions().getForceReliabilityMethod().isLoyalty()) {
            loyalty = MathUtility.clamp(loyalty / filteredPersonnel.size(), -3, 3);
        } else {
            loyalty = 0;
        }

        double morale = campaign.getMorale();

        ArrayList<Unit> unitList = new ArrayList<>();

        if (isDesertion) {
            unitList = (ArrayList<Unit>) campaign.getUnits();
        }

        boolean someoneHasDeserted = false;
        boolean someoneHasMutinied = false;

        List<Person> loyalists = new ArrayList<>();
        List<Person> rebels = new ArrayList<>();

        for (Person person : filteredPersonnel) {
            int modifier = getMoraleCheckModifiers(campaign, person, isDesertion, loyalty);
            int roll = Compute.d6(2) + modifier;

            if ((campaign.getCampaignOptions().isUseLoyaltyModifiers()) && (campaign.getCampaignOptions().isUseMoraleModifierLoyalty())) {
                roll += getLoyaltyModifier(isDesertion, person.getLoyalty());
            }

            if (roll <= targetNumber) {
                if (isDesertion) {
                    // TODO emergency bonuses give reroll

                    if ((processDisertion(campaign, person, roll, targetNumber, morale, unitList, resources)) && (!someoneHasDeserted)) {
                        someoneHasDeserted = true;
                    }
                } else {
                    rebels.add(person);
                    someoneHasMutinied = true;
                }
            } else {
                loyalists.add(person);
            }
        }

        if (someoneHasMutinied) {
            processMoraleLoss(campaign, -2);
            processMutiny(campaign, loyalists, rebels);
        } else if (someoneHasDeserted) {
            processMoraleLoss(campaign, -1);
        }
    }

    private static boolean processDisertion(Campaign campaign, Person person, int roll, double targetNumber, double morale, ArrayList<Unit> unitList, ResourceBundle resources) {
        if (roll <= (targetNumber - 2)) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.DESERTED);

            if ((roll <= (morale - 4)) && (campaign.getCampaignOptions().isUseTheft())) {
                processTheft(campaign, unitList, resources);
            }

            return true;
        } else {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.AWOL);
            person.setAwolDays(Compute.d6(2));

            return false;
        }
    }

    /**
     * Process theft of units and money.
     *
     * @param campaign   the campaign from which units or funds are stolen
     * @param unitList   the list of available units
     * @param resources  the resource bundle for retrieving localized strings
     */
    private static void processTheft(Campaign campaign, ArrayList<Unit> unitList, ResourceBundle resources) {
        boolean committingTheft = true;
        int attempts = 3;

        while (committingTheft) {
            Unit desiredUnit = unitList.get(new Random().nextInt(unitList.size()));

            if ((desiredUnit.getEntity().isLargeCraft()) || (desiredUnit.getEntity().isWarShip()) || (desiredUnit.isDeployed())) {
                attempts--;

                if (attempts == 0) {
                    Money stolenMoney = campaign.getFinances().getBalance().multipliedBy(0.1);

                    campaign.getFinances().debit(TransactionType.THEFT, campaign.getLocalDate(), stolenMoney, resources.getString("desertionHeist.text"));
                    committingTheft = false;
                }
            } else {
                StringBuilder unitName = new StringBuilder(desiredUnit.getName());

                if (!Objects.equals(desiredUnit.getFluffName(), "")) {
                    unitName.append(' ').append(desiredUnit.getFluffName());
                }

                campaign.addReport(String.format(resources.getString("desertionStolen.text"), unitName));

                campaign.removeUnit(desiredUnit.getId());
                unitList.remove(desiredUnit);

                committingTheft = false;
            }
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
        int awolDays = person.getAwolDays();

        if (awolDays == 0) {
            if (Compute.d6(1) <= 2) {
                person.setAwolDays(awolDays + Compute.d6(1));
            } else {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
            }
        } else {
            person.setAwolDays(awolDays - 1);
        }
    }

    /**
     * Process morale recovery for a campaign by decreasing the morale value based on the given number of steps.
     * If the current morale value is not equal to 1, the morale value will be updated by subtracting the 'steps' value.
     * The updated morale value will be clamped within the range of 1 to 7.
     * After the morale value is updated, the morale report will be generated.
     *
     * @param campaign the Campaign object representing the campaign to process morale loss for
     * @param steps the Integer value representing the number of steps to decrease morale by
     */
    public static void processMoraleRecovery(Campaign campaign, Integer steps) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        double morale = campaign.getMorale();

        if (morale != 1) {
            campaign.setMorale(MathUtility.clamp(morale - (steps * campaign.getCampaignOptions().getStepSize()), 1.0, 7.0));
            getMoraleReport(campaign);

            if ((morale >= 5) && ((morale + steps) < 5)) {
                campaign.addReport(resources.getString("moraleReportRecovered.text"));
            }
        }
    }

    /**
     * Process morale loss for a campaign by increasing the morale value based on the given number of steps.
     * If the current morale value is not equal to 7, the morale value will be updated by adding the 'steps' value.
     * The updated morale value will be clamped within the range of 1 to 7.
     * After the morale value is updated, the morale report will be generated.
     *
     * @param campaign the Campaign object representing the campaign to process morale loss for
     * @param steps the Integer value representing the number of steps to increase morale by
     */
    public static void processMoraleLoss(Campaign campaign, Integer steps) {
        double morale = campaign.getMorale();

        if (morale != 7) {
            campaign.setMorale(MathUtility.clamp(morale + (steps * campaign.getCampaignOptions().getStepSize()), 1.0, 7.0));
            getMoraleReport(campaign);
        }
    }

    private static void processMutiny(Campaign campaign, List<Person> loyalists, List<Person> rebels) {
        // TODO process mutinies
        // There should be three possible events: violent transfer of power, demand leader step down, or failed to garner enough support
        // Player should have the option to join the rebels or stay with the loyalists
    }
}
