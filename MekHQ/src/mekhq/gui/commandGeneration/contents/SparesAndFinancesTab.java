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
 * NOTICE: The MekHQ organization is a non-profit group of volunteers
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
package mekhq.gui.commandGeneration.contents;

import static mekhq.gui.commandGeneration.components.CommandGenerationUtilities.getCommandGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.universe.commandGeneration.ratgen.CommandGenerator;
import mekhq.gui.commandGeneration.components.CommandGenerationCheckBox;
import mekhq.gui.commandGeneration.components.CommandGenerationLabel;
import mekhq.gui.commandGeneration.components.CommandGenerationStandardPanel;

/**
 * Spares &amp; Finances tab: the merged former Spares and Other tabs, laid out in the two-column
 * style of the Personnel &amp; Officers tab.
 *
 * <p><b>Left column - Spare Parts Coverage.</b> Mirrors the AutoLogistics restock percentages from
 * Campaign Options' Acquisition &amp; Delivery page: the same thirteen part categories, in the same
 * order, with the same labels, tooltips, and 0-10000 range. The values ARE the campaign options:
 * spinners load from and write back to the campaign's {@link CampaignOptions} {@code autoLogistics*}
 * fields directly, so the tab always opens showing whatever is set in Campaign Options and edits
 * here apply to the campaign on OK - no field duplication on {@link CommandGenerationOptions}. The
 * same percentages drive both the starting spare inventory at generation time and the campaign's
 * ongoing auto-logistics resupply during play.</p>
 *
 * <p><b>Right column</b> - the post-generation rule sections:</p>
 * <ol>
 *   <li><b>Contracts</b> - Select Starting Contract, Start Course to Contract Planet</li>
 *   <li><b>Finances</b> - Process Finances toggle plus the starting-cash percentage: the command
 *       is granted free, and starting cash is working capital sized as a percentage of the
 *       generated units' total purchase cost (default 10%)</li>
 *   <li><b>Starting Simulation</b> - Run Starting Simulation toggle plus duration spinner and the
 *       two random-event toggles (marriages, procreation)</li>
 * </ol>
 */
public class SparesAndFinancesTab {

    // Mirror the Campaign Options AutoLogistics spinners (AcquisitionPage.createAutoLogisticsPanel):
    // 0-10000 in steps of 1, so no value settable there is ever clamped or rounded here.
    private static final int MIN_PERCENT = 0;
    private static final int MAX_PERCENT = 10000;
    private static final int STEP_PERCENT = 1;

    private final Campaign campaign;
    private CommandGenerationOptions options;

    /** Ordered map: bundle-key suffix (also the lbl{key}.text key) → spares spinner. */
    private final Map<String, JSpinner> spinners = new LinkedHashMap<>();

    // Contracts
    private CommandGenerationCheckBox chkSelectStartingContract;
    private CommandGenerationCheckBox chkStartCourseToContractPlanet;

    // Finances
    private CommandGenerationCheckBox chkProcessFinances;
    private JSpinner spnStartingCashPercent;
    private JLabel lblStartingCashPreviewValue;
    private CommandGenerationCheckBox chkRandomizeStartingCash;
    private JSpinner spnRandomStartingCashDiceCount;
    private JSpinner spnMinimumStartingFloat;
    private CommandGenerationCheckBox chkStartingLoan;
    private final Map<String, CommandGenerationCheckBox> payForToggles = new LinkedHashMap<>();

    /** Supplies the Force Generator tab's current design model, for the starting-cash preview. */
    private final Supplier<ForceDescriptor> modelSupplier;

    // Starting Simulation
    private CommandGenerationCheckBox chkRunStartingSimulation;
    private JSpinner spnSimulationDuration;
    private CommandGenerationCheckBox chkSimulateRandomMarriages;
    private CommandGenerationCheckBox chkSimulateRandomProcreation;

    /**
     * @param campaign      the campaign the dialog is generating into
     * @param options       the options round-tripped through the tabs
     * @param modelSupplier supplies the Force Generator tab's current design model for the
     *                      starting-cash preview, or {@code null} when no preview is wanted
     */
    public SparesAndFinancesTab(Campaign campaign, CommandGenerationOptions options,
          @Nullable Supplier<ForceDescriptor> modelSupplier) {
        this.campaign = campaign;
        this.options = options;
        this.modelSupplier = modelSupplier;
    }

    public JPanel createTab() {
        // Two-column composition matching SetupTab: each column is a BoxLayout stack of bordered
        // sections, pinned north-west so the columns grow downward together.
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("pnlSparesAndFinancesTab");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(3, 6, 3, 6);

        // Left column: the help text leads so the player reads what the percentages do before the
        // thirteen-row spares grid beneath it - together roughly the height of the right column's
        // three stacked sections.
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.add(buildHelpSection());
        leftColumn.add(Box.createVerticalStrut(6));
        leftColumn.add(buildSparesSection());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        panel.add(leftColumn, gbc);

        // Right column: Contracts (2 rows) + Finances (the tallest section) + Starting Simulation.
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.add(buildContractsSection());
        rightColumn.add(Box.createVerticalStrut(6));
        rightColumn.add(buildFinancesSection());
        rightColumn.add(Box.createVerticalStrut(6));
        rightColumn.add(buildStartingSimulationSection());
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        panel.add(rightColumn, gbc);

        return panel;
    }

    // region Spares column

    private JPanel buildSparesSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "SparesPercentages", true, "SparesPercentages");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        // Same categories and order as the Campaign Options AutoLogistics grid.
        String[] keys = {
              "SparesMekHead",
              "SparesMekLocation",
              "SparesNonRepairableLocation",
              "SparesArmor",
              "SparesAmmunition",
              "SparesHeatSink",
              "SparesWeapons",
              "SparesActuators",
              "SparesJumpJets",
              "SparesHeadComponents",
              "SparesEngines",
              "SparesGyros",
              "SparesOther"
        };

        int row = 0;
        for (String key : keys) {
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(100, MIN_PERCENT, MAX_PERCENT, STEP_PERCENT));
            spinner.setName("spn" + key);
            spinners.put(key, spinner);

            JLabel label = new CommandGenerationLabel(key);
            label.setLabelFor(spinner);

            gbc.gridy = row;
            gbc.gridx = 0;
            section.add(label, gbc);
            gbc.gridx = 1;
            section.add(spinner, gbc);
            row++;
        }

        return section;
    }

    private JPanel buildHelpSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "SparesHelp", true, "SparesHelp");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        // The bundle text carries a fixed HTML body width so the label wraps; an unconstrained HTML
        // JLabel reports its whole text as one line of preferred width, which inflated this column
        // until the finances column was pushed off-screen.
        section.add(new CommandGenerationLabel("SparesHelpBody", true), gbc);
        return section;
    }

    // endregion Spares column

    // region Finances column

    private JPanel buildContractsSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "Contracts", true, "Contracts");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        chkSelectStartingContract = new CommandGenerationCheckBox("SelectStartingContract");
        chkStartCourseToContractPlanet = new CommandGenerationCheckBox("StartCourseToContractPlanet");
        chkSelectStartingContract.addActionListener(evt ->
              chkStartCourseToContractPlanet.setEnabled(chkSelectStartingContract.isSelected()));

        gbc.gridy = 0;
        section.add(chkSelectStartingContract, gbc);
        gbc.gridy = 1;
        section.add(chkStartCourseToContractPlanet, gbc);

        return section;
    }

    private JPanel buildFinancesSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "Finances", true, "Finances");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        chkProcessFinances = new CommandGenerationCheckBox("ProcessFinances");
        spnStartingCashPercent = new JSpinner(new SpinnerNumberModel(10, 0, 1000, 1));
        spnStartingCashPercent.setName("spnStartingCashPercent");
        lblStartingCashPreviewValue = new JLabel();
        lblStartingCashPreviewValue.setName("lblStartingCashPreviewValue");
        chkRandomizeStartingCash = new CommandGenerationCheckBox("RandomizeStartingCash");
        spnRandomStartingCashDiceCount = new JSpinner(new SpinnerNumberModel(18, 1, 100, 1));
        spnRandomStartingCashDiceCount.setName("spnRandomStartingCashDiceCount");
        spnMinimumStartingFloat = new JSpinner(new SpinnerNumberModel(0, 0, 100_000_000, 100_000));
        spnMinimumStartingFloat.setName("spnMinimumStartingFloat");
        chkStartingLoan = new CommandGenerationCheckBox("StartingLoan");

        chkProcessFinances.addActionListener(evt -> refreshFinanceEnablement());
        chkRandomizeStartingCash.addActionListener(evt -> {
            refreshFinanceEnablement();
            refreshStartingCashPreview();
        });
        spnStartingCashPercent.addChangeListener(evt -> refreshStartingCashPreview());

        int row = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        section.add(chkProcessFinances, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = row;
        gbc.gridx = 0;
        section.add(new CommandGenerationLabel("StartingCashPercent"), gbc);
        gbc.gridx = 1;
        section.add(spnStartingCashPercent, gbc);
        row++;

        gbc.gridy = row;
        gbc.gridx = 0;
        section.add(new CommandGenerationLabel("StartingCashPreview"), gbc);
        gbc.gridx = 1;
        section.add(lblStartingCashPreviewValue, gbc);
        row++;

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        section.add(chkRandomizeStartingCash, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = row;
        gbc.gridx = 0;
        section.add(new CommandGenerationLabel("RandomStartingCashDiceCount"), gbc);
        gbc.gridx = 1;
        section.add(spnRandomStartingCashDiceCount, gbc);
        row++;

        gbc.gridy = row;
        gbc.gridx = 0;
        section.add(new CommandGenerationLabel("MinimumStartingFloat"), gbc);
        gbc.gridx = 1;
        section.add(spnMinimumStartingFloat, gbc);
        row++;

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        section.add(chkStartingLoan, gbc);

        // Pay For sub-section: PayForSetup is the master gate; the rest pick which generation costs
        // are debited from the starting cash.
        String[] payForNames = { "PayForSetup", "PayForPersonnel", "PayForUnits", "PayForParts",
              "PayForArmour", "PayForAmmunition" };
        for (String payForName : payForNames) {
            CommandGenerationCheckBox chk = new CommandGenerationCheckBox(payForName);
            payForToggles.put(payForName, chk);
            gbc.gridy = row++;
            gbc.gridx = 0;
            gbc.gridwidth = 2;
            section.add(chk, gbc);
        }
        payForToggles.get("PayForSetup").addActionListener(evt -> refreshFinanceEnablement());

        return section;
    }

    /**
     * Applies the Finances section's enable rules: Process Finances gates everything; Randomize
     * swaps the percentage spinner for the dice count; the individual pay-for toggles are live only
     * while Pay for Initial Setup is on.
     */
    private void refreshFinanceEnablement() {
        boolean finances = chkProcessFinances.isSelected();
        boolean randomize = chkRandomizeStartingCash.isSelected();
        spnStartingCashPercent.setEnabled(finances && !randomize);
        chkRandomizeStartingCash.setEnabled(finances);
        spnRandomStartingCashDiceCount.setEnabled(finances && randomize);
        spnMinimumStartingFloat.setEnabled(finances);
        chkStartingLoan.setEnabled(finances);
        boolean paySetup = payForToggles.get("PayForSetup").isSelected();
        for (Map.Entry<String, CommandGenerationCheckBox> entry : payForToggles.entrySet()) {
            entry.getValue().setEnabled(finances && ("PayForSetup".equals(entry.getKey()) || paySetup));
        }
    }

    /**
     * Recomputes the estimated starting cash from the Force Generator tab's current design model:
     * the configured percentage of the rolled combat units' estimated purchase value. Support units
     * are only generated at build time, so the estimate is a floor. Randomized cash shows the dice
     * pool instead, and pay-for debits are not previewed (they depend on build-time costs).
     */
    public void refreshStartingCashPreview() {
        if (lblStartingCashPreviewValue == null) {
            return;
        }
        if (chkRandomizeStartingCash.isSelected()) {
            lblStartingCashPreviewValue.setText(getFormattedTextAt(getCommandGenerationResourceBundle(),
                  "startingCashPreview.random", (Integer) spnRandomStartingCashDiceCount.getValue()));
            return;
        }
        ForceDescriptor model = (modelSupplier == null) ? null : modelSupplier.get();
        List<Entity> entities = (model == null) ? List.of() : CommandGenerator.collectEntities(model);
        if (entities.isEmpty() || campaign == null) {
            lblStartingCashPreviewValue.setText(getTextAt(getCommandGenerationResourceBundle(),
                  "startingCashPreview.empty"));
            return;
        }
        int percent = (Integer) spnStartingCashPercent.getValue();
        Money unitValue = CommandGenerator.estimateUnitValue(campaign, entities);
        Money estimate = unitValue.multipliedBy(percent).dividedBy(100).round();
        lblStartingCashPreviewValue.setText(getFormattedTextAt(getCommandGenerationResourceBundle(),
              "startingCashPreview.value", estimate.toAmountAndSymbolString(), percent,
              unitValue.toAmountAndSymbolString()));
    }

    private JPanel buildStartingSimulationSection() {
        CommandGenerationStandardPanel section = new CommandGenerationStandardPanel(
              "StartingSimulation", true, "StartingSimulation");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        chkRunStartingSimulation = new CommandGenerationCheckBox("RunStartingSimulation");
        spnSimulationDuration = new JSpinner(new SpinnerNumberModel(12, 1, 600, 1));
        spnSimulationDuration.setName("spnSimulationDuration");
        chkSimulateRandomMarriages = new CommandGenerationCheckBox("SimulateRandomMarriages");
        chkSimulateRandomProcreation = new CommandGenerationCheckBox("SimulateRandomProcreation");

        chkRunStartingSimulation.addActionListener(evt -> {
            boolean sim = chkRunStartingSimulation.isSelected();
            spnSimulationDuration.setEnabled(sim);
            chkSimulateRandomMarriages.setEnabled(sim);
            chkSimulateRandomProcreation.setEnabled(sim);
        });

        int row = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        section.add(chkRunStartingSimulation, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = row;
        gbc.gridx = 0;
        section.add(new CommandGenerationLabel("SimulationDuration"), gbc);
        gbc.gridx = 1;
        section.add(spnSimulationDuration, gbc);
        row++;

        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        section.add(chkSimulateRandomMarriages, gbc);

        gbc.gridy = row;
        section.add(chkSimulateRandomProcreation, gbc);

        return section;
    }

    // endregion Finances column

    private static GridBagConstraints sectionConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 6, 3, 6);
        return gbc;
    }

    /**
     * Reads values into the tab's controls. The spares spinners load from the campaign's
     * {@link CampaignOptions} (their source of truth); the contract / finance / simulation controls
     * load from the supplied {@code sourceOptions}.
     */
    public void loadValuesFromOptions(CommandGenerationOptions sourceOptions) {
        this.options = sourceOptions;

        loadSparesFromCampaignOptions();

        if (sourceOptions == null) {
            return;
        }

        chkSelectStartingContract.setSelected(sourceOptions.isSelectStartingContract());
        chkStartCourseToContractPlanet.setSelected(sourceOptions.isStartCourseToContractPlanet());
        chkStartCourseToContractPlanet.setEnabled(chkSelectStartingContract.isSelected());

        chkProcessFinances.setSelected(sourceOptions.isProcessFinances());
        spnStartingCashPercent.setValue(sourceOptions.getStartingCashPercent());
        chkRandomizeStartingCash.setSelected(sourceOptions.isRandomizeStartingCash());
        spnRandomStartingCashDiceCount.setValue(sourceOptions.getRandomStartingCashDiceCount());
        spnMinimumStartingFloat.setValue(sourceOptions.getMinimumStartingFloat());
        chkStartingLoan.setSelected(sourceOptions.isStartingLoan());
        payForToggles.get("PayForSetup").setSelected(sourceOptions.isPayForSetup());
        payForToggles.get("PayForPersonnel").setSelected(sourceOptions.isPayForPersonnel());
        payForToggles.get("PayForUnits").setSelected(sourceOptions.isPayForUnits());
        payForToggles.get("PayForParts").setSelected(sourceOptions.isPayForParts());
        payForToggles.get("PayForArmour").setSelected(sourceOptions.isPayForArmour());
        payForToggles.get("PayForAmmunition").setSelected(sourceOptions.isPayForAmmunition());
        refreshFinanceEnablement();
        refreshStartingCashPreview();

        chkRunStartingSimulation.setSelected(sourceOptions.isRunStartingSimulation());
        spnSimulationDuration.setValue(sourceOptions.getSimulationDuration());
        chkSimulateRandomMarriages.setSelected(sourceOptions.isSimulateRandomMarriages());
        chkSimulateRandomProcreation.setSelected(sourceOptions.isSimulateRandomProcreation());
        boolean sim = chkRunStartingSimulation.isSelected();
        spnSimulationDuration.setEnabled(sim);
        chkSimulateRandomMarriages.setEnabled(sim);
        chkSimulateRandomProcreation.setEnabled(sim);
    }

    private void loadSparesFromCampaignOptions() {
        if (campaign == null) {
            return;
        }
        CampaignOptions co = campaign.getCampaignOptions();
        if (co == null) {
            return;
        }
        spinners.get("SparesMekHead").setValue(clamp(co.getAutoLogisticsMekHead()));
        spinners.get("SparesMekLocation").setValue(clamp(co.getAutoLogisticsMekLocation()));
        spinners.get("SparesNonRepairableLocation").setValue(clamp(co.getAutoLogisticsNonRepairableLocation()));
        spinners.get("SparesArmor").setValue(clamp(co.getAutoLogisticsArmor()));
        spinners.get("SparesAmmunition").setValue(clamp(co.getAutoLogisticsAmmunition()));
        spinners.get("SparesHeatSink").setValue(clamp(co.getAutoLogisticsHeatSink()));
        spinners.get("SparesWeapons").setValue(clamp(co.getAutoLogisticsWeapons()));
        spinners.get("SparesActuators").setValue(clamp(co.getAutoLogisticsActuators()));
        spinners.get("SparesJumpJets").setValue(clamp(co.getAutoLogisticsJumpJets()));
        spinners.get("SparesHeadComponents").setValue(clamp(co.getAutoLogisticsHeadComponents()));
        spinners.get("SparesEngines").setValue(clamp(co.getAutoLogisticsEngines()));
        spinners.get("SparesGyros").setValue(clamp(co.getAutoLogisticsGyros()));
        spinners.get("SparesOther").setValue(clamp(co.getAutoLogisticsOther()));
    }

    /**
     * Writes the tab's controls back out. The spares spinners write to the campaign's
     * {@link CampaignOptions} (making the dialog's selections effective immediately for both the
     * initial spawn and ongoing resupply); the contract / finance / simulation controls write to the
     * supplied {@code targetOptions}.
     */
    public void writeValuesToOptions(CommandGenerationOptions targetOptions) {
        writeSparesToCampaignOptions();

        if (targetOptions == null) {
            return;
        }

        targetOptions.setSelectStartingContract(chkSelectStartingContract.isSelected());
        targetOptions.setStartCourseToContractPlanet(chkStartCourseToContractPlanet.isSelected());

        targetOptions.setProcessFinances(chkProcessFinances.isSelected());
        targetOptions.setStartingCashPercent((Integer) spnStartingCashPercent.getValue());
        targetOptions.setRandomizeStartingCash(chkRandomizeStartingCash.isSelected());
        targetOptions.setRandomStartingCashDiceCount((Integer) spnRandomStartingCashDiceCount.getValue());
        targetOptions.setMinimumStartingFloat((Integer) spnMinimumStartingFloat.getValue());
        targetOptions.setStartingLoan(chkStartingLoan.isSelected());
        targetOptions.setPayForSetup(payForToggles.get("PayForSetup").isSelected());
        targetOptions.setPayForPersonnel(payForToggles.get("PayForPersonnel").isSelected());
        targetOptions.setPayForUnits(payForToggles.get("PayForUnits").isSelected());
        targetOptions.setPayForParts(payForToggles.get("PayForParts").isSelected());
        targetOptions.setPayForArmour(payForToggles.get("PayForArmour").isSelected());
        targetOptions.setPayForAmmunition(payForToggles.get("PayForAmmunition").isSelected());

        targetOptions.setRunStartingSimulation(chkRunStartingSimulation.isSelected());
        targetOptions.setSimulationDuration((Integer) spnSimulationDuration.getValue());
        targetOptions.setSimulateRandomMarriages(chkSimulateRandomMarriages.isSelected());
        targetOptions.setSimulateRandomProcreation(chkSimulateRandomProcreation.isSelected());
    }

    private void writeSparesToCampaignOptions() {
        if (campaign == null) {
            return;
        }
        CampaignOptions co = campaign.getCampaignOptions();
        if (co == null) {
            return;
        }
        co.setAutoLogisticsMekHead((Integer) spinners.get("SparesMekHead").getValue());
        co.setAutoLogisticsMekLocation((Integer) spinners.get("SparesMekLocation").getValue());
        co.setAutoLogisticsNonRepairableLocation((Integer) spinners.get("SparesNonRepairableLocation").getValue());
        co.setAutoLogisticsArmor((Integer) spinners.get("SparesArmor").getValue());
        co.setAutoLogisticsAmmunition((Integer) spinners.get("SparesAmmunition").getValue());
        co.setAutoLogisticsHeatSink((Integer) spinners.get("SparesHeatSink").getValue());
        co.setAutoLogisticsWeapons((Integer) spinners.get("SparesWeapons").getValue());
        co.setAutoLogisticsActuators((Integer) spinners.get("SparesActuators").getValue());
        co.setAutoLogisticsJumpJets((Integer) spinners.get("SparesJumpJets").getValue());
        co.setAutoLogisticsHeadComponents((Integer) spinners.get("SparesHeadComponents").getValue());
        co.setAutoLogisticsEngines((Integer) spinners.get("SparesEngines").getValue());
        co.setAutoLogisticsGyros((Integer) spinners.get("SparesGyros").getValue());
        co.setAutoLogisticsOther((Integer) spinners.get("SparesOther").getValue());
    }

    private static int clamp(int value) {
        if (value < MIN_PERCENT) return MIN_PERCENT;
        if (value > MAX_PERCENT) return MAX_PERCENT;
        return value;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CommandGenerationOptions getOptions() {
        return options;
    }
}
