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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.*;

import megamek.client.ui.swing.dialog.imageChooser.CamoChooserDialog;
import megamek.client.ui.swing.util.PlayerColour;
import megamek.common.icons.Camouflage;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.Systems;
import mekhq.gui.FactionComboBox;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.utilities.MarkdownEditorPanel;
import mekhq.preferences.PreferencesNode;

/**
 * @author Neoancient
 */
public class CustomizeAtBContractDialog extends JDialog {
    private static final long serialVersionUID = -7018467869340880912L;
    private JFrame frame;
    private AtBContract contract;
    private Campaign campaign;
    private String allyCamoCategory;
    private String allyCamoFileName;
    private PlayerColour allyColour;
    private String enemyCamoCategory;
    private String enemyCamoFileName;
    private PlayerColour enemyColour;

    protected JTextField txtName;
    protected FactionComboBox cbEmployer;
    protected FactionComboBox cbEnemy;
    protected JCheckBox chkShowAllFactions;

    protected JComboBox<String> cbMissionType;
    protected MarkdownEditorPanel txtDesc;
    protected JSuggestField suggestPlanet;
    protected JComboBox<String> cbAllySkill;
    protected JComboBox<String> cbAllyQuality;
    protected JComboBox<String> cbEnemySkill;
    protected JComboBox<String> cbEnemyQuality;
    protected JSpinner spnRequiredLances;
    protected JComboBox<String> cbEnemyMorale;
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
        allyCamoCategory = contract.getAllyCamoCategory();
        allyCamoFileName = contract.getAllyCamoFileName();
        allyColour = contract.getAllyColour();
        enemyCamoCategory = contract.getEnemyCamoCategory();
        enemyCamoFileName = contract.getEnemyCamoFileName();
        enemyColour = contract.getEnemyColour();

        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private void initComponents() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.NewContractDialog", new EncodeControl());
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
        cbMissionType = new JComboBox<>(AtBContract.missionTypeNames);
        JLabel lblType = new JLabel();
        btnOK = new JButton();
        btnClose = new JButton();
        txtDesc = new MarkdownEditorPanel("Contract Description");
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
        cbEnemyMorale = new JComboBox<String>(AtBContract.moraleLevelNames);

        int y = 0;

        lblName.setText(resourceMap.getString("lblName.text")); // NOI18N
        lblName.setName("lblName"); // NOI18N
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblName, gbc);

        txtName.setText(contract.getName());
        txtName.setName("txtName"); // NOI18N

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(txtName, gbc);

        lblEmployer.setText(resourceMap.getString("lblEmployer.text")); // NOI18N
        lblEmployer.setName("lblEmployer"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblEmployer, gbc);

        cbEmployer.setSelectedItemByKey(contract.getEmployerCode());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(cbEmployer, gbc);

        lblEnemy.setText(resourceMap.getString("lblEnemy.text")); // NOI18N
        lblEnemy.setName("lblEnemy"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblEnemy, gbc);

        cbEnemy.setSelectedItemByKey(contract.getEnemyCode());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(cbEnemy, gbc);

        chkShowAllFactions.setText(resourceMap.getString("chkShowAllFactions.text"));
        chkShowAllFactions.setName("chkShowAllFactions");
        chkShowAllFactions.setSelected(false);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(chkShowAllFactions, gbc);
        chkShowAllFactions.addActionListener(arg0 -> showAllFactions(chkShowAllFactions.isSelected()));

        lblPlanetName.setText(resourceMap.getString("lblPlanetName.text")); // NOI18N
        lblPlanetName.setName("lblPlanetName"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblPlanetName, gbc);

        suggestPlanet = new JSuggestField(this, campaign.getSystemNames());
        suggestPlanet.setText(contract.getSystemName(campaign.getLocalDate()));
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(suggestPlanet, gbc);

        lblType.setText(resourceMap.getString("lblType.text")); // NOI18N
        lblType.setName("lblType"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblType, gbc);

        cbMissionType.setSelectedItem(contract.getMissionTypeName());
        cbMissionType.setName("cbMissionType"); // NOI18N
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(cbMissionType, gbc);

        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text")); // NOI18N
        lblEnemy.setName("lblAllyRating"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblAllyRating, gbc);

        cbAllySkill.setSelectedIndex(contract.getAllySkill());
        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(cbAllySkill, gbc);

        cbAllyQuality.setSelectedIndex(contract.getAllyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(cbAllyQuality, gbc);

        lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text")); // NOI18N
        lblEnemyRating.setName("lblEnemyRating"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblEnemyRating, gbc);

        cbEnemySkill.setSelectedIndex(contract.getEnemySkill());
        gbc.gridx = 1;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(cbEnemySkill, gbc);

        cbEnemyQuality.setSelectedIndex(contract.getEnemyQuality());
        gbc.gridx = 2;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(cbEnemyQuality, gbc);

        lblRequiredLances.setText(resourceMap.getString("lblRequiredLances.text")); // NOI18N
        lblRequiredLances.setName("lblRequiredLances"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblRequiredLances, gbc);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(spnRequiredLances, gbc);

        lblEnemyMorale.setText(resourceMap.getString("lblEnemyMorale.text")); // NOI18N
        lblEnemyMorale.setName("lblEnemyMorale"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblEnemyMorale, gbc);

        cbEnemyMorale.setSelectedIndex(contract.getMoraleLevel());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(cbEnemyMorale, gbc);

        lblContractScoreArbitraryModifier.setText(resourceMap.getString("lblContractScoreArbitraryModifier.text")); // NOI18N
        lblContractScoreArbitraryModifier.setName("lblContractScoreArbitraryModifier"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(lblContractScoreArbitraryModifier, gbc);

        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        leftPanel.add(spnContractScoreArbitraryModifier, gbc);

        txtDesc.setText(contract.getDescription());
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
        leftPanel.add(txtDesc, gbc);

        y = 0;

        lblAllyBotName.setText(resourceMap.getString("lblAllyBotName.text")); // NOI18N
        lblAllyBotName.setName("lblAllyBotName"); // NOI18N
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        rightPanel.add(lblAllyBotName, gbc);

        txtAllyBotName.setText(contract.getAllyBotName());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        rightPanel.add(txtAllyBotName, gbc);

        lblEnemyBotName.setText(resourceMap.getString("lblEnemyBotName.text")); // NOI18N
        lblEnemyBotName.setName("lblEnemyBotName"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        rightPanel.add(lblEnemyBotName, gbc);

        txtEnemyBotName.setText(contract.getEnemyBotName());
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        rightPanel.add(txtEnemyBotName, gbc);

        lblAllyCamo.setText(resourceMap.getString("lblAllyCamo.text")); // NOI18N
        lblAllyCamo.setName("lblEnemyBotName"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        rightPanel.add(lblAllyCamo, gbc);

        btnAllyCamo.setPreferredSize(new Dimension(84, 72));
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        rightPanel.add(btnAllyCamo, gbc);
        btnAllyCamo.addActionListener(camoButtonListener);
        setCamoIcon(btnAllyCamo, allyCamoCategory, allyCamoFileName);

        lblEnemyCamo.setText(resourceMap.getString("lblEnemyCamo.text")); // NOI18N
        lblEnemyCamo.setName("lblEnemyCamo"); // NOI18N
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        rightPanel.add(lblEnemyCamo, gbc);

        btnEnemyCamo.setPreferredSize(new Dimension(84, 72));
        gbc.gridx = 1;
        gbc.gridy = y++;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        rightPanel.add(btnEnemyCamo, gbc);
        btnEnemyCamo.addActionListener(camoButtonListener);
        setCamoIcon(btnEnemyCamo, enemyCamoCategory, enemyCamoFileName);

        btnOK.setText(resourceMap.getString("btnOkay.text")); // NOI18N
        btnOK.setName("btnOK"); // NOI18N
        btnOK.addActionListener(this::btnOKActionPerformed);
        buttonPanel.add(btnOK, gbc);

        btnClose.setText(resourceMap.getString("btnCancel.text")); // NOI18N
        btnClose.setName("btnClose"); // NOI18N
        btnClose.addActionListener(this::btnCloseActionPerformed);
        buttonPanel.add(btnClose, gbc);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(CustomizeAtBContractDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    ActionListener camoButtonListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            CamoChooserDialog ccd;
            if (e.getSource().equals(btnAllyCamo)) {
                ccd = new CamoChooserDialog(frame, new Camouflage(allyCamoCategory, allyCamoFileName));
                if ((ccd.showDialog() == JOptionPane.CANCEL_OPTION) || (ccd.getSelectedItem() == null)) {
                    return;
                }
                allyCamoCategory = ccd.getSelectedItem().getCategory();
                allyCamoFileName = ccd.getSelectedItem().getFilename();
                setCamoIcon(btnAllyCamo, allyCamoCategory, allyCamoFileName);
            } else {
                ccd = new CamoChooserDialog(frame, new Camouflage(enemyCamoCategory, enemyCamoFileName));
                if ((ccd.showDialog() == JOptionPane.CANCEL_OPTION) || (ccd.getSelectedItem() == null)) {
                    return;
                }
                enemyCamoCategory = ccd.getSelectedItem().getCategory();
                enemyCamoFileName = ccd.getSelectedItem().getFilename();
                setCamoIcon(btnEnemyCamo, enemyCamoCategory, enemyCamoFileName);
            }
        }
    };

    /* Copied from CampaignOptionsDialog */
    private void setCamoIcon(JButton btnCamo, String camoCategory, String camoFileName) {
        btnCamo.setIcon(new Camouflage(camoCategory, camoFileName).getImageIcon());
    }

    private void btnOKActionPerformed(ActionEvent evt) {
        contract.setName(txtName.getText());
        contract.setEmployerCode(cbEmployer.getSelectedItemKey(), campaign.getGameYear());
        contract.setEnemyCode(cbEnemy.getSelectedItemKey());
        contract.setMissionType(cbMissionType.getSelectedIndex());
        contract.setAllySkill(cbAllySkill.getSelectedIndex());
        contract.setAllyQuality(cbAllyQuality.getSelectedIndex());
        contract.setEnemySkill(cbEnemySkill.getSelectedIndex());
        contract.setEnemyQuality(cbEnemyQuality.getSelectedIndex());
        contract.setRequiredLances((Integer)spnRequiredLances.getValue());
        contract.setMoraleLevel(cbEnemyMorale.getSelectedIndex());
        contract.setContractScoreArbitraryModifier((Integer)spnContractScoreArbitraryModifier.getValue());
        contract.setAllyBotName(txtAllyBotName.getText());
        contract.setEnemyBotName(txtEnemyBotName.getText());
        contract.setAllyCamoCategory(allyCamoCategory);
        contract.setAllyCamoFileName(allyCamoFileName);
        contract.setAllyColour(allyColour);
        contract.setEnemyCamoCategory(enemyCamoCategory);
        contract.setEnemyCamoFileName(enemyCamoFileName);
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

    public int getMissionId() {
        return contract.getId();
    }

}
