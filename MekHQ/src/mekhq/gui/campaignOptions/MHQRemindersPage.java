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
package mekhq.gui.campaignOptions;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import mekhq.MHQConstants;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;

/**
 * The Reminders &amp; Confirmations page of the MekHQ Client Options dialog: two sections of "ignore this warning"
 * check boxes (the new-day nags and the action confirmations). Owns its check boxes and copies them back into the
 * shared {@link MHQOptionsModel} in {@link #writeToModel()}.
 */
class MHQRemindersPage extends MHQOptionsPage {
    // Reminders & Confirmations (keyed by MHQConstants nag/confirmation key, matching MHQOptionsModel.nagIgnores)
    private final Map<String, CampaignOptionsCheckBox> nagCheckBoxes = new HashMap<>();

    MHQRemindersPage(MHQOptionsModel model) {
        super(model);
    }

    @Override
    Component createPage() {
        JComponent nagsContent = createRemindersNagsSection();
        JComponent confirmationsContent = createRemindersConfirmationsSection();
        // Register each section body's tips before the page is built; |= always evaluates its right side, so no
        // section's registration is skipped.
        boolean hasTooltips = registerDetailsTips(nagsContent);
        hasTooltips |= registerDetailsTips(confirmationsContent);
        Component page = pageBuilder("MHQRemindersPage", hasTooltips)
                     .section("lblMHQNagSection.text", "lblMHQNagSection.summary", nagsContent)
                     .section("lblMHQConfirmationSection.text", "lblMHQConfirmationSection.summary",
                           confirmationsContent)
                     .build();
        created = true;
        return page;
    }

    private JPanel createRemindersNagsSection() {
        // Each entry is a {resourceName, nagIgnoreKey} pair. The resource name resolves the check box text/tooltip in
        // the GUI bundle; the ignore key is the MHQConstants key the state is stored under. A few resource names differ
        // from the stored key (e.g. AdminStrain vs HR_STRAIN, Astechs vs AS_TECHS), so they are kept explicit here.
        String[][] nagOptions = {
              { "optionUnmaintainedUnitsNag", MHQConstants.NAG_UNMAINTAINED_UNITS },
              { "optionPregnantCombatantNag", MHQConstants.NAG_PREGNANT_COMBATANT },
              { "optionPrisonersNag", MHQConstants.NAG_PRISONERS },
              { "optionAdminStrainNag", MHQConstants.NAG_HR_STRAIN },
              { "optionUntreatedPersonnelNag", MHQConstants.NAG_UNTREATED_PERSONNEL },
              { "optionNoCommanderNag", MHQConstants.NAG_NO_COMMANDER },
              { "optionContractEndedNag", MHQConstants.NAG_CONTRACT_ENDED },
              { "optionSingleDropNag", MHQConstants.NAG_SINGLE_DROP_SET_UP },
              { "optionInsufficientAstechsNag", MHQConstants.NAG_INSUFFICIENT_AS_TECHS },
              { "optionInsufficientAstechTimeNag", MHQConstants.NAG_INSUFFICIENT_AS_TECH_TIME },
              { "optionInsufficientMedicsNag", MHQConstants.NAG_INSUFFICIENT_MEDICS },
              { "optionShortDeploymentNag", MHQConstants.NAG_SHORT_DEPLOYMENT },
              { "optionCombatChallengeNag", MHQConstants.NAG_COMBAT_CHALLENGE },
              { "optionUnresolvedStratConContactsNag", MHQConstants.NAG_UNRESOLVED_STRAT_CON_CONTACTS },
              { "optionOutstandingScenariosNag", MHQConstants.NAG_OUTSTANDING_SCENARIOS },
              { "optionInvalidFactionNag", MHQConstants.NAG_INVALID_FACTION },
              { "optionUnableToAffordExpensesNag", MHQConstants.NAG_UNABLE_TO_AFFORD_EXPENSES },
              { "optionUnableToAffordRentNag", MHQConstants.NAG_UNABLE_TO_AFFORD_RENT },
              { "optionUnableToAffordLoanPaymentNag", MHQConstants.NAG_UNABLE_TO_AFFORD_LOAN_PAYMENT },
              { "optionUnableToAffordJumpNag", MHQConstants.NAG_UNABLE_TO_AFFORD_JUMP },
              { "optionUnableToAffordShoppingListNag", MHQConstants.NAG_UNABLE_TO_AFFORD_SHOPPING_LIST },
              { "optionSomeoneRandomlyDiedCombatNag", MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_COMBAT },
              { "optionSomeoneRandomlyDiedTechNag", MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_TECH },
              { "optionSomeoneRandomlyDiedOtherSupportNag", MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_OTHER_SUPPORT },
              { "optionSomeoneRandomlyDiedCivilianNag", MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_CIVILIAN },
              { "optionSomeoneRandomlyDiedCampFollowerNag", MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_CAMP_FOLLOWER },
              { "optionSomeoneRandomlyDiedRetiredNag", MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_RETIREE },
        };
        return nagCheckBoxGrid("MHQNagContent", nagOptions);
    }

    private JPanel createRemindersConfirmationsSection() {
        String[][] confirmationOptions = {
              { "optionContractRentalConfirmation", MHQConstants.CONFIRMATION_CONTRACT_RENTAL },
              { "optionFactionStandingsUltimatumConfirmation", MHQConstants.CONFIRMATION_FACTION_STANDINGS_ULTIMATUM },
              { "optionBeginTransitConfirmation", MHQConstants.CONFIRMATION_BEGIN_TRANSIT },
              { "optionStratConBatchallBreachConfirmation", MHQConstants.CONFIRMATION_STRATCON_BATCHALL_BREACH },
              { "optionStratConDeployConfirmation", MHQConstants.CONFIRMATION_STRATCON_DEPLOY },
              { "optionResolveScenarioConfirmation", MHQConstants.CONFIRMATION_RESOLVE_SCENARIO },
              { "optionAbandonUnitsConfirmation", MHQConstants.CONFIRMATION_ABANDON_UNITS },
              { "optionAssignTechsConfirmation", MHQConstants.CONFIRMATION_ASSIGN_TECHS },
        };
        return nagCheckBoxGrid("MHQConfirmationContent", confirmationOptions);
    }

    /**
     * Builds a two-column check box grid from {@code entries}, where each entry is a {@code {resourceName, ignoreKey}}
     * pair. Each check box loads its state from {@link MHQOptionsModel#nagIgnores} and is registered in
     * {@link #nagCheckBoxes} under its ignore key so {@link #writeToModel()} can read it back.
     */
    private CampaignOptionsFormPanel nagCheckBoxGrid(String name, String[][] entries) {
        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (String[] entry : entries) {
            String ignoreKey = entry[1];
            CampaignOptionsCheckBox checkBox = new CampaignOptionsCheckBox(RESOURCE_BUNDLE, entry[0]);
            checkBox.setSelected(Boolean.TRUE.equals(model.nagIgnores.get(ignoreKey)));
            nagCheckBoxes.put(ignoreKey, checkBox);
            checkBoxes.add(checkBox);
        }
        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel(name, FORM_LABEL_WIDTH, FORM_CONTROL_WIDTH);
        panel.addCheckBoxGrid(2, checkBoxes.toArray(new JCheckBox[0]));
        return panel;
    }

    @Override
    void writeToModel() {
        if (!created) {
            return;
        }
        nagCheckBoxes.forEach((key, checkBox) -> model.nagIgnores.put(key, checkBox.isSelected()));
    }
}
