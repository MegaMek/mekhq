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
package mekhq.campaign.mission.contract.contractSpecialRules;

import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.DOUBLE_SUPPORT_PAYOUTS;
import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.NO_IN_CONTRACT_SUPPORT;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules;

/**
 * Renders the ongoing support an employer owes the player during a contract: the straight-support share of repair and
 * maintenance costs, and battle loss compensation for units lost beyond repair.
 *
 * <p>Both are paid as they are earned for an ordinary contract. Under the {@link ChaosObjectiveSpecialRules
 * #NO_IN_CONTRACT_SUPPORT} special rule they are instead withheld onto the contract (see
 * {@link AbstractContract#changeWithheldSupportPayments(Money)}) and settled as a single lump sum when the contract ends
 * through {@link #renderWithheldSupport(Campaign, AbstractContract)}.</p>
 *
 * <p>Straight support is a per-contract term but repairs and maintenance are campaign-wide costs, so the share is drawn
 * from the active contract offering the most generous support (see {@link #getStraightSupportContract(Campaign)}).
 * Battle loss compensation, by contrast, is settled against the specific contract whose scenario cost the unit.</p>
 *
 * <p>Both payouts are doubled for a contract carrying the {@link ChaosObjectiveSpecialRules#DOUBLE_SUPPORT_PAYOUTS}
 * special rule. The doubling is applied before the withhold check, so a contract that also carries
 * {@link ChaosObjectiveSpecialRules#NO_IN_CONTRACT_SUPPORT} withholds - and later pays out - the already-doubled
 * amount.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractSupportPayments {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractSpecialRules";

    /** The factor applied to every support payout of a contract carrying {@code DOUBLE_SUPPORT_PAYOUTS}. */
    private static final double DOUBLE_SUPPORT_PAYOUT_MULTIPLIER = 2.0;

    private ContractSupportPayments() {
    }

    /**
     * Reimburses the straight-support share of a campaign-wide cost the player has just paid, such as a repair or a
     * maintenance charge.
     *
     * <p>The share is {@code cost * } the support multiplier of the active contract offering the most generous straight
     * support, doubled when that contract carries {@link ChaosObjectiveSpecialRules#DOUBLE_SUPPORT_PAYOUTS}. When the
     * contract carries the {@link ChaosObjectiveSpecialRules#NO_IN_CONTRACT_SUPPORT} special rule the share is withheld
     * until the contract ends; otherwise it is credited immediately. Does nothing when the cost is not positive, no
     * contract is active, or the support multiplier is zero.</p>
     *
     * @param campaign        the active campaign
     * @param cost            the cost the player paid, whose support share is reimbursed
     * @param costDescription a short description of the cost, used in the ledger note (e.g. the repaired part or unit)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void reimburseStraightSupport(Campaign campaign, Money cost, String costDescription) {
        if (!cost.isPositive()) {
            return;
        }

        AbstractContract contract = getStraightSupportContract(campaign);
        if (contract == null) {
            return;
        }

        double supportMultiplier = contract.getSupportMultiplier();
        if (supportMultiplier <= 0) {
            return;
        }

        Money share = applyDoubleSupportPayouts(contract, cost.multipliedBy(supportMultiplier));
        if (!share.isPositive()) {
            return;
        }

        String note = getFormattedTextAt(RESOURCE_BUNDLE, "ContractSupport.straightSupport.note", costDescription);
        // Straight support fires on every repair and maintenance charge, so it is credited silently to the ledger
        // rather than adding a daily report line for each one.
        creditOrWithhold(campaign, contract, share, note, null);
    }

    /**
     * Settles battle loss compensation for a unit lost beyond repair, either crediting it immediately or, under the
     * {@link ChaosObjectiveSpecialRules#NO_IN_CONTRACT_SUPPORT} special rule, withholding it until the contract ends.
     *
     * <p>The amount is doubled when the contract carries {@link ChaosObjectiveSpecialRules#DOUBLE_SUPPORT_PAYOUTS}. Does
     * nothing when the resulting amount is not positive.</p>
     *
     * @param campaign the active campaign
     * @param contract the contract whose scenario cost the unit
     * @param amount   the battle loss compensation owed, before the {@code DOUBLE_SUPPORT_PAYOUTS} rule is applied
     * @param unitName the name of the lost unit, used in the ledger note and report
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void payBattlefieldLoss(Campaign campaign, AbstractContract contract, Money amount, String unitName) {
        Money payout = applyDoubleSupportPayouts(contract, amount);
        if (!payout.isPositive()) {
            return;
        }

        String note = getFormattedTextAt(RESOURCE_BUNDLE, "ContractSupport.battlefieldLoss.note", unitName);
        String report = getFormattedTextAt(RESOURCE_BUNDLE,
              "ContractSupport.battlefieldLoss.report",
              payout.toAmountAndSymbolString(),
              unitName);
        creditOrWithhold(campaign, contract, payout, note, report);
    }

    /**
     * Pays out, in a single lump sum, all support withheld on the contract under the
     * {@link ChaosObjectiveSpecialRules#NO_IN_CONTRACT_SUPPORT} special rule, then clears the withheld total.
     *
     * <p>Does nothing when nothing has been withheld, so it is safe to call for every completed contract.</p>
     *
     * @param campaign the active campaign
     * @param contract the contract being completed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void renderWithheldSupport(Campaign campaign, AbstractContract contract) {
        Money withheld = contract.getWithheldSupportPayments();
        if (!withheld.isPositive()) {
            return;
        }

        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.CONTRACT_PAYMENT,
                    campaign.getLocalDate(),
                    withheld,
                    getFormattedTextAt(RESOURCE_BUNDLE, "ContractSupport.withheld.note", contract.getName()));
        campaign.addReport(FINANCES,
              getFormattedTextAt(RESOURCE_BUNDLE,
                    "ContractSupport.withheld.report",
                    withheld.toAmountAndSymbolString(),
                    contract.getHyperlinkedName()));

        contract.setWithheldSupportPayments(Money.zero());
    }

    /**
     * @param campaign the active campaign
     *
     * @return the active contract offering the highest straight-support multiplier, or {@code null} when no contract is
     *       active
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable AbstractContract getStraightSupportContract(Campaign campaign) {
        AbstractContract bestContract = null;
        double bestMultiplier = 0;
        for (AbstractContract contract : campaign.getActiveContracts()) {
            double multiplier = contract.getSupportMultiplier();
            if (bestContract == null || multiplier > bestMultiplier) {
                bestContract = contract;
                bestMultiplier = multiplier;
            }
        }
        return bestContract;
    }

    /**
     * Applies the {@link ChaosObjectiveSpecialRules#DOUBLE_SUPPORT_PAYOUTS} special rule to a support payout: the amount
     * is doubled when the contract carries the rule and returned unchanged otherwise.
     *
     * @param contract the contract the payout is settled against
     * @param amount   the support payout before the rule is applied
     *
     * @return the payout after the special rule, doubled when the rule is present
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static Money applyDoubleSupportPayouts(AbstractContract contract, Money amount) {
        if (contract.usesSpecialRule(DOUBLE_SUPPORT_PAYOUTS)) {
            return amount.multipliedBy(DOUBLE_SUPPORT_PAYOUT_MULTIPLIER);
        }
        return amount;
    }

    /**
     * Credits the given support amount to the player now, or withholds it onto the contract when the contract carries
     * the {@link ChaosObjectiveSpecialRules#NO_IN_CONTRACT_SUPPORT} special rule.
     *
     * @param campaign      the active campaign
     * @param contract      the contract the support is settled against
     * @param amount        the positive amount to credit or withhold
     * @param ledgerNote    the note recorded against the ledger transaction when credited immediately
     * @param reportMessage the daily report line to add when credited immediately, or {@code null} to credit silently
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void creditOrWithhold(Campaign campaign, AbstractContract contract, Money amount, String ledgerNote,
          @Nullable String reportMessage) {
        if (contract.usesSpecialRule(NO_IN_CONTRACT_SUPPORT)) {
            // Withheld silently; the whole total is reported when it is paid out on completion.
            contract.changeWithheldSupportPayments(amount);
            return;
        }

        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.CONTRACT_PAYMENT, campaign.getLocalDate(), amount, ledgerNote);
        if (reportMessage != null) {
            campaign.addReport(FINANCES, reportMessage);
        }
    }
}
