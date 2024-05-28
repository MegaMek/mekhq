package mekhq.campaign.personnel.turnoverAndRetention.Morale;

import megamek.common.Compute;
import megamek.common.MechSummaryCache;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.market.enums.UnitMarketType;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static mekhq.campaign.personnel.turnoverAndRetention.Morale.MoraleController.getPercentageModifier;
import static mekhq.campaign.personnel.turnoverAndRetention.Morale.MoraleController.reclaimOriginalUnit;

public class Desertion {
    /**
     * Processes desertion for a person.
     *
     * @param campaign       the current campaign
     * @param deserters      the potential deserters and their desertion rolls
     * @param targetNumber   the target number for desertion
     * @param resources      the resource bundle for localized messages
     */
    private static void processDesertion(Campaign campaign, HashMap<Person, Integer> deserters, int targetNumber,
                                         List<Unit> possibleTheftTargets, ResourceBundle resources) {
        // morale is used to determine whether a theft occurs
        int morale = campaign.getMorale() / 10;

        // if a theft occurs, it will take an item from one of these lists

        // we assume units with a crew size > 5 cannot be stolen by one person,
        // neither can units that are currently unavailable
        ArrayList<Unit> theftTargetsUnits = campaign.getUnits().stream()
                .filter(unit -> (!unit.isAvailable()) && (unit.getFullCrewSize() < 6))
                .collect(Collectors.toCollection(ArrayList::new));


        // here we collect a list of parts that are available to be stolen
        ArrayList<Part> theftTargetsParts = campaign.getWarehouse().getSpareParts().stream()
                .filter(part -> part.getDaysToArrival() == 0)
                .filter(part -> part.getDaysToWait() == 0)
                .filter(part -> !part.isBeingWorkedOn())
                .filter(part -> !part.needsFixing())
                .filter(part -> !part.isOmniPodded())
                .collect(Collectors.toCollection(ArrayList::new));

        // we make a list of thieves, so they can be processed at the same time
        List<Person> thieves = new ArrayList<>();

        // next we check what type of desertion is occurring: going AWOL, deserting, or deserting with theft
        for (Person person : deserters.keySet()) {
            int roll = deserters.get(person);

            // the Iron Fist leadership style makes desertions worse
            if (campaign.getCampaignOptions().getMoraleModifierLeadershipMethod().isIronFist()) {
                roll -= 1;
            }

            // if margin of failure is 1-2 person goes AWOL instead of deserting
            if ((targetNumber - roll) >= 2) {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.AWOL);
                person.setAwolDays(Compute.d6(2));

                return;
            }

            // otherwise, reclaim original unit
            if (person.getOriginalUnitId() != null) {
                if (reclaimOriginalUnit(campaign, person)) {
                    campaign.addReport(person.getHyperlinkedFullTitle() + ' '
                            + String.format(resources.getString("reclaimSuccessful.text"),
                            campaign.getUnit(person.getOriginalUnitId())));
                } else {
                    campaign.addReport(person.getHyperlinkedFullTitle() + ' '
                            + resources.getString("reclaimFailed.text"));

                    // if we're unable to reclaim original unit, roll is set to 0 to force a theft
                    roll = 0;
                }
            }

            // if margin of failure is 4+ a theft occurs
            if ((morale - roll) >= 4) {
                thieves.add(person);
            }
        }

        if (!thieves.isEmpty()) {
            theftController(campaign, thieves, theftTargetsParts, theftTargetsUnits, resources);
        }
    }

    /**
     * Processes the theft of petty objects, units, money, or spare parts
     *
     * @param campaign The current campaign
     * @param thieves The list of thieves involved in the theft
     * @param theftTargetsParts The list of parts targeted for theft
     * @param theftTargetsUnits The list of units targeted for theft
     * @param resources The ResourceBundle containing game resources
     */
    static void theftController(Campaign campaign, List<Person> thieves, List<Part> theftTargetsParts, ArrayList<Unit> theftTargetsUnits, ResourceBundle resources) {
        for (Person ignored : thieves) {
            boolean theftComplete = false;

            // we want to keep rolling for a theft until a successful one occurs.
            while (theftComplete) {
                int roll = Compute.randomInt(15);

                switch (roll) {
                    case 0:
                        // unit theft
                        if ((campaign.getCampaignOptions().isUseTheftUnit()) && (!theftTargetsUnits.isEmpty())) {
                            theftTargetsUnits.remove(processUnitTheft(campaign, theftTargetsUnits, resources));
                            theftComplete = true;
                        }
                        break;
                    case 1:
                    case 2:
                        // money theft
                        if (campaign.getCampaignOptions().isUseTheftMoney()) {
                            theftComplete = processMoneyTheft(campaign, resources);
                        }
                        break;
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        // part theft
                        if ((campaign.getCampaignOptions().isUseTheftParts()) && (!theftTargetsParts.isEmpty())) {
                            theftTargetsParts = processPartTheft(campaign, theftTargetsParts, resources);
                            theftComplete = true;
                        }
                        break;
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                        // petty theft
                        processPettyTheft(campaign, resources);
                        theftComplete = true;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value in theftController: " + roll);
                }
            }
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
    static Unit processUnitTheft(Campaign campaign, List<Unit> possibleTheftTargets, ResourceBundle resources) {
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
     * @return true if money theft was successfully processed, false otherwise
     */
    static boolean processMoneyTheft(Campaign campaign, ResourceBundle resources) {
        int theftPercentage = campaign.getCampaignOptions().getTheftValue() + getPercentageModifier();

        Money theft = campaign.getFunds()
                .multipliedBy(theftPercentage)
                .dividedBy(100)
                .round();

        if (theft.isPositive()) {
            campaign.getFinances().debit(TransactionType.THEFT, campaign.getLocalDate(), theft,
                    String.format(resources.getString("desertionTheftTransactionReport.text"),
                            FinancialInstitutions.randomFinancialInstitution(campaign.getLocalDate()).toString()));

            campaign.addReport(String.format(String.format(resources.getString("desertionTheftMoney.text"), theft.getAmount())));

            return true;
        } else {
            return false;
        }
    }

    /**
     * Processes the theft of parts.
     *
     * @param campaign           the current campaign
     * @param theftTargetsParts  the list of parts targeted for theft
     * @param resources          the ResourceBundle containing game resources
     * @return a list of parts that have been stolen
     */
    static List<Part> processPartTheft(Campaign campaign, List<Part> theftTargetsParts, ResourceBundle resources) {
        // how many parts should be stolen?
        int theftCount = 1;

        if (campaign.getCampaignOptions().getTheftPartsDiceCount() != 0) {
            theftCount += IntStream
                    .range(0, campaign.getCampaignOptions()
                            .getTheftPartsDiceCount()).map(roll -> Compute.randomInt(3) + 1)
                    .sum();
        }

        HashMap<Part, Integer> stolenItems = new HashMap<>();

        for (int theft = 0; theft < theftCount; theft++) {
            int itemCount;

            // if everything has been stolen the would-be thief will leave empty-handed
            if (!theftTargetsParts.isEmpty()) {
                // pick the part to be stolen
                Part stolenPart = theftTargetsParts.get(new Random().nextInt(theftTargetsParts.size()));

                // how many parts should be stolen?
                if ((stolenPart instanceof AmmoStorage) || (stolenPart instanceof Armor)) {
                    itemCount = Compute.d6(2);

                    if (itemCount > stolenPart.getQuantity()) {
                        itemCount = stolenPart.getQuantity();
                    }
                } else {
                    itemCount = 1;
                }

                // steal the part
                if (stolenPart instanceof AmmoStorage) {
                    campaign.getWarehouse().removeAmmo((AmmoStorage) stolenPart, itemCount);
                } else if (stolenPart instanceof Armor) {
                    campaign.getWarehouse().removeArmor((Armor) stolenPart, itemCount);
                } else {
                    campaign.getWarehouse().getPart(stolenPart.getId());
                }

                // add the item and count to our list of stolen items
                stolenItems.put(stolenPart, itemCount);

                // after each theft we need to rebuild our list of spare parts
                theftTargetsParts = campaign.getWarehouse().getSpareParts().stream()
                        .filter(part -> part.getDaysToArrival() == 0)
                        .filter(part -> part.getDaysToWait() == 0)
                        .filter(part -> !part.isBeingWorkedOn())
                        .filter(part -> !part.needsFixing())
                        .filter(part -> !part.isOmniPodded())
                        .collect(Collectors.toCollection(ArrayList::new));
            }
        }

        campaign.addReport(stolenItems.keySet().stream()
                .map(part -> ' ' + part.getName() + 'x' + stolenItems.get(part))
                .collect(Collectors.joining("", resources.getString("desertionTheftParts.text"), "")));

        return theftTargetsParts;
    }

    /**
     * This method is used to process a petty theft incident in a company.
     * It randomly selects an item from a list of stolen items and adds the item to the campaign report.
     *
     * @param campaign   The campaign object to add the report to.
     * @param resources  The ResourceBundle object to retrieve localized strings.
     */
    static void processPettyTheft(Campaign campaign, ResourceBundle resources) {
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
}
