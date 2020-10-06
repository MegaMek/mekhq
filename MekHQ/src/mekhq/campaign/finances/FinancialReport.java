/*
 * FinancialReport.java
 *
 * Copyright (c) 2019 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.finances;

import java.util.stream.Collectors;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Dropship;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.unit.Unit;

public class FinancialReport {
    private Money assets = Money.zero();
    private Money liabilities = Money.zero();
    private Money cash = Money.zero();
    private Money loans = Money.zero();
    private Money mech = Money.zero();
    private Money vee = Money.zero();
    private Money ba = Money.zero();
    private Money infantry = Money.zero();
    private Money smallCraft = Money.zero();
    private Money largeCraft = Money.zero();
    private Money proto = Money.zero();
    private Money spareParts = Money.zero();
    private Money coSpareParts = Money.zero();
    private Money coFuel = Money.zero();
    private Money coAmmo = Money.zero();
    private Money maintenance = Money.zero();
    private Money salaries = Money.zero();
    private Money overhead = Money.zero();
    private Money contracts = Money.zero();

    public Money getNetWorth() {
        return getTotalAssets().minus(getTotalLiabilities());
    }

    public Money getTotalAssets() {
        return assets.plus(cash).plus(mech).plus(vee).plus(ba).plus(infantry).plus(largeCraft)
                    .plus(smallCraft).plus(proto).plus(spareParts);
    }

    public Money getTotalLiabilities() {
        return liabilities.plus(loans);
    }

    public Money getMonthlyIncome() {
        return contracts;
    }

    public Money getMonthlyExpenses() {
        return maintenance.plus(salaries).plus(overhead).plus(coSpareParts).plus(coAmmo).plus(coFuel);
    }

    public Money getCash() {
        return cash;
    }

    public Money getLoans() {
        return loans;
    }

    public Money getContracts() {
        return contracts;
    }

    public Money getOverheadCosts() {
        return overhead;
    }

    public Money getSalaries() {
        return salaries;
    }

    public Money getMaintenance() {
        return maintenance;
    }

    public Money getMonthlyAmmoCosts() {
        return coAmmo;
    }

    public Money getMonthlyFuelCosts() {
        return coFuel;
    }

    public Money getMonthlySparePartCosts() {
        return coSpareParts;
    }

    public Money getSparePartsValue() {
        return spareParts;
    }

    public Money getProtomechValue() {
        return proto;
    }

    public Money getLargeCraftValue() {
        return largeCraft;
    }

    public Money getSmallCraftValue() {
        return smallCraft;
    }

    public Money getInfantryValue() {
        return infantry;
    }

    public Money getBattleArmorValue() {
        return ba;
    }

    public Money getVeeValue() {
        return vee;
    }

    public Money getMechValue() {
        return mech;
    }

    public static FinancialReport calculate(Campaign campaign) {
        FinancialReport r = new FinancialReport();

        r.cash = campaign.getFinances().getBalance();
        r.loans = campaign.getFinances().getLoanBalance();
        r.assets = campaign.getFinances().getTotalAssetValue();

        campaign.getHangar().forEachUnit(u -> {
            Money value = u.getSellValue();
            if (u.getEntity() instanceof Mech) {
                r.mech = r.mech.plus(value);
            } else if (u.getEntity() instanceof Tank) {
                r.vee = r.vee.plus(value);
            } else if (u.getEntity() instanceof BattleArmor) {
                r.ba = r.ba.plus(value);
            } else if (u.getEntity() instanceof Infantry) {
                r.infantry = r.infantry.plus(value);
            } else if (u.getEntity() instanceof Dropship
                    || u.getEntity() instanceof Jumpship) {
                r.largeCraft = r.largeCraft.plus(value);
            } else if (u.getEntity() instanceof Aero) {
                r.smallCraft = r.smallCraft.plus(value);
            } else if (u.getEntity() instanceof Protomech) {
                r.proto = r.proto.plus(value);
            }
        });

        r.spareParts = r.spareParts.plus(
            campaign.streamSpareParts()
                .map(x -> x.getActualValue().multipliedBy(x.getQuantity()))
                .collect(Collectors.toList()));

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Accountant accountant = new Accountant(campaign);

        if (campaignOptions.payForMaintain()) {
            r.maintenance = accountant.getWeeklyMaintenanceCosts().multipliedBy(4);
        }
        if (campaignOptions.payForSalaries()) {
            r.salaries = accountant.getPayRoll();
        }
        if (campaignOptions.payForOverhead()) {
            r.overhead = accountant.getOverheadExpenses();
        }
        if (campaignOptions.usePeacetimeCost()) {
            r.coSpareParts = accountant.getMonthlySpareParts();
            r.coAmmo = accountant.getMonthlyAmmo();
            r.coFuel = accountant.getMonthlyFuel();
        }

        r.contracts = r.contracts.plus(
            campaign.getActiveContracts()
                .stream().map(Contract::getMonthlyPayOut)
                .collect(Collectors.toList()));

        return r;
    }
}
