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
        InvalidFactionNagDialog invalidFactionNagDialog = new InvalidFactionNagDialog(campaign);
        invalidFactionNagDialog.checkNag();
        if (invalidFactionNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // No Commander
        NoCommanderNagDialog noCommanderNagDialog = new NoCommanderNagDialog(campaign);
        noCommanderNagDialog.checkNag();
        if (noCommanderNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Untreated personnel
        UntreatedPersonnelNagDialog untreatedPersonnelNagDialog = new UntreatedPersonnelNagDialog(campaign);
        untreatedPersonnelNagDialog.checkNag();
        if (untreatedPersonnelNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Unable to afford expenses
        UnableToAffordExpensesNagDialog unableToAffordExpensesNagDialog = new UnableToAffordExpensesNagDialog(campaign);
        unableToAffordExpensesNagDialog.checkNag();
        if (unableToAffordExpensesNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Unable to afford next jump
        UnableToAffordJumpNagDialog unableToAffordJumpNagDialog = new UnableToAffordJumpNagDialog(campaign);
        unableToAffordJumpNagDialog.checkNag();
        if (unableToAffordJumpNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Unable to afford next loan payment
        UnableToAffordLoanPaymentNagDialog unableToAffordLoanPaymentNagDialog = new UnableToAffordLoanPaymentNagDialog(campaign);
        unableToAffordLoanPaymentNagDialog.checkNag();
        if (unableToAffordLoanPaymentNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Unmaintained Units
        UnmaintainedUnitsNagDialog unmaintainedUnitsNagDialog = new UnmaintainedUnitsNagDialog(campaign);
        unmaintainedUnitsNagDialog.checkNag();
        if (unmaintainedUnitsNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Insufficient Medics
        InsufficientMedicsNagDialog insufficientMedicsNagDialog = new InsufficientMedicsNagDialog(campaign);
        insufficientMedicsNagDialog.checkNag();
        if (insufficientMedicsNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Insufficient AsTechs
        InsufficientAstechsNagDialog insufficientAstechsNagDialog = new InsufficientAstechsNagDialog(campaign);
        insufficientAstechsNagDialog.checkNag();
        if (insufficientAstechsNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Insufficient AsTech Time
        InsufficientAstechTimeNagDialog insufficientAstechTimeNagDialog = new InsufficientAstechTimeNagDialog(campaign);
        insufficientAstechTimeNagDialog.checkNag();
        if (insufficientAstechTimeNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Unresolved StratCon AO Contacts
        UnresolvedStratConContactsNagDialog unresolvedStratConContactsNagDialog = new UnresolvedStratConContactsNagDialog(campaign);
        unresolvedStratConContactsNagDialog.checkNag();
        if (unresolvedStratConContactsNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Outstanding Scenarios
        OutstandingScenariosNagDialog outstandingScenariosNagDialog = new OutstandingScenariosNagDialog(campaign);
        outstandingScenariosNagDialog.checkNag();
        if (outstandingScenariosNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Deployment Shortfall
        DeploymentShortfallNagDialog deploymentShortfallNagDialog = new DeploymentShortfallNagDialog(campaign);
        deploymentShortfallNagDialog.checkNag();
        if (deploymentShortfallNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Prisoners of War
        PrisonersNagDialog prisonersNagDialog = new PrisonersNagDialog(campaign);
        prisonersNagDialog.checkNag();
        if (prisonersNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Pregnant Personnel Assigned to Active Force
        PregnantCombatantNagDialog pregnantCombatantNagDialog = new PregnantCombatantNagDialog(campaign);
        pregnantCombatantNagDialog.checkNag();
        if (pregnantCombatantNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Contract Ended
        EndContractNagDialog endContractNagDialog = new EndContractNagDialog(campaign);
        endContractNagDialog.checkNag();
        if (endContractNagDialog.wasAdvanceDayCanceled()) {
            return true;
        }

        // Player did not cancel Advance Day at any point
        return false;
    }
}
