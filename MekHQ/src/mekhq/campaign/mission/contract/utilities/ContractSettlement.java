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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.AbstractContract;

/**
 * End-of-contract settlement steps that run when a contract is completed: repaying the employer for any salvage the
 * player kept beyond their salvage rights, and paying shareholders their cut of the final payout. Kept out of
 * {@link Campaign} so that class does not grow further.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractSettlement {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractSettlement";

    private ContractSettlement() {}

    /**
     * The value of salvage the player kept beyond their salvage rights, which the employer is owed at contract end.
     * Under normal (non-exchange) salvage rights the player physically keeps the salvage they recover during the
     * contract; if the value they kept exceeds the fraction their salvage rights allow of the total salvage recovered,
     * the excess is owed. Salvage-exchange contracts are already paid out as cash during the contract, so there is
     * nothing owed. Whether this is actually repaid is the caller's decision (the
     * {@code OverageRepaymentInFinalPayment} option), which folds it into the final contract payment.
     *
     * @param contract the contract being completed
     *
     * @return the salvage overage owed, or {@link Money#zero()} when none is owed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money salvageOverage(AbstractContract contract) {
        if (contract.isSalvageExchange()) {
            return Money.zero();
        }

        Money playerSalvage = contract.getSalvagedByUnitValue();
        Money totalSalvage = playerSalvage.plus(contract.getSalvagedByEmployerValue());
        if (!totalSalvage.isPositive()) {
            return Money.zero();
        }

        // The salvage-rights multiplier (0-1) caps the fraction of the total salvage value the player may keep.
        Money allowedSalvage = totalSalvage.multipliedBy(contract.getSalvageRightsMultiplier());
        return playerSalvage.isGreaterThan(allowedSalvage) ? playerSalvage.minus(allowedSalvage) : Money.zero();
    }

    /**
     * Pays shareholding personnel their cut of the end-of-contract lump payout, mirroring the monthly share payout.
     * Runs only when the share system is enabled and there is a positive payout to draw the share from.
     *
     * @param campaign the paying campaign
     * @param contract the contract being completed (supplies the shares percentage)
     * @param payout   the final payout that was just credited to the campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void settleShares(Campaign campaign, AbstractContract contract, Money payout) {
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_SHARE_SYSTEM)) {
            return;
        }
        if ((payout == null) || !payout.isPositive()) {
            return;
        }

        Money shares = payout.multipliedBy(contract.getSharesPercent()).dividedBy(100);
        if (!shares.isPositive()) {
            return;
        }

        Finances finances = campaign.getPlayerForce().getFinances();
        boolean paid = finances.debit(TransactionType.SALARIES,
              campaign.getLocalDate(),
              shares,
              getFormattedTextAt(RESOURCE_BUNDLE, "ContractSettlement.shares.transaction", contract.getName()));
        if (paid) {
            campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE, "ContractSettlement.shares.report",
                  shares.toAmountAndSymbolString(), contract.getHyperlinkedName()));
            finances.payOutSharesToPersonnel(campaign, shares);
        } else {
            campaign.addReport(FINANCES, getFormattedTextAt(RESOURCE_BUNDLE, "ContractSettlement.shares.insufficient",
                  contract.getName()));
        }
    }
}
