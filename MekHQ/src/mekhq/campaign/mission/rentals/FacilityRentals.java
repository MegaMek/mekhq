/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.mission.rentals;

import static java.lang.Math.max;
import static mekhq.MHQConstants.CONFIRMATION_CONTRACT_RENTAL;
import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import megamek.common.units.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.RepairStatusChangedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.dialog.BayRentalDialog;
import mekhq.gui.dialog.ContractStartRentalDialog;

/**
 * Handles rental opportunities and transactions for various campaign facilities such as repair bays, hospital beds,
 * kitchens, and holding cells.
 *
 * <p>Provides utility methods for offering facility rentals, calculating costs, and managing rental-related finances
 * and reporting within the campaign context.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class FacilityRentals {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FacilityRentals";

    private static final int FACTORY_CONDITIONS_MULTIPLIER = 20;
    private static final int LARGE_VESSEL_MULTIPLIER = 10;
    private static final int CAPACITY_INCREASE_HOSPITALS = 25; // One MASH Theater
    private static final int CAPACITY_INCREASE_KITCHENS = 150; // One Field Kitchen
    private static final int CAPACITY_INCREASE_SECURITY = 35; // One squad of 7 soldiers

    public static int getCapacityIncreaseFromRentals(List<Contract> activeContracts, ContractRentalType rentalType) {
        if (rentalType == ContractRentalType.MAINTENANCE_BAYS || rentalType == ContractRentalType.FACTORY_CONDITIONS) {
            return 0;
        }

        int capacityMultiplier = switch (rentalType) {
            case HOSPITAL_BEDS -> CAPACITY_INCREASE_HOSPITALS;
            case KITCHENS -> CAPACITY_INCREASE_KITCHENS;
            case HOLDING_CELLS -> CAPACITY_INCREASE_SECURITY;
            default -> 0;
        };

        int rentedFacilities = getRentedFacilities(activeContracts, rentalType);
        return rentedFacilities * capacityMultiplier;
    }

    private static int getRentedFacilities(List<Contract> activeContracts, ContractRentalType rentalType) {
        int rentedFacilities = 0;
        for (Contract contract : activeContracts) {
            rentedFacilities += switch (rentalType) {
                case HOSPITAL_BEDS -> contract.getHospitalBedsRented();
                case KITCHENS -> contract.getKitchensRented();
                case HOLDING_CELLS -> contract.getHoldingCellsRented();
                default -> 0;
            };
        }
        return rentedFacilities;
    }

    public static void offerContractRentalOpportunity(Campaign campaign, Contract contract) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        int hospitalCost = campaignOptions.getRentedFacilitiesCostHospitalBeds();
        int kitchenCost = campaignOptions.getRentedFacilitiesCostKitchens();
        int holdingCellCost = campaignOptions.getRentedFacilitiesCostHoldingCells();

        // If all rentals are disabled, we're just going to back out entirely
        if ((hospitalCost + kitchenCost + holdingCellCost) == 0) {
            return;
        }

        boolean wasRentConfirmed = false;
        boolean wasConfirmedOverall = false;
        ContractStartRentalDialog offerDialog;
        while (!wasConfirmedOverall) {
            new ContractStartRentalDialog(campaign, contract, hospitalCost, kitchenCost, holdingCellCost);

            if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_CONTRACT_RENTAL)) {
                ImmersiveDialogConfirmation confirmation = new ImmersiveDialogConfirmation(campaign,
                      CONFIRMATION_CONTRACT_RENTAL);
                wasConfirmedOverall = confirmation.wasConfirmed();
            } else {
                wasConfirmedOverall = true;
            }
        }

        contract.setHospitalBedsRented(ContractStartRentalDialog.getHospitalSpinnerValue());
        contract.setKitchensRented(ContractStartRentalDialog.getKitchensSpinnerValue());
        contract.setHoldingCellsRented(ContractStartRentalDialog.getSecuritySpinnerValue());
    }

    /**
     * Offers a rental opportunity for repair bays or factory conditions bays.
     *
     * <p>Presents dialog, handles cost calculation, and attempts to debit the player's account.</p>
     *
     * @param campaign        the active campaign
     * @param unitCount       the number of units intended for bay rental
     * @param largeCraftCount the number of large craft intended for bay rental
     * @param rentalType      the type of bay rental (maintenance or factory conditions)
     *
     * @return {@code true} if the transaction completes successfully
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static boolean offerBayRentalOpportunity(Campaign campaign, int unitCount, int largeCraftCount,
          ContractRentalType rentalType) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        int baseCost = campaignOptions.getRentedFacilitiesCostRepairBays();
        if (baseCost <= 0) { // This rental option is disabled
            return true;
        }

        // This shouldn't be able to go into negative, but we use max just in case
        int nonLargeCraft = max(0, unitCount - largeCraftCount);
        int nonLargeCraftCost = baseCost * nonLargeCraft;
        int largeCraftCost = baseCost * LARGE_VESSEL_MULTIPLIER * largeCraftCount;
        if (rentalType == ContractRentalType.FACTORY_CONDITIONS) {
            nonLargeCraftCost *= FACTORY_CONDITIONS_MULTIPLIER;
            largeCraftCost *= FACTORY_CONDITIONS_MULTIPLIER;
        }

        Money totalCost = Money.of(nonLargeCraftCost + largeCraftCost);
        if (!presentBayRentDialog(campaign, totalCost)) { // The player chose not to rent anything
            return false;
        }

        // Returns false if the player cannot afford the rental
        if (!performRentalTransaction(campaign.getFinances(), campaign.getLocalDate(), totalCost,
              ContractRentalType.MAINTENANCE_BAYS)) {
            String report = getFormattedTextAt(RESOURCE_BUNDLE, "FacilityRentals.bay.unableToAfford",
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG, totalCost.toAmountString());
            campaign.addReport(FINANCES, report);

            return false;
        }

        return true;
    }

    /**
     * Presents a dialog to the user for confirming bay rental costs.
     *
     * @param campaign   the {@link Campaign} context for the rental operation
     * @param rentalCost the {@link Money} amount representing the bay rental cost to display
     *
     * @return {@code true} if the user confirmed the bay rental; {@code false} if declined
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static boolean presentBayRentDialog(Campaign campaign, Money rentalCost) {
        BayRentalDialog offerDialog = new BayRentalDialog(campaign, rentalCost);
        return offerDialog.wasConfirmed();
    }

    /**
     * Calculates the total rental cost for a group of active contracts and a given rental type.
     *
     * @param cost            the cost per rental unit
     * @param activeContracts list of active contracts
     * @param rentalType      the facility type being calculated
     *
     * @return the total rental cost as a {@link Money} object
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static Money calculateContractRentalCost(int cost, List<Contract> activeContracts,
          ContractRentalType rentalType) {
        int rentalCount = getRentedFacilities(activeContracts, rentalType);

        return Money.of(cost * rentalCount);
    }

    /**
     * Debits the finances for all contract rentals (hospitals, kitchens, holding cells) for the current day.
     *
     * <p>Reports errors for each type (if payment unsuccessful).</p>
     *
     * @param finances         the finances object to debit from
     * @param today            the date of transaction
     * @param hospitalCosts    the rental cost for hospital beds
     * @param kitchenCosts     the rental cost for kitchens
     * @param holdingCellCosts the rental cost for holding cells
     *
     * @return list of error report strings, if any
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static List<String> payForAllContractRentals(Finances finances, LocalDate today, Money hospitalCosts,
          Money kitchenCosts, Money holdingCellCosts) {
        List<String> reports = new ArrayList<>();

        // Will return false if the payment was unsuccessful
        if (!performRentalTransaction(finances, today, hospitalCosts, ContractRentalType.HOSPITAL_BEDS)) {
            String report = getFailedTransactionReport(ContractRentalType.HOSPITAL_BEDS, hospitalCosts);
            reports.add(report);
        }

        if (!performRentalTransaction(finances, today, kitchenCosts, ContractRentalType.KITCHENS)) {
            String report = getFailedTransactionReport(ContractRentalType.KITCHENS, kitchenCosts);
            reports.add(report);
        }

        if (!performRentalTransaction(finances, today, holdingCellCosts, ContractRentalType.HOLDING_CELLS)) {
            String report = getFailedTransactionReport(ContractRentalType.HOLDING_CELLS, holdingCellCosts);
            reports.add(report);

        }

        return reports;
    }

    /**
     * Generates a formatted report message when a rental transaction fails due to insufficient funds.
     *
     * @param hospitalBeds  the type of facility (rental type) for which the transaction failed
     * @param hospitalCosts the amount of money required for the rental
     *
     * @return a localized, formatted string describing the failed transaction and required cost
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getFailedTransactionReport(ContractRentalType hospitalBeds, Money hospitalCosts) {
        String facilityName = getTextAt(RESOURCE_BUNDLE, "ContractRentalType." + hospitalBeds.name());
        return getFormattedTextAt(RESOURCE_BUNDLE, "FacilityRentals.other.unableToAfford",
              spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG, facilityName,
              hospitalCosts.toAmountString());
    }

    /**
     * Calculates and performs payment for all rented maintenance or factory repair bays for all units in the hangar.
     *
     * <p>If total available funds are exceeded, units revert to a fallback repair site.</p>
     *
     * @param campaign the campaign in which to process bay rental payment
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void payForAllRentedBays(Campaign campaign) {
        Finances finances = campaign.getFinances();
        LocalDate today = campaign.getLocalDate();
        Money totalCharge = getTotalRentSumFromRentedBays(campaign, finances);

        if (!totalCharge.isZero()) {
            performRentalTransaction(finances, today, totalCharge, ContractRentalType.MAINTENANCE_BAYS);
        }
    }

    /**
     * Calculates the total rental sum owed for all currently rented maintenance and factory bays.
     *
     * <p>Iterates over all units in the campaign hangar, sums the rental cost for those at qualifying repair sites,
     * and applies fallback site logic if sufficient funds are not available.</p>
     *
     * @param campaign the current {@link Campaign} context
     * @param finances the {@link Finances} object representing current campaign funds
     *
     * @return the total {@link Money} amount owed for rented bays, or {@code Money.zero()} if rental costs are disabled
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static Money getTotalRentSumFromRentedBays(Campaign campaign, Finances finances) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        int costPerBay = campaignOptions.getRentedFacilitiesCostRepairBays();
        if (costPerBay <= 0) { // Costs have been disabled, so we're not going to perform any actions
            return Money.zero();
        }

        List<Mission> activeMissions = campaign.getActiveMissions(false);
        Money totalAvailableFunds = finances.getBalance();
        Collection<Unit> units = campaign.getHangar().getUnits();

        Money totalCharge = Money.zero();

        int fallbackSite = getFallbackRepairSite(activeMissions);
        for (Unit unit : units) {
            if (shouldBeIgnoredByBayRentals(unit)) {
                continue;
            }

            int unitSite = unit.getSite();
            if (unitSite == Unit.SITE_FACILITY_MAINTENANCE) {
                totalCharge = updateTotalCharge(campaign, costPerBay, unit, totalCharge, totalAvailableFunds,
                      fallbackSite);
            } else if (unitSite == Unit.SITE_FACTORY_CONDITIONS) {
                int cost = costPerBay * FACTORY_CONDITIONS_MULTIPLIER;
                totalCharge = updateTotalCharge(campaign, cost, unit, totalCharge, totalAvailableFunds, fallbackSite);
            }
        }

        return totalCharge;
    }

    /**
     * Determines whether a unit should be ignored for bay rental cost calculation (e.g., mothballed or large craft).
     *
     * @param unit the unit to check
     *
     * @return {@code true} if the unit should be ignored
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static boolean shouldBeIgnoredByBayRentals(Unit unit) {
        if (unit.isMothballed()) {
            return true;
        }

        return false;
    }

    /**
     * Updates the total repair bay rental charge for a unit, handling overflow, and fallback site logic.
     *
     * <p>Sets the repair site of the unit to a fallback site if funds are insufficient.</p>
     *
     * @param campaign            the campaign context
     * @param cost                the cost to add for the unit
     * @param unit                the unit being processed
     * @param totalCharge         current accumulated total
     * @param totalAvailableFunds current available funds for rentals
     * @param fallbackSite        site used if payment is impossible
     *
     * @return updated total charge (unchanged if fallback applied)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static Money updateTotalCharge(Campaign campaign, int cost, Unit unit, Money totalCharge,
          Money totalAvailableFunds, int fallbackSite) {
        Money newTotalCharge = totalCharge.plus(cost);
        if (newTotalCharge.isGreaterThan(totalAvailableFunds)) {
            String previousSiteName = unit.getCurrentSiteName();
            unit.setSite(fallbackSite);
            String report = getFormattedTextAt(RESOURCE_BUNDLE, "FacilityRentals.bay.unableToAffordUpkeep",
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG, previousSiteName,
                  unit.getHyperlinkedName(), unit.getCurrentSiteName());
            campaign.addReport(FINANCES, report);

            MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
        } else {
            totalCharge = newTotalCharge;
        }

        return totalCharge;
    }

    /**
     * Gets the best fallback repair site from the list of active missions, favoring higher-priority sites.
     *
     * <p>Returns {@link Unit#SITE_FACILITY_BASIC} if no missions are active.</p>
     *
     * @param activeMissions list of currently active missions
     *
     * @return fallback repair site constant
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getFallbackRepairSite(List<Mission> activeMissions) {
        if (activeMissions.isEmpty()) {
            return Unit.SITE_FACILITY_BASIC;
        }

        int fallbackSite = Unit.SITE_IMPROVISED;
        for (Mission contract : activeMissions) {
            int newSite = contract.getRepairLocation();
            if (newSite > fallbackSite) {
                fallbackSite = newSite;
            }
        }

        return fallbackSite;
    }

    /**
     * Performs a rental transaction by debiting the corresponding amount for the specified rental type.
     *
     * <p>Adjusts rentalType for historical display if needed.</p>
     *
     * @param finances   finances object to debit from
     * @param today      date of the transaction
     * @param rentalCost amount to be debited
     * @param rentalType type of rental (MAINTENANCE_BAYS, FACTORY_CONDITIONS, etc.)
     *
     * @return {@code true} if the debit was successful
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static boolean performRentalTransaction(Finances finances, LocalDate today, Money rentalCost,
          ContractRentalType rentalType) {
        if (rentalType == ContractRentalType.FACTORY_CONDITIONS) {
            rentalType = ContractRentalType.MAINTENANCE_BAYS; // No text differentiates Factory and Maintenance
        }

        return finances.debit(TransactionType.RENT, today, rentalCost,
              getFormattedTextAt(RESOURCE_BUNDLE, "FacilityRentals.rental." + rentalType, rentalCost.toAmountString()));
    }

    /**
     * Processes a request to change bay assignments for the specified units, offering bay rental opportunities
     * if allowed by the current campaign state.
     * <p>
     * Bay rentals are only permitted if the campaign is either off-contract or on a garrison-type contract,
     * and the campaign's location is planetside. If these conditions are not met, a dialog is shown to the user
     * indicating that no facilities are available.
     * </p>
     *
     * @param campaign      the current campaign context
     * @param selectedUnits the units for which bay changes are requested
     * @param bayType       the type of bay being requested (e.g., {@link Unit#SITE_FACILITY_MAINTENANCE}, {@link Unit#SITE_FACTORY_CONDITIONS})
     * @return {@code true} if the bay change process can proceed; {@code false} if not allowed (e.g., due to contract or location restrictions)
     */
    public static boolean processBayChangeRequest(Campaign campaign, Unit[] selectedUnits, int bayType) {
        List<AtBContract> activeAtBContracts = campaign.getActiveAtBContracts();
        boolean isOffContractOrOnGarrison = activeAtBContracts.isEmpty();

        for (AtBContract atBContract : activeAtBContracts) {
            if (atBContract.getContractType().isGarrisonType()) {
                isOffContractOrOnGarrison = true;
                break;
            }
        }

        if (!isOffContractOrOnGarrison || !campaign.getLocation().isOnPlanet()) {
            BayRentalDialog.showNoFacilitiesAvailableDialog(campaign);
            return false;
        }

        // This counts how many units are eligible and how many are eligible and a large craft. We handle
        // it this way to allow us to fetch the counts in a single pass
        Predicate<Unit> eligibleForBayRental =
              unit -> !FacilityRentals.shouldBeIgnoredByBayRentals(unit);
        long[] counts = Arrays.stream(selectedUnits)
                              .filter(eligibleForBayRental)
                              .collect(() -> new long[2], (results, unit) -> {
                                  if (!unit.isDeployed()) {
                                      results[0]++; // eligible
                                      Entity entity = unit.getEntity();
                                      if (entity != null && entity.isLargeCraft()) {
                                          results[1]++; // large vessel
                                      }
                                  }
                              }, (a, b) -> {
                                  a[0] += b[0];
                                  a[1] += b[1];
                              });

        int eligibleUnitCount = (int) counts[0];
        int eligibleLargeVesselCount = (int) counts[1];

        ContractRentalType rentalType = switch (bayType) {
            case Unit.SITE_FACILITY_MAINTENANCE -> ContractRentalType.MAINTENANCE_BAYS;
            case Unit.SITE_FACTORY_CONDITIONS -> ContractRentalType.FACTORY_CONDITIONS;
            default -> null; // Should never happen as we're already filtering out invalid sites
        };

        if (rentalType == null) {
            return true; // We don't want a bug preventing bay changes.
        }

        return FacilityRentals.offerBayRentalOpportunity(campaign,
              eligibleUnitCount,
              eligibleLargeVesselCount,
              rentalType);
    }
}
