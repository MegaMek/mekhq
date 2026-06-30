/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.finances;

import static mekhq.campaign.force.Formation.FORMATION_NONE;
import static mekhq.campaign.market.contractMarket.AlternatePaymentModelValues.adjustValuesForDiminishingReturns;
import static mekhq.campaign.market.contractMarket.AlternatePaymentModelValues.getDiminishingReturnsStart;
import static mekhq.campaign.personnel.ranks.Rank.RWO_MIN;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.common.equipment.Engine;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.HumanResources;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.market.contractMarket.AlternatePaymentModelValues;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.education.EducationController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * Provides accounting for a Campaign.
 */
public record Accountant(Campaign campaign) {
    private static final MMLogger LOGGER = MMLogger.create(Accountant.class);

    final public static int HOUSING_PRISONER_OR_DEPENDENT = 228;
    final public static int HOUSING_ENLISTED = 312;
    final public static int HOUSING_OFFICER = 780;
    final public static int FOOD_PRISONER_OR_DEPENDENT = 120;
    final public static int FOOD_ENLISTED = 240;
    final public static int FOOD_OFFICER = 480;

    private CampaignOptions getCampaignOptions() {
        return campaign().getCampaignOptions();
    }

    private Hangar getHangar() {
        return campaign().getHangar();
    }

    private HumanResources getHumanResources() {
        return campaign.getHumanResources();
    }

    private AbstractLocation getCurrentLocation() {
        return campaign.getCurrentLocation();
    }

    private boolean isClanCampaign() {
        return campaign.isClanCampaign();
    }

    private LocalDate getLocalDate() {
        return campaign.getLocalDate();
    }

    private Faction getFaction() {
        return campaign.getFaction();
    }

    private List<Formation> getAllFormations() {
        return campaign.getAllFormations();
    }

    /**
     * Gets the monthly payroll, including infantry, for the campaign.
     *
     * @return the {@link Money} value of the payroll
     */
    public Money getPayRoll() {
        return payRoll(getCampaignOptions(), getHumanResources(), false, getLocalDate(),
              isClanCampaign());
    }

    /**
     * Gets the monthly payroll for the campaign, optionally excluding infantry.
     *
     * @param noInfantry if {@code true}, soldiers are excluded from the payroll calculation
     *
     * @return the {@link Money} value of the payroll
     */
    public Money getPayRoll(boolean noInfantry) {
        return payRoll(getCampaignOptions(), getHumanResources(), noInfantry, getLocalDate(),
              isClanCampaign());
    }

    /**
     * Static version of {@link #getPayRoll(boolean)}.
     *
     * @param campaignOptions the campaign options used to determine whether salaries are paid
     * @param humanResources  the personnel pool to draw salaries from
     * @param noInfantry      if {@code true}, soldiers are excluded from the payroll calculation
     * @param today           the current campaign date, used for salary calculations
     * @param isClanCampaign  whether the campaign is a Clan campaign
     *
     * @return the {@link Money} value of the payroll, or zero if salaries are not paid
     */
    public static Money payRoll(CampaignOptions campaignOptions, HumanResources humanResources,
          boolean noInfantry, LocalDate today, boolean isClanCampaign) {
        if (campaignOptions.isPayForSalaries()) {
            return theoreticalPayroll(campaignOptions, humanResources, noInfantry, today, isClanCampaign);
        } else {
            return Money.zero();
        }
    }

    /**
     * Calculates the sum of all salary-eligible personnel salaries plus temporary crew pay, regardless of whether the
     * campaign is configured to actually pay salaries.
     *
     * @param campaignOptions the campaign options used for salary calculations
     * @param humanResources  the personnel pool to draw salaries from
     * @param noInfantry      if {@code true}, soldiers are excluded from the calculation
     * @param today           the current campaign date, used for salary calculations
     * @param isClanCampaign  whether the campaign is a Clan campaign
     *
     * @return the total {@link Money} value of salaries and temporary crew pay
     */
    private static Money theoreticalPayroll(CampaignOptions campaignOptions, HumanResources humanResources,
          boolean noInfantry, LocalDate today, boolean isClanCampaign) {
        Money salaries = Money.zero();
        for (Person person : humanResources.getSalaryEligiblePersonnel()) {
            if (!(noInfantry && person.getPrimaryRole().isSoldier())) {
                Money individualSalary = person.getSalary(campaignOptions, today, isClanCampaign);
                salaries = salaries.plus(individualSalary);
            }
        }

        // Add all temporary personnel (medics, astechs, temp crew)
        salaries = salaries.plus(sumTempCrewPay(campaignOptions, humanResources, noInfantry));

        return salaries;
    }

    /**
     * Gets the total monthly maintenance costs for all units that require maintenance and have an assigned tech.
     *
     * @return the {@link Money} value of monthly maintenance costs, or zero if maintenance costs are disabled
     */
    public Money getMaintenanceCosts() {
        return maintenanceCosts(getCampaignOptions(), getHangar());
    }

    /**
     * Static version of {@link #getMaintenanceCosts()}.
     *
     * @param campaignOptions the campaign options used to determine whether maintenance is paid for
     * @param hangar          the hangar containing the units to evaluate
     *
     * @return the {@link Money} value of monthly maintenance costs, or zero if maintenance costs are disabled
     */
    public static Money maintenanceCosts(CampaignOptions campaignOptions, Hangar hangar) {
        if (campaignOptions.isPayForMaintain()) {
            return hangar.getUnitsStream()
                         .filter(u -> u.requiresMaintenance() && (null != u.getTech()))
                         .map(Unit::getMaintenanceCost)
                         .reduce(Money.zero(), Money::plus);
        }
        return Money.zero();
    }

    /**
     * Gets the total weekly maintenance costs for all units in the hangar, regardless of whether maintenance is enabled
     * in the campaign options.
     *
     * @return the {@link Money} value of weekly maintenance costs
     */
    public Money getWeeklyMaintenanceCosts() {
        return weeklyMaintenanceCosts(getHangar());
    }

    /**
     * Static version of {@link #getWeeklyMaintenanceCosts()}.
     *
     * @param hangar the hangar containing the units to evaluate
     *
     * @return the {@link Money} value of weekly maintenance costs
     */
    public static Money weeklyMaintenanceCosts(Hangar hangar) {
        return hangar.getUnitsStream().map(Unit::getWeeklyMaintenanceCost).reduce(Money.zero(), Money::plus);
    }

    /**
     * Gets the monthly overhead expenses for the campaign, calculated as a percentage of the theoretical payroll.
     *
     * @return the {@link Money} value of overhead expenses, or zero if overhead costs are disabled
     */
    public Money getOverheadExpenses() {
        return overheadExpenses(getCampaignOptions(),
              getHumanResources(),
              getLocalDate(),
              isClanCampaign());
    }

    /**
     * Static version of {@link #getOverheadExpenses()}.
     *
     * @param campaignOptions the campaign options used to determine whether overhead is paid for
     * @param humanResources  the personnel pool used to calculate the theoretical payroll
     * @param today           the current campaign date, used for salary calculations
     * @param isClanCampaign  whether the campaign is a Clan campaign
     *
     * @return the {@link Money} value of overhead expenses, or zero if overhead costs are disabled
     */
    public static Money overheadExpenses(CampaignOptions campaignOptions, HumanResources humanResources,
          LocalDate today, boolean isClanCampaign) {
        if (campaignOptions.isPayForOverhead()) {
            return theoreticalPayroll(campaignOptions, humanResources, false, today, isClanCampaign).multipliedBy(0.05);
        } else {
            return Money.zero();
        }
    }

    /**
     * Calculates the total monthly expenses for food and housing for all active personnel in the campaign.
     *
     * <p>The calculation considers both food and housing costs based on the campaign's configuration and
     * personnel roles, including officers, enlisted members, prisoners, and dependents. Housing costs are only applied
     * if the campaign is located on a planet. Food and housing usage is counted using fixed per-person rates according
     * to their role/status:</p>
     *
     * <ul>
     *   <li>Prisoners or dependents have specific food and housing rates.</li>
     *   <li>Officers have higher food and housing rates than enlisted personnel.</li>
     *   <li>Crew members of non-DropShip large vessels live aboard their vessel and are exempt from housing charges.</li>
     * </ul>
     *
     * <p>If neither food nor housing expenses are enabled in the campaign options, this method returns zero.</p>
     *
     * @return a {@link Money} object representing the total monthly food and housing expenses for the campaign
     *
     * @author Illiani
     * @since 0.50.06
     */
    public Money getMonthlyFoodAndHousingExpenses() {
        return monthlyFoodAndHousingExpenses(getCampaignOptions(), getHumanResources(),
              getCurrentLocation(), campaign.getFactionStandings(), campaign.getActiveAtBContracts(),
              getLocalDate());
    }

    /**
     * Static version of {@link #getMonthlyFoodAndHousingExpenses()}.
     *
     * @param campaignOptions    the campaign options used to determine whether food and/or housing are paid for
     * @param humanResources     the personnel pool to evaluate
     * @param location           the campaign's current location, used to determine planet-side status and the barrack
     *                           cost multiplier
     * @param factionStandings   the campaign's faction standings, used to calculate the barrack cost multiplier
     * @param activeAtBContracts the campaign's active AtB contracts, used to calculate the barrack cost multiplier
     * @param today              the current campaign date
     *
     * @return a {@link Money} object representing the total monthly food and housing expenses for the campaign
     */
    public static Money monthlyFoodAndHousingExpenses(CampaignOptions campaignOptions, HumanResources humanResources,
          AbstractLocation location, FactionStandings factionStandings, List<AtBContract> activeAtBContracts,
          LocalDate today) {
        boolean payForFood = campaignOptions.isPayForFood();
        boolean isOnPlanet = location.isOnPlanet();
        boolean payForHousing = campaignOptions.isPayForHousing() && isOnPlanet;

        if (!payForFood && !payForHousing) {
            return Money.zero();
        }

        double barrackCostMultiplier = 1.0;
        if (isOnPlanet && campaignOptions.isUseFactionStandingBarracksCostsSafe()) {
            barrackCostMultiplier = setFactionStandingBarrackCostMultiplier(factionStandings, location,
                  activeAtBContracts, today);
        }

        int prisonerOrDependentHousingUsage = 0;
        int enlistedHousingUsage = 0;
        int officerHousingUsage = 0;

        int prisonerOrDependentFoodUsage = 0;
        int enlistedFoodUsage = 0;
        int officerFoodUsage = 0;

        // Determine housing and food requirements
        List<Person> personnel = new ArrayList<>(humanResources.getPersonnel());
        for (Person person : personnel) {
            if (person.getStatus().isDepartedUnit()) {
                // No paying for dead people or folks who left the campaign unit
                continue;
            }

            if (person.getStatus().isStudent() && !EducationController.isBeingHomeSchooled(person)) {
                // Tuition includes room and board
                continue;
            }

            boolean isPrisonerOrDependent = person.getPrisonerStatus().isCurrentPrisoner() ||
                                                  person.getPrimaryRole().isCivilian();
            boolean isOfficer = person.getRankNumeric() >= RWO_MIN;

            if (payForHousing) {
                Unit unit = person.getUnit();

                // Crew of non-DropShip vessels live on their vessel full time
                if (!isNonDropShipLargeVessel(unit)) {
                    if (isPrisonerOrDependent) {
                        prisonerOrDependentHousingUsage++;
                    } else if (isOfficer) {
                        officerHousingUsage++;
                    } else {
                        enlistedHousingUsage++;
                    }
                }
            }

            if (payForFood) {
                if (isPrisonerOrDependent) {
                    prisonerOrDependentFoodUsage++;
                } else if (isOfficer) {
                    officerFoodUsage++;
                } else {
                    enlistedFoodUsage++;
                }
            }
        }

        // calculate total costs
        int expenses = 0;
        if (payForHousing) {
            expenses += prisonerOrDependentHousingUsage * HOUSING_PRISONER_OR_DEPENDENT;
            expenses += enlistedHousingUsage * HOUSING_ENLISTED;
            expenses += officerHousingUsage * HOUSING_OFFICER;
        }

        if (payForFood) {
            expenses += prisonerOrDependentFoodUsage * FOOD_PRISONER_OR_DEPENDENT;
            expenses += enlistedFoodUsage * FOOD_ENLISTED;
            expenses += officerFoodUsage * FOOD_OFFICER;
        }

        LOGGER.debug("prisonerOrDependentHousingUsage: {}", prisonerOrDependentHousingUsage);
        LOGGER.debug("enlistedHousingUsage: {}", enlistedHousingUsage);
        LOGGER.debug("officerHousingUsage: {}", officerHousingUsage);
        LOGGER.debug("prisonerOrDependentFoodUsage: {}", prisonerOrDependentFoodUsage);
        LOGGER.debug("enlistedFoodUsage: {}", enlistedFoodUsage);
        LOGGER.debug("officerFoodUsage: {}", officerFoodUsage);
        LOGGER.debug("expenses: {}", expenses);

        return Money.of(expenses).multipliedBy(barrackCostMultiplier);
    }

    /**
     * Calculates the barrack cost multiplier for the given location based on faction standing.
     *
     * <p>This method determines the highest "regard" value the player has with employers of contracts active in the
     * current system. If no contracts are present, it uses the highest regard among all local factions in the planetary
     * system. The multiplier is then derived from this maximum regard value.</p>
     *
     * @param factionStandings   the faction standings used to look up regard values
     * @param location           the current location within the campaign
     * @param activeAtBContracts the campaign's active AtB contracts
     * @param today              the current campaign date, used to resolve local factions
     *
     * @return the barrack cost multiplier determined by the best available faction regard
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static double setFactionStandingBarrackCostMultiplier(FactionStandings factionStandings,
          AbstractLocation location, List<AtBContract> activeAtBContracts, LocalDate today) {
        PlanetarySystem currentSystem = location.getCurrentSystem();

        double maxRegard = 0.0;
        boolean foundContract = false;

        // Consider contracts in the current system
        for (AtBContract contract : activeAtBContracts) {
            if (contract.getSystem().equals(currentSystem)) {
                double currentRegard = factionStandings.getRegardForFaction(contract.getEmployerCode(), true);
                if (currentRegard > maxRegard) {
                    maxRegard = currentRegard;
                }
                foundContract = true;
            }
        }

        // If no contract found, check local factions
        if (!foundContract) {
            for (Faction faction : currentSystem.getFactionSet(today)) {
                double currentRegard = factionStandings.getRegardForFaction(faction.getShortName(), true);
                if (currentRegard > maxRegard) {
                    maxRegard = currentRegard;
                }
            }
        }

        return FactionStandingUtilities.getBarrackCostsMultiplier(maxRegard);
    }

    /**
     * Determines whether the specified unit is a large vessel that is not a DropShip.
     *
     * <p>This method checks if the given {@code unit} is non-null, retrieves its associated {@link Entity}, and
     * evaluates whether it qualifies as a large craft but is not a DropShip.</p>
     *
     * @param unit the unit to be tested; may be {@code null}
     *
     * @return {@code true} if the unit exists, its entity is a large craft, and it is not a DropShip; {@code false}
     *       otherwise
     *
     * @author Illiani
     * @since 0.50.06
     */
    public static boolean isNonDropShipLargeVessel(Unit unit) {
        if (unit == null) {
            return false;
        }

        Entity entity = unit.getEntity();
        return (entity != null) && entity.isLargeCraft() && !entity.isDropShip();
    }

    /**
     * Gets peacetime costs including salaries.
     *
     * @return The peacetime costs of the campaign including salaries.
     */
    public Money getPeacetimeCost() {
        return getPeacetimeCost(true);
    }

    /**
     * Gets peacetime costs, optionally including salaries.
     * <p>
     * This can be used to ensure salaries are not double counted.
     *
     * @param includeSalaries A value indicating whether salaries should be included in peacetime cost calculations.
     *
     * @return The peacetime costs of the campaign, optionally including salaries.
     */
    public Money getPeacetimeCost(boolean includeSalaries) {
        return peacetimeCost(getCampaignOptions(), getHangar(), getHumanResources(),
              getLocalDate(), isClanCampaign(), includeSalaries);
    }

    /**
     * Static version of {@link #getPeacetimeCost(boolean)}.
     *
     * @param campaignOptions the campaign options used for salary calculations
     * @param hangar          the hangar used to calculate spare parts, fuel, and ammo costs
     * @param humanResources  the personnel pool used to calculate salaries
     * @param today           the current campaign date, used for salary calculations
     * @param isClanCampaign  whether the campaign is a Clan campaign
     * @param includeSalaries whether salaries should be included in the calculation
     *
     * @return the {@link Money} value of the peacetime costs of the campaign
     */
    public static Money peacetimeCost(CampaignOptions campaignOptions, Hangar hangar, HumanResources humanResources,
          LocalDate today, boolean isClanCampaign, boolean includeSalaries) {
        Money peaceTimeCosts = Money.zero()
                                     .plus(monthlySpareParts(hangar))
                                     .plus(monthlyFuel(hangar))
                                     .plus(monthlyAmmo(hangar));
        if (includeSalaries) {
            peaceTimeCosts = peaceTimeCosts.plus(payRoll(campaignOptions,
                  humanResources,
                  campaignOptions.isInfantryDontCount(),
                  today,
                  isClanCampaign));
        }

        return peaceTimeCosts;
    }

    /**
     * Gets the total monthly spare parts costs for all non-mothballed units in the hangar.
     *
     * @return the {@link Money} value of monthly spare parts costs
     */
    public Money getMonthlySpareParts() {
        return monthlySpareParts(getHangar());
    }

    /**
     * Static version of {@link #getMonthlySpareParts()}.
     *
     * @param hangar the hangar containing the units to evaluate
     *
     * @return the {@link Money} value of monthly spare parts costs
     */
    public static Money monthlySpareParts(Hangar hangar) {
        return hangar.getUnitCosts(u -> !u.isMothballed(), Unit::getSparePartsCost);
    }

    /**
     * Gets the total monthly fuel costs for all non-mothballed units assigned to the TO&amp;E, based on hydrogen
     * production from fusion-engined units.
     *
     * @return the {@link Money} value of monthly fuel costs
     */
    public Money getMonthlyFuel() {
        return monthlyFuel(getHangar());
    }

    /**
     * Static version of {@link #getMonthlyFuel()}.
     *
     * @param hangar the hangar containing the units to evaluate
     *
     * @return the {@link Money} value of monthly fuel costs
     */
    public static Money monthlyFuel(Hangar hangar) {
        int daysInMonth = 28; // we use a 28-day month so we don't need to bring in and process the exact date
        int dailyHydrogenProduction = 10;
        int monthlyHydrogenProduction = daysInMonth * dailyHydrogenProduction;

        int totalFusionEngines = 0;
        for (Unit unit : hangar.getUnits()) {
            if (unit.isMothballed()) {
                continue;
            }

            Entity entity = unit.getEntity();

            if (entity == null) {
                LOGGER.info("(getMonthlyFuel) entity is null for {}", unit);
                continue;
            }

            // While there will be times when a unit is unavailable, we don't check for that as we also don't track
            // hydrogen stored, and we don't want to unfairly penalize the player.
            Engine engine = entity.getEngine();

            if (engine == null) {
                LOGGER.debug("(getMonthlyFuel) engine is null for {}", unit);
                continue;
            }

            if (entity.getEngine().isFusion()) {
                totalFusionEngines++;
            }
        }

        // Calculate total hydrogen production based on the number of fusion engines
        int hydrogenProduction = totalFusionEngines * monthlyHydrogenProduction;

        return hangar.getUnitCosts(
              // Is it in the TO&E and by extension in use?
              unit -> unit.getFormationId() != FORMATION_NONE, unit -> unit.getFuelCost(hydrogenProduction));
    }

    /**
     * Gets the total monthly ammo costs for all non-mothballed units in the hangar.
     *
     * @return the {@link Money} value of monthly ammo costs
     */
    public Money getMonthlyAmmo() {
        return monthlyAmmo(getHangar());
    }

    /**
     * Static version of {@link #getMonthlyAmmo()}.
     *
     * @param hangar the hangar containing the units to evaluate
     *
     * @return the {@link Money} value of monthly ammo costs
     */
    public static Money monthlyAmmo(Hangar hangar) {
        return hangar.getUnitCosts(u -> !u.isMothballed(), Unit::getAmmoCost);
    }

    /**
     * Calculates the total contract value of all qualifying units assigned to combat-role standard forces, applying
     * category-specific percentage multipliers and (optionally) diminishing returns.
     *
     * <p>This method iterates over {@code campaign().getAllForces()} and includes only forces that are both:</p>
     * <ul>
     *     <li>a standard force type ({@code force.getForceType().isStandard()}); and</li>
     *     <li>marked as a combat role ({@code force.getCombatRoleInMemory().isCombatRole()}).</li>
     * </ul>
     *
     * <p>Units are resolved via the campaign hangar; {@code null} units and units with {@code null} entities are
     * skipped. Conventional infantry is skipped when {@code excludeInfantry} is {@code true}.</p>
     *
     * <p>Large-craft categories are included only when their associated percentage is non-zero:</p>
     * <ul>
     *     <li><b>DropShips / Small Craft</b> use {@code dropShipContractPercent}</li>
     *     <li><b>WarShips</b> use {@code warShipContractPercent}</li>
     *     <li><b>JumpShips / Space Stations</b> use {@code jumpShipContractPercent}</li>
     * </ul>
     *
     * <p>Per-unit values are provided by {@link #getEquipmentContractValue(Unit, boolean)}. This method does not
     * apply the percentage itself; instead, it uses the percentage parameters as inclusion/exclusion flags (non-zero
     * = included, zero = excluded) for the corresponding large-craft categories.</p>
     *
     * <p>If {@code useDiminishingContractPay} is {@code true} and the number of included units exceeds the diminishing
     * returns start ({@link AlternatePaymentModelValues#getDiminishingReturnsStart(Faction)}), the total is computed
     * using {@link AlternatePaymentModelValues#adjustValuesForDiminishingReturns(Faction, List)}. Otherwise, the
     * method returns the straight sum.</p>
     *
     * @param useDiminishingContractPay whether diminishing returns should be applied (only when the unit count exceeds
     *                                  the diminishing-returns start)
     * @param excludeInfantry           if {@code true}, conventional infantry units are excluded from the calculation
     * @param dropShipContractPercent   inclusion flag for DropShips and Small Craft; if {@code 0}, these units are
     *                                  excluded
     * @param warShipContractPercent    inclusion flag for WarShips; if {@code 0}, these units are excluded
     * @param jumpShipContractPercent   inclusion flag for JumpShips and Space Stations; if {@code 0}, these units are
     *                                  excluded
     * @param useEquipmentSaleValue     if {@code true}, {@link #getEquipmentContractValue(Unit, boolean)} uses
     *                                  equipment sale values; if {@code false}, it uses standard values
     *
     * @return the total {@link Money} value of all included units, with diminishing returns applied when enabled and
     *       relevant
     */
    public Money getForceValue(boolean useDiminishingContractPay, boolean excludeInfantry,
          double dropShipContractPercent, double warShipContractPercent, double jumpShipContractPercent,
          boolean useEquipmentSaleValue) {
        return forceValue(getCampaignOptions(),
              getHangar(),
              getAllFormations(),
              getFaction(),
              useDiminishingContractPay,
              excludeInfantry,
              dropShipContractPercent,
              warShipContractPercent,
              jumpShipContractPercent,
              useEquipmentSaleValue);
    }

    /**
     * Static version of {@link #getForceValue(boolean, boolean, double, double, double, boolean)}.
     *
     * @param campaignOptions           the campaign options used to calculate per-unit contract values
     * @param hangar                    the hangar used to resolve unit IDs to units
     * @param formations                the formations to evaluate
     * @param campaignFaction           the campaign's faction, used for diminishing returns calculations
     * @param useDiminishingContractPay whether diminishing returns should be applied (only when the unit count exceeds
     *                                  the diminishing-returns start)
     * @param excludeInfantry           if {@code true}, conventional infantry units are excluded from the calculation
     * @param dropShipContractPercent   inclusion flag for DropShips and Small Craft; if {@code 0}, these units are
     *                                  excluded
     * @param warShipContractPercent    inclusion flag for WarShips; if {@code 0}, these units are excluded
     * @param jumpShipContractPercent   inclusion flag for JumpShips and Space Stations; if {@code 0}, these units are
     *                                  excluded
     * @param useEquipmentSaleValue     if {@code true}, {@link #equipmentContractValue(CampaignOptions, Unit, boolean)}
     *                                  uses equipment sale values; if {@code false}, it uses standard values
     *
     * @return the total {@link Money} value of all included units, with diminishing returns applied when enabled and
     *       relevant
     */
    public static Money forceValue(CampaignOptions campaignOptions, Hangar hangar, List<Formation> formations,
          Faction campaignFaction, boolean useDiminishingContractPay, boolean excludeInfantry,
          double dropShipContractPercent, double warShipContractPercent, double jumpShipContractPercent,
          boolean useEquipmentSaleValue) {
        List<Money> unitValues = new ArrayList<>();

        Money total = Money.zero();
        for (Formation formation : formations) {
            if (!formation.getFormationType().isStandard()) {
                continue;
            }
            if (!formation.getCombatRoleInMemory().isCombatRole()) {
                continue;
            }

            for (UUID uuid : formation.getUnits()) {
                Unit unit = hangar.getUnit(uuid);
                if (unit == null) {
                    continue;
                }

                Entity entity = unit.getEntity();
                if (entity == null) {
                    continue;
                }

                // Infantry
                if (unit.isConventionalInfantry() && excludeInfantry) {
                    continue;
                }

                Money unitValue = equipmentContractValue(campaignOptions, unit, useEquipmentSaleValue);

                // DropShips / Small Craft
                if (entity.isDropShip() || entity.isSmallCraft()) {
                    if (dropShipContractPercent != 0) {
                        unitValues.add(unitValue);
                        total = total.plus(unitValue);
                    }
                    continue;
                }

                // WarShips
                if (entity.isWarShip()) {
                    if (warShipContractPercent != 0) {
                        unitValues.add(unitValue);
                        total = total.plus(unitValue);
                    }
                    continue;
                }

                // JumpShips / Space Stations
                if (entity.isJumpShip() || entity.isSpaceStation()) {
                    if (jumpShipContractPercent != 0) {
                        unitValues.add(unitValue);
                        total = total.plus(unitValue);
                    }
                    continue;
                }

                // Other
                unitValues.add(unitValue);
                total = total.plus(unitValue);
            }
        }

        if (unitValues.isEmpty()) {
            return Money.zero();
        }

        // Only process diminishing returns if it is both enabled and relevant.
        boolean isAffectedByDiminishingReturns = unitValues.size() > getDiminishingReturnsStart(campaignFaction);
        if (useDiminishingContractPay && isAffectedByDiminishingReturns) {
            return adjustValuesForDiminishingReturns(campaignFaction, unitValues);
        }

        return total;
    }

    /**
     * Gets the total value of all units in the hangar plus all spare parts in the warehouse.
     *
     * @return the total {@link Money} value of all equipment owned by the campaign
     */
    public Money getTotalEquipmentValue() {
        return totalEquipmentValue(getHangar(), campaign.getWarehouse());
    }

    /**
     * Static version of {@link #getTotalEquipmentValue()}.
     *
     * @param hangar    the hangar containing the units to evaluate
     * @param warehouse the warehouse containing the spare parts to evaluate
     *
     * @return the total {@link Money} value of all equipment
     */
    public static Money totalEquipmentValue(Hangar hangar, Warehouse warehouse) {
        Money unitsSellValue = hangar.getUnitCosts(Unit::getSellValue);
        return warehouse
                     .streamSpareParts()
                     .map(Part::getActualValue)
                     .reduce(unitsSellValue, Money::plus);
    }

    /**
     * Gets the contract value of a single unit, as a percentage of its sell or buy value depending on unit type.
     *
     * @param unit         the unit to evaluate
     * @param useSaleValue if {@code true}, the unit's sell value is used as the base; otherwise its buy cost is used
     *
     * @return the {@link Money} contract value of the unit
     */
    public Money getEquipmentContractValue(Unit unit, boolean useSaleValue) {
        return equipmentContractValue(getCampaignOptions(), unit, useSaleValue);
    }

    /**
     * Static version of {@link #getEquipmentContractValue(Unit, boolean)}.
     *
     * @param campaignOptions the campaign options containing the contract percentages for each unit category
     * @param unit            the unit to evaluate
     * @param useSaleValue    if {@code true}, the unit's sell value is used as the base; otherwise its buy cost is
     *                        used
     *
     * @return the {@link Money} contract value of the unit
     */
    public static Money equipmentContractValue(CampaignOptions campaignOptions, Unit unit, boolean useSaleValue) {
        Money value;
        Money percentValue;

        if (useSaleValue) {
            value = unit.getSellValue();
        } else {
            value = unit.getBuyCost();
        }

        if (unit.getEntity().hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            percentValue = value.multipliedBy(campaignOptions.getDropShipContractPercent()).dividedBy(100);
        } else if (unit.getEntity().hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            percentValue = value.multipliedBy(campaignOptions.getWarShipContractPercent()).dividedBy(100);
        } else if (unit.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) ||
                         unit.getEntity().hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            percentValue = value.multipliedBy(campaignOptions.getJumpShipContractPercent()).dividedBy(100);
        } else {
            percentValue = value.multipliedBy(campaignOptions.getEquipmentContractPercent()).dividedBy(100);
        }

        return percentValue;
    }

    /**
     * Calculates the base monetary value for contracts in the campaign based on the specified campaign options. The
     * calculation considers whether peacetime costs, equipment contracts, or theoretical payroll costs should be used
     * as the base value.
     *
     * <p>This method retrieves relevant options from the campaign's {@link CampaignOptions}
     * to control how the base contract value is computed. Based on the campaign settings, it takes into account factors
     * such as infantry exclusion, contract percentages for different types of units (DropShips, WarShips, JumpShips),
     * and whether to use the equipment's sale value in the calculations.</p>
     *
     * @return A {@link Money} object representing the calculated base contract value, adjusted according to the
     *       campaign's configuration.
     */
    public Money getContractBase() {
        return contractBase(getCampaignOptions(), campaign.getAllHangar(), getHumanResources(),
              getLocalDate(), getFaction(), getAllFormations());
    }

    /**
     * Static version of {@link #getContractBase()}.
     *
     * @param campaignOptions the campaign options used to control how the base contract value is computed
     * @param hangar          the hangar used to resolve units for force-value calculations
     * @param humanResources  the personnel pool used for payroll-based calculations
     * @param today           the current campaign date
     * @param campaignFaction the campaign's faction
     * @param formations      the campaign's formations, used for force-value calculations
     *
     * @return a {@link Money} object representing the calculated base contract value, adjusted according to the
     *       campaign's configuration
     */
    public static Money contractBase(CampaignOptions campaignOptions, Hangar hangar, HumanResources humanResources,
          LocalDate today, Faction campaignFaction, List<Formation> formations) {
        final boolean isClanCampaign = campaignFaction.isClan();
        final boolean excludeInfantry = campaignOptions.isInfantryDontCount();
        final double combatUnitContractPercent = campaignOptions.getEquipmentContractPercent();
        final double dropShipContractPercent = campaignOptions.getDropShipContractPercent();
        final double warShipContractPercent = campaignOptions.getWarShipContractPercent();
        final double jumpShipContractPercent = campaignOptions.getJumpShipContractPercent();
        final boolean useEquipmentSellValue = campaignOptions.isEquipmentContractSaleValue();
        final boolean useDiminishingContractPay = campaignOptions.isUseDiminishingContractPay();

        if (campaignOptions.isUseAlternatePaymentMode()) {
            final Money forceValue = AlternatePaymentModelValues.getForceValue(campaignFaction,
                  formations,
                  hangar,
                  useDiminishingContractPay,
                  excludeInfantry,
                  combatUnitContractPercent,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent);

            if (useEquipmentSellValue) {
                return forceValue.multipliedBy(0.5);
            }

            return forceValue;
        }

        if (campaignOptions.isUsePeacetimeCost()) {
            final Money forceValue = forceValue(campaignOptions,
                  hangar,
                  formations,
                  campaignFaction,
                  useDiminishingContractPay,
                  excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSellValue);

            return peacetimeCost(campaignOptions,
                  hangar,
                  humanResources,
                  today,
                  isClanCampaign,
                  true).multipliedBy(0.75).plus(forceValue);
        }

        if (campaignOptions.isEquipmentContractBase()) {
            return forceValue(campaignOptions,
                  hangar,
                  formations,
                  campaignFaction,
                  useDiminishingContractPay,
                  excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSellValue);
        }

        return theoreticalPayroll(campaignOptions,
              humanResources,
              campaignOptions.isInfantryDontCount(),
              today,
              isClanCampaign);
    }

    /**
     * Returns a map of every Person and their salary.
     *
     * @return map of personnel to their pay, including pool as a null key
     *
     * @see Finances#debit(TransactionType, LocalDate, Money, String, Map, boolean)
     */
    public Map<Person, Money> getPayRollSummary() {
        return payRollSummary(getCampaignOptions(),
              getHumanResources(),
              getLocalDate(),
              isClanCampaign());
    }

    /**
     * Static version of {@link #getPayRollSummary()}.
     */
    public static Map<Person, Money> payRollSummary(CampaignOptions campaignOptions, HumanResources humanResources,
          LocalDate today, boolean isClanCampaign) {
        Map<Person, Money> payRollSummary = new HashMap<>();
        for (Person person : humanResources.getSalaryEligiblePersonnel()) {
            payRollSummary.put(person, person.getSalary(campaignOptions, today, isClanCampaign));
        }
        // And pay our pool
        payRollSummary.put(null, Money.of(sumTempCrewPay(campaignOptions, humanResources)));

        return payRollSummary;
    }

    /**
     * Sums the pay for all temporary crew (medics, astechs, and other temporary pool roles), including infantry.
     *
     * @param campaignOptions the campaign options used for role base salaries
     * @param humanResources  the personnel pool containing the temporary crew pools
     *
     * @return the total temporary crew pay
     */
    private static double sumTempCrewPay(CampaignOptions campaignOptions, HumanResources humanResources) {
        return sumTempCrewPay(campaignOptions, humanResources, false);
    }

    /**
     * Sums the pay for all temporary crew (medics, astechs, and other temporary pool roles), optionally excluding
     * soldier roles.
     *
     * @param campaignOptions the campaign options used for role base salaries
     * @param humanResources  the personnel pool containing the temporary crew pools
     * @param noInfantry      if {@code true}, soldier roles are excluded from the calculation
     *
     * @return the total temporary crew pay
     */
    private static double sumTempCrewPay(CampaignOptions campaignOptions, HumanResources humanResources,
          boolean noInfantry) {
        double tempCrewPay = 0.0;
        tempCrewPay += getTempCrewPay(campaignOptions, PersonnelRole.ASTECH, humanResources.getTemporaryAsTechPool());
        tempCrewPay += getTempCrewPay(campaignOptions, PersonnelRole.MEDIC, humanResources.getTemporaryMedicPool());

        for (PersonnelRole personnelRole : humanResources.getTempCrewRoleKeys()) {
            if (!(noInfantry && personnelRole.isSoldier())) {
                tempCrewPay += getTempCrewPay(campaignOptions,
                      personnelRole,
                      humanResources.getTempCrewPool(personnelRole));
            }
        }

        return tempCrewPay;
    }

    /**
     * Calculates the total pay for a single temporary-crew role, based on its base salary and pool size.
     *
     * @param campaignOptions   the campaign options used to look up the role's base salary
     * @param personnelRole     the role to calculate pay for
     * @param tempPersonnelPool the number of temporary personnel filling this role
     *
     * @return the total pay for the role's temporary pool
     */
    private static double getTempCrewPay(CampaignOptions campaignOptions, PersonnelRole personnelRole,
          int tempPersonnelPool) {
        return campaignOptions
                     .getRoleBaseSalaries()[personnelRole.ordinal()].getAmount().doubleValue() *
                     tempPersonnelPool;
    }
}
