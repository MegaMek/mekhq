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
     * </p>
     *
     * @param campaign The {@link Campaign} object representing the current campaign.
     * @return {@code true} if the player cancels any nag dialog, {@code false} otherwise.
     */
    public static boolean triggerDailyNags(Campaign campaign) {
        // Invalid Faction
        InvalidFactionNagDialog invalidFactionNagDialog = new InvalidFactionNagDialog(campaign);
        invalidFactionNagDialog.checkNag(campaign);
        if (!invalidFactionNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // No Commander
        NoCommanderNagDialog noCommanderNagDialog = new NoCommanderNagDialog(campaign);
        noCommanderNagDialog.checkNag(campaign);
        if (!noCommanderNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Untreated personnel
        UntreatedPersonnelNagDialog untreatedPersonnelNagDialog = new UntreatedPersonnelNagDialog(campaign);
        untreatedPersonnelNagDialog.checkNag(campaign);
        if (!untreatedPersonnelNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Unable to afford expenses
        UnableToAffordExpensesNagDialog unableToAffordExpensesNagDialog = new UnableToAffordExpensesNagDialog(campaign);
        unableToAffordExpensesNagDialog.checkNag(campaign);
        if (!unableToAffordExpensesNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Unable to afford next jump
        UnableToAffordJumpNagDialog unableToAffordJumpNagDialog = new UnableToAffordJumpNagDialog(campaign);
        unableToAffordJumpNagDialog.checkNag(campaign);
        if (!unableToAffordJumpNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Unable to afford next loan payment
        UnableToAffordLoanPaymentNagDialog unableToAffordLoanPaymentNagDialog = new UnableToAffordLoanPaymentNagDialog(campaign);
        unableToAffordLoanPaymentNagDialog.checkNag(campaign);
        if (!unableToAffordLoanPaymentNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Unmaintained Units
        UnmaintainedUnitsNagDialog unmaintainedUnitsNagDialog = new UnmaintainedUnitsNagDialog(campaign);
        unmaintainedUnitsNagDialog.checkNag(campaign);
        if (!unmaintainedUnitsNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Insufficient Medics
        InsufficientMedicsNagDialog insufficientMedicsNagDialog = new InsufficientMedicsNagDialog(campaign);
        insufficientMedicsNagDialog.checkNag();
        if (!insufficientMedicsNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Insufficient AsTechs
        InsufficientAstechsNagDialog insufficientAstechsNagDialog = new InsufficientAstechsNagDialog(campaign);
        insufficientAstechsNagDialog.checkNag();
        if (!insufficientAstechsNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Insufficient AsTech Time
        InsufficientAstechTimeNagDialog insufficientAstechTimeNagDialog = new InsufficientAstechTimeNagDialog(campaign);
        insufficientAstechTimeNagDialog.checkNag();
        if (!insufficientAstechTimeNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Unresolved StratCon AO Contacts
        UnresolvedStratConContactsNagDialog unresolvedStratConContactsNagDialog = new UnresolvedStratConContactsNagDialog(campaign);
        unresolvedStratConContactsNagDialog.checkNag(campaign);
        if (!unresolvedStratConContactsNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Outstanding Scenarios
        OutstandingScenariosNagDialog outstandingScenariosNagDialog = new OutstandingScenariosNagDialog(campaign);
        outstandingScenariosNagDialog.checkNag(campaign);
        if (!outstandingScenariosNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Deployment Shortfall
        DeploymentShortfallNagDialog deploymentShortfallNagDialog = new DeploymentShortfallNagDialog(campaign);
        deploymentShortfallNagDialog.checkNag(campaign);
        if (!deploymentShortfallNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Prisoners of War
        PrisonersNagDialog prisonersNagDialog = new PrisonersNagDialog(campaign);
        prisonersNagDialog.checkNag(campaign);
        if (!prisonersNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Pregnant Personnel Assigned to Active Force
        PregnantCombatantNagDialog pregnantCombatantNagDialog = new PregnantCombatantNagDialog(campaign);
        pregnantCombatantNagDialog.checkNag(campaign);
        if (!pregnantCombatantNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Contract Ended
        EndContractNagDialog endContractNagDialog = new EndContractNagDialog(campaign);
        endContractNagDialog.checkNag(campaign);
        if (!endContractNagDialog.isAdvanceDaySelected()) {
            return true;
        }

        // Player did not cancel Advance Day at any point
        return false;
    }
}
