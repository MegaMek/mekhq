/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * (GPL), version 3 or (at your option) any later version, as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project; if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers creating free software for the BattleTech
 * community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or affiliated with Microsoft.
 */
package mekhq.campaign.chaosCampaign;

import mekhq.campaign.universe.enums.HiringHallLevel;

/**
 * Unofficial limits that stop large Chaos Campaign forces snowballing: bigger forces earn more, which funds bigger
 * forces. Each limit is enabled by its own campaign option.
 *
 * <ul>
 *     <li>Contract Scale is capped by the Hiring Hall of the system the offer comes from.</li>
 *     <li>Combat pay and salvage rights taper off above {@link #FULL_VALUE_SCALE}.</li>
 *     <li>Hot Spots upkeep rises faster than force size above {@link #UPKEEP_ESCALATION_THRESHOLD}.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ChaosScaleLimits {
    /** Scale up to which combat pay and salvage are earned in full */
    static final int FULL_VALUE_SCALE = 3;
    /** The value of each point of Scale above {@link #FULL_VALUE_SCALE} */
    static final double TAPERED_SCALE_VALUE = 0.5;
    /** TO&amp;E Scale above which upkeep escalates */
    static final int UPKEEP_ESCALATION_THRESHOLD = 4;
    /** Extra upkeep per point of TO&amp;E Scale above {@link #UPKEEP_ESCALATION_THRESHOLD} */
    static final double UPKEEP_ESCALATION_PER_SCALE = 0.05;

    private ChaosScaleLimits() {
    }

    /**
     * @param hiringHallLevel the Hiring Hall of the system the contract is offered in
     *
     * @return the largest contract Scale that Hiring Hall brokers, or {@link Integer#MAX_VALUE} if uncapped
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getMaximumContractScale(HiringHallLevel hiringHallLevel) {
        return switch (hiringHallLevel) {
            case GREAT -> Integer.MAX_VALUE;
            case STANDARD -> 8;
            case MINOR -> 6;
            case QUESTIONABLE -> 4;
            case NONE -> 2;
        };
    }

    /**
     * Scale with diminishing returns: full value up to {@link #FULL_VALUE_SCALE}, then {@link #TAPERED_SCALE_VALUE} per
     * point. For example, Scale 7 is worth 5.
     *
     * @param scale the contract Scale
     *
     * @return the effective Scale
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getTaperedScale(int scale) {
        if (scale <= FULL_VALUE_SCALE) {
            return scale;
        }
        return FULL_VALUE_SCALE + ((scale - FULL_VALUE_SCALE) * TAPERED_SCALE_VALUE);
    }

    /**
     * @param scale the contract Scale
     *
     * @return the fraction of full value earned at this Scale, {@code (0, 1]}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getTaperMultiplier(int scale) {
        if (scale <= FULL_VALUE_SCALE) {
            return 1.0;
        }
        return getTaperedScale(scale) / scale;
    }

    /**
     * @param tableOfOrganizationScale the Scale of the player's entire TO&amp;E
     *
     * @return the Hot Spots upkeep multiplier, e.g. ×1.30 at Scale 10
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getUpkeepEscalationMultiplier(int tableOfOrganizationScale) {
        int scaleAboveThreshold = Math.max(0, tableOfOrganizationScale - UPKEEP_ESCALATION_THRESHOLD);
        return 1.0 + (scaleAboveThreshold * UPKEEP_ESCALATION_PER_SCALE);
    }
}
