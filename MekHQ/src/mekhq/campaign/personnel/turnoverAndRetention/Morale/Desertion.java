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
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static mekhq.campaign.personnel.enums.PersonnelStatus.DESERTED;
import static mekhq.campaign.personnel.turnoverAndRetention.Morale.MoraleController.getPercentageModifier;
import static mekhq.campaign.personnel.turnoverAndRetention.Morale.MoraleController.reclaimOriginalUnit;

public class Desertion {
    /**
     * Processes a desertion event in a campaign.
     *
     * @param campaign   The current campaign.
     * @param deserters  A map of deserters and their corresponding desertion rolls.
     * @param targetNumber The target number for determining the severity of the desertion.
     * @param resources  The ResourceBundle containing game resources.
     */
    static void processDesertion(Campaign campaign, HashMap<Person, Integer> deserters, int targetNumber, ResourceBundle resources) {
        // morale is used to determine whether a theft occurs
        int morale = campaign.getMorale() / 10;

        // we start by building our theft target lists.
        // if a theft occurs, it will take an item from one of these lists

        // we assume units with a crew size > 5 cannot be stolen by one person,
        // neither can units that are currently unavailable
        ArrayList<Unit> theftTargetsUnits = campaign.getUnits().stream()
                .filter(unit -> (!unit.isAvailable()) && (unit.getFullCrewSize() < 6))
                .collect(Collectors.toCollection(ArrayList::new));

        // here we collect a list of parts that can be stolen
        ArrayList<Part> theftTargetsParts = campaign.getWarehouse().getSpareParts().stream()
                .filter(part -> part.getDaysToArrival() == 0)
                .filter(part -> part.getDaysToWait() == 0)
                .filter(part -> !part.isBeingWorkedOn())
                .filter(part -> !part.needsFixing())
                .filter(part -> !part.isOmniPodded())
                .collect(Collectors.toCollection(ArrayList::new));

        // we're going to make a list of thieves, so they can be processed at the same time
        List<Person> thieves = new ArrayList<>();

        // next, we check what type of desertion is occurring: going AWOL, deserting, or deserting with theft
        for (Person person : deserters.keySet()) {
            // this allows us to avoid double-handling spouses
            if (person.getStatus() == DESERTED) {
                continue;
            }

            int roll = deserters.get(person);

            // the Iron Fist leadership style makes desertions worse
            if (campaign.getCampaignOptions().getMoraleModifierLeadershipMethod().isIronFist()) {
                roll -= 1;
            }

            // if the margin of failure is 1-2 person goes AWOL instead of deserting
            if ((targetNumber - roll) <= 2) {
                person.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.AWOL);
                person.setAwolDays(Compute.d6(2));

                continue;
            }

            // At this point we know the person is going to desert, so we run with that assumption

            // reclaim the original unit
            if (person.getOriginalUnitId() != null) {
                if (reclaimOriginalUnit(campaign, person)) {
                    campaign.addReport(person.getHyperlinkedFullTitle() + ' ' + String.format(resources.getString("reclaimSuccessful.text"), campaign.getUnit(person.getOriginalUnitId())));
                } else {
                    campaign.addReport(person.getHyperlinkedFullTitle() + ' ' + resources.getString("reclaimFailed.text"));

                    // if we're unable to reclaim the original unit, roll is set to 0 to force a theft
                    roll = 0;
                }
            }

            // if the margin of failure is a 4+, theft occurs
            if ((morale - roll) >= 4) {
                thieves.add(person);
            }

            person.changeStatus(campaign, campaign.getLocalDate(), DESERTED);

            if (person.getGenealogy().getChildren() != null) {
                processChildDesertion(campaign, person, resources);
            }

            if ((person.getGenealogy().hasSpouse()) && (campaign.getCampaignOptions().isUseMoraleModifierMarriage())) {
                Person spouse = person.getGenealogy().getSpouse();

                campaign.addReport(spouse.getHyperlinkedFullTitle() + ' ' + resources.getString("desertionSpouse.text"));

                spouse.changeStatus(campaign, campaign.getLocalDate(), DESERTED);
            }
        }

        if (!thieves.isEmpty()) {
            theftController(campaign, thieves, theftTargetsParts, theftTargetsUnits, resources);
        }
    }

    /**
     * Processes child desertion for a given person in the campaign.
     *
     * @param campaign   the current campaign
     * @param person     the person whose children are being processed for desertion
     * @param resources  the ResourceBundle containing string resources
     */
    private static void processChildDesertion(Campaign campaign, Person person, ResourceBundle resources) {
        for (Person child : person.getGenealogy().getChildren()) {
            if (!child.isChild(campaign.getLocalDate())) {
                continue;
            } else if (isAbsentChild(child)) {
                continue;
            }

            List<Person> parents = child.getGenealogy().getParents();

            boolean singleAbsentParent = false;
            boolean bothAbsentParents = false;

            // orphans do not desert
            if (parents.size() == 1) {
                child.changeStatus(campaign, campaign.getLocalDate(), DESERTED);
            } else if (parents.size() == 2) {
                for (Person parent : parents) {
                    if (isAbsentAdult(parent)) {
                        if (singleAbsentParent) {
                            bothAbsentParents = true;
                        } else {
                            singleAbsentParent = true;
                        }
                    }
                }
            }

            if (bothAbsentParents) {
                child.changeStatus(campaign, campaign.getLocalDate(), DESERTED);
                campaign.addReport(child.getHyperlinkedFullTitle() + ' ' + resources.getString("desertionChild.text"));
            } else if (singleAbsentParent) {
                if (Compute.d6(1) >= 4) {
                    child.changeStatus(campaign, campaign.getLocalDate(), DESERTED);
                    campaign.addReport(child.getHyperlinkedFullTitle() + ' ' + resources.getString("desertionChild.text"));
                }
            }
        }
    }

    /**
     * Determines if an adult person is absent based on their status.
     *
     * @param adult The adult person to check for absence.
     * @return True if the adult is absent, otherwise false.
     */
    public static boolean isAbsentAdult(Person adult) {
        PersonnelStatus status = adult.getStatus();

        return status.isMIA() || status.isPoW() || status.isOnLeave() || status.isAWOL() || status.isStudent() || status.isMissing() || status.isDead() || status.isRetired() || status.isDeserted();
    }

    /**
     * Checks if the child person is considered an absent child.
     *
     * @param child The child to check for absence.
     * @return True if the child is considered absent, otherwise false.
     */
    public static boolean isAbsentChild(Person child) {
        PersonnelStatus status = child.getStatus();

        return status.isMIA() || status.isPoW() || status.isMissing() || status.isDead() || status.isRetired() || status.isDeserted();
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
        Random random = new Random();

        for (Person ignored : thieves) {
            boolean theftComplete = false;

            // we want to keep rolling for a theft until a successful one occurs.
            while (!theftComplete) {
                int roll = Compute.randomInt(15);

                switch (roll) {
                    case 0:
                        // unit theft
                        if ((campaign.getCampaignOptions().isUseTheftUnit()) && (!theftTargetsUnits.isEmpty())) {
                            theftTargetsUnits.remove(processUnitTheft(campaign, theftTargetsUnits, random, resources));
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
                            theftTargetsParts = processPartTheft(campaign, theftTargetsParts, random, resources);
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
                        processPettyTheft(campaign, random, resources);
                        theftComplete = true;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value in theftController: " + roll);
                }
            }
        }
    }

    /**
     * Processes the theft of a unit in a campaign.
     * This method selects a random unit from a list of possible theft targets
     * and performs the theft operation.
     * If the campaign options allow, and the faction is not a clan, the stolen unit may be
     * resold on the black market with a specified resale value.
     * Otherwise, the theft is reported in the campaign report.
     *
     * @param campaign               The current campaign.
     * @param possibleTheftTargets   The list of possible units that can be stolen.
     * @param random                 The random number generator.
     * @param resources              The ResourceBundle containing game resources.
     * @return The stolen unit.
     */
    static Unit processUnitTheft(Campaign campaign, List<Unit> possibleTheftTargets, Random random, ResourceBundle resources) {
        Unit stolenUnit = possibleTheftTargets.get(random.nextInt(possibleTheftTargets.size()));
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

        if (theftPercentage < 1) {
            theftPercentage = 1;
        }

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
     * @param campaign           The current campaign.
     * @param theftTargetsParts  The list of parts targeted for theft.
     * @param random             The random number generator.
     * @param resources          The ResourceBundle containing game resources.
     * @return The updated list of parts after the theft.
     */
    static List<Part> processPartTheft(Campaign campaign, List<Part> theftTargetsParts, Random random, ResourceBundle resources) {
        // how many parts should be stolen?
        int theftCount = 1;

        if (campaign.getCampaignOptions().getTheftPartsDiceCount() != 0) {
            theftCount += IntStream
                    .range(0, campaign.getCampaignOptions()
                            .getTheftPartsDiceCount()).map(roll -> Compute.randomInt(3) + 1)
                    .sum();
        }

        LogManager.getLogger().info(theftCount);

        HashMap<String, Integer> stolenItems = new HashMap<>();

        for (int theft = 0; theft < theftCount; theft++) {
            int itemCount;

            // if everything has been stolen, the would-be thief will leave empty-handed
            if (!theftTargetsParts.isEmpty()) {
                // pick the part to be stolen
                Part stolenPart = theftTargetsParts.get(random.nextInt(theftTargetsParts.size()));

                // how many parts should be stolen?
                if (stolenPart instanceof AmmoStorage) {
                    itemCount = Compute.d6(2);

                    if (itemCount > (stolenPart.getQuantity())) {
                        itemCount = stolenPart.getQuantity();
                    }
                } else if (stolenPart instanceof Armor) {
                    itemCount = Compute.d6(2);

                    if (itemCount > ((Armor) stolenPart).getAmount()) {
                        itemCount = ((Armor) stolenPart).getAmount();
                    }
                } else {
                    itemCount = 1;
                }

                // steal the part/s
                campaign.getWarehouse().removePart(stolenPart, itemCount);

                // add the item and count to our list of stolen items
                if (stolenItems.containsKey(stolenPart.getName())) {
                    stolenItems.put(stolenPart.getName(), stolenItems.get(stolenPart.getName()) + itemCount);
                } else {
                    stolenItems.put(stolenPart.getName(), itemCount);
                }

                // after each theft, we need to rebuild our list of spare parts
                theftTargetsParts = campaign.getWarehouse().getSpareParts().stream()
                        .filter(part -> part.getDaysToArrival() == 0)
                        .filter(part -> part.getDaysToWait() == 0)
                        .filter(part -> !part.isBeingWorkedOn())
                        .filter(part -> !part.needsFixing())
                        .filter(part -> !part.isOmniPodded())
                        .collect(Collectors.toCollection(ArrayList::new));
            }
        }

        String theftReport = stolenItems.keySet().stream()
                .map(stolenItem -> "<li>" + stolenItem + " x" + stolenItems.get(stolenItem) + "</li>")
                .collect(Collectors.joining("", resources.getString("desertionTheftParts.text"), "</ul></html>"));

        campaign.addReport(theftReport);

        return theftTargetsParts;
    }

    /**
     * Processes the theft of petty objects during a campaign.
     *
     * @param campaign   The current campaign.
     * @param random     The random number generator.
     * @param resources  The ResourceBundle containing game resources.
     */
    static void processPettyTheft(Campaign campaign, Random random, ResourceBundle resources) {
        List<String> items = List.of(
                "stapler.text",
                "mascot.text",
                "phones.text",
                "interface.text",
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
                resources.getString(items.get(random.nextInt(items.size())))));
    }
}
