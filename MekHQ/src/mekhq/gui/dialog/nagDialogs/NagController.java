/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.nagDialogs;

import static mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker.getHRStrainModifier;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * A controller class responsible for managing and triggering daily nag dialogs in the campaign.
 *
 * <p>
 * The purpose of this class is to sequentially check all predefined "nag" conditions to alert the player about issues
 * that require their attention before advancing the day in the campaign. Each nag dialog is displayed based on specific
 * conditions, and the daily nag process stops if the player cancels proceeding to the next day.
 * </p>
 *
 * <strong>Usage:</strong>
 * <p>
 * This class is primarily called during the "Advance Day" process in MekHQ, to notify players of campaign-related
 * issues spanning financial, personnel, and strategic concerns.
 * </p>
 */
public class NagController {
    /**
     * Triggers a sequence of daily nag dialogs to check and display issues in the campaign.
     *
     * <p>
     * This method iterates through all predefined nag dialogs, each associated with a specific condition or scenario
     * within the campaign. Nags include, but are not limited to, the following:
     * <ul>
     *     <li>Invalid faction settings.</li>
     *     <li>Missing commander.</li>
     *     <li>Untreated personnel requiring medical attention.</li>
     *     <li>Insufficient funds to cover daily expenses or upcoming costs.</li>
     *     <li>Unmaintained units in the hangar.</li>
     *     <li>Unresolved mission or contract scenarios.</li>
     * </ul>
     * If the player cancels any nag dialog, this method returns {@code true} and stops
     * further processing. If all nag dialogs are successfully passed, it returns {@code false},
     * allowing the campaign to progress to the next day.
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     *
     * @return {@code true} if the player cancels any nag dialog, {@code false} otherwise.
     */
    public static boolean triggerDailyNags(Campaign campaign) {
        // Invalid Faction
        final LocalDate today = campaign.getLocalDate();

        if (InvalidFactionNagDialog.checkNag(campaign.getFaction(), today)) {
            InvalidFactionNagDialog invalidFactionNagDialog = new InvalidFactionNagDialog(campaign);
            if (invalidFactionNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // No Commander
        if (NoCommanderNagDialog.checkNag(campaign.getCommander())) {
            NoCommanderNagDialog noCommanderNagDialog = new NoCommanderNagDialog(campaign);
            if (noCommanderNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        final List<Person> activePersonnel = campaign.getActivePersonnel(false);
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final int doctorCapacity = campaignOptions.getMaximumPatients();
        final boolean isDoctorsUseAdministration = campaignOptions.isDoctorsUseAdministration();

        // Untreated personnel
        if (UntreatedPersonnelNagDialog.checkNag(activePersonnel, doctorCapacity, isDoctorsUseAdministration)) {
            UntreatedPersonnelNagDialog untreatedPersonnelNagDialog = new UntreatedPersonnelNagDialog(campaign);
            if (untreatedPersonnelNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Unable to afford expenses
        if (UnableToAffordExpensesNagDialog.checkNag(campaign)) {
            UnableToAffordExpensesNagDialog unableToAffordExpensesNagDialog = new UnableToAffordExpensesNagDialog(
                  campaign);
            if (unableToAffordExpensesNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Unable to afford next jump
        if (UnableToAffordJumpNagDialog.checkNag(campaign)) {
            UnableToAffordJumpNagDialog unableToAffordJumpNagDialog = new UnableToAffordJumpNagDialog(campaign);
            if (unableToAffordJumpNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Unable to afford next loan payment
        final Finances finances = campaign.getFinances();

        if (UnableToAffordLoanPaymentNagDialog.checkNag(finances.getLoans(), today, finances.getBalance())) {
            UnableToAffordLoanPaymentNagDialog unableToAffordLoanPaymentNagDialog = new UnableToAffordLoanPaymentNagDialog(
                  campaign);
            if (unableToAffordLoanPaymentNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Unable to afford all items on shopping list
        final Money totalBuyCost = campaign.getShoppingList().getTotalBuyCost();
        final Money currentFunds = campaign.getFunds();
        if (UnableToAffordShoppingListNagDialog.checkNag(totalBuyCost, currentFunds)) {
            UnableToAffordShoppingListNagDialog unableToAffordShoppingListNagDialog = new UnableToAffordShoppingListNagDialog(
                  campaign);
            if (unableToAffordShoppingListNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Unmaintained Units
        final Collection<Unit> units = campaign.getUnits();
        final boolean isCheckMaintenance = campaignOptions.isCheckMaintenance();

        if (UnmaintainedUnitsNagDialog.checkNag(units, isCheckMaintenance)) {
            UnmaintainedUnitsNagDialog unmaintainedUnitsNagDialog = new UnmaintainedUnitsNagDialog(campaign);
            if (unmaintainedUnitsNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Insufficient Medics
        if (InsufficientMedicsNagDialog.checkNag(campaign.getMedicsNeed())) {
            InsufficientMedicsNagDialog insufficientMedicsNagDialog = new InsufficientMedicsNagDialog(campaign);
            if (insufficientMedicsNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Insufficient AsTechs
        if (InsufficientAsTechsNagDialog.checkNag(campaign.getAsTechNeed())) {
            InsufficientAsTechsNagDialog insufficientAstechsNagDialog = new InsufficientAsTechsNagDialog(campaign);
            if (insufficientAstechsNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Insufficient AsTech Time
        final int possibleAsTechPoolMinutes = campaign.getPossibleAsTechPoolMinutes();
        final boolean isOvertimeAllowed = campaign.isOvertimeAllowed();
        final int possibleAsTechPoolOvertime = campaign.getPossibleAsTechPoolOvertime();

        if (InsufficientAstechTimeNagDialog.checkNag(units,
              possibleAsTechPoolMinutes,
              isOvertimeAllowed,
              possibleAsTechPoolOvertime)) {
            InsufficientAstechTimeNagDialog insufficientAstechTimeNagDialog = new InsufficientAstechTimeNagDialog(
                  campaign);
            if (insufficientAstechTimeNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Unresolved StratCon AO Contacts
        final boolean isUseStratCon = campaignOptions.isUseStratCon();
        final List<AtBContract> activeContracts = campaign.getActiveAtBContracts();

        if (UnresolvedStratConContactsNagDialog.checkNag(isUseStratCon, activeContracts, today)) {
            UnresolvedStratConContactsNagDialog unresolvedStratConContactsNagDialog = new UnresolvedStratConContactsNagDialog(
                  campaign);
            if (unresolvedStratConContactsNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Outstanding Scenarios
        if (OutstandingScenariosNagDialog.checkNag(campaign)) {
            OutstandingScenariosNagDialog outstandingScenariosNagDialog = new OutstandingScenariosNagDialog(campaign);
            if (outstandingScenariosNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Deployment Shortfall
        final boolean isUseAtB = campaignOptions.isUseAtB();

        if (DeploymentShortfallNagDialog.checkNag(isUseAtB, campaign)) {
            DeploymentShortfallNagDialog deploymentShortfallNagDialog = new DeploymentShortfallNagDialog(campaign);
            if (deploymentShortfallNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Prisoners of War
        final boolean hasActiveContract = campaign.hasActiveContract();
        final boolean hasPrisoners = !campaign.getCurrentPrisoners().isEmpty();

        if (PrisonersNagDialog.checkNag(hasActiveContract, hasPrisoners)) {
            PrisonersNagDialog prisonersNagDialog = new PrisonersNagDialog(campaign);
            if (prisonersNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Pregnant Personnel Assigned to Active Force
        if (PregnantCombatantNagDialog.checkNag(hasActiveContract, activePersonnel)) {
            PregnantCombatantNagDialog pregnantCombatantNagDialog = new PregnantCombatantNagDialog(campaign);
            if (pregnantCombatantNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // HR Strain
        if (HRStrainNagDialog.checkNag(campaignOptions.isUseRandomRetirement(),
              campaignOptions.isUseHRStrain(),
              getHRStrainModifier(campaign))) {
            HRStrainNagDialog HRStrainNagDialog = new HRStrainNagDialog(campaign);
            if (HRStrainNagDialog.shouldCancelAdvanceDay()) {
                return true;
            }
        }

        // Contract Ended
        if (EndContractNagDialog.checkNag(today, activeContracts)) {
            EndContractNagDialog endContractNagDialog = new EndContractNagDialog(campaign);
            return endContractNagDialog.shouldCancelAdvanceDay();
        }

        // Player did not cancel Advance Day at any point
        return false;
    }
}
