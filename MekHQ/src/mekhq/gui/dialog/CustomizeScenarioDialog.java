/*
 * CustomizeScenarioDialog.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.PlanetaryConditionsDialog;
import megamek.common.planetaryconditions.PlanetaryConditions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.gui.FileDialogs;
import mekhq.gui.model.LootTableModel;
import mekhq.gui.utilities.MarkdownEditorPanel;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * @author Taharqa
 */
public class CustomizeScenarioDialog extends JDialog {
    private JFrame frame;

    private Scenario scenario;
    private Mission mission;
    private Campaign campaign;
    private boolean newScenario;
    private LocalDate date;
    private PlanetaryConditions planetaryConditions;

    // begin: loot
    private ArrayList<Loot> loots;
    private JTable lootTable;
    private LootTableModel lootModel;
    // end: loot

    // begin: panels
    private JPanel panMain;
    private JPanel panLeft;
    private JPanel panRight;
    private JPanel panLoot;
    private JPanel panBtn;
    // end: panels

    // begin: labels
    private JLabel lblName;
    private JLabel lblDate;
    private JLabel lblStatus;
    // end: labels

    // begin: textfields
    private JTextField txtName;
    // end: textfields

    // begin: comboboxes
    private JComboBox<String> modifierBox;
    private JComboBox<ScenarioStatus> choiceStatus;
    //end: comboboxes

    // begin: buttons
    private JButton btnDate;
    private JButton btnPlanetaryConditions;
    private JButton btnAddLoot;
    private JButton btnEditLoot;
    private JButton btnDeleteLoot;
    private JButton btnClose;
    private JButton btnOK;
    // end: buttons

    // begin: markdown editors
    private MarkdownEditorPanel txtDesc;
    private MarkdownEditorPanel txtReport;
    // end: markdown editors

    public CustomizeScenarioDialog(JFrame parent, boolean modal, Scenario s, Mission m, Campaign c) {
        super(parent, modal);
        this.frame = parent;
        this.mission = m;
        if (null == s) {
            scenario = new Scenario("New Scenario");
            newScenario = true;
        } else {
            scenario = s;
            newScenario = false;
        }
        campaign = c;
        if (scenario.getDate() == null) {
            scenario.setDate(campaign.getLocalDate());
        }
        date = scenario.getDate();

        planetaryConditions = scenario.createPlanetaryConditions();

        loots = new ArrayList<>();
        for (Loot loot : scenario.getLoot()) {
            loots.add((Loot) loot.clone());
        }
        lootModel = new LootTableModel(loots);
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
        pack();
    }

    private void initComponents() {
        getContentPane().setLayout(new BorderLayout());
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizeScenarioDialog",
                MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("title.new"));

        // set up panels
        panMain = new JPanel(new GridLayout(0, 2));
        panLeft = new JPanel(new GridBagLayout());
        panRight = new JPanel(new GridBagLayout());
        panBtn = new JPanel(new GridLayout(0,2));

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        panMain.add(panLeft);
        panMain.add(panRight);

        // set up left panel
        lblName = new JLabel(resourceMap.getString("lblName.text"));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panLeft.add(lblName, gridBagConstraints);

        txtName = new JTextField();
        txtName.setText(scenario.getName());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panLeft.add(txtName, gridBagConstraints);

        lblStatus = new JLabel(resourceMap.getString("lblStatus.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panLeft.add(lblStatus, gridBagConstraints);

        choiceStatus = new JComboBox<>(new DefaultComboBoxModel<>(ScenarioStatus.values()));
        choiceStatus.setSelectedItem(scenario.getStatus());
        choiceStatus.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                              final int index, final boolean isSelected,
                                                              final boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof ScenarioStatus) {
                        list.setToolTipText(((ScenarioStatus) value).getToolTipText());
                    }
                    return this;
                }
        });
        gridBagConstraints.gridx = 1;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        choiceStatus.setEnabled(!scenario.getStatus().isCurrent());
        panLeft.add(choiceStatus, gridBagConstraints);

        lblDate = new JLabel(resourceMap.getString("lblDate.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panLeft.add(lblDate, gridBagConstraints);

        btnDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        btnDate.addActionListener(evt -> changeDate());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panLeft.add(btnDate, gridBagConstraints);

        if (scenario.getStatus().isCurrent() && (scenario instanceof AtBDynamicScenario)) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.gridwidth = 1;

            modifierBox = new JComboBox<>();
            EventTiming scenarioState = ((AtBDynamicScenario) scenario).getNumBots() > 0 ?
                    EventTiming.PostForceGeneration : EventTiming.PreForceGeneration;

            for (String modifierKey : AtBScenarioModifier.getOrderedModifierKeys()) {
                if (AtBScenarioModifier.getScenarioModifier(modifierKey).getEventTiming() == scenarioState) {
                    modifierBox.addItem(modifierKey);
                }
            }
            panLeft.add(modifierBox, gridBagConstraints);

            JButton addEventButton = new JButton("Apply Modifier");
            addEventButton.addActionListener(this::btnAddModifierActionPerformed);
            gridBagConstraints.gridx = 1;
            panLeft.add(addEventButton, gridBagConstraints);
        }

        btnPlanetaryConditions = new JButton();
        btnPlanetaryConditions.setText("Planetary Conditions");
        btnPlanetaryConditions.addActionListener(evt -> changePlanetaryConditions());
        btnPlanetaryConditions.setEnabled(scenario.getStatus().isCurrent());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        panLeft.add(btnPlanetaryConditions, gridBagConstraints);

        initLootPanel(resourceMap);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panLoot.setPreferredSize(new Dimension(400,150));
        panLoot.setMinimumSize(new Dimension(400,150));
        panLoot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Scenario Costs & Payouts"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        panLeft.add(panLoot, gridBagConstraints);

        //set up right panel
        txtDesc = new MarkdownEditorPanel("Description");
        txtDesc.setText(scenario.getDescription());
        txtDesc.setMinimumSize(new Dimension(400, 100));
        txtDesc.setPreferredSize(new Dimension(400, 250));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        panRight.add(txtDesc, gridBagConstraints);

        if (!scenario.getStatus().isCurrent()) {
            txtReport = new MarkdownEditorPanel("After-Action Report");
            txtReport.setText(scenario.getReport());
            txtReport.setMinimumSize(new Dimension(400, 100));
            txtReport.setPreferredSize(new Dimension(400, 250));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            panRight.add(txtReport, gridBagConstraints);
        }

        // set up buttons
        if (newScenario && (mission instanceof AtBContract)) {
            JButton btnLoad = new JButton("Generate From Template");
            btnLoad.addActionListener(this::btnLoadActionPerformed);
            panBtn.add(btnLoad);
        } else if ((mission instanceof AtBContract) &&
                (scenario instanceof AtBDynamicScenario) &&
                (scenario.getStatus().isCurrent())) {
            JButton btnFinalize = new JButton();

            if (((AtBDynamicScenario) scenario).getNumBots() > 0) {
                btnFinalize.setText("Regenerate Bot Forces");
            } else {
                btnFinalize.setText("Generate Bot Forces");
            }

            btnFinalize.addActionListener(this::btnFinalizeActionPerformed);
            panBtn.add(btnFinalize);
        }

        btnOK = new JButton(resourceMap.getString("btnOkay.text"));
        btnOK.addActionListener(this::btnOKActionPerformed);
        panBtn.add(btnOK);

        btnClose = new JButton(resourceMap.getString("btnCancel.text"));
        btnClose.addActionListener(this::btnCloseActionPerformed);
        panBtn.add(btnClose);

        pack();
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(CustomizeScenarioDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        scenario.setName(txtName.getText());
        scenario.setDesc(txtDesc.getText());
        if (!scenario.getStatus().isCurrent()
                || (campaign.getCampaignOptions().isUseAtB() && (scenario instanceof AtBScenario))) {
            if (txtReport != null) {
                scenario.setReport(txtReport.getText());
            }

            if (choiceStatus.getSelectedItem() != null) {
                scenario.setStatus((ScenarioStatus) choiceStatus.getSelectedItem());
            }
        }
        scenario.readPlanetaryConditions(planetaryConditions);
        scenario.setDate(date);
        scenario.resetLoot();
        for (Loot loot : lootModel.getAllLoot()) {
            scenario.addLoot(loot);
        }
        if (newScenario) {
            campaign.addScenario(scenario, mission);
        }
        this.setVisible(false);
    }

    private void btnLoadActionPerformed(ActionEvent evt) {
        File file = FileDialogs.openScenarioTemplate((JFrame) getOwner()).orElse(null);
        if (file == null) {
            return;
        }

        ScenarioTemplate scenarioTemplate = ScenarioTemplate.Deserialize(file);

        if (scenarioTemplate == null) {
            JOptionPane.showMessageDialog(this, "Error loading specified file. See log for details.", "Load Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AtBDynamicScenario scenario = AtBDynamicScenarioFactory.initializeScenarioFromTemplate(scenarioTemplate, (AtBContract) mission, campaign);
        if (scenario.getDate() == null) {
            scenario.setDate(date);
        }

        if (newScenario) {
            campaign.addScenario(scenario, mission);
        }

        this.setVisible(false);
    }

    private void btnFinalizeActionPerformed(ActionEvent evt) {
        AtBDynamicScenarioFactory.finalizeScenario((AtBDynamicScenario) scenario, (AtBContract) mission, campaign);
        this.setVisible(false);
    }

    public int getMissionId() {
        return mission.getId();
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    private void changeDate() {
        // show the date chooser
        DateChooser dc = new DateChooser(frame, date);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            if (scenario.getStatus().isCurrent()) {
                if (dc.getDate().isBefore(campaign.getLocalDate())) {
                    JOptionPane.showMessageDialog(frame, "You cannot choose a date before the current date for a pending battle.", "Invalid date", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            date = dc.getDate();
            btnDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        }
    }

    private void changePlanetaryConditions() {
        PlanetaryConditionsDialog pc = new PlanetaryConditionsDialog(frame, planetaryConditions);
        if(pc.showDialog()) {
            planetaryConditions = pc.getConditions();
        }
    }

    private void initLootPanel(ResourceBundle resourceMap) {
        panLoot = new JPanel(new BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1,0));
        btnAddLoot = new JButton(resourceMap.getString("btnAddLoot.text"));
        btnAddLoot.addActionListener(evt -> addLoot());
        btnAddLoot.setEnabled(scenario.getStatus().isCurrent());
        panBtns.add(btnAddLoot);

        btnEditLoot = new JButton(resourceMap.getString("btnEditLoot.text"));
        btnEditLoot.setEnabled(false);
        btnEditLoot.addActionListener(evt -> editLoot());
        btnEditLoot.setEnabled(scenario.getStatus().isCurrent());
        panBtns.add(btnEditLoot);

        btnDeleteLoot = new JButton(resourceMap.getString("btnDeleteLoot.text"));
        btnDeleteLoot.setEnabled(false);
        btnDeleteLoot.addActionListener(evt -> deleteLoot());
        btnDeleteLoot.setEnabled(scenario.getStatus().isCurrent());
        panBtns.add(btnDeleteLoot);
        panLoot.add(panBtns, BorderLayout.PAGE_START);

        lootTable = new JTable(lootModel);
        TableColumn column;
        for (int i = 0; i < LootTableModel.N_COL; i++) {
            column = lootTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(lootModel.getColumnWidth(i));
            column.setCellRenderer(lootModel.getRenderer());
        }
        lootTable.setIntercellSpacing(new Dimension(0, 0));
        lootTable.setShowGrid(false);
        lootTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lootTable.getSelectionModel().addListSelectionListener(this::lootTableValueChanged);

        panLoot.add(new JScrollPane(lootTable), BorderLayout.CENTER);
    }

    private void lootTableValueChanged(ListSelectionEvent evt) {
        int row = lootTable.getSelectedRow();
        btnDeleteLoot.setEnabled(row != -1);
        btnEditLoot.setEnabled(row != -1);
    }

    private void addLoot() {
        LootDialog ekld = new LootDialog(frame, true, new Loot(), campaign);
        ekld.setVisible(true);
        if (null != ekld.getLoot()) {
            lootModel.addLoot(ekld.getLoot());
        }
        refreshTable();
    }

    private void editLoot() {
        Loot loot = lootModel.getLootAt(lootTable.getSelectedRow());
        if (null != loot) {
            LootDialog ekld = new LootDialog(frame, true, loot, campaign);
            ekld.setVisible(true);
            refreshTable();
        }
    }

    private void deleteLoot() {
        int row = lootTable.getSelectedRow();
        if (-1 != row) {
            loots.remove(row);
        }
        refreshTable();
    }

    private void refreshTable() {
        int selectedRow = lootTable.getSelectedRow();
        lootModel.setData(loots);
        if (selectedRow != -1) {
            if (lootTable.getRowCount() > 0) {
                if (lootTable.getRowCount() == selectedRow) {
                    lootTable.setRowSelectionInterval(selectedRow-1, selectedRow-1);
                } else {
                    lootTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    }

    /**
     * Event handler for the 'add modifier' button.
     * @param event
     */
    private void btnAddModifierActionPerformed(ActionEvent event) {
        AtBDynamicScenario scenarioPtr = (AtBDynamicScenario) scenario;
        AtBScenarioModifier modifierPtr = AtBScenarioModifier.getScenarioModifier(modifierBox.getSelectedItem().toString());
        EventTiming timing = scenarioPtr.getNumBots() > 0 ? EventTiming.PostForceGeneration : EventTiming.PreForceGeneration;

        modifierPtr.processModifier(scenarioPtr, campaign, timing);
        txtDesc.setText(txtDesc.getText() + "\n\n" + modifierPtr.getAdditionalBriefingText());
    }

}
