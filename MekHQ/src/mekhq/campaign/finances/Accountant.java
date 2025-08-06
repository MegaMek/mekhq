/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.personnel.ranks.Rank.RWO_MIN;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.common.Engine;
import megamek.common.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Hangar;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.enums.TransactionType;
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
    private static final MMLogger logger = MMLogger.create(Accountant.class);

    final public static int HOUSING_PRISONER_OR_DEPENDENT = 228;
    final public static int HOUSING_ENLISTED = 312;
    final public static int HOUSING_OFFICER = 780;
    final public static int FOOD_PRISONER_OR_DEPENDENT = 120;
    final public static int FOOD_ENLISTED = 240;
    final public static int FOOD_OFFICER = 480;

    public CampaignOptions getCampaignOptions() {
        return campaign().getCampaignOptions();
    }

    public Hangar getHangar() {
        return campaign().getHangar();
    }

    public Money getPayRoll() {
        return getPayRoll(false);
    }

    public Money getPayRoll(boolean noInfantry) {
        if (getCampaignOptions().isPayForSalaries()) {
            return getTheoreticalPayroll(noInfantry);
        } else {
            return Money.zero();
        }
    }

    private Money getTheoreticalPayroll(boolean noInfantry) {
        Money salaries = Money.zero();
        for (Person person : campaign().getSalaryEligiblePersonnel()) {
            // Optionized infantry (Unofficial)
            if (!(noInfantry && person.getPrimaryRole().isSoldier())) {
                salaries = salaries.plus(person.getSalary(campaign()));
            }
        }

        // And pay our pool
        salaries = salaries.plus(campaign().getCampaignOptions()
                                       .getRoleBaseSalaries()[PersonnelRole.ASTECH.ordinal()].getAmount()
                                       .doubleValue() * campaign().getAstechPool());
        salaries = salaries.plus(campaign().getCampaignOptions()
                                       .getRoleBaseSalaries()[PersonnelRole.MEDIC.ordinal()].getAmount().doubleValue() *
                                       campaign().getMedicPool());

        return salaries;
    }

    public Money getMaintenanceCosts() {
        if (getCampaignOptions().isPayForMaintain()) {
            return getHangar().getUnitsStream()
                         .filter(u -> u.requiresMaintenance() && (null != u.getTech()))
                         .map(Unit::getMaintenanceCost)
                         .reduce(Money.zero(), Money::plus);
        }
        return Money.zero();
    }

    public Money getWeeklyMaintenanceCosts() {
        return getHangar().getUnitsStream().map(Unit::getWeeklyMaintenanceCost).reduce(Money.zero(), Money::plus);
    }

    public Money getOverheadExpenses() {
        if (getCampaignOptions().isPayForOverhead()) {
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
        boolean payForFood = getCampaignOptions().isPayForFood();
        CurrentLocation location = campaign.getLocation();
        boolean isOnPlanet = location.isOnPlanet();
        boolean payForHousing = getCampaignOptions().isPayForHousing() && isOnPlanet;

        if (!payForFood && !payForHousing) {
            return Money.zero();
        }

        double barrackCostMultiplier = 1.0;
        if (isOnPlanet && getCampaignOptions().isUseFactionStandingBarracksCostsSafe()) {
            barrackCostMultiplier = setFactionStandingBarrackCostMultiplier(location);
        }

        int prisonerOrDependentHousingUsage = 0;
        int enlistedHousingUsage = 0;
        int officerHousingUsage = 0;

        int prisonerOrDependentFoodUsage = 0;
        int enlistedFoodUsage = 0;
        int officerFoodUsage = 0;

        // Determine housing and food requirements
        List<Person> personnel = new ArrayList<>(campaign().getPersonnel());
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

        logger.debug("prisonerOrDependentHousingUsage: {}", prisonerOrDependentHousingUsage);
        logger.debug("enlistedHousingUsage: {}", enlistedHousingUsage);
        logger.debug("officerHousingUsage: {}", officerHousingUsage);
        logger.debug("prisonerOrDependentFoodUsage: {}", prisonerOrDependentFoodUsage);
        logger.debug("enlistedFoodUsage: {}", enlistedFoodUsage);
        logger.debug("officerFoodUsage: {}", officerFoodUsage);
        logger.debug("expenses: {}", expenses);

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
    private double setFactionStandingBarrackCostMultiplier(CurrentLocation location) {
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
     * @param includeSalaries A value indicating whether or not salaries should be included in peacetime cost
     *                        calculations.
     *
     * @return The peacetime costs of the campaign, optionally including salaries.
     */
    public Money getPeacetimeCost(boolean includeSalaries) {
        Money peaceTimeCosts = Money.zero().plus(getMonthlySpareParts()).plus(getMonthlyFuel()).plus(getMonthlyAmmo());
        if (includeSalaries) {
            peaceTimeCosts = peaceTimeCosts.plus(getPayRoll(getCampaignOptions().isInfantryDontCount()));
        }

        return peaceTimeCosts;
    }

    public Money getMonthlySpareParts() {
        return getHangar().getUnitCosts(u -> !u.isMothballed(), Unit::getSparePartsCost);
    }

    public Money getMonthlyFuel() {
        int daysInMonth = 28; // we use a 28-day month so we don't need to bring in and process the exact date
        int dailyHydrogenProduction = 10;
        int monthlyHydrogenProduction = daysInMonth * dailyHydrogenProduction;

        int totalFusionEngines = 0;
        for (Unit unit : getHangar().getUnits()) {
            if (unit.isMothballed()) {
                continue;
            }

            Entity entity = unit.getEntity();

            if (entity == null) {
                logger.info("(getMonthlyFuel) entity is null for {}", unit);
                continue;
            }

            // While there will be times when a unit is unavailable, we don't check for that as we also don't track
            // hydrogen stored, and we don't want to unfairly penalize the player.
            Engine engine = entity.getEngine();

            if (engine == null) {
                logger.debug("(getMonthlyFuel) engine is null for {}", unit);
                continue;
            }

            if (entity.getEngine().isFusion()) {
                totalFusionEngines++;
            }
        }

        // Calculate total hydrogen production based on the number of fusion engines
        int hydrogenProduction = totalFusionEngines * monthlyHydrogenProduction;

        return getHangar().getUnitCosts(
              // Is it in the TO&E and by extension in use?
              unit -> unit.getForceId() != FORCE_NONE, unit -> unit.getFuelCost(hydrogenProduction));
    }

    public Money getMonthlyAmmo() {
        return getHangar().getUnitCosts(u -> !u.isMothballed(), Unit::getAmmoCost);
    }

    /**
     * Calculates the total monetary value of all units in the current campaign's forces, applying specific percentage
     * adjustments based on unit types. Units such as DropShips, WarShips, JumpShips, Space Stations, infantry, and
     * others are calculated according to the provided contract percentages and rules.
     *
     * <p>This method iterates over all units in the campaign's forces and computes their
     * total value by checking each unit's type and applying the appropriate adjustments.</p>
     *
     * <p>The value of each unit is based on the {@code useEquipmentSaleValue} flag, which
     * determines whether to use the equipment's sale value during the calculation.</p>
     *
     * @param excludeInfantry         A {@code boolean} flag specifying whether conventional infantry units (non-Battle
     *                                Armor) should be excluded.
     * @param dropShipContractPercent The percentage adjustment applied specifically to DropShips. If set to {@code 0},
     *                                DropShips are excluded from the calculation.
     * @param warShipContractPercent  The percentage adjustment applied specifically to WarShips. If set to {@code 0},
     *                                WarShips are excluded from the calculation.
     * @param jumpShipContractPercent The percentage adjustment applied specifically to JumpShips and Space Stations. If
     *                                set to {@code 0}, these units are excluded.
     * @param useEquipmentSaleValue   A {@code boolean} flag that determines whether to use the equipment's sale value
     *                                in the calculation.
     *
     * @return A {@link Money} object representing the total force value of the campaign's units after applying the
     *       provided rules and percentages.
     */
    public Money getForceValue(boolean excludeInfantry, double dropShipContractPercent, double warShipContractPercent,
          double jumpShipContractPercent, boolean useEquipmentSaleValue) {
        Money value = Money.zero();

        for (UUID uuid : campaign().getAllUnitsInTheTOE(true)) {
            Unit unit = getHangar().getUnit(uuid);

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

            // DropShips
            if (entity.isDropShip() || entity.isSmallCraft()) {
                if (dropShipContractPercent != 0) {
                    value = value.plus(getEquipmentContractValue(unit, useEquipmentSaleValue));
                }

                continue;
            }

            // WarShips
            if (entity.isWarShip()) {
                if (warShipContractPercent != 0) {
                    value = value.plus(getEquipmentContractValue(unit, useEquipmentSaleValue));
                }

                continue;
            }

            // JumpShips
            if (entity.isJumpShip() || entity.isSpaceStation()) {
                if (jumpShipContractPercent != 0) {
                    value = value.plus(getEquipmentContractValue(unit, useEquipmentSaleValue));
                }

                continue;
            }

            // Other
            value = value.plus(getEquipmentContractValue(unit, useEquipmentSaleValue));
        }

        return value;
    }


    public Money getTotalEquipmentValue() {
        Money unitsSellValue = getHangar().getUnitCosts(Unit::getSellValue);
        return campaign().getWarehouse()
                     .streamSpareParts()
                     .map(Part::getActualValue)
                     .reduce(unitsSellValue, Money::plus);
    }

    public Money getEquipmentContractValue(Unit u, boolean useSaleValue) {
        Money value;
        Money percentValue;

        if (useSaleValue) {
            value = u.getSellValue();
        } else {
            value = u.getBuyCost();
        }

        if (u.getEntity().hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            percentValue = value.multipliedBy(getCampaignOptions().getDropShipContractPercent()).dividedBy(100);
        } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            percentValue = value.multipliedBy(getCampaignOptions().getWarShipContractPercent()).dividedBy(100);
        } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) ||
                         u.getEntity().hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            percentValue = value.multipliedBy(getCampaignOptions().getJumpShipContractPercent()).dividedBy(100);
        } else {
            percentValue = value.multipliedBy(getCampaignOptions().getEquipmentContractPercent()).dividedBy(100);
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
        final CampaignOptions options = getCampaignOptions();

        final boolean excludeInfantry = options.isInfantryDontCount();
        final double dropShipContractPercent = options.getDropShipContractPercent();
        final double warShipContractPercent = options.getWarShipContractPercent();
        final double jumpShipContractPercent = options.getJumpShipContractPercent();
        final boolean useEquipmentSalveValue = options.isEquipmentContractSaleValue();

        if (getCampaignOptions().isUsePeacetimeCost()) {
            final Money forceValue = getForceValue(excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSalveValue);

            return getPeacetimeCost().multipliedBy(0.75).plus(forceValue);
        }

        if (getCampaignOptions().isEquipmentContractBase()) {
            return getForceValue(excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSalveValue);
        }

        return getTheoreticalPayroll(getCampaignOptions().isInfantryDontCount());
    }

    /**
     * Returns a map of every Person and their salary.
     *
     * @return map of personnel to their pay, including pool as a null key
     *
     * @see Finances#debit(TransactionType, LocalDate, Money, String, Map, boolean)
     */
    public Map<Person, Money> getPayRollSummary() {
        Map<Person, Money> payRollSummary = new HashMap<>();
        for (Person person : campaign().getSalaryEligiblePersonnel()) {
            payRollSummary.put(person, person.getSalary(campaign()));
        }
        // And pay our pool
        payRollSummary.put(null,
              Money.of((campaign().getCampaignOptions()
                              .getRoleBaseSalaries()[PersonnelRole.ASTECH.ordinal()].getAmount().doubleValue() *
                              campaign().getAstechPool()) +
                             (campaign().getCampaignOptions()
                                    .getRoleBaseSalaries()[PersonnelRole.MEDIC.ordinal()].getAmount().doubleValue() *
                                    campaign().getMedicPool())));

        return payRollSummary;
    }
}
