/*
 * Accountant.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.finances;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import megamek.common.Entity;
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
public class Accountant {
    private final Campaign campaign;

    public Accountant(Campaign campaign) {
        this.campaign = campaign;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CampaignOptions getCampaignOptions() {
        return getCampaign().getCampaignOptions();
    }

    public Hangar getHangar() {
        return getCampaign().getHangar();
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
        for (Person p : getCampaign().getActivePersonnel()) {
            // Optionized infantry (Unofficial)
            if (!(noInfantry && p.getPrimaryRole().isSoldier())) {
                salaries = salaries.plus(p.getSalary(getCampaign()));
            }
        }

        // And pay our pool
        salaries = salaries.plus(getCampaign().getCampaignOptions()
                .getRoleBaseSalaries()[PersonnelRole.ASTECH.ordinal()].getAmount().doubleValue()
                * getCampaign().getAstechPool());
        salaries = salaries.plus(getCampaign().getCampaignOptions()
                .getRoleBaseSalaries()[PersonnelRole.MEDIC.ordinal()].getAmount().doubleValue()
                * getCampaign().getMedicPool());

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
        return getHangar().getUnitsStream()
            .map(Unit::getWeeklyMaintenanceCost)
            .reduce(Money.zero(), Money::plus);
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
     * @return The peacetime costs of the campaign including salaries.
     */
    public Money getPeacetimeCost() {
        return getPeacetimeCost(true);
    }

    /**
     * Gets peacetime costs, optionally including salaries.
     *
     * This can be used to ensure salaries are not double counted.
     *
     * @param includeSalaries A value indicating whether or not salaries
     *                        should be included in peacetime cost calculations.
     * @return The peacetime costs of the campaign, optionally including salaries.
     */
    public Money getPeacetimeCost(boolean includeSalaries) {
        Money peaceTimeCosts = Money.zero()
                                .plus(getMonthlySpareParts())
                                .plus(getMonthlyFuel())
                                .plus(getMonthlyAmmo());
        if (includeSalaries) {
            peaceTimeCosts = peaceTimeCosts.plus(getPayRoll(getCampaignOptions().isInfantryDontCount()));
        }

        return peaceTimeCosts;
    }

    public Money getMonthlySpareParts() {
        return getHangar().getUnitCosts(u -> !u.isMothballed(), Unit::getSparePartsCost);
    }

    public Money getMonthlyFuel() {
        return getHangar().getUnitCosts(u -> !u.isMothballed(), Unit::getFuelCost);
    }

    public Money getMonthlyAmmo() {
        return getHangar().getUnitCosts(u -> !u.isMothballed(), Unit::getAmmoCost);
    }

    /**
     * @return the total value of units in the TO&amp;E. This serves as the basis for contract payments
     * in the StellarOps Beta.
     */
    public Money getForceValue(boolean noInfantry) {
        Money value = Money.zero();
        for (UUID uuid : getCampaign().getForces().getAllUnits(false)) {
            Unit u = getHangar().getUnit(uuid);
            if (null == u) {
                continue;
            }
            if (noInfantry && ((u.getEntity().getEntityType() & Entity.ETYPE_INFANTRY) == Entity.ETYPE_INFANTRY)
                    && !((u.getEntity().getEntityType() & Entity.ETYPE_BATTLEARMOR) == Entity.ETYPE_BATTLEARMOR)) {
                continue;
            }
            if (u.getEntity().hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
                if (getCampaignOptions().getDropShipContractPercent() == 0) {
                    continue;
                }
                value = value.plus(getEquipmentContractValue(u, getCampaignOptions().isEquipmentContractSaleValue()));
            } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_WARSHIP)) {
                if (getCampaignOptions().getWarShipContractPercent() == 0) {
                    continue;
                }
                value = value.plus(getEquipmentContractValue(u, getCampaignOptions().isEquipmentContractSaleValue()));
            } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) || u.getEntity().hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
                if (getCampaignOptions().getJumpShipContractPercent() == 0) {
                    continue;
                }
                value = value.plus(getEquipmentContractValue(u, getCampaignOptions().isEquipmentContractSaleValue()));
            } else {
                value = value.plus(getEquipmentContractValue(u, getCampaignOptions().isEquipmentContractSaleValue()));
            }
        }
        return value;
    }


    public Money getTotalEquipmentValue() {
        Money unitsSellValue = getHangar().getUnitCosts(Unit::getSellValue);
        return getCampaign().getWarehouse().streamSpareParts().map(Part::getActualValue)
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
        } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) || u.getEntity().hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            percentValue = value.multipliedBy(getCampaignOptions().getJumpShipContractPercent()).dividedBy(100);
        } else {
            percentValue = value.multipliedBy(getCampaignOptions().getEquipmentContractPercent()).dividedBy(100);
        }

        return percentValue;
    }

    public Money getContractBase() {
        if (getCampaignOptions().isUsePeacetimeCost()) {
            return getPeacetimeCost()
                    .multipliedBy(0.75)
                    .plus(getForceValue(getCampaignOptions().isInfantryDontCount()));
        } else if (getCampaignOptions().isEquipmentContractBase()) {
            return getForceValue(getCampaignOptions().isInfantryDontCount());
        } else {
            return getTheoreticalPayroll(getCampaignOptions().isInfantryDontCount());
        }
    }

    /**
     * Returns a map of every Person and their salary.
     *
     * @see Finances#debit(TransactionType, LocalDate, Money, String, Map, boolean)
     * @return map of personnel to their pay, including pool as a null key
     */
    public Map<Person, Money> getPayRollSummary() {
        Map<Person, Money> payRollSummary = new HashMap<>();
        for (Person p : getCampaign().getActivePersonnel()) {
                payRollSummary.put(p, p.getSalary(getCampaign()));
            }
        // And pay our pool
        payRollSummary.put(null, Money.of(
            (getCampaign().getCampaignOptions().getRoleBaseSalaries()
                [PersonnelRole.ASTECH.ordinal()].getAmount().doubleValue()
                * getCampaign().getAstechPool())
            + (getCampaign().getCampaignOptions().getRoleBaseSalaries()
                [PersonnelRole.MEDIC.ordinal()].getAmount().doubleValue()
                * getCampaign().getMedicPool())));

        return payRollSummary;
    }
}
