/*
 * CustomizeScenarioDialog.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;
import mekhq.gui.FileDialogs;
import mekhq.gui.model.LootTableModel;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.utilities.MarkdownEditorPanel;
import mekhq.preferences.PreferencesNode;

/**
 * @author  Taharqa
 */
public class CustomizeScenarioDialog extends javax.swing.JDialog {
	private static final long serialVersionUID = -8038099101234445018L;
    private JFrame frame;
    private Scenario scenario;
    private Mission mission;
    private Campaign campaign;
    private boolean newScenario;
    private LocalDate date;

    private LootTableModel lootModel;

    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnDelete;
    private ArrayList<Loot> loots;
    private JTable lootTable;
    private JPanel panLoot;

    private JComboBox<String> modifierBox;

    private javax.swing.JPanel panMain;
    private javax.swing.JPanel panBtn;
    private javax.swing.JButton btnClose;
    private javax.swing.JButton btnOK;
    private javax.swing.JLabel lblName;
    private javax.swing.JTextField txtName;
    private MarkdownEditorPanel txtDesc;
    private MarkdownEditorPanel txtReport;
    private javax.swing.JComboBox<String> choiceStatus;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JButton btnDate;

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
        date = scenario.getDate();
        if (null == date) {
            date = campaign.getLocalDate();
        }
        loots = new ArrayList<>();
        for (Loot loot : scenario.getLoot()) {
            loots.add((Loot)loot.clone());
        }
        lootModel = new LootTableModel(loots);
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
        pack();
    }

    private void initComponents() {
         java.awt.GridBagConstraints gridBagConstraints;

        txtName = new javax.swing.JTextField();
        lblName = new javax.swing.JLabel();
        btnOK = new javax.swing.JButton();
        btnClose = new javax.swing.JButton();
        lblStatus = new javax.swing.JLabel();
        panMain = new javax.swing.JPanel();
        panBtn = new javax.swing.JPanel();
        choiceStatus = new javax.swing.JComboBox<>();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizeScenarioDialog", new EncodeControl()); //$NON-NLS-1$
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form"); // NOI18N
        setTitle(resourceMap.getString("title.new"));

        getContentPane().setLayout(new BorderLayout());
        panMain.setLayout(new GridBagLayout());
        panBtn.setLayout(new GridLayout(0,2));

        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(lblName, gridBagConstraints);

        txtName.setText(scenario.getName());
        txtName.setName("txtName"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtName, gridBagConstraints);

        if (!scenario.isCurrent()) {
            lblStatus.setText(resourceMap.getString("lblStatus.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panMain.add(lblStatus, gridBagConstraints);

            DefaultComboBoxModel<String> statusModel = new DefaultComboBoxModel<>();
            for (int i = 1; i < Scenario.S_NUM; i++) {
                statusModel.addElement(Scenario.getStatusName(i));
            }
            choiceStatus.setModel(statusModel);
            choiceStatus.setName("choiceStatus"); // NOI18N
            choiceStatus.setSelectedIndex(scenario.getStatus()-1);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panMain.add(choiceStatus, gridBagConstraints);
        }
        if (!scenario.isCurrent() || (campaign.getCampaignOptions().getUseAtB() && (scenario instanceof AtBScenario))) {
            btnDate = new javax.swing.JButton();
            btnDate.setText(date.format(DateTimeFormatter.ofPattern(campaign.getCampaignOptions().getDisplayDateFormat())));
            btnDate.addActionListener(evt -> changeDate());
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
            panMain.add(btnDate, gridBagConstraints);
        }
        if (scenario.isCurrent()) {
            initLootPanel();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            panLoot.setPreferredSize(new Dimension(400,150));
            panLoot.setMinimumSize(new Dimension(400,150));
            panLoot.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Potential Rewards"),
                    BorderFactory.createEmptyBorder(5,5,5,5)));
            panMain.add(panLoot, gridBagConstraints);
        }

        txtDesc = new MarkdownEditorPanel("Description");
        txtDesc.setText(scenario.getDescription());
        txtDesc.setMinimumSize(new Dimension(400, 100));
        txtDesc.setPreferredSize(new Dimension(400, 250));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panMain.add(txtDesc, gridBagConstraints);

        if ((scenario instanceof AtBDynamicScenario) && scenario.isCurrent()) {
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
            panMain.add(modifierBox, gridBagConstraints);

            JButton addEventButton = new JButton("Apply Modifier");
            addEventButton.addActionListener(this::btnAddModifierActionPerformed);
            gridBagConstraints.gridx = 1;
            panMain.add(addEventButton, gridBagConstraints);
        }

        if (!scenario.isCurrent()) {
            txtReport = new MarkdownEditorPanel("After-Action Report");
            txtReport.setText(scenario.getReport());
            txtReport.setMinimumSize(new Dimension(400, 100));
            txtReport.setPreferredSize(new Dimension(400, 250));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            panMain.add(txtReport, gridBagConstraints);
        }

        if (newScenario && (mission instanceof AtBContract)) {
            JButton btnLoad = new JButton("Generate From Template");
            btnLoad.addActionListener(this::btnLoadActionPerformed);
            panBtn.add(btnLoad);
        } else if ((mission instanceof AtBContract) && (scenario instanceof AtBDynamicScenario)) {
            JButton btnFinalize = new JButton();

            if (((AtBDynamicScenario) scenario).getNumBots() > 0) {
                btnFinalize.setText("Regenerate Bot Forces");
            } else {
                btnFinalize.setText("Generate Bot Forces");
            }

            btnFinalize.addActionListener(this::btnFinalizeActionPerformed);
            panBtn.add(btnFinalize);
        }

        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(this::btnOKActionPerformed);
        panBtn.add(btnOK);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(this::btnCloseActionPerformed);
        gridBagConstraints.gridx = GridBagConstraints.RELATIVE;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
        panBtn.add(btnClose);

        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CustomizeScenarioDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        scenario.setName(txtName.getText());
        scenario.setDesc(txtDesc.getText());
        if (!scenario.isCurrent() || (campaign.getCampaignOptions().getUseAtB() && (scenario instanceof AtBScenario))) {
            if (txtReport != null) {
                scenario.setReport(txtReport.getText());
            }

            scenario.setStatus(choiceStatus.getSelectedIndex()+1);
            scenario.setDate(date);
        }
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

    private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
    }

    private void changeDate() {
        // show the date chooser
        GregorianCalendar day = GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault()));
        DateChooser dc = new DateChooser(frame, day);
        // user can either choose a date or cancel by closing
        if (dc.showDateChooser() == DateChooser.OK_OPTION) {
            if (scenario.isCurrent()) {
                if (dc.getDate().getTime().before(campaign.getDate())) {
                    JOptionPane.showMessageDialog(frame,
                            "You cannot choose a date before the current date for a pending battle.",
                            "Invalid date",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                } else {
                    //Calendar math necessitated by variations in locales
                    GregorianCalendar nextMonday = new GregorianCalendar();
                    nextMonday.setTime(campaign.getDate());
                    nextMonday.add(Calendar.DAY_OF_MONTH, 1);
                    while (nextMonday.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                        nextMonday.add(Calendar.DAY_OF_MONTH, 1);
                    }

                    if (!dc.getDate().getTime().before(nextMonday.getTime())) {
                        JOptionPane.showMessageDialog(frame,
                                "You cannot choose a date beyond the current week.",
                                "Invalid date",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            } else if (dc.getDate().getTime().after(campaign.getDate())) {
                JOptionPane.showMessageDialog(frame,
                        "You cannot choose a date after the current date.",
                        "Invalid date",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            date = dc.getDate().toZonedDateTime().toLocalDate();
            btnDate.setText(date.format(DateTimeFormatter.ofPattern(campaign.getCampaignOptions()
                    .getDisplayDateFormat())));
        }
    }

    private void initLootPanel() {
        panLoot = new JPanel(new BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1,0));
        btnAdd = new JButton("Add Loot"); // NOI18N
        btnAdd.addActionListener(evt -> addLoot());
        panBtns.add(btnAdd);

        btnEdit = new JButton("Edit Loot"); // NOI18N
        btnEdit.setEnabled(false);
        btnEdit.addActionListener(evt -> editLoot());
        panBtns.add(btnEdit);

        btnDelete = new JButton("Delete Loot"); // NOI18N
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(evt -> deleteLoot());
        panBtns.add(btnDelete);
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

    private void lootTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        int row = lootTable.getSelectedRow();
        btnDelete.setEnabled(row != -1);
        btnEdit.setEnabled(row != -1);
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
