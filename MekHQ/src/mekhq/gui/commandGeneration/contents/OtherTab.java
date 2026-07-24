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
package mekhq.gui.commandGeneration.contents;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.gui.commandGeneration.components.CommandGenerationCheckBox;
import mekhq.gui.commandGeneration.components.CommandGenerationLabel;
import mekhq.gui.commandGeneration.components.CommandGenerationStandardPanel;

/**
 * Post-generation rules and miscellany. Four titled sections stacked vertically:
 *
 * <ol>
 *   <li><b>Contracts</b> — Select Starting Contract, Start Course to Contract Planet</li>
 *   <li><b>Finances</b> — Process Finances master toggle plus starting cash / randomization /
 *       minimum-float / initial contract payment / starting-loan / six "Pay For" toggles</li>
 *   <li><b>Starting Simulation</b> — Run Starting Simulation toggle plus duration spinner and the
 *       two random-event toggles (marriages, procreation)</li>
 * </ol>
 *
 * <p>Legacy "Unit Extras" (mothballed counts, AtB/Windchild-only roll customizations) are
 * intentionally omitted — those controls are tied to the soon-to-be-deleted Windchild / AtB
 * generators and have no analogue in the Force Generator pipeline.</p>
 */
public class OtherTab {

    private final Campaign campaign;
    private CompanyGenerationOptions options;

    // Contracts
    private CommandGenerationCheckBox chkSelectStartingContract;
    private CommandGenerationCheckBox chkStartCourseToContractPlanet;

    // Finances
    private CommandGenerationCheckBox chkProcessFinances;
    private JSpinner spnStartingCash;
    private CommandGenerationCheckBox chkRandomizeStartingCash;
    private JSpinner spnRandomStartingCashDiceCount;
    private JSpinner spnMinimumStartingFloat;
    private CommandGenerationCheckBox chkStartingLoan;
    private final Map<String, CommandGenerationCheckBox> payForToggles = new LinkedHashMap<>();

    // Starting Simulation
    private CommandGenerationCheckBox chkRunStartingSimulation;
    private JSpinner spnSimulationDuration;
    private CommandGenerationCheckBox chkSimulateRandomMarriages;
    private CommandGenerationCheckBox chkSimulateRandomProcreation;

    public OtherTab(Campaign campaign, CompanyGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    public JPanel createTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setName("pnlOtherTab");

        panel.add(buildContractsSection());
        panel.add(Box.createVerticalStrut(6));
        panel.add(buildFinancesSection());
        panel.add(Box.createVerticalStrut(6));
        panel.add(buildStartingSimulationSection());

        return panel;
    }

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
        spnStartingCash = new JSpinner(new SpinnerNumberModel(0, 0, 100_000_000, 100_000));
        spnStartingCash.setName("spnStartingCash");
        chkRandomizeStartingCash = new CommandGenerationCheckBox("RandomizeStartingCash");
        spnRandomStartingCashDiceCount = new JSpinner(new SpinnerNumberModel(6, 1, 20, 1));
        spnRandomStartingCashDiceCount.setName("spnRandomStartingCashDiceCount");
        spnMinimumStartingFloat = new JSpinner(new SpinnerNumberModel(0, 0, 100_000_000, 100_000));
        spnMinimumStartingFloat.setName("spnMinimumStartingFloat");
        chkStartingLoan = new CommandGenerationCheckBox("StartingLoan");

        chkRandomizeStartingCash.addActionListener(evt -> {
            boolean randomize = chkRandomizeStartingCash.isSelected();
            spnStartingCash.setEnabled(!randomize);
            spnRandomStartingCashDiceCount.setEnabled(randomize);
        });

        int row = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        section.add(chkProcessFinances, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = row;
        gbc.gridx = 0;
        section.add(new CommandGenerationLabel("StartingCash"), gbc);
        gbc.gridx = 1;
        section.add(spnStartingCash, gbc);
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

        // Pay For sub-section
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

        return section;
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

    public void loadValuesFromOptions(CompanyGenerationOptions sourceOptions) {
        this.options = sourceOptions;
        if (sourceOptions == null) {
            return;
        }

        chkSelectStartingContract.setSelected(sourceOptions.isSelectStartingContract());
        chkStartCourseToContractPlanet.setSelected(sourceOptions.isStartCourseToContractPlanet());
        chkStartCourseToContractPlanet.setEnabled(chkSelectStartingContract.isSelected());

        chkProcessFinances.setSelected(sourceOptions.isProcessFinances());
        spnStartingCash.setValue(sourceOptions.getStartingCash());
        chkRandomizeStartingCash.setSelected(sourceOptions.isRandomizeStartingCash());
        spnRandomStartingCashDiceCount.setValue(sourceOptions.getRandomStartingCashDiceCount());
        spnStartingCash.setEnabled(!chkRandomizeStartingCash.isSelected());
        spnRandomStartingCashDiceCount.setEnabled(chkRandomizeStartingCash.isSelected());
        spnMinimumStartingFloat.setValue(sourceOptions.getMinimumStartingFloat());
        chkStartingLoan.setSelected(sourceOptions.isStartingLoan());
        payForToggles.get("PayForSetup").setSelected(sourceOptions.isPayForSetup());
        payForToggles.get("PayForPersonnel").setSelected(sourceOptions.isPayForPersonnel());
        payForToggles.get("PayForUnits").setSelected(sourceOptions.isPayForUnits());
        payForToggles.get("PayForParts").setSelected(sourceOptions.isPayForParts());
        payForToggles.get("PayForArmour").setSelected(sourceOptions.isPayForArmour());
        payForToggles.get("PayForAmmunition").setSelected(sourceOptions.isPayForAmmunition());

        chkRunStartingSimulation.setSelected(sourceOptions.isRunStartingSimulation());
        spnSimulationDuration.setValue(sourceOptions.getSimulationDuration());
        chkSimulateRandomMarriages.setSelected(sourceOptions.isSimulateRandomMarriages());
        chkSimulateRandomProcreation.setSelected(sourceOptions.isSimulateRandomProcreation());
        boolean sim = chkRunStartingSimulation.isSelected();
        spnSimulationDuration.setEnabled(sim);
        chkSimulateRandomMarriages.setEnabled(sim);
        chkSimulateRandomProcreation.setEnabled(sim);
    }

    public void writeValuesToOptions(CompanyGenerationOptions targetOptions) {
        if (targetOptions == null) {
            return;
        }

        targetOptions.setSelectStartingContract(chkSelectStartingContract.isSelected());
        targetOptions.setStartCourseToContractPlanet(chkStartCourseToContractPlanet.isSelected());

        targetOptions.setProcessFinances(chkProcessFinances.isSelected());
        targetOptions.setStartingCash((Integer) spnStartingCash.getValue());
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

    public Campaign getCampaign() {
        return campaign;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }
}
