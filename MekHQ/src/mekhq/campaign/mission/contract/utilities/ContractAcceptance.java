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

import static mekhq.MHQConstants.CONFIRMATION_ACCEPT_CONTRACT;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;

/**
 * Commits a {@link AbstractContract} offer from the market into an active campaign mission.
 *
 * <p>Acceptance runs as an ordered pipeline: confirm with the player, take the offer off the market and register it as
 * a mission, seed StratCon (unless opted out), pay the employer's transport reimbursement, then start the contract
 * (mothballing and transit) and announce it. The player is not shown transit/mothball prompts here - those choices were
 * already captured by the market dialog's checkboxes and are passed in.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractAcceptance {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractAutomation";

    private ContractAcceptance() {}

    /**
     * Accepts a market offer, committing it to the campaign as an active mission.
     *
     * @param campaign    the current campaign
     * @param contract    the offer being accepted
     * @param bucket      the market bucket the offer is drawn from (so it can be removed from that pool)
     * @param useStratCon whether the player wants StratCon for this contract (ignored when the campaign is not using
     *                    StratCon); when {@code false} the contract keeps a {@code null} StratCon campaign state
     * @param mothball    whether to GM-mothball eligible units on departure
     * @param travel      whether to plot and charge the jump to the target system now
     *
     * @return {@code true} if the contract was accepted; {@code false} if the player cancelled at the confirmation
     */
    public static boolean accept(Campaign campaign, AbstractContract contract, ContractSearchType bucket,
          boolean useStratCon, boolean mothball, boolean travel) {
        // Final confirmation nag, honoring the player's "don't ask again" preference.
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_ACCEPT_CONTRACT)) {
            ImmersiveDialogConfirmation confirmation = new ImmersiveDialogConfirmation(campaign,
                  CONFIRMATION_ACCEPT_CONTRACT);
            if (!confirmation.wasConfirmed()) {
                return false;
            }
        }

        // Take the offer off the market and register it as a mission. addMission must come before StratCon setup so the
        // contract has its mission id when the tracks are seeded.
        campaign.getPlayerForce().getContractMarket().removeContract(bucket, contract);
        // A market offer carries no status; accepting it is what makes it a running mission. Set this before
        // addMission, since every contractHistory filter keys off the status.
        contract.setStatus(MissionStatus.ACTIVE);
        campaign.addMission(contract);

        // Seed StratCon, unless the campaign is not using it or the player opted this contract out. When opted out the
        // StratCon campaign state is left null.
        if (useStratCon && campaign.getCampaignOptions().isUseStratCon()) {
            StratConContractDefinition definition = StratConContractDefinition.getContractDefinition(
                  contract.getObjectiveType());
            if (definition != null) {
                StratConContractInitializer.initializeCampaignState(contract, campaign, definition);
            }
        } else {
            contract.setStratConCampaignState(null);
        }

        // Pay the employer's transport reimbursement immediately, before any transit cost is charged to the player.
        Money transportReimbursement = contract.getTransportPayment();
        if ((transportReimbursement != null) && transportReimbursement.isPositive()) {
            campaign.getPlayerForce().getFinances().credit(TransactionType.CONTRACT_PAYMENT, campaign.getLocalDate(),
                  transportReimbursement, getFormattedTextAt(RESOURCE_BUNDLE, "acceptContract.transport.report",
                        contract.getName()));
            campaign.addReport(DailyReportType.GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
                  "acceptContract.transport.report", contract.getName()));
        }

        // Start the contract: dates it to the projected arrival day and (per the checkboxes) mothballs and/or jumps.
        ContractAutomation.performContractStart(campaign, contract, mothball, travel);

        // Announce the fully-initialized contract so listeners (e.g. the StratCon tab) pick it up.
        MekHQ.triggerEvent(new MissionChangedEvent(contract));

        return true;
    }
}
