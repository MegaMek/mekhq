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

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
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
 * <p>The rates are for Scale counted with battlefield support points factored in, which makes for smaller Scales.
 * Without that option, every {@value #SUPPORT_POINT_SCALE_RATIO} points of Scale count as one (see
 * {@link #getSpawnScale}), as they do for sizing facilities, so the option does not change how often anything
 * comes.</p>
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

    /**
     * How many points of a contract's Scale count as one when battlefield support points are not factored into Scale,
     * which makes for larger Scales; also used to size facilities (see {@link StratConContractInitializer}).
     */
    public static final int SUPPORT_POINT_SCALE_RATIO = 3;

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
     * @param exactScale                     the contract's exact Scale (see {@link AbstractContract#getExactScale()})
     * @param isFactorSupportPointsIntoScale whether the contract's Scale was worked out with battlefield support
     *                                       points factored in
     *
     * @return the Scale the rates are counted against: the exact Scale with support points factored in, and a
     *       {@value #SUPPORT_POINT_SCALE_RATIO}th of it without
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getSpawnScale(double exactScale, boolean isFactorSupportPointsIntoScale) {
        return isFactorSupportPointsIntoScale ? exactScale : (exactScale / SUPPORT_POINT_SCALE_RATIO);
    }

    /**
     * As {@link #getCount(double, double)}, for a contract.
     *
     * @param contract                       the contract
     * @param isFactorSupportPointsIntoScale whether the contract's Scale was worked out with battlefield support
     *                                       points factored in (see {@link #isFactorSupportPointsIntoScale})
     * @param ratePerScale                   how many come per point of Scale; zero or less for none
     *
     * @return the count
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getCount(AbstractContract contract, boolean isFactorSupportPointsIntoScale,
          double ratePerScale) {
        return getCount(getSpawnScale(contract.getExactScale(), isFactorSupportPointsIntoScale), ratePerScale);
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
     * @param contract                       the contract
     * @param isFactorSupportPointsIntoScale whether the contract's Scale was worked out with battlefield support
     *                                       points factored in (see {@link #isFactorSupportPointsIntoScale})
     * @param ratePerScale                   how many come each month per point of Scale; zero or less for none
     *
     * @return one entry per month
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<Integer> getMonthlySchedule(AbstractContract contract, boolean isFactorSupportPointsIntoScale,
          double ratePerScale) {
        return getMonthlySchedule(contract.getLengthInMonths(),
              getCount(contract, isFactorSupportPointsIntoScale, ratePerScale));
    }

    /**
     * Whether a contract's Scale was worked out with battlefield support points factored in. A contract that has been
     * accepted keeps the option as it stood then (see
     * {@link StratConCampaignState#getSupportPointsFactoredIntoScale()}); an offer, or a contract from before that was
     * recorded, reads the option as it stands now.
     *
     * @param campaignOptions the campaign options
     * @param contract        the contract
     *
     * @return {@code true} if support points were factored into the contract's Scale
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isFactorSupportPointsIntoScale(CampaignOptions campaignOptions, AbstractContract contract) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if ((campaignState != null) && (campaignState.getSupportPointsFactoredIntoScale() != null)) {
            return campaignState.getSupportPointsFactoredIntoScale();
        }
        return isFactorSupportPointsIntoScale(campaignOptions);
    }

    /**
     * @param campaignOptions the campaign options
     *
     * @return the "Use Chaos Scale Support Point Conversion" option; on, its default, if the options do not say (as a
     *       mocked options object does not)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isFactorSupportPointsIntoScale(CampaignOptions campaignOptions) {
        return !Boolean.FALSE.equals(campaignOptions.get(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION));
    }

    /**
     * As {@link #isFactorSupportPointsIntoScale(CampaignOptions, AbstractContract)}, for scheduling that has only the
     * contract's StratCon state to go on. A state from before the option was recorded reads as the option's default,
     * on; the daily StratCon lifecycle records it from the campaign options for such a state (see
     * {@link #recordSupportPointsFactoredIntoScale}).
     *
     * @param campaignState the contract's StratCon campaign state
     *
     * @return {@code true} if support points were factored into the contract's Scale
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isFactorSupportPointsIntoScale(StratConCampaignState campaignState) {
        return !Boolean.FALSE.equals(campaignState.getSupportPointsFactoredIntoScale());
    }

    /**
     * Records on a contract's StratCon state whether its Scale was worked out with battlefield support points factored
     * in, if it has not been recorded yet: from the campaign options as they stand now.
     *
     * @param campaignOptions the campaign options
     * @param campaignState   the contract's StratCon campaign state
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void recordSupportPointsFactoredIntoScale(CampaignOptions campaignOptions,
          StratConCampaignState campaignState) {
        if (campaignState.getSupportPointsFactoredIntoScale() == null) {
            campaignState.setSupportPointsFactoredIntoScale(isFactorSupportPointsIntoScale(campaignOptions));
        }
    }
}
