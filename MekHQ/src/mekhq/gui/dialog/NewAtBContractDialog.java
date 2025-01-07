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

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.stratcon.StratconContractDefinition;
import mekhq.campaign.stratcon.StratconContractInitializer;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;
import mekhq.gui.FactionComboBox;
import mekhq.gui.baseComponents.SortedComboBoxModel;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.utilities.MarkdownEditorPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static mekhq.campaign.market.contractMarket.ContractAutomation.contractStartPrompt;

/**
 * @author Neoancient
 */
public class NewAtBContractDialog extends NewContractDialog {
    private static final MMLogger logger = MMLogger.create(NewAtBContractDialog.class);

    protected FactionComboBox cbEmployer;
    protected FactionComboBox cbEnemy;
    protected JCheckBox chkShowAllFactions;
    protected JComboBox<String> cbPlanets;
    protected JCheckBox chkShowAllPlanets;
    protected MMComboBox<AtBContractType> comboContractType;
    protected MMComboBox<SkillLevel> comboAllySkill;
    protected JComboBox<String> cbAllyQuality;
    protected MMComboBox<SkillLevel> comboEnemySkill;
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

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(NewAtBContractDialog.class);
            setName("NewAtBContractDialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    @Override
    protected void initComponents() {
        currentFactions = RandomFactionGenerator.getInstance().getCurrentFactions();
        employerSet = RandomFactionGenerator.getInstance().getEmployerSet();
        contract = new AtBContract("New Contract");
        contract.calculateContract(campaign);
        ((AtBContract) contract).initContractDetails(campaign);
        dragoonRating = campaign.getAtBUnitRatingMod();
        super.initComponents();

        updateEnemies();
        updatePlanets();

        if (getCurrentEmployerCode() != null) {
            ((AtBContract) contract).setEmployerCode(getCurrentEmployerCode(), campaign.getGameYear());
        }

        if (getCurrentEnemyCode() != null) {
            ((AtBContract) contract).setEnemyCode(getCurrentEnemyCode());
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
        AtBContract contract = (AtBContract) (this.contract);

        GridBagConstraints gbc;
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
        JLabel lblType = new JLabel();
        btnOK = new JButton();
        btnClose = new JButton();
        txtDesc = new MarkdownEditorPanel();
        JLabel lblPlanetName = new JLabel();
        // TODO : Switch me to use IUnitRating
        String[] ratingNames = { "F", "D", "C", "B", "A" };

        final DefaultComboBoxModel<SkillLevel> allySkillModel = new DefaultComboBoxModel<>();
        allySkillModel.addAll(SkillLevel.getGeneratableValues());
        comboAllySkill = new MMComboBox<>("comboAllySkill", allySkillModel);
        cbAllyQuality = new JComboBox<>(ratingNames);
        JLabel lblAllyRating = new JLabel();
        final DefaultComboBoxModel<SkillLevel> enemySkillModel = new DefaultComboBoxModel<>();
        enemySkillModel.addAll(SkillLevel.getGeneratableValues());
        comboEnemySkill = new MMComboBox<>("comboEnemySkill", enemySkillModel);
        cbEnemyQuality = new JComboBox<>(ratingNames);
        JLabel lblEnemyRating = new JLabel();
        JLabel lblShares = new JLabel();
        spnShares = new JSpinner(new SpinnerNumberModel(20, 20, 50, 10));
        lblRequiredLances = new JLabel();

        int y = 0;

        lblName.setText(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(lblName, gbc);

        txtName.setText(contract.getName());
        txtName.setName("txtName");

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(txtName, gbc);

        if (campaign.getFaction().isMercenary()) {
            lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
            lblEmployer.setName("lblEmployer");
            gbc.gridx = 0;
            gbc.gridy = y;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(5, 5, 5, 5);
            descPanel.add(lblEmployer, gbc);

            gbc.gridx = 1;
            gbc.gridy = y++;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(5, 5, 5, 5);
            descPanel.add(cbEmployer, gbc);
        }

        lblEnemy.setText(resourceMap.getString("lblEnemy.text"));
        lblEnemy.setName("lblEnemy");

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(lblEnemy, gbc);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(cbEnemy, gbc);

        chkShowAllFactions.setText(resourceMap.getString("chkShowAllFactions.text"));
        chkShowAllFactions.setName("chkShowAllFactions");
        chkShowAllFactions.setSelected(false);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(chkShowAllFactions, gbc);
        chkShowAllFactions.addActionListener(evt -> showAllFactions(chkShowAllFactions.isSelected()));

        lblPlanetName.setText(resourceMap.getString("lblPlanetName.text"));
        lblPlanetName.setName("lblPlanetName");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(lblPlanetName, gbc);

        suggestPlanet = new JSuggestField(this, campaign.getSystemNames());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(suggestPlanet, gbc);
        descPanel.add(cbPlanets, gbc);
        suggestPlanet.setVisible(false);

        chkShowAllPlanets.setText(resourceMap.getString("chkShowAllPlanets.text"));
        chkShowAllPlanets.setName("chkShowAllPlanets");
        chkShowAllPlanets.setSelected(false);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(chkShowAllPlanets, gbc);
        chkShowAllPlanets.addActionListener(evt -> showAllPlanets(chkShowAllPlanets.isSelected()));

        lblType.setText(resourceMap.getString("lblType.text"));
        lblType.setName("lblType");

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(lblType, gbc);

        comboContractType = new MMComboBox<>("comboContractType", AtBContractType.values());
        comboContractType.setSelectedItem(contract.getContractType());
        comboContractType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                    final int index, final boolean isSelected,
                    final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AtBContractType) {
                    list.setToolTipText(((AtBContractType) value).getToolTipText());
                }
                return this;
            }
        });

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(comboContractType, gbc);

        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text"));
        lblEnemy.setName("lblAllyRating");

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(lblAllyRating, gbc);

        comboAllySkill.setSelectedItem(contract.getAllySkill());

        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(comboAllySkill, gbc);

        cbAllyQuality.setSelectedIndex(contract.getAllyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(cbAllyQuality, gbc);

        lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text"));
        lblEnemyRating.setName("lblAllyRating");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(lblEnemyRating, gbc);

        comboEnemySkill.setSelectedItem(contract.getEnemySkill());
        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(comboEnemySkill, gbc);

        cbEnemyQuality.setSelectedIndex(contract.getEnemyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(cbEnemyQuality, gbc);

        lblShares.setText(resourceMap.getString("lblShares.text"));
        lblShares.setName("lblShares");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(lblShares, gbc);

        spnShares.setName("spnShares");
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(spnShares, gbc);

        txtDesc.setText(contract.getDescription());
        txtDesc.setPreferredSize(new Dimension(400, 200));
        txtDesc.setMinimumSize(new Dimension(400, 200));
        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        descPanel.add(txtDesc, gbc);
    }

    private void addAllListeners() {
        cbPlanets.addActionListener(contractUpdateActionListener);
        comboContractType.addActionListener(contractUpdateActionListener);
        cbEmployer.addActionListener(contractUpdateActionListener);
        cbEnemy.addActionListener(contractUpdateActionListener);
        comboAllySkill.addActionListener(contractUpdateActionListener);
        cbAllyQuality.addActionListener(contractUpdateActionListener);
        comboEnemySkill.addActionListener(contractUpdateActionListener);
        cbEnemyQuality.addActionListener(contractUpdateActionListener);
        suggestPlanet.addFocusListener(contractUpdateFocusListener);
        suggestPlanet.addActionListener(contractUpdateActionListener);
    }

    private void removeAllListeners() {
        cbPlanets.removeActionListener(contractUpdateActionListener);
        comboContractType.removeActionListener(contractUpdateActionListener);
        cbEmployer.removeActionListener(contractUpdateActionListener);
        cbEnemy.removeActionListener(contractUpdateActionListener);
        comboAllySkill.removeActionListener(contractUpdateActionListener);
        cbAllyQuality.removeActionListener(contractUpdateActionListener);
        comboEnemySkill.removeActionListener(contractUpdateActionListener);
        cbEnemyQuality.removeActionListener(contractUpdateActionListener);
        suggestPlanet.removeFocusListener(contractUpdateFocusListener);
        suggestPlanet.removeActionListener(contractUpdateActionListener);
    }

    private String getCurrentEmployerCode() {
        return campaign.getFaction().isMercenary() ? cbEmployer.getSelectedItemKey()
                : campaign.getFactionCode();
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
        cbEnemy.addFactionEntries(RandomFactionGenerator.getInstance().getEnemyList(getCurrentEmployerCode()),
                campaign.getGameYear());
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
                getCurrentEnemyCode() == null) {
            return;
        }
        AtBContract contract = (AtBContract) this.contract;
        HashSet<String> systems = new HashSet<>();
        if (!contract.getContractType().isGarrisonType()
                || Factions.getInstance().getFaction(getCurrentEnemyCode()).isRebelOrPirate()) {
            for (PlanetarySystem p : RandomFactionGenerator.getInstance().getMissionTargetList(getCurrentEmployerCode(),
                    getCurrentEnemyCode())) {
                systems.add(p.getName(campaign.getLocalDate()));
            }
        }

        if ((contract.getContractType().isGarrisonType() || contract.getContractType().isReliefDuty())
                && !contract.getEnemy().isRebel()) {
            for (PlanetarySystem p : RandomFactionGenerator.getInstance().getMissionTargetList(getCurrentEnemyCode(),
                    getCurrentEmployerCode())) {
                systems.add(p.getName(campaign.getLocalDate()));
            }
        }

        cbPlanets.removeAllItems();
        for (String system : systems) {
            cbPlanets.addItem(system);
        }
    }

    protected void updatePaymentMultiplier() {
        if (((AtBContract) contract).getEmployerCode() != null &&
                ((AtBContract) contract).getEnemyCode() != null) {
            double multiplier = campaign.getContractMarket().calculatePaymentMultiplier(campaign, (AtBContract) contract);
            contract.setMultiplier(multiplier);
            spnMultiplier.setValue(multiplier);
        }
    }

    @Override
    protected void btnOKActionPerformed(ActionEvent evt) {

        if (!btnOK.equals(evt.getSource())) {
            return;
        }

        if (getCurrentEmployerCode() == null) {
            JOptionPane.showMessageDialog(rootPane, "Make sure you set Employer!",
                    "Contract is Missing Field", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (getCurrentEnemyCode() == null) {
            JOptionPane.showMessageDialog(rootPane, "Make sure you set Enemy!",
                    "Contract is Missing Field", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cbPlanets.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(rootPane, "Make sure you set the Planet!",
                    "Contract is Missing Field", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AtBContract contract = (AtBContract) this.contract;

        contract.setName(txtName.getText());
        if (chkShowAllPlanets.isSelected()) {
            // contract.setPlanetName(suggestPlanet.getText());
        } else {
            contract.setSystemId((Systems.getInstance().getSystemByName((String) cbPlanets.getSelectedItem(),
                    campaign.getLocalDate())).getId());
        }
        contract.setEmployerCode(getCurrentEmployerCode(), campaign.getGameYear());
        contract.setContractType(comboContractType.getSelectedItem());
        contract.setDesc(txtDesc.getText());
        contract.setCommandRights(choiceCommand.getSelectedItem());

        contract.setRequiredLances(AtBContract.calculateRequiredLances(campaign));

        contract.setEnemyCode(getCurrentEnemyCode());
        contract.setAllySkill(comboAllySkill.getSelectedItem());
        contract.setAllyQuality(cbAllyQuality.getSelectedIndex());
        contract.setEnemySkill(comboEnemySkill.getSelectedItem());
        contract.setEnemyQuality(cbEnemyQuality.getSelectedIndex());
        contract.setAllyBotName(contract.getEmployerName(campaign.getGameYear()));
        contract.setEnemyBotName(contract.getEnemyName(campaign.getGameYear()));
        contract.setAtBSharesPercent((Integer) spnShares.getValue());

        contract.setPartsAvailabilityLevel(contract.getContractType().calculatePartsAvailabilityLevel());

        campaign.getFinances().credit(TransactionType.CONTRACT_PAYMENT, campaign.getLocalDate(),
                contract.getTotalAdvanceAmount(), "Advance funds for " + contract.getName());
        campaign.addMission(contract);

        // note that the contract must be initialized after the mission is added to the
        // campaign
        // to ensure presence of mission ID
        if (campaign.getCampaignOptions().isUseStratCon()) {
            StratconContractInitializer.initializeCampaignState(contract, campaign,
                    StratconContractDefinition.getContractDefinition(contract.getContractType()));
        }

        setVisible(false);

        contractStartPrompt(campaign, contract);
    }

    @Override
    protected void doUpdateContract(Object source) {
        removeAllListeners();

        boolean needUpdatePayment = false;
        AtBContract contract = (AtBContract) this.contract;
        if (cbPlanets.equals(source) && null != cbPlanets.getSelectedItem()) {
            contract.setSystemId((Systems.getInstance().getSystemByName((String) cbPlanets.getSelectedItem(),
                    campaign.getLocalDate())).getId());
            // reset the start date as null so we recalculate travel time
            contract.setStartDate(null);
            needUpdatePayment = true;
        } else if (source.equals(cbEmployer)) {
            logger.info("Setting employer code to " + getCurrentEmployerCode());
            long time = System.currentTimeMillis();
            contract.setEmployerCode(getCurrentEmployerCode(), campaign.getGameYear());
            logger.info("to set employer code: " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            updateEnemies();
            logger.info("to update enemies: " + (System.currentTimeMillis() - time));
            time = System.currentTimeMillis();
            updatePlanets();
            logger.info("to update planets: " + (System.currentTimeMillis() - time));
            needUpdatePayment = true;
        } else if (source.equals(cbEnemy)) {
            contract.setEnemyCode(getCurrentEnemyCode());
            updatePlanets();
            needUpdatePayment = true;
        } else if (source.equals(comboContractType)) {
            contract.setContractType(comboContractType.getSelectedItem());
            contract.calculateLength(campaign.getCampaignOptions().isVariableContractLength());
            spnLength.setValue(contract.getLength());
            updatePlanets();
            needUpdatePayment = true;
        } else if (source.equals(comboAllySkill)) {
            contract.setAllySkill(comboAllySkill.getSelectedItem());
        } else if (source.equals(cbAllyQuality)) {
            contract.setAllyQuality(cbAllyQuality.getSelectedIndex());
        } else if (source.equals(comboEnemySkill)) {
            contract.setEnemySkill(comboEnemySkill.getSelectedItem());
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
