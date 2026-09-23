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

import static mekhq.campaign.mission.contract.contractData.ChaosObjectiveSpecialRules.END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;

/**
 * Implements the {@code END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS} contract special rule: the contract ends early once
 * the player secures two qualifying tracks in a row, or fails two in a row.
 *
 * <p>Each qualifying scenario resolution nudges a running tally on the {@link AbstractContract}: a player victory adds
 * {@code +1} and any other result subtracts {@code 1}. When the tally reaches {@code +2} the contract is drawn to a
 * close on the player's success and any outstanding pay is settled; when it reaches {@code -2} the contract ends on the
 * player's failure and no further pay is rendered. The ending itself mirrors how a morale rout finishes a contract
 * early, staging an end date and routed payout the completion flow then honours.</p>
 *
 * <p>When the contract is tracked by StratCon only Essential scenarios - those tied to a strategic objective - count
 * towards the tally. When the contract has no StratCon campaign state every scenario counts.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class TwoConsecutiveTracks {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractSpecialRules";

    private static final int PLAYER_SUCCESS_THRESHOLD = 2;
    private static final int PLAYER_FAILURE_THRESHOLD = -2;

    /**
     * Processes a resolved scenario for the {@code END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS} special rule, updating the
     * contract's running tally and ending the contract early once two consecutive qualifying tracks have been won or
     * lost.
     *
     * <p>Does nothing when the contract is {@code null}, does not carry the special rule, or the scenario does not
     * qualify. Must be called before the scenario is removed from its StratCon track, so the Essential lookup can still
     * resolve.</p>
     *
     * @param campaign the active campaign
     * @param contract the contract the scenario belongs to, or {@code null}
     * @param scenario the scenario that was just resolved
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void processScenarioResolution(Campaign campaign, @Nullable AbstractContract contract,
          AtBScenario scenario) {
        if (contract == null) {
            return;
        }

        if (!contract.usesSpecialRule(END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS)) {
            return;
        }

        if (!isQualifyingScenario(campaign, contract, scenario)) {
            return;
        }

        boolean playerWasSuccessful = scenario.getStatus().isOverallVictory();
        contract.changeConsecutiveTrackResultTally(playerWasSuccessful ? 1 : -1);

        if (hasReachedPlayerSuccessThreshold(contract)) {
            endContract(campaign, contract, true);
        } else if (hasReachedPlayerFailureThreshold(contract)) {
            endContract(campaign, contract, false);
        }
    }

    /**
     * Reports whether the contract carries the {@code END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS} special rule and its
     * running tally has reached the player-success threshold, meaning the contract ends (or has ended) on the player's
     * success. The threshold scales with the contract's scale.
     *
     * @param contract the contract to test
     *
     * @return {@code true} if the contract ends on the player's success under this special rule
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean hasReachedPlayerSuccessThreshold(AbstractContract contract) {
        return contract.usesSpecialRule(END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS)
                     && (contract.getConsecutiveTrackResultTally() >= playerSuccessThreshold(contract));
    }

    /**
     * Reports whether the contract carries the {@code END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS} special rule and its
     * running tally has reached the player-failure threshold, meaning the contract ends (or has ended) on the player's
     * failure. The threshold scales with the contract's scale.
     *
     * @param contract the contract to test
     *
     * @return {@code true} if the contract ends on the player's failure under this special rule
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean hasReachedPlayerFailureThreshold(AbstractContract contract) {
        return contract.usesSpecialRule(END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS)
                     && (contract.getConsecutiveTrackResultTally() <= playerFailureThreshold(contract));
    }

    private static int playerSuccessThreshold(AbstractContract contract) {
        return PLAYER_SUCCESS_THRESHOLD * contract.getScale();
    }

    private static int playerFailureThreshold(AbstractContract contract) {
        return PLAYER_FAILURE_THRESHOLD * contract.getScale();
    }

    /**
     * Determines whether a resolved scenario counts towards the consecutive-track tally.
     *
     * <p>Without a StratCon campaign state there are no Essential scenarios to single out, so every scenario counts.
     * With StratCon only Essential scenarios - those tied to a strategic objective - count.</p>
     *
     * @param campaign the active campaign
     * @param contract the contract the scenario belongs to
     * @param scenario the scenario that was just resolved
     *
     * @return {@code true} if the scenario counts towards the tally
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isQualifyingScenario(Campaign campaign, AbstractContract contract, AtBScenario scenario) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState == null) {
            return true;
        }

        StratConScenario stratConScenario = StratConCampaignState.getStratConScenarioFromAtBScenario(campaign, scenario);
        return (stratConScenario != null) && stratConScenario.isStrategicObjective();
    }

    /**
     * Ends the contract early, mirroring how a morale rout finishes a contract early. A success ending settles the
     * remaining escrow; a failure ending renders no outstanding pay.
     *
     * @param campaign             the active campaign
     * @param contract             the contract to end
     * @param renderOutstandingPay whether the ending settles the contract's remaining pay (player success) or renders
     *                             none (player failure)
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void endContract(Campaign campaign, AbstractContract contract, boolean renderOutstandingPay) {
        LocalDate today = campaign.getLocalDate();
        LocalDate endDate = today.plusDays(1);

        String messageKey = renderOutstandingPay
                                  ? "TwoConsecutiveTracks.end.success"
                                  : "TwoConsecutiveTracks.end.failure";
        String message = getFormattedTextAt(RESOURCE_BUNDLE, messageKey, contract.getHyperlinkedName());
        new ImmersiveDialogNotification(campaign, message, true);

        if (renderOutstandingPay) {
            // Settle the remaining escrow through the routed payout, exactly as a morale rout that finishes a contract
            // early does.
            long remainingMonths = contract.getMonthsLeft(endDate);
            Money finalPayout = contract.getMonthlyPayOut().multipliedBy(remainingMonths);
            contract.changeMorale(endDate, finalPayout);
        } else {
            // A zero routed payout overrides the escrow settlement when the contract is completed, so a failure ending
            // renders no outstanding pay.
            contract.changeMorale(endDate);
        }
    }
}
