package mekhq.campaign.personnel.turnoverAndRetention;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.UnitType;
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
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;

import java.util.*;
import java.util.stream.Collectors;

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

        ArrayList<Unit> theftTargets = new ArrayList<>();

        if (isDesertion) {
            theftTargets = campaign.getHangar().getUnits().stream()
                    .filter(unit -> (!unit.isDamaged()) || (!unit.isDeployed()) || (!unit.getEntity().isLargeCraft()) || (!unit.getEntity().isWarShip()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        boolean someoneHasDeserted = false;
        boolean someoneHasMutinied = false;

        List<Person> loyalists = new ArrayList<>();
        List<Person> rebels = new ArrayList<>();

        for (Person person : filteredPersonnel) {
            int modifier = getMoraleCheckModifiers(campaign, person, isDesertion, loyalty);
            int roll = Compute.d6(2) + modifier;

            if (roll <= targetNumber) {
                if (isDesertion) {
                    if ((processDesertion(campaign, person, roll, targetNumber, morale, theftTargets, resources))
                            && (!someoneHasDeserted)) {
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

    /**
     * Processes desertion for a person.
     *
     * @param campaign       the current campaign
     * @param person         the potential deserter
     * @param roll           the desertion roll result
     * @param targetNumber   the target number for desertion
     * @param morale         the morale value
     * @param unitList       the list of units
     * @param resources      the resource bundle for localized messages
     * @return true if desertion occurred, false otherwise
     */
    private static boolean processDesertion(Campaign campaign, Person person, int roll, double targetNumber,
                                            double morale, ArrayList<Unit> unitList, ResourceBundle resources) {
        if (roll <= (targetNumber - 2)) {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.DESERTED);

            if ((roll <= (morale - 4)) && (campaign.getCampaignOptions().isUseTheftUnit()) && (!unitList.isEmpty())) {
                processUnitTheft(campaign, unitList, resources);
            } else if ((roll <= (morale - 3)) && (campaign.getCampaignOptions().isUseTheftMoney())) {
                processMoneyTheft(campaign, resources);
            } else if (roll <= (morale - 2)) {
                processPettyTheft(campaign, resources);
            }

            return true;
        } else {
            person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.AWOL);
            person.setAwolDays(Compute.d6(2));

            return false;
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

            campaign.addReport(String.format(resources.getString("desertionTheftUnitBlackMarket.text"), unitName));
        } else {
            campaign.addReport(String.format(resources.getString("desertionTheftUnit.text"), unitName));
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

        campaign.addReport(String.format(resources.getString("desertionStolen.text"), new Random().nextInt(items.size())));
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
