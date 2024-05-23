package mekhq.campaign.personnel.turnoverAndRetention;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Mounted;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.ForceReliabilityMethod;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.MutinySupportDialog;

import java.util.*;
import java.util.stream.Collectors;

import static megamek.common.EntityWeightClass.WEIGHT_LARGE_WAR;

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
    private static Integer getTargetNumber(Campaign campaign, boolean isDesertion) {
        double morale = campaign.getMorale();

        if ((morale >= 1) && (morale < 4)) {
            return 0;
        } else if (morale < 5) {
            if (isDesertion) {
                return 2;
            } else {
                return 0;
            }
        } else if (morale < 7) {
            if (isDesertion) {
                return 5;
            } else {
                return 4;
            }
        } else if (morale == 7) {
            if (isDesertion) {
                return 8;
            } else {
                return 7;
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

        // Loyalty Modifier
        if ((campaign.getCampaignOptions().isUseLoyaltyModifiers()) && (campaign.getCampaignOptions().isUseMoraleModifierLoyalty())) {
            modifier += getLoyaltyModifier(isDesertion, person.getLoyalty());
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

    public static boolean makeMoraleChecks(Campaign campaign, boolean isDesertion) {
        if ((isDesertion) && (!campaign.getCampaignOptions().isUseDesertions())) {
            return isDesertion;
        } else if ((isDesertion) && (!campaign.getLocation().isOnPlanet())) {
            return isDesertion;
        } else if ((!isDesertion) && (!campaign.getCampaignOptions().isUseMutinies())) {
            return isDesertion;
        }

        double targetNumber = getTargetNumber(campaign, isDesertion);

        if (targetNumber == 0) {
            return isDesertion;
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

        ArrayList<Unit> theftTargets = new ArrayList<>();

        if (isDesertion) {
            theftTargets = campaign.getHangar().getUnits().stream()
                    .filter(unit -> (!unit.isDamaged()) && (!unit.isDeployed()) && (!unit.getEntity().isLargeCraft()) && (!unit.getEntity().isWarShip()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        boolean someoneHasDeserted = false;
        boolean someoneHasMutinied = false;

        List<Person> loyalists = new ArrayList<>();
        Person loyalistLeader = campaign.getFlaggedCommander();

        // if there is no Commander, we assume the highest ranked person is the loyalist leader
        if (loyalistLeader == null) {
            campaign.getHighestRankedPerson(filteredPersonnel, true);
        }

        Person rebelLeader;
        HashMap<Person, Integer> rebels = new HashMap<>();

        for (Person person : filteredPersonnel) {
            // the loyalistLeader can't rebel against themselves
            if (Objects.equals(person, loyalistLeader)) {
                loyalists.add(person);
                continue;
            }

            int modifier = getMoraleCheckModifiers(campaign, person, isDesertion, loyalty);

            int firstRoll = Compute.d6(2) + modifier;
            int secondRoll = 0;

            if (isDesertion) {
                secondRoll = Compute.d6(2) + modifier;
            }

            if ((firstRoll <= targetNumber) && (secondRoll <= targetNumber)) {
                if (isDesertion) {
                    if ((processDesertion(campaign, person, secondRoll, targetNumber, theftTargets, resources))
                            && (!someoneHasDeserted)) {
                        someoneHasDeserted = true;
                    }
                } else {
                    rebels.put(person, firstRoll);
                    someoneHasMutinied = true;
                }
            } else {
                loyalists.add(person);
            }
        }

        if (someoneHasMutinied) {
            processMutiny(campaign, loyalistLeader, loyalists, rebels, theftTargets, resources);
            return true;
        } else {
            return someoneHasDeserted;
        }
    }

    /**
     * Processes desertion for a person.
     *
     * @param campaign       the current campaign
     * @param person         the potential deserter
     * @param roll           the desertion roll result
     * @param targetNumber   the target number for desertion=
     * @param unitList       the list of units
     * @param resources      the resource bundle for localized messages
     * @return true if desertion occurred, false otherwise
     */
    private static boolean processDesertion(Campaign campaign, Person person, int roll, double targetNumber,
                                            ArrayList<Unit> unitList, ResourceBundle resources) {
        double morale = campaign.getMorale();

        if (roll <= (targetNumber - 2)) {
            if (campaign.getCampaignOptions().isUseRuleWithIronFist()) {
                morale -= 1;
            }

            if (roll <= (morale - 2)) {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.DESERTED);

                // reclaim original unit (if available)
                if (person.getOriginalUnitId() != null) {
                    morale = reclaimOriginalUnit(campaign, person);
                }

                // check for theft
                if ((roll <= (morale - 4)) && (campaign.getCampaignOptions().isUseTheftUnit()) && (!unitList.isEmpty())) {
                    processUnitTheft(campaign, unitList, resources);
                } else if ((roll <= (morale - 3)) && (campaign.getCampaignOptions().isUseTheftMoney())) {
                    processMoneyTheft(campaign, resources);
                } else if (roll <= (morale - 2)) {
                    processPettyTheft(campaign, resources);
                }

                return true;
            }
        }

        person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.AWOL);
        person.setAwolDays(Compute.d6(2));

        return false;
    }

    /**
     * Reclaims the original unit for a person in a campaign.
     * If the unit is no longer available, return an integer used to reduce effective morale.
     *
     * @param campaign The campaign the person is participating in
     * @param person The person whose original unit is to be reclaimed
     * @return 0 if the original unit is removed successfully, or an integer if removal fails
     */
    private static double reclaimOriginalUnit(Campaign campaign, Person person) {
        UUID originalUnitId = person.getOriginalUnitId();
        int originalUnitWeight = person.getOriginalUnitWeight();

        // this stops support vehicles being over-valued
        if (originalUnitWeight > WEIGHT_LARGE_WAR) {
            originalUnitWeight -= WEIGHT_LARGE_WAR;
        }

        if (!campaign.getUnit(originalUnitId).isDeployed()) {
            try {
                campaign.getHangar().removeUnit(person.getOriginalUnitId());
                return 0;
            } catch (Exception e) {
                return originalUnitWeight;
            }
        } else {
            return originalUnitWeight;
        }
    }

    /**
     * Process theft of units.
     *
     * @param campaign   the campaign from which units or funds are stolen
     * @param unitList   the list of available units
     * @param resources  the resource bundle for retrieving localized strings
     */
    private static void processUnitTheft(Campaign campaign, ArrayList<Unit> unitList, ResourceBundle resources) {
        Unit desiredUnit = unitList.get(new Random().nextInt(unitList.size()));

        StringBuilder unitName = new StringBuilder(desiredUnit.getName());

        if (!Objects.equals(desiredUnit.getFluffName(), "")) {
            unitName.append(' ').append(desiredUnit.getFluffName());
        }

        if ((!campaign.getFaction().isClan()) && (Compute.d6(1) >= 3)) {
            campaign.getUnitMarket().addOffers(campaign, 1, UnitMarketType.BLACK_MARKET, desiredUnit.getEntity().getUnitType(),
                    campaign.getFaction(), desiredUnit.getQuality(), 6);

            campaign.addReport(String.format(resources.getString("desertionTheftBlackMarket.text"), unitName));
        } else {
            campaign.addReport(String.format(resources.getString("desertionTheft.text"), unitName));
        }

        campaign.removeUnit(desiredUnit.getId());
        unitList.remove(desiredUnit);
    }

    /**
     * Processes money theft.
     *
     * @param campaign the campaign for which the theft is being processed
     * @param resources the ResourceBundle containing the necessary resources
     */
    private static void processMoneyTheft(Campaign campaign, ResourceBundle resources) {
        int theftPercentage = campaign.getCampaignOptions().getTheftValue();

        switch(Compute.d6(2)) {
            case 2:
                theftPercentage += 3;
                break;
            case 3:
                theftPercentage += 2;
                break;
            case 4:
            case 5:
                theftPercentage++;
                break;
            case 9:
                theftPercentage--;
                break;
            case 10:
            case 11:
                theftPercentage -= 2;
                break;
            case 12:
                theftPercentage -= 3;
                break;
            default:
                break;
        }

        Money theft = campaign.getFunds().multipliedBy(MathUtility.clamp(theftPercentage, 1, 100) / 100).round();

        if (theft.isPositive()) {
            campaign.getFinances().debit(TransactionType.THEFT, campaign.getLocalDate(), theft, resources.getString("desertionTheftTransactionReport.text"));

            campaign.addReport(String.format(resources.getString("desertionTheftMoney.text"), theft.getAmount()));
        } else {
            processPettyTheft(campaign, resources);
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
        List<String> items = List.of("officeSupplies.text",
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
                "employeeBelongings.text",
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
                "toiletSeats.text");

        campaign.addReport(String.format(resources.getString("desertionTheft.text"), new Random().nextInt(items.size())));
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
    public static void processMoraleChange(Campaign campaign, double steps) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        double morale = campaign.getMorale();
        double change = campaign.getCampaignOptions().getMoraleStepSize() * steps;

        if ((change > 0) && (morale != 1)) {
            campaign.setMorale(MathUtility.clamp(morale - (steps * campaign.getCampaignOptions().getMoraleStepSize()), 1.0, 7.0));
            getMoraleReport(campaign);

            if ((morale >= 5) && ((morale + steps) < 5)) {
                campaign.addReport(resources.getString("moraleReportRecovered.text"));
            }
        } else if ((change < 0) && (morale != 7)) {
            campaign.setMorale(MathUtility.clamp(morale + change, 1.0, 7.0));
            getMoraleReport(campaign);
        }
    }

    private static void processMutiny(Campaign campaign,
                                      Person loyalistLeader, List<Person> loyalists,
                                      HashMap<Person, Integer> rebels,
                                      ArrayList<Unit> unitList,
                                      ResourceBundle resources) {

        // This prevents us from needing to do the full process for tiny mutinies that have no chance of success
        if ((loyalists.size() / 10) > rebels.size()) {
            if (rebels.size() > 1) {
                campaign.addReport(String.format(resources.getString("mutinyThwartedPlural.text"), rebels.size()));
            } else {
                campaign.addReport(String.format(resources.getString("mutinyThwartedSingular.text"), rebels.size()));
            }

            for (Person person : rebels.keySet()) {
                processDesertion(campaign, person, rebels.get(person), getTargetNumber(campaign, true), unitList, resources);
            }

            return;
        }

        // A civil war breaks out.
        List<Person> bystanders = new ArrayList<>();

        // The rebels have already picked their side, so we only need to process the loyalists
        Iterator<Person> iterator = loyalists.iterator();

        while (iterator.hasNext()) {
            Person person = iterator.next();

            int roll = Compute.d6(1);
            int civilWarTargetNumber = getCivilWarTargetNumber(campaign, person);

            if ((roll >= (civilWarTargetNumber - 1)) &&
                    (roll <= (civilWarTargetNumber + 1))) {

                iterator.remove();
                bystanders.add(person);
            }
            else if (roll < (civilWarTargetNumber - 1)) {

                iterator.remove();
                rebels.put(person, 0);
            }
        }

        // We now need to determine the leader of the rebels.
        // This might not always be someone who initially joined the mutiny,
        // in which case we can assume they were persuaded into the role by the original mutineers
        Person rebelLeader = getRebelLeader(campaign, new ArrayList<>(rebels.keySet()));

        // with the line drawn in the sand, we need to assess the forces available to each side
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

        if (campaign.getCampaignOptions().isUseRuleWithIronFist()) {
            modifier++;
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
