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
package mekhq.gui.companyGeneration.contents;

import static mekhq.gui.companyGeneration.components.CompanyGenerationUtilities.getCompanyGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.comboBoxes.MMComboBox;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import mekhq.gui.companyGeneration.components.CompanyGenerationCheckBox;
import mekhq.gui.companyGeneration.components.CompanyGenerationLabel;
import mekhq.gui.companyGeneration.components.CompanyGenerationStandardPanel;
import mekhq.gui.panels.RandomOriginOptionsPanel;

/**
 * Pre-generation rules tab. Five titled sections, stacked vertically:
 *
 * <ol>
 *   <li><b>Force Shape</b> — Company Command Lance toggle, formation-naming method</li>
 *   <li><b>Support Personnel</b> — count spinners for the nine support roles (Mek Tech, Mechanic, Aero
 *       Tek, BA Tech, Doctor, Administrator × 4) plus the Pool Assistants toggle. Step 4 keeps the
 *       legacy absolute-count semantics; a follow-up step swaps these for percentage sliders driven
 *       by the canonical hours-needed formulas in {@code FieldManualMercRevDragoonsRating}.</li>
 *   <li><b>Officer Selection</b> — eight toggles controlling commander / officer picks and skill
 *       weighting</li>
 *   <li><b>Naming &amp; Ranks</b> — four toggles for rank auto-assignment, callsigns, and the founder
 *       flag</li>
 *   <li><b>Random Origin</b> — the existing {@link RandomOriginOptionsPanel} sub-panel</li>
 * </ol>
 *
 * <p>Field names match the legacy {@code CompanyGenerationOptionsPanel} so the load/write methods
 * here can map cleanly into {@link CompanyGenerationOptions} without renaming or migrating XML.</p>
 */
public class SetupTab {

    private static final int SUPPORT_SPINNER_MIN = 0;
    private static final int SUPPORT_SPINNER_MAX = 100;
    private static final int SUPPORT_SPINNER_STEP = 1;

    private final Campaign campaign;
    private CompanyGenerationOptions options;

    // Force shape
    private CompanyGenerationCheckBox chkGenerateMercenaryCompanyCommandLance;
    private MMComboBox<ForceNamingMethod> comboForceNamingMethod;

    // Support personnel
    private final Map<PersonnelRole, JSpinner> spnSupportPersonnelNumbers = new LinkedHashMap<>();
    private CompanyGenerationCheckBox chkPoolAssistants;

    // Officer selection
    private CompanyGenerationCheckBox chkAssignBestCompanyCommander;
    private CompanyGenerationCheckBox chkPrioritizeCompanyCommanderCombatSkills;
    private CompanyGenerationCheckBox chkAssignBestOfficers;
    private CompanyGenerationCheckBox chkPrioritizeOfficerCombatSkills;
    private CompanyGenerationCheckBox chkAssignMostSkilledToPrimaryLances;
    private CompanyGenerationCheckBox chkGenerateCaptains;
    private CompanyGenerationCheckBox chkAssignCompanyCommanderFlag;
    private CompanyGenerationCheckBox chkApplyOfficerStatBonusToWorstSkill;

    // Naming & ranks
    private CompanyGenerationCheckBox chkAutomaticallyAssignRanks;
    private CompanyGenerationCheckBox chkUseSpecifiedFactionToAssignRanks;
    private CompanyGenerationCheckBox chkAssignMekWarriorsCallSigns;
    private CompanyGenerationCheckBox chkAssignFounderFlag;

    // Random origin
    private RandomOriginOptionsPanel randomOriginOptionsPanel;

    public SetupTab(Campaign campaign, CompanyGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    /**
     * The ordered list of support roles surfaced as spinners in the Support Personnel section. Order
     * matches the legacy panel.
     */
    private static final PersonnelRole[] SUPPORT_ROLES = {
          PersonnelRole.MEK_TECH,
          PersonnelRole.MECHANIC,
          PersonnelRole.AERO_TEK,
          PersonnelRole.BA_TECH,
          PersonnelRole.DOCTOR,
          PersonnelRole.ADMINISTRATOR_COMMAND,
          PersonnelRole.ADMINISTRATOR_LOGISTICS,
          PersonnelRole.ADMINISTRATOR_TRANSPORT,
          PersonnelRole.ADMINISTRATOR_HR
    };

    public JPanel createTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setName("pnlSetupTab");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(3, 6, 3, 6);

        // Row 0: Force Shape spans the full width — it's only two controls so giving it both
        // columns avoids leaving an awkward gap on the right.
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buildForceShapeSection(), gbc);

        // Row 1: left column stacks Support Personnel above Naming & Ranks; right column hosts
        // Officer Selection. Officer Selection is taller than either left section alone, so the
        // pair on the left balances it visually.
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.add(buildSupportPersonnelSection());
        leftColumn.add(Box.createVerticalStrut(6));
        leftColumn.add(buildNamingAndRanksSection());
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        panel.add(leftColumn, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        panel.add(buildOfficerSelectionSection(), gbc);

        // Row 2: Random Origin spans full width — it's a dense sub-panel with its own internal
        // layout, so giving it the whole width keeps its controls from being cramped.
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        panel.add(buildRandomOriginSection(), gbc);

        return panel;
    }

    private JPanel buildForceShapeSection() {
        CompanyGenerationStandardPanel section = new CompanyGenerationStandardPanel(
              "ForceShape", true, "ForceShape");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        chkGenerateMercenaryCompanyCommandLance =
              new CompanyGenerationCheckBox("GenerateMercenaryCompanyCommandLance");

        comboForceNamingMethod = new MMComboBox<>("comboForceNamingMethod", ForceNamingMethod.values());
        comboForceNamingMethod.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                  int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ForceNamingMethod m) {
                    // Append the first-three preview inline so the user can see what each scheme
                    // produces without hovering for the tooltip.
                    setText(m.toString() + " — " + m.getExample());
                    list.setToolTipText(m.getToolTipText());
                }
                return this;
            }
        });

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        section.add(chkGenerateMercenaryCompanyCommandLance, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        section.add(new CompanyGenerationLabel("ForceNamingMethod"), gbc);
        gbc.gridx = 1;
        section.add(comboForceNamingMethod, gbc);

        return section;
    }

    private JPanel buildSupportPersonnelSection() {
        CompanyGenerationStandardPanel section = new CompanyGenerationStandardPanel(
              "SupportPersonnel", true, "SupportPersonnel");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();

        spnSupportPersonnelNumbers.clear();
        String spinnerTooltipTemplate = getTextAt(getCompanyGenerationResourceBundle(),
              "supportPersonnelNumber.toolTipText");

        // 2-column grid (label/spinner per role). Roles are split down the middle so the right
        // column starts at the Administrator group — keeps tech-roles together on the left and
        // admin-roles together on the right.
        int halfCount = (SUPPORT_ROLES.length + 1) / 2;
        for (int i = 0; i < SUPPORT_ROLES.length; i++) {
            PersonnelRole role = SUPPORT_ROLES[i];
            String roleDisplay = role.getLabel(campaign != null && campaign.getFaction().isClan());

            JLabel roleLabel = new JLabel(roleDisplay);
            roleLabel.setName("lblSupport" + role.name());

            JSpinner spinner = new JSpinner(new SpinnerNumberModel(SUPPORT_SPINNER_MIN,
                  SUPPORT_SPINNER_MIN, SUPPORT_SPINNER_MAX, SUPPORT_SPINNER_STEP));
            spinner.setName("spnSupport" + role.name());
            if (spinnerTooltipTemplate != null && !spinnerTooltipTemplate.isEmpty()) {
                spinner.setToolTipText(String.format(spinnerTooltipTemplate, roleDisplay));
                roleLabel.setToolTipText(spinner.getToolTipText());
            }
            roleLabel.setLabelFor(spinner);
            spnSupportPersonnelNumbers.put(role, spinner);

            boolean leftColumn = i < halfCount;
            gbc.gridy = leftColumn ? i : i - halfCount;
            gbc.gridx = leftColumn ? 0 : 2;
            section.add(roleLabel, gbc);
            gbc.gridx = leftColumn ? 1 : 3;
            section.add(spinner, gbc);
        }

        chkPoolAssistants = new CompanyGenerationCheckBox("PoolAssistants");
        gbc.gridy = halfCount;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        section.add(chkPoolAssistants, gbc);

        return section;
    }

    private JPanel buildOfficerSelectionSection() {
        CompanyGenerationStandardPanel section = new CompanyGenerationStandardPanel(
              "OfficerSelection", true, "OfficerSelection");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();
        gbc.gridwidth = 1;

        chkAssignBestCompanyCommander = new CompanyGenerationCheckBox("AssignBestCompanyCommander");
        chkPrioritizeCompanyCommanderCombatSkills =
              new CompanyGenerationCheckBox("PrioritizeCompanyCommanderCombatSkills");
        indentAsSubOption(chkPrioritizeCompanyCommanderCombatSkills);
        chkAssignBestCompanyCommander.addActionListener(evt ->
              chkPrioritizeCompanyCommanderCombatSkills.setEnabled(chkAssignBestCompanyCommander.isSelected()));

        chkAssignBestOfficers = new CompanyGenerationCheckBox("AssignBestOfficers");
        chkPrioritizeOfficerCombatSkills =
              new CompanyGenerationCheckBox("PrioritizeOfficerCombatSkills");
        indentAsSubOption(chkPrioritizeOfficerCombatSkills);
        chkAssignBestOfficers.addActionListener(evt ->
              chkPrioritizeOfficerCombatSkills.setEnabled(chkAssignBestOfficers.isSelected()));

        chkAssignMostSkilledToPrimaryLances = new CompanyGenerationCheckBox("AssignMostSkilledToPrimaryLances");
        chkGenerateCaptains = new CompanyGenerationCheckBox("GenerateCaptains");
        chkAssignCompanyCommanderFlag = new CompanyGenerationCheckBox("AssignCompanyCommanderFlag");
        chkApplyOfficerStatBonusToWorstSkill =
              new CompanyGenerationCheckBox("ApplyOfficerStatBonusToWorstSkill");

        stack(section, gbc,
              chkAssignBestCompanyCommander,
              chkPrioritizeCompanyCommanderCombatSkills,
              chkAssignBestOfficers,
              chkPrioritizeOfficerCombatSkills,
              chkAssignMostSkilledToPrimaryLances,
              chkGenerateCaptains,
              chkAssignCompanyCommanderFlag,
              chkApplyOfficerStatBonusToWorstSkill);

        return section;
    }

    private JPanel buildNamingAndRanksSection() {
        CompanyGenerationStandardPanel section = new CompanyGenerationStandardPanel(
              "NamingAndRanks", true, "NamingAndRanks");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();
        gbc.gridwidth = 1;

        chkAutomaticallyAssignRanks = new CompanyGenerationCheckBox("AutomaticallyAssignRanks");
        chkUseSpecifiedFactionToAssignRanks = new CompanyGenerationCheckBox("UseSpecifiedFactionToAssignRanks");
        indentAsSubOption(chkUseSpecifiedFactionToAssignRanks);
        chkAutomaticallyAssignRanks.addActionListener(evt ->
              chkUseSpecifiedFactionToAssignRanks.setEnabled(chkAutomaticallyAssignRanks.isSelected()));
        chkAssignMekWarriorsCallSigns = new CompanyGenerationCheckBox("AssignMekWarriorsCallSigns");
        chkAssignFounderFlag = new CompanyGenerationCheckBox("AssignFounderFlag");

        stack(section, gbc,
              chkAutomaticallyAssignRanks,
              chkUseSpecifiedFactionToAssignRanks,
              chkAssignMekWarriorsCallSigns,
              chkAssignFounderFlag);

        return section;
    }

    private JPanel buildRandomOriginSection() {
        CompanyGenerationStandardPanel section = new CompanyGenerationStandardPanel(
              "RandomOrigin", true, "RandomOrigin");
        section.setLayout(new GridBagLayout());
        GridBagConstraints gbc = sectionConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        randomOriginOptionsPanel = new RandomOriginOptionsPanel(null, campaign,
              campaign == null ? null : campaign.getFaction());
        randomOriginOptionsPanel.setBorder(BorderFactory.createEmptyBorder());
        section.add(randomOriginOptionsPanel, gbc);

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

    private static void stack(JPanel section, GridBagConstraints gbc, JComponent... components) {
        gbc.gridx = 0;
        for (int i = 0; i < components.length; i++) {
            gbc.gridy = i;
            section.add(components[i], gbc);
        }
    }

    /**
     * Adds a left-margin border to a checkbox so it visually reads as a sub-option of the checkbox
     * above it. Pairs with the existing parent-toggle ActionListener that disables the sub-option
     * when the parent is unchecked.
     */
    private static void indentAsSubOption(JComponent component) {
        component.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
    }

    /**
     * Pushes values from the supplied options onto this tab's controls. Field-by-field copy; matches
     * the legacy {@code CompanyGenerationOptionsPanel.setOptions(CompanyGenerationOptions)} mapping.
     */
    public void loadValuesFromOptions(CompanyGenerationOptions sourceOptions) {
        this.options = sourceOptions;
        if (sourceOptions == null) {
            return;
        }
        chkGenerateMercenaryCompanyCommandLance.setSelected(sourceOptions.isGenerateMercenaryCompanyCommandLance());
        comboForceNamingMethod.setSelectedItem(sourceOptions.getForceNamingMethod());

        for (Map.Entry<PersonnelRole, JSpinner> entry : spnSupportPersonnelNumbers.entrySet()) {
            Integer count = sourceOptions.getSupportPersonnel().get(entry.getKey());
            entry.getValue().setValue(count == null ? 0 : count);
        }
        chkPoolAssistants.setSelected(sourceOptions.isPoolAssistants());

        chkAssignBestCompanyCommander.setSelected(sourceOptions.isAssignBestCompanyCommander());
        chkPrioritizeCompanyCommanderCombatSkills.setSelected(sourceOptions.isPrioritizeCompanyCommanderCombatSkills());
        chkPrioritizeCompanyCommanderCombatSkills.setEnabled(chkAssignBestCompanyCommander.isSelected());

        chkAssignBestOfficers.setSelected(sourceOptions.isAssignBestOfficers());
        chkPrioritizeOfficerCombatSkills.setSelected(sourceOptions.isPrioritizeOfficerCombatSkills());
        chkPrioritizeOfficerCombatSkills.setEnabled(chkAssignBestOfficers.isSelected());

        chkAssignMostSkilledToPrimaryLances.setSelected(sourceOptions.isAssignMostSkilledToPrimaryLances());
        chkGenerateCaptains.setSelected(sourceOptions.isGenerateCaptains());
        chkAssignCompanyCommanderFlag.setSelected(sourceOptions.isAssignCompanyCommanderFlag());
        chkApplyOfficerStatBonusToWorstSkill.setSelected(sourceOptions.isApplyOfficerStatBonusToWorstSkill());

        chkAutomaticallyAssignRanks.setSelected(sourceOptions.isAutomaticallyAssignRanks());
        chkUseSpecifiedFactionToAssignRanks.setSelected(sourceOptions.isUseSpecifiedFactionToAssignRanks());
        chkUseSpecifiedFactionToAssignRanks.setEnabled(chkAutomaticallyAssignRanks.isSelected());
        chkAssignMekWarriorsCallSigns.setSelected(sourceOptions.isAssignMekWarriorsCallSigns());
        chkAssignFounderFlag.setSelected(sourceOptions.isAssignFounderFlag());
    }

    /**
     * Reads values back from this tab's controls into the supplied options. Same mapping as
     * {@link #loadValuesFromOptions} in reverse.
     */
    public void writeValuesToOptions(CompanyGenerationOptions targetOptions) {
        if (targetOptions == null) {
            return;
        }
        targetOptions.setGenerateMercenaryCompanyCommandLance(chkGenerateMercenaryCompanyCommandLance.isSelected());
        Object selectedNamingMethod = comboForceNamingMethod.getSelectedItem();
        if (selectedNamingMethod instanceof ForceNamingMethod m) {
            targetOptions.setForceNamingMethod(m);
        }

        Map<PersonnelRole, Integer> supportMap = targetOptions.getSupportPersonnel();
        for (Map.Entry<PersonnelRole, JSpinner> entry : spnSupportPersonnelNumbers.entrySet()) {
            supportMap.put(entry.getKey(), (Integer) entry.getValue().getValue());
        }
        targetOptions.setPoolAssistants(chkPoolAssistants.isSelected());

        targetOptions.setAssignBestCompanyCommander(chkAssignBestCompanyCommander.isSelected());
        targetOptions.setPrioritizeCompanyCommanderCombatSkills(chkPrioritizeCompanyCommanderCombatSkills.isSelected());
        targetOptions.setAssignBestOfficers(chkAssignBestOfficers.isSelected());
        targetOptions.setPrioritizeOfficerCombatSkills(chkPrioritizeOfficerCombatSkills.isSelected());
        targetOptions.setAssignMostSkilledToPrimaryLances(chkAssignMostSkilledToPrimaryLances.isSelected());
        targetOptions.setGenerateCaptains(chkGenerateCaptains.isSelected());
        targetOptions.setAssignCompanyCommanderFlag(chkAssignCompanyCommanderFlag.isSelected());
        targetOptions.setApplyOfficerStatBonusToWorstSkill(chkApplyOfficerStatBonusToWorstSkill.isSelected());

        targetOptions.setAutomaticallyAssignRanks(chkAutomaticallyAssignRanks.isSelected());
        targetOptions.setUseSpecifiedFactionToAssignRanks(chkUseSpecifiedFactionToAssignRanks.isSelected());
        targetOptions.setAssignMekWarriorsCallSigns(chkAssignMekWarriorsCallSigns.isSelected());
        targetOptions.setAssignFounderFlag(chkAssignFounderFlag.isSelected());
    }

    public RandomOriginOptionsPanel getRandomOriginOptionsPanel() {
        return randomOriginOptionsPanel;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }
}
