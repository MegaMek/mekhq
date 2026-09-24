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
package mekhq.campaign;

import mekhq.campaign.enums.LithiumFusionBatteryMode;

/**
 * Describes how a fleet's jump drives behave for travel-time purposes, beyond what the planetary system itself
 * dictates. It carries the effect of Lithium-Fusion batteries into both the live day-by-day travel simulation and the
 * journey-time estimates.
 *
 * @param rechargeMultiplier the multiplier applied to every system's recharge time; {@code 1.0} is standard, {@code 0.5}
 *                           is the {@link LithiumFusionBatteryMode#HALVED_RECHARGE} rule
 * @param storedJumpCharges  the number of stored jump charges (charged LF batteries) available at the start of the
 *                           journey, each of which skips the recharge wait at one intermediate system
 *
 * @author Illiani
 * @since 0.51.01
 */
public record JumpDriveProfile(double rechargeMultiplier, int storedJumpCharges) {
    /** A fleet with no Lithium-Fusion benefit. */
    public static final JumpDriveProfile STANDARD = new JumpDriveProfile(1.0, 0);

    private static final double HALVED_RECHARGE_MULTIPLIER = 0.5;

    /**
     * Builds the profile for a fleet using the given effective Lithium-Fusion battery mode.
     *
     * @param mode             the effective mode; pass {@link LithiumFusionBatteryMode#DISABLED} when the fleet does not
     *                         qualify for the benefit
     * @param isBatteryCharged whether the fleet's batteries currently hold a charge; only relevant for
     *                         {@link LithiumFusionBatteryMode#DOUBLE_JUMP}
     *
     * @return the matching profile
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JumpDriveProfile fromMode(LithiumFusionBatteryMode mode, boolean isBatteryCharged) {
        return switch (mode) {
            case HALVED_RECHARGE -> new JumpDriveProfile(HALVED_RECHARGE_MULTIPLIER, 0);
            case DOUBLE_JUMP -> new JumpDriveProfile(1.0, isBatteryCharged ? 1 : 0);
            case DISABLED -> STANDARD;
        };
    }

    /**
     * Applies this profile's multiplier to a system's recharge time. Infinite recharge times (recharging impossible)
     * stay infinite.
     *
     * @param rechargeHours the system's recharge time in hours
     *
     * @return the adjusted recharge time in hours
     *
     * @author Illiani
     * @since 0.51.01
     */
    public double adjustRechargeTime(double rechargeHours) {
        return rechargeHours * rechargeMultiplier;
    }
}
