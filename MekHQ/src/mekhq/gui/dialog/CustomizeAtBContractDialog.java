/*
 * CustomizeAtBContract.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;
import mekhq.gui.FactionComboBox;
import mekhq.gui.utilities.JMoneyTextField;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.utilities.MarkdownEditorPanel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * @author Neoancient
 */
public class CustomizeAtBContractDialog extends JDialog {
    private JFrame frame;
    private AtBContract contract;
    private Campaign campaign;
    private Camouflage allyCamouflage;
    private PlayerColour allyColour;
    private Camouflage enemyCamouflage;
    private PlayerColour enemyColour;

    protected JTextField txtName;
    protected FactionComboBox cbEmployer;
    protected FactionComboBox cbEnemy;
    protected JCheckBox chkShowAllFactions;

    protected MMComboBox<AtBContractType> comboContractType;
    protected MarkdownEditorPanel txtDesc;
    protected JSuggestField suggestPlanet;
    protected MMComboBox<SkillLevel> comboAllySkill;
    protected JComboBox<String> cbAllyQuality;
    protected MMComboBox<SkillLevel> comboEnemySkill;
    protected JComboBox<String> cbEnemyQuality;
    protected JSpinner spnRequiredLances;
    protected JMoneyTextField txtBasePay;
    protected MMComboBox<AtBMoraleLevel> comboEnemyMorale;
    protected JSpinner spnContractScoreArbitraryModifier;
    protected JTextField txtAllyBotName;
    protected JTextField txtEnemyBotName;
    protected JButton btnAllyCamo;
    protected JButton btnEnemyCamo;

    protected JButton btnClose;
    protected JButton btnOK;

    Set<String> currentFactions;

    public CustomizeAtBContractDialog(JFrame parent, boolean modal, AtBContract contract, Campaign c) {
        super(parent, modal);
        this.frame = parent;
        this.contract = contract;
        campaign = c;
        allyCamouflage = contract.getAllyCamouflage();
        allyColour = contract.getAllyColour();
        enemyCamouflage = contract.getEnemyCamouflage();
        enemyColour = contract.getEnemyColour();

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    public AtBContract getAtBContract() {
        return contract;
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewContractDialog",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setName("Form");
        setTitle(resourceMap.getString("Form.title"));

        getContentPane().setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createTitledBorder("Contract Details"),
                 BorderFactory.createEmptyBorder(5,5,5,5)));
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBorder(BorderFactory.createCompoundBorder(
                 BorderFactory.createTitledBorder("Bot Settings"),
                 BorderFactory.createEmptyBorder(5,5,5,5)));
        JPanel buttonPanel = new JPanel();
        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        currentFactions = RandomFactionGenerator.getInstance().getCurrentFactions();

        GridBagConstraints gbc = new GridBagConstraints();

        txtName = new JTextField();
        JLabel lblName = new JLabel();
        cbEmployer = new FactionComboBox();
        cbEmployer.addFactionEntries(currentFactions, campaign.getGameYear());
        JLabel lblEmployer = new JLabel();
        cbEnemy = new FactionComboBox();
        cbEnemy.addFactionEntries(currentFactions, campaign.getGameYear());
        JLabel lblEnemy = new JLabel();
        chkShowAllFactions = new JCheckBox();

        comboContractType = new MMComboBox<>("comboContractType", AtBContractType.values());
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

        JLabel lblType = new JLabel();
        btnOK = new JButton();
        btnClose = new JButton();
        txtDesc = new MarkdownEditorPanel("Contract Description");
        JLabel lblPlanetName = new JLabel();
        // TODO : Switch me to use IUnitRating
        String[] ratingNames = {"F", "D", "C", "B", "A"};
        final DefaultComboBoxModel<SkillLevel> allySkillModel = new DefaultComboBoxModel<>();
        allySkillModel.addAll(SkillLevel.getGeneratableValues());
        comboAllySkill = new MMComboBox<>("comboAllySkill", allySkillModel);
        cbAllyQuality = new JComboBox<>(ratingNames);
        JLabel lblAllyRating = new JLabel();
        final DefaultComboBoxModel<SkillLevel> enemySkillModel = new DefaultComboBoxModel<>();
        enemySkillModel.addAll(SkillLevel.getGeneratableValues());
        comboEnemySkill = new MMComboBox<>("comboEnemySkill", enemySkillModel);
        cbEnemyQuality = new JComboBox<>(ratingNames);
        JLabel lblAllyBotName = new JLabel();
        txtAllyBotName = new JTextField();
        JLabel lblEnemyBotName = new JLabel();
        txtEnemyBotName = new JTextField();
        JLabel lblAllyCamo = new JLabel();
        btnAllyCamo = new JButton();
        JLabel lblEnemyCamo = new JLabel();
        btnEnemyCamo = new JButton();
        JLabel lblEnemyRating = new JLabel();
        JLabel lblRequiredLances = new JLabel();

        int requiredLances = contract.getRequiredLances() > 0 ? contract.getRequiredLances() : 1;

        spnRequiredLances = new JSpinner(new SpinnerNumberModel(requiredLances, 1, null, 1));
        JLabel lblEnemyMorale = new JLabel();
        spnContractScoreArbitraryModifier = new JSpinner(
                new SpinnerNumberModel(contract.getContractScoreArbitraryModifier(),
                        null,null,1));
        JLabel lblContractScoreArbitraryModifier = new JLabel();
        
        txtBasePay = new JMoneyTextField();
        txtBasePay.setMoney(contract.getBaseAmount());
        JLabel lblBasePay = new JLabel();

        comboEnemyMorale = new MMComboBox<>("comboEnemyMorale", AtBMoraleLevel.values());
        comboContractType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AtBMoraleLevel) {
                    list.setToolTipText(((AtBMoraleLevel) value).getToolTipText());
                }
                return this;
            }
        });

        int y = 0;

        lblName.setText(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblName, gbc);

        txtName.setText(contract.getName());
        txtName.setName("txtName");

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(txtName, gbc);

        lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
        lblEmployer.setName("lblEmployer");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblEmployer, gbc);

        cbEmployer.setSelectedItemByKey(contract.getEmployerCode());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(cbEmployer, gbc);

        lblEnemy.setText(resourceMap.getString("lblEnemy.text"));
        lblEnemy.setName("lblEnemy");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblEnemy, gbc);

        cbEnemy.setSelectedItemByKey(contract.getEnemyCode());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(cbEnemy, gbc);

        chkShowAllFactions.setText(resourceMap.getString("chkShowAllFactions.text"));
        chkShowAllFactions.setName("chkShowAllFactions");
        chkShowAllFactions.setSelected(false);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(chkShowAllFactions, gbc);
        chkShowAllFactions.addActionListener(evt -> showAllFactions(chkShowAllFactions.isSelected()));

        lblPlanetName.setText(resourceMap.getString("lblPlanetName.text"));
        lblPlanetName.setName("lblPlanetName");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblPlanetName, gbc);

        suggestPlanet = new JSuggestField(this, campaign.getSystemNames());
        suggestPlanet.setText(contract.getSystemName(campaign.getLocalDate()));
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(suggestPlanet, gbc);

        lblType.setText(resourceMap.getString("lblType.text"));
        lblType.setName("lblType");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblType, gbc);

        comboContractType.setSelectedItem(contract.getContractType());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(comboContractType, gbc);

        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text"));
        lblEnemy.setName("lblAllyRating");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblAllyRating, gbc);

        comboAllySkill.setSelectedItem(contract.getAllySkill());
        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(comboAllySkill, gbc);

        cbAllyQuality.setSelectedIndex(contract.getAllyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(cbAllyQuality, gbc);

        lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text"));
        lblEnemyRating.setName("lblEnemyRating");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblEnemyRating, gbc);

        comboEnemySkill.setSelectedItem(contract.getEnemySkill());
        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(comboEnemySkill, gbc);

        cbEnemyQuality.setSelectedIndex(contract.getEnemyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(cbEnemyQuality, gbc);

        lblRequiredLances.setText(resourceMap.getString("lblRequiredLances.text"));
        lblRequiredLances.setName("lblRequiredLances");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblRequiredLances, gbc);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(spnRequiredLances, gbc);
        
        lblEnemyMorale.setText(resourceMap.getString("lblEnemyMorale.text"));
        lblEnemyMorale.setName("lblEnemyMorale");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblEnemyMorale, gbc);

        comboEnemyMorale.setSelectedItem(contract.getMoraleLevel());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(comboEnemyMorale, gbc);

        lblContractScoreArbitraryModifier.setText(resourceMap.getString("lblContractScoreArbitraryModifier.text"));
        lblContractScoreArbitraryModifier.setName("lblContractScoreArbitraryModifier");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblContractScoreArbitraryModifier, gbc);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(spnContractScoreArbitraryModifier, gbc);
        
        lblBasePay.setText(resourceMap.getString("lblBasePay.text"));
        lblBasePay.setName("lblBasePay");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(lblBasePay, gbc);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        leftPanel.add(txtBasePay, gbc);
        

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
        leftPanel.add(txtDesc, gbc);

        y = 0;

        lblAllyBotName.setText(resourceMap.getString("lblAllyBotName.text"));
        lblAllyBotName.setName("lblAllyBotName");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(lblAllyBotName, gbc);

        txtAllyBotName.setText(contract.getAllyBotName());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(txtAllyBotName, gbc);

        lblEnemyBotName.setText(resourceMap.getString("lblEnemyBotName.text"));
        lblEnemyBotName.setName("lblEnemyBotName");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(lblEnemyBotName, gbc);

        txtEnemyBotName.setText(contract.getEnemyBotName());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(txtEnemyBotName, gbc);

        lblAllyCamo.setText(resourceMap.getString("lblAllyCamo.text"));
        lblAllyCamo.setName("lblEnemyBotName");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(lblAllyCamo, gbc);

        btnAllyCamo.setPreferredSize(new Dimension(84, 72));
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(btnAllyCamo, gbc);
        btnAllyCamo.addActionListener(camoButtonListener);
        btnAllyCamo.setIcon(allyCamouflage.getImageIcon());

        lblEnemyCamo.setText(resourceMap.getString("lblEnemyCamo.text"));
        lblEnemyCamo.setName("lblEnemyCamo");
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(lblEnemyCamo, gbc);

        btnEnemyCamo.setPreferredSize(new Dimension(84, 72));
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        rightPanel.add(btnEnemyCamo, gbc);
        btnEnemyCamo.addActionListener(camoButtonListener);
        btnEnemyCamo.setIcon(enemyCamouflage.getImageIcon());

        btnOK.setText(resourceMap.getString("btnOkay.text"));
        btnOK.setName("btnOK");
        btnOK.addActionListener(this::btnOKActionPerformed);
        buttonPanel.add(btnOK, gbc);

        btnClose.setText(resourceMap.getString("btnCancel.text"));
        btnClose.setName("btnClose");
        btnClose.addActionListener(this::btnCloseActionPerformed);
        buttonPanel.add(btnClose, gbc);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(CustomizeAtBContractDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    ActionListener camoButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            CamoChooserDialog ccd;
            if (e.getSource().equals(btnAllyCamo)) {
                ccd = new CamoChooserDialog(frame, allyCamouflage);
                if (ccd.showDialog().isConfirmed()) {
                    allyCamouflage = ccd.getSelectedItem();
                    btnAllyCamo.setIcon(allyCamouflage.getImageIcon());
                }
            } else {
                ccd = new CamoChooserDialog(frame, enemyCamouflage);
                if (ccd.showDialog().isConfirmed()) {
                    enemyCamouflage = ccd.getSelectedItem();
                    btnEnemyCamo.setIcon(enemyCamouflage.getImageIcon());
                }
            }
        }
    };

    private void btnOKActionPerformed(ActionEvent evt) {
        contract.setName(txtName.getText());
        contract.setEmployerCode(cbEmployer.getSelectedItemKey(), campaign.getGameYear());
        contract.setEnemyCode(cbEnemy.getSelectedItemKey());
        contract.setContractType(comboContractType.getSelectedItem());
        contract.setAllySkill(comboAllySkill.getSelectedItem());
        contract.setAllyQuality(cbAllyQuality.getSelectedIndex());
        contract.setEnemySkill(comboEnemySkill.getSelectedItem());
        contract.setEnemyQuality(cbEnemyQuality.getSelectedIndex());
        contract.setRequiredLances((Integer) spnRequiredLances.getValue());
        contract.setMoraleLevel(comboEnemyMorale.getSelectedItem());
        contract.setContractScoreArbitraryModifier((Integer) spnContractScoreArbitraryModifier.getValue());
        contract.setBaseAmount(txtBasePay.getMoney());
        contract.setAllyBotName(txtAllyBotName.getText());
        contract.setEnemyBotName(txtEnemyBotName.getText());
        contract.setAllyCamouflage(allyCamouflage);
        contract.setAllyColour(allyColour);
        contract.setEnemyCamouflage(enemyCamouflage);
        contract.setEnemyColour(enemyColour);

        PlanetarySystem canonSystem = Systems.getInstance().getSystemByName(suggestPlanet.getText(),
                campaign.getLocalDate());

        if (canonSystem != null) {
            contract.setSystemId(canonSystem.getId());
        } else {
            contract.setSystemId(null);
            contract.setLegacyPlanetName(suggestPlanet.getText());
        }

        contract.setDesc(txtDesc.getText());
        this.setVisible(false);
    }

    private void btnCloseActionPerformed(ActionEvent evt) {
        this.setVisible(false);
    }

    private void showAllFactions(boolean allFactions) {
        cbEmployer.removeAllItems();
        cbEnemy.removeAllItems();
        if (allFactions) {
            cbEmployer.addFactionEntries(Factions.getInstance().getFactionList(), campaign.getGameYear());
            cbEnemy.addFactionEntries(Factions.getInstance().getFactionList(), campaign.getGameYear());
        } else {
            cbEmployer.addFactionEntries(currentFactions, campaign.getGameYear());
            cbEnemy.addFactionEntries(currentFactions, campaign.getGameYear());
        }
    }
}
