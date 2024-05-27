package mekhq.campaign.personnel.turnoverAndRetention;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.ForceReliabilityMethod;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.MutinySupportDialog;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;

import static megamek.common.EntityWeightClass.WEIGHT_LARGE_WAR;

public class Morale {
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
            case 0:
                LogManager.getLogger().error("IMPORTANT: Morale has weirdly reset");
                return 0;
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
     * @param campaign the Campaign for which to generate the report
     */
    public static void getMoraleReport(Campaign campaign) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

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

        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        List<Person> filteredPersonnel = campaign.getActivePersonnel().stream()
                .filter(person -> (person.getPrisonerStatus().isFree()) || (!isDesertion))
                .filter(person -> !person.isChild(campaign.getLocalDate()))
                .collect(Collectors.toList());

        int meanLoyalty = getMeanLoyalty(campaign, isDesertion);

        List<Unit> possibleTheftTargets = new ArrayList<>();

        if (campaign.getCampaignOptions().isUseTheftUnit()) {
            possibleTheftTargets = campaign.getHangar().getUnits().stream()
                    .filter(unit ->
                            (!unit.isDamaged())
                            && (!unit.isDeployed())
                            && (!unit.getEntity().isLargeCraft())
                            && (!unit.getEntity().isWarShip())
                    )
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        // Here we identify who the loyalist commander is
        Person loyalistLeader = campaign.getFlaggedCommander();

        // if there is no Commander, we assume the highest ranked person is the loyalist leader
        if (loyalistLeader == null) {
            loyalistLeader = campaign.getHighestRankedPerson(filteredPersonnel, true);
        }

        // Next we perform the actual checks, building a list of loyalists and rebels (if relevant)
        List<Person> loyalists = new ArrayList<>();
        HashMap<Person, Integer> rebels = new HashMap<>();

        boolean someoneHasDeserted = false;
        boolean someoneHasMutinied = false;

        for (Person person : filteredPersonnel) {
            int modifier = getMoraleCheckModifiers(campaign, person, isDesertion, meanLoyalty);

            int firstRoll = Compute.d6(2) + modifier;
            int secondRoll = Compute.d6(2) + modifier;

            if (isDesertion) {
                if ((firstRoll < targetNumber) && (secondRoll < targetNumber)) {
                    campaign.addReport(person.getFullName() + " failed their morale check [TN" + targetNumber + "] [rA" + firstRoll +  "][rB" + secondRoll + ']');
                    possibleTheftTargets.remove(processDesertion(campaign, person, secondRoll, targetNumber, possibleTheftTargets, resources));
                    someoneHasDeserted = true;
                }
            } else {
                if (firstRoll < targetNumber) {
                    if (person.getUnit() != null) {
                        if ((person.getUnit().getCrew().contains(loyalistLeader)) && (person.isCommander())) {
                            loyalists.addAll(person.getUnit().getCrew());
                        } else if (person.isCommander()) {
                            for (Person crew : person.getUnit().getCrew()) {
                                if (person.isCommander()) {
                                    rebels.put(person, firstRoll);
                                } else {
                                    rebels.put(crew, targetNumber - Compute.d6(1));
                                }
                            }
                        }
                    } else {
                        rebels.put(person, firstRoll);
                    }

                    someoneHasMutinied = true;
                } else {
                    if ((person.getUnit() != null) && (person.isCommander())) {
                        loyalists.addAll(person.getUnit().getCrew());
                    } else if (person.getUnit() == null) {
                        loyalists.add(person);
                    }
                }
            }
        }

        // the rolls made, we check whether a mutiny or desertion has occurred and if so, process it
        if (someoneHasDeserted) {
            return true;
        } else if (someoneHasMutinied) {
            processMutiny(campaign, loyalistLeader, loyalists, rebels, possibleTheftTargets, resources);
            return true;
        }

        return false;
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
     * Processes desertion for a person.
     *
     * @param campaign       the current campaign
     * @param person         the potential deserter
     * @param roll           the desertion roll result
     * @param targetNumber   the target number for desertion=
     * @param possibleTheftTargets       the list of units
     * @param resources      the resource bundle for localized messages
     */
    private static Unit processDesertion(Campaign campaign, Person person, int roll, int targetNumber,
                                          List<Unit> possibleTheftTargets, ResourceBundle resources) {
        int morale = campaign.getMorale();

        if (roll <= (targetNumber - 2)) {
            if (campaign.getCampaignOptions().getMoraleModifierLeadershipMethod().isIronFist()) {
                morale -= 2;
            }

            if (roll <= (morale - 2)) {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.DESERTED);

                // reclaim original unit (if available)
                if (person.getOriginalUnitId() != null) {
                    morale = reclaimOriginalUnit(campaign, person);
                }

                // check for theft
                if (((roll + 6) < (morale / 10)) && (campaign.getCampaignOptions().isUseTheftUnit()) && (!possibleTheftTargets.isEmpty())) {
                    return processUnitTheft(campaign, possibleTheftTargets, resources);
                } else if (((roll + 5) < (morale / 10)) && (campaign.getCampaignOptions().isUseTheftMoney())) {
                    processMoneyTheft(campaign, resources);
                    return null;
                } else if (((roll + 4) < (morale / 10)) && (campaign.getCampaignOptions().isUseTheftParts())) {
                    processPartTheft(campaign, resources);
                    return null;
                } else if ((roll + 3) < (morale / 10)) {
                    processPettyTheft(campaign, resources);
                    return null;
                }
            }
        } else {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.AWOL);
            person.setAwolDays(Compute.d6(2));
        }

        return null;
    }

    /**
     * Reclaims the original unit for a person in a campaign.
     * If the unit is no longer available, return an integer used to reduce effective morale.
     *
     * @param campaign The campaign the person is participating in
     * @param person The person whose original unit is to be reclaimed
     * @return 0 if the original unit is removed successfully, or an integer if removal fails
     */
    private static int reclaimOriginalUnit(Campaign campaign, Person person) {
        UUID originalUnitId = person.getOriginalUnitId();
        int originalUnitWeight = person.getOriginalUnitWeight();

        // this stops support vehicles being over-valued
        if (originalUnitWeight > WEIGHT_LARGE_WAR) {
            originalUnitWeight -= WEIGHT_LARGE_WAR;
        }

        if (!campaign.getUnit(originalUnitId).isDeployed()) {
            try {
                campaign.removeUnit(person.getOriginalUnitId());
                return 0;
            } catch (Exception e) {
                return originalUnitWeight;
            }
        } else {
            return originalUnitWeight;
        }
    }

    /**
     * Processes unit thefts during a campaign.
     *
     * @param campaign               The campaign in which the unit thefts occur.
     * @param possibleTheftTargets   The list of units that can be stolen.
     * @param resources              The resource bundle for internationalization.
     * @return A list of stolen units.
     */
    private static Unit processUnitTheft(Campaign campaign, List<Unit> possibleTheftTargets, ResourceBundle resources) {
        // if there is nothing left to steal, downgrade the theft
        if (possibleTheftTargets.isEmpty()) {
            if (campaign.getCampaignOptions().isUseTheftMoney()) {
                processMoneyTheft(campaign, resources);
            } else if (campaign.getCampaignOptions().isUseTheftParts()) {
                processPartTheft(campaign, resources);
            } else {
                processPettyTheft(campaign, resources);
            }

            return null;
        }

        // process the theft
        Unit stolenUnit = possibleTheftTargets.get(new Random().nextInt(possibleTheftTargets.size()));
        String theftString = stolenUnit.getName();

        if (!Objects.equals(stolenUnit.getFluffName(), "")) {
            theftString += ' ' + stolenUnit.getFluffName();
        }

        if ((campaign.getCampaignOptions().isUseAtB()) && (!campaign.getFaction().isClan()) && (Compute.d6(1) >= 5)) {
            int stolenUnitType = stolenUnit.getEntity().getUnitType();
            String stolenUnitShortNameRaw = stolenUnit.getEntity().getShortNameRaw();

            campaign.getUnitMarket().addSingleUnit(campaign,
                    UnitMarketType.BLACK_MARKET,
                    stolenUnitType,
                    MechSummaryCache.getInstance().getMech(stolenUnitShortNameRaw),
                    campaign.getCampaignOptions().getTheftResellValue() + getPercentageModifier());

            campaign.addReport(String.format(resources.getString("desertionTheft.text"), theftString));
            campaign.addReport(String.format(resources.getString("desertionTheftBlackMarket.text"), theftString));
        } else {
            campaign.addReport(String.format(resources.getString("desertionTheft.text"), theftString));
        }

        return stolenUnit;
    }

    /**
     * Processes money theft.
     *
     * @param campaign   the campaign for which the theft is being processed
     * @param resources  the ResourceBundle containing the necessary resources
     */
    private static void processMoneyTheft(Campaign campaign, ResourceBundle resources) {
        int theftPercentage = campaign.getCampaignOptions().getTheftValue();

        theftPercentage += getPercentageModifier();

        Money theft = campaign.getFunds()
                .multipliedBy(theftPercentage)
                .dividedBy(100)
                .round();

        if (theft.isPositive()) {
            campaign.getFinances().debit(TransactionType.THEFT, campaign.getLocalDate(), theft,
                    String.format(resources.getString("desertionTheftTransactionReport.text"),
                            FinancialInstitutions.randomFinancialInstitution(campaign.getLocalDate()).toString()));

            campaign.addReport(String.format(String.format(resources.getString("desertionTheftMoney.text"), theft.getAmount())));
        } else if (campaign.getCampaignOptions().isUseTheftParts()) {
            processPartTheft(campaign, resources);
        } else {
            processPettyTheft(campaign, resources);
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
    private static int getPercentageModifier() {
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

    private static void processPartTheft(Campaign campaign, ResourceBundle resources) {
        List<Part> possibleTheftTargets = campaign.getWarehouse().getSpareParts();

        // if there are no parts to steal, commit petty theft instead
        if (possibleTheftTargets.isEmpty()) {
            processPettyTheft(campaign, resources);

            return;
        }

        // how many thefts should be rolled?
        int originalTheftCount = 1;

        if (campaign.getCampaignOptions().getTheftPartsDiceCount() != 0) {
            originalTheftCount = Compute.d6(campaign.getCampaignOptions().getTheftPartsDiceCount());
        }

        boolean committingTheft = true;
        int theftCount = originalTheftCount;
        HashMap<String, Integer> stolenItems = new HashMap<>();

        while (committingTheft) {
            if (possibleTheftTargets.isEmpty()) {
                if (stolenItems.isEmpty()) {
                    processPettyTheft(campaign, resources);

                    return;
                }
                committingTheft = false;
                continue;
            }

            Part desiredPart = possibleTheftTargets.get(new Random().nextInt(possibleTheftTargets.size()));

            boolean partStolen = false;

            while (!partStolen) {
                boolean hasParent = true;

                while (hasParent) {
                    if (desiredPart.getParentPart() != null) {
                        possibleTheftTargets.remove(desiredPart);
                        desiredPart = desiredPart.getParentPart();
                    } else {
                        hasParent = false;
                    }
                }

                // if the part is in transit,
                // we don't want to steal it, so we pick another item
                if (desiredPart.getDaysToArrival() > 0) {
                    possibleTheftTargets.remove(desiredPart);
                    partStolen = true;
                    continue;
                } else if (desiredPart.getDaysToWait() > 0) {
                    possibleTheftTargets.remove(desiredPart);
                    partStolen = true;
                    continue;
                }

                // if the part is being actively worked on,
                // we don't want to steal it, so we pick another item
                if (desiredPart.isBeingWorkedOn()) {
                    possibleTheftTargets.remove(desiredPart);
                    partStolen = true;
                    continue;
                }

                // this is where we try to steal an item
                if ((desiredPart instanceof AmmoStorage) || (desiredPart instanceof Armor)) {
                    int roll = Compute.d6(3);

                    if (campaign.getWarehouse().removePart(desiredPart, roll)) {
                        theftCount--;
                        possibleTheftTargets.remove(desiredPart);
                        stolenItems.put(desiredPart.getName(), roll);
                    } else {
                        LogManager.getLogger().info("Part theft failed to steal ammo/armor ({})", desiredPart);
                        partStolen = true;
                        possibleTheftTargets.remove(desiredPart);
                        continue;
                    }
                } else {
                    if (campaign.getWarehouse().removePart(desiredPart, 1)) {
                        theftCount--;
                        possibleTheftTargets.remove(desiredPart);
                        stolenItems.put(desiredPart.getName(), 1);
                    } else {
                        LogManager.getLogger().info("Part theft failed to steal part ({})", desiredPart);
                        partStolen = true;
                        possibleTheftTargets.remove(desiredPart);
                        continue;
                    }
                }

                partStolen = true;

                if (theftCount == 0) {
                    committingTheft = false;

                    campaign.addReport(stolenItems.keySet().stream()
                            .map(entry -> " [" + stolenItems.get(entry) + "x " + entry + ']')
                            .collect(Collectors.joining(""
                                    , resources.getString("desertionTheftParts.text")
                                    , "")));
                }
            }
        }
    }

    /**
     * This method is used to process a petty theft incident in a company.
     * It randomly selects an item from a list of stolen items and adds the item to the campaign report.
     *
     * @param campaign   The campaign object to add the report to.
     * @param resources  The ResourceBundle object to retrieve localized strings.
     */
    private static void processPettyTheft(Campaign campaign, ResourceBundle resources) {
        List<String> items = List.of(
                "stapler.text",
                "mascot.text",
                "phones.text",
                "tablets.text",
                "hardDrives.text",
                "flashDrive.text",
                "companyCreditCard.text",
                "officePet.text",
                "confidentialReports.text",
                "clientLists.text",
                "unitSchematics.text",
                "businessPlans.text",
                "marketingMaterials.text",
                "trainingPresentations.text",
                "softwareLicenses.text",
                "rifle.text",
                "financialRecords.text",
                "employeeRecords.text",
                "proprietarySoftware.text",
                "networkAccessCredentials.text",
                "companyUniforms.text",
                "desks.text",
                "monitors.text",
                "printers.text",
                "projectors.text",
                "carKeys.text",
                "dartboard.text",
                "securityBadges.text",
                "officeKeys.text",
                "pettyCashBox.text",
                "cheques.text",
                "diary.text",
                "giftCards.text",
                "coupons.text",
                "personalDataOfCoworkers.text",
                "battlePlans.text",
                "legalDocuments.text",
                "signedContracts.text",
                "clientFeedbackForms.text",
                "trainingManuals.text",
                "marketResearch.text",
                "businessContacts.text",
                "meetingNotes.text",
                "contractLeads.text",
                "urbanMechPlushie.text",
                "brandedMugs.text",
                "companyPhoneDirectories.text",
                "logbooks.text",
                "inventoryLists.text",
                "confidentialHpgMessages.text",
                "strategyDocuments.text",
                "passwordLists.text",
                "internalMemos.text",
                "surveillanceCameraRecordings.text",
                "brandedPens.text",
                "engineeringBlueprints.text",
                "codeRepositories.text",
                "internalNewsletters.text",
                "hrPolicies.text",
                "companyHandbooks.text",
                "procedureManuals.text",
                "securityPolicies.text",
                "simulationData.text",
                "businessCards.text",
                "ndaAgreements.text",
                "nonCompeteAgreements.text",
                "softwareCode.text",
                "technicalSpecifications.text",
                "securitySchedules.text",
                "underWear.text",
                "marketAnalysis.text",
                "salesContracts.text",
                "expenseReports.text",
                "reimbursementReceipts.text",
                "invoices.text",
                "employeeBenefitsInformation.text",
                "insuranceDocuments.text",
                "lightBulbs.text",
                "strategicAlliancesInformation.text",
                "computers.text",
                "boots.text",
                "employeeDiscountStructures.text",
                "meetingMinutes.text",
                "itInfrastructureDetails.text",
                "serverAccessCodes.text",
                "backupDrives.text",
                "missionData.text",
                "executiveMeetingNotes.text",
                "toe.text",
                "clientComplaints.text",
                "inventoryControlSystems.text",
                "chairs.text",
                "shippingLogs.text",
                "printerPaper.text",
                "internalAuditReports.text",
                "corruption.text",
                "officePlants.text",
                "battlefieldPerformanceReports.text",
                "companyStandard.text",
                "analyticsReports.text",
                "fridge.text",
                "coffeeMachine.text",
                "mug.text",
                "toiletSeats.text",
                "miniatures.text",
                "dropShip.text");

        campaign.addReport(String.format(resources.getString("desertionTheft.text"),
                resources.getString(items.get(new Random().nextInt(items.size())))));
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
                campaign.addReport(person.getHyperlinkedName() + ' ' + resources.getString("desertionAwolExtended.text"));
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
            getMoraleReport(campaign);

            if ((oldMorale >= 50) && (newMorale < 50)) {
                campaign.addReport(resources.getString("moraleReportRecovered.text"));
            }
        }
    }

    private static void processMutiny(Campaign campaign,
                                      Person loyalistLeader, List<Person> loyalists,
                                      HashMap<Person, Integer> rebels,
                                      List<Unit> possibleTheftTargets,
                                      ResourceBundle resources) {
        // This prevents us from needing to do the full process for tiny mutinies that have no chance of success
        if ((loyalists.size() / 2) > rebels.size()) {
            if (rebels.size() > 1) {
                campaign.addReport(String.format(resources.getString("mutinyThwartedPlural.text"), rebels.size()));
            } else {
                campaign.addReport(String.format(resources.getString("mutinyThwartedSingular.text"), rebels.size()));
            }

            for (Person person : rebels.keySet()) {
                possibleTheftTargets.remove(processDesertion(campaign,
                        person,
                        rebels.get(person),
                        getTargetNumber(campaign, true),
                        possibleTheftTargets, resources));
            }

            return;
        }

        // A civil war breaks out.
        List<Person> bystanders = new ArrayList<>();

        // The rebels have already picked their side, so we only need to process the loyalists
        // This represents the mutineers gathering support
        Iterator<Person> iterator = loyalists.iterator();

        while (iterator.hasNext()) {
            Person person = iterator.next();

            // The loyalist leader isn't allowed to rebel against themselves
            if (person.equals(loyalistLeader)) {
                continue;
            }


            // we then process everyone else
            int roll = Compute.d6(1);
            int civilWarTargetNumber = getCivilWarTargetNumber(campaign, person);

            if ((roll >= (civilWarTargetNumber - 1)) &&
                    (roll <= (civilWarTargetNumber + 1))) {

                iterator.remove();
                bystanders.add(person);
            } else if (roll < (civilWarTargetNumber - 1)) {
                iterator.remove();
                rebels.put(person, 0);
            }
        }

        // We now need to determine the leader of the rebels.
        // This might not always be someone who initially joined the mutiny,
        // in which case we can assume they were persuaded into the role by the original mutineers
        Person rebelLeader = getRebelLeader(campaign, new ArrayList<>(rebels.keySet()));

        HashMap<Unit, Integer> rebelUnits = getUnits(new ArrayList<>(rebels.keySet()), false);
        int rebelBv = rebelUnits.keySet().stream()
                .mapToInt(unit -> unit.getEntity()
                        .calculateBattleValue(true, false)).sum();

        HashMap<Unit, Integer> loyalUnits = getUnits(loyalists, true);
        int loyalistBv = loyalUnits.keySet().stream()
                .mapToInt(unit -> unit.getEntity()
                        .calculateBattleValue(true, false)).sum();

        // we now need to present the player with a choice: join the rebels, or support the loyalists
        int supportDecision = -1;

        while (supportDecision == -1) {
            supportDecision = MutinySupportDialog.supportDialog(
                    campaign, resources, false,
                    bystanders.size(),
                    loyalistLeader, loyalists.size(), new ArrayList<>(loyalUnits.keySet()), loyalistBv,
                    rebelLeader, rebels.size(), new ArrayList<>(rebelUnits.keySet()), rebelBv
            );
        }
    }

    /**
     * This method is used to determine the rebel leader based on the given campaign and rebel information.
     *
     * @param campaign The current campaign.
     * @param rebels   The list of rebels.
     * @return The person object representing the rebel leader.
     */
    private static Person getRebelLeader(Campaign campaign, List<Person> rebels) {
        Person rebelLeader = null;

        for (Person person : rebels) {
            if (rebelLeader == null) {
                rebelLeader = person;
                continue;
            }

            int oldRankNumeric = getAdjustedRankNumeric(campaign, rebelLeader);
            int newRankNumeric = getAdjustedRankNumeric(campaign, person);

            if (newRankNumeric == oldRankNumeric) {
                // in the case of a tie, we use the negotiation skill
                if ((person.hasSkill(SkillType.S_NEG)) && (Objects.requireNonNull(rebelLeader).hasSkill(SkillType.S_NEG))) {
                    if (person.getSkillLevel(SkillType.S_NEG) > rebelLeader.getSkillLevel(SkillType.S_NEG)) {
                        rebelLeader = person;
                    } else if (person.getSkillLevel(SkillType.S_NEG) == rebelLeader.getSkillLevel(SkillType.S_NEG)) {
                        // if we still have a tie, we use overall experience level.
                        // if this fails to break the tie, we give up and just use the new person
                        if (person.getExperienceLevel(campaign, false) > rebelLeader.getExperienceLevel(campaign, false)) {
                            rebelLeader = person;
                        } else if (person.getExperienceLevel(campaign, false) == rebelLeader.getExperienceLevel(campaign, false)) {
                            rebelLeader = person;
                        }
                    }
                } else if (person.hasSkill(SkillType.S_NEG)) {
                    rebelLeader = person;
                }
            } else if (newRankNumeric > oldRankNumeric) {
                rebelLeader = person;
            }
        }
        return rebelLeader;
    }

    /**
     * Returns the adjusted rank numeric for a person.
     *
     * @param campaign the current campaign
     * @param person the person whose rank numeric is being calculated
     * @return the adjusted numeric rank of the person
     */
    private static Integer getAdjustedRankNumeric(Campaign campaign, Person person) {
        int rankNumeric = person.getRankNumeric();

        if (person.hasSkill(SkillType.S_LEADER)) {
            rankNumeric = person.getSkillLevel(SkillType.S_LEADER);

            if (campaign.getCampaignOptions().isUseManagementSkill()) {
                rankNumeric += campaign.getCampaignOptions().getManagementSkillPenalty();
            }
        }

        if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            rankNumeric += person.getLoyalty();
        }

        return rankNumeric;
    }

    /**
     * Updates the ammo usage of each unit in the force, taking into account attrition.
     *
     * @param force the list of units in the force
     * @param attrition the attrition value to apply to the ammo usage calculation
     */
    private static void getAmmoUsage(List<Unit> force, int attrition) {
        for (Unit unit : force) {
            for (Mounted bin : unit.getEntity().getAmmo()) {
                int ammo = bin.getUsableShotsLeft();
                int roll = Compute.randomInt((int) ((ammo * 0.33) + attrition));

                bin.setShotsLeft(Math.max(0, ammo - roll));
            }
        }
    }

    /**
     * Maps force damage based on the given parameters.
     *
     * @param forceSize           the size of the force
     * @param attrition           the attrition value
     * @param enemyAttackDice     the number of dice used for enemy attacks
     * @param friendlyDefenceDice the number of dice used for friendly defenses
     * @return a HashMap containing the force damage:
     *         - "attrition": the attrition value after mapping
     *         - "damagedLight": the number of units damaged lightly
     *         - "damagedModerate": the number of units damaged moderately
     *         - "damagedBadly": the number of units damaged badly
     */
    private static HashMap<String, Integer> mapForceDamage(int forceSize, int attrition, int enemyAttackDice, int friendlyDefenceDice) {
        HashMap<String, Integer> forceDamage = new HashMap<>();

        attrition = attrition * (forceSize / 12);

        int damageDice = Math.max(0, enemyAttackDice - friendlyDefenceDice);

        int damagedBadly = 0;
        int damagedModerate = 0;
        int damagedLight = 0;

        for (int rollNumber = 0; rollNumber < damageDice; rollNumber++) {
            switch (Compute.d6(1)) {
                case 1:
                    damagedBadly++;
                    break;
                case 2:
                case 3:
                    damagedModerate++;
                    break;
                case 4:
                case 5:
                case 6:
                default:
                    damagedLight++;
                    break;
            }
        }

        while ((attrition + damagedBadly + damagedModerate + damagedLight) > forceSize) {
            if (damagedLight > 0) {
                damagedLight--;
            } else if (damagedModerate > 0) {
                damagedModerate--;
            } else if (damagedBadly > 0) {
                damagedBadly--;
            } else {
                attrition--;
            }
        }

        forceDamage.put("attrition", attrition);
        forceDamage.put("damagedLight", damagedLight);
        forceDamage.put("damagedModerate", damagedModerate);
        forceDamage.put("damagedBadly", damagedBadly);

        return forceDamage;
    }

    private static HashMap<String, Integer> processEngagement(int loyalistAttackDice, int loyalistDefenceDice, int rebelAttackDice, int rebelDefenceDice) {
        HashMap<String, Integer> combatResults = new HashMap<>();

        boolean concludeEngagement = false;
        int attrition = 0;
        int loyalistVictory = 0;
        int rebelVictory = 0;

        while (!concludeEngagement) {
            int loyalistAttack = Compute.d6(loyalistAttackDice);
            int loyalistDefence = Compute.d6(loyalistDefenceDice);
            int rebelAttack = Compute.d6(rebelAttackDice);
            int rebelDefence = Compute.d6(rebelDefenceDice);

            loyalistVictory = 0;
            rebelVictory = 0;

            if (loyalistAttack > rebelDefence) {
                if (loyalistAttack < (rebelDefence * 1.25)) {
                    attrition++;
                }

                loyalistVictory = 1;
            }

            if (rebelAttack > loyalistDefence) {
                if (rebelAttack < (loyalistDefence * 1.25)) {
                    attrition++;
                }

                rebelVictory = 1;
            }

            if (loyalistVictory == rebelVictory) {
                attrition++;
            } else {
                concludeEngagement = true;
            }
        }

        combatResults.put("attrition", attrition);
        combatResults.put("loyalistVictory", loyalistVictory);

        return combatResults;
    }

    /**
     * Calculates a faction's abstract battle statistics based on the given combatants, force size, and battle value.
     *
     * @param combatants the list of combatants participating in the battle
     * @param forceSize the size of the force
     * @param battleValue the value of the battle
     * @return a HashMap containing the statistics of the battle
     */
    private static HashMap<String, Integer> getAbstractBattleStatistics(List<Person> combatants, Integer forceSize, Integer battleValue) {
        HashMap<String, Integer> statistics = new HashMap<>();

        // TODO make the dividers Campaign Options
        int loyalistLeadership = (int) combatants.stream().filter(person -> person.getRank().isOfficer()).count() / (forceSize / 12);
        int loyalistMedical =  (int) combatants.stream().filter(person -> (person.getPrimaryRole().isDoctor())).count() / (forceSize / 12);
        int loyalistAdministration = (int) combatants.stream().filter(person -> (person.getPrimaryRole().isAdministrator())).count() / (forceSize / 3);
        int loyalistTech = (int) combatants.stream().filter(person -> (person.getPrimaryRole().isTech())).count() / (forceSize / 6);

        int attackDice = (battleValue / 250) + loyalistLeadership + loyalistAdministration;
        int defenceDice = (battleValue / 250) + loyalistTech + loyalistMedical;

        statistics.put("attackDice", attackDice);
        statistics.put("defenceDice", defenceDice);

        return statistics;
    }

    /**
     * Calculates the target number for civil war loyalty checks
     *
     * @param campaign the ongoing campaign
     * @param person the person for which loyalty is being tested
     * @return the target number for a civil war loyalty check
     * @throws IllegalStateException if the loyalty value is unexpected
     */
    private static int getCivilWarTargetNumber(Campaign campaign, Person person) {
        int modifier = 0;

        if (campaign.getCampaignOptions().getMoraleModifierLeadershipMethod().isIronFist()) {
            modifier += 2;
        }

        if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            switch (person.getLoyalty()) {
                case -3:
                    return 6 + modifier;
                case -2:
                    return 5 + modifier;
                case -1:
                case 0:
                    return 4 + modifier;
                case 1:
                    return 3 + modifier;
                case 2:
                    return 2 + modifier;
                case 3:
                    return 1 + modifier;
                default:
                    throw new IllegalStateException("Unexpected value in getCivilWarTargetNumber: " + person.getLoyalty());
            }
        }
        return 4 + modifier;
    }

    /**
     * Retrieves the units that are eligible to participate in the civil war based on the provided personnel.
     * Multi-crewed units perform a vote to determine which side they join.
     *
     * @param personnel A list of personnel (should all belong to the same mutiny faction.
     * @param isLoyalists A boolean value indicating whether to retrieve units for loyalists or rebels.
     * @return A HashMap of units and their corresponding battle values.
     */
    private static HashMap<Unit, Integer> getUnits(List<Person> personnel, boolean isLoyalists) {
        HashMap<Unit, Integer> forces = new HashMap<>();

        for (Person person: personnel) {
            if (person.getUnit() != null) {
                Unit unit = person.getUnit();

                if ((unit.getEntity().isJumpShip()) || (unit.getEntity().isWarShip()) || (unit.getEntity().isSupportVehicle())) {
                    continue;
                }

                // We only care about the commander, as this allows us to ensure each Unit is only counted once.
                // We also check to ensure the unit isn't already deployed, or too damaged to fight.
                if ((unit.isCommander(person)) && (!unit.isDeployed()) && (!unit.getEntity().isCrippled()) && (!unit.getEntity().isDmgHeavy())) {
                    int loyalVoteCount = 0;
                    int rebelVoteCount = 0;

                    for (Person crew : unit.getCrew()) {
                        if (personnel.contains(crew)) {
                            if (isLoyalists) {
                                loyalVoteCount++;
                            } else {
                                rebelVoteCount++;
                            }
                        }
                    }

                    // if the votes are equal, the unit abstains from the conflict
                    if (loyalVoteCount > rebelVoteCount) {
                        if (isLoyalists) {
                            forces.put(unit, unit.getEntity().calculateBattleValue(true, false));
                        }
                    } else if (loyalVoteCount < rebelVoteCount) {
                        if (!isLoyalists) {
                            forces.put(unit, unit.getEntity().calculateBattleValue(true, false));
                        }
                    }
                }
            }
        }

        return forces;
    }
}
