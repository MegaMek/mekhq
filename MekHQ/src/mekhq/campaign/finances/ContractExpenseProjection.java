/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Maintenance;
import mekhq.campaign.unit.Unit;

/**
 * Projects the expenses the force will be charged over a contract it has not yet taken, as they would actually be
 * charged: on the target planet for the contract term, and off contract while travelling out to it. Used both to set
 * the cost-based Base Pay and to estimate a contract offer's profit.
 *
 * <p>The offer is assumed to be the only contract, so the transit out is off contract, and the force is assumed to be
 * on the target planet for the whole term.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractExpenseProjection {
    /** The average length of a month, in days. */
    public static final double DAYS_PER_MONTH = 365.25 / 12;

    private ContractExpenseProjection() {
    }

    /**
     * @param contract the contract to check
     *
     * @return {@code true} if the contract is neither Garrison Duty nor a Retainer, so maintenance runs at full pace
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isCombatContract(AbstractContract contract) {
        ContractObjectiveType objectiveType = contract.getObjectiveType();
        return (objectiveType != null) && !objectiveType.isGarrisonDuty() && !objectiveType.isRetainer();
    }

    /**
     * The maintenance the whole hangar will be charged in an average month of the contract term, before straight
     * support.
     *
     * @param campaign the campaign
     * @param contract the contract being projected
     *
     * @return the projected monthly maintenance on the contract
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getMonthlyContractMaintenance(Campaign campaign, AbstractContract contract) {
        boolean isCombatContract = isCombatContract(contract);
        Money dailyMaintenance = Money.zero();
        for (Unit unit : campaign.getPlayerForce().getHangar().getUnits()) {
            dailyMaintenance = dailyMaintenance.plus(Maintenance.getProjectedDailyMaintenanceCost(campaign,
                  unit,
                  true,
                  isCombatContract,
                  false));
        }
        return dailyMaintenance.multipliedBy(DAYS_PER_MONTH);
    }

    /**
     * The maintenance the whole hangar will be charged while travelling out to a contract, off contract.
     *
     * @param campaign    the campaign
     * @param transitDays the length of the journey, in days
     *
     * @return the projected maintenance over the journey
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getTransitMaintenance(Campaign campaign, int transitDays) {
        return getTransitMaintenance(campaign, transitDays, List.of());
    }

    /**
     * As {@link #getTransitMaintenance(Campaign, int)}, leaving out units that will be mothballed for the journey, as
     * a mothballed unit needs no maintenance.
     *
     * @param campaign        the campaign
     * @param transitDays     the length of the journey, in days
     * @param mothballedUnits the units that will be mothballed before departure
     *
     * @return the projected maintenance over the journey
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getTransitMaintenance(Campaign campaign, int transitDays, Collection<Unit> mothballedUnits) {
        if (transitDays <= 0) {
            return Money.zero();
        }

        Money dailyMaintenance = Money.zero();
        for (Unit unit : campaign.getPlayerForce().getHangar().getUnits()) {
            if (mothballedUnits.contains(unit)) {
                continue;
            }

            dailyMaintenance = dailyMaintenance.plus(Maintenance.getProjectedDailyMaintenanceCost(campaign,
                  unit,
                  false,
                  false,
                  true));
        }
        return dailyMaintenance.multipliedBy(transitDays);
    }

    /**
     * The food and housing charged each month on the contract's target planet, with the faction-standing barrack cost
     * multiplier for the contract's employer.
     *
     * @param campaign the campaign
     * @param contract the contract being projected
     *
     * @return the projected monthly food and housing on the contract
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getMonthlyContractFoodAndHousing(Campaign campaign, AbstractContract contract) {
        return Accountant.getMonthlyFoodAndHousingExpenses(campaign,
              getPersonnel(campaign),
              true,
              contract.getTargetSystem(),
              List.of(contract));
    }

    /**
     * The food charged each month while travelling. Housing is only charged on a planet, so none is charged in transit.
     *
     * @param campaign the campaign
     *
     * @return the projected monthly food in transit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getMonthlyTransitFood(Campaign campaign) {
        return Accountant.getMonthlyFoodAndHousingExpenses(campaign, getPersonnel(campaign), false, null, List.of());
    }

    private static List<Person> getPersonnel(Campaign campaign) {
        return new ArrayList<>(campaign.getPlayerForce().getHumanResources().getPersonnel());
    }
}
