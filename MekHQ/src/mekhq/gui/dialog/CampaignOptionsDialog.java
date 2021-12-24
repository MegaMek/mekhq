/*
 * Copyright (C) 2009-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.CampaignPreset;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.market.PersonnelMarketDylan;
import mekhq.campaign.market.PersonnelMarketRandom;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.rating.UnitRatingMethod;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.RATManager;
import mekhq.gui.FileDialogs;
import mekhq.gui.SpecialAbilityPanel;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.baseComponents.JDisableablePanel;
import mekhq.gui.dialog.iconDialogs.UnitIconDialog;
import mekhq.gui.displayWrappers.FactionDisplay;
import mekhq.gui.panes.RankSystemsPane;
import mekhq.module.PersonnelMarketServiceManager;
import mekhq.module.api.PersonnelMarketMethod;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class CampaignOptionsDialog extends AbstractMHQButtonDialog {

    //region Constructors
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign, final boolean startup) {
        super(frame, true, ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog", new EncodeControl()),
                "CampaignOptionsDialog", "CampaignOptionsDialog.title");
        initialize();
        setOptions(campaign.getCampaignOptions(), campaign.getRandomSkillPreferences());
    }
    //endregion Constructors

    //region Initialization
    //region Center Pane
    @Override
    protected Container createCenterPane() {
        //region Variable Declaration and Initialisation
        comboFactionNames = new JComboBox<>();
        sldGender = new JSlider(SwingConstants.HORIZONTAL);
        panRepair = new JPanel();
        panSupplies = new JPanel();
        panMercenary = new JPanel();
        panNameGen = new JPanel();
        panXP = new JPanel();
        panSkill = new JPanel();
        panTech = new JPanel();
        panRandomSkill = new JPanel();
        panRandomPortrait = new JPanel();
        useEraModsCheckBox = new JCheckBox();
        assignedTechFirstCheckBox = new JCheckBox();
        resetToFirstTechCheckBox = new JCheckBox();
        payForPartsBox = new JCheckBox();
        payForRepairsBox = new JCheckBox();
        payForUnitsBox = new JCheckBox();
        payForSalariesBox = new JCheckBox();
        payForRecruitmentBox = new JCheckBox();
        useLoanLimitsBox = new JCheckBox();
        payForOverheadBox = new JCheckBox();
        payForMaintainBox = new JCheckBox();
        payForTransportBox = new JCheckBox();
        usePeacetimeCostBox = new JCheckBox();
        useExtendedPartsModifierBox = new JCheckBox();
        showPeacetimeCostBox = new JCheckBox();
        chkAssignPortraitOnRoleChange = new JCheckBox();
        sellUnitsBox = new JCheckBox();
        sellPartsBox = new JCheckBox();
        useQuirksBox = new JCheckBox();
        limitByYearBox = new JCheckBox();
        disallowExtinctStuffBox = new JCheckBox();
        allowClanPurchasesBox = new JCheckBox();
        allowISPurchasesBox = new JCheckBox();
        allowCanonOnlyBox = new JCheckBox();
        allowCanonRefitOnlyBox = new JCheckBox();
        variableTechLevelBox = new JCheckBox();
        factionIntroDateBox = new JCheckBox();
        useAmmoByTypeBox = new JCheckBox();
        choiceTechLevel = new JComboBox<>();

        usePlanetaryAcquisitions = new JCheckBox();
        usePlanetaryAcquisitionsVerbose = new JCheckBox();
        disallowPlanetaryAcquisitionClanCrossover = new JCheckBox();
        disallowClanPartsFromIS = new JCheckBox();
        useDamageMargin = new JCheckBox();
        useAeroSystemHitsBox = new JCheckBox();
        useQualityMaintenance = new JCheckBox();
        useUnofficialMaintenance = new JCheckBox();
        checkMaintenance = new JCheckBox();
        reverseQualityNames = new JCheckBox();

        chkSupportStaffOnly = new JCheckBox();

        GridBagConstraints gridBagConstraints;
        int gridy = 0;
        int gridx = 0;

        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        //endregion Variable Declaration and Initialisation

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog", new EncodeControl());

        setOptionsPane(new JTabbedPane());
        getOptionsPane().setName("optionsPane");

        //region General Tab
        panGeneral = new JPanel(new GridBagLayout());
        panGeneral.setName("panGeneral");

        JLabel lblName = new JLabel(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panGeneral.add(lblName, gridBagConstraints);

        txtName = new JTextField(campaign.getName());
        txtName.setName("txtName");
        txtName.setMinimumSize(new Dimension(500, 30));
        txtName.setPreferredSize(new Dimension(500, 30));
        gridBagConstraints.gridx = gridx--;
        panGeneral.add(txtName, gridBagConstraints);

        JLabel lblFaction = new JLabel(resourceMap.getString("lblFaction.text"));
        lblFaction.setName("lblFaction");
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        panGeneral.add(lblFaction, gridBagConstraints);

        final DefaultComboBoxModel<FactionDisplay> factionModel = new DefaultComboBoxModel<>();
        factionModel.addAll(FactionDisplay
                .getSortedValidFactionDisplays(Factions.getInstance().getChoosableFactions(), date));
        comboFaction = new MMComboBox<>("comboFaction", factionModel);
        comboFaction.setSelectedItem(new FactionDisplay(campaign.getFaction(), date));
        comboFaction.setMinimumSize(new Dimension(400, 30));
        comboFaction.setPreferredSize(new Dimension(400, 30));
        gridBagConstraints.gridx = gridx--;
        panGeneral.add(comboFaction, gridBagConstraints);

        JPanel unitRatingPanel = new JPanel(new GridBagLayout());

        JLabel unitRatingMethodLabel = new JLabel(resourceMap.getString("unitRatingMethodLabel.text"));
        unitRatingMethodLabel.setName("unitRatingMethodLabel");
        gridBagConstraints.gridx = 0;
        unitRatingPanel.add(unitRatingMethodLabel, gridBagConstraints);

        unitRatingMethodCombo = new JComboBox<>(UnitRatingMethod.values());
        unitRatingMethodCombo.setName("unitRatingMethodCombo");
        gridBagConstraints.gridx = 1;
        unitRatingPanel.add(unitRatingMethodCombo, gridBagConstraints);

        JLabel manualUnitRatingModifierLabel = new JLabel(resourceMap.getString("manualUnitRatingModifierLabel.text"));
        gridBagConstraints.gridx = 2;
        unitRatingPanel.add(manualUnitRatingModifierLabel, gridBagConstraints);

        manualUnitRatingModifier = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
        gridBagConstraints.gridx = 3;
        unitRatingPanel.add(manualUnitRatingModifier, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panGeneral.add(unitRatingPanel, gridBagConstraints);

        JLabel lblDate = new JLabel(resourceMap.getString("lblDate.text"));
        lblDate.setName("lblDate");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panGeneral.add(lblDate, gridBagConstraints);

        btnDate = new JButton(MekHQ.getMekHQOptions().getDisplayFormattedDate(date));
        btnDate.setName("btnDate");
        btnDate.setMinimumSize(new Dimension(400, 30));
        btnDate.setPreferredSize(new Dimension(400, 30));
        btnDate.addActionListener(this::btnDateActionPerformed);
        gridBagConstraints.gridx = gridx--;
        panGeneral.add(btnDate, gridBagConstraints);

        JLabel lblCamo = new JLabel(resourceMap.getString("lblCamo.text"));
        lblCamo.setName("lblCamo");
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        panGeneral.add(lblCamo, gridBagConstraints);

        btnCamo = new JButton();
        btnCamo.setName("btnCamo");
        btnCamo.setMinimumSize(new Dimension(84, 72));
        btnCamo.setPreferredSize(new Dimension(84, 72));
        btnCamo.setMaximumSize(new Dimension(84, 72));
        btnCamo.addActionListener(this::btnCamoActionPerformed);
        gridBagConstraints.gridx = gridx--;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panGeneral.add(btnCamo, gridBagConstraints);

        JLabel lblIcon = new JLabel(resourceMap.getString("lblIcon.text"));
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panGeneral.add(lblIcon, gridBagConstraints);

        btnIcon = new JButton();
        btnIcon.setMinimumSize(new Dimension(84, 72));
        btnIcon.setPreferredSize(new Dimension(84, 72));
        btnIcon.setMaximumSize(new Dimension(84, 72));
        btnIcon.addActionListener(evt -> {
            final UnitIconDialog unitIconDialog = new UnitIconDialog(getFrame(), unitIcon);
            if (unitIconDialog.showDialog().isConfirmed() && (unitIconDialog.getSelectedItem() != null)) {
                unitIcon = unitIconDialog.getSelectedItem();
                btnIcon.setIcon(unitIcon.getImageIcon(75));
            }
        });
        gridBagConstraints.gridx = gridx--;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panGeneral.add(btnIcon, gridBagConstraints);

        getOptionsPane().addTab(resourceMap.getString("panGeneral.TabConstraints.tabTitle"), panGeneral);
        //endregion General Tab

        //region Repair and Maintenance Tab
        panRepair.setName("panRepair");
        panRepair.setLayout(new java.awt.GridBagLayout());

        JPanel panSubRepair = new JPanel(new GridBagLayout());
        JPanel panSubMaintenance = new JPanel(new GridBagLayout());

        panSubRepair.setBorder(BorderFactory.createTitledBorder("Repair"));
        panSubMaintenance.setBorder(BorderFactory.createTitledBorder("Maintenance"));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panRepair.add(panSubRepair, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        panRepair.add(panSubMaintenance, gridBagConstraints);

        useEraModsCheckBox.setText(resourceMap.getString("useEraModsCheckBox.text")); // NOI18N
        useEraModsCheckBox.setToolTipText(resourceMap.getString("useEraModsCheckBox.toolTipText")); // NOI18N
        useEraModsCheckBox.setName("useEraModsCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useEraModsCheckBox, gridBagConstraints);

        assignedTechFirstCheckBox.setText(resourceMap.getString("assignedTechFirstCheckBox.text")); // NOI18N
        assignedTechFirstCheckBox.setToolTipText(resourceMap.getString("assignedTechFirstCheckBox.toolTipText")); // NOI18N
        assignedTechFirstCheckBox.setName("assignedTechFirstCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(assignedTechFirstCheckBox, gridBagConstraints);

        resetToFirstTechCheckBox.setText(resourceMap.getString("resetToFirstTechCheckBox.text")); // NOI18N
        resetToFirstTechCheckBox.setToolTipText(resourceMap.getString("resetToFirstTechCheckBox.toolTipText")); // NOI18N
        resetToFirstTechCheckBox.setName("resetToFirstTechCheckBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(resetToFirstTechCheckBox, gridBagConstraints);

        useQuirksBox.setText(resourceMap.getString("useQuirksBox.text")); // NOI18N
        useQuirksBox.setToolTipText(resourceMap.getString("useQuirksBox.toolTipText")); // NOI18N
        useQuirksBox.setName("useQuirksBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useQuirksBox, gridBagConstraints);

        useAeroSystemHitsBox.setText(resourceMap.getString("useAeroSystemHits.text")); // NOI18N
        useAeroSystemHitsBox.setToolTipText(resourceMap.getString("useAeroSystemHits.toolTipText")); // NOI18N
        useAeroSystemHitsBox.setName("useAeroSystemHits"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useAeroSystemHitsBox, gridBagConstraints);

        useDamageMargin.setText(resourceMap.getString("useDamageMargin.text"));
        useDamageMargin.setToolTipText(resourceMap.getString("useDamageMargin.toolTipText"));
        useDamageMargin.addActionListener(evt -> spnDamageMargin.setEnabled(useDamageMargin.isSelected()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(useDamageMargin, gridBagConstraints);

        spnDamageMargin = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        ((JSpinner.DefaultEditor) spnDamageMargin.getEditor()).getTextField().setEditable(false);
        JPanel pnlDamageMargin = new JPanel();
        pnlDamageMargin.add(new JLabel("Margin:"));
        pnlDamageMargin.add(spnDamageMargin);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(pnlDamageMargin, gridBagConstraints);

        spnDestroyPartTarget = new JSpinner(new SpinnerNumberModel(2, 2, 13, 1));
        ((JSpinner.DefaultEditor) spnDestroyPartTarget.getEditor()).getTextField().setEditable(false);

        JPanel pnlDestroyPartTarget = new JPanel();
        pnlDestroyPartTarget.add(new JLabel("Equipment hit in combat survives on a roll of"));
        pnlDestroyPartTarget.add(spnDestroyPartTarget);
        pnlDestroyPartTarget.add(new JLabel("or better"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubRepair.add(pnlDestroyPartTarget, gridBagConstraints);

        checkMaintenance.setText(resourceMap.getString("checkMaintenance.text"));
        checkMaintenance.setToolTipText(resourceMap.getString("checkMaintenance.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(checkMaintenance, gridBagConstraints);

        checkMaintenance.addActionListener(evt -> {
            if (checkMaintenance.isSelected()) {
                spnMaintenanceDays.setEnabled(true);
                useQualityMaintenance.setEnabled(true);
                useUnofficialMaintenance.setEnabled(true);
                reverseQualityNames.setEnabled(true);
                spnMaintenanceBonus.setEnabled(true);
                logMaintenance.setEnabled(true);
            } else {
                spnMaintenanceDays.setEnabled(false);
                useQualityMaintenance.setEnabled(false);
                useUnofficialMaintenance.setEnabled(false);
                reverseQualityNames.setEnabled(false);
                spnMaintenanceBonus.setEnabled(false);
                logMaintenance.setEnabled(false);
            }
        });

        spnMaintenanceDays = new JSpinner(new SpinnerNumberModel(30, 1, 365, 1));
        ((JSpinner.DefaultEditor) spnMaintenanceDays.getEditor()).getTextField().setEditable(false);
        JPanel pnlMaintenanceDays = new JPanel();
        pnlMaintenanceDays.add(spnMaintenanceDays);
        pnlMaintenanceDays.add(new JLabel("Maintenance cycle length in days"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlMaintenanceDays, gridBagConstraints);

        spnMaintenanceBonus = new JSpinner(new SpinnerNumberModel(0, -13, 13, 1));
        ((JSpinner.DefaultEditor) spnMaintenanceBonus.getEditor()).getTextField().setEditable(false);
        spnMaintenanceBonus.setToolTipText(resourceMap.getString("spnMaintenanceBonus.toolTipText"));

        JPanel pnlMaintenanceBonus = new JPanel();
        pnlMaintenanceBonus.add(spnMaintenanceBonus);
        pnlMaintenanceBonus.add(new JLabel("Maintenance modifier"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(pnlMaintenanceBonus, gridBagConstraints);

        useQualityMaintenance.setText(resourceMap.getString("useQualityMaintenance.text"));
        useQualityMaintenance.setToolTipText(resourceMap.getString("useQualityMaintenance.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useQualityMaintenance, gridBagConstraints);

        reverseQualityNames.setText(resourceMap.getString("reverseQualityNames.text"));
        reverseQualityNames.setToolTipText(resourceMap.getString("reverseQualityNames.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(reverseQualityNames, gridBagConstraints);


        useUnofficialMaintenance.setText(resourceMap.getString("useUnofficialMaintenance.text"));
        useUnofficialMaintenance.setToolTipText(resourceMap.getString("useUnofficialMaintenance.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubMaintenance.add(useUnofficialMaintenance, gridBagConstraints);

        logMaintenance = new JCheckBox(resourceMap.getString("logMaintenance.text"));
        logMaintenance.setToolTipText(resourceMap.getString("logMaintenance.toolTipText"));
        logMaintenance.setName("logMaintenance");
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panSubMaintenance.add(logMaintenance, gridBagConstraints);

        getOptionsPane().addTab(resourceMap.getString("panRepair.TabConstraints.tabTitle"), panRepair);
        //endregion Repair and Maintenance Tab

        //region Supplies and Acquisition Tab
        panSupplies.setName("panSupplies");
        panSupplies.setLayout(new GridBagLayout());

        JPanel panSubAcquire = new JPanel(new GridBagLayout());
        JPanel panSubDelivery = new JPanel(new GridBagLayout());
        JPanel panSubPlanetAcquire = new JPanel(new GridBagLayout());

        panSubAcquire.setBorder(BorderFactory.createTitledBorder("Acquisition"));
        panSubDelivery.setBorder(BorderFactory.createTitledBorder("Delivery"));
        panSubPlanetAcquire.setBorder(BorderFactory.createTitledBorder("Planetary Acquisition"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = .5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panSupplies.add(panSubAcquire, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = .5;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panSupplies.add(panSubDelivery, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panSupplies.add(panSubPlanetAcquire, gridBagConstraints);

        spnAcquireWaitingPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        ((JSpinner.DefaultEditor) spnAcquireWaitingPeriod.getEditor()).getTextField().setEditable(false);

        JPanel pnlWaitingPeriod = new JPanel();
        pnlWaitingPeriod.add(spnAcquireWaitingPeriod);
        pnlWaitingPeriod.add(new JLabel("Waiting period (in days) between acquisition rolls"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlWaitingPeriod, gridBagConstraints);

        DefaultComboBoxModel<String> acquireSkillModel = new DefaultComboBoxModel<>();
        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_SCROUNGE);
        acquireSkillModel.addElement(SkillType.S_NEG);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);
        choiceAcquireSkill = new JComboBox<>(acquireSkillModel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panSubAcquire.add(new JLabel("Acquisition Skill:"), gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panSubAcquire.add(choiceAcquireSkill, gridBagConstraints);

        chkSupportStaffOnly.setText("Only support personnel can make acquisition checks");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(chkSupportStaffOnly, gridBagConstraints);

        spnAcquireClanPenalty = new JSpinner(new SpinnerNumberModel(0, 0, 13, 1));
        ((JSpinner.DefaultEditor) spnAcquireClanPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlClanPenalty = new JPanel();
        pnlClanPenalty.add(spnAcquireClanPenalty);
        pnlClanPenalty.add(new JLabel("Penalty for Clan equipment"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlClanPenalty, gridBagConstraints);

        spnAcquireIsPenalty = new JSpinner(new SpinnerNumberModel(0, 0, 13, 1));
        ((JSpinner.DefaultEditor) spnAcquireIsPenalty.getEditor()).getTextField().setEditable(false);

        JPanel pnlIsPenalty = new JPanel();
        pnlIsPenalty.add(spnAcquireIsPenalty);
        pnlIsPenalty.add(new JLabel("Penalty for Inner Sphere equipment"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlIsPenalty, gridBagConstraints);

        txtMaxAcquisitions = new JTextField(4);
        txtMaxAcquisitions.setHorizontalAlignment(JTextField.RIGHT);
        txtMaxAcquisitions.setName("txtName");

        JPanel pnlMaxAcquisitions = new JPanel();
        pnlMaxAcquisitions.add(txtMaxAcquisitions);
        pnlMaxAcquisitions.add(new JLabel("Maximum Acquisitions Per Day (0 for unlimited)"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAcquire.add(pnlMaxAcquisitions, gridBagConstraints);

        spnNDiceTransitTime = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((JSpinner.DefaultEditor) spnNDiceTransitTime.getEditor()).getTextField().setEditable(false);

        spnConstantTransitTime = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((JSpinner.DefaultEditor) spnConstantTransitTime.getEditor()).getTextField().setEditable(false);

        spnAcquireMosBonus = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((JSpinner.DefaultEditor) spnAcquireMosBonus.getEditor()).getTextField().setEditable(false);

        spnAcquireMinimum = new JSpinner(new SpinnerNumberModel(0, 0, 365, 1));
        ((JSpinner.DefaultEditor) spnAcquireMinimum.getEditor()).getTextField().setEditable(false);

        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        choiceTransitTimeUnits = new JComboBox<>(transitUnitModel);

        DefaultComboBoxModel<String> transitMosUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMosUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        choiceAcquireMosUnits = new JComboBox<>(transitMosUnitModel);

        DefaultComboBoxModel<String> transitMinUnitModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < CampaignOptions.TRANSIT_UNIT_NUM; i++) {
            transitMinUnitModel.addElement(CampaignOptions.getTransitUnitName(i));
        }
        choiceAcquireMinimumUnit = new JComboBox<>(transitMinUnitModel);

        JPanel pnlTransitTime = new JPanel();
        pnlTransitTime.add(new JLabel("Delivery Time:"));
        pnlTransitTime.add(spnNDiceTransitTime);
        pnlTransitTime.add(new JLabel("d6 + "));
        pnlTransitTime.add(spnConstantTransitTime);
        pnlTransitTime.add(choiceTransitTimeUnits);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlTransitTime, gridBagConstraints);

        JPanel pnlMinTransit = new JPanel();
        pnlMinTransit.add(new JLabel("Minimum Transit Time:"));
        pnlMinTransit.add(spnAcquireMinimum);
        pnlMinTransit.add(choiceAcquireMinimumUnit);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlMinTransit, gridBagConstraints);

        JPanel pnlMosBonus = new JPanel();
        pnlMosBonus.add(new JLabel("Reduce delivery time by"));
        pnlMosBonus.add(spnAcquireMosBonus);
        pnlMosBonus.add(choiceAcquireMosUnits);
        pnlMosBonus.add(new JLabel("per MoS"));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubDelivery.add(pnlMosBonus, gridBagConstraints);

        usePlanetaryAcquisitions.setText(resourceMap.getString("usePlanetaryAcquisitions.text"));
        usePlanetaryAcquisitions.setToolTipText(resourceMap.getString("usePlanetaryAcquisitions.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(usePlanetaryAcquisitions, gridBagConstraints);

        spnMaxJumpPlanetaryAcquisitions = new JSpinner(new SpinnerNumberModel(2, 0, 5, 1));
        JPanel panMaxJump = new JPanel();
        panMaxJump.add(spnMaxJumpPlanetaryAcquisitions);
        panMaxJump.add(new JLabel(resourceMap.getString("lblMaxJumpPlanetaryAcquisitions.text")));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panMaxJump, gridBagConstraints);

        comboPlanetaryAcquisitionsFactionLimits = new JComboBox<>(PlanetaryAcquisitionFactionLimit.values());
        JPanel panFactionLimit = new JPanel();
        panFactionLimit.add(new JLabel(resourceMap.getString("lblPlanetaryAcquisitionsFactionLimits.text")));
        panFactionLimit.add(comboPlanetaryAcquisitionsFactionLimits);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panFactionLimit, gridBagConstraints);

        disallowPlanetaryAcquisitionClanCrossover.setText(resourceMap.getString("disallowPlanetaryAcquisitionClanCrossover.text"));
        disallowPlanetaryAcquisitionClanCrossover.setToolTipText(resourceMap.getString("disallowPlanetaryAcquisitionClanCrossover.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(disallowPlanetaryAcquisitionClanCrossover, gridBagConstraints);

        disallowClanPartsFromIS.setText(resourceMap.getString("disallowClanPartsFromIS.text"));
        disallowClanPartsFromIS.setToolTipText(resourceMap.getString("disallowClanPartsFromIS.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(disallowClanPartsFromIS, gridBagConstraints);

        spnPenaltyClanPartsFromIS = new JSpinner(new SpinnerNumberModel(0, 0, 12, 1));
        JPanel panPenaltyClanPartsFromIS = new JPanel();
        panPenaltyClanPartsFromIS.add(spnPenaltyClanPartsFromIS);
        JLabel lblPenaltyClanPartsFromIS = new JLabel(resourceMap.getString("spnPenaltyClanPartsFromIS.text"));
        lblPenaltyClanPartsFromIS.setToolTipText(resourceMap.getString("spnPenaltyClanPartsFromIS.toolTipText"));
        panPenaltyClanPartsFromIS.add(lblPenaltyClanPartsFromIS);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panPenaltyClanPartsFromIS, gridBagConstraints);

        usePlanetaryAcquisitionsVerbose.setText(resourceMap.getString("usePlanetaryAcquisitionsVerbose.text"));
        usePlanetaryAcquisitionsVerbose.setToolTipText(resourceMap.getString("usePlanetaryAcquisitionsVerbose.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(usePlanetaryAcquisitionsVerbose, gridBagConstraints);

        JPanel panSocioIndustrialBonus = new JPanel();
        panSocioIndustrialBonus.setLayout(new BoxLayout(panSocioIndustrialBonus, BoxLayout.LINE_AXIS));
        panSocioIndustrialBonus.setBorder(BorderFactory.createTitledBorder("Planet socio-industrial modifiers "));

        JPanel panTechBonus = new JPanel(new GridBagLayout());
        JPanel panIndustryBonus = new JPanel(new GridBagLayout());
        JPanel panOutputBonus = new JPanel(new GridBagLayout());

        spnPlanetAcquireTechBonus = new JSpinner[6];
        spnPlanetAcquireIndustryBonus = new JSpinner[6];
        spnPlanetAcquireOutputBonus = new JSpinner[6];

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridwidth = 2;
        panTechBonus.add(new JLabel("<html><b>Tech<b></html>"), gridBagConstraints);
        panIndustryBonus.add(new JLabel("<html><b>Industry<b></html>"), gridBagConstraints);
        panOutputBonus.add(new JLabel("<html><b>Output<b></html>"), gridBagConstraints);
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridy++;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.insets = new Insets(0, 20, 0, 0);
            panTechBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            panIndustryBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            panOutputBonus.add(new JLabel(ITechnology.getRatingName(i) + " Level"), gridBagConstraints);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            spnPlanetAcquireTechBonus[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            spnPlanetAcquireIndustryBonus[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            spnPlanetAcquireOutputBonus[i] = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            ((JSpinner.DefaultEditor) spnPlanetAcquireTechBonus[i].getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) spnPlanetAcquireIndustryBonus[i].getEditor()).getTextField().setEditable(false);
            ((JSpinner.DefaultEditor) spnPlanetAcquireOutputBonus[i].getEditor()).getTextField().setEditable(false);

            panTechBonus.add(spnPlanetAcquireTechBonus[i], gridBagConstraints);
            panOutputBonus.add(spnPlanetAcquireOutputBonus[i], gridBagConstraints);
            panIndustryBonus.add(spnPlanetAcquireIndustryBonus[i], gridBagConstraints);

        }

        panSocioIndustrialBonus.add(panTechBonus);
        panSocioIndustrialBonus.add(panIndustryBonus);
        panSocioIndustrialBonus.add(panOutputBonus);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubPlanetAcquire.add(panSocioIndustrialBonus, gridBagConstraints);

        getOptionsPane().addTab(resourceMap.getString("panSupplies.TabConstraints.tabTitle"), panSupplies);
        //endregion Supplies and Acquisition Tab

        //region Tech Limits Tab
        gridy = 0;

        panTech.setName("panTech");
        panTech.setLayout(new java.awt.GridBagLayout());

        limitByYearBox.setText(resourceMap.getString("limitByYearBox.text"));
        limitByYearBox.setToolTipText(resourceMap.getString("limitByYearBox.toolTipText"));
        limitByYearBox.setName("limitByYearBox");
        limitByYearBox.addActionListener(e -> variableTechLevelBox.setEnabled(limitByYearBox.isSelected()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(limitByYearBox, gridBagConstraints);

        disallowExtinctStuffBox.setText(resourceMap.getString("disallowExtinctStuffBox.text"));
        disallowExtinctStuffBox.setToolTipText(resourceMap.getString("disallowExtinctStuffBox.toolTipText"));
        disallowExtinctStuffBox.setName("disallowExtinctStuffBox");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(disallowExtinctStuffBox, gridBagConstraints);

        allowClanPurchasesBox.setText(resourceMap.getString("allowClanPurchasesBox.text"));
        allowClanPurchasesBox.setToolTipText(resourceMap.getString("allowClanPurchasesBox.toolTipText"));
        allowClanPurchasesBox.setName("allowClanPurchasesBox");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowClanPurchasesBox, gridBagConstraints);

        allowISPurchasesBox.setText(resourceMap.getString("allowISPurchasesBox.text"));
        allowISPurchasesBox.setToolTipText(resourceMap.getString("allowISPurchasesBox.toolTipText"));
        allowISPurchasesBox.setName("allowISPurchasesBox");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowISPurchasesBox, gridBagConstraints);

        allowCanonOnlyBox.setText(resourceMap.getString("allowCanonOnlyBox.text"));
        allowCanonOnlyBox.setToolTipText(resourceMap.getString("allowCanonOnlyBox.toolTipText"));
        allowCanonOnlyBox.setName("allowCanonOnlyBox");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowCanonOnlyBox, gridBagConstraints);

        allowCanonRefitOnlyBox.setText(resourceMap.getString("allowCanonRefitOnlyBox.text")); // NOI18N
        allowCanonRefitOnlyBox.setToolTipText(resourceMap.getString("allowCanonRefitOnlyBox.toolTipText")); // NOI18N
        allowCanonRefitOnlyBox.setName("allowCanonRefitOnlyBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(allowCanonRefitOnlyBox, gridBagConstraints);

        JLabel lblTechLevel = new JLabel(resourceMap.getString("lblTechLevel.text"));
        lblTechLevel.setName("lblTechLevel");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTech.add(lblTechLevel, gridBagConstraints);

        DefaultComboBoxModel<String> techLevelComboBoxModel = new DefaultComboBoxModel<>();
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_INTRO));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_STANDARD));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_ADVANCED));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_EXPERIMENTAL));
        techLevelComboBoxModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_UNOFFICIAL));
        choiceTechLevel.setModel(techLevelComboBoxModel);
        //choiceTechLevel.setToolTipText(resourceMap.getString("choiceTechLevel.toolTipText")); // NOI18N
        choiceTechLevel.setName("choiceTechLevel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(choiceTechLevel, gridBagConstraints);

        variableTechLevelBox.setText(resourceMap.getString("variableTechLevelBox.text")); // NOI18N
        variableTechLevelBox.setToolTipText(resourceMap.getString("variableTechLevelBox.toolTipText")); // NOI18N
        variableTechLevelBox.setName("variableTechLevelBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(variableTechLevelBox, gridBagConstraints);

        factionIntroDateBox.setText(resourceMap.getString("factionIntroDateBox.text"));
        factionIntroDateBox.setToolTipText(resourceMap.getString("factionIntroDateBox.toolTipText"));
        factionIntroDateBox.setName("factionIntroDateBox");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(factionIntroDateBox, gridBagConstraints);

        useAmmoByTypeBox.setText(resourceMap.getString("useAmmoByTypeBox.text"));
        useAmmoByTypeBox.setToolTipText(resourceMap.getString("useAmmoByTypeBox.toolTipText"));
        useAmmoByTypeBox.setName("useAmmoByTypeBox");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panTech.add(useAmmoByTypeBox, gridBagConstraints);

        getOptionsPane().addTab(resourceMap.getString("panTech.TabConstraints.tabTitle"), panTech);
        //endregion Tech Limits Tab

        //region Personnel Tab
        getOptionsPane().addTab(resourceMap.getString("personnelPanel.title"), createPersonnelTab());
        //endregion Personnel Tab

        //region Finances Tab
        JPanel panFinances = new JPanel();
        panFinances.setName("panFinances");
        panFinances.setLayout(new GridBagLayout());
        gridy = 0;

        payForPartsBox.setText(resourceMap.getString("payForPartsBox.text"));
        payForPartsBox.setToolTipText(resourceMap.getString("payForPartsBox.toolTipText"));
        payForPartsBox.setName("payForPartsBox");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForPartsBox, gridBagConstraints);

        payForRepairsBox.setText(resourceMap.getString("payForRepairsBox.text")); // NOI18N
        payForRepairsBox.setToolTipText(resourceMap.getString("payForRepairsBox.toolTipText")); // NOI18N
        payForRepairsBox.setName("payForRepairsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForRepairsBox, gridBagConstraints);

        payForUnitsBox.setText(resourceMap.getString("payForUnitsBox.text")); // NOI18N
        payForUnitsBox.setToolTipText(resourceMap.getString("payForUnitsBox.toolTipText")); // NOI18N
        payForUnitsBox.setName("payForUnitsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForUnitsBox, gridBagConstraints);

        payForSalariesBox.setText(resourceMap.getString("payForSalariesBox.text")); // NOI18N
        payForSalariesBox.setToolTipText(resourceMap.getString("payForSalariesBox.toolTipText")); // NOI18N
        payForSalariesBox.setName("payForSalariesBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForSalariesBox, gridBagConstraints);

        payForOverheadBox.setText(resourceMap.getString("payForOverheadBox.text")); // NOI18N
        payForOverheadBox.setToolTipText(resourceMap.getString("payForOverheadBox.toolTipText")); // NOI18N
        payForOverheadBox.setName("payForOverheadBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForOverheadBox, gridBagConstraints);

        payForMaintainBox.setText(resourceMap.getString("payForMaintainBox.text")); // NOI18N
        payForMaintainBox.setToolTipText(resourceMap.getString("payForMaintainBox.toolTipText")); // NOI18N
        payForMaintainBox.setName("payForMaintainBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForMaintainBox, gridBagConstraints);

        payForTransportBox.setText(resourceMap.getString("payForTransportBox.text")); // NOI18N
        payForTransportBox.setToolTipText(resourceMap.getString("payForTransportBox.toolTipText")); // NOI18N
        payForTransportBox.setName("payForTransportBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForTransportBox, gridBagConstraints);

        sellUnitsBox.setText(resourceMap.getString("sellUnitsBox.text")); // NOI18N
        sellUnitsBox.setToolTipText(resourceMap.getString("sellUnitsBox.toolTipText")); // NOI18N
        sellUnitsBox.setName("sellUnitsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(sellUnitsBox, gridBagConstraints);

        sellPartsBox.setText(resourceMap.getString("sellPartsBox.text")); // NOI18N
        sellPartsBox.setToolTipText(resourceMap.getString("sellPartsBox.toolTipText")); // NOI18N
        sellPartsBox.setName("sellPartsBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(sellPartsBox, gridBagConstraints);

        payForRecruitmentBox.setText(resourceMap.getString("payForRecruitmentBox.text")); // NOI18N
        payForRecruitmentBox.setToolTipText(resourceMap.getString("payForRecruitmentBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(payForRecruitmentBox, gridBagConstraints);

        useLoanLimitsBox.setText(resourceMap.getString("useLoanLimitsBox.text")); // NOI18N
        useLoanLimitsBox.setToolTipText(resourceMap.getString("useLoanLimitsBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(useLoanLimitsBox, gridBagConstraints);

        // Unofficial maintenance costs
        usePercentageMaintBox = new JCheckBox(resourceMap.getString("usePercentageMaintBox.text")); // NOI18N
        usePercentageMaintBox.setToolTipText(resourceMap.getString("usePercentageMaintBox.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(usePercentageMaintBox, gridBagConstraints);

        // Unofficial infantry don't count for contract pay
        useInfantryDontCountBox = new JCheckBox(resourceMap.getString("infantryDontCount.text")); // NOI18N
        useInfantryDontCountBox.setToolTipText(resourceMap.getString("infantryDontCount.toolTipText")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(useInfantryDontCountBox, gridBagConstraints);

        // Campaign Operations Peacetime operating costs
        usePeacetimeCostBox.setText(resourceMap.getString("usePeacetimeCostBox.text")); // NOI18N
        usePeacetimeCostBox.setToolTipText(resourceMap.getString("usePeacetimeCostBox.toolTipText")); // NOI18N
        usePeacetimeCostBox.setName("usePeacetimeCostBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(usePeacetimeCostBox, gridBagConstraints);

        useExtendedPartsModifierBox.setText(resourceMap.getString("useExtendedPartsModifierBox.text")); // NOI18N
        useExtendedPartsModifierBox.setName("useExtendedPartsModifierBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(useExtendedPartsModifierBox, gridBagConstraints);

        showPeacetimeCostBox.setText(resourceMap.getString("showPeacetimeCostBox.text")); // NOI18N
        showPeacetimeCostBox.setToolTipText(resourceMap.getString("showPeacetimeCostBox.toolTipText")); // NOI18N
        showPeacetimeCostBox.setName("showPeacetimeCostBox"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panFinances.add(showPeacetimeCostBox, gridBagConstraints);

        DefaultComboBoxModel<FinancialYearDuration> financialYearDurationModel = new DefaultComboBoxModel<>(FinancialYearDuration.values());
        comboFinancialYearDuration = new JComboBox<>(financialYearDurationModel);
        comboFinancialYearDuration.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof FinancialYearDuration) {
                        list.setToolTipText(((FinancialYearDuration) value).getToolTipText());
                    }
                    return this;
                }
            });
        JPanel pnlFinancialYearDuration = new JPanel();
        pnlFinancialYearDuration.add(new JLabel(resourceMap.getString("financialYearDuration.text")));
        pnlFinancialYearDuration.setToolTipText(resourceMap.getString("financialYearDuration.toolTipText"));
        pnlFinancialYearDuration.add(comboFinancialYearDuration);
        gridBagConstraints.gridy = gridy++;
        panFinances.add(pnlFinancialYearDuration, gridBagConstraints);

        newFinancialYearFinancesToCSVExportBox = new JCheckBox(resourceMap.getString("newFinancialYearFinancesToCSVExportBox.text"));
        newFinancialYearFinancesToCSVExportBox.setToolTipText(resourceMap.getString("newFinancialYearFinancesToCSVExportBox.toolTipText"));
        newFinancialYearFinancesToCSVExportBox.setName("newFinancialYearFinancesToCSVExportBox");
        gridBagConstraints.gridy = gridy++;
        panFinances.add(newFinancialYearFinancesToCSVExportBox, gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 20;
        panFinances.add(createPriceModifiersPanel(), gridBagConstraints);

        getOptionsPane().addTab(resourceMap.getString("panFinances.TabConstraints.tabTitle"), panFinances);
        //endregion Finances Tab

        //region Mercenary Tab
        panMercenary.setName("panMercenary");
        panMercenary.setLayout(new GridBagLayout());

        btnContractEquipment = new JRadioButton(resourceMap.getString("panMercenary.IntOpsPayment.title"));
        btnContractEquipment.setToolTipText(resourceMap.getString("panMercenary.IntOpsPayment.tooltip"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(btnContractEquipment, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("Combat Percent:"), gridBagConstraints);

        spnEquipPercent = new JSpinner(new SpinnerNumberModel(0.1, 0.1, CampaignOptions.MAXIMUM_COMBAT_EQUIPMENT_PERCENT, 0.1));
        spnEquipPercent.setEditor(new JSpinner.NumberEditor(spnEquipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnEquipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnEquipPercent, gridBagConstraints);

        chkEquipContractSaleValue = new JCheckBox("Base on equipment sale value");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(chkEquipContractSaleValue, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("DropShip Percent:"), gridBagConstraints);

        spnDropshipPercent = new JSpinner(new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_DROPSHIP_EQUIPMENT_PERCENT, 0.1));
        spnDropshipPercent.setEditor(new JSpinner.NumberEditor(spnDropshipPercent, "0.0"));
        ((JSpinner.NumberEditor) spnDropshipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnDropshipPercent, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("JumpShip Percent:"), gridBagConstraints);

        spnJumpshipPercent = new JSpinner(new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_JUMPSHIP_EQUIPMENT_PERCENT, 0.1));
        spnJumpshipPercent.setEditor(new JSpinner.NumberEditor(spnJumpshipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnJumpshipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnJumpshipPercent, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 30, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(new JLabel("WarShip Percent:"), gridBagConstraints);

        spnWarshipPercent = new JSpinner(new SpinnerNumberModel(0.1, 0.0, CampaignOptions.MAXIMUM_WARSHIP_EQUIPMENT_PERCENT, 0.1));
        spnWarshipPercent.setEditor(new JSpinner.NumberEditor(spnWarshipPercent, "0.0"));
        ((JSpinner.DefaultEditor) spnWarshipPercent.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(spnWarshipPercent, gridBagConstraints);

        btnContractPersonnel = new JRadioButton(resourceMap.getString("panMercenary.FMMRPayment.title"));
        btnContractPersonnel.setToolTipText(resourceMap.getString("panMercenary.FMMRPayment.tooltip"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panMercenary.add(btnContractPersonnel, gridBagConstraints);

        chkBLCSaleValue = new JCheckBox("Base battle loss compensation on equipment sale value");
        gridBagConstraints.gridy = 6;
        panMercenary.add(chkBLCSaleValue, gridBagConstraints);

        chkOverageRepaymentInFinalPayment = new JCheckBox(resourceMap.getString("chkOverageRepaymentInFinalPayment.text"));
        chkOverageRepaymentInFinalPayment.setToolTipText(resourceMap.getString("chkOverageRepaymentInFinalPayment.toolTipText"));
        gridBagConstraints.gridy = 7;
        panMercenary.add(chkOverageRepaymentInFinalPayment, gridBagConstraints);

        ButtonGroup groupContract = new ButtonGroup();
        groupContract.add(btnContractEquipment);
        groupContract.add(btnContractPersonnel);

        getOptionsPane().addTab(resourceMap.getString("panMercenary.TabConstraints.tabTitle"), panMercenary);
        //endregion Mercenary Tab

        //region XP Tab
        panXP.setName("panXP");
        panXP.setLayout(new java.awt.GridBagLayout());

        JLabel lblScenarioXP = new JLabel(resourceMap.getString("lblScenarioXP.text"));
        spnScenarioXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnScenarioXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnScenarioXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblScenarioXP, gridBagConstraints);

        JLabel lblKillXP = new JLabel(resourceMap.getString("lblKillXP.text"));
        spnKillXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnKillXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnKillXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblKillXP, gridBagConstraints);

        JLabel lblKills = new JLabel(resourceMap.getString("lblKills.text"));
        spnKills = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnKills.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnKills, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblKills, gridBagConstraints);

        JLabel lblTaskXP = new JLabel(resourceMap.getString("lblKillXP.text"));
        spnTaskXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnTaskXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnTaskXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblTaskXP, gridBagConstraints);

        JLabel lblTasks = new JLabel(resourceMap.getString("lblTasks.text"));
        spnNTasksXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnNTasksXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnNTasksXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblTasks, gridBagConstraints);

        JLabel lblSuccessXp = new JLabel(resourceMap.getString("lblSuccessXP.text"));
        spnSuccessXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnSuccessXP.getEditor()).getTextField().setEditable(false);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnSuccessXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblSuccessXp, gridBagConstraints);

        JLabel lblMistakeXP = new JLabel(resourceMap.getString("lblMistakeXP.text"));
        spnMistakeXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnMistakeXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnMistakeXP, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(lblMistakeXP, gridBagConstraints);

        spnIdleXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnIdleXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP for every"), gridBagConstraints);

        spnMonthsIdleXP = new JSpinner(new SpinnerNumberModel(0, 0, 36, 1));
        ((JSpinner.DefaultEditor) spnMonthsIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnMonthsIdleXP, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("active month(s) on a 2d6 roll of greater than or equal to"), gridBagConstraints);

        spnTargetIdleXP = new JSpinner(new SpinnerNumberModel(2, 2, 13, 1));
        ((JSpinner.DefaultEditor) spnTargetIdleXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnTargetIdleXP, gridBagConstraints);

        spnContractNegotiationXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnContractNegotiationXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnContractNegotiationXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP awarded to the selected negotiator for a new contract"), gridBagConstraints);

        spnAdminWeeklyXP = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnAdminWeeklyXP.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnAdminWeeklyXP, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP awarded to each administrator every Monday for the work of the previous"), gridBagConstraints);

        spnAdminWeeklyXPPeriod = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        ((JSpinner.DefaultEditor) spnAdminWeeklyXPPeriod.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnAdminWeeklyXPPeriod, gridBagConstraints);

        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("week(s)"), gridBagConstraints);

        spnEdgeCost = new JSpinner(new SpinnerNumberModel(0, 0, 10000, 1));
        ((JSpinner.DefaultEditor) spnEdgeCost.getEditor()).getTextField().setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(spnEdgeCost, gridBagConstraints);

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(new JLabel("XP Cost for 1 Edge Point"), gridBagConstraints);

        txtInstructionsXP = new JTextArea();
        txtInstructionsXP.setText(resourceMap.getString("txtInstructionsXP.text"));
        txtInstructionsXP.setName("txtInstructions");
        txtInstructionsXP.setEditable(false);
        txtInstructionsXP.setEditable(false);
        txtInstructionsXP.setLineWrap(true);
        txtInstructionsXP.setWrapStyleWord(true);
        txtInstructionsXP.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructionsXP.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtInstructionsXP.setOpaque(false);
        txtInstructionsXP.setMinimumSize(new Dimension(550, 120));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(txtInstructionsXP, gridBagConstraints);

        tableXP = new JTable(getSkillCostsArray(SkillType.getSkillHash()), TABLE_XP_COLUMN_NAMES);
        tableXP.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableXP.setRowSelectionAllowed(false);
        tableXP.setColumnSelectionAllowed(false);
        tableXP.setCellSelectionEnabled(true);
        scrXP = new JScrollPane(tableXP);
        scrXP.setMinimumSize(new Dimension(550, 140));
        scrXP.setPreferredSize(new Dimension(550, 140));
        JTable rowTable = new RowNamesTable(tableXP);
        scrXP.setRowHeaderView(rowTable);
        scrXP.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panXP.add(scrXP, gridBagConstraints);

        getOptionsPane().addTab(resourceMap.getString("panXP.TabConstraints.tabTitle"), panXP);
        //endregion XP Tab

        //region Skill Tab
        panSkill.setName("panSkill");
        panSkill.setLayout(new java.awt.GridBagLayout());

        JPanel skPanel;

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);

        GridBagConstraints c;
        JSpinner spnTarget;
        JSpinner spnGreen;
        JSpinner spnReg;
        JSpinner spnVet;
        JSpinner spnElite;
        SkillType type;
        JLabel lblSkill;
        for (String skillName : SkillType.getSkillList()) {
            type = SkillType.getType(skillName);
            skPanel = new JPanel();
            c = new java.awt.GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = java.awt.GridBagConstraints.BOTH;
            c.anchor = java.awt.GridBagConstraints.WEST;
            c.insets = new java.awt.Insets(5, 5, 5, 5);
            lblSkill = new JLabel(resourceMap.getString("lblSkillTarget.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnTarget = new JSpinner(new SpinnerNumberModel(type.getTarget(), 0, 12, 1));
            ((JSpinner.DefaultEditor) spnTarget.getEditor()).getTextField().setEditable(false);
            hashSkillTargets.put(skillName, spnTarget);
            skPanel.add(spnTarget, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillGreen.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnGreen = new JSpinner(new SpinnerNumberModel(type.getGreenLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnGreen.getEditor()).getTextField().setEditable(false);
            hashGreenSkill.put(skillName, spnGreen);
            skPanel.add(spnGreen, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillRegular.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnReg = new JSpinner(new SpinnerNumberModel(type.getRegularLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnReg.getEditor()).getTextField().setEditable(false);
            hashRegSkill.put(skillName, spnReg);
            skPanel.add(spnReg, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillVeteran.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnVet = new JSpinner(new SpinnerNumberModel(type.getVeteranLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnVet.getEditor()).getTextField().setEditable(false);
            hashVetSkill.put(skillName, spnVet);
            skPanel.add(spnVet, c);
            c.gridx++;
            lblSkill = new JLabel(resourceMap.getString("lblSkillElite.text"));
            skPanel.add(lblSkill, c);
            c.gridx++;
            spnElite = new JSpinner(new SpinnerNumberModel(type.getEliteLevel(), 0, 10, 1));
            ((JSpinner.DefaultEditor) spnElite.getEditor()).getTextField().setEditable(false);
            hashEliteSkill.put(skillName, spnElite);
            skPanel.add(spnElite, c);
            c.gridx++;

            skPanel.setBorder(BorderFactory.createTitledBorder(skillName));
            panSkill.add(skPanel, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        JScrollPane scrSkill = new JScrollPane(panSkill);
        scrSkill.setPreferredSize(new java.awt.Dimension(500, 400));

        getOptionsPane().addTab(resourceMap.getString("panSkill.TabConstraints.tabTitle"), scrSkill);
        //endregion Skills Tab

        //region Special Abilities Tab
        panSpecialAbilities = new JPanel(new GridBagLayout());

        Set<String> spaNames = SpecialAbility.getAllSpecialAbilities().keySet();
        //We need to create a temporary hash of special abilities that we can modify without
        //changing the underlying one in case the user cancels the changes
        tempSPA = new Hashtable<>();
        for (String name : spaNames) {
            getCurrentSPA().put(name, SpecialAbility.getAbility(name).clone());
        }

        btnAddSPA = new JButton("Add Another Special Ability");
        btnAddSPA.addActionListener(evt -> btnAddSPA());

        recreateSPAPanel(!getUnusedSPA().isEmpty());

        JScrollPane scrSPA = new JScrollPane(panSpecialAbilities);
        scrSPA.setPreferredSize(new java.awt.Dimension(500, 400));

        getOptionsPane().addTab("Special Abilities", scrSPA);
        //endregion Special Abilities Tab

        //region Skill Randomization Tab
        panRandomSkill.setName("panRandomSkill");
        panRandomSkill.setLayout(new java.awt.GridBagLayout());

        JPanel panRollTable = new JPanel(new GridLayout(6, 3, 5, 0));
        panRollTable.add(new JLabel("<html><b>Value</b></html>"));
        panRollTable.add(new JLabel("<html><b>Level</b></html>"));
        panRollTable.add(new JLabel("<html><b># Abils</b></html>"));
        panRollTable.add(new JLabel("less than 2"));
        JLabel lblUltraGreen = new JLabel("Ultra-Green/None");
        lblUltraGreen.setToolTipText(resourceMap.getString("lblUltraGreen.toolTipText"));
        panRollTable.add(lblUltraGreen);
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("2-5"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_GREEN]));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("6-9"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_REGULAR]));
        panRollTable.add(new JLabel("0"));
        panRollTable.add(new JLabel("10-11"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_VETERAN]));
        panRollTable.add(new JLabel("1"));
        panRollTable.add(new JLabel("12 or more"));
        panRollTable.add(new JLabel(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ELITE]));
        panRollTable.add(new JLabel("2"));
        panRollTable.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("2d6 + Bonus"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JLabel lblOverallRecruitBonus = new JLabel(resourceMap.getString("lblOverallRecruitBonus.text"));
        chkExtraRandom = new JCheckBox(resourceMap.getString("chkExtraRandom.text"));
        chkExtraRandom.setToolTipText(resourceMap.getString("chkExtraRandom.toolTipText"));
        JLabel lblProbAntiMek = new JLabel(resourceMap.getString("lblProbAntiMek.text"));
        spnProbAntiMek = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((JSpinner.DefaultEditor) spnProbAntiMek.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
        ((JSpinner.DefaultEditor) spnOverallRecruitBonus.getEditor()).getTextField().setEditable(false);
        spnOverallRecruitBonus.setToolTipText(resourceMap.getString("spnOverallRecruitBonus.toolTipText"));
        spnTypeRecruitBonus = new JSpinner[personnelRoles.length];
        int nRow = (int) Math.ceil(personnelRoles.length / 4.0);
        JPanel panTypeRecruitBonus = new JPanel(new GridLayout(nRow, 4));
        JSpinner spin;
        JPanel panRecruit;
        for (PersonnelRole role : personnelRoles) {
            panRecruit = new JPanel(new GridBagLayout());

            spin = new JSpinner(new SpinnerNumberModel(0, -12, 12, 1));
            ((JSpinner.DefaultEditor) spin.getEditor()).getTextField().setEditable(false);
            spnTypeRecruitBonus[role.ordinal()] = spin;
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(2, 5, 0, 0);
            panRecruit.add(spin, gridBagConstraints);

            gridBagConstraints.gridx = 1;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            panRecruit.add(new JLabel(role.toString()), gridBagConstraints);

            panTypeRecruitBonus.add(panRecruit);
        }

        panTypeRecruitBonus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("panTypeRecruitBonus.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(chkExtraRandom, gridBagConstraints);

        // Phenotype Percentage Generation
        List<Phenotype> phenotypes = Phenotype.getExternalPhenotypes();
        phenotypeSpinners = new JSpinner[phenotypes.size()];

        JPanel phenotypesPanel = new JPanel(new GridLayout((int) Math.ceil(phenotypes.size() / 2.0), 2));
        phenotypesPanel.setBorder(BorderFactory.createTitledBorder("Trueborn Phenotype Probabilities"));

        for (int i = 0; i < phenotypes.size(); i++) {
            JSpinner phenotypeSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
            phenotypeSpinners[i] = phenotypeSpinner;
            JPanel phenotypePanel = new JPanel();
            phenotypePanel.add(phenotypeSpinner);
            phenotypePanel.add(new JLabel(phenotypes.get(i).getName()));
            phenotypePanel.setToolTipText(phenotypes.get(i).getToolTip());
            phenotypesPanel.add(phenotypePanel);
        }

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRandomSkill.add(phenotypesPanel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(panRollTable, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(spnProbAntiMek, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(lblProbAntiMek, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(spnOverallRecruitBonus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(lblOverallRecruitBonus, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(panTypeRecruitBonus, gridBagConstraints);

        JPanel panArtillery = new JPanel();
        panArtillery.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Artillery Skill"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        spnArtyProb = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((JSpinner.DefaultEditor) spnArtyProb.getEditor()).getTextField().setEditable(false);
        spnArtyProb.setToolTipText(resourceMap.getString("spnArtyProb.toolTipText"));
        panArtillery.add(spnArtyProb);
        panArtillery.add(new JLabel("Probability"));
        spnArtyBonus = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnArtyBonus.getEditor()).getTextField().setEditable(false);
        panArtillery.add(spnArtyBonus);
        panArtillery.add(new JLabel("Bonus"));
        JPanel panSecondary = new JPanel();
        spnSecondProb = new JSpinner(new SpinnerNumberModel(0, 0, 100, 5));
        ((JSpinner.DefaultEditor) spnSecondProb.getEditor()).getTextField().setEditable(false);
        spnSecondProb.setToolTipText(resourceMap.getString("spnSecondProb.toolTipText"));
        panSecondary.add(spnSecondProb);
        panSecondary.add(new JLabel("Probability"));
        spnSecondBonus = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnSecondBonus.getEditor()).getTextField().setEditable(false);
        panSecondary.add(spnSecondBonus);
        panSecondary.add(new JLabel("Bonus"));
        panSecondary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Secondary Skills"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panTactics = new JPanel();
        spnTacticsGreen = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnTacticsGreen.getEditor()).getTextField().setEditable(false);
        spnTacticsGreen.setToolTipText(resourceMap.getString("spnTacticsGreen.toolTipText"));
        spnTacticsReg = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnTacticsReg.getEditor()).getTextField().setEditable(false);
        spnTacticsReg.setToolTipText(resourceMap.getString("spnTacticsReg.toolTipText"));
        spnTacticsVet = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnTacticsVet.getEditor()).getTextField().setEditable(false);
        spnTacticsVet.setToolTipText(resourceMap.getString("spnTacticsVet.toolTipText"));
        spnTacticsElite = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnTacticsElite.getEditor()).getTextField().setEditable(false);
        spnTacticsElite.setToolTipText(resourceMap.getString("spnTacticsElite.toolTipText"));
        panTactics.add(spnTacticsGreen);
        panTactics.add(new JLabel("Green"));
        panTactics.add(spnTacticsReg);
        panTactics.add(new JLabel("Reg"));
        panTactics.add(spnTacticsVet);
        panTactics.add(new JLabel("Vet"));
        panTactics.add(spnTacticsElite);
        panTactics.add(new JLabel("Elite"));
        panTactics.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Tactics Skill"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panSmallArms = new JPanel();
        spnCombatSA = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnCombatSA.getEditor()).getTextField().setEditable(false);
        spnCombatSA.setToolTipText(resourceMap.getString("spnCombatSA.toolTipText"));
        spnSupportSA = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnSupportSA.getEditor()).getTextField().setEditable(false);
        spnSupportSA.setToolTipText(resourceMap.getString("spnSupportSA.toolTipText"));
        panSmallArms.add(spnCombatSA);
        panSmallArms.add(new JLabel("Combat Personnel"));
        panSmallArms.add(spnSupportSA);
        panSmallArms.add(new JLabel("Support Personnel"));
        panSmallArms.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Small Arms Skill"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JPanel panAbilities = new JPanel();
        spnAbilGreen = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnAbilGreen.getEditor()).getTextField().setEditable(false);
        spnAbilGreen.setToolTipText(resourceMap.getString("spnAbilGreen.toolTipText"));
        spnAbilReg = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnAbilReg.getEditor()).getTextField().setEditable(false);
        spnAbilReg.setToolTipText(resourceMap.getString("spnAbilReg.toolTipText"));
        spnAbilVet = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnAbilVet.getEditor()).getTextField().setEditable(false);
        spnAbilVet.setToolTipText(resourceMap.getString("spnAbilVet.toolTipText"));
        spnAbilElite = new JSpinner(new SpinnerNumberModel(0, -10, 10, 1));
        ((JSpinner.DefaultEditor) spnAbilElite.getEditor()).getTextField().setEditable(false);
        spnAbilElite.setToolTipText(resourceMap.getString("spnAbilElite.toolTipText"));
        panAbilities.add(spnAbilGreen);
        panAbilities.add(new JLabel("Green"));
        panAbilities.add(spnAbilReg);
        panAbilities.add(new JLabel("Reg"));
        panAbilities.add(spnAbilVet);
        panAbilities.add(new JLabel("Vet"));
        panAbilities.add(spnAbilElite);
        panAbilities.add(new JLabel("Elite"));
        panAbilities.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Special Abilities"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JPanel panOtherBonuses = new JPanel(new GridLayout(3, 2));
        panOtherBonuses.add(panArtillery);
        panOtherBonuses.add(panSecondary);
        panOtherBonuses.add(panTactics);
        panOtherBonuses.add(panAbilities);
        panOtherBonuses.add(panSmallArms);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panRandomSkill.add(panOtherBonuses, gridBagConstraints);

        JScrollPane scrRandomSkill = new JScrollPane(panRandomSkill);
        scrRandomSkill.setPreferredSize(new java.awt.Dimension(500, 400));

        getOptionsPane().addTab(resourceMap.getString("panRandomSkill.TabConstraints.tabTitle"), scrRandomSkill);
        //endregion Skill Randomization Tab

        //region Rank Systems Tab
        getOptionsPane().addTab(resourceMap.getString("rankSystemsPanel.title"), createRankSystemsTab(getFrame(), campaign));
        //endregion Rank Systems Tab

        //region Name and Portrait Generation Tab
        panNameGen.setName("panNameGen");
        panNameGen.setLayout(new GridBagLayout());

        chkUseOriginFactionForNames = new JCheckBox(resourceMap.getString("chkUseOriginFactionForNames.text"));
        chkUseOriginFactionForNames.setToolTipText(resourceMap.getString("chkUseOriginFactionForNames.toolTipText"));
        chkUseOriginFactionForNames.setName("chkUseOriginFactionForNames");
        chkUseOriginFactionForNames.addActionListener(
                evt -> comboFactionNames.setEnabled(!chkUseOriginFactionForNames.isSelected()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridy = 0;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panNameGen.add(chkUseOriginFactionForNames, gridBagConstraints);


        JLabel lblFactionNames = new JLabel(resourceMap.getString("lblFactionNames.text"));
        lblFactionNames.setName("lblFactionNames");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panNameGen.add(lblFactionNames, gridBagConstraints);

        DefaultComboBoxModel<String> factionNamesModel = new DefaultComboBoxModel<>();
        for (String faction : RandomNameGenerator.getInstance().getFactions()) {
            factionNamesModel.addElement(faction);
        }
        factionNamesModel.setSelectedItem(RandomNameGenerator.getInstance().getChosenFaction());
        comboFactionNames.setModel(factionNamesModel);
        comboFactionNames.setMinimumSize(new java.awt.Dimension(400, 30));
        comboFactionNames.setName("comboFactionNames");
        comboFactionNames.setPreferredSize(new java.awt.Dimension(400, 30));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panNameGen.add(comboFactionNames, gridBagConstraints);

        JLabel lblGender = new JLabel(resourceMap.getString("lblGender.text"));
        lblGender.setName("lblGender");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panNameGen.add(lblGender, gridBagConstraints);

        sldGender.setMaximum(100);
        sldGender.setMinimum(0);
        sldGender.setMajorTickSpacing(25);
        sldGender.setPaintTicks(true);
        sldGender.setPaintLabels(true);
        sldGender.setValue(RandomGenderGenerator.getPercentFemale());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = gridy;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        panNameGen.add(sldGender, gridBagConstraints);

        panRandomPortrait.setName("panRandomPortrait");
        panRandomPortrait.setLayout(new BorderLayout());

        // The math below is used to determine how to split the personnel role options for portraits,
        // which it does into 4 columns with rows equal to the number of roles plus two, with the
        // additional two being the all role and no role options.
        JPanel panUsePortrait = new JPanel(new GridLayout((int) Math.ceil((personnelRoles.length + 2) / 4.0), 4));
        chkUsePortrait = new JCheckBox[personnelRoles.length];
        allPortraitsBox = new JCheckBox(resourceMap.getString("panUsePortrait.all.text"));
        noPortraitsBox = new JCheckBox(resourceMap.getString("panUsePortrait.no.text"));
        allPortraitsBox.addActionListener(evt -> {
            final boolean selected = allPortraitsBox.isSelected();
            for (JCheckBox box : chkUsePortrait) {
                if (selected) {
                    box.setSelected(true);
                }
                box.setEnabled(!selected);
            }
            if (selected) {
                noPortraitsBox.setSelected(false);
            }
        });
        noPortraitsBox.addActionListener(evt -> {
            final boolean selected = noPortraitsBox.isSelected();
            for (JCheckBox box : chkUsePortrait) {
                if (selected) {
                    box.setSelected(false);
                }
                box.setEnabled(!selected);
            }
            if (selected) {
                allPortraitsBox.setSelected(false);
            }
        });
        panUsePortrait.add(allPortraitsBox);
        panUsePortrait.add(noPortraitsBox);

        JCheckBox box;
        for (PersonnelRole role : personnelRoles) {
            box = new JCheckBox(role.toString());
            panUsePortrait.add(box);
            chkUsePortrait[role.ordinal()] = box;
        }

        panRandomPortrait.add(panUsePortrait, BorderLayout.CENTER);
        JTextArea txtPortraitInst = new JTextArea(resourceMap.getString("txtPortraitInst.text"));
        txtPortraitInst.setPreferredSize(new Dimension(728, 60));
        txtPortraitInst.setEditable(false);
        txtPortraitInst.setLineWrap(true);
        txtPortraitInst.setWrapStyleWord(true);
        txtPortraitInst.setOpaque(false);
        panRandomPortrait.add(txtPortraitInst, BorderLayout.PAGE_START);

        panRandomPortrait.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("panRandomPortrait.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(10, 0, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panNameGen.add(panRandomPortrait, gridBagConstraints);

        chkAssignPortraitOnRoleChange.setText(resourceMap.getString("chkAssignPortraitOnRoleChange.text"));
        chkAssignPortraitOnRoleChange.setToolTipText(resourceMap.getString("chkAssignPortraitOnRoleChange.toolTipText"));
        chkAssignPortraitOnRoleChange.setName("chkAssignPortraitOnRoleChange");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = ++gridy;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panNameGen.add(chkAssignPortraitOnRoleChange, gridBagConstraints);

        getOptionsPane().addTab(resourceMap.getString("panNameGen.TabConstraints.tabTitle"), panNameGen);
        //endregion Name and Portrait Generation Tab

        //region Markets Tab
        getOptionsPane().addTab(resourceMap.getString("marketsPanel.title"), createMarketsTab());
        //endregion Markets Tab

        //region RATs Tab
        getOptionsPane().addTab(resourceMap.getString("ratPanel.title"), createRATTab());
        //endregion RATs Tab

        //region Against the Bot Tab
        panAtB = new JPanel();

        cbSkillLevel = new JComboBox<>();
        chkUseShareSystem = new JCheckBox();
        chkRetirementRolls = new JCheckBox();
        chkTrackUnitFatigue = new JCheckBox();
        chkCustomRetirementMods = new JCheckBox();
        chkFoundersNeverRetire = new JCheckBox();
        chkTrackOriginalUnit = new JCheckBox();
        chkLimitLanceWeight = new JCheckBox();
        chkLimitLanceNumUnits = new JCheckBox();
        chkUseLeadership = new JCheckBox();
        chkUseStrategy = new JCheckBox();
        spnBaseStrategyDeployment = new JSpinner();
        spnAdditionalStrategyDeployment = new JSpinner();

        chkUseAero = new JCheckBox();
        chkUseVehicles = new JCheckBox();
        chkClanVehicles = new JCheckBox();
        chkDoubleVehicles = new JCheckBox();
        chkAdjustPlayerVehicles = new JCheckBox();
        spnOpforLanceTypeMechs = new JSpinner();
        spnOpforLanceTypeMixed = new JSpinner();
        spnOpforLanceTypeVehicles = new JSpinner();
        chkOpforUsesVTOLs = new JCheckBox();
        chkOpforUsesAero = new JCheckBox();
        chkOpforUsesLocalForces = new JCheckBox();
        spnOpforAeroChance = new JSpinner();
        spnOpforLocalForceChance = new JSpinner();
        spnFixedMapChance = new JSpinner();

        spnSearchRadius = new JSpinner();
        chkVariableContractLength = new JCheckBox();
        chkMercSizeLimited = new JCheckBox();
        chkRestrictPartsByMission = new JCheckBox();
        chkUseLightConditions = new JCheckBox();
        chkUsePlanetaryConditions = new JCheckBox();
        chkAeroRecruitsHaveUnits = new JCheckBox();

        panAtB.setName("panAtB");
        panAtB.setLayout(new GridBagLayout());

        JPanel panSubAtBAdmin = new JPanel(new GridBagLayout());
        JPanel panSubAtBContract = new JPanel(new GridBagLayout());
        JPanel panSubAtBScenario = new JPanel(new GridBagLayout());
        panSubAtBAdmin.setBorder(BorderFactory.createTitledBorder("Unit Administration"));
        panSubAtBContract.setBorder(BorderFactory.createTitledBorder("Contract Operations"));
        panSubAtBScenario.setBorder(BorderFactory.createTitledBorder("Scenarios"));

        chkUseAtB = new JCheckBox(resourceMap.getString("chkUseAtB.text"));
        chkUseAtB.setToolTipText(resourceMap.getString("chkUseAtB.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(10, 10, 10, 10);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panAtB.add(chkUseAtB, gridBagConstraints);
        chkUseAtB.addActionListener(ev -> enableAtBComponents(panAtB, chkUseAtB.isSelected()));

        JLabel lblSkillLevel = new JLabel(resourceMap.getString("lblSkillLevel.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        panAtB.add(lblSkillLevel, gridBagConstraints);

        // TODO : Switch me to use a modified RandomSkillsGenerator.levelNames
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ULTRA_GREEN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_GREEN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_REGULAR]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_VETERAN]);
        cbSkillLevel.addItem(SkillType.SKILL_LEVEL_NAMES[SkillType.EXP_ELITE]);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        panAtB.add(cbSkillLevel, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        panAtB.add(panSubAtBAdmin, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        panAtB.add(panSubAtBScenario, gridBagConstraints);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        panAtB.add(panSubAtBContract, gridBagConstraints);

        chkUseStratCon = new JCheckBox(resourceMap.getString("chkUseStratCon.text"));
        chkUseStratCon.setToolTipText(resourceMap.getString("chkUseStratCon.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        panAtB.add(chkUseStratCon, gridBagConstraints);

        // AtB options: "Unit Administration" frame controls
        chkUseShareSystem.setText(resourceMap.getString("chkUseShareSystem.text"));
        chkUseShareSystem.setToolTipText(resourceMap.getString("chkUseShareSystem.toolTipText"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAtBAdmin.add(chkUseShareSystem, gridBagConstraints);

        chkSharesExcludeLargeCraft = new JCheckBox(resourceMap.getString("chkSharesExcludeLargeCraft.text"));
        chkSharesExcludeLargeCraft.setToolTipText(resourceMap.getString("chkSharesExcludeLargeCraft.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkSharesExcludeLargeCraft, gridBagConstraints);

        chkSharesForAll = new JCheckBox(resourceMap.getString("chkSharesForAll.text"));
        chkSharesForAll.setToolTipText(resourceMap.getString("chkSharesForAll.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkSharesForAll, gridBagConstraints);

        chkAeroRecruitsHaveUnits.setText(resourceMap.getString("chkAeroRecruitsHaveUnits.text"));
        chkAeroRecruitsHaveUnits.setToolTipText(resourceMap.getString("chkAeroRecruitsHaveUnits.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkAeroRecruitsHaveUnits, gridBagConstraints);

        chkRetirementRolls.setText(resourceMap.getString("chkRetirementRolls.text"));
        chkRetirementRolls.setToolTipText(resourceMap.getString("chkRetirementRolls.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkRetirementRolls, gridBagConstraints);

        chkCustomRetirementMods.setText(resourceMap.getString("chkCustomRetirementMods.text"));
        chkCustomRetirementMods.setToolTipText(resourceMap.getString("chkCustomRetirementMods.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkCustomRetirementMods, gridBagConstraints);

        chkFoundersNeverRetire.setText(resourceMap.getString("chkFoundersNeverRetire.text"));
        chkFoundersNeverRetire.setToolTipText(resourceMap.getString("chkFoundersNeverRetire.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkFoundersNeverRetire, gridBagConstraints);

        chkAddDependents = new JCheckBox(resourceMap.getString("chkAddDependents.text"));
        chkAddDependents.setToolTipText(resourceMap.getString("chkAddDependents.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkAddDependents, gridBagConstraints);

        chkDependentsNeverLeave = new JCheckBox(resourceMap.getString("chkDependentsNeverLeave.text"));
        chkDependentsNeverLeave.setToolTipText(resourceMap.getString("chkDependentsNeverLeave.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkDependentsNeverLeave, gridBagConstraints);

        chkTrackUnitFatigue.setText(resourceMap.getString("chkTrackUnitFatigue.text"));
        chkTrackUnitFatigue.setToolTipText(resourceMap.getString("chkTrackUnitFatigue.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkTrackUnitFatigue, gridBagConstraints);

        chkUseLeadership.setText(resourceMap.getString("chkUseLeadership.text"));
        chkUseLeadership.setToolTipText(resourceMap.getString("chkUseLeadership.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseLeadership, gridBagConstraints);

        chkTrackOriginalUnit.setText(resourceMap.getString("chkTrackOriginalUnit.text"));
        chkTrackOriginalUnit.setToolTipText(resourceMap.getString("chkTrackOriginalUnit.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkTrackOriginalUnit, gridBagConstraints);

        chkUseAero.setText(resourceMap.getString("chkUseAero.text"));
        chkUseAero.setToolTipText(resourceMap.getString("chkUseAero.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseAero, gridBagConstraints);

        chkUseVehicles.setText(resourceMap.getString("chkUseVehicles.text"));
        chkUseVehicles.setToolTipText(resourceMap.getString("chkUseVehicles.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkUseVehicles, gridBagConstraints);

        chkClanVehicles.setText(resourceMap.getString("chkClanVehicles.text"));
        chkClanVehicles.setToolTipText(resourceMap.getString("chkClanVehicles.toolTipText"));
        gridBagConstraints.gridy++;
        panSubAtBAdmin.add(chkClanVehicles, gridBagConstraints);

        JLabel lblSearchRadius = new JLabel(resourceMap.getString("lblSearchRadius.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panSubAtBContract.add(lblSearchRadius, gridBagConstraints);

        spnSearchRadius.setModel(new SpinnerNumberModel(300, 100, 2500, 100));
        spnSearchRadius.setToolTipText(resourceMap.getString("spnSearchRadius.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        panSubAtBContract.add(spnSearchRadius, gridBagConstraints);

        chkVariableContractLength.setText(resourceMap.getString("chkVariableContractLength.text"));
        chkVariableContractLength.setToolTipText(resourceMap.getString("chkVariableContractLength.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkVariableContractLength, gridBagConstraints);

        chkMercSizeLimited.setText(resourceMap.getString("chkMercSizeLimited.text"));
        chkMercSizeLimited.setToolTipText(resourceMap.getString("chkMercSizeLimited.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkMercSizeLimited, gridBagConstraints);

        chkRestrictPartsByMission.setText(resourceMap.getString("chkRestrictPartsByMission.text"));
        chkRestrictPartsByMission.setToolTipText(resourceMap.getString("chkRestrictPartsByMission.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkRestrictPartsByMission, gridBagConstraints);

        chkLimitLanceWeight.setText(resourceMap.getString("chkLimitLanceWeight.text"));
        chkLimitLanceWeight.setToolTipText(resourceMap.getString("chkLimitLanceWeight.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkLimitLanceWeight, gridBagConstraints);

        chkLimitLanceNumUnits.setText(resourceMap.getString("chkLimitLanceNumUnits.text"));
        chkLimitLanceNumUnits.setToolTipText(resourceMap.getString("chkLimitLanceNumUnits.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkLimitLanceNumUnits, gridBagConstraints);

        JLabel lblLanceStructure = new JLabel(resourceMap.getString("lblLanceStructure.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblLanceStructure, gridBagConstraints);

        chkUseStrategy.setText(resourceMap.getString("chkUseStrategy.text"));
        chkUseStrategy.setToolTipText(resourceMap.getString("chkUseStrategy.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkUseStrategy, gridBagConstraints);

        JLabel lblBaseStrategyDeployment = new JLabel(resourceMap.getString("lblBaseStrategyDeployment.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblBaseStrategyDeployment, gridBagConstraints);

        spnBaseStrategyDeployment.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnBaseStrategyDeployment.setToolTipText(resourceMap.getString("spnBaseStrategyDeployment.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        panSubAtBContract.add(spnBaseStrategyDeployment, gridBagConstraints);

        JLabel lblAdditionalStrategyDeployment = new JLabel(resourceMap.getString("lblAdditionalStrategyDeployment.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblAdditionalStrategyDeployment, gridBagConstraints);

        spnAdditionalStrategyDeployment.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnAdditionalStrategyDeployment.setToolTipText(resourceMap.getString("spnAdditionalStrategyDeployment.toolTipText"));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        panSubAtBContract.add(spnAdditionalStrategyDeployment, gridBagConstraints);

        chkAdjustPaymentForStrategy = new JCheckBox(resourceMap.getString("chkAdjustPaymentForStrategy.text"));
        chkAdjustPaymentForStrategy.setName("chkAdjustPaymentForStrategy");
        chkAdjustPaymentForStrategy.setToolTipText(resourceMap.getString("chkAdjustPaymentForStrategy.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(chkAdjustPaymentForStrategy, gridBagConstraints);

        JLabel lblIntensity = new JLabel(resourceMap.getString("lblIntensity.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        panSubAtBContract.add(lblIntensity, gridBagConstraints);

        // Note that spnAtBBattleIntensity is located here visibly, however must be initialized
        // following the chance of battle by role

        JLabel lblBattleFrequency = new JLabel(resourceMap.getString("lblBattleFrequency.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(lblBattleFrequency, gridBagConstraints);

        spnAtBBattleChance = new JSpinner[AtBLanceRole.values().length - 1];

        JLabel lblFightChance = new JLabel(AtBLanceRole.FIGHTING.toString() + ":");
        gridBagConstraints.gridy = 13;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(lblFightChance, gridBagConstraints);

        JSpinner atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblDefendChance = new JLabel(AtBLanceRole.DEFENCE.toString() + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        panSubAtBContract.add(lblDefendChance, gridBagConstraints);

        atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblScoutChance = new JLabel(AtBLanceRole.SCOUTING.toString() + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        panSubAtBContract.add(lblScoutChance, gridBagConstraints);

        atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        JLabel lblTrainingChance = new JLabel(AtBLanceRole.TRAINING.toString() + ":");
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        panSubAtBContract.add(lblTrainingChance, gridBagConstraints);

        atbBattleChance = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()] = atbBattleChance;
        gridBagConstraints.gridx = 1;
        panSubAtBContract.add(atbBattleChance, gridBagConstraints);

        btnIntensityUpdate = new JButton(resourceMap.getString("btnIntensityUpdate.text"));
        AtBBattleIntensityChangeListener atBBattleIntensityChangeListener = new AtBBattleIntensityChangeListener();
        btnIntensityUpdate.addChangeListener(evt -> {
            spnAtBBattleIntensity.removeChangeListener(atBBattleIntensityChangeListener);
            spnAtBBattleIntensity.setValue(determineAtBBattleIntensity());
            spnAtBBattleIntensity.addChangeListener(atBBattleIntensityChangeListener);
        });
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.gridwidth = 2;
        panSubAtBContract.add(btnIntensityUpdate, gridBagConstraints);

        // Note that this must be after the chance by role because it requires the chance by role
        // for the initial value to be calculated
        spnAtBBattleIntensity = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 0.1));
        spnAtBBattleIntensity.setToolTipText(resourceMap.getString("spnIntensity.toolTipText"));
        spnAtBBattleIntensity.addChangeListener(atBBattleIntensityChangeListener);
        spnAtBBattleIntensity.setMinimumSize(new Dimension(60, 25));
        spnAtBBattleIntensity.setPreferredSize(new Dimension(60, 25));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 1;
        panSubAtBContract.add(spnAtBBattleIntensity, gridBagConstraints);

        chkGenerateChases = new JCheckBox(resourceMap.getString("chkGenerateChases.text"));
        chkGenerateChases.setName("chkGenerateChases");
        chkGenerateChases.setToolTipText(resourceMap.getString("chkGenerateChases.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        panSubAtBContract.add(chkGenerateChases, gridBagConstraints);

        int yTablePosition = 0;
        chkDoubleVehicles.setText(resourceMap.getString("chkDoubleVehicles.text"));
        chkDoubleVehicles.setToolTipText(resourceMap.getString("chkDoubleVehicles.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkDoubleVehicles, gridBagConstraints);

        JLabel lblOpforLanceType = new JLabel(resourceMap.getString("lblOpforLanceType.text"));
        lblOpforLanceType.setToolTipText(resourceMap.getString("lblOpforLanceType.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(lblOpforLanceType, gridBagConstraints);

        spnOpforLanceTypeMechs.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpforLanceTypeMechs.setToolTipText(resourceMap.getString("lblOpforLanceType.toolTipText"));
        spnOpforLanceTypeMixed.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpforLanceTypeMixed.setToolTipText(resourceMap.getString("lblOpforLanceType.toolTipText"));
        spnOpforLanceTypeVehicles.setModel(new SpinnerNumberModel(0, 0, 10, 1));
        spnOpforLanceTypeVehicles.setToolTipText(resourceMap.getString("lblOpforLanceType.toolTipText"));
        JPanel panOpforLanceType = new JPanel();
        panOpforLanceType.add(new JLabel(resourceMap.getString("lblOpforLanceTypeMek.text")));
        panOpforLanceType.add(spnOpforLanceTypeMechs);
        panOpforLanceType.add(new JLabel(resourceMap.getString("lblOpforLanceTypeMixed.text")));
        panOpforLanceType.add(spnOpforLanceTypeMixed);
        panOpforLanceType.add(new JLabel(resourceMap.getString("lblOpforLanceTypeVehicle.text")));
        panOpforLanceType.add(spnOpforLanceTypeVehicles);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforLanceType, gridBagConstraints);

        chkOpforUsesVTOLs.setText(resourceMap.getString("chkOpforUsesVTOLs.text"));
        chkOpforUsesVTOLs.setToolTipText(resourceMap.getString("chkOpforUsesVTOLs.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkOpforUsesVTOLs, gridBagConstraints);

        JPanel panOpforAero = new JPanel();
        chkOpforUsesAero.setText(resourceMap.getString("chkOpforUsesAero.text"));
        chkOpforUsesAero.setToolTipText(resourceMap.getString("chkOpforUsesAero.toolTipText"));
        JLabel lblOpforAeroChance = new JLabel(resourceMap.getString("lblOpforAeroLikelihood.text"));
        lblOpforAeroChance.setToolTipText(resourceMap.getString("lblOpforAeroLikelihood.toolTipText"));
        spnOpforAeroChance.setModel(new SpinnerNumberModel(0, 0, 6, 1));
        panOpforAero.add(chkOpforUsesAero);
        panOpforAero.add(spnOpforAeroChance);
        panOpforAero.add(lblOpforAeroChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforAero, gridBagConstraints);

        JPanel panOpforLocal = new JPanel();
        chkOpforUsesLocalForces.setText(resourceMap.getString("chkOpforUsesLocalForces.text"));
        chkOpforUsesLocalForces.setToolTipText(resourceMap.getString("chkOpforUsesLocalForces.toolTipText"));
        JLabel lblOpforLocalForceChance = new JLabel(resourceMap.getString("lblOpforLocalForceLikelihood.text"));
        lblOpforLocalForceChance.setToolTipText(resourceMap.getString("lblOpforLocalForceLikelihood.toolTipText"));
        spnOpforLocalForceChance.setModel(new SpinnerNumberModel(0, 0, 6, 1));
        panOpforLocal.add(chkOpforUsesLocalForces);
        panOpforLocal.add(spnOpforLocalForceChance);
        panOpforLocal.add(lblOpforLocalForceChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panOpforLocal, gridBagConstraints);

        chkAdjustPlayerVehicles.setText(resourceMap.getString("chkAdjustPlayerVehicles.text"));
        chkAdjustPlayerVehicles.setToolTipText(resourceMap.getString("chkAdjustPlayerVehicles.toolTipText"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkAdjustPlayerVehicles, gridBagConstraints);

        chkRegionalMechVariations = new JCheckBox(resourceMap.getString("chkRegionalMechVariations.text"));
        chkRegionalMechVariations.setToolTipText(resourceMap.getString("chkRegionalMechVariations.toolTipText"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkRegionalMechVariations, gridBagConstraints);

        chkAttachedPlayerCamouflage = new JCheckBox(resourceMap.getString("chkAttachedPlayerCamouflage.text"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkAttachedPlayerCamouflage, gridBagConstraints);

        chkPlayerControlsAttachedUnits = new JCheckBox(resourceMap.getString("chkPlayerControlsAttachedUnits.text"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkPlayerControlsAttachedUnits, gridBagConstraints);

        chkUseDropShips = new JCheckBox(resourceMap.getString("chkUseDropShips.text"));
        chkUseDropShips.setToolTipText(resourceMap.getString("chkUseDropShips.toolTipText"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkUseDropShips, gridBagConstraints);

        chkUseWeatherConditions = new JCheckBox(resourceMap.getString("chkUseWeatherConditions.text"));
        chkUseWeatherConditions.setToolTipText(resourceMap.getString("chkUseWeatherConditions.toolTipText"));
        gridBagConstraints.gridy = yTablePosition++;
        panSubAtBScenario.add(chkUseWeatherConditions, gridBagConstraints);

        chkUseLightConditions.setText(resourceMap.getString("chkUseLightConditions.text"));
        chkUseLightConditions.setToolTipText(resourceMap.getString("chkUseLightConditions.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUseLightConditions, gridBagConstraints);

        chkUsePlanetaryConditions.setText(resourceMap.getString("chkUsePlanetaryConditions.text"));
        chkUsePlanetaryConditions.setToolTipText(resourceMap.getString("chkUsePlanetaryConditions.toolTipText"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(chkUsePlanetaryConditions, gridBagConstraints);

        JPanel panFixedMapChance = new JPanel();
        JLabel lblFixedMapChance = new JLabel(resourceMap.getString("lblFixedMapChance.text"));
        lblFixedMapChance.setToolTipText(resourceMap.getString("lblFixedMapChance.toolTipText"));
        spnFixedMapChance.setModel(new SpinnerNumberModel(0, 0, 100, 10));
        panFixedMapChance.add(lblFixedMapChance);
        panFixedMapChance.add(spnFixedMapChance);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = yTablePosition++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(0, 5, 5, 5);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panSubAtBScenario.add(panFixedMapChance, gridBagConstraints);

        JScrollPane scrAtB = new JScrollPane(panAtB);
        scrAtB.setPreferredSize(new java.awt.Dimension(500, 410));

        getOptionsPane().addTab(resourceMap.getString("panAtB.TabConstraints.tabTitle"), scrAtB);
        enableAtBComponents(panAtB, chkUseAtB.isSelected());

        SwingUtilities.invokeLater(() -> {
            scrSPA.getVerticalScrollBar().setValue(0);
            scrAtB.getVerticalScrollBar().setValue(0);
        });
        //endregion Against the Bot Tab

        return getOptionsPane();
    }
    //endregion Center Pane

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 0));

        panel.add(new MMButton("btnOkay", resources, "btnOkay.text", null,
                this::okButtonActionPerformed));

        panel.add(new MMButton("btnSavePreset", resources, "btnSavePreset.text",
                "btnSavePreset.toolTipText", evt -> btnSaveActionPerformed()));

        panel.add(new MMButton("btnLoadPreset", resources, "btnLoadPreset.text",
                "btnLoadPreset.toolTipText", evt -> {
            final CampaignPresetSelectionDialog presetSelectionDialog = new CampaignPresetSelectionDialog(getFrame());
            if (presetSelectionDialog.showDialog().isConfirmed()) {
                applyPreset(presetSelectionDialog.getSelectedPreset());
            }
        }));

        panel.add(new MMButton("btnCancel", resources, "btnCancel.text", null,
                this::cancelActionPerformed));

        return panel;
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JTabbedPanePreference(getOptionsPane()));
    }

    public void applyPreset(final @Nullable CampaignPreset preset) {
        if (preset == null) {
            return;
        }

        if (isStartup()) {
            if (preset.getDate() != null) {
                setDate(preset.getDate());
            }

            if (preset.getFaction() != null) {
                comboFaction.setSelectedItem(new FactionDisplay(preset.getFaction(), date));
            }

            if (preset.getRankSystem() != null) {
                if (preset.getRankSystem().getType().isCampaign()) {
                    rankSystemsPane.getRankSystemModel().addElement(preset.getRankSystem());
                }
                rankSystemsPane.getComboRankSystems().setSelectedItem(preset.getRankSystem());
            }
        }

        // Handle CampaignOptions and RandomSkillPreferences
        if (preset.getCampaignOptions() != null) {
            setOptions(preset.getCampaignOptions(), preset.getRandomSkillPreferences());
        }

        // Handle SPAs
        if (!preset.getSpecialAbilities().isEmpty()) {
            tempSPA = preset.getSpecialAbilities();
            recreateSPAPanel(!getUnusedSPA().isEmpty());
        }

        if (!preset.getSkills().isEmpty()) {
            // Overwriting XP Table
            tableXP.setModel(new DefaultTableModel(getSkillCostsArray(preset.getSkills()), TABLE_XP_COLUMN_NAMES));
            ((DefaultTableModel) tableXP.getModel()).fireTableDataChanged();

            // Overwriting Skill List
            for (final String skillName : SkillType.getSkillList()) {
                final JSpinner spnTarget = hashSkillTargets.get(skillName);
                if (spnTarget == null) {
                    continue;
                }
                final SkillType skillType = preset.getSkills().get(skillName);

                spnTarget.setValue(skillType.getTarget());
                hashGreenSkill.get(skillName).setValue(skillType.getGreenLevel());
                hashRegSkill.get(skillName).setValue(skillType.getRegularLevel());
                hashVetSkill.get(skillName).setValue(skillType.getVeteranLevel());
                hashEliteSkill.get(skillName).setValue(skillType.getEliteLevel());
            }
        }
    }

    public void setOptions(@Nullable CampaignOptions options,
                           @Nullable RandomSkillPreferences randomSkillPreferences) {
        // Use the provided options and preferences when possible, but flip if they are null to be safe
        if (options != null) {
            this.options = options;
        } else {
            options = this.options;
        }

        if (randomSkillPreferences != null) {
            this.rSkillPrefs = randomSkillPreferences;
        } else {
            randomSkillPreferences = this.rSkillPrefs;
        }

        //region General Tab
        unitRatingMethodCombo.setSelectedItem(options.getUnitRatingMethod());
        manualUnitRatingModifier.setValue(options.getManualUnitRatingModifier());
        //endregion General Tab

        //region Repair and Maintenance Tab
        useEraModsCheckBox.setSelected(options.useEraMods());
        assignedTechFirstCheckBox.setSelected(options.useAssignedTechFirst());
        resetToFirstTechCheckBox.setSelected(options.useResetToFirstTech());
        useQuirksBox.setSelected(options.useQuirks());
        useAeroSystemHitsBox.setSelected(options.useAeroSystemHits());
        if (useDamageMargin.isSelected() != options.isDestroyByMargin()) {
            useDamageMargin.doClick();
        }
        spnDamageMargin.setValue(options.getDestroyMargin());
        spnDestroyPartTarget.setValue(options.getDestroyPartTarget());

        if (checkMaintenance.isSelected() != options.checkMaintenance()) {
            checkMaintenance.doClick();
        }
        spnMaintenanceDays.setValue(options.getMaintenanceCycleDays());
        spnMaintenanceBonus.setValue(options.getMaintenanceBonus());
        useQualityMaintenance.setSelected(options.useQualityMaintenance());
        reverseQualityNames.setSelected(options.reverseQualityNames());
        useUnofficialMaintenance.setSelected(options.useUnofficialMaintenance());
        logMaintenance.setSelected(options.logMaintenance());
        //endregion Repair and Maintenance Tab

        //region Supplies and Acquisitions Tab
        spnAcquireWaitingPeriod.setValue(options.getWaitingPeriod());
        choiceAcquireSkill.setSelectedItem(options.getAcquisitionSkill());
        chkSupportStaffOnly.setSelected(options.isAcquisitionSupportStaffOnly());
        spnAcquireClanPenalty.setValue(options.getClanAcquisitionPenalty());
        spnAcquireIsPenalty.setValue(options.getIsAcquisitionPenalty());
        txtMaxAcquisitions.setText(Integer.toString(options.getMaxAcquisitions()));

        spnNDiceTransitTime.setValue(options.getNDiceTransitTime());
        spnConstantTransitTime.setValue(options.getConstantTransitTime());
        choiceTransitTimeUnits.setSelectedItem(CampaignOptions.getTransitUnitName(options.getUnitTransitTime()));
        spnAcquireMinimum.setValue(options.getAcquireMinimumTime());
        choiceAcquireMinimumUnit.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMinimumTimeUnit()));
        spnAcquireMosBonus.setValue(options.getAcquireMosBonus());
        choiceAcquireMosUnits.setSelectedItem(CampaignOptions.getTransitUnitName(options.getAcquireMosUnit()));

        usePlanetaryAcquisitions.setSelected(options.usesPlanetaryAcquisition());
        spnMaxJumpPlanetaryAcquisitions.setValue(options.getMaxJumpsPlanetaryAcquisition());
        comboPlanetaryAcquisitionsFactionLimits.setSelectedItem(options.getPlanetAcquisitionFactionLimit());
        disallowPlanetaryAcquisitionClanCrossover.setSelected(options.disallowPlanetAcquisitionClanCrossover());
        disallowClanPartsFromIS.setSelected(options.disallowClanPartsFromIS());
        spnPenaltyClanPartsFromIS.setValue(options.getPenaltyClanPartsFroIS());
        usePlanetaryAcquisitionsVerbose.setSelected(options.usePlanetAcquisitionVerboseReporting());
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            spnPlanetAcquireTechBonus[i].setValue(options.getPlanetTechAcquisitionBonus(i));
            spnPlanetAcquireIndustryBonus[i].setValue(options.getPlanetIndustryAcquisitionBonus(i));
            spnPlanetAcquireOutputBonus[i].setValue(options.getPlanetOutputAcquisitionBonus(i));
        }
        //endregion Supplies and Acquisitions Tab

        //region Tech Limits Tab
        if (limitByYearBox.isSelected() != options.limitByYear()) {
            limitByYearBox.doClick();
        }
        disallowExtinctStuffBox.setSelected(options.disallowExtinctStuff());
        allowClanPurchasesBox.setSelected(options.allowClanPurchases());
        allowISPurchasesBox.setSelected(options.allowISPurchases());
        allowCanonOnlyBox.setSelected(options.allowCanonOnly());
        allowCanonRefitOnlyBox.setSelected(options.allowCanonRefitOnly());
        choiceTechLevel.setSelectedIndex(options.getTechLevel());
        variableTechLevelBox.setSelected(options.useVariableTechLevel() && options.limitByYear());
        factionIntroDateBox.setSelected(options.useFactionIntroDate());
        useAmmoByTypeBox.setSelected(options.useAmmoByType());
        //endregion Tech Limits Tab

        //region Personnel Tab
        // General Personnel
        chkUseTactics.setSelected(options.useTactics());
        chkUseInitiativeBonus.setSelected(options.useInitiativeBonus());
        chkUseToughness.setSelected(options.useToughness());
        chkUseArtillery.setSelected(options.useArtillery());
        chkUseAbilities.setSelected(options.useAbilities());
        if (chkUseEdge.isSelected() != options.useEdge()) {
            chkUseEdge.doClick();
        }
        chkUseSupportEdge.setSelected(options.useSupportEdge());
        chkUseImplants.setSelected(options.useImplants());
        chkUseAlternativeQualityAveraging.setSelected(options.useAlternativeQualityAveraging());
        chkUseTransfers.setSelected(options.useTransfers());
        chkUseExtendedTOEForceName.setSelected(options.isUseExtendedTOEForceName());
        chkPersonnelLogSkillGain.setSelected(options.isPersonnelLogSkillGain());
        chkPersonnelLogAbilityGain.setSelected(options.isPersonnelLogAbilityGain());
        chkPersonnelLogEdgeGain.setSelected(options.isPersonnelLogEdgeGain());

        // Expanded Personnel Information
        if (chkUseTimeInService.isSelected() != options.getUseTimeInService()) {
            chkUseTimeInService.doClick();
        }
        comboTimeInServiceDisplayFormat.setSelectedItem(options.getTimeInServiceDisplayFormat());
        if (chkUseTimeInRank.isSelected() != options.getUseTimeInRank()) {
            chkUseTimeInRank.doClick();
        }
        comboTimeInRankDisplayFormat.setSelectedItem(options.getTimeInRankDisplayFormat());
        chkUseRetirementDateTracking.setSelected(options.useRetirementDateTracking());
        chkTrackTotalEarnings.setSelected(options.isTrackTotalEarnings());
        chkTrackTotalXPEarnings.setSelected(options.isTrackTotalXPEarnings());
        chkShowOriginFaction.setSelected(options.showOriginFaction());

        // Medical
        chkUseAdvancedMedical.setSelected(options.useAdvancedMedical());
        spnHealWaitingPeriod.setValue(options.getHealingWaitingPeriod());
        spnNaturalHealWaitingPeriod.setValue(options.getNaturalHealingWaitingPeriod());
        spnMinimumHitsForVehicles.setValue(options.getMinimumHitsForVehicles());
        chkUseRandomHitsForVehicles.setSelected(options.useRandomHitsForVehicles());
        chkUseTougherHealing.setSelected(options.useTougherHealing());

        // Prisoners
        comboPrisonerCaptureStyle.setSelectedItem(options.getPrisonerCaptureStyle());
        comboPrisonerStatus.setSelectedItem(options.getDefaultPrisonerStatus());
        chkPrisonerBabyStatus.setSelected(options.getPrisonerBabyStatus());
        chkAtBPrisonerDefection.setSelected(options.useAtBPrisonerDefection());
        chkAtBPrisonerRansom.setSelected(options.useAtBPrisonerRansom());

        // Personnel Randomization
        chkUseDylansRandomXP.setSelected(options.useDylansRandomXP());
        if (chkRandomizeOrigin.isSelected() != options.randomizeOrigin()) {
            chkRandomizeOrigin.doClick();
        }
        chkRandomizeDependentsOrigin.setSelected(options.getRandomizeDependentOrigin());
        spnOriginSearchRadius.setValue(options.getOriginSearchRadius());
        chkExtraRandomOrigin.setSelected(options.extraRandomOrigin());
        spnOriginDistanceScale.setValue(options.getOriginDistanceScale());

        // Family
        comboDisplayFamilyLevel.setSelectedItem(options.getDisplayFamilyLevel());

        // Salary
        spnCommissionedSalary.setValue(options.getSalaryCommissionMultiplier());
        spnEnlistedSalary.setValue(options.getSalaryEnlistedMultiplier());
        spnAntiMekSalary.setValue(options.getSalaryAntiMekMultiplier());
        spnSpecialistInfantrySalary.setValue(options.getSalarySpecialistInfantryMultiplier());
        for (int i = 0; i < spnSalaryExperienceMultipliers.length; i++) {
            spnSalaryExperienceMultipliers[i].setValue(options.getSalaryXPMultiplier(i));
        }
        for (int i = 0; i < spnBaseSalary.length; i++) {
            spnBaseSalary[i].setValue(options.getRoleBaseSalaries()[i].getAmount().doubleValue());
        }

        // Marriage
        chkUseManualMarriages.setSelected(options.isUseManualMarriages());
        chkUseClannerMarriages.setSelected(options.isUseClannerMarriages());
        chkUsePrisonerMarriages.setSelected(options.isUsePrisonerMarriages());
        spnMinimumMarriageAge.setValue(options.getMinimumMarriageAge());
        spnCheckMutualAncestorsDepth.setValue(options.getCheckMutualAncestorsDepth());
        chkLogMarriageNameChanges.setSelected(options.isLogMarriageNameChanges());
        for (final Map.Entry<MergingSurnameStyle, JSpinner> entry : spnMarriageSurnameWeights.entrySet()) {
            entry.getValue().setValue(options.getMarriageSurnameWeights().get(entry.getKey()) / 10.0);
        }
        comboRandomMarriageMethod.setSelectedItem(options.getRandomMarriageMethod());
        if (chkUseRandomSameSexMarriages.isSelected() != options.isUseRandomSameSexMarriages()) {
            if (chkUseRandomSameSexMarriages.isEnabled()) {
                chkUseRandomSameSexMarriages.doClick();
            } else {
                chkUseRandomSameSexMarriages.setSelected(options.isUseRandomSameSexMarriages());
            }
        }
        chkUseRandomClannerMarriages.setSelected(options.isUseRandomClannerMarriages());
        chkUseRandomPrisonerMarriages.setSelected(options.isUseRandomPrisonerMarriages());
        spnRandomMarriageAgeRange.setValue(options.getRandomMarriageAgeRange());
        spnPercentageRandomMarriageOppositeSexChance.setValue(options.getPercentageRandomMarriageOppositeSexChance() * 100.0);
        spnPercentageRandomMarriageSameSexChance.setValue(options.getPercentageRandomMarriageSameSexChance() * 100.0);

        // Divorce
        chkUseManualDivorce.setSelected(options.isUseManualDivorce());
        chkUseClannerDivorce.setSelected(options.isUseClannerDivorce());
        chkUsePrisonerDivorce.setSelected(options.isUsePrisonerDivorce());
        for (final Map.Entry<SplittingSurnameStyle, JSpinner> entry : spnDivorceSurnameWeights.entrySet()) {
            entry.getValue().setValue(options.getDivorceSurnameWeights().get(entry.getKey()) / 10.0);
        }
        comboRandomDivorceMethod.setSelectedItem(options.getRandomDivorceMethod());
        if (chkUseRandomOppositeSexDivorce.isSelected() != options.isUseRandomOppositeSexDivorce()) {
            if (chkUseRandomOppositeSexDivorce.isEnabled()) {
                chkUseRandomOppositeSexDivorce.doClick();
            } else {
                chkUseRandomOppositeSexDivorce.setSelected(options.isUseRandomOppositeSexDivorce());
            }
        }

        if (chkUseRandomSameSexDivorce.isSelected() != options.isUseRandomSameSexDivorce()) {
            if (chkUseRandomSameSexDivorce.isEnabled()) {
                chkUseRandomSameSexDivorce.doClick();
            } else {
                chkUseRandomSameSexDivorce.setSelected(options.isUseRandomSameSexDivorce());
            }
        }
        chkUseRandomClannerDivorce.setSelected(options.isUseRandomClannerDivorce());
        chkUseRandomPrisonerDivorce.setSelected(options.isUseRandomPrisonerDivorce());
        spnPercentageRandomDivorceOppositeSexChance.setValue(options.getPercentageRandomDivorceOppositeSexChance() * 100.0);
        spnPercentageRandomDivorceSameSexChance.setValue(options.getPercentageRandomDivorceSameSexChance() * 100.0);

        // Procreation
        chkUseManualProcreation.setSelected(options.isUseManualProcreation());
        chkUseClannerProcreation.setSelected(options.isUseClannerProcreation());
        chkUsePrisonerProcreation.setSelected(options.isUsePrisonerProcreation());
        spnMultiplePregnancyOccurrences.setValue(options.getMultiplePregnancyOccurrences());
        comboBabySurnameStyle.setSelectedItem(options.getBabySurnameStyle());
        chkAssignNonPrisonerBabiesFounderTag.setSelected(options.isAssignNonPrisonerBabiesFounderTag());
        chkAssignChildrenOfFoundersFounderTag.setSelected(options.isAssignChildrenOfFoundersFounderTag());
        chkDetermineFatherAtBirth.setSelected(options.isDetermineFatherAtBirth());
        chkDisplayTrueDueDate.setSelected(options.isDisplayTrueDueDate());
        chkLogProcreation.setSelected(options.isLogProcreation());
        comboRandomProcreationMethod.setSelectedItem(options.getRandomProcreationMethod());
        if (chkUseRelationshiplessRandomProcreation.isSelected() != options.isUseRelationshiplessRandomProcreation()) {
            if (chkUseRelationshiplessRandomProcreation.isEnabled()) {
                chkUseRelationshiplessRandomProcreation.doClick();
            } else {
                chkUseRelationshiplessRandomProcreation.setSelected(options.isUseRelationshiplessRandomProcreation());
            }
        }
        chkUseRandomClannerProcreation.setSelected(options.isUseRandomClannerProcreation());
        chkUseRandomPrisonerProcreation.setSelected(options.isUseRandomPrisonerProcreation());
        spnPercentageRandomProcreationRelationshipChance.setValue(options.getPercentageRandomProcreationRelationshipChance() * 100.0);
        spnPercentageRandomProcreationRelationshiplessChance.setValue(options.getPercentageRandomProcreationRelationshiplessChance() * 100.0);

        // Death
        chkKeepMarriedNameUponSpouseDeath.setSelected(options.getKeepMarriedNameUponSpouseDeath());
        //endregion Personnel Tab

        //region Finances Tab
        payForPartsBox.setSelected(options.payForParts());
        payForRepairsBox.setSelected(options.payForRepairs());
        payForUnitsBox.setSelected(options.payForUnits());
        payForSalariesBox.setSelected(options.payForSalaries());
        payForOverheadBox.setSelected(options.payForOverhead());
        payForMaintainBox.setSelected(options.payForMaintain());
        payForTransportBox.setSelected(options.payForTransport());
        sellUnitsBox.setSelected(options.canSellUnits());
        sellPartsBox.setSelected(options.canSellParts());
        payForRecruitmentBox.setSelected(options.payForRecruitment());
        useLoanLimitsBox.setSelected(options.useLoanLimits());
        usePercentageMaintBox.setSelected(options.usePercentageMaint());
        useInfantryDontCountBox.setSelected(options.useInfantryDontCount());
        usePeacetimeCostBox.setSelected(options.usePeacetimeCost());
        useExtendedPartsModifierBox.setSelected(options.useExtendedPartsModifier());
        showPeacetimeCostBox.setSelected(options.showPeacetimeCost());
        comboFinancialYearDuration.setSelectedItem(options.getFinancialYearDuration());
        newFinancialYearFinancesToCSVExportBox.setSelected(options.getNewFinancialYearFinancesToCSVExport());

        // Price Multipliers
        spnCommonPartPriceMultiplier.setValue(options.getCommonPartPriceMultiplier());
        spnInnerSphereUnitPriceMultiplier.setValue(options.getInnerSphereUnitPriceMultiplier());
        spnInnerSpherePartPriceMultiplier.setValue(options.getInnerSpherePartPriceMultiplier());
        spnClanUnitPriceMultiplier.setValue(options.getClanUnitPriceMultiplier());
        spnClanPartPriceMultiplier.setValue(options.getClanPartPriceMultiplier());
        spnMixedTechUnitPriceMultiplier.setValue(options.getMixedTechUnitPriceMultiplier());
        for (int i = 0; i < spnUsedPartPriceMultipliers.length; i++) {
            spnUsedPartPriceMultipliers[i].setValue(options.getUsedPartPriceMultipliers()[i]);
        }
        spnDamagedPartsValueMultiplier.setValue(options.getDamagedPartsValueMultiplier());
        spnUnrepairablePartsValueMultiplier.setValue(options.getUnrepairablePartsValueMultiplier());
        spnCancelledOrderRefundMultiplier.setValue(options.getCancelledOrderRefundMultiplier());
        //endregion Finances Tab

        //region Mercenary Tab
        if (options.useEquipmentContractBase()) {
            btnContractEquipment.setSelected(true);
        } else {
            btnContractPersonnel.setSelected(true);
        }
        spnEquipPercent.setValue(options.getEquipmentContractPercent());
        spnDropshipPercent.setValue(options.getDropshipContractPercent());
        spnJumpshipPercent.setValue(options.getJumpshipContractPercent());
        spnWarshipPercent.setValue(options.getWarshipContractPercent());
        chkEquipContractSaleValue.setSelected(options.useEquipmentContractSaleValue());
        chkBLCSaleValue.setSelected(options.useBLCSaleValue());
        chkOverageRepaymentInFinalPayment.setSelected(options.getOverageRepaymentInFinalPayment());
        //endregion Mercenary Tab

        //region Experience Tab
        spnScenarioXP.setValue(options.getScenarioXP());
        spnKillXP.setValue(options.getKillXPAward());
        spnKills.setValue(options.getKillsForXP());
        spnTaskXP.setValue(options.getTaskXP());
        spnNTasksXP.setValue(options.getNTasksXP());
        spnSuccessXP.setValue(options.getSuccessXP());
        spnMistakeXP.setValue(options.getMistakeXP());
        spnIdleXP.setValue(options.getIdleXP());
        spnMonthsIdleXP.setValue(options.getMonthsIdleXP());
        spnTargetIdleXP.setValue(options.getTargetIdleXP());
        spnContractNegotiationXP.setValue(options.getContractNegotiationXP());
        spnAdminWeeklyXP.setValue(options.getAdminXP());
        spnAdminWeeklyXPPeriod.setValue(options.getAdminXPPeriod());
        spnEdgeCost.setValue(options.getEdgeCost());
        //endregion Experience Tab

        //region Skills Tab
        //endregion Skills Tab

        //region Special Abilities Tab
        //endregion Special Abilities Tab

        //region Skill Randomization Tab
        chkExtraRandom.setSelected(randomSkillPreferences.randomizeSkill());
        final int[] phenotypeProbabilities = options.getPhenotypeProbabilities();
        for (int i = 0; i < phenotypeSpinners.length; i++) {
            phenotypeSpinners[i].setValue(phenotypeProbabilities[i]);
        }
        spnProbAntiMek.setValue(rSkillPrefs.getAntiMekProb());
        spnOverallRecruitBonus.setValue(rSkillPrefs.getOverallRecruitBonus());
        for (int i = 0; i < spnTypeRecruitBonus.length; i++) {
            spnTypeRecruitBonus[i].setValue(rSkillPrefs.getRecruitBonuses()[i]);
        }
        spnArtyProb.setValue(rSkillPrefs.getArtilleryProb());
        spnArtyBonus.setValue(rSkillPrefs.getArtilleryBonus());
        spnSecondProb.setValue(rSkillPrefs.getSecondSkillProb());
        spnSecondBonus.setValue(rSkillPrefs.getSecondSkillBonus());
        spnTacticsGreen.setValue(rSkillPrefs.getTacticsMod(SkillType.EXP_GREEN));
        spnTacticsReg.setValue(rSkillPrefs.getTacticsMod(SkillType.EXP_REGULAR));
        spnTacticsVet.setValue(rSkillPrefs.getTacticsMod(SkillType.EXP_VETERAN));
        spnTacticsElite.setValue(rSkillPrefs.getTacticsMod(SkillType.EXP_ELITE));
        spnAbilGreen.setValue(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_GREEN));
        spnAbilReg.setValue(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_REGULAR));
        spnAbilVet.setValue(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_VETERAN));
        spnAbilElite.setValue(rSkillPrefs.getSpecialAbilBonus(SkillType.EXP_ELITE));
        spnCombatSA.setValue(rSkillPrefs.getCombatSmallArmsBonus());
        spnSupportSA.setValue(rSkillPrefs.getSupportSmallArmsBonus());
        //endregion Skill Randomization Tab

        //region Rank System Tab
        //endregion Rank System Tab

        //region Name and Portrait Generation Tab
        if (chkUseOriginFactionForNames.isSelected() != options.useOriginFactionForNames()) {
            chkUseOriginFactionForNames.doClick();
        }

        boolean allSelected = true;
        boolean noneSelected = true;
        final boolean[] usePortraitForRole = options.usePortraitForRoles();
        for (int i = 0; i < chkUsePortrait.length; i++) {
            chkUsePortrait[i].setSelected(usePortraitForRole[i]);
            if (usePortraitForRole[i]) {
                noneSelected = false;
            } else {
                allSelected = false;
            }
        }
        if (allSelected != allPortraitsBox.isSelected()) {
            allPortraitsBox.doClick();
        }

        if (noneSelected != noPortraitsBox.isSelected()) {
            noPortraitsBox.doClick();
        }

        chkAssignPortraitOnRoleChange.setSelected(options.getAssignPortraitOnRoleChange());
        //endregion Name and Portrait Generation Tab

        //region Markets Tab
        comboPersonnelMarketType.setSelectedItem(options.getPersonnelMarketType());
        chkPersonnelMarketReportRefresh.setSelected(options.getPersonnelMarketReportRefresh());
        spnPersonnelMarketRandomEliteRemoval.setValue(options.getPersonnelMarketRandomEliteRemoval());
        spnPersonnelMarketRandomVeteranRemoval.setValue(options.getPersonnelMarketRandomVeteranRemoval());
        spnPersonnelMarketRandomRegularRemoval.setValue(options.getPersonnelMarketRandomRegularRemoval());
        spnPersonnelMarketRandomGreenRemoval.setValue(options.getPersonnelMarketRandomGreenRemoval());
        spnPersonnelMarketRandomUltraGreenRemoval.setValue(options.getPersonnelMarketRandomUltraGreenRemoval());
        spnPersonnelMarketDylansWeight.setValue(options.getPersonnelMarketDylansWeight());

        // Unit Market
        comboUnitMarketMethod.setSelectedItem(options.getUnitMarketMethod());
        chkUnitMarketRegionalMechVariations.setSelected(options.useUnitMarketRegionalMechVariations());
        chkInstantUnitMarketDelivery.setSelected(options.getInstantUnitMarketDelivery());
        chkUnitMarketReportRefresh.setSelected(options.getUnitMarketReportRefresh());

        // Contract Market
        comboContractMarketMethod.setSelectedItem(options.getContractMarketMethod());
        chkContractMarketReportRefresh.setSelected(options.getContractMarketReportRefresh());
        //endregion Markets Tab

        //region RATs Tab
        btnUseRATGenerator.setSelected(!options.isUseStaticRATs());
        if (options.isUseStaticRATs() != btnUseStaticRATs.isSelected()) {
            btnUseStaticRATs.doClick();
        }
        for (final String rat : options.getRATs()) {
            final List<Integer> eras = RATManager.getAllRATCollections().get(rat);
            if (eras != null) {
                final StringBuilder displayName = new StringBuilder(rat);
                if (!eras.isEmpty()) {
                    displayName.append(" (").append(eras.get(0));
                    if (eras.size() > 1) {
                        displayName.append("-").append(eras.get(eras.size() - 1));
                    }
                    displayName.append(")");
                }

                if (availableRATModel.contains(displayName.toString())) {
                    chosenRATModel.addElement(displayName.toString());
                    availableRATModel.removeElement(displayName.toString());
                }
            }
        }
        chkIgnoreRATEra.setSelected(options.isIgnoreRATEra());
        //endregion RATs Tab

        //region Against the Bot Tab
        if (chkUseAtB.isSelected() != options.getUseAtB()) {
            chkUseAtB.doClick();
        }
        chkUseStratCon.setSelected(options.getUseStratCon());
        cbSkillLevel.setSelectedIndex(options.getSkillLevel());

        chkUseShareSystem.setSelected(options.getUseShareSystem());
        chkSharesExcludeLargeCraft.setSelected(options.getSharesExcludeLargeCraft());
        chkSharesForAll.setSelected(options.getSharesForAll());
        chkAeroRecruitsHaveUnits.setSelected(options.getAeroRecruitsHaveUnits());
        chkRetirementRolls.setSelected(options.doRetirementRolls());
        chkCustomRetirementMods.setSelected(options.getCustomRetirementMods());
        chkFoundersNeverRetire.setSelected(options.getFoundersNeverRetire());
        chkAddDependents.setSelected(options.canAtBAddDependents());
        chkDependentsNeverLeave.setSelected(options.getDependentsNeverLeave());
        chkTrackUnitFatigue.setSelected(options.getTrackUnitFatigue());
        chkUseLeadership.setSelected(options.getUseLeadership());
        chkTrackOriginalUnit.setSelected(options.getTrackOriginalUnit());
        chkUseAero.setSelected(options.getUseAero());
        chkUseVehicles.setSelected(options.getUseVehicles());
        chkClanVehicles.setSelected(options.getClanVehicles());

        spnSearchRadius.setValue(options.getSearchRadius());
        chkVariableContractLength.setSelected(options.getVariableContractLength());
        chkMercSizeLimited.setSelected(options.isMercSizeLimited());
        chkRestrictPartsByMission.setSelected(options.getRestrictPartsByMission());
        chkLimitLanceWeight.setSelected(options.getLimitLanceWeight());
        chkLimitLanceNumUnits.setSelected(options.getLimitLanceNumUnits());
        chkUseStrategy.setSelected(options.getUseStrategy());
        spnBaseStrategyDeployment.setValue(options.getBaseStrategyDeployment());
        spnAdditionalStrategyDeployment.setValue(options.getAdditionalStrategyDeployment());
        chkAdjustPaymentForStrategy.setSelected(options.getAdjustPaymentForStrategy());
        spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(options.getAtBBattleChance(AtBLanceRole.FIGHTING));
        spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(options.getAtBBattleChance(AtBLanceRole.DEFENCE));
        spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].setValue(options.getAtBBattleChance(AtBLanceRole.SCOUTING));
        spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].setValue(options.getAtBBattleChance(AtBLanceRole.TRAINING));
        btnIntensityUpdate.doClick();
        chkGenerateChases.setSelected(options.generateChases());

        chkDoubleVehicles.setSelected(options.getDoubleVehicles());
        spnOpforLanceTypeMechs.setValue(options.getOpforLanceTypeMechs());
        spnOpforLanceTypeMixed.setValue(options.getOpforLanceTypeMixed());
        spnOpforLanceTypeVehicles.setValue(options.getOpforLanceTypeVehicles());
        chkOpforUsesVTOLs.setSelected(options.getOpforUsesVTOLs());
        chkOpforUsesAero.setSelected(options.getAllowOpforAeros());
        spnOpforAeroChance.setValue(options.getOpforAeroChance());
        chkOpforUsesLocalForces.setSelected(options.getAllowOpforLocalUnits());
        spnOpforLocalForceChance.setValue(options.getOpforLocalUnitChance());
        chkAdjustPlayerVehicles.setSelected(options.getAdjustPlayerVehicles());
        spnFixedMapChance.setValue(options.getFixedMapChance());
        chkRegionalMechVariations.setSelected(options.getRegionalMechVariations());
        chkAttachedPlayerCamouflage.setSelected(options.getAttachedPlayerCamouflage());
        chkPlayerControlsAttachedUnits.setSelected(options.getPlayerControlsAttachedUnits());
        chkUseDropShips.setSelected(options.getUseDropShips());
        chkUseWeatherConditions.setSelected(options.getUseWeatherConditions());
        chkUseLightConditions.setSelected(options.getUseLightConditions());
        chkUsePlanetaryConditions.setSelected(options.getUsePlanetaryConditions());
        //endregion Against the Bot Tab
    }

    public static String[][] getSkillCostsArray(Hashtable<String, SkillType> skillHash) {
        String[][] array = new String[SkillType.getSkillList().length][11];
        int i = 0;
        for (String name : SkillType.getSkillList()) {
            SkillType type = skillHash.get(name);
            for (int j = 0; j < 11; j++) {
                array[i][j] = Integer.toString(type.getCost(j));
            }
            i++;
        }
        return array;
    }
    //endregion Initialization

    private void updateOptions() {
        try {
            campaign.setName(txtName.getText());
            if (isStartup()) {
                getCampaign().getForces().setName(getCampaign().getName());
            }
            campaign.setLocalDate(date);
            // Ensure that the MegaMek year GameOption matches the campaign year
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(campaign.getGameYear());
            campaign.setFactionCode(comboFaction.getSelectedItem().getFaction().getShortName());
            if (null != comboFactionNames.getSelectedItem()) {
                RandomNameGenerator.getInstance().setChosenFaction((String) comboFactionNames.getSelectedItem());
            }
            RandomGenderGenerator.setPercentFemale(sldGender.getValue());
            rankSystemsPane.applyToCampaign();
            campaign.setCamouflage(camouflage);
            campaign.setColour(colour);
            campaign.setUnitIcon(unitIcon);

            for (int i = 0; i < chkUsePortrait.length; i++) {
                options.setUsePortraitForRole(i, chkUsePortrait[i].isSelected());
            }

            updateSkillTypes();
            updateXPCosts();

            // Rules panel
            options.setEraMods(useEraModsCheckBox.isSelected());
            options.setAssignedTechFirst(assignedTechFirstCheckBox.isSelected());
            options.setResetToFirstTech(resetToFirstTechCheckBox.isSelected());
            options.setQuirks(useQuirksBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(useQuirksBox.isSelected());
            options.setUnitRatingMethod((UnitRatingMethod) unitRatingMethodCombo.getSelectedItem());
            options.setManualUnitRatingModifier((Integer) manualUnitRatingModifier.getValue());
            options.setUseOriginFactionForNames(chkUseOriginFactionForNames.isSelected());
            options.setDestroyByMargin(useDamageMargin.isSelected());
            options.setDestroyMargin((Integer) spnDamageMargin.getValue());
            options.setDestroyPartTarget((Integer) spnDestroyPartTarget.getValue());
            options.setUseAeroSystemHits(useAeroSystemHitsBox.isSelected());
            options.setCheckMaintenance(checkMaintenance.isSelected());
            options.setUseQualityMaintenance(useQualityMaintenance.isSelected());
            options.setReverseQualityNames(reverseQualityNames.isSelected());
            options.setUseUnofficialMaintenance(useUnofficialMaintenance.isSelected());
            options.setLogMaintenance(logMaintenance.isSelected());
            options.setMaintenanceBonus((Integer) spnMaintenanceBonus.getValue());
            options.setMaintenanceCycleDays((Integer) spnMaintenanceDays.getValue());
            options.setPayForParts(payForPartsBox.isSelected());
            options.setPayForRepairs(payForRepairsBox.isSelected());
            options.setPayForUnits(payForUnitsBox.isSelected());
            options.setPayForSalaries(payForSalariesBox.isSelected());
            options.setPayForOverhead(payForOverheadBox.isSelected());
            options.setPayForMaintain(payForMaintainBox.isSelected());
            options.setPayForTransport(payForTransportBox.isSelected());
            options.setPayForRecruitment(payForRecruitmentBox.isSelected());
            options.setLoanLimits(useLoanLimitsBox.isSelected());
            options.setUsePercentageMaint(usePercentageMaintBox.isSelected());
            options.setUseInfantryDontCount(useInfantryDontCountBox.isSelected());
            options.setSellUnits(sellUnitsBox.isSelected());
            options.setSellParts(sellPartsBox.isSelected());
            options.setUsePeacetimeCost(usePeacetimeCostBox.isSelected());
            options.setUseExtendedPartsModifier(useExtendedPartsModifierBox.isSelected());
            options.setShowPeacetimeCost(showPeacetimeCostBox.isSelected());
            options.setNewFinancialYearFinancesToCSVExport(newFinancialYearFinancesToCSVExportBox.isSelected());
            options.setFinancialYearDuration((FinancialYearDuration) comboFinancialYearDuration.getSelectedItem());
            options.setAssignPortraitOnRoleChange(chkAssignPortraitOnRoleChange.isSelected());

            options.setEquipmentContractBase(btnContractEquipment.isSelected());
            options.setEquipmentContractPercent((Double) spnEquipPercent.getValue());
            options.setDropshipContractPercent((Double) spnDropshipPercent.getValue());
            options.setJumpshipContractPercent((Double) spnJumpshipPercent.getValue());
            options.setWarshipContractPercent((Double) spnWarshipPercent.getValue());
            options.setEquipmentContractSaleValue(chkEquipContractSaleValue.isSelected());
            options.setBLCSaleValue(chkBLCSaleValue.isSelected());
            options.setOverageRepaymentInFinalPayment(chkOverageRepaymentInFinalPayment.isSelected());

            options.setWaitingPeriod((Integer) spnAcquireWaitingPeriod.getValue());
            options.setAcquisitionSkill((String) choiceAcquireSkill.getSelectedItem());
            options.setAcquisitionSupportStaffOnly(chkSupportStaffOnly.isSelected());
            options.setClanAcquisitionPenalty((Integer) spnAcquireClanPenalty.getValue());
            options.setIsAcquisitionPenalty((Integer) spnAcquireIsPenalty.getValue());
            options.setMaxAcquisitions(Integer.parseInt(txtMaxAcquisitions.getText()));

            options.setNDiceTransitTime((Integer) spnNDiceTransitTime.getValue());
            options.setConstantTransitTime((Integer) spnConstantTransitTime.getValue());
            options.setUnitTransitTime(choiceTransitTimeUnits.getSelectedIndex());
            options.setAcquireMosBonus((Integer) spnAcquireMosBonus.getValue());
            options.setAcquireMinimumTime((Integer) spnAcquireMinimum.getValue());
            options.setAcquireMinimumTimeUnit(choiceAcquireMinimumUnit.getSelectedIndex());
            options.setAcquireMosUnit(choiceAcquireMosUnits.getSelectedIndex());
            options.setPlanetaryAcquisition(usePlanetaryAcquisitions.isSelected());
            options.setDisallowClanPartsFromIS(disallowClanPartsFromIS.isSelected());
            options.setPlanetAcquisitionVerboseReporting(usePlanetaryAcquisitionsVerbose.isSelected());
            options.setDisallowPlanetAcquisitionClanCrossover(disallowPlanetaryAcquisitionClanCrossover.isSelected());
            options.setMaxJumpsPlanetaryAcquisition((int) spnMaxJumpPlanetaryAcquisitions.getValue());
            options.setPenaltyClanPartsFroIS((int) spnPenaltyClanPartsFromIS.getValue());
            options.setPlanetAcquisitionFactionLimit((PlanetaryAcquisitionFactionLimit) comboPlanetaryAcquisitionsFactionLimits.getSelectedItem());

            for (int i = ITechnology.RATING_A; i <= ITechnology.RATING_F; i++) {
                options.setPlanetTechAcquisitionBonus((int) spnPlanetAcquireTechBonus[i].getValue(), i);
                options.setPlanetIndustryAcquisitionBonus((int) spnPlanetAcquireIndustryBonus[i].getValue(), i);
                options.setPlanetOutputAcquisitionBonus((int) spnPlanetAcquireOutputBonus[i].getValue(), i);

            }

            options.setScenarioXP((Integer) spnScenarioXP.getValue());
            options.setKillsForXP((Integer) spnKills.getValue());
            options.setKillXPAward((Integer) spnKillXP.getValue());

            options.setTaskXP((Integer) spnTaskXP.getValue());
            options.setNTasksXP((Integer) spnNTasksXP.getValue());
            options.setSuccessXP((Integer) spnSuccessXP.getValue());
            options.setMistakeXP((Integer) spnMistakeXP.getValue());
            options.setIdleXP((Integer) spnIdleXP.getValue());
            options.setMonthsIdleXP((Integer) spnMonthsIdleXP.getValue());
            options.setContractNegotiationXP((Integer) spnContractNegotiationXP.getValue());
            options.setAdminXP((Integer) spnAdminWeeklyXP.getValue());
            options.setAdminXPPeriod((Integer) spnAdminWeeklyXPPeriod.getValue());
            options.setEdgeCost((Integer) spnEdgeCost.getValue());
            options.setTargetIdleXP((Integer) spnTargetIdleXP.getValue());

            options.setLimitByYear(limitByYearBox.isSelected());
            options.setDisallowExtinctStuff(disallowExtinctStuffBox.isSelected());
            options.setAllowClanPurchases(allowClanPurchasesBox.isSelected());
            options.setAllowISPurchases(allowISPurchasesBox.isSelected());
            options.setAllowCanonOnly(allowCanonOnlyBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_CANON_ONLY).setValue(allowCanonOnlyBox.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_ERA_BASED).setValue(variableTechLevelBox.isSelected());
            options.setVariableTechLevel(variableTechLevelBox.isSelected() && options.limitByYear());
            options.setFactionIntroDate(factionIntroDateBox.isSelected());
            campaign.updateTechFactionCode();
            options.setAllowCanonRefitOnly(allowCanonRefitOnlyBox.isSelected());
            options.setUseAmmoByType(useAmmoByTypeBox.isSelected());
            options.setTechLevel(choiceTechLevel.getSelectedIndex());
            campaign.getGameOptions().getOption(OptionsConstants.ALLOWED_TECHLEVEL).setValue((String) choiceTechLevel.getSelectedItem());

            rSkillPrefs.setOverallRecruitBonus((Integer) spnOverallRecruitBonus.getValue());
            for (int i = 0; i < spnTypeRecruitBonus.length; i++) {
                rSkillPrefs.setRecruitBonus(i, (Integer) spnTypeRecruitBonus[i].getValue());
            }
            rSkillPrefs.setRandomizeSkill(chkExtraRandom.isSelected());
            rSkillPrefs.setAntiMekProb((Integer) spnProbAntiMek.getValue());
            rSkillPrefs.setArtilleryProb((Integer) spnArtyProb.getValue());
            rSkillPrefs.setArtilleryBonus((Integer) spnArtyBonus.getValue());
            rSkillPrefs.setSecondSkillProb((Integer) spnSecondProb.getValue());
            rSkillPrefs.setSecondSkillBonus((Integer) spnSecondBonus.getValue());
            rSkillPrefs.setTacticsMod(SkillType.EXP_GREEN, (Integer) spnTacticsGreen.getValue());
            rSkillPrefs.setTacticsMod(SkillType.EXP_REGULAR, (Integer) spnTacticsReg.getValue());
            rSkillPrefs.setTacticsMod(SkillType.EXP_VETERAN, (Integer) spnTacticsVet.getValue());
            rSkillPrefs.setTacticsMod(SkillType.EXP_ELITE, (Integer) spnTacticsElite.getValue());
            rSkillPrefs.setCombatSmallArmsBonus((Integer) spnCombatSA.getValue());
            rSkillPrefs.setSupportSmallArmsBonus((Integer) spnSupportSA.getValue());
            rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_GREEN, (Integer) spnAbilGreen.getValue());
            rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_REGULAR, (Integer) spnAbilReg.getValue());
            rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_VETERAN, (Integer) spnAbilVet.getValue());
            rSkillPrefs.setSpecialAbilBonus(SkillType.EXP_ELITE, (Integer) spnAbilElite.getValue());
            campaign.setRandomSkillPreferences(rSkillPrefs);

            for (int i = 0; i < phenotypeSpinners.length; i++) {
                options.setPhenotypeProbability(i, (Integer) phenotypeSpinners[i].getValue());
            }

            //region Personnel Tab
            // General Personnel
            options.setUseTactics(chkUseTactics.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_COMMAND_INIT).setValue(chkUseTactics.isSelected());
            options.setUseInitiativeBonus(chkUseInitiativeBonus.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).setValue(chkUseInitiativeBonus.isSelected());
            options.setUseToughness(chkUseToughness.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_TOUGHNESS).setValue(chkUseToughness.isSelected());
            options.setUseArtillery(chkUseArtillery.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_ARTILLERY_SKILL).setValue(chkUseArtillery.isSelected());
            options.setUseAbilities(chkUseAbilities.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_PILOT_ADVANTAGES).setValue(chkUseAbilities.isSelected());
            options.setUseEdge(chkUseEdge.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.EDGE).setValue(chkUseEdge.isSelected());
            options.setUseSupportEdge(chkUseEdge.isSelected() && chkUseSupportEdge.isSelected());
            options.setUseImplants(chkUseImplants.isSelected());
            campaign.getGameOptions().getOption(OptionsConstants.RPG_MANEI_DOMINI).setValue(chkUseImplants.isSelected());
            options.setAlternativeQualityAveraging(chkUseAlternativeQualityAveraging.isSelected());
            options.setUseTransfers(chkUseTransfers.isSelected());
            options.setUseExtendedTOEForceName(chkUseExtendedTOEForceName.isSelected());
            options.setPersonnelLogSkillGain(chkPersonnelLogSkillGain.isSelected());
            options.setPersonnelLogAbilityGain(chkPersonnelLogAbilityGain.isSelected());
            options.setPersonnelLogEdgeGain(chkPersonnelLogEdgeGain.isSelected());

            // Expanded Personnel Information
            options.setUseTimeInService(chkUseTimeInService.isSelected());
            options.setTimeInServiceDisplayFormat((TimeInDisplayFormat) comboTimeInServiceDisplayFormat.getSelectedItem());
            options.setUseTimeInRank(chkUseTimeInRank.isSelected());
            options.setTimeInRankDisplayFormat((TimeInDisplayFormat) comboTimeInRankDisplayFormat.getSelectedItem());
            options.setUseRetirementDateTracking(chkUseRetirementDateTracking.isSelected());
            options.setTrackTotalEarnings(chkTrackTotalEarnings.isSelected());
            options.setTrackTotalXPEarnings(chkTrackTotalXPEarnings.isSelected());
            options.setShowOriginFaction(chkShowOriginFaction.isSelected());

            // Medical
            options.setUseAdvancedMedical(chkUseAdvancedMedical.isSelected());
            // we need to reset healing time options through the campaign because we may need to
            // loop through personnel to make adjustments
            campaign.setHealingTimeOptions((Integer) spnHealWaitingPeriod.getValue(),
                    (Integer) spnNaturalHealWaitingPeriod.getValue());
            options.setMinimumHitsForVehicles((Integer) spnMinimumHitsForVehicles.getValue());
            options.setUseRandomHitsForVehicles(chkUseRandomHitsForVehicles.isSelected());
            options.setTougherHealing(chkUseTougherHealing.isSelected());

            // Prisoners
            options.setPrisonerCaptureStyle((PrisonerCaptureStyle) comboPrisonerCaptureStyle.getSelectedItem());
            options.setDefaultPrisonerStatus((PrisonerStatus) comboPrisonerStatus.getSelectedItem());
            options.setPrisonerBabyStatus(chkPrisonerBabyStatus.isSelected());
            options.setUseAtBPrisonerDefection(chkAtBPrisonerDefection.isSelected());
            options.setUseAtBPrisonerRansom(chkAtBPrisonerRansom.isSelected());

            // Personnel Randomization
            options.setUseDylansRandomXP(chkUseDylansRandomXP.isSelected());
            options.setRandomizeOrigin(chkRandomizeOrigin.isSelected());
            options.setRandomizeDependentOrigin(chkRandomizeDependentsOrigin.isSelected());
            options.setOriginSearchRadius((Integer) spnOriginSearchRadius.getValue());
            options.setExtraRandomOrigin(chkExtraRandomOrigin.isSelected());
            options.setOriginDistanceScale((Double) spnOriginDistanceScale.getValue());

            // Family
            options.setDisplayFamilyLevel((FamilialRelationshipDisplayLevel) comboDisplayFamilyLevel.getSelectedItem());

            // Salary
            options.setSalaryCommissionMultiplier((Double) spnCommissionedSalary.getValue());
            options.setSalaryEnlistedMultiplier((Double) spnEnlistedSalary.getValue());
            options.setSalaryAntiMekMultiplier((Double) spnAntiMekSalary.getValue());
            options.setSalarySpecialistInfantryMultiplier((Double) spnSpecialistInfantrySalary.getValue());
            for (int i = 0; i < spnSalaryExperienceMultipliers.length; i++) {
                options.setSalaryXPMultiplier(i, (Double) spnSalaryExperienceMultipliers[i].getValue());
            }
            for (final PersonnelRole personnelRole : PersonnelRole.values()) {
                options.setRoleBaseSalary(personnelRole, (double) spnBaseSalary[personnelRole.ordinal()].getValue());
            }

            // Marriage
            options.setUseManualMarriages(chkUseManualMarriages.isSelected());
            options.setUseClannerMarriages(chkUseClannerMarriages.isSelected());
            options.setUsePrisonerMarriages(chkUsePrisonerMarriages.isSelected());
            options.setMinimumMarriageAge((Integer) spnMinimumMarriageAge.getValue());
            options.setCheckMutualAncestorsDepth((Integer) spnCheckMutualAncestorsDepth.getValue());
            options.setLogMarriageNameChanges(chkLogMarriageNameChanges.isSelected());
            for (final Map.Entry<MergingSurnameStyle, JSpinner> entry : spnMarriageSurnameWeights.entrySet()) {
                options.getMarriageSurnameWeights().put(entry.getKey(), (int) Math.round((Double) entry.getValue().getValue() * 10.0));
            }
            options.setRandomMarriageMethod(comboRandomMarriageMethod.getSelectedItem());
            options.setUseRandomSameSexMarriages(chkUseRandomSameSexMarriages.isSelected());
            options.setUseRandomClannerMarriages(chkUseRandomClannerMarriages.isSelected());
            options.setUseRandomPrisonerMarriages(chkUseRandomPrisonerMarriages.isSelected());
            options.setRandomMarriageAgeRange((Integer) spnRandomMarriageAgeRange.getValue());
            options.setPercentageRandomMarriageOppositeSexChance((Double) spnPercentageRandomMarriageOppositeSexChance.getValue() / 100.0);
            options.setPercentageRandomMarriageSameSexChance((Double) spnPercentageRandomMarriageSameSexChance.getValue() / 100.0);

            // Divorce
            options.setUseManualDivorce(chkUseManualDivorce.isSelected());
            options.setUseClannerDivorce(chkUseClannerDivorce.isSelected());
            options.setUsePrisonerDivorce(chkUsePrisonerDivorce.isSelected());
            for (final Map.Entry<SplittingSurnameStyle, JSpinner> entry : spnDivorceSurnameWeights.entrySet()) {
                options.getDivorceSurnameWeights().put(entry.getKey(), (int) Math.round((Double) entry.getValue().getValue() * 10.0));
            }
            options.setRandomDivorceMethod(comboRandomDivorceMethod.getSelectedItem());
            options.setUseRandomOppositeSexDivorce(chkUseRandomOppositeSexDivorce.isSelected());
            options.setUseRandomSameSexDivorce(chkUseRandomSameSexDivorce.isSelected());
            options.setUseRandomClannerDivorce(chkUseRandomClannerDivorce.isSelected());
            options.setUseRandomPrisonerDivorce(chkUseRandomPrisonerDivorce.isSelected());
            options.setPercentageRandomDivorceOppositeSexChance((Double) spnPercentageRandomDivorceOppositeSexChance.getValue() / 100.0);
            options.setPercentageRandomDivorceSameSexChance((Double) spnPercentageRandomDivorceSameSexChance.getValue() / 100.0);

            // Procreation
            options.setUseManualProcreation(chkUseManualProcreation.isSelected());
            options.setUseClannerProcreation(chkUseClannerProcreation.isSelected());
            options.setUsePrisonerProcreation(chkUsePrisonerProcreation.isSelected());
            options.setMultiplePregnancyOccurrences((Integer) spnMultiplePregnancyOccurrences.getValue());
            options.setBabySurnameStyle(comboBabySurnameStyle.getSelectedItem());
            options.setAssignNonPrisonerBabiesFounderTag(chkAssignNonPrisonerBabiesFounderTag.isSelected());
            options.setAssignChildrenOfFoundersFounderTag(chkAssignChildrenOfFoundersFounderTag.isSelected());
            options.setDetermineFatherAtBirth(chkDetermineFatherAtBirth.isSelected());
            options.setDisplayTrueDueDate(chkDisplayTrueDueDate.isSelected());
            options.setLogProcreation(chkLogProcreation.isSelected());
            options.setRandomProcreationMethod(comboRandomProcreationMethod.getSelectedItem());
            options.setUseRelationshiplessRandomProcreation(chkUseRelationshiplessRandomProcreation.isSelected());
            options.setUseRandomClannerProcreation(chkUseRandomClannerProcreation.isSelected());
            options.setUseRandomPrisonerProcreation(chkUseRandomPrisonerProcreation.isSelected());
            options.setPercentageRandomProcreationRelationshipChance((Double) spnPercentageRandomProcreationRelationshipChance.getValue() / 100.0);
            options.setPercentageRandomProcreationRelationshiplessChance((Double) spnPercentageRandomProcreationRelationshiplessChance.getValue() / 100.0);

            // Death
            options.setKeepMarriedNameUponSpouseDeath(chkKeepMarriedNameUponSpouseDeath.isSelected());
            //endregion Personnel Tab

            //region Finances Tab

            // Price Multipliers
            options.setCommonPartPriceMultiplier((Double) spnCommonPartPriceMultiplier.getValue());
            options.setInnerSphereUnitPriceMultiplier((Double) spnInnerSphereUnitPriceMultiplier.getValue());
            options.setInnerSpherePartPriceMultiplier((Double) spnInnerSpherePartPriceMultiplier.getValue());
            options.setClanUnitPriceMultiplier((Double) spnClanUnitPriceMultiplier.getValue());
            options.setClanPartPriceMultiplier((Double) spnClanPartPriceMultiplier.getValue());
            options.setMixedTechUnitPriceMultiplier((Double) spnMixedTechUnitPriceMultiplier.getValue());
            for (int i = 0; i < spnUsedPartPriceMultipliers.length; i++) {
                options.getUsedPartPriceMultipliers()[i] = (Double) spnUsedPartPriceMultipliers[i].getValue();
            }
            options.setDamagedPartsValueMultiplier((Double) spnDamagedPartsValueMultiplier.getValue());
            options.setUnrepairablePartsValueMultiplier((Double) spnUnrepairablePartsValueMultiplier.getValue());
            options.setCancelledOrderRefundMultiplier((Double) spnCancelledOrderRefundMultiplier.getValue());
            //endregion Finances Tab

            //start SPA
            SpecialAbility.replaceSpecialAbilities(getCurrentSPA());
            //end SPA

            //region Markets Tab
            // Personnel Market
            options.setPersonnelMarketType((String) comboPersonnelMarketType.getSelectedItem());
            options.setPersonnelMarketReportRefresh(chkPersonnelMarketReportRefresh.isSelected());
            options.setPersonnelMarketRandomEliteRemoval((Integer) spnPersonnelMarketRandomEliteRemoval.getValue());
            options.setPersonnelMarketRandomVeteranRemoval((Integer) spnPersonnelMarketRandomVeteranRemoval.getValue());
            options.setPersonnelMarketRandomRegularRemoval((Integer) spnPersonnelMarketRandomRegularRemoval.getValue());
            options.setPersonnelMarketRandomGreenRemoval((Integer) spnPersonnelMarketRandomGreenRemoval.getValue());
            options.setPersonnelMarketRandomUltraGreenRemoval((Integer) spnPersonnelMarketRandomUltraGreenRemoval.getValue());
            options.setPersonnelMarketDylansWeight((Double) spnPersonnelMarketDylansWeight.getValue());

            // Unit Market
            options.setUnitMarketMethod((UnitMarketMethod) comboUnitMarketMethod.getSelectedItem());
            options.setUnitMarketRegionalMechVariations(chkUnitMarketRegionalMechVariations.isSelected());
            options.setInstantUnitMarketDelivery(chkInstantUnitMarketDelivery.isSelected());
            options.setUnitMarketReportRefresh(chkUnitMarketReportRefresh.isSelected());

            // Contract Market
            options.setContractMarketMethod((ContractMarketMethod) comboContractMarketMethod.getSelectedItem());
            options.setContractMarketReportRefresh(chkContractMarketReportRefresh.isSelected());
            //endregion Markets Tab

            //region RATs Tab
            options.setUseStaticRATs(btnUseStaticRATs.isSelected());
            //Strip dates used in display name
            String[] ratList = new String[chosenRATModel.size()];
            for (int i = 0; i < chosenRATModel.size(); i++) {
                ratList[i] = chosenRATModel.elementAt(i).replaceFirst(" \\(.*?\\)", "");
            }
            options.setRATs(ratList);
            options.setIgnoreRATEra(chkIgnoreRATEra.isSelected());
            //endregion RATs Tab

            // Start Against the Bot
            options.setUseAtB(chkUseAtB.isSelected());
            options.setUseStratCon(chkUseStratCon.isSelected());
            options.setSkillLevel(cbSkillLevel.getSelectedIndex());
            options.setUseShareSystem(chkUseShareSystem.isSelected());
            options.setSharesExcludeLargeCraft(chkSharesExcludeLargeCraft.isSelected());
            options.setSharesForAll(chkSharesForAll.isSelected());
            options.setTrackOriginalUnit(chkTrackOriginalUnit.isSelected());
            options.setRetirementRolls(chkRetirementRolls.isSelected());
            options.setCustomRetirementMods(chkCustomRetirementMods.isSelected());
            options.setFoundersNeverRetire(chkFoundersNeverRetire.isSelected());
            options.setAtBAddDependents(chkAddDependents.isSelected());
            options.setDependentsNeverLeave(chkDependentsNeverLeave.isSelected());
            options.setTrackUnitFatigue(chkTrackUnitFatigue.isSelected());
            options.setLimitLanceWeight(chkLimitLanceWeight.isSelected());
            options.setLimitLanceNumUnits(chkLimitLanceNumUnits.isSelected());
            options.setUseLeadership(chkUseLeadership.isSelected());
            options.setUseStrategy(chkUseStrategy.isSelected());
            options.setBaseStrategyDeployment((Integer) spnBaseStrategyDeployment.getValue());
            options.setAdditionalStrategyDeployment((Integer) spnAdditionalStrategyDeployment.getValue());
            options.setAdjustPaymentForStrategy(chkAdjustPaymentForStrategy.isSelected());

            options.setUseAero(chkUseAero.isSelected());
            options.setUseVehicles(chkUseVehicles.isSelected());
            options.setClanVehicles(chkClanVehicles.isSelected());
            options.setDoubleVehicles(chkDoubleVehicles.isSelected());
            options.setAdjustPlayerVehicles(chkAdjustPlayerVehicles.isSelected());
            options.setOpforLanceTypeMechs((Integer) spnOpforLanceTypeMechs.getValue());
            options.setOpforLanceTypeMixed((Integer) spnOpforLanceTypeMixed.getValue());
            options.setOpforLanceTypeVehicles((Integer) spnOpforLanceTypeVehicles.getValue());
            options.setOpforUsesVTOLs(chkOpforUsesVTOLs.isSelected());
            options.setAllowOpforAeros(chkOpforUsesAero.isSelected());
            options.setAllowOpforLocalUnits(chkOpforUsesLocalForces.isSelected());
            options.setOpforAeroChance((Integer) spnOpforAeroChance.getValue());
            options.setOpforLocalUnitChance((Integer) spnOpforLocalForceChance.getValue());
            options.setFixedMapChance((Integer) spnFixedMapChance.getValue());
            options.setUseDropShips(chkUseDropShips.isSelected());

            options.setSearchRadius((Integer) spnSearchRadius.getValue());
            for (int i = 0; i < spnAtBBattleChance.length; i++) {
                options.setAtBBattleChance(i, (Integer) spnAtBBattleChance[i].getValue());
            }
            options.setGenerateChases(chkGenerateChases.isSelected());
            options.setVariableContractLength(chkVariableContractLength.isSelected());
            options.setMercSizeLimited(chkMercSizeLimited.isSelected());
            options.setRestrictPartsByMission(chkRestrictPartsByMission.isSelected());
            options.setRegionalMechVariations(chkRegionalMechVariations.isSelected());
            options.setAttachedPlayerCamouflage(chkAttachedPlayerCamouflage.isSelected());
            options.setPlayerControlsAttachedUnits(chkPlayerControlsAttachedUnits.isSelected());
            options.setUseWeatherConditions(chkUseWeatherConditions.isSelected());
            options.setUseLightConditions(chkUseLightConditions.isSelected());
            options.setUsePlanetaryConditions(chkUsePlanetaryConditions.isSelected());
            options.setAeroRecruitsHaveUnits(chkAeroRecruitsHaveUnits.isSelected());
            // End Against the Bot

            campaign.setCampaignOptions(options);

            MekHQ.triggerEvent(new OptionsChangedEvent(campaign, options));
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            JOptionPane.showMessageDialog(getFrame(),
                    "Campaign Options update failure, please check the logs for the exception reason.",
                    "Error Updating Campaign Options", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected void okButtonActionPerformed(final ActionEvent evt) {
        if (!txtName.getText().isBlank()) {
            updateOptions();
            setResult(DialogResult.CONFIRMED);
            setVisible(false);
        }
    }

    private void btnSaveActionPerformed() {
        if (txtName.getText().isBlank()) {
            return;
        }
        updateOptions();
        setResult(DialogResult.CONFIRMED);

        final CreateCampaignPresetDialog createCampaignPresetDialog
                = new CreateCampaignPresetDialog(getFrame(), getCampaign(), null);
        if (!createCampaignPresetDialog.showDialog().isConfirmed()) {
            setVisible(false);
            return;
        }
        final CampaignPreset preset = createCampaignPresetDialog.getPreset();
        if (preset == null) {
            setVisible(false);
            return;
        }
        preset.writeToFile(getFrame(),
                FileDialogs.saveCampaignPreset(getFrame(), preset).orElse(null));
        setVisible(false);
    }

    private void updateXPCosts() {
        for (int i = 0; i < SkillType.skillList.length; i++) {
            for (int j = 0; j < 11; j++) {
                try {
                    int cost = Integer.parseInt((String) tableXP.getValueAt(i, j));
                    SkillType.setCost(SkillType.skillList[i], cost, j);
                } catch (NumberFormatException e) {
                    LogManager.getLogger().error("unreadable value in skill cost table for " + SkillType.skillList[i]);
                }
            }
        }
    }

    private void updateSkillTypes() {
        for (String skillName : SkillType.getSkillList()) {
            SkillType type = SkillType.getType(skillName);
            if (null != hashSkillTargets.get(skillName)) {
                type.setTarget((Integer) hashSkillTargets.get(skillName).getValue());
            }
            if (null != hashGreenSkill.get(skillName)) {
                type.setGreenLevel((Integer) hashGreenSkill.get(skillName).getValue());
            }
            if (null != hashRegSkill.get(skillName)) {
                type.setRegularLevel((Integer) hashRegSkill.get(skillName).getValue());
            }
            if (null != hashVetSkill.get(skillName)) {
                type.setVeteranLevel((Integer) hashVetSkill.get(skillName).getValue());
            }
            if (null != hashEliteSkill.get(skillName)) {
                type.setEliteLevel((Integer) hashEliteSkill.get(skillName).getValue());
            }
        }
    }

    private void btnDateActionPerformed(ActionEvent evt) {
        // show the date chooser
        DateChooser dc = new DateChooser(getFrame(), date);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            setDate(dc.getDate());
        }
    }

    private void setDate(final @Nullable LocalDate date) {
        if (date == null) {
            return;
        }

        this.date = date;
        btnDate.setText(MekHQ.getMekHQOptions().getDisplayFormattedDate(date));

        final FactionDisplay factionDisplay = comboFaction.getSelectedItem();
        comboFaction.removeAllItems();
        ((DefaultComboBoxModel<FactionDisplay>) comboFaction.getModel()).addAll(FactionDisplay
                .getSortedValidFactionDisplays(Factions.getInstance().getChoosableFactions(), date));
        comboFaction.setSelectedItem(factionDisplay);
    }

    private void btnCamoActionPerformed(ActionEvent evt) {
        CamoChooserDialog ccd = new CamoChooserDialog(getFrame(), camouflage);
        if (ccd.showDialog().isConfirmed()) {
            camouflage = ccd.getSelectedItem();
            btnCamo.setIcon(camouflage.getImageIcon());
        }
    }

    private Vector<String> getUnusedSPA() {
        Vector<String> unused = new Vector<>();
        PersonnelOptions poptions = new PersonnelOptions();
        for (Enumeration<IOptionGroup> i = poptions.getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(PersonnelOptions.LVL3_ADVANTAGES)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption option = j.nextElement();
                if (getCurrentSPA().get(option.getName()) == null) {
                    unused.add(option.getName());
                }
            }
        }

        for (String key : SpecialAbility.getAllDefaultSpecialAbilities().keySet()) {
            if ((getCurrentSPA().get(key) == null) && !unused.contains(key)) {
                unused.add(key);
            }
        }

        return unused;
    }

    public Hashtable<String, SpecialAbility> getCurrentSPA() {
        return tempSPA;
    }

    private void btnAddSPA() {
        SelectUnusedAbilityDialog suad = new SelectUnusedAbilityDialog(getFrame(), getUnusedSPA(), getCurrentSPA());
        suad.setVisible(true);

        recreateSPAPanel(!getUnusedSPA().isEmpty());
    }

    public void btnRemoveSPA(String name) {
        getCurrentSPA().remove(name);

        //we also need to cycle through the existing SPAs and remove this one from
        //any prereqs
        for (String key: getCurrentSPA().keySet()) {
            SpecialAbility otherAbil = getCurrentSPA().get(key);
            Vector<String> prereq = otherAbil.getPrereqAbilities();
            Vector<String> invalid = otherAbil.getInvalidAbilities();
            Vector<String> remove = otherAbil.getRemovedAbilities();
            if (prereq.remove(name)) {
                otherAbil.setPrereqAbilities(prereq);
            }
            if (invalid.remove(name)) {
                otherAbil.setInvalidAbilities(invalid);
            }
            if (remove.remove(name)) {
                otherAbil.setRemovedAbilities(remove);
            }
        }

        recreateSPAPanel(true);
    }

    public void recreateSPAPanel(boolean enableAddSPA) {
        panSpecialAbilities.removeAll();

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panSpecialAbilities.add(btnAddSPA, gridBagConstraints);
        btnAddSPA.setEnabled(enableAddSPA);

        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 1.0;

        NaturalOrderComparator naturalOrderComparator = new NaturalOrderComparator();
        getCurrentSPA().values().stream().sorted((o1, o2) ->
                naturalOrderComparator.compare(o1.getDisplayName(), o2.getDisplayName())
        ).forEach(spa -> {
            panSpecialAbilities.add(new SpecialAbilityPanel(spa, this), gridBagConstraints);
            gridBagConstraints.gridy++;
        });
        panSpecialAbilities.revalidate();
        panSpecialAbilities.repaint();
    }

    private void enableAtBComponents(JPanel panel, boolean enabled) {
        for (Component c : panel.getComponents()) {
            if (c.equals(chkUseAtB)) {
                continue;
            }

            if (c instanceof JPanel) {
                enableAtBComponents((JPanel) c, enabled);
            } else {
                c.setEnabled(enabled);
            }
        }
    }

    private double determineAtBBattleIntensity() {
        double intensity = 0.0;
        int x;

        x = (Integer) spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].getValue();
        intensity += ((-3.0 / 2.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].getValue();
        intensity += ((-4.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].getValue();
        intensity += ((-2.0 / 3.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        x = (Integer) spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].getValue();
        intensity += ((-9.0) * (2.0 * x - 1.0)) / (2.0 * x - 201.0);

        intensity = intensity / 4.0;

        if (intensity > 100.0) {
            intensity = 100.0;
        }

        return Math.round(intensity * 10.0) / 10.0;
    }

    private class AtBBattleIntensityChangeListener implements ChangeListener  {
        @Override
        public void stateChanged(ChangeEvent e) {
            double intensity = (Double) spnAtBBattleIntensity.getValue();

            if (intensity >= AtBContract.MINIMUM_INTENSITY) {
                int value = (int) Math.min(Math.round(400.0 * intensity / (4.0 * intensity + 6.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(200.0 * intensity / (2.0 * intensity + 8.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(600.0 * intensity / (6.0 * intensity + 4.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].setValue(value);
                value = (int) Math.min(Math.round(100.0 * intensity / (intensity + 9.0) + 0.05), 100);
                spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].setValue(value);
            } else {
                spnAtBBattleChance[AtBLanceRole.FIGHTING.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.DEFENCE.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.SCOUTING.ordinal()].setValue(0);
                spnAtBBattleChance[AtBLanceRole.TRAINING.ordinal()].setValue(0);
            }
        }
    }

    /*
     * Taken from:
     *  http://tips4java.wordpress.com/2008/11/18/row-number-table/
     *	Use a JTable as a renderer for row numbers of a given main table.
     *  This table must be added to the row header of the scrollpane that
     *  contains the main table.
     */
    public static class RowNamesTable extends JTable implements ChangeListener, PropertyChangeListener {
        private static final long serialVersionUID = 3151119498072423302L;
        private JTable main;

        public RowNamesTable(JTable table) {
            main = table;
            main.addPropertyChangeListener(this);

            setFocusable(false);
            setAutoCreateColumnsFromModel(false);
            setModel(main.getModel());
            setSelectionModel(main.getSelectionModel());

            TableColumn column = new TableColumn();
            column.setHeaderValue(" ");
            addColumn(column);
            column.setCellRenderer(new RowNumberRenderer());

            getColumnModel().getColumn(0).setPreferredWidth(120);
            setPreferredScrollableViewportSize(getPreferredSize());
        }

        @Override
        public void addNotify() {
            super.addNotify();
            Component c = getParent();
            //  Keep scrolling of the row table in sync with the main table.
            if (c instanceof JViewport) {
                JViewport viewport = (JViewport) c;
                viewport.addChangeListener(this);
            }
        }

        /*
         *  Delegate method to main table
         */
        @Override
        public int getRowCount() {
            return main.getRowCount();
        }

        @Override
        public int getRowHeight(int row) {
            return main.getRowHeight(row);
        }

        /*
         *  This table does not use any data from the main TableModel,
         *  so just return a value based on the row parameter.
         */
        @Override
        public Object getValueAt(int row, int column) {
            return SkillType.skillList[row];
        }

        /*
         *  Don't edit data in the main TableModel by mistake
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        //
        //  Implement the ChangeListener
        //
        @Override
        public void stateChanged(ChangeEvent e) {
            //  Keep the scrolling of the row table in sync with main table
            JViewport viewport = (JViewport) e.getSource();
            JScrollPane scrollPane = (JScrollPane) viewport.getParent();
            scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
        }

        //
        //  Implement the PropertyChangeListener
        //
        @Override
        public void propertyChange(PropertyChangeEvent e) {
            //  Keep the row table in sync with the main table

            if ("selectionModel".equals(e.getPropertyName())) {
                setSelectionModel(main.getSelectionModel());
            }

            if ("model".equals(e.getPropertyName())) {
                setModel(main.getModel());
            }
        }

        /*
         *  Borrow the renderer from JDK1.4.2 table header
         */
        private static class RowNumberRenderer extends DefaultTableCellRenderer {
            private static final long serialVersionUID = -5430873664301394767L;

            public RowNumberRenderer() {
                setHorizontalAlignment(JLabel.LEFT);
            }

            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (table != null) {
                    JTableHeader header = table.getTableHeader();

                    if (header != null) {
                        setForeground(header.getForeground());
                        setBackground(header.getBackground());
                        setFont(header.getFont());
                    }
                }

                if (isSelected) {
                    setFont(getFont().deriveFont(Font.BOLD));
                }

                setText((value == null) ? "" : value.toString());
                setBorder(UIManager.getBorder("TableHeader.cellBorder"));

                return this;
            }
        }
    }
}
