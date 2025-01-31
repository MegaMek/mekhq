package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;

/**
 * A controller class responsible for managing and triggering daily nag dialogs in the campaign.
 *
 * <p>
 * The purpose of this class is to sequentially check all predefined "nag" conditions
 * to alert the player about issues that require their attention before advancing the day
 * in the campaign. Each nag dialog is displayed based on specific conditions, and the daily
 * nag process stops if the player cancels proceeding to the next day.
 * </p>
 *
 * <strong>Usage:</strong>
 * <p>
 * This class is primarily called during the "Advance Day" process in MekHQ, to notify
 * players of campaign-related issues spanning financial, personnel, and strategic concerns.
 * </p>
 */
public class NagController {
    /**
     * Triggers a sequence of daily nag dialogs to check and display issues in the campaign.
     *
     * <p>
     * This method iterates through all predefined nag dialogs, each associated with a specific
     * condition or scenario within the campaign. Nags include, but are not limited to, the following:
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
     * @return {@code true} if the player cancels any nag dialog, {@code false} otherwise.
     */
    public static boolean triggerDailyNags(Campaign campaign) {
        // Invalid Faction
        if (InvalidFactionNagDialog.checkNag(campaign)) {
            InvalidFactionNagDialog invalidFactionNagDialog = new InvalidFactionNagDialog(campaign);
            if (invalidFactionNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // No Commander
        if (NoCommanderNagDialog.checkNag(campaign)) {
            NoCommanderNagDialog noCommanderNagDialog = new NoCommanderNagDialog(campaign);
            if (noCommanderNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Untreated personnel
        if (UntreatedPersonnelNagDialog.checkNag(campaign)) {
            UntreatedPersonnelNagDialog untreatedPersonnelNagDialog = new UntreatedPersonnelNagDialog(campaign);
            if (untreatedPersonnelNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Unable to afford expenses
        if (UnableToAffordExpensesNagDialog.checkNag(campaign)) {
            UnableToAffordExpensesNagDialog unableToAffordExpensesNagDialog = new UnableToAffordExpensesNagDialog(campaign);
            if (unableToAffordExpensesNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Unable to afford next jump
        if (UnableToAffordJumpNagDialog.checkNag(campaign)) {
            UnableToAffordJumpNagDialog unableToAffordJumpNagDialog = new UnableToAffordJumpNagDialog(campaign);
            if (unableToAffordJumpNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Unable to afford next loan payment
        if (UnableToAffordLoanPaymentNagDialog.checkNag(campaign)) {
            UnableToAffordLoanPaymentNagDialog unableToAffordLoanPaymentNagDialog = new UnableToAffordLoanPaymentNagDialog(campaign);
            if (unableToAffordLoanPaymentNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Unmaintained Units
        if (UnmaintainedUnitsNagDialog.checkNag(campaign)) {
            UnmaintainedUnitsNagDialog unmaintainedUnitsNagDialog = new UnmaintainedUnitsNagDialog(campaign);
            if (unmaintainedUnitsNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Insufficient Medics
        if (InsufficientMedicsNagDialog.checkNag(campaign)) {
            InsufficientMedicsNagDialog insufficientMedicsNagDialog = new InsufficientMedicsNagDialog(campaign);
            if (insufficientMedicsNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Insufficient AsTechs
        if (InsufficientAstechsNagDialog.checkNag(campaign)) {
            InsufficientAstechsNagDialog insufficientAstechsNagDialog = new InsufficientAstechsNagDialog(campaign);
            if (insufficientAstechsNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Insufficient AsTech Time
        if (InsufficientAstechTimeNagDialog.checkNag(campaign)) {
            InsufficientAstechTimeNagDialog insufficientAstechTimeNagDialog = new InsufficientAstechTimeNagDialog(campaign);
            if (insufficientAstechTimeNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Unresolved StratCon AO Contacts
        if (UnresolvedStratConContactsNagDialog.checkNag(campaign)) {
            UnresolvedStratConContactsNagDialog unresolvedStratConContactsNagDialog = new UnresolvedStratConContactsNagDialog(campaign);
            if (unresolvedStratConContactsNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Outstanding Scenarios
        if (OutstandingScenariosNagDialog.checkNag(campaign)) {
            OutstandingScenariosNagDialog outstandingScenariosNagDialog = new OutstandingScenariosNagDialog(campaign);
            if (outstandingScenariosNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Deployment Shortfall
        if (DeploymentShortfallNagDialog.checkNag(campaign)) {
            DeploymentShortfallNagDialog deploymentShortfallNagDialog = new DeploymentShortfallNagDialog(campaign);
            if (deploymentShortfallNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Prisoners of War
        if (PrisonersNagDialog.checkNag(campaign)) {
            PrisonersNagDialog prisonersNagDialog = new PrisonersNagDialog(campaign);
            if (prisonersNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Pregnant Personnel Assigned to Active Force
        if (PregnantCombatantNagDialog.checkNag(campaign)) {
            PregnantCombatantNagDialog pregnantCombatantNagDialog = new PregnantCombatantNagDialog(campaign);
            if (pregnantCombatantNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Contract Ended
        if (EndContractNagDialog.checkNag(campaign)) {
            EndContractNagDialog endContractNagDialog = new EndContractNagDialog(campaign);
            if (endContractNagDialog.wasAdvanceDayCanceled()) {
                return true;
            }
        }

        // Player did not cancel Advance Day at any point
        return false;
    }
}
