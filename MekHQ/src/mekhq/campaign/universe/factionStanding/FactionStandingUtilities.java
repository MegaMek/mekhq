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

import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.mission.AtBContract;


public class FactionStandingUtilities {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingUtilities.class);

    /**
     * Determines the {@link FactionStandingLevel} corresponding to the given regard value.
     *
     * <p>Iterates through all defined standing levels and returns the one whose regard range (exclusive of minimum,
     * and inclusive of maximum) contains the provided regard value.</p>
     *
     * <p>If the regard value does not fall within any defined standing level range, this method logs a warning and
     * returns {@link FactionStandingLevel#STANDING_LEVEL_4} as a default.</p>
     *
     * @param regard the regard value to evaluate
     *
     * @return the matching {@code FactionStandingLevel} for the given regard, or {@code STANDING_LEVEL_4} if no match is
     *       found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionStandingLevel calculateFactionStandingLevel(double regard) {
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            if (regard > standingLevel.getMinimumRegard() && regard <= standingLevel.getMaximumRegard()) {
                return standingLevel;
            }
        }

        // I'm not expecting this to happen given we already accept all values between Integer#MIN_VALUE and
        // Integer#MAX_VALUE. But if it somehow does, we'll just return STANDING_LEVEL_4 as a default.
        LOGGER.warn("Regard value {} is outside of the faction standing level range. Returning STANDING_LEVEL_4.",
              FactionStandingLevel.STANDING_LEVEL_4);

        return FactionStandingLevel.STANDING_LEVEL_4;
    }

    /**
     * Retrieves the current standing level based on the provided regard value.
     *
     * @param regard the regard value used to evaluate the faction standing level
     *
     * @return the corresponding standing level as an integer
     *
     * @author Illiani
     * @see FactionStandingLevel#getStandingLevel()
     * @since 0.50.07
     */
    public static int getStandingLevel(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getStandingLevel();
    }

    /**
     * Retrieves the negotiation modifier associated with the provided regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the negotiation modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getNegotiationModifier()
     * @since 0.50.07
     */
    public static int getNegotiationModifier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getNegotiationModifier();
    }

    /**
     * Returns the resupply weight modifier for the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the resupply weight modifier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getResupplyWeightModifier()
     * @since 0.50.07
     */
    public static double getResupplyWeightModifier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getResupplyWeightModifier();
    }

    /**
     * Determines if the command circuit access is available at the given regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return {@code true} if command circuit access is granted; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#hasCommandCircuitAccess()
     * @since 0.50.07
     */
    public static boolean hasCommandCircuitAccess(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.hasCommandCircuitAccess();
    }

    /**
     * Checks whether the specified regard value results in outlawed status.
     *
     * @param regard the regard value to evaluate
     *
     * @return {@code true} if outlawed; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#isOutlawed()
     * @since 0.50.07
     */
    public static boolean isOutlawed(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.isOutlawed();
    }

    /**
     * Checks if Batchalls are allowed for the provided regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return {@code true} if Batchall is allowed; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#isBatchallAllowed()
     * @since 0.50.07
     */
    public static boolean isBatchallAllowed(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.isBatchallAllowed();
    }

    /**
     * Returns the number of recruitment tickets granted for the given regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the number of recruitment tickets
     *
     * @author Illiani
     * @see FactionStandingLevel#getRecruitmentTickets()
     * @since 0.50.07
     */
    public static int getRecruitmentTickets(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getRecruitmentTickets();
    }

    /**
     * Returns the recruitment rolls modifier based on the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the recruitment rolls modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getRecruitmentRollsModifier()
     * @since 0.50.07
     */
    public static int getRecruitmentRollsModifier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getRecruitmentRollsModifier();
    }

    /**
     * Retrieves the barrack costs multiplier for the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the barrack costs multiplier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getBarrackCostsMultiplier()
     * @since 0.50.07
     */
    public static double getBarrackCostsMultiplier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getBarrackCostsMultiplier();
    }

    /**
     * Returns the unit market rarity modifier for the given regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the unit market rarity modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getUnitMarketRarityModifier()
     * @since 0.50.07
     */
    public static int getUnitMarketRarityModifier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getUnitMarketRarityModifier();
    }

    /**
     * Retrieves the contract pay multiplier corresponding to the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the contract pay multiplier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getContractPayMultiplier()
     * @since 0.50.07
     */
    public static double getContractPayMultiplier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getContractPayMultiplier();
    }

    /**
     * Returns the support point modifier applied at the start of a contract for the given regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the support point modifier for contract start
     *
     * @author Illiani
     * @see FactionStandingLevel#getSupportPointModifierContractStart()
     * @since 0.50.07
     */
    public static int getSupportPointModifierContractStart(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getSupportPointModifierContractStart();
    }

    /**
     * Returns the periodic support point modifier for the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the periodic support point modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getSupportPointModifierPeriodic()
     * @since 0.50.07
     */
    public static int getSupportPointModifierPeriodic(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getSupportPointModifierPeriodic();
    }

    /**
     * Determines whether command circuit access should be granted based on campaign settings, game master mode, current
     * faction standings, and a list of active contracts.
     *
     * <p>Access is immediately granted if both command circuit requirements are overridden and game master mode is
     * active. If not, and if faction standing is used as a criterion, the method evaluates the player's highest faction
     * regard across all active contracts, granting access if this level meets the threshold.</p>
     *
     * <p>If there are no active contracts, access is denied.</p>
     *
     * @param overridingCommandCircuitRequirements {@code true} if command circuit requirements are overridden
     * @param isGM                                 {@code true} if game master mode is enabled
     * @param useFactionStandingCommandCircuit     {@code true} if faction standing is used to determine access
     * @param factionStandings                     player faction standing data
     * @param activeContracts                      list of currently active contracts to evaluate for access
     *
     * @return {@code true} if command circuit access should be used; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static boolean isUseCommandCircuit(boolean overridingCommandCircuitRequirements, boolean isGM,
          boolean useFactionStandingCommandCircuit, FactionStandings factionStandings,
          List<AtBContract> activeContracts) {
        boolean useCommandCircuit = overridingCommandCircuitRequirements && isGM;

        if (useCommandCircuit) {
            return true;
        }

        if (activeContracts.isEmpty()) {
            return false;
        }

        double highestRegard = FactionStandingLevel.STANDING_LEVEL_0.getMinimumRegard();
        if (useFactionStandingCommandCircuit) {
            for (AtBContract contract : activeContracts) {
                double currentRegard = factionStandings.getRegardForFaction(contract.getEmployerCode(), true);
                if (currentRegard > highestRegard) {
                    highestRegard = currentRegard;
                }
            }
        }

        useCommandCircuit = hasCommandCircuitAccess(highestRegard);
        return useCommandCircuit;
    }
}
