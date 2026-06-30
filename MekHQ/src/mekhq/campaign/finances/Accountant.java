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
public class Accountant {
    private static final MMLogger LOGGER = MMLogger.create(Accountant.class);

    final public static int HOUSING_PRISONER_OR_DEPENDENT = 228;
    final public static int HOUSING_ENLISTED = 312;
    final public static int HOUSING_OFFICER = 780;
    final public static int FOOD_PRISONER_OR_DEPENDENT = 120;
    final public static int FOOD_ENLISTED = 240;
    final public static int FOOD_OFFICER = 480;

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final Hangar hangar;
    private final HumanResources humanResources;
    private final List<Formation> formations;
    private final Warehouse warehouse;
    private final int temporaryAsTechPool;
    private final int temporaryMedicPool;
    private final Map<PersonnelRole, Integer> tempPersonnelRoleMap;

    /**
     * Constructs a new Accountant instance.
     *
     * @param campaign             the current campaign
     * @param units                the list of units in the campaign
     * @param people               the roster of personnel
     * @param formations           the active formations
     * @param parts                the tracking components in storage
     * @param temporaryAsTechPool  count of temporary Assistant Technicians
     * @param temporaryMedicPool   count of temporary Medical staff
     * @param tempPersonnelRoleMap mapping of temporary personnel allocations by role
     */
    public Accountant(Campaign campaign, List<Unit> units, List<Person> people, List<Formation> formations,
          List<Part> parts, int temporaryAsTechPool, int temporaryMedicPool,
          Map<PersonnelRole, Integer> tempPersonnelRoleMap) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();

        this.hangar = new Hangar();
        for (Unit unit : units) {
            hangar.addUnit(unit);
        }

        this.humanResources = new HumanResources();
        for (Person person : people) {
            humanResources.importPerson(person);
        }

        this.formations = formations;

        this.warehouse = new Warehouse();
        for (Part part : parts) {
            warehouse.addPart(part);
        }

        this.temporaryAsTechPool = temporaryAsTechPool;
        this.temporaryMedicPool = temporaryMedicPool;
        this.tempPersonnelRoleMap = tempPersonnelRoleMap;
    }

    /**
     * Constructs a new Accountant instance.
     *
     * @param campaign the current campaign
     */
    public Accountant(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.hangar = campaign.getHangar();
        this.humanResources = campaign.getHumanResources();
        this.formations = campaign.getAllFormations();
        this.warehouse = campaign.getWarehouse();
        this.temporaryAsTechPool = campaign.getTemporaryAsTechPool();
        this.temporaryMedicPool = campaign.getTemporaryMedicPool();
        this.tempPersonnelRoleMap = humanResources.getTempPersonnelRoleMap();
    }

    /**
     * Gets total payroll costs, defaulting to tracking all personnel.
     *
     * @return a {@link Money} object representing the current payroll costs
     */
    public Money getPayRoll() {
        return getPayRoll(false);
    }

    /**
     * Gets total payroll costs with an option to exclude infantry units.
     *
     * @param noInfantry if {@code true}, conventional infantry roles are excluded
     *
     * @return a {@link Money} object representing the payroll total
     */
    public Money getPayRoll(boolean noInfantry) {
        if (campaignOptions.isPayForSalaries()) {
            return getTheoreticalPayroll(noInfantry);
        } else {
            return Money.zero();
        }
    }

    /**
     * Internal calculation for base payroll totals including active salary-eligible personnel and temporary staff
     * pools.
     *
     * @param noInfantry if {@code true}, infantry combat roles are skipped
     *
     * @return total theoretical payroll expense
     */
    private Money getTheoreticalPayroll(boolean noInfantry) {
        Money salaries = Money.zero();
        for (Person person : humanResources.getSalaryEligiblePersonnel()) {
            if (!(noInfantry && person.getPrimaryRole().isSoldier())) {
                salaries = salaries.plus(person.getSalary(campaign));
            }
        }

        // Add all temporary personnel (medics, astechs, temp crew)
        salaries = salaries.plus(sumTempCrewPay(noInfantry));

        return salaries;
    }

    /**
     * Calculates the total maintenance costs for active equipment needing upkeep.
     *
     * @return total maintenance expenditures
     */
    public Money getMaintenanceCosts() {
        if (campaignOptions.isPayForMaintain()) {
            return hangar.getUnitsStream()
                         .filter(u -> u.requiresMaintenance() && (null != u.getTech()))
                         .map(Unit::getMaintenanceCost)
                         .reduce(Money.zero(), Money::plus);
        }
        return Money.zero();
    }

    /**
     * Calculates maintenance costs broken down weekly.
     *
     * @return total weekly equipment upkeep costs
     */
    public Money getWeeklyMaintenanceCosts() {
        return hangar.getUnitsStream().map(Unit::getWeeklyMaintenanceCost).reduce(Money.zero(), Money::plus);
    }

    /**
     * Calculates base operational overhead cost, derived as a percentage of overall payroll.
     *
     * @return total overhead operational costs
     */
    public Money getOverheadExpenses() {
        if (campaignOptions.isPayForOverhead()) {
            return getTheoreticalPayroll(false).multipliedBy(0.05);
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
     *     <li>Prisoners or dependents have specific food and housing rates.</li>
     *     <li>Officers have higher food and housing rates than enlisted personnel.</li>
     *     <li>Crew members of non-DropShip large vessels live aboard their vessel and are exempt from housing charges.</li>
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
        boolean payForFood = campaignOptions.isPayForFood();
        AbstractLocation location = campaign.getCurrentLocation();
        boolean isOnPlanet = location.isOnPlanet();
        boolean payForHousing = campaignOptions.isPayForHousing() && isOnPlanet;

        if (!payForFood && !payForHousing) {
            return Money.zero();
        }

        double barrackCostMultiplier = 1.0;
        if (isOnPlanet && campaignOptions.isUseFactionStandingBarracksCostsSafe()) {
            barrackCostMultiplier = setFactionStandingBarrackCostMultiplier(location);
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
     * @param location the current location within the campaign
     *
     * @return the barrack cost multiplier determined by the best available faction regard
     *
     * @author Illiani
     * @since 0.50.07
     */
    private double setFactionStandingBarrackCostMultiplier(AbstractLocation location) {
        FactionStandings factionStandings = campaign.getFactionStandings();
        PlanetarySystem currentSystem = location.getCurrentSystem();

        double maxRegard = 0.0;
        boolean foundContract = false;

        // Consider contracts in the current system
        for (AtBContract contract : campaign.getActiveAtBContracts()) {
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
            for (Faction faction : currentSystem.getFactionSet(campaign.getLocalDate())) {
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
    public boolean isNonDropShipLargeVessel(Unit unit) {
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
        Money peaceTimeCosts = Money.zero().plus(getMonthlySpareParts()).plus(getMonthlyFuel()).plus(getMonthlyAmmo());
        if (includeSalaries) {
            peaceTimeCosts = peaceTimeCosts.plus(getPayRoll(campaignOptions.isInfantryDontCount()));
        }

        return peaceTimeCosts;
    }

    /**
     * Sums monthly required costs for spare parts across active, non-mothballed assets.
     *
     * @return total spare parts costs
     */
    public Money getMonthlySpareParts() {
        return hangar.getUnitCosts(u -> !u.isMothballed(), Unit::getSparePartsCost);
    }

    /**
     * Calculates monthly fuel expenses using an idealized 28-day month simulation baseline. Total hydrogen credits are
     * calculated against the number of active operational fusion engines.
     *
     * @return total structural monthly fuel cost
     */
    public Money getMonthlyFuel() {
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
     * Sums monthly ammo costs across active, non-mothballed assets.
     *
     * @return total ammo expenses
     */
    public Money getMonthlyAmmo() {
        return hangar.getUnitCosts(u -> !u.isMothballed(), Unit::getAmmoCost);
    }

    /**
     * Calculates the total contract value of all qualifying units assigned to combat-role standard forces, applying
     * category-specific percentage multipliers and (optionally) diminishing returns.
     *
     * <p>This method iterates over {@code campaign().getAllForces()} and includes only forces that are both:</p>
     * <ul>
     * <li>a standard force type ({@code force.getForceType().isStandard()}); and</li>
     * <li>marked as a combat role ({@code force.getCombatRoleInMemory().isCombatRole()}).</li>
     * </ul>
     *
     * <p>Units are resolved via the campaign hangar; {@code null} units and units with {@code null} entities are
     * skipped. Conventional infantry is skipped when {@code excludeInfantry} is {@code true}.</p>
     *
     * <p>Large-craft categories are included only when their associated percentage is non-zero:</p>
     * <ul>
     * <li><b>DropShips / Small Craft</b> use {@code dropShipContractPercent}</li>
     * <li><b>WarShips</b> use {@code warShipContractPercent}</li>
     * <li><b>JumpShips / Space Stations</b> use {@code jumpShipContractPercent}</li>
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

                Money unitValue = getEquipmentContractValue(unit, useEquipmentSaleValue);

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
        Faction campaignFaction = campaign.getFaction();
        boolean isAffectedByDiminishingReturns = unitValues.size() > getDiminishingReturnsStart(campaignFaction);
        if (useDiminishingContractPay && isAffectedByDiminishingReturns) {
            return adjustValuesForDiminishingReturns(campaignFaction, unitValues);
        }

        return total;
    }

    /**
     * Calculates the total value of all owned asset hardware combined with the inventory of spare warehouse
     * components.
     *
     * @return total evaluation value of hardware and warehouse inventory assets
     */
    public Money getTotalEquipmentValue() {
        Money unitsSellValue = hangar.getUnitCosts(Unit::getSellValue);
        return warehouse
                     .streamSpareParts()
                     .map(Part::getActualValue)
                     .reduce(unitsSellValue, Money::plus);
    }

    /**
     * Extracts contract processing value modifiers assigned dynamically to specific structural units.
     *
     * @param unit         the individual target equipment unit
     * @param useSaleValue if {@code true}, targets market sale listings; otherwise targets buying costs
     *
     * @return adjusted percentage contract modifier value
     */
    public Money getEquipmentContractValue(Unit unit, boolean useSaleValue) {
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
        final boolean excludeInfantry = campaignOptions.isInfantryDontCount();
        final double combatUnitContractPercent = campaignOptions.getEquipmentContractPercent();
        final double dropShipContractPercent = campaignOptions.getDropShipContractPercent();
        final double warShipContractPercent = campaignOptions.getWarShipContractPercent();
        final double jumpShipContractPercent = campaignOptions.getJumpShipContractPercent();
        final boolean useEquipmentSellValue = campaignOptions.isEquipmentContractSaleValue();
        final boolean useDiminishingContractPay = campaignOptions.isUseDiminishingContractPay();

        if (campaignOptions.isUseAlternatePaymentMode()) {
            final Money forceValue = AlternatePaymentModelValues.getForceValue(campaign.getFaction(),
                  campaign.getAllFormations(),
                  campaign.getAllHangar(),
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
            final Money forceValue = this.getForceValue(useDiminishingContractPay,
                  excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSellValue);

            return getPeacetimeCost().multipliedBy(0.75).plus(forceValue);
        }

        if (campaignOptions.isEquipmentContractBase()) {
            return this.getForceValue(useDiminishingContractPay,
                  excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSellValue);
        }

        return getTheoreticalPayroll(campaignOptions.isInfantryDontCount());
    }

    /**
     * Returns a map of every Person and their salary.
     *
     * @return map of personnel to their pay, including pool as a null key
     */
    public Map<Person, Money> getPayRollSummary() {
        Map<Person, Money> payRollSummary = new HashMap<>();
        for (Person person : humanResources.getSalaryEligiblePersonnel()) {
            payRollSummary.put(person, person.getSalary(campaign));
        }
        // And pay our pool
        payRollSummary.put(null, Money.of(sumTempCrewPay()));

        return payRollSummary;
    }

    /**
     * Aggregates total support salaries paid out to active unlisted temporary pools.
     *
     * @return numerical sum total of temporary workforce payroll
     */
    private double sumTempCrewPay() {
        return sumTempCrewPay(false);
    }

    /**
     * Aggregates total support salaries paid out to active unlisted temporary pools, with optional filter parameters.
     *
     * @param noInfantry if {@code true}, exclusions apply to combat infantry roles
     *
     * @return total temporary crew pay sum
     */
    private double sumTempCrewPay(boolean noInfantry) {
        double tempCrewPay = 0.0;
        tempCrewPay += getTempCrewPay(PersonnelRole.ASTECH, temporaryAsTechPool);
        tempCrewPay += getTempCrewPay(PersonnelRole.MEDIC, temporaryMedicPool);

        for (PersonnelRole personnelRole : tempPersonnelRoleMap.keySet()) {
            if (!(noInfantry && personnelRole.isSoldier())) {
                tempCrewPay += getTempCrewPay(personnelRole, tempPersonnelRoleMap.get(personnelRole));
            }
        }

        return tempCrewPay;
    }

    /**
     * Resolves individual role salary rules against unlisted pool deployment counts.
     *
     * @param personnelRole     target structural type classification
     * @param tempPersonnelPool deployment size for requested workforce
     *
     * @return calculated cost total
     */
    private double getTempCrewPay(PersonnelRole personnelRole, int tempPersonnelPool) {
        return campaignOptions
                     .getRoleBaseSalaries()[personnelRole.ordinal()].getAmount().doubleValue() *
                     tempPersonnelPool;
    }
}
