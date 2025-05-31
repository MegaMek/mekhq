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
package mekhq.campaign.universe.factionStanding.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.util.ArrayList;
import java.util.List;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;

/**
 * Represents a standing level within the Faction Standing reputation system.
 *
 * <p>The {@code FactionStanding} enum defines distinct standing levels that a campaign can have with a particular
 * faction. Each standing level determines a set of modifiers and properties that affect various mechanics such as
 * negotiations, recruitment, contract payments, access to special features, and penalties for outlaw status.</p>
 *
 * <p>Each standing level encapsulates:</p>
 * <ul>
 *   <li>The regard threshold range for the level</li>
 *   <li>Contract negotiation and payment modifiers</li>
 *   <li>Resource, recruitment, market, and cost multipliers</li>
 *   <li>Special status indicators, such as outlawed state or command circuit access</li>
 *   <li>Support point adjustments for contracts</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum FactionStandingLevel {
    STANDING_LEVEL_0(0, Integer.MIN_VALUE, -50, -4, 0.0, false, true, false, 0, -2, 3.0, -3, 0.6, -2, -4),
    STANDING_LEVEL_1(1, -50, -40, -3, 0.25, false, true, false, 0, -1, 2.0, -2, 0.7, -1, -3),
    STANDING_LEVEL_2(2, -40, -25, -2, 0.5, false, false, true, 1, 0, 1.75, -1, 0.8, -1, -2),
    STANDING_LEVEL_3(3, -25, -10, -1, 0.75, false, false, true, 2, 0, 1.5, 0, 0.9, 0, -1),
    STANDING_LEVEL_4(4, -10, 10, 0, 1.0, false, false, true, 3, 0, 1.0, 0, 1.0, 0, 0),
    STANDING_LEVEL_5(5, 10, 25, 1, 1.9, false, false, true, 4, 0, 1.0, 0, 1.05, 0, 1),
    STANDING_LEVEL_6(6, 25, 40, 2, 1.5, false, false, true, 5, 0, 0.85, 1, 1.1, 1, 1),
    STANDING_LEVEL_7(7, 40, 50, 3, 1.75, true, false, true, 10, 1, 0.80, 2, 1.15, 1, 2),
    STANDING_LEVEL_8(8, 50, Integer.MAX_VALUE, 4, 2.0, true, false, true, 15, 2, 0.75, 3, 1.2, 2, 3);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingLevel.class);

    private final static String FALLBACK_LABEL_SUFFIX_INNER_SPHERE = "innerSphere";
    private final static String FALLBACK_LABEL_SUFFIX_CLAN = "clan";
    private final static String FALLBACK_LABEL_SUFFIX_PERIPHERY = "periphery";

    private final static int MINIMUM_STANDING_LEVEL = 0;
    private final static int MAXIMUM_STANDING_LEVEL = 8;

    private final int standingLevel;
    private final double minimumRegard;
    private final double maximumRegard;
    private final int negotiationModifier;
    private final double resupplyWeightModifier;
    private final boolean hasCommandCircuitAccess;
    private final boolean isOutlawed;
    private final boolean isBatchallAllowed;
    private final int recruitmentTickets;
    private final int recruitmentRollsModifier;
    private final double barrackCostsMultiplier;
    private final int unitMarketRarityModifier;
    private final double contractPayMultiplier;
    private final int supportPointModifierContractStart;
    private final int supportPointModifierPeriodic;

    /**
     * Constructs a standing level with all modifiers and properties.
     *
     * @param standingLevel                     The level of the standing. Should be exclusive to this standing level.
     * @param minimumRegard                       Minimum regard for this level (inclusive).
     * @param maximumRegard                       Maximum regard for this level (exclusive).
     * @param negotiationModifier               Modifier to contract negotiations.
     * @param resupplyWeightModifier            Modifier for resupply weight calculations.
     * @param hasCommandCircuitAccess           Whether Command Circuit access is granted at this level.
     * @param isOutlawed                        Whether the unit is outlawed at this level.
     * @param isBatchallAllowed                 Whether Clan factions will attempt to Batchall the campaign.
     * @param recruitmentTickets                Modifier for recruitment ticket awards.
     * @param barrackCostsMultiplier            Multiplier for barrack costs.
     * @param unitMarketRarityModifier          Modifier applied to unit rarity on markets.
     * @param contractPayMultiplier             Multiplier to contract pay.
     * @param supportPointModifierContractStart Support points at contract start.
     * @param supportPointModifierPeriodic      Periodic support point modifier.
     *
     * @author Illiani
     * @since 0.50.07
     */
    FactionStandingLevel(int standingLevel, int minimumRegard, int maximumRegard, int negotiationModifier,
          double resupplyWeightModifier, boolean hasCommandCircuitAccess, boolean isOutlawed, boolean isBatchallAllowed,
          int recruitmentTickets, int recruitmentRollsModifier, double barrackCostsMultiplier,
          int unitMarketRarityModifier, double contractPayMultiplier, int supportPointModifierContractStart,
          int supportPointModifierPeriodic) {
        this.standingLevel = standingLevel;
        this.minimumRegard = minimumRegard;
        this.maximumRegard = maximumRegard;
        this.negotiationModifier = negotiationModifier;
        this.resupplyWeightModifier = resupplyWeightModifier;
        this.hasCommandCircuitAccess = hasCommandCircuitAccess;
        this.isOutlawed = isOutlawed;
        this.isBatchallAllowed = isBatchallAllowed;
        this.recruitmentTickets = recruitmentTickets;
        this.recruitmentRollsModifier = recruitmentRollsModifier;
        this.barrackCostsMultiplier = barrackCostsMultiplier;
        this.unitMarketRarityModifier = unitMarketRarityModifier;
        this.contractPayMultiplier = contractPayMultiplier;
        this.supportPointModifierContractStart = supportPointModifierContractStart;
        this.supportPointModifierPeriodic = supportPointModifierPeriodic;
    }

    /**
     * @return the lowest possible standing level as an integer
     */
    public static int getMinimumStandingLevel() {
        return MINIMUM_STANDING_LEVEL;
    }

    /**
     * @return the highest possible standing level as an integer.
     */
    public static int getMaximumStandingLevel() {
        return MAXIMUM_STANDING_LEVEL;
    }

    /**
     * Retrieves the current standing level of this faction.
     *
     * @return the standing level as an integer.
     */
    public int getStandingLevel() {
        return standingLevel;
    }

    /**
     * Retrieves the minimum regard value associated with this faction standing.
     *
     * @return the minimum regard as a double.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getMinimumRegard() {
        return minimumRegard;
    }

    /**
     * Retrieves the maximum regard value associated with the faction standing.
     *
     * @return A double representing the maximum regard value.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getMaximumRegard() {
        return maximumRegard;
    }

    /**
     * Retrieves the negotiation modifier associated with the faction standing.
     *
     * <p>This is the modifier to initial contract negotiations and renegotiation checks.</p>
     *
     * @return the negotiation modifier as an integer.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getNegotiationModifier() {
        return negotiationModifier;
    }

    /**
     * Retrieves the resupply weight modifier for the faction standing.
     *
     * <p>This is a multiplier applied to the weight of Resupplies.</p>
     *
     * @return The resupply weight modifier as a double value.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getResupplyWeightModifier() {
        return resupplyWeightModifier;
    }

    /**
     * Checks if the campaign has access to the command circuit.
     *
     * @return true if the campaign has access to the command circuit, false otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean hasCommandCircuitAccess() {
        return hasCommandCircuitAccess;
    }

    /**
     * Indicates whether the campaign is considered outlawed based on its current standing.
     *
     * @return {@code true} if the campaign is outlawed, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isOutlawed() {
        return isOutlawed;
    }

    /**
     * Indicates whether the campaign is considered a valid target for Clan Batchalls.
     *
     * @return {@code true} if the campaign is a valid Batchall target, {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isBatchallAllowed() {
        return isBatchallAllowed;
    }

    /**
     * Retrieves the number of recruitment tickets associated with the faction standing.
     *
     * <p>This represents the willingness of the faction's people to join the campaign.</p>
     *
     * @return The number of recruitment tickets as an integer.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getRecruitmentTickets() {
        return recruitmentTickets;
    }

    /**
     * Retrieves the recruitment rolls modifier associated with a faction's standing.
     *
     * <p>This represents the faction suppressing or promoting the campaign, when the campaign recruits on their
     * planets.</p>
     *
     * @return The recruitment rolls modifier as an integer, which influences the number of rolls or chances available
     *       when recruiting personnel within the current faction standing.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getRecruitmentRollsModifier() {
        return recruitmentRollsModifier;
    }

    /**
     * Retrieves the multiplier for barrack costs associated with this faction standing.
     *
     * <p>This multiplier is applied to housing and food costs while on a planet controlled by the faction.</p>
     *
     * @return The barrack costs multiplier as a double.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getBarrackCostsMultiplier() {
        return barrackCostsMultiplier;
    }

    /**
     * Retrieves the unit market rarity modifier associated with the faction standing.
     *
     * <p>This reduces (or increases) the unit type rarity of units appearing in the 'Employer Market' portion of
     * the Unit Market. It does not affect the rarity of units that appear in the market, just their frequency.</p>
     *
     * @return The unit market rarity modifier as an integer.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getUnitMarketRarityModifier() {
        return unitMarketRarityModifier;
    }

    /**
     * Retrieves the contract pay multiplier for the faction.
     *
     * @return A double representing the multiplier applied to contract pay for the faction.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getContractPayMultiplier() {
        return contractPayMultiplier;
    }

    /**
     * Retrieves the support point modifier for the start of a contract.
     *
     * <p>This is a direct modifier to the number of Support Points a campaign begins a contract with. This should
     * be multiplied by the contract's number of required forces.</p>
     *
     * @return an integer representing the support point modifier at the start of a contract.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getSupportPointModifierContractStart() {
        return supportPointModifierContractStart;
    }

    /**
     * Retrieves the periodic support point modifier associated with the faction.
     *
     * <p>This is a modifier applied to the Administration checks made by personnel to periodically generate Support
     * Points while on contract.</p>
     *
     * @return The periodic support point modifier as an integer value.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getSupportPointModifierPeriodic() {
        return supportPointModifierPeriodic;
    }

    /**
     * Retrieves the label associated with the faction standing, based on the specified {@link Faction}.
     *
     * @param relevantFaction the {@link Faction} used to determine the specific label for the standing. The faction
     *                        helps provide context, such as whether it is a clan, ComStar, or an Inner Sphere faction.
     *
     * @return a {@link String} representing the label for the faction standing.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getLabel(Faction relevantFaction) {
        String key = "factionStandingLevel." + name() + '.' + relevantFaction.getShortName() + ".label";
        String label = getTextAt(RESOURCE_BUNDLE, key);

        if (isResourceKeyValid(label)) {
            return label;
        }

        // Use Fallback
        String fallbackSuffix = getFallbackSuffix(relevantFaction);
        key = "factionStandingLevel." + name() + '.' + fallbackSuffix + ".label";
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    /**
     * Retrieves the description associated with the faction standing, based on the specified {@link Faction}.
     *
     * @param relevantFaction the {@link Faction} used to determine the specific description for the standing.
     *
     * @return a {@link String} representing the description of the faction standing.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getDescription(Faction relevantFaction) {
        String label = " ";

        String key = "factionStandingLevel." + name() + '.' + relevantFaction.getShortName() + ".description";
        label = getTextAt(RESOURCE_BUNDLE, key);

        if (isResourceKeyValid(label)) {
            return label;
        } else {
            // If the key is invalid, we don't want to return an empty string as that will mess with gui spacing.
            // Instead, we return a placeholder white space.
            label = " "; // This can be removed if we ever decide to use the fallback code below.
        }

        // For descriptions, we don't want to use the fallback because the description becomes quickly repetitive, and
        // it detracts from the overall experience. However, if we change our mind, the following code can be used.
        //        // Use Fallback
        //        String fallbackSuffix = getFallbackSuffix(relevantFaction);
        //        key = "factionStandingLevel." + name() + '.' + fallbackSuffix + ".description";
        //        label = getTextAt(RESOURCE_BUNDLE, key);

        return label;
    }

    /**
     * Generates a textual description of all effects based on the current faction standing modifiers.
     *
     * <p>This method inspects various modifiers (such as negotiation, resupply, command circuit access, outlaw status,
     * batchall permission, recruitment popularity, barracks cost, unit market rarity, contract pay, and support point
     * modifiers) and compiles their effects into a comma-separated string. Only effects that deviate from their default
     * values are included in the output.</p>
     *
     * @return a comma-separated {@link String} listing all active faction standing effects; returns
     * an empty string if there are no effects.
     */
    public String getEffectsDescription() {
        List<String> effects = new ArrayList<>();

        // If we're fetching for STANDING_LEVEL_4, then we're guaranteed not to pass any of the conditionals, so exit.
        if (this == STANDING_LEVEL_4) {
            return "";
        }

        if (negotiationModifier != STANDING_LEVEL_4.getNegotiationModifier()) {
            effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandingLevel.negotiation",
                  getPolarityOfModifier(negotiationModifier)));
        }

        int resupplyPercentage = (int) resupplyWeightModifier * 100;
        effects.add(getFormattedTextAt(RESOURCE_BUNDLE, "factionStandingLevel.resupply", resupplyPercentage));

        if (hasCommandCircuitAccess) {
            effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandingLevel.commandCircuit",
                  spanOpeningWithCustomColor(getPositiveColor()),
                  CLOSING_SPAN_TAG));
        }

        if (isOutlawed) {
            effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandingLevel.outlawed",
                  spanOpeningWithCustomColor(getNegativeColor()),
                  CLOSING_SPAN_TAG));
        }

        if (!isBatchallAllowed) {
            effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandingLevel.batchall",
                  spanOpeningWithCustomColor(getWarningColor()),
                  CLOSING_SPAN_TAG));
        }

        effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
              "factionStandingLevel.recruitment.popularity",
              getPolarityOfModifier(recruitmentTickets - STANDING_LEVEL_4.getRecruitmentTickets())));

        if (recruitmentRollsModifier != STANDING_LEVEL_4.getRecruitmentRollsModifier()) {
            effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandingLevel.recruitment.rolls",
                  getPolarityOfModifier(recruitmentRollsModifier)));
        }

        int barracksCostPercentage = (int) barrackCostsMultiplier * 100;
        effects.add(getFormattedTextAt(RESOURCE_BUNDLE, "factionStandingLevel.barracks", barracksCostPercentage));

        if (unitMarketRarityModifier != STANDING_LEVEL_4.getUnitMarketRarityModifier()) {
            effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandingLevel.unitMarket",
                  getPolarityOfModifier(unitMarketRarityModifier)));
        }

        int payPercentage = (int) contractPayMultiplier * 100;
        effects.add(getFormattedTextAt(RESOURCE_BUNDLE, "factionStandingLevel.contractPay", payPercentage));

        if (supportPointModifierContractStart != STANDING_LEVEL_4.getSupportPointModifierContractStart()) {
            effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandingLevel.supportPoints.signing",
                  getPolarityOfModifier(supportPointModifierContractStart)));
        }

        if (supportPointModifierPeriodic != STANDING_LEVEL_4.getSupportPointModifierPeriodic()) {
            effects.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandingLevel.supportPoints.periodic",
                  getPolarityOfModifier(supportPointModifierContractStart)));
        }

        return String.join(", ", effects);
    }

    private static String getPolarityOfModifier(int modifier) {
        if (modifier >= 0) {
            return "+" + modifier;
        }

        return modifier + "";
    }

    /**
     * Determines the appropriate fallback suffix for the given {@link Faction} based on its characteristics.
     *
     * @param relevantFaction the {@link Faction} whose suffix label is being determined. It will provide information
     *                        about whether the faction is a clan, periphery, or of the Inner Sphere.
     *
     * @return the suffix label as a {@link String} representing the type of the faction. Possible values include a clan
     *       suffix, periphery suffix, or Inner Sphere suffix.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getFallbackSuffix(Faction relevantFaction) {
        if (relevantFaction.isClan()) {
            return FALLBACK_LABEL_SUFFIX_CLAN;
        } else if (relevantFaction.isPeriphery()) {
            return FALLBACK_LABEL_SUFFIX_PERIPHERY;
        }

        return FALLBACK_LABEL_SUFFIX_INNER_SPHERE;
    }

    /**
     * Returns the {@link FactionStandingLevel} that matches the given text input.
     *
     * <p>This method attempts to resolve a {@code FactionStanding} by interpreting the provided string in two
     * ways:</p>
     *
     * <ol>
     *   <li>Treats the input as the name of a standing level, ignoring case and replacing spaces with underscores to
     *   match enum constant formatting.</li>
     *   <li>If that fails, attempts to parse the input as an integer and use the corresponding ordinal index of the
     *   enum values.</li>
     * </ol>
     *
     * <p>If neither approach succeeds, the method logs an error and returns the default {@code STANDING_LEVEL_4}.</p>
     *
     * @param text the string input to resolve into a {@code FactionStanding}; may be a name or ordinal value
     *
     * @return the matching {@code FactionStanding}, or {@code STANDING_LEVEL_4} if no match is found
     */
    public static FactionStandingLevel fromString(String text) {
        try {
            // Attempt to parse as a string with case/space adjustments.
            return FactionStandingLevel.valueOf(text.toUpperCase().replace(" ", "_"));
        } catch (Exception ignored) {
        }

        try {
            // Attempt to parse as an integer and use as ordinal.
            return FactionStandingLevel.values()[MathUtility.parseInt(text, STANDING_LEVEL_4.standingLevel)];
        } catch (Exception ignored) {
        }

        // Log error if parsing fails and return default value.
        LOGGER.error("Unknown FactionStandingLevel: {} - returning {}.", text, STANDING_LEVEL_4);

        return STANDING_LEVEL_4;
    }

    @Override
    public String toString() {
        return name();
    }
}
