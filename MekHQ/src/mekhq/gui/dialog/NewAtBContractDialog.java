/*
 * NewAtBContractDialog.java
 *
 * Copyright (c) 2014 - Carl Spain. All rights reserved.
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.*;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.stratcon.StratconContractDefinition;
import mekhq.campaign.stratcon.StratconContractInitializer;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;
import mekhq.gui.FactionComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import mekhq.gui.baseComponents.SortedComboBoxModel;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.utilities.MarkdownEditorPanel;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * @author Neoancient
 */
public class NewAtBContractDialog extends NewContractDialog {
    private static final long serialVersionUID = 7965491540448120578L;

    protected FactionComboBox cbEmployer;
    protected FactionComboBox cbEnemy;
    protected JCheckBox chkShowAllFactions;
    protected JComboBox<String> cbPlanets;
    protected JCheckBox chkShowAllPlanets;
    protected JComboBox<String> cbMissionType;
    protected JComboBox<String> cbAllySkill;
    protected JComboBox<String> cbAllyQuality;
    protected JComboBox<String> cbEnemySkill;
    protected JComboBox<String> cbEnemyQuality;
    protected JSpinner spnShares;
    protected JLabel lblRequiredLances;

    Set<String> currentFactions;
    Set<String> employerSet;

    int dragoonRating;

    public NewAtBContractDialog(JFrame parent, boolean modal, Campaign c) {
        super(parent, modal, c);
        setUserPreferences();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(getClass());
        setName("NewAtBContractDialog");
        preferences.manage(new JWindowPreference(this));
    }

    @Override
    protected void initComponents() {
        currentFactions = RandomFactionGenerator.getInstance().getCurrentFactions();
        employerSet = RandomFactionGenerator.getInstance().getEmployerSet();
        contract = new AtBContract("New Contract");
        contract.calculateContract(campaign);
        ((AtBContract) contract).initContractDetails(campaign);
        IUnitRating rating = campaign.getUnitRating();
        dragoonRating = rating.getUnitRatingAsInteger();
        super.initComponents();

        updateEnemies();
        updatePlanets();

        if (getCurrentEmployerCode() != null) {
            ((AtBContract)contract).setEmployerCode(getCurrentEmployerCode(), campaign.getGameYear());
        }

        if (getCurrentEnemyCode() != null) {
            ((AtBContract)contract).setEnemyCode(getCurrentEnemyCode());
        }


        if (cbPlanets.getSelectedItem() != null) {
            contract.setSystemId((Systems.getInstance().getSystemByName((String) cbPlanets.getSelectedItem(),
                    campaign.getLocalDate())).getId());
        }

        spnMultiplier.setModel(new SpinnerNumberModel(contract.getMultiplier(), 0.1, 10.0, 0.1));
        updatePaymentMultiplier();
        contract.calculateContract(campaign);
        this.doUpdateContract(cbPlanets);

        addAllListeners();
    }

    @Override
    protected void initDescPanel(ResourceBundle resourceMap, JPanel descPanel) {
        AtBContract contract = (AtBContract)(this.contract);

        java.awt.GridBagConstraints gbc;
        txtName = new JTextField();
        JLabel lblName = new JLabel();
        cbEmployer = new FactionComboBox();
        cbEmployer.addFactionEntries(employerSet, campaign.getGameYear());
        JLabel lblEmployer = new JLabel();
        cbEnemy = new FactionComboBox();
        JLabel lblEnemy = new JLabel();
        chkShowAllFactions = new JCheckBox();
        cbPlanets = new JComboBox<>();
        cbPlanets.setModel(new SortedComboBoxModel<>());
        chkShowAllPlanets = new JCheckBox();
        cbMissionType = new JComboBox<>(AtBContract.missionTypeNames);
        JLabel lblType = new JLabel();
        btnOK = new JButton();
        btnClose = new JButton();
        txtDesc = new MarkdownEditorPanel();
        JLabel lblPlanetName = new JLabel();
        // TODO : Switch me to use a modified RandomSkillsGenerator.levelNames
        String[] skillNames = {"Green", "Regular", "Veteran", "Elite"};
        // TODO : Switch me to use IUnitRating
        String[] ratingNames = {"F", "D", "C", "B", "A"};
        cbAllySkill = new JComboBox<>(skillNames);
        cbAllyQuality = new JComboBox<>(ratingNames);
        JLabel lblAllyRating = new JLabel();
        cbEnemySkill = new JComboBox<>(skillNames);
        cbEnemyQuality = new JComboBox<>(ratingNames);;
        JLabel lblEnemyRating = new JLabel();
        JLabel lblShares = new JLabel();
        spnShares = new JSpinner(new SpinnerNumberModel(20, 20, 50, 10));
        lblRequiredLances = new JLabel();

        int y = 0;

        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblName, gbc);

        txtName.setText(contract.getName());
        txtName.setName("txtName"); // NOI18N

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(txtName, gbc);

        if (campaign.getFactionCode().equals("MERC")) {
            lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
            lblEmployer.setName("lblEmployer");
            gbc.gridx = 0;
            gbc.gridy = y;
            gbc.gridwidth = 1;
            gbc.anchor = java.awt.GridBagConstraints.WEST;
            gbc.insets = new java.awt.Insets(5, 5, 5, 5);
            descPanel.add(lblEmployer, gbc);

            gbc.gridx = 1;
            gbc.gridy = y++;
            gbc.gridwidth = 2;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gbc.anchor = java.awt.GridBagConstraints.WEST;
            gbc.insets = new java.awt.Insets(5, 5, 5, 5);
            descPanel.add(cbEmployer, gbc);
        }

        lblEnemy.setText(resourceMap.getString("lblEnemy.text"));
        lblEnemy.setName("lblEnemy");

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblEnemy, gbc);


        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(cbEnemy, gbc);

        chkShowAllFactions.setText(resourceMap.getString("chkShowAllFactions.text"));
        chkShowAllFactions.setName("chkShowAllFactions");
        chkShowAllFactions.setSelected(false);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(chkShowAllFactions, gbc);
        chkShowAllFactions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                showAllFactions(chkShowAllFactions.isSelected());
            }
        });

        lblPlanetName.setText(resourceMap.getString("lblPlanetName.text")); // NOI18N
        lblPlanetName.setName("lblPlanetName"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblPlanetName, gbc);

        suggestPlanet = new JSuggestField(this, campaign.getSystemNames());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(suggestPlanet, gbc);
        descPanel.add(cbPlanets, gbc);
        suggestPlanet.setVisible(false);

        chkShowAllPlanets.setText(resourceMap.getString("chkShowAllPlanets.text"));
        chkShowAllPlanets.setName("chkShowAllPlanets");
        chkShowAllPlanets.setSelected(false);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(chkShowAllPlanets, gbc);
        chkShowAllPlanets.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                showAllPlanets(chkShowAllPlanets.isSelected());
            }
        });

        lblType.setText(resourceMap.getString("lblType.text")); // NOI18N
        lblType.setName("lblType"); // NOI18N

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblType, gbc);

        cbMissionType.setSelectedItem(contract.getMissionTypeName());
        cbMissionType.setName("cbMissionType"); // NOI18N

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(cbMissionType, gbc);

        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text")); // NOI18N
        lblEnemy.setName("lblAllyRating"); // NOI18N

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblAllyRating, gbc);

        cbAllySkill.setSelectedIndex(contract.getAllySkill());

        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(cbAllySkill, gbc);

        cbAllyQuality.setSelectedIndex(contract.getAllyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(cbAllyQuality, gbc);

        lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text")); // NOI18N
        lblEnemyRating.setName("lblAllyRating"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblEnemyRating, gbc);

        cbEnemySkill.setSelectedIndex(contract.getEnemySkill());
        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(cbEnemySkill, gbc);

        cbEnemyQuality.setSelectedIndex(contract.getEnemyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(cbEnemyQuality, gbc);

        lblShares.setText(resourceMap.getString("lblShares.text")); // NOI18N
        lblShares.setName("lblShares"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(lblShares, gbc);

        spnShares.setName("spnShares"); // NOI18N
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(spnShares, gbc);

        txtDesc.setText(contract.getDescription());;;
        txtDesc.setPreferredSize(new Dimension(400, 200));
        txtDesc.setMinimumSize(new Dimension(400, 200));
        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        descPanel.add(txtDesc, gbc);
    }

    private void addAllListeners() {
        cbPlanets.addActionListener(contractUpdateActionListener);
        cbMissionType.addActionListener(contractUpdateActionListener);
        cbEmployer.addActionListener(contractUpdateActionListener);
        cbEnemy.addActionListener(contractUpdateActionListener);
        cbAllySkill.addActionListener(contractUpdateActionListener);
        cbAllyQuality.addActionListener(contractUpdateActionListener);
        cbEnemySkill.addActionListener(contractUpdateActionListener);
        cbEnemyQuality.addActionListener(contractUpdateActionListener);
        suggestPlanet.addFocusListener(contractUpdateFocusListener);
        suggestPlanet.addActionListener(contractUpdateActionListener);
    }

    private void removeAllListeners() {
        cbPlanets.removeActionListener(contractUpdateActionListener);
        cbMissionType.removeActionListener(contractUpdateActionListener);
        cbEmployer.removeActionListener(contractUpdateActionListener);
        cbEnemy.removeActionListener(contractUpdateActionListener);
        cbAllySkill.removeActionListener(contractUpdateActionListener);
        cbAllyQuality.removeActionListener(contractUpdateActionListener);
        cbEnemySkill.removeActionListener(contractUpdateActionListener);
        cbEnemyQuality.removeActionListener(contractUpdateActionListener);
        suggestPlanet.removeFocusListener(contractUpdateFocusListener);
        suggestPlanet.removeActionListener(contractUpdateActionListener);
    }

    private String getCurrentEmployerCode() {
        if (campaign.getFactionCode().equals("MERC")) {
            return cbEmployer.getSelectedItemKey();
        } else {
            return campaign.getFactionCode();
        }
    }

    private String getCurrentEnemyCode() {
        return cbEnemy.getSelectedItemKey();
    }

    private void updateEnemies() {
        if (chkShowAllFactions.isSelected()) {
            return;
        }
        cbEnemy.removeAllItems();
        if (getCurrentEmployerCode() == null) {
            return;
        }
        cbEnemy.addFactionEntries(RandomFactionGenerator.getInstance().
                getEnemyList(getCurrentEmployerCode()), campaign.getGameYear());
        cbEnemy.setSelectedItemByKey(((AtBContract) contract).getEnemyCode());
    }

    private void showAllFactions(boolean show) {
        removeAllListeners();

        if (show) {
            cbEmployer.removeAllItems();
            cbEnemy.removeAllItems();
            cbEmployer.addFactionEntries(currentFactions, campaign.getGameYear());
            cbEnemy.addFactionEntries(currentFactions, campaign.getGameYear());
            cbEmployer.setSelectedItemByKey(((AtBContract) contract).getEmployerCode());
            cbEnemy.setSelectedItemByKey(((AtBContract) contract).getEnemyCode());
        } else {
            cbEmployer.removeAllItems();
            cbEmployer.addFactionEntries(employerSet, campaign.getGameYear());
            cbEmployer.setSelectedItemByKey(((AtBContract) contract).getEmployerCode());
            updateEnemies();
        }
        addAllListeners();
    }

    private void showAllPlanets(boolean show) {
        removeAllListeners();
        updatePlanets();
        suggestPlanet.setVisible(show);
        cbPlanets.setVisible(!show);
        addAllListeners();
    }

    private void updatePlanets() {
        if (chkShowAllPlanets.isSelected() ||
                getCurrentEmployerCode() == null ||
                getCurrentEnemyCode()== null) {
            return;
        }
        AtBContract contract = (AtBContract) this.contract;
        HashSet<String> systems = new HashSet<>();
        if (contract.getMissionType() >= AtBContract.MT_PLANETARYASSAULT ||
                getCurrentEnemyCode().equals("REB") ||
                getCurrentEnemyCode().equals("PIR")) {
            for (PlanetarySystem p : RandomFactionGenerator.getInstance().
                    getMissionTargetList(getCurrentEmployerCode(), getCurrentEnemyCode())) {
                systems.add(p.getName(campaign.getLocalDate()));
            }
        }
        if ((contract.getMissionType() < AtBContract.MT_PLANETARYASSAULT ||
                contract.getMissionType() == AtBContract.MT_RELIEFDUTY) &&
                !contract.getEnemyCode().equals("REB")) {
            for (PlanetarySystem p : RandomFactionGenerator.getInstance().
                    getMissionTargetList(getCurrentEnemyCode(), getCurrentEmployerCode())) {
                systems.add(p.getName(campaign.getLocalDate()));
            }
        }
        cbPlanets.removeAllItems();
        for (String system : systems) {
            cbPlanets.addItem(system);
        }
    }

    protected void updatePaymentMultiplier() {
        if (((AtBContract)contract).getEmployerCode() != null &&
                ((AtBContract)contract).getEnemyCode() != null) {
            ((AtBContract)contract).calculatePaymentMultiplier(campaign);
            spnMultiplier.setValue(contract.getMultiplier());
        }
    }

    @Override
    protected void btnOKActionPerformed(ActionEvent evt) {//GEN-FIRST:event_btnHireActionPerformed
        if (!btnOK.equals(evt.getSource())) {
            return;
        }

        AtBContract contract = (AtBContract)this.contract;

        contract.setName(txtName.getText());
        if (chkShowAllPlanets.isSelected()) {
            //contract.setPlanetName(suggestPlanet.getText());
        } else {
            contract.setSystemId((Systems.getInstance().getSystemByName((String) cbPlanets.getSelectedItem(),
                    campaign.getLocalDate())).getId());
        }
        contract.setEmployerCode(getCurrentEmployerCode(), campaign.getGameYear());
        contract.setMissionType(cbMissionType.getSelectedIndex());
        contract.setDesc(txtDesc.getText());
        contract.setCommandRights(choiceCommand.getSelectedIndex());

        contract.setEnemyCode(getCurrentEnemyCode());
        contract.setAllySkill(cbAllySkill.getSelectedIndex());
        contract.setAllyQuality(cbAllyQuality.getSelectedIndex());
        contract.setEnemySkill(cbEnemySkill.getSelectedIndex());
        contract.setEnemyQuality(cbEnemyQuality.getSelectedIndex());
        contract.setAllyBotName(contract.getEmployerName(campaign.getGameYear()));
        contract.setEnemyBotName(contract.getEnemyName(campaign.getGameYear()));
        contract.setSharesPct((Integer)spnShares.getValue());

        contract.calculatePartsAvailabilityLevel(campaign);

        campaign.getFinances().credit(contract.getTotalAdvanceAmount(), Transaction.C_CONTRACT,
                "Advance monies for " + contract.getName(), campaign.getLocalDate());
        campaign.addMission(contract);
        
        // note that the contract must be initialized after the mission is added to the campaign
        // to ensure presence of mission ID
        if (campaign.getCampaignOptions().getUseStratCon()) {
            StratconContractInitializer.initializeCampaignState(contract, campaign,
                    StratconContractDefinition.getContractDefinition(contract.getMissionType()));
        }
        
        setVisible(false);
    }

    @Override
    protected void doUpdateContract(Object source) {
        removeAllListeners();

        boolean needUpdatePayment = false;
        AtBContract contract = (AtBContract) this.contract;
        if (cbPlanets.equals(source) && null != cbPlanets.getSelectedItem()) {
            contract.setSystemId((Systems.getInstance().getSystemByName((String) cbPlanets.getSelectedItem(),
                    campaign.getLocalDate())).getId());
            //reset the start date as null so we recalculate travel time
            contract.setStartDate(null);
            needUpdatePayment = true;
        } else if (source.equals(cbEmployer)) {
            MekHQ.getLogger().info("Setting employer code to " + getCurrentEmployerCode());
            long time = System.currentTimeMillis();
            contract.setEmployerCode(getCurrentEmployerCode(), campaign.getGameYear());
            MekHQ.getLogger().info("to set employer code: " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            updateEnemies();
            MekHQ.getLogger().info("to update enemies: " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            updatePlanets();
            MekHQ.getLogger().info("to update planets: " + (System.currentTimeMillis() - time));
            needUpdatePayment = true;
        } else if (source.equals(cbEnemy)) {
            contract.setEnemyCode(getCurrentEnemyCode());
            updatePlanets();
            needUpdatePayment = true;
        } else if (source.equals(cbMissionType)) {
            contract.setMissionType(cbMissionType.getSelectedIndex());
            contract.calculateLength(campaign.getCampaignOptions().getVariableContractLength());
            spnLength.setValue(contract.getLength());
            updatePlanets();
            needUpdatePayment = true;
        } else if (source.equals(cbAllySkill)) {
            contract.setAllySkill(cbAllySkill.getSelectedIndex());
        } else if (source.equals(cbAllyQuality)) {
            contract.setAllyQuality(cbAllyQuality.getSelectedIndex());
        } else if (source.equals(cbEnemySkill)) {
            contract.setEnemySkill(cbEnemySkill.getSelectedIndex());
        } else if (source.equals(cbEnemyQuality)) {
            contract.setEnemyQuality(cbEnemyQuality.getSelectedIndex());
        }

        if (needUpdatePayment) {
            updatePaymentMultiplier();
        }
        super.doUpdateContract(source);

        addAllListeners();
   }
}
