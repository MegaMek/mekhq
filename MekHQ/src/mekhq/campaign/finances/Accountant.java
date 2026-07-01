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
import java.util.Collection;
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

    final static int HOUSING_PRISONER_OR_DEPENDENT = 228;
    final static int HOUSING_ENLISTED = 312;
    final static int HOUSING_OFFICER = 780;
    final static int FOOD_PRISONER_OR_DEPENDENT = 120;
    final static int FOOD_ENLISTED = 240;
    final static int FOOD_OFFICER = 480;

    private CampaignOptions getCampaignOptions() {
        return campaign().getCampaignOptions();
    }

    private Hangar getHangar() {
        return campaign().getHangar();
    }

    private boolean isClanCampaign() {
        return campaign().isClanCampaign();
    }

    private LocalDate getLocalDate() {
        return campaign().getLocalDate();
    }

    private int getTemporaryAsTechPool() {
        return campaign().getTemporaryAsTechPool();
    }

    private int getTemporaryMedicPool() {
        return campaign().getTemporaryMedicPool();
    }

    private Map<PersonnelRole, Integer> getTempCrewMap() {
        return campaign().getTempCrewMapCopy();
    }


    public Money getPayRoll() {
        return getPayRoll(false);
    }

    public Money getPayRoll(boolean noInfantry) {
        return getPayRollTotal(campaign().getSalaryEligiblePersonnel(),
              getCampaignOptions(),
              isClanCampaign(),
              getLocalDate(),
              getTemporaryAsTechPool(),
              getTemporaryMedicPool(),
              getTempCrewMap(),
              noInfantry,
              getCampaignOptions().isPayForSalaries());
    }

    /**
     * Static version of {@link #getPayRoll(boolean)}.
     *
     * @param personnel               the personnel to total salaries for
     * @param campaignOptions         the campaign options used to resolve salaries and temporary crew pay
     * @param isClanCampaign          whether the campaign is a Clan campaign, used to resolve each person's salary
     * @param today                   the current campaign date, used to resolve each person's salary
     * @param temporaryAsTechPoolSize the size of the campaign's temporary astech pool
     * @param temporaryMedicPool      the size of the campaign's temporary medic pool
     * @param tempCrewMap             the campaign's other temporary crew roles, mapped to their pool sizes
     * @param noInfantry              if {@code true}, personnel whose primary role is soldier are excluded
     * @param payForSalaries          if {@code false}, this method returns zero regardless of the other parameters
     *
     * @return the total {@link Money} value of the payroll, or zero if salaries are not paid
     */
    public static Money getPayRollTotal(Collection<Person> personnel, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today, int temporaryAsTechPoolSize, int temporaryMedicPool,
          Map<PersonnelRole, Integer> tempCrewMap, boolean noInfantry, boolean payForSalaries) {
        if (!payForSalaries) {
            return Money.zero();
        }

        return getTheoreticalPayrollTotal(personnel,
              campaignOptions,
              isClanCampaign,
              today,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              tempCrewMap,
              noInfantry);
    }

    /**
     * Calculates the total salary owed to the given personnel.
     *
     * @param personnel       the personnel to total salaries for
     * @param campaignOptions the campaign options used to resolve each person's salary
     * @param isClanCampaign  whether the campaign is a Clan campaign, used to resolve each person's salary
     * @param today           the current campaign date, used to resolve each person's salary
     * @param noInfantry      if {@code true}, personnel whose primary role is soldier are excluded
     *
     * @return the total {@link Money} owed in salaries
     */
    public static Money getSalaryTotal(Collection<Person> personnel, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today, boolean noInfantry) {
        Money salaries = Money.zero();
        for (Person person : personnel) {
            if (!(noInfantry && person.getPrimaryRole().isSoldier())) {
                salaries = salaries.plus(person.getSalary(campaignOptions, isClanCampaign, today));
            }
        }

        return salaries;
    }

    /**
     * Calculates the sum of all given personnel's salaries plus temporary crew pay, regardless of whether the campaign
     * is configured to actually pay salaries.
     *
     * @param personnel               the personnel to total salaries for
     * @param campaignOptions         the campaign options used to resolve salaries and temporary crew pay
     * @param isClanCampaign          whether the campaign is a Clan campaign, used to resolve each person's salary
     * @param today                   the current campaign date, used to resolve each person's salary
     * @param temporaryAsTechPoolSize the size of the campaign's temporary astech pool
     * @param temporaryMedicPool      the size of the campaign's temporary medic pool
     * @param tempCrewMap             the campaign's other temporary crew roles, mapped to their pool sizes
     * @param noInfantry              if {@code true}, personnel whose primary role is soldier are excluded
     *
     * @return the total {@link Money} value of salaries and temporary crew pay
     */
    private static Money getTheoreticalPayrollTotal(Collection<Person> personnel, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today, int temporaryAsTechPoolSize, int temporaryMedicPool,
          Map<PersonnelRole, Integer> tempCrewMap, boolean noInfantry) {
        Money salaries = getSalaryTotal(personnel, campaignOptions, isClanCampaign, today, noInfantry);

        // Add all temporary personnel (medics, astechs, temp crew)
        double sumTempCrewPay = sumTempCrewPay(campaignOptions,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              tempCrewMap,
              noInfantry);
        salaries = salaries.plus(Money.of(sumTempCrewPay));

        return salaries;
    }

    public Money getMaintenanceCosts() {
        return getMaintenanceTotal(getHangar().getUnits(), getCampaignOptions().isPayForMaintain());
    }

    /**
     * Static version of {@link #getMaintenanceCosts()}.
     *
     * @param units          the units to total maintenance costs for
     * @param payForMaintain if {@code false}, this method returns zero regardless of the given units
     *
     * @return the total {@link Money} cost of maintenance, or zero if maintenance is not paid for
     */
    public static Money getMaintenanceTotal(Collection<Unit> units, boolean payForMaintain) {
        if (!payForMaintain) {
            return Money.zero();
        }

        Money total = Money.zero();
        for (Unit unit : units) {
            if (unit.requiresMaintenance() && (unit.getTech() != null)) {
                total = total.plus(unit.getMaintenanceCost());
            }
        }

        return total;
    }

    public Money getWeeklyMaintenanceCosts() {
        return getWeeklyMaintenanceTotal(getHangar().getUnits());
    }

    /**
     * Static version of {@link #getWeeklyMaintenanceCosts()}.
     *
     * @param units the units to total weekly maintenance costs for
     *
     * @return the total {@link Money} weekly cost of maintenance
     */
    public static Money getWeeklyMaintenanceTotal(Collection<Unit> units) {
        Money total = Money.zero();
        for (Unit unit : units) {
            total = total.plus(unit.getWeeklyMaintenanceCost());
        }

        return total;
    }

    public Money getOverheadExpenses() {
        return getOverheadTotal(campaign().getSalaryEligiblePersonnel(),
              getCampaignOptions(),
              isClanCampaign(),
              getLocalDate(),
              getTemporaryAsTechPool(),
              getTemporaryMedicPool(),
              getTempCrewMap(),
              getCampaignOptions().isPayForOverhead());
    }

    /**
     * Static version of {@link #getOverheadExpenses()}.
     *
     * @param personnel               the personnel to base the theoretical payroll on
     * @param campaignOptions         the campaign options used to resolve salaries and temporary crew pay
     * @param isClanCampaign          whether the campaign is a Clan campaign, used to resolve each person's salary
     * @param today                   the current campaign date, used to resolve each person's salary
     * @param temporaryAsTechPoolSize the size of the campaign's temporary astech pool
     * @param temporaryMedicPool      the size of the campaign's temporary medic pool
     * @param tempCrewMap             the campaign's other temporary crew roles, mapped to their pool sizes
     * @param payForOverhead          if {@code false}, this method returns zero regardless of the other parameters
     *
     * @return the total {@link Money} value of overhead expenses, or zero if overhead is not paid for
     */
    public static Money getOverheadTotal(Collection<Person> personnel, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today, int temporaryAsTechPoolSize, int temporaryMedicPool,
          Map<PersonnelRole, Integer> tempCrewMap, boolean payForOverhead) {
        if (!payForOverhead) {
            return Money.zero();
        }

        return getTheoreticalPayrollTotal(personnel,
              campaignOptions,
              isClanCampaign,
              today,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              tempCrewMap,
              false).multipliedBy(0.05);
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
        AbstractLocation location = campaign.getCurrentLocation();
        List<Person> allPersonnel = new ArrayList<>(campaign.getAllPersonnel());
        return getMonthlyFoodAndHousingExpenses(campaign, allPersonnel, location);
    }

    /**
     * Static version of {@link #getMonthlyFoodAndHousingExpenses()}, additionally resolving the faction-standing
     * barrack cost multiplier (if enabled) from the given campaign rather than {@code this.campaign()}.
     *
     * @param campaign  the campaign, used to resolve the barrack cost multiplier (faction standings, active AtB
     *                  contracts, and the current date)
     * @param personnel the personnel to evaluate
     * @param location  the location to evaluate; determines whether housing is charged at all and, together with the
     *                  campaign, whether a faction-standing barrack cost multiplier applies
     *
     * @return a {@link Money} object representing the total monthly food and housing expenses
     */
    public static Money getMonthlyFoodAndHousingExpenses(Campaign campaign, List<Person> personnel,
          AbstractLocation location) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean payForFood = campaignOptions.isPayForFood();
        boolean isOnPlanet = location.isOnPlanet();
        boolean payForHousing = campaignOptions.isPayForHousing() && isOnPlanet;

        if (!payForFood && !payForHousing) {
            return Money.zero();
        }

        double barrackCostMultiplier = 1.0;
        if (isOnPlanet && campaignOptions.isUseFactionStandingBarracksCostsSafe()) {
            barrackCostMultiplier = setFactionStandingBarrackCostMultiplier(campaign.getFactionStandings(),
                  location.getCurrentSystem(), campaign.getActiveAtBContracts(), campaign.getLocalDate());
        }

        return getFoodAndHousingTotal(personnel, payForFood, payForHousing, barrackCostMultiplier);
    }

    /**
     * Static version of {@link #getMonthlyFoodAndHousingExpenses()}.
     *
     * @param personnel             the personnel to evaluate
     * @param payForFood            whether food expenses should be included
     * @param payForHousing         whether housing expenses should be included (should already account for whether the
     *                              campaign is on a planet)
     * @param barrackCostMultiplier the multiplier applied to the total housing and food expenses
     *
     * @return a {@link Money} object representing the total monthly food and housing expenses
     */
    public static Money getFoodAndHousingTotal(Collection<Person> personnel, boolean payForFood,
          boolean payForHousing, double barrackCostMultiplier) {
        if (!payForFood && !payForHousing) {
            return Money.zero();
        }

        int prisonerOrDependentHousingUsage = 0;
        int enlistedHousingUsage = 0;
        int officerHousingUsage = 0;

        int prisonerOrDependentFoodUsage = 0;
        int enlistedFoodUsage = 0;
        int officerFoodUsage = 0;

        // Determine housing and food requirements
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
     * @param currentSystem      the planetary system to evaluate
     * @param activeAtBContracts the campaign's active AtB contracts
     * @param today              the current campaign date, used to resolve local factions
     *
     * @return the barrack cost multiplier determined by the best available faction regard
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static double setFactionStandingBarrackCostMultiplier(FactionStandings factionStandings,
          PlanetarySystem currentSystem, List<AtBContract> activeAtBContracts, LocalDate today) {
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
    static boolean isNonDropShipLargeVessel(Unit unit) {
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
        return getPeacetimeOperatingCosts(campaign().getFormations().getSubFormations(),
              getHangar(),
              getCampaignOptions(),
              isClanCampaign(),
              getLocalDate(),
              getTemporaryAsTechPool(),
              getTemporaryMedicPool(),
              getTempCrewMap(),
              includeSalaries);
    }

    /**
     * Calculates the peacetime operating costs (spare parts, fuel, ammo and, optionally, salaries) of the given
     * formations.
     *
     * <p>Units and personnel are resolved by walking the given formations and all of their sub-formations, so
     * callers only need to supply the formations they care about. This lets the method double as both a whole-campaign
     * total (by passing every top-level formation) and, in future, a total scoped to some smaller grouping of
     * formations without any change to this method.</p>
     *
     * <p>Salaries, when included, cover both the crews of the resolved units and the campaign's temporary
     * personnel pools (astechs, medics, and other temporary crew), since those pools are not tied to any particular
     * formation.</p>
     *
     * @param formations              the formations (and, recursively, their sub-formations) to total operating costs
     *                                for
     * @param hangar                  the hangar used to resolve unit ids into units
     * @param campaignOptions         the campaign options used to resolve salaries and temporary crew pay
     * @param isClanCampaign          whether the campaign is a Clan campaign, used to resolve each person's salary
     * @param today                   the current campaign date, used to resolve each person's salary
     * @param temporaryAsTechPoolSize the size of the campaign's temporary astech pool
     * @param temporaryMedicPool      the size of the campaign's temporary medic pool
     * @param tempCrewMap             the campaign's other temporary crew roles, mapped to their pool sizes
     * @param includeSalaries         whether salaries should be included in the total
     *
     * @return the total {@link Money} peacetime operating cost of the given formations
     */
    public static Money getPeacetimeOperatingCosts(Collection<Formation> formations, Hangar hangar,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> tempCrewMap, boolean includeSalaries) {
        Collection<Unit> units = getUnitsInFormations(formations, hangar);

        Money peacetimeCosts = getSparePartsTotal(units).plus(getFuelTotal(units)).plus(getAmmoTotal(units));

        if (includeSalaries && campaignOptions.isPayForSalaries()) {
            Collection<Person> personnel = getCrewsOfUnits(units);
            boolean noInfantry = campaignOptions.isInfantryDontCount();
            peacetimeCosts = peacetimeCosts.plus(getSalaryTotal(personnel,
                  campaignOptions,
                  isClanCampaign,
                  today,
                  noInfantry));
            peacetimeCosts = peacetimeCosts.plus(Money.of(sumTempCrewPay(campaignOptions,
                  temporaryAsTechPoolSize,
                  temporaryMedicPool,
                  tempCrewMap,
                  noInfantry)));
        }

        return peacetimeCosts;
    }

    /**
     * Resolves every unit assigned to the given formations, recursing into their sub-formations.
     *
     * @param formations the formations to resolve units for
     * @param hangar     the hangar used to resolve unit ids into {@link Unit} instances
     *
     * @return every unit assigned to the given formations and their sub-formations
     */
    private static List<Unit> getUnitsInFormations(Collection<Formation> formations, Hangar hangar) {
        List<Unit> units = new ArrayList<>();
        for (Formation formation : formations) {
            for (UUID unitId : formation.getUnits()) {
                Unit unit = hangar.getUnit(unitId);
                if (unit != null) {
                    units.add(unit);
                }
            }

            units.addAll(getUnitsInFormations(formation.getSubFormations(), hangar));
        }

        return units;
    }

    /**
     * Collects the crews of the given units.
     *
     * @param units the units to collect crews from
     *
     * @return every person crewing the given units
     */
    private static List<Person> getCrewsOfUnits(Collection<Unit> units) {
        List<Person> personnel = new ArrayList<>();
        for (Unit unit : units) {
            personnel.addAll(unit.getCrew());
        }

        return personnel;
    }

    public Money getMonthlySpareParts() {
        return getSparePartsTotal(getHangar().getUnits());
    }

    public Money getMonthlyFuel() {
        return getFuelTotal(getHangar().getUnits());
    }

    public Money getMonthlyAmmo() {
        return getAmmoTotal(getHangar().getUnits());
    }

    /**
     * Calculates the total monthly spare parts cost for the given units.
     *
     * @param units the units to total spare parts costs for
     *
     * @return the total {@link Money} cost of spare parts
     */
    public static Money getSparePartsTotal(Collection<Unit> units) {
        Money total = Money.zero();
        for (Unit unit : units) {
            if (!unit.isMothballed()) {
                total = total.plus(unit.getSparePartsCost());
            }
        }

        return total;
    }

    /**
     * Calculates the total monthly fuel cost for the given units.
     *
     * <p>Every non-mothballed unit with a fusion engine contributes to the pool of hydrogen produced each month;
     * that pooled production is then used to determine the fuel cost of every unit that is in the TOE (and by extension
     * in use).</p>
     *
     * @param units the units to total fuel costs for
     *
     * @return the total {@link Money} cost of fuel
     */
    public static Money getFuelTotal(Collection<Unit> units) {
        int daysInMonth = 28; // we use a 28-day month so we don't need to bring in and process the exact date
        int dailyHydrogenProduction = 10;
        int monthlyHydrogenProduction = daysInMonth * dailyHydrogenProduction;

        int totalFusionEngines = 0;
        for (Unit unit : units) {
            if (unit.isMothballed()) {
                continue;
            }

            Entity entity = unit.getEntity();

            if (entity == null) {
                LOGGER.info("(getFuelTotal) entity is null for {}", unit);
                continue;
            }

            // While there will be times when a unit is unavailable, we don't check for that as we also don't track
            // hydrogen stored, and we don't want to unfairly penalize the player.
            Engine engine = entity.getEngine();

            if (engine == null) {
                LOGGER.debug("(getFuelTotal) engine is null for {}", unit);
                continue;
            }

            if (entity.getEngine().isFusion()) {
                totalFusionEngines++;
            }
        }

        // Calculate total hydrogen production based on the number of fusion engines
        int hydrogenProduction = totalFusionEngines * monthlyHydrogenProduction;

        Money total = Money.zero();
        for (Unit unit : units) {
            // Is it in the TO&E and by extension in use?
            if (unit.getFormationId() != FORMATION_NONE) {
                total = total.plus(unit.getFuelCost(hydrogenProduction));
            }
        }

        return total;
    }

    /**
     * Calculates the total monthly ammo cost for the given units.
     *
     * @param units the units to total ammo costs for
     *
     * @return the total {@link Money} cost of ammo
     */
    public static Money getAmmoTotal(Collection<Unit> units) {
        Money total = Money.zero();
        for (Unit unit : units) {
            if (!unit.isMothballed()) {
                total = total.plus(unit.getAmmoCost());
            }
        }

        return total;
    }

    /**
     * Calculates the total contract value of all qualifying units assigned to combat-role standard forces, applying
     * category-specific percentage multipliers and (optionally) diminishing returns.
     *
     * <p>This method iterates over {@code campaign().getAllFormations()} and includes only forces that are both:</p>
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
        return getForceValue(campaign().getAllFormations(), getHangar(), campaign().getFaction(),
              getCampaignOptions(), useDiminishingContractPay, excludeInfantry, dropShipContractPercent,
              warShipContractPercent, jumpShipContractPercent, useEquipmentSaleValue);
    }

    /**
     * Static version of {@link #getForceValue(boolean, boolean, double, double, double, boolean)}.
     *
     * @param formations                the formations to evaluate
     * @param hangar                    the hangar used to resolve unit ids to units
     * @param campaignFaction           the campaign's faction, used for diminishing returns calculations
     * @param campaignOptions           the campaign options used to calculate per-unit contract values
     * @param useDiminishingContractPay whether diminishing returns should be applied (only when the unit count exceeds
     *                                  the diminishing-returns start)
     * @param excludeInfantry           if {@code true}, conventional infantry units are excluded from the calculation
     * @param dropShipContractPercent   inclusion flag for DropShips and Small Craft; if {@code 0}, these units are
     *                                  excluded
     * @param warShipContractPercent    inclusion flag for WarShips; if {@code 0}, these units are excluded
     * @param jumpShipContractPercent   inclusion flag for JumpShips and Space Stations; if {@code 0}, these units are
     *                                  excluded
     * @param useEquipmentSaleValue     if {@code true},
     *                                  {@link #getEquipmentContractValue(CampaignOptions, Unit, boolean)} uses
     *                                  equipment sale values; if {@code false}, it uses standard values
     *
     * @return the total {@link Money} value of all included units, with diminishing returns applied when enabled and
     *       relevant
     */
    public static Money getForceValue(Collection<Formation> formations, Hangar hangar, Faction campaignFaction,
          CampaignOptions campaignOptions, boolean useDiminishingContractPay, boolean excludeInfantry,
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

                Money unitValue = getEquipmentContractValue(campaignOptions, unit, useEquipmentSaleValue);

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

    public Money getTotalEquipmentValue() {
        return getTotalEquipmentValue(getHangar().getUnits(), campaign().getWarehouse());
    }

    /**
     * Static version of {@link #getTotalEquipmentValue()}.
     *
     * @param units     the units to total sell value for
     * @param warehouse the warehouse containing the spare parts to evaluate
     *
     * @return the total {@link Money} value of all equipment
     */
    public static Money getTotalEquipmentValue(Collection<Unit> units, Warehouse warehouse) {
        Money total = Money.zero();
        for (Unit unit : units) {
            total = total.plus(unit.getSellValue());
        }

        return warehouse.streamSpareParts().map(Part::getActualValue).reduce(total, Money::plus);
    }

    public Money getEquipmentContractValue(Unit unit, boolean useSaleValue) {
        return getEquipmentContractValue(getCampaignOptions(), unit, useSaleValue);
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
    public static Money getEquipmentContractValue(CampaignOptions campaignOptions, Unit unit, boolean useSaleValue) {
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
        return getContractBase(getCampaignOptions(),
              campaign().getFaction(),
              getLocalDate(),
              getHangar(),
              campaign().getSalaryEligiblePersonnel(),
              getTemporaryAsTechPool(),
              getTemporaryMedicPool(),
              getTempCrewMap(),
              campaign().getAllFormations());
    }

    /**
     * Static version of {@link #getContractBase()}.
     *
     * @param campaignOptions         the campaign options used to control how the base contract value is computed
     * @param campaignFaction         the campaign's faction, used for diminishing-returns and clan-campaign
     *                                calculations
     * @param today                   the current campaign date, used to resolve salaries
     * @param hangar                  the hangar used to resolve unit ids to units
     * @param salaryEligiblePersonnel the personnel used for the theoretical-payroll fallback branch
     * @param temporaryAsTechPoolSize the size of the campaign's temporary astech pool
     * @param temporaryMedicPool      the size of the campaign's temporary medic pool
     * @param tempCrewMap             the campaign's other temporary crew roles, mapped to their pool sizes
     * @param formations              the campaign's formations, used for force-value and peacetime-cost calculations
     *
     * @return a {@link Money} object representing the calculated base contract value, adjusted according to the
     *       campaign's configuration
     */
    public static Money getContractBase(CampaignOptions campaignOptions, Faction campaignFaction, LocalDate today,
          Hangar hangar, List<Person> salaryEligiblePersonnel, int temporaryAsTechPoolSize, int temporaryMedicPool,
          Map<PersonnelRole, Integer> tempCrewMap, List<Formation> formations) {
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
            final Money forceValue = getForceValue(formations,
                  hangar,
                  campaignFaction,
                  campaignOptions,
                  useDiminishingContractPay,
                  excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSellValue);

            Money peacetimeOperatingCost = getPeacetimeOperatingCosts(formations,
                  hangar,
                  campaignOptions,
                  isClanCampaign,
                  today,
                  temporaryAsTechPoolSize,
                  temporaryMedicPool,
                  tempCrewMap,
                  true);
            return peacetimeOperatingCost
                         .multipliedBy(0.75)
                         .plus(forceValue);
        }

        if (campaignOptions.isEquipmentContractBase()) {
            return getForceValue(formations,
                  hangar,
                  campaignFaction,
                  campaignOptions,
                  useDiminishingContractPay,
                  excludeInfantry,
                  dropShipContractPercent,
                  warShipContractPercent,
                  jumpShipContractPercent,
                  useEquipmentSellValue);
        }

        return getTheoreticalPayrollTotal(salaryEligiblePersonnel,
              campaignOptions,
              isClanCampaign,
              today,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              tempCrewMap,
              campaignOptions.isInfantryDontCount());
    }

    /**
     * Returns a map of every Person and their salary.
     *
     * @return map of personnel to their pay, including pool as a null key
     *
     * @see Finances#debit(TransactionType, LocalDate, Money, String, Map, boolean)
     */
    public Map<Person, Money> getPayRollSummary() {
        return getPayRollSummary(campaign().getSalaryEligiblePersonnel(),
              getCampaignOptions(),
              isClanCampaign(),
              getLocalDate(),
              getTemporaryAsTechPool(),
              getTemporaryMedicPool(),
              getTempCrewMap());
    }

    /**
     * Static version of {@link #getPayRollSummary()}.
     *
     * @param personnel               the personnel to total salaries for
     * @param campaignOptions         the campaign options used to resolve salaries and temporary crew pay
     * @param isClanCampaign          whether the campaign is a Clan campaign, used to resolve each person's salary
     * @param today                   the current campaign date, used to resolve each person's salary
     * @param temporaryAsTechPoolSize the size of the campaign's temporary astech pool
     * @param temporaryMedicPool      the size of the campaign's temporary medic pool
     * @param tempCrewMap             the campaign's other temporary crew roles, mapped to their pool sizes
     *
     * @return map of personnel to their pay, including pool as a null key
     */
    public static Map<Person, Money> getPayRollSummary(Collection<Person> personnel, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today, int temporaryAsTechPoolSize, int temporaryMedicPool,
          Map<PersonnelRole, Integer> tempCrewMap) {
        Map<Person, Money> payRollSummary = new HashMap<>();
        for (Person person : personnel) {
            payRollSummary.put(person, person.getSalary(campaignOptions, isClanCampaign, today));
        }
        // And pay our pool
        double tempCrewPay = sumTempCrewPay(campaignOptions,
              temporaryAsTechPoolSize,
              temporaryMedicPool,
              tempCrewMap,
              false);
        payRollSummary.put(null, Money.of(tempCrewPay));

        return payRollSummary;
    }

    /**
     * Calculates the total pay owed to the campaign's temporary personnel pools (astechs, medics, and any other
     * temporary crew roles).
     *
     * @param campaignOptions         the campaign options used to look up each role's base salary
     * @param temporaryAsTechPoolSize the size of the campaign's temporary astech pool
     * @param temporaryMedicPool      the size of the campaign's temporary medic pool
     * @param tempCrewMap             the campaign's other temporary crew roles, mapped to their pool sizes
     * @param noInfantry              if {@code true}, temporary personnel in soldier roles are excluded
     *
     * @return the total pay owed to temporary personnel
     */
    private static double sumTempCrewPay(CampaignOptions campaignOptions, int temporaryAsTechPoolSize,
          int temporaryMedicPool, Map<PersonnelRole, Integer> tempCrewMap, boolean noInfantry) {
        double tempCrewPay = 0.0;
        tempCrewPay += getTempCrewPay(campaignOptions, PersonnelRole.ASTECH, temporaryAsTechPoolSize);
        tempCrewPay += getTempCrewPay(campaignOptions, PersonnelRole.MEDIC, temporaryMedicPool);

        for (PersonnelRole personnelRole : tempCrewMap.keySet()) {
            if (!(noInfantry && personnelRole.isSoldier())) {
                tempCrewPay += getTempCrewPay(campaignOptions, personnelRole, tempCrewMap.get(personnelRole));
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
