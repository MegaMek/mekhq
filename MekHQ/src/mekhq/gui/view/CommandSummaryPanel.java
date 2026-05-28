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

package mekhq.gui.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.events.assets.AssetEvent;
import mekhq.campaign.events.loans.LoanEvent;
import mekhq.campaign.events.transactions.TransactionEvent;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.ActionScheduler;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.ScalingWidthConstrainedPanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.ReportingUtilities;

/**
 * Displays a command summary including current funds, unit reputation, experience rating, and the combat strength
 * used for StratCon calculations.
 * <p>
 * This panel subscribes to the global event bus updates to stay synchronized with changes in the displayed stats.
 * </p>
 */
public class CommandSummaryPanel extends ScalingWidthConstrainedPanel {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.CommandSummary";

    private final JLabel lblFunds = new JLabel();
    private final JLabel lblFundsValue = new JLabel();
    private final JLabel lblUnitReputationValue = new JLabel();
    private final JLabel lblCombatStrengthValue = new JLabel();

    /**
     * Scheduler used to debounce funds refreshing. Multiple rapid financial events will collapse
     * into a single UI update.
     */
    private final ActionScheduler refreshFundsScheduler = new ActionScheduler(this::refreshFunds);

    private final Campaign campaign;

    /**
     * Constructs a new {@code CommandSummaryPanel}.
     *
     * @param minWidth the minimum enforced width of the panel in pixels
     * @param maxWidth the maximum enforced width of the panel in pixels
     * @param campaign the active {@link Campaign} to pull statistics from
     */
    public CommandSummaryPanel(int minWidth, int maxWidth, Campaign campaign) {
        super(minWidth, maxWidth);
        this.campaign = campaign;

        setBorder(RoundedLineBorder.createRoundedLineBorder(getTextAt("panel.title")));
        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        // Column 0: static labels
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;

        add(lblFunds, gridBagConstraints);
        gridBagConstraints.gridy++;
        add(new JLabel(getTextAt("reputation.label")), gridBagConstraints);
        gridBagConstraints.gridy++;
        add(new JLabel(getTextAt("combatStrength.label")), gridBagConstraints);

        // Column 1: dynamic data values
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.insets = new Insets(0, CampaignGUI.MEDIUM_GAP, 0, 0);

        add(lblFundsValue, gridBagConstraints);
        gridBagConstraints.gridy++;
        add(lblUnitReputationValue, gridBagConstraints);
        gridBagConstraints.gridy++;
        add(lblCombatStrengthValue, gridBagConstraints);

        refreshAll();
        MekHQ.registerHandler(this);
    }

    /**
     * Fully recalculates and updates all metrics on the panel.
     */
    private void refreshAll() {
        String experience = SkillType.getColoredExperienceLevelName(campaign.getReputation().getAverageSkillLevel());
        lblUnitReputationValue.setText(getFormattedTextAt("reputation.text", campaign.getUnitRatingText(), experience));
        int totalBv = AtBDynamicScenarioFactory.getBVBudgetForStratConSingles(campaign, true);
        lblCombatStrengthValue.setText(getFormattedTextAt("combatStrength.text", totalBv));
        refreshFunds();
    }

    /**
     * Refreshes only the funds label. Checks if the unit holds any active loans and
     * alters the formatting accordingly.
     */
    private void refreshFunds() {
        String amount = campaign.getFunds().toAmountString();
        lblFundsValue.setText(getFormattedTextAt("funds.text", amount));
        if (campaign.getFinances().isInDebt()) {
            lblFunds.setText(getFormattedTextAt("funds.label.hasLoan", ReportingUtilities.getNegativeColor()));
        } else  {
            lblFunds.setText(getTextAt("funds.label.noLoan"));
        }
    }

    /**
    * Retrieves and formats localized text from the panel's resource bundle.
    */
    private static String getFormattedTextAt(String key, Object... args) {
        return MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE, key, args);
    }

    /**
     * Retrieves localized text from the panel's resource bundle.
     */
    private static String getTextAt(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }

    // ======================================
    // Event handlers for UI synchronization
    // ======================================

    @Subscribe
    public void handle(NewDayEvent event) {
        refreshAll();
    }

    @Subscribe
    public void handle(final OptionsChangedEvent event) {
        refreshAll();
    }

    @Subscribe
    public void handle(TransactionEvent event) {
        refreshFundsScheduler.schedule();
    }

    @Subscribe
    public void handle(LoanEvent event) {
        refreshFundsScheduler.schedule();
    }

    @Subscribe
    public void handle(AssetEvent event) {
        refreshFundsScheduler.schedule();
    }

}
