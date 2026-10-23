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
package mekhq.gui.dialog.markets.contractMarket;

import static mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationDifficulty.MAXIMUM_DIFFICULTY;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.DANGER;
import static mekhq.gui.baseComponents.hud.HudStyle.READY;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules;
import mekhq.campaign.mission.contract.contractData.ObfuscatableIntel;
import mekhq.campaign.universe.Faction;

/**
 * Shared, presentation-only formatting helpers for the Chaos contract market UI.
 *
 * <p>These turn raw contract data into the short, player-facing strings and colours the board, the dossier and the
 * comparison render. Kept out of the data classes deliberately, so {@link AbstractContract} and its records stay
 * logic-free.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractMarketFormatting {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ChaosContractMarketDialog";

    private static final int DEFAULT_DIFFICULTY = 5;
    private static final double MILLION = 1_000_000.0;
    private static final double THOUSAND = 1_000.0;

    private ContractMarketFormatting() {
    }

    /**
     * One badge marking something unusual about an offer, such as a batchall or a special rule.
     *
     * @param text    the badge's text
     * @param toolTip what it means
     * @param color   its HUD colour
     */
    public record MarketBadge(String text, String toolTip, Color color) {}

    /**
     * The badges an offer carries: a batchall when the visible opposition is a Clan, the contract's special type
     * (Proving Ground or Covert Operation, which are mutually exclusive), and each special rule of its objective.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<MarketBadge> badges(AbstractContract contract) {
        List<MarketBadge> badges = new ArrayList<>();
        // A batchall badge would reveal the enemy is a Clan, so it is left off while the opposition is hidden.
        Faction enemyFaction = contract.getEnemyFaction();
        if (!contract.isIntelObfuscated(ObfuscatableIntel.OPPOSITION) && (enemyFaction != null)
                  && enemyFaction.isClan()) {
            badges.add(new MarketBadge(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.batchall"),
                  getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.batchall.tooltip"), AMBER));
        }
        if (contract.isProvingGround()) {
            badges.add(new MarketBadge(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.provingGround"),
                  getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.provingGround.tooltip"), READY));
        }
        if (contract.isCovert()) {
            badges.add(new MarketBadge(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.covert"),
                  getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.covert.tooltip"), ACCENT_BRIGHT));
        }
        for (ChaosObjectiveSpecialRules specialRule : contract.getSpecialRules()) {
            badges.add(new MarketBadge(specialRule.getName(), specialRule.getToolTipText(), AMBER));
        }
        return badges;
    }

    /**
     * The clamped 1-10 difficulty used for the pip meter, the ring and the assessment word, so they always agree. An
     * unknown or uncomputed difficulty reads as an even fight (5).
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int assessmentDifficulty(AbstractContract contract) {
        int difficulty = contract.getCachedContractDifficulty();
        // Generation clamps its own result, but a value from elsewhere (such as a converted legacy contract) need not
        // be, and every difficulty above the top would otherwise have no assessment word.
        return difficulty < 1 ? DEFAULT_DIFFICULTY : Math.min(difficulty, MAXIMUM_DIFFICULTY);
    }

    /**
     * A plain-language read on the fight, keyed by the same 1-10 difficulty the pips show. Difficulty 5 means the
     * player and enemy forces are evenly matched; lower is favorable to the player, higher is dangerous.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String assessmentLabel(AbstractContract contract) {
        return getTextAt(RESOURCE_BUNDLE, "value.contractMarket.assessment." + assessmentDifficulty(contract));
    }

    /**
     * The HUD status colour for a difficulty: green while the fight favours the player, cyan near an even match, amber
     * when it is hard, and red when it is very hard or worse.
     *
     * @param difficulty a 1-10 difficulty, as {@link #assessmentDifficulty} returns
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Color severityColor(int difficulty) {
        if (difficulty <= 3) {
            return READY;
        }
        if (difficulty <= 5) {
            return ACCENT;
        }
        return difficulty <= 7 ? AMBER : DANGER;
    }

    /**
     * The name the market shows for an offer. A name generated from the opposition would give away hidden intel, so
     * it is replaced by the "No Intel" placeholder unless it is an operation codename.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String marketName(AbstractContract contract) {
        boolean nameRevealsOpposition = contract.isIntelObfuscated(ObfuscatableIntel.OPPOSITION)
                                              && !contract.isNameOperationCodename();
        return nameRevealsOpposition
                     ? getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.intel.obfuscated")
                     : contract.getName().replace('_', ' ');
    }

    /**
     * The offer's mission type, or a placeholder when the campaign hides contract types behind operation codenames.
     *
     * @param hideContractType {@code true} if the campaign hides contract types
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String objective(AbstractContract contract, boolean hideContractType) {
        return hideContractType
                     ? getTextAt(RESOURCE_BUNDLE, "value.contractMarket.objective.hidden")
                     : contract.getObjectiveType().toString();
    }

    /**
     * The destination: the target planet's name, or the system's when no planet is set.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String destination(AbstractContract contract, LocalDate currentDate) {
        String planet = contract.getTargetPlanetName(currentDate);
        return planet == null ? contract.getTargetSystemName(currentDate) : planet;
    }

    /**
     * The intel value to display: the real {@code value}, or a "No Intel" placeholder when the contract hides that
     * field from the player in the market.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String intel(AbstractContract contract, ObfuscatableIntel field, String value) {
        return contract.isIntelObfuscated(field)
                     ? getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.intel.obfuscated")
                     : value;
    }

    /**
     * Formats the opposing force as skill plus equipment letter, e.g. "Veteran / B-rated".
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String threat(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.threat",
              contract.getEnemyForceSkill().toString(),
              ratingLetter(contract.getEnemyEquipmentRating()));
    }

    /**
     * Formats the employer's own force as skill plus equipment letter, in the same style as {@link #threat}, e.g.
     * "Regular / C-rated".
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String alliedCommand(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.threat",
              contract.getEmployerForceSkill().toString(),
              ratingLetter(contract.getEmployerEquipmentRating()));
    }

    /**
     * Formats salvage terms as either "Exchange only" or a rounded percentage.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String salvage(AbstractContract contract) {
        if (contract.isSalvageExchange()) {
            return getTextAt(RESOURCE_BUNDLE, "value.contractMarket.salvage.exchange");
        }
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.salvage.percentage",
              asPercent(contract.getSalvageRightsMultiplier()));
    }

    public static String support(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.support",
              asPercent(contract.getSupportMultiplier()));
    }

    public static String transport(AbstractContract contract) {
        return getFormattedTextAt(RESOURCE_BUNDLE,
              "value.contractMarket.transport",
              asPercent(contract.getTransportMultiplier()));
    }

    public static int asPercent(double multiplier) {
        return (int) Math.round(multiplier * 100);
    }

    /**
     * A short money figure for tiles and tight columns, such as "4.85M" or "712K". Negative amounts keep their sign.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String compactMoney(Money money) {
        double amount = money.getAmount().doubleValue();
        String sign = amount < 0 ? getTextAt(RESOURCE_BUNDLE, "value.contractMarket.compact.minus") : "";
        double magnitude = Math.abs(amount);

        NumberFormat format = NumberFormat.getNumberInstance(MekHQ.getMHQOptions().getNumberFormatLocale());
        // The unit is chosen after rounding, so an amount just short of a million reads "1.00M" rather than "1,000K",
        // and one just short of a thousand reads "1K" rather than "1,000".
        if (Math.round(magnitude / THOUSAND) >= THOUSAND) {
            format.setMinimumFractionDigits(2);
            format.setMaximumFractionDigits(2);
            return sign + getFormattedTextAt(RESOURCE_BUNDLE, "value.contractMarket.compact.millions",
                  format.format(magnitude / MILLION));
        }
        format.setMaximumFractionDigits(0);
        if (Math.round(magnitude) >= THOUSAND) {
            return sign + getFormattedTextAt(RESOURCE_BUNDLE, "value.contractMarket.compact.thousands",
                  format.format(Math.round(magnitude / THOUSAND)));
        }
        return sign + format.format(Math.round(magnitude));
    }

    /**
     * {@link #compactMoney} with a leading plus sign on positive amounts, for figures that can go either way.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String signedCompactMoney(Money money) {
        String compact = compactMoney(money);
        return money.getAmount().compareTo(BigDecimal.ZERO) > 0
                     ? getTextAt(RESOURCE_BUNDLE, "value.contractMarket.compact.plus") + compact
                     : compact;
    }

    /**
     * A full money amount with its sign, in the same glyphs as {@link #signedCompactMoney}: a minus for a loss, a plus
     * for a gain, and no sign at all for zero.
     */
    public static String signedAmount(Money money) {
        int sign = money.getAmount().signum();
        String prefix = sign > 0 ? getTextAt(RESOURCE_BUNDLE, "value.contractMarket.compact.plus")
                              : sign < 0 ? getTextAt(RESOURCE_BUNDLE, "value.contractMarket.compact.minus") : "";
        return prefix + money.absolute().toAmountString();
    }

    /**
     * A full money amount shown as a cost: with the minus glyph, unless it is zero.
     */
    public static String costAmount(Money cost) {
        return signedAmount(cost.multipliedBy(-1));
    }

    /**
     * The one-way trip to an offer's target, such as "12 days", or "No route" when no jump path reaches it.
     */
    public static String transit(ContractProfitEstimate estimate) {
        return estimate.hasRoute()
                     ? getFormattedTextAt(RESOURCE_BUNDLE, "value.contractMarket.days", estimate.transitDays())
                     : getTextAt(RESOURCE_BUNDLE, "value.contractMarket.noRoute");
    }

    /**
     * A decimal format for the given pattern in the player's chosen locale, as {@link #compactMoney} formats, rather
     * than the system default.
     */
    public static DecimalFormat decimalFormat(String pattern) {
        return new DecimalFormat(pattern,
              DecimalFormatSymbols.getInstance(MekHQ.getMHQOptions().getNumberFormatLocale()));
    }

    private static String ratingLetter(int equipmentRating) {
        String name = DragoonRating.fromRating(equipmentRating).name().replace("DRAGOON_", "");
        return name.equals("ASTAR") ? "A*" : name;
    }
}
