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

import java.util.UUID;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Hangar;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
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
        if (getCampaignOptions().payForSalaries()) {
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
                salaries = salaries.plus(p.getSalary());
            }
        }
        // add in astechs from the astech pool
        // we will assume Mech Tech * able-bodied * enlisted (changed from vee mechanic)
        // 800 * 0.5 * 0.6 = 240
        salaries = salaries.plus(240.0 * getCampaign().getAstechPool());
        salaries = salaries.plus(320.0 * getCampaign().getMedicPool());
        return salaries;
    }

    public Money getMaintenanceCosts() {
        if (getCampaignOptions().payForMaintain()) {
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
        if (getCampaignOptions().payForOverhead()) {
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
            peaceTimeCosts = peaceTimeCosts.plus(getPayRoll(getCampaignOptions().useInfantryDontCount()));
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
     * Calculate the total value of units in the TO&E. This serves as the basis for contract payments in the StellarOps
     * Beta.
     *
     * @return
     */
    public Money getForceValue() {
        return getForceValue(false);
    }

    /**
     * Calculate the total value of units in the TO&E. This serves as the basis for contract payments in the StellarOps
     * Beta.
     *
     * @return
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
                if (getCampaignOptions().getDropshipContractPercent() == 0) {
                    continue;
                }
                value = value.plus(getEquipmentContractValue(u, getCampaignOptions().useEquipmentContractSaleValue()));
            } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_WARSHIP)) {
                if (getCampaignOptions().getWarshipContractPercent() == 0) {
                    continue;
                }
                value = value.plus(getEquipmentContractValue(u, getCampaignOptions().useEquipmentContractSaleValue()));
            } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) || u.getEntity().hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
                if (getCampaignOptions().getJumpshipContractPercent() == 0) {
                    continue;
                }
                value = value.plus(getEquipmentContractValue(u, getCampaignOptions().useEquipmentContractSaleValue()));
            } else {
                value = value.plus(getEquipmentContractValue(u, getCampaignOptions().useEquipmentContractSaleValue()));
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
            percentValue = value.multipliedBy(getCampaignOptions().getDropshipContractPercent()).dividedBy(100);
        } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            percentValue = value.multipliedBy(getCampaignOptions().getWarshipContractPercent()).dividedBy(100);
        } else if (u.getEntity().hasETypeFlag(Entity.ETYPE_JUMPSHIP) || u.getEntity().hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            percentValue = value.multipliedBy(getCampaignOptions().getJumpshipContractPercent()).dividedBy(100);
        } else {
            percentValue = value.multipliedBy(getCampaignOptions().getEquipmentContractPercent()).dividedBy(100);
        }

        return percentValue;
    }

    public Money getContractBase() {
        if (getCampaignOptions().usePeacetimeCost()) {
            return getPeacetimeCost()
                    .multipliedBy(0.75)
                    .plus(getForceValue(getCampaignOptions().useInfantryDontCount()));
        } else if (getCampaignOptions().useEquipmentContractBase()) {
            return getForceValue(getCampaignOptions().useInfantryDontCount());
        } else {
            return getTheoreticalPayroll(getCampaignOptions().useInfantryDontCount());
        }
    }
}
