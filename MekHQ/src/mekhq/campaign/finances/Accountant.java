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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import megamek.common.Engine;
import megamek.common.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Hangar;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

/**
 * Provides accounting for a Campaign.
 */
public record Accountant(Campaign campaign) {
    private static final MMLogger logger = MMLogger.create(Accountant.class);

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
        for (Person person : campaign().getActivePersonnelAndHomeSchooledStudents()) {
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
        for (Person person : campaign().getActivePersonnelAndHomeSchooledStudents()) {
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
