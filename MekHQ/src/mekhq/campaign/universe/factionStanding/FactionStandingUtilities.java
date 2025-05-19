/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import megamek.logging.MMLogger;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;


public class FactionStandingUtilities {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingUtilities.class);

    /**
     * Determines the {@link FactionStandingLevel} corresponding to the given fame value.
     *
     * <p>Iterates through all defined standing levels and returns the one whose fame range (inclusive of minimum
     * and maximum) contains the provided fame value.</p>
     *
     * <p>If the fame value does not fall within any defined standing level range, this method logs a warning and
     * returns {@link FactionStandingLevel#STANDING_LEVEL_4} as a default.</p>
     *
     * @param fame the fame value to evaluate
     *
     * @return the matching {@code FactionStandingLevel} for the given fame, or {@code STANDING_LEVEL_4} if no match is
     *       found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionStandingLevel calculateFactionStandingLevel(int fame) {
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            if (fame >= standingLevel.getMinimumFame() && fame <= standingLevel.getMaximumFame()) {
                return standingLevel;
            }
        }

        // I'm not expecting this to happen given we already accept all values between Integer#MIN_VALUE and
        // Integer#MAX_VALUE. But if it somehow does, we'll just return STANDING_LEVEL_4 as a default.
        LOGGER.warn("Fame value {} is outside of the faction standing level range. Returning STANDING_LEVEL_4.",
              FactionStandingLevel.STANDING_LEVEL_4);

        return FactionStandingLevel.STANDING_LEVEL_4;
    }

    /**
     * Retrieves the current standing level based on the provided fame value.
     *
     * @param fame the fame value used to evaluate the faction standing level
     *
     * @return the corresponding standing level as an integer
     *
     * @author Illiani
     * @see FactionStandingLevel#getStandingLevel()
     * @since 0.50.07
     */
    public static int getStandingLevel(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getStandingLevel();
    }

    /**
     * Retrieves the negotiation modifier associated with the provided fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the negotiation modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getNegotiationModifier()
     * @since 0.50.07
     */
    public static int getNegotiationModifier(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getNegotiationModifier();
    }

    /**
     * Returns the resupply weight modifier for the specified fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the resupply weight modifier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getResupplyWeightModifier()
     * @since 0.50.07
     */
    public static double getResupplyWeightModifier(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getResupplyWeightModifier();
    }

    /**
     * Determines if the command circuit access is available at the given fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return {@code true} if command circuit access is granted; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#hasCommandCircuitAccess()
     * @since 0.50.07
     */
    public static boolean hasCommandCircuitAccess(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.hasCommandCircuitAccess();
    }

    /**
     * Checks whether the specified fame value results in outlawed status.
     *
     * @param fame the fame value to evaluate
     *
     * @return {@code true} if outlawed; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#isOutlawed()
     * @since 0.50.07
     */
    public static boolean isOutlawed(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.isOutlawed();
    }

    /**
     * Checks if Batchalls are allowed for the provided fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return {@code true} if Batchall is allowed; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#isBatchallAllowed()
     * @since 0.50.07
     */
    public static boolean isBatchallAllowed(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.isBatchallAllowed();
    }

    /**
     * Returns the number of recruitment tickets granted for the given fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the number of recruitment tickets
     *
     * @author Illiani
     * @see FactionStandingLevel#getRecruitmentTickets()
     * @since 0.50.07
     */
    public static int getRecruitmentTickets(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getRecruitmentTickets();
    }

    /**
     * Returns the recruitment rolls modifier based on the specified fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the recruitment rolls modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getRecruitmentRollsModifier()
     * @since 0.50.07
     */
    public static int getRecruitmentRollsModifier(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getRecruitmentRollsModifier();
    }

    /**
     * Retrieves the barrack costs multiplier for the specified fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the barrack costs multiplier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getBarrackCostsMultiplier()
     * @since 0.50.07
     */
    public static double getBarrackCostsMultiplier(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getBarrackCostsMultiplier();
    }

    /**
     * Returns the unit market rarity modifier for the given fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the unit market rarity modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getUnitMarketRarityModifier()
     * @since 0.50.07
     */
    public static int getUnitMarketRarityModifier(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getUnitMarketRarityModifier();
    }

    /**
     * Retrieves the contract pay multiplier corresponding to the specified fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the contract pay multiplier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getContractPayMultiplier()
     * @since 0.50.07
     */
    public static double getContractPayMultiplier(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getContractPayMultiplier();
    }

    /**
     * Returns the support point modifier applied at the start of a contract for the given fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the support point modifier for contract start
     *
     * @author Illiani
     * @see FactionStandingLevel#getSupportPointModifierContractStart()
     * @since 0.50.07
     */
    public static int getSupportPointModifierContractStart(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getSupportPointModifierContractStart();
    }

    /**
     * Returns the periodic support point modifier for the specified fame value.
     *
     * @param fame the fame value to evaluate
     *
     * @return the periodic support point modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getSupportPointModifierPeriodic()
     * @since 0.50.07
     */
    public static int getSupportPointModifierPeriodic(final int fame) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(fame);

        return standing.getSupportPointModifierPeriodic();
    }
}
