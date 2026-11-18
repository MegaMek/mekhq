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
package mekhq.campaign.digitalGM.stratCon;

import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.mission.contract.AbstractContract;

/**
 * How often each thing the enemy and the employer bring to a StratCon contract turns up. Everything is counted per
 * point of the contract's Scale, each month, and the product is rounded normally:
 *
 * <ul>
 *     <li>Essential scenarios: {@value #ESSENTIAL_SCENARIOS_PER_SCALE} a month per point of Scale.</li>
 *     <li>Nav Points (the contract type's own): {@value #NAV_POINTS_PER_SCALE} a month per point of Scale.</li>
 *     <li>Enemy engineers: {@value #ENGINEERS_PER_SCALE} a month per point of Scale.</li>
 *     <li>Facility attacks: {@value #FACILITY_ATTACKS_PER_SCALE} a month per point of Scale.</li>
 *     <li>Normal scenario rolls: {@value #NORMAL_SCENARIO_ROLLS_PER_SCALE} each week per point of Scale.</li>
 *     <li>Deployment Orders: the contract's command step a month per point of Scale (see
 *     {@link StratConDeploymentOrders}).</li>
 * </ul>
 *
 * <p>Anything that comes at all comes at least once, so a contract whose Scale rounds to nothing still gets one.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConSpawnRates {
    /** Essential scenarios a month, per point of Scale. */
    public static final int ESSENTIAL_SCENARIOS_PER_SCALE = 1;
    /** Nav Points a month, per point of Scale. */
    public static final int NAV_POINTS_PER_SCALE = 3;
    /** Enemy engineers a month, per point of Scale. */
    public static final int ENGINEERS_PER_SCALE = 1;
    /** Facility attacks a month, per point of Scale. */
    public static final int FACILITY_ATTACKS_PER_SCALE = 1;
    /** Normal scenario rolls each week, per point of Scale. */
    public static final int NORMAL_SCENARIO_ROLLS_PER_SCALE = 1;

    private StratConSpawnRates() {}

    /**
     * Works out how many of something come each period.
     *
     * @param exactScale    the contract's exact Scale (see {@link AbstractContract#getExactScale()})
     * @param ratePerScale  how many come per point of Scale; zero or less for none
     *
     * @return {@code exactScale * ratePerScale}, rounded normally, but never below one; zero if the rate is zero or less
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getCount(double exactScale, double ratePerScale) {
        if (ratePerScale <= 0) {
            return 0;
        }

        return max(1, (int) Math.round(exactScale * ratePerScale));
    }

    /**
     * As {@link #getCount(double, double)}, for a contract.
     *
     * @param contract     the contract
     * @param ratePerScale how many come per point of Scale; zero or less for none
     *
     * @return the count
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getCount(AbstractContract contract, double ratePerScale) {
        return getCount(contract.getExactScale(), ratePerScale);
    }

    /**
     * Lays out the same count in every month of a contract, ready for
     * {@link StratConContractInitializer#rollSpawnDates} to give each a random day within its month.
     *
     * @param lengthInMonths the contract's length in months; a contract under a month still gets one
     * @param monthlyCount   how many come each month
     *
     * @return one entry per month, each {@code monthlyCount}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Integer> getMonthlySchedule(int lengthInMonths, int monthlyCount) {
        List<Integer> schedule = new ArrayList<>();
        for (int month = 0; month < max(1, lengthInMonths); month++) {
            schedule.add(max(0, monthlyCount));
        }
        return schedule;
    }

    /**
     * As {@link #getMonthlySchedule(int, int)}, at the given rate for the contract's Scale and over its length.
     *
     * @param contract     the contract
     * @param ratePerScale how many come each month per point of Scale; zero or less for none
     *
     * @return one entry per month
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Integer> getMonthlySchedule(AbstractContract contract, double ratePerScale) {
        return getMonthlySchedule(contract.getLengthInMonths(), getCount(contract, ratePerScale));
    }
}
