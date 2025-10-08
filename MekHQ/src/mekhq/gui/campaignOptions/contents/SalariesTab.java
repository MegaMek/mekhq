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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createGroupLayout;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createParentPanel;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelRoleSubType;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsGridBagConstraints;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;
import mekhq.gui.campaignOptions.components.CampaignOptionsStandardPanel;

/**
 * The {@link SalariesTab} class represents the user interface components for configuring salary-related options in the
 * MekHQ Campaign Options dialog. This class handles the initialization, layout, and logic for various salary settings
 * spanning multiple tabs.
 */
public class SalariesTab {
    private final CampaignOptions campaignOptions;

    //start Combat Salaries Tab
    private CampaignOptionsHeaderPanel combatSalariesHeader;
    private JCheckBox chkDisableSecondaryRoleSalary;

    private JPanel pnlSalaryMultipliersPanel;
    private JLabel lblAntiMekSalary;
    private JSpinner spnAntiMekSalary;
    private JLabel lblSpecialistInfantrySalary;
    private JSpinner spnSpecialistInfantrySalary;

    private JPanel pnlSalaryExperienceMultipliersPanel;
    private Map<SkillLevel, JLabel> lblSalaryExperienceMultipliers;
    private Map<SkillLevel, JSpinner> spnSalaryExperienceMultipliers;

    private JPanel pnlSalaryBaseSalaryPanel;

    private List<PersonnelRole> combatRoles;
    private JLabel[] lblBaseSalaryCombat;
    private JSpinner[] spnBaseSalaryCombat;

    private List<PersonnelRole> supportRoles;
    private JLabel[] lblBaseSalarySupport;
    private JSpinner[] spnBaseSalarySupport;

    private List<PersonnelRole> civilianRoles;
    private JLabel[] lblBaseSalaryCivilian;
    private JSpinner[] spnBaseSalaryCivilian;
    //end Salaries Tab

    /**
     * Constructs the {@code PersonnelTab} object with the given campaign options.
     *
     * @param campaignOptions the {@link CampaignOptions} instance to be used for initializing and managing personnel
     *                        options.
     */
    public SalariesTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Initializes all tabs and their components within the PersonnelTab.
     */
    private void initialize() {
        chkDisableSecondaryRoleSalary = new JCheckBox();

        pnlSalaryMultipliersPanel = new JPanel();

        lblAntiMekSalary = new JLabel();
        spnAntiMekSalary = new JSpinner();

        lblSpecialistInfantrySalary = new JLabel();
        spnSpecialistInfantrySalary = new JSpinner();

        pnlSalaryExperienceMultipliersPanel = new JPanel();
        lblSalaryExperienceMultipliers = new HashMap<>();
        spnSalaryExperienceMultipliers = new HashMap<>();

        pnlSalaryBaseSalaryPanel = new JPanel();

        combatRoles = PersonnelRole.getCombatRoles();
        combatRoles.sort(Comparator.comparing(role -> role.getLabel(false)));
        lblBaseSalaryCombat = new JLabel[combatRoles.size()];
        spnBaseSalaryCombat = new JSpinner[combatRoles.size()];

        supportRoles = PersonnelRole.getSupportRoles();
        supportRoles.sort(Comparator.comparing(role -> role.getLabel(false)));
        lblBaseSalarySupport = new JLabel[supportRoles.size()];
        spnBaseSalarySupport = new JSpinner[supportRoles.size()];

        civilianRoles = PersonnelRole.getCivilianRoles();
        civilianRoles.sort(Comparator.comparing(role -> role.getLabel(false)));
        civilianRoles.remove(PersonnelRole.NONE);
        civilianRoles.add(0, PersonnelRole.NONE);
        civilianRoles.remove(PersonnelRole.DEPENDENT);
        civilianRoles.add(0, PersonnelRole.DEPENDENT);
        lblBaseSalaryCivilian = new JLabel[civilianRoles.size()];
        spnBaseSalaryCivilian = new JSpinner[civilianRoles.size()];
    }

    /**
     * Creates the layout for the Salaries Tab, including components for salary multipliers and base salary settings.
     *
     * @return a {@link JPanel} representing the Salaries Tab.
     */
    public JPanel createSalariesTab(PersonnelRoleSubType type) {
        // Header
        combatSalariesHeader = switch (type) {
            case COMBAT ->
                  new CampaignOptionsHeaderPanel("CombatSalariesTab", getImageDirectory() + "logo_clan_coyote.png", 2);
            case SUPPORT ->
                  new CampaignOptionsHeaderPanel("SupportSalariesTab", getImageDirectory() + "logo_clan_coyote.png", 2);
            case CIVILIAN ->
                  new CampaignOptionsHeaderPanel("CivilianSalariesTab", getImageDirectory() + "logo_clan_coyote.png");
        };

        // Contents
        if (type == PersonnelRoleSubType.COMBAT) {
            chkDisableSecondaryRoleSalary = new CampaignOptionsCheckBox("DisableSecondaryRoleSalary");
            chkDisableSecondaryRoleSalary.addMouseListener(createTipPanelUpdater(combatSalariesHeader,
                  "DisableSecondaryRoleSalary"));
            pnlSalaryMultipliersPanel = createSalaryMultipliersPanel();
            pnlSalaryExperienceMultipliersPanel = createExperienceMultipliersPanel();
        }

        pnlSalaryBaseSalaryPanel = createBaseSalariesPanel(type);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SalariesTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(combatSalariesHeader, layout);

        if (type == PersonnelRoleSubType.COMBAT) {
            layout.gridx = 0;
            layout.gridy++;
            layout.gridwidth = 1;
            panel.add(chkDisableSecondaryRoleSalary, layout);

            layout.gridy++;
            panel.add(pnlSalaryMultipliersPanel, layout);
            layout.gridx++;
            panel.add(pnlSalaryExperienceMultipliersPanel, layout);
        }

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(pnlSalaryBaseSalaryPanel, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "SalariesTab");
    }

    /**
     * Creates the panel for configuring salary multipliers for specific roles in the Salaries Tab.
     *
     * @return a {@link JPanel} containing salary multiplier options.
     */
    private JPanel createSalaryMultipliersPanel() {
        // Contents
        lblAntiMekSalary = new CampaignOptionsLabel("AntiMekSalary");
        lblAntiMekSalary.addMouseListener(createTipPanelUpdater(combatSalariesHeader, "AntiMekSalary"));
        spnAntiMekSalary = new CampaignOptionsSpinner("AntiMekSalary", 0, 0, 100, 0.01);
        spnAntiMekSalary.addMouseListener(createTipPanelUpdater(combatSalariesHeader, "AntiMekSalary"));

        lblSpecialistInfantrySalary = new CampaignOptionsLabel("SpecialistInfantrySalary");
        lblSpecialistInfantrySalary.addMouseListener(createTipPanelUpdater(combatSalariesHeader,
              "SpecialistInfantrySalary"));
        spnSpecialistInfantrySalary = new CampaignOptionsSpinner("SpecialistInfantrySalary", 0, 0, 100, 0.01);
        spnSpecialistInfantrySalary.addMouseListener(createTipPanelUpdater(combatSalariesHeader,
              "SpecialistInfantrySalary"));

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SalaryMultipliersPanel", true, "SalaryMultipliersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblAntiMekSalary, layout);
        layout.gridx++;
        panel.add(spnAntiMekSalary, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblSpecialistInfantrySalary, layout);
        layout.gridx++;
        panel.add(spnSpecialistInfantrySalary, layout);

        return panel;
    }

    /**
     * Creates the panel for configuring experience multipliers based on skill levels in the Salaries Tab.
     *
     * @return a {@link JPanel} containing settings for skill-based experience multipliers.
     */
    private JPanel createExperienceMultipliersPanel() {
        // Contents
        SkillLevel[] skillLevels = Skills.SKILL_LEVELS;

        for (final SkillLevel skillLevel : skillLevels) {
            final JLabel label = new CampaignOptionsLabel("SkillLevel" + skillLevel.toString(), null, true);
            label.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lblSkillLevelMultiplier.tooltip"));
            label.addMouseListener(createTipPanelUpdater(combatSalariesHeader, "SkillLevelMultiplier"));
            lblSalaryExperienceMultipliers.put(skillLevel, label);

            final JSpinner spinner = new CampaignOptionsSpinner("SkillLevel" + skillLevel, null, 0, 0, 100, 0.1, true);
            spinner.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lblSkillLevelMultiplier.tooltip"));
            spinner.addMouseListener(createTipPanelUpdater(combatSalariesHeader, "SkillLevelMultiplier"));
            spnSalaryExperienceMultipliers.put(skillLevel, spinner);
        }

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ExperienceMultipliersPanel",
              true,
              "ExperienceMultipliersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblSalaryExperienceMultipliers.get(skillLevels[0]), layout);
        layout.gridx++;
        panel.add(spnSalaryExperienceMultipliers.get(skillLevels[0]), layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblSalaryExperienceMultipliers.get(skillLevels[4]), layout);
        layout.gridx++;
        panel.add(spnSalaryExperienceMultipliers.get(skillLevels[4]), layout);

        // new column

        layout.gridx = 2;
        layout.gridy = 0;
        panel.add(lblSalaryExperienceMultipliers.get(skillLevels[1]), layout);
        layout.gridx++;
        panel.add(spnSalaryExperienceMultipliers.get(skillLevels[1]), layout);

        layout.gridx = 2;
        layout.gridy++;
        panel.add(lblSalaryExperienceMultipliers.get(skillLevels[5]), layout);
        layout.gridx++;
        panel.add(spnSalaryExperienceMultipliers.get(skillLevels[5]), layout);

        // new column

        layout.gridx = 4;
        layout.gridy = 0;
        panel.add(lblSalaryExperienceMultipliers.get(skillLevels[2]), layout);
        layout.gridx++;
        panel.add(spnSalaryExperienceMultipliers.get(skillLevels[2]), layout);

        layout.gridx = 4;
        layout.gridy++;
        panel.add(lblSalaryExperienceMultipliers.get(skillLevels[6]), layout);
        layout.gridx++;
        panel.add(spnSalaryExperienceMultipliers.get(skillLevels[6]), layout);

        // new column

        layout.gridx = 6;
        layout.gridy = 0;
        panel.add(lblSalaryExperienceMultipliers.get(skillLevels[3]), layout);
        layout.gridx++;
        panel.add(spnSalaryExperienceMultipliers.get(skillLevels[3]), layout);

        layout.gridx = 6;
        layout.gridy++;
        panel.add(lblSalaryExperienceMultipliers.get(skillLevels[7]), layout);
        layout.gridx++;
        panel.add(spnSalaryExperienceMultipliers.get(skillLevels[7]), layout);

        return panel;
    }

    /**
     * Creates the panel for configuring base salaries for various personnel roles in the Salaries Tab.
     *
     * @return a {@link JPanel} containing settings for base salaries.
     */
    private JPanel createBaseSalariesPanel(PersonnelRoleSubType type) {
        List<PersonnelRole> roles = switch (type) {
            case COMBAT -> combatRoles;
            case SUPPORT -> supportRoles;
            case CIVILIAN -> civilianRoles;
        };
        JLabel[] trackingLabel = switch (type) {
            case COMBAT -> lblBaseSalaryCombat;
            case SUPPORT -> lblBaseSalarySupport;
            case CIVILIAN -> lblBaseSalaryCivilian;
        };
        JSpinner[] trackingSpinner = switch (type) {
            case COMBAT -> spnBaseSalaryCombat;
            case SUPPORT -> spnBaseSalarySupport;
            case CIVILIAN -> spnBaseSalaryCivilian;
        };

        // Contents
        for (final PersonnelRole personnelRole : roles) {
            String componentName = personnelRole.toString().replaceAll(" ", "");

            // JLabel
            JLabel jLabel = new JLabel(personnelRole.toString());
            jLabel.setToolTipText(personnelRole.getDescription(false));
            jLabel.addMouseListener(createTipPanelUpdater(combatSalariesHeader,
                  null,
                  personnelRole.getDescription(false)));
            jLabel.setName("lbl" + componentName);

            Dimension labelSize = jLabel.getPreferredSize();
            jLabel.setMinimumSize(UIUtil.scaleForGUI(labelSize.width, labelSize.height));

            // JSpinner
            JSpinner jSpinner = new JSpinner();
            jSpinner.setToolTipText(personnelRole.getDescription(false));
            jSpinner.addMouseListener(createTipPanelUpdater(combatSalariesHeader,
                  null,
                  personnelRole.getDescription(false)));
            jSpinner.setModel(new SpinnerNumberModel(250.0, 0.0, 1000000, 10.0));
            jSpinner.setName("spn" + componentName);

            DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);

            Dimension spinnerSize = jSpinner.getPreferredSize();
            jSpinner.setMinimumSize(UIUtil.scaleForGUI(spinnerSize.width, spinnerSize.height));

            // Component Tracking Assignment
            int index = roles.indexOf(personnelRole);
            switch (type) {
                case COMBAT -> {
                    lblBaseSalaryCombat[index] = jLabel;
                    spnBaseSalaryCombat[index] = jSpinner;
                }
                case SUPPORT -> {
                    lblBaseSalarySupport[index] = jLabel;
                    spnBaseSalarySupport[index] = jSpinner;
                }
                case CIVILIAN -> {
                    lblBaseSalaryCivilian[index] = jLabel;
                    spnBaseSalaryCivilian[index] = jSpinner;
                }
            }
        }

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("BaseSalariesPanel", true, "BaseSalariesPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        SequentialGroup mainHorizontalGroup = layout.createSequentialGroup();
        SequentialGroup mainVerticalGroup = layout.createSequentialGroup();

        int columns = 3;
        int rows = (int) Math.ceil((double) trackingLabel.length / columns);

        // Create an array to store ParallelGroups for each column
        ParallelGroup[] columnGroups = new ParallelGroup[columns];
        for (int i = 0; i < columns; i++) {
            columnGroups[i] = layout.createParallelGroup();
        }

        for (int j = 0; j < rows; j++) {
            ParallelGroup verticalGroup = layout.createParallelGroup(Alignment.BASELINE);

            for (int i = 0; i < columns; i++) {
                int index = i * rows + j;

                if (index < trackingLabel.length) {
                    // Create a SequentialGroup for the label and spinner
                    SequentialGroup horizontalSequentialGroup = layout.createSequentialGroup();

                    horizontalSequentialGroup.addComponent(trackingLabel[index]);
                    horizontalSequentialGroup.addComponent(trackingSpinner[index]);
                    if (i != (columns - 1)) {
                        horizontalSequentialGroup.addGap(10);
                    }

                    // Add the SequentialGroup to the column's ParallelGroup
                    columnGroups[i].addGroup(horizontalSequentialGroup);

                    verticalGroup.addComponent(trackingLabel[index]);
                    verticalGroup.addComponent(trackingSpinner[index]);
                }
            }
            mainVerticalGroup.addGroup(verticalGroup);
        }
        for (ParallelGroup columnGroup : columnGroups) {
            mainHorizontalGroup.addGroup(columnGroup);
        }

        layout.setHorizontalGroup(mainHorizontalGroup);
        layout.setVerticalGroup(mainVerticalGroup);

        return panel;
    }

    /**
     * Shortcut method to load default {@link CampaignOptions} values into the tab components.
     */
    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    /**
     * Loads and applies configuration values from the provided {@link CampaignOptions} object, or uses the default
     * campaign options if none are provided. The configuration includes general settings, personnel logs, personnel
     * information, awards, medical settings, prisoner and dependent settings, and salary-related options. It also
     * adjusts certain values based on the version of the application.
     *
     * @param presetCampaignOptions the {@link CampaignOptions} object to load settings from. If null, default campaign
     *                              options will be used.
     */
    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Salaries
        chkDisableSecondaryRoleSalary.setSelected(options.isDisableSecondaryRoleSalary());
        spnAntiMekSalary.setValue(options.getSalaryAntiMekMultiplier());
        spnSpecialistInfantrySalary.setValue(options.getSalarySpecialistInfantryMultiplier());
        for (final Entry<SkillLevel, JSpinner> entry : spnSalaryExperienceMultipliers.entrySet()) {
            entry.getValue().setValue(options.getSalaryXPMultipliers().get(entry.getKey()));
        }

        Money[] baseSalaryTable = options.getRoleBaseSalaries();
        for (int i = 0; i < spnBaseSalaryCombat.length; i++) {
            PersonnelRole personnelRole = combatRoles.get(i);
            int ordinal = personnelRole.ordinal();
            spnBaseSalaryCombat[i].setValue(baseSalaryTable[ordinal].getAmount().doubleValue());
        }
        for (int i = 0; i < spnBaseSalarySupport.length; i++) {
            PersonnelRole personnelRole = supportRoles.get(i);
            int ordinal = personnelRole.ordinal();
            spnBaseSalarySupport[i].setValue(baseSalaryTable[ordinal].getAmount().doubleValue());
        }
        for (int i = 0; i < spnBaseSalaryCivilian.length; i++) {
            PersonnelRole personnelRole = civilianRoles.get(i);
            int ordinal = personnelRole.ordinal();
            spnBaseSalaryCivilian[i].setValue(baseSalaryTable[ordinal].getAmount().doubleValue());
        }
    }

    /**
     * Applies the modified salary tab settings to the repository's campaign options. If no preset
     * {@link CampaignOptions} is provided, the changes are applied to the current options.
     *
     * @param presetCampaignOptions optional custom {@link CampaignOptions} to apply changes to.
     */
    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // Salaries
        options.setDisableSecondaryRoleSalary(chkDisableSecondaryRoleSalary.isSelected());
        options.setSalaryAntiMekMultiplier((double) spnAntiMekSalary.getValue());
        options.setSalarySpecialistInfantryMultiplier((double) spnSpecialistInfantrySalary.getValue());

        for (final Entry<SkillLevel, JSpinner> entry : spnSalaryExperienceMultipliers.entrySet()) {
            options.getSalaryXPMultipliers().put(entry.getKey(), (double) entry.getValue().getValue());
        }

        for (PersonnelRole personnelRole : combatRoles) {
            int index = combatRoles.indexOf(personnelRole);
            double newValue = (double) spnBaseSalaryCombat[index].getValue();
            options.setRoleBaseSalary(personnelRole, newValue);
        }

        for (PersonnelRole personnelRole : supportRoles) {
            int index = supportRoles.indexOf(personnelRole);
            if (index != -1) {
                double newValue = (double) spnBaseSalarySupport[index].getValue();
                options.setRoleBaseSalary(personnelRole, newValue);
            }
        }

        for (PersonnelRole personnelRole : civilianRoles) {
            int index = civilianRoles.indexOf(personnelRole);
            if (index != -1) {
                double newValue = (double) spnBaseSalaryCivilian[index].getValue();
                options.setRoleBaseSalary(personnelRole, newValue);
            }
        }
    }
}
