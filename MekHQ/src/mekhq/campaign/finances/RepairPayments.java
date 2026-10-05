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
package mekhq.campaign.finances;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.contractSpecialRules.ContractSupportPayments;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.work.IPartWork;

/**
 * Payment for repairs under the Pay for Repairs option. A paid repair is checked against the balance before it is
 * attempted, and charged once it succeeds, so a force with no money cannot have its units fixed for free.
 */
public final class RepairPayments {
    private static final MMLogger LOGGER = MMLogger.create(RepairPayments.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    private RepairPayments() {}

    /**
     * @param campaign the campaign
     * @param partWork the task
     * @param isFix    {@code true} if the task fixes a damaged part, rather than replacing, salvaging, reloading,
     *                 re-attaching or sealing it
     *
     * @return {@code true} if the task is paid for: Pay for Repairs is on, and it is a fix of anything but armor. A
     *       refit is never paid for here; it has its own costs.
     */
    public static boolean isPaidFor(Campaign campaign, IPartWork partWork, boolean isFix) {
        boolean isPayForRepairsOn = campaign.getCampaignOptions().get(CampaignOption.PAY_FOR_REPAIRS);
        boolean isArmor = partWork instanceof Armor;
        boolean isRefit = partWork instanceof Refit;
        return isPayForRepairsOn && isFix && !isArmor && !isRefit;
    }

    /**
     * @return what fixing the part costs, with the planetary and Clan multipliers applied
     */
    public static Money getCost(Campaign campaign, IPartWork partWork) {
        return partWork.getRepairCost().multipliedBy(RepairCosts.getRepairCostMultiplier(campaign,
              partWork.getUnit()));
    }

    /**
     * Not logged here: the repair screen asks this for every part it lists. A task held for this reason is logged
     * when it is held.
     *
     * @return why the task cannot be paid for, or {@code null} if the balance covers it
     */
    public static @Nullable String getUnaffordableReason(Campaign campaign, IPartWork partWork) {
        Money cost = getCost(campaign, partWork);
        if (campaign.getPlayerForce().getFinances().getBalance().isLessThan(cost)) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "repair.unaffordable", cost.toAmountAndSymbolString());
        }
        return null;
    }

    /**
     * Pays for a successful repair. The cost is reported, and an employer covering straight support reimburses its
     * share, only when the payment goes through.
     *
     * @param campaign the campaign
     * @param cost     the cost, worked out before the repair succeeded
     * @param partName the repaired part's name, for the ledger
     *
     * @return the report line for the payment, empty if it was refused
     */
    public static String pay(Campaign campaign, Money cost, String partName) {
        boolean isPaid = campaign.getPlayerForce().getFinances().debit(TransactionType.REPAIRS,
              campaign.getLocalDate(), cost, getFormattedTextAt(RESOURCE_BUNDLE, "repair.cost.finances", partName));
        if (!isPaid) {
            LOGGER.error("[PayForRepairs] Payment of {} for {} was refused after the repair was allowed",
                  cost.toAmountAndSymbolString(), partName);
            return "";
        }
        ContractSupportPayments.reimburseStraightSupport(campaign, cost, partName);
        return getFormattedTextAt(RESOURCE_BUNDLE, "repair.cost.report", cost.toAmountAndSymbolString());
    }
}
