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
import megamek.client.ui.swing.lobby.PlayerSettingsDialog;
import megamek.common.Player;
import megamek.common.planetaryconditions.PlanetaryConditions;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.gui.FileDialogs;
import mekhq.gui.model.BotForceTableModel;
import mekhq.gui.model.LootTableModel;
import mekhq.gui.model.ObjectiveTableModel;
import mekhq.gui.utilities.MarkdownEditorPanel;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * @author Taharqa
 */
public class CustomizeScenarioDialog extends JDialog {

    // region Variable declarations
    private JFrame frame;
    private Scenario scenario;
    private Mission mission;
    private Campaign campaign;
    private boolean newScenario;
    private LocalDate date;
    private ScenarioDeploymentLimit deploymentLimits;
    private PlanetaryConditions planetaryConditions;
    private Player player;
    private List<BotForce> botForces;

    // objectives
    private List<ScenarioObjective> objectives;
    private JTable objectiveTable;
    private ObjectiveTableModel objectiveModel;

    // loot
    private ArrayList<Loot> loots;
    private JTable lootTable;
    private LootTableModel lootModel;

    // other forces
    private JTable forcesTable;
    private BotForceTableModel forcesModel;

    // panels
    private JPanel panDeploymentLimits;
    private JPanel panLoot;
    private JPanel panObjectives;
    private JPanel panOtherForces;
    private JPanel panPlanetaryConditions;

    // labels
    private JLabel lblAllowedUnitsDesc;
    private JLabel lblQuantityLimitDesc;
    private JLabel lblRequiredPersonnelDesc;
    private JLabel lblRequiredUnitsDesc;
    private JLabel lblLightDesc;
    private JLabel lblWindDesc;
    private JLabel lblAtmosphereDesc;
    private JLabel lblWeatherDesc;
    private JLabel lblFogDesc;
    private JLabel lblBlowingSandDesc;
    private JLabel lblEMIDesc;
    private JLabel lblTemperatureDesc;
    private JLabel lblGravityDesc;
    private JLabel lblOtherConditionsDesc;
    // end: labels

    // textfields
    private JTextField txtName;

    // comboboxes
    private JComboBox<String> modifierBox;
    private JComboBox<ScenarioStatus> choiceStatus;

    // buttons
    private JButton btnDate;
    private JButton btnDeployment;
    private JButton btnPlanetaryConditions;
    private JButton btnAddLoot;
    private JButton btnEditLoot;
    private JButton btnDeleteLoot;

    private JButton btnAddObjective;
    private JButton btnEditObjective;
    private JButton btnDeleteObjective;
    private JButton btnAddForce;
    private JButton btnEditForce;
    private JButton btnDeleteForce;
    private JButton btnClose;
    private JButton btnOK;

    //region markdown editors
    private MarkdownEditorPanel txtDesc;
    private MarkdownEditorPanel txtReport;
    //endregion Variable declarations

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

        if(scenario.getDeploymentLimit() != null) {
            deploymentLimits = scenario.getDeploymentLimit().getCopy();
        }

        player = Utilities.createPlayer(scenario);

        planetaryConditions = scenario.createPlanetaryConditions();

        botForces = new ArrayList<>();
        for(BotForce bf : scenario.getBotForces()) {
            botForces.add(bf.clone());
        }
        forcesModel = new BotForceTableModel(botForces, campaign);

        loots = new ArrayList<>();
        for (Loot loot : scenario.getLoot()) {
            loots.add((Loot) loot.clone());
        }
        lootModel = new LootTableModel(loots);

        // FIXME: clone this so we don't change on a cancel
        objectives = scenario.getScenarioObjectives();
        objectiveModel = new ObjectiveTableModel(objectives);

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

        JPanel panMain = new JPanel(new GridBagLayout());
        JPanel panInfo = new JPanel(new GridBagLayout());
        JPanel panWrite = new JPanel(new GridBagLayout());
        JPanel panBtn = new JPanel(new GridLayout(0,2));

        // region Set up info panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panInfo.add(new JLabel(resourceMap.getString("lblName.text")), gbc);

        txtName = new JTextField();
        txtName.setText(scenario.getName());
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        panInfo.add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 0, 0);
        panInfo.add(new JLabel(resourceMap.getString("lblStatus.text")), gbc);

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
        gbc.gridx = 1;
        gbc.insets = new Insets(5, 5, 0, 0);
        choiceStatus.setEnabled(!scenario.getStatus().isCurrent());
        panInfo.add(choiceStatus, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        panInfo.add(new JLabel(resourceMap.getString("lblDate.text")), gbc);

        btnDate = new JButton(MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        btnDate.addActionListener(evt -> changeDate());
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 0, 0);
        panInfo.add(btnDate, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        panInfo.add(new JLabel(resourceMap.getString("lblDeployment.text")), gbc);

        btnDeployment = new JButton(Utilities.getDeploymentString(player));
        btnDeployment.addActionListener(evt -> changeDeployment());
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 0, 0);
        panInfo.add(btnDeployment, gbc);

        if (scenario.getStatus().isCurrent() && (scenario instanceof AtBDynamicScenario)) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;

            modifierBox = new JComboBox<>();
            EventTiming scenarioState = ((AtBDynamicScenario) scenario).getNumBots() > 0 ?
                    EventTiming.PostForceGeneration : EventTiming.PreForceGeneration;

            for (String modifierKey : AtBScenarioModifier.getOrderedModifierKeys()) {
                if (AtBScenarioModifier.getScenarioModifier(modifierKey).getEventTiming() == scenarioState) {
                    modifierBox.addItem(modifierKey);
                }
            }
            panInfo.add(modifierBox, gbc);

            JButton addEventButton = new JButton("Apply Modifier");
            addEventButton.addActionListener(this::btnAddModifierActionPerformed);
            gbc.gridx = 1;
            panInfo.add(addEventButton, gbc);
        }

        initDeployLimitPanel(resourceMap);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panInfo.add(panDeploymentLimits, gbc);

        initPlanetaryConditionsPanel(resourceMap);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panInfo.add(panPlanetaryConditions, gbc);
        // endregion Set up info panel

        initObjectivesPanel(resourceMap);
        //panObjectives.setPreferredSize(new Dimension(300,150));
        //panObjectives.setMinimumSize(new Dimension(300,150));
        panObjectives.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Scenario Objectives"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        initLootPanel(resourceMap);
        //panLoot.setPreferredSize(new Dimension(300,150));
        //panLoot.setMinimumSize(new Dimension(300,150));
        panLoot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Scenario Costs & Payouts"),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        initOtherForcesPanel(resourceMap);
        panOtherForces.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 10, 0),
                BorderFactory.createTitledBorder(resourceMap.getString("panOtherForces.title"))));
        panOtherForces.setPreferredSize(new Dimension(600,150));
        panOtherForces.setMinimumSize(new Dimension(600,150));

        // region Set up writing panel
        txtDesc = new MarkdownEditorPanel("Description");
        txtDesc.setText(scenario.getDescription());
        txtDesc.setMinimumSize(new Dimension(400, 100));
        txtDesc.setPreferredSize(new Dimension(400, 250));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panWrite.add(txtDesc, gbc);

        if (!scenario.getStatus().isCurrent()) {
            txtReport = new MarkdownEditorPanel("After-Action Report");
            txtReport.setText(scenario.getReport());
            txtReport.setMinimumSize(new Dimension(400, 100));
            txtReport.setPreferredSize(new Dimension(400, 250));
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.gridwidth = 1;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(5, 5, 5, 5);
            panWrite.add(txtReport, gbc);
            txtReport.setEnabled(!scenario.getStatus().isCurrent());
        }
        // endregion Set up writing panel

        // region Set up buttons
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
        //endregion Set up buttons

        // layout main panel
        getContentPane().add(panMain, BorderLayout.CENTER);
        getContentPane().add(panBtn, BorderLayout.PAGE_END);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.gridheight = 2;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        panMain.add(panInfo, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weighty = 0.5;
        gbc.gridheight = 1;
        panMain.add(panObjectives, gbc);
        gbc.gridy = 1;
        panMain.add(panLoot, gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.weighty = 1.0;
        panMain.add(panWrite, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        panMain.add(panOtherForces, gbc);

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
        scenario.setDeploymentLimit(deploymentLimits);
        Utilities.updatePlayerSettings(scenario, player);
        scenario.readPlanetaryConditions(planetaryConditions);
        scenario.setDate(date);
        scenario.setBotForces(botForces);
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

    private void changeDeployment() {
        EditDeploymentDialog edd = new EditDeploymentDialog(frame, true, player);
        edd.setVisible(true);
        btnDeployment.setText(Utilities.getDeploymentString(player));
    }

    private void initDeployLimitPanel(ResourceBundle resourceMap) {

        panDeploymentLimits = new JPanel(new GridBagLayout());
        panDeploymentLimits.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 10, 0),
                BorderFactory.createTitledBorder(resourceMap.getString("panDeploymentLimits.title"))));

        JPanel panButtons = new JPanel(new GridLayout(0, 2));
        JButton btnEditLimits = new JButton("Edit Limits");
        btnEditLimits.addActionListener(this::editLimits);
        panButtons.add(btnEditLimits);
        JButton btnRemoveLimits = new JButton("Remove Limits");
        btnRemoveLimits.addActionListener(this::removeLimits);
        panButtons.add(btnRemoveLimits);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panDeploymentLimits.add(panButtons, gbc);

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.gridy = 1;
        leftGbc.gridwidth = 1;
        leftGbc.weightx = 0.0;
        leftGbc.weighty = 0.0;
        leftGbc.insets = new Insets(0, 0, 5, 10);
        leftGbc.fill = GridBagConstraints.NONE;
        leftGbc.anchor = GridBagConstraints.NORTHWEST;

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 1;
        rightGbc.gridy = 1;
        rightGbc.gridwidth = 1;
        rightGbc.weightx = 1.0;
        rightGbc.weighty = 0.0;
        rightGbc.insets = new Insets(0, 10, 5, 0);
        rightGbc.fill = GridBagConstraints.HORIZONTAL;
        rightGbc.anchor = GridBagConstraints.NORTHWEST;

        leftGbc.gridy++;
        panDeploymentLimits.add(new JLabel(resourceMap.getString("lblAllowedUnits.text")), leftGbc);

        lblAllowedUnitsDesc = new JLabel();
        rightGbc.gridy++;
        panDeploymentLimits.add(lblAllowedUnitsDesc, rightGbc);

        leftGbc.gridy++;
        panDeploymentLimits.add(new JLabel(resourceMap.getString("lblQuantityLimit.text")), leftGbc);

        lblQuantityLimitDesc = new JLabel();
        rightGbc.gridy++;
        panDeploymentLimits.add(lblQuantityLimitDesc, rightGbc);

        leftGbc.gridy++;
        panDeploymentLimits.add(new JLabel(resourceMap.getString("lblRequiredPersonnel.text")), leftGbc);

        lblRequiredPersonnelDesc = new JLabel();
        rightGbc.gridy++;
        panDeploymentLimits.add(lblRequiredPersonnelDesc, rightGbc);

        leftGbc.gridy++;
        panDeploymentLimits.add(new JLabel(resourceMap.getString("lblRequiredUnits.text")), leftGbc);

        lblRequiredUnitsDesc = new JLabel();
        rightGbc.gridy++;
        panDeploymentLimits.add(lblRequiredUnitsDesc, rightGbc);

        refreshDeploymentLimits();
    }

    private void refreshDeploymentLimits() {
        if(deploymentLimits != null) {
            lblAllowedUnitsDesc.setText("<html>" + deploymentLimits.getAllowedUnitTypeDesc() + "</html>");
            lblQuantityLimitDesc.setText("<html>" +deploymentLimits.getQuantityLimitDesc(scenario, campaign) + "</html>");
            lblRequiredPersonnelDesc.setText("<html>" + deploymentLimits.getRequiredPersonnelDesc(campaign) + "</html>");
            lblRequiredUnitsDesc.setText("<html>" + deploymentLimits.getRequiredUnitDesc(campaign) + "</html>");
        } else {
            lblAllowedUnitsDesc.setText("All");
            lblQuantityLimitDesc.setText("No Limits");
            lblRequiredPersonnelDesc.setText("None");
            lblRequiredUnitsDesc.setText("None");
        }
    }

    private void editLimits(ActionEvent evt) {
        EditScenarioDeploymentLimitDialog esdld = new EditScenarioDeploymentLimitDialog(frame, true, deploymentLimits);
        esdld.setVisible(true);
        deploymentLimits = esdld.getDeploymentLimit();
        refreshDeploymentLimits();
    }

    private void removeLimits(ActionEvent evt) {
        deploymentLimits = null;
        refreshDeploymentLimits();
    }

    private void initPlanetaryConditionsPanel(ResourceBundle resourceMap) {
        panPlanetaryConditions = new JPanel(new GridBagLayout());
        panPlanetaryConditions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 0, 10, 0),
                BorderFactory.createTitledBorder(resourceMap.getString("panPlanetaryConditions.title"))));

        btnPlanetaryConditions = new JButton(resourceMap.getString("btnPlanetaryConditions.text"));
        btnPlanetaryConditions.addActionListener(evt -> changePlanetaryConditions());
        btnPlanetaryConditions.setEnabled(scenario.getStatus().isCurrent());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 0, 0, 0);
        panPlanetaryConditions.add(btnPlanetaryConditions, gbc);

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.gridy = 0;
        leftGbc.gridwidth = 1;
        leftGbc.weightx = 0.0;
        leftGbc.weighty = 0.0;
        leftGbc.insets = new Insets(0, 5, 5, 5);
        leftGbc.fill = GridBagConstraints.NONE;
        leftGbc.anchor = GridBagConstraints.NORTHWEST;

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 1;
        rightGbc.gridy = 0;
        rightGbc.gridwidth = 1;
        rightGbc.weightx = 0.0;
        rightGbc.weighty = 0.0;
        rightGbc.insets = new Insets(0, 5, 5, 0);
        rightGbc.fill = GridBagConstraints.NONE;
        rightGbc.anchor = GridBagConstraints.NORTHWEST;

        leftGbc.gridy++;
        panPlanetaryConditions.add(new JLabel(resourceMap.getString("lblLight.text")), leftGbc);

        lblLightDesc = new JLabel(scenario.getLight().toString());
        rightGbc.gridy++;
        panPlanetaryConditions.add(lblLightDesc, rightGbc);

        leftGbc.gridy++;
        panPlanetaryConditions.add(new JLabel(resourceMap.getString("lblWeather.text")), leftGbc);

        lblWeatherDesc = new JLabel(scenario.getWeather().toString());
        rightGbc.gridy++;
        panPlanetaryConditions.add(lblWeatherDesc, rightGbc);

        leftGbc.gridy++;
        panPlanetaryConditions.add(new JLabel(resourceMap.getString("lblWind.text")), leftGbc);

        lblWindDesc = new JLabel(scenario.getWind().toString());
        rightGbc.gridy++;
        panPlanetaryConditions.add(lblWindDesc, rightGbc);

        leftGbc.gridy++;
        panPlanetaryConditions.add(new JLabel(resourceMap.getString("lblFog.text")), leftGbc);

        lblFogDesc = new JLabel(scenario.getFog().toString());
        rightGbc.gridy++;
        rightGbc.weightx = 1.0;
        panPlanetaryConditions.add(lblFogDesc, rightGbc);

        leftGbc.gridy++;
        panPlanetaryConditions.add(new JLabel(resourceMap.getString("lblOtherConditions.text")), leftGbc);

        ArrayList<String> otherConditions = new ArrayList<>();
        if (scenario.getEMI().isEMI()) {
            otherConditions.add(resourceMap.getString("emi.text"));
        }
        if (scenario.getBlowingSand().isBlowingSand()) {
            otherConditions.add(resourceMap.getString("sand.text"));
        }

        lblOtherConditionsDesc = new JLabel(String.join(", ", otherConditions));
        if (otherConditions.isEmpty()) {
            lblOtherConditionsDesc.setText("None");
        }
        rightGbc.gridy++;
        rightGbc.gridwidth = 3;
        panPlanetaryConditions.add(lblOtherConditionsDesc, rightGbc);

        leftGbc.gridx = 2;
        leftGbc.gridy = 1;
        panPlanetaryConditions.add(new JLabel(resourceMap.getString("lblTemperature.text")), leftGbc);

        lblTemperatureDesc = new JLabel(PlanetaryConditions.getTemperatureDisplayableName(scenario.getModifiedTemperature()));
        rightGbc.gridx = 3;
        rightGbc.gridy = 1;
        rightGbc.gridwidth = 1;
        panPlanetaryConditions.add(lblTemperatureDesc, rightGbc);

        leftGbc.gridy++;
        panPlanetaryConditions.add(new JLabel(resourceMap.getString("lblGravity.text")), leftGbc);

        lblGravityDesc = new JLabel(DecimalFormat.getInstance().format(scenario.getGravity()));
        rightGbc.gridy++;
        panPlanetaryConditions.add(lblGravityDesc, rightGbc);

        leftGbc.gridy++;
        panPlanetaryConditions.add(new JLabel(resourceMap.getString("lblAtmosphere.text")), leftGbc);

        lblAtmosphereDesc = new JLabel(scenario.getAtmosphere().toString());
        rightGbc.gridy++;
        panPlanetaryConditions.add(lblAtmosphereDesc, rightGbc);


    }

    private void refreshPlanetaryConditions() {
        lblLightDesc.setText(planetaryConditions.getLight().toString());
        lblAtmosphereDesc.setText(planetaryConditions.getAtmosphere().toString());
        lblWeatherDesc.setText(planetaryConditions.getWeather().toString());
        lblFogDesc.setText(planetaryConditions.getFog().toString());
        lblWindDesc.setText(planetaryConditions.getWind().toString());
        lblGravityDesc.setText(DecimalFormat.getInstance().format(planetaryConditions.getGravity()));
        lblTemperatureDesc.setText(PlanetaryConditions.getTemperatureDisplayableName(planetaryConditions.getTemperature()));
        ArrayList<String> otherConditions = new ArrayList<>();
        if (planetaryConditions.getEMI().isEMI()) {
            otherConditions.add("Electromagnetic interference");
        }
        if (planetaryConditions.getBlowingSand().isBlowingSand()) {
            otherConditions.add("Blowing sand");
        }
        if (otherConditions.isEmpty()) {
            lblOtherConditionsDesc.setText("None");
        } else {
            lblOtherConditionsDesc.setText(String.join(", ", otherConditions));
        }
    }

    private void changePlanetaryConditions() {
        PlanetaryConditionsDialog pc = new PlanetaryConditionsDialog(frame, planetaryConditions);
        if(pc.showDialog()) {
            planetaryConditions = pc.getConditions();
        }
        refreshPlanetaryConditions();
    }

    private void initObjectivesPanel(ResourceBundle resourceMap) {
        panObjectives = new JPanel(new BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1,0));
        btnAddObjective = new JButton(resourceMap.getString("btnAddObjective.text"));
        btnAddObjective.addActionListener(evt -> addObjective());
        btnAddObjective.setEnabled(scenario.getStatus().isCurrent());
        panBtns.add(btnAddObjective);

        btnEditObjective = new JButton(resourceMap.getString("btnEditObjective.text"));
        btnEditObjective.setEnabled(false);
        btnEditObjective.addActionListener(evt -> editObjective());
        panBtns.add(btnEditObjective);

        btnDeleteObjective = new JButton(resourceMap.getString("btnDeleteObjective.text"));
        btnDeleteObjective.setEnabled(false);
        btnDeleteObjective.addActionListener(evt -> deleteObjective());
        panBtns.add(btnDeleteObjective);
        panObjectives.add(panBtns, BorderLayout.PAGE_START);

        objectiveTable = new JTable(objectiveModel);
        TableColumn column;
        for (int i = 0; i < ObjectiveTableModel.N_COL; i++) {
            column = objectiveTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(objectiveModel.getColumnWidth(i));
            column.setCellRenderer(objectiveModel.getRenderer());
        }
        objectiveTable.setIntercellSpacing(new Dimension(0, 0));
        objectiveTable.setShowGrid(false);
        objectiveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        objectiveTable.getSelectionModel().addListSelectionListener(this::objectiveTableValueChanged);

        panObjectives.add(new JScrollPane(objectiveTable), BorderLayout.CENTER);

    }

    private void objectiveTableValueChanged(ListSelectionEvent evt) {
        int row = objectiveTable.getSelectedRow();
        btnDeleteObjective.setEnabled(row != -1);
        btnEditObjective.setEnabled(row != -1);
    }

    private void addObjective() {
        CustomizeScenarioObjectiveDialog csod = new CustomizeScenarioObjectiveDialog(frame, true, new ScenarioObjective(), scenario.getBotForces());
        csod.setVisible(true);
        if (null != csod.getObjective()) {
            objectiveModel.addObjective(csod.getObjective());
        }
        refreshObjectiveTable();
    }

    private void editObjective() {
        ScenarioObjective objective = objectiveModel.getObjectiveAt(objectiveTable.getSelectedRow());
        if (null != objective) {
            CustomizeScenarioObjectiveDialog csod = new CustomizeScenarioObjectiveDialog(frame, true, objective, scenario.getBotForces());
            csod.setVisible(true);
            refreshObjectiveTable();
        }
    }

    private void deleteObjective() {
        int row = objectiveTable.getSelectedRow();
        if (-1 != row) {
            objectives.remove(row);
        }
        refreshObjectiveTable();
    }

    private void refreshObjectiveTable() {
        int selectedRow = objectiveTable.getSelectedRow();
        objectiveModel.setData(objectives);
        if (selectedRow != -1) {
            if (objectiveTable.getRowCount() > 0) {
                if (objectiveTable.getRowCount() == selectedRow) {
                    objectiveTable.setRowSelectionInterval(selectedRow-1, selectedRow-1);
                } else {
                    objectiveTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
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
        panBtns.add(btnEditLoot);

        btnDeleteLoot = new JButton(resourceMap.getString("btnDeleteLoot.text"));
        btnDeleteLoot.setEnabled(false);
        btnDeleteLoot.addActionListener(evt -> deleteLoot());
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
        refreshLootTable();
    }

    private void editLoot() {
        Loot loot = lootModel.getLootAt(lootTable.getSelectedRow());
        if (null != loot) {
            LootDialog ekld = new LootDialog(frame, true, loot, campaign);
            ekld.setVisible(true);
            refreshLootTable();
        }
    }

    private void deleteLoot() {
        int row = lootTable.getSelectedRow();
        if (-1 != row) {
            loots.remove(row);
        }
        refreshLootTable();
    }

    private void refreshLootTable() {
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

    private void initOtherForcesPanel(ResourceBundle resourceMap) {
        panOtherForces = new JPanel(new BorderLayout());

        JPanel panBtns = new JPanel(new GridLayout(1,0));
        btnAddForce = new JButton(resourceMap.getString("btnAddForce.text"));
        btnAddForce.addActionListener(evt -> addForce());
        btnAddForce.setEnabled(scenario.getStatus().isCurrent());
        panBtns.add(btnAddForce);

        btnEditForce = new JButton(resourceMap.getString("btnEditForce.text"));
        btnEditForce.setEnabled(false);
        btnEditForce.addActionListener(evt -> editForce());
        btnEditForce.setEnabled(false);
        panBtns.add(btnEditForce);

        btnDeleteForce = new JButton(resourceMap.getString("btnDeleteForce.text"));
        btnDeleteForce.setEnabled(false);
        btnDeleteForce.addActionListener(evt -> deleteForce());
        btnDeleteForce.setEnabled(false);
        panBtns.add(btnDeleteForce);
        panOtherForces.add(panBtns, BorderLayout.PAGE_START);

        forcesTable = new JTable(forcesModel);
        TableColumn column;
        for (int i = 0; i < BotForceTableModel.N_COL; i++) {
            column = forcesTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(forcesModel.getColumnWidth(i));
            column.setCellRenderer(forcesModel.getRenderer());
        }
        forcesTable.setIntercellSpacing(new Dimension(0, 0));
        forcesTable.setShowGrid(false);
        forcesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        forcesTable.getSelectionModel().addListSelectionListener(this::forcesTableValueChanged);

        panOtherForces.add(new JScrollPane(forcesTable), BorderLayout.CENTER);
    }

    private void forcesTableValueChanged(ListSelectionEvent evt) {
        int row = forcesTable.getSelectedRow();
        btnDeleteForce.setEnabled(row != -1);
        btnEditForce.setEnabled(row != -1);
    }

    private void addForce() {
        CustomizeBotForceDialog cbfd = new CustomizeBotForceDialog(frame, true, null, campaign);
        cbfd.setVisible(true);
        if (null != cbfd.getBotForce()) {
            forcesModel.addForce(cbfd.getBotForce());
        }
        refreshForcesTable();
    }

    private void editForce() {
        BotForce bf = forcesModel.getBotForceAt(forcesTable.getSelectedRow());
        if (null != bf) {
            CustomizeBotForceDialog cbfd = new CustomizeBotForceDialog(frame, true, bf, campaign);
            cbfd.setVisible(true);
            refreshForcesTable();
        }
    }

    private void deleteForce() {
        int row = forcesTable.getSelectedRow();
        if (-1 != row) {
            botForces.remove(row);
        }
        refreshForcesTable();
    }

    private void refreshForcesTable() {
        int selectedRow = forcesTable.getSelectedRow();
        forcesModel.setData(botForces);
        if (selectedRow != -1) {
            if (forcesTable.getRowCount() > 0) {
                if (forcesTable.getRowCount() == selectedRow) {
                    forcesTable.setRowSelectionInterval(selectedRow-1, selectedRow-1);
                } else {
                    forcesTable.setRowSelectionInterval(selectedRow, selectedRow);
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
