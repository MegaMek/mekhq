/*
 * ContractSummaryPanel.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.util.EncodeControl;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.universe.Systems;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;

/**
 * Contract summary view for ContractMarketDialog
 *
 * @author Neoancient
 *
 */
public class ContractSummaryPanel extends JPanel {
    private static final long serialVersionUID = 8773615661962644614L;

    private Campaign campaign;
    private CampaignGUI gui;
    private Contract contract;
    private boolean allowRerolls;
    private int cmdRerolls;
    private int logRerolls;
    private int tranRerolls;

    private JPanel mainPanel;

    private JTextField txtName;
    private JTextArea txtCommand;
    private JTextArea txtTransport;
    private JTextArea txtStraightSupport;
    private JTextArea txtBattleLossComp;

    private ResourceBundle resourceMap;
    private ContractPaymentBreakdown contractPaymentBreakdown;

    public ContractSummaryPanel(Contract contract, Campaign campaign, CampaignGUI gui, boolean allowRerolls) {
        this.contract = contract;
        this.campaign = campaign;
        this.gui = gui;
        this.allowRerolls = allowRerolls;
        if (allowRerolls) {
            Person admin = campaign.findBestInRole(Person.T_ADMIN_COM, SkillType.S_NEG, SkillType.S_ADMIN);
            cmdRerolls = (admin == null || admin.getSkill(SkillType.S_NEG) == null)
                    ? 0 : admin.getSkill(SkillType.S_NEG).getLevel();
            admin = campaign.findBestInRole(Person.T_ADMIN_LOG, SkillType.S_NEG, SkillType.S_ADMIN);
            logRerolls = (admin == null || admin.getSkill(SkillType.S_NEG) == null)
                    ? 0 : admin.getSkill(SkillType.S_NEG).getLevel();
            admin = campaign.findBestInRole(Person.T_ADMIN_TRA, SkillType.S_NEG, SkillType.S_ADMIN);
            tranRerolls = (admin == null || admin.getSkill(SkillType.S_NEG) == null)
                    ? 0 : admin.getSkill(SkillType.S_NEG).getLevel();
        }

        initComponents();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gbc;

        mainPanel = new JPanel();

        setLayout(new java.awt.GridBagLayout());

        mainPanel.setName("pnlStats");
        mainPanel.setBorder(BorderFactory.createTitledBorder(contract.getName()));
        contractPaymentBreakdown = new ContractPaymentBreakdown(mainPanel, contract, campaign);

        fillStats();

        gbc = new java.awt.GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new java.awt.Insets(5, 5, 5, 5);
        gbc.fill = java.awt.GridBagConstraints.BOTH;
        gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(mainPanel, gbc);

    }

    private void fillStats() {

        String[] skillNames = {"Green", "Regular", "Veteran", "Elite"};
        String[] ratingNames = {"F", "D", "C", "B", "A"};

        JLabel lblName = new JLabel();
        txtName = new JTextField();
        JLabel lblLocation = new JLabel();
        JLabel txtLocation = new JLabel();
        JLabel lblDistance = new JLabel();
        JTextArea txtDistance = new JTextArea();
        JLabel lblEmployer = new JLabel();
        JTextArea txtEmployer = new JTextArea();
        JLabel lblEnemy = new JLabel();
        JTextArea txtEnemy = new JTextArea();
        JLabel lblMissionType = new JLabel();
        JTextArea txtMissionType = new JTextArea();
        JLabel lblStartDate = new JLabel();
        JTextArea txtStartDate = new JTextArea();
        JLabel lblLength = new JLabel();
        JTextArea txtLength = new JTextArea();
        JLabel lblAllyRating = new JLabel();
        JTextArea txtAllyRating = new JTextArea();
        JLabel lblEnemyRating = new JLabel();
        JTextArea txtEnemyRating = new JTextArea();

        JLabel lblOverhead = new JLabel();
        JTextArea txtOverhead = new JTextArea();
        JLabel lblCommand = new JLabel();
        txtCommand = new JTextArea();
        JLabel lblTransport = new JLabel();
        txtTransport = new JTextArea();
        JLabel lblStraightSupport = new JLabel();
        txtStraightSupport = new JTextArea();
        JLabel lblBattleLossComp = new JLabel();
        txtBattleLossComp = new JTextArea();
        JLabel lblRequiredLances = new JLabel();
        JTextArea txtRequiredLances = new JTextArea();
        JLabel lblSalvageRights = new JLabel();
        JTextArea txtSalvageRights = new JTextArea();

        resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractMarketDialog",
                new EncodeControl());

        java.awt.GridBagConstraints gridBagConstraints;
        mainPanel.setLayout(new java.awt.GridBagLayout());

        // TODO : Remove Inline date
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");

        int y = 0;

        lblName.setName("lblName");
        lblName.setText(resourceMap.getString("lblName.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblName, gridBagConstraints);

        txtName.setName("txtName");
        txtName.setText(contract.getName());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtName, gridBagConstraints);

        lblEmployer.setName("lblEmployer");
        lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblEmployer, gridBagConstraints);

        txtEmployer.setName("txtEmployer");
        txtEmployer.setText(contract.getEmployer());
        txtEmployer.setEditable(false);
        txtEmployer.setLineWrap(true);
        txtEmployer.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtEmployer, gridBagConstraints);

        if (contract instanceof AtBContract) {
            lblEnemy.setName("lblEnemy");
            lblEnemy.setText(resourceMap.getString("lblEnemy.text"));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(lblEnemy, gridBagConstraints);

            txtEnemy.setName("txtEnemy");
            txtEnemy.setText(((AtBContract)contract).getEnemyName(campaign.getGameYear()));
            txtEnemy.setEditable(false);
            txtEnemy.setLineWrap(true);
            txtEnemy.setWrapStyleWord(true);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(txtEnemy, gridBagConstraints);
        }

        lblMissionType.setName("lblMissionType");
        lblMissionType.setText(resourceMap.getString("lblMissionType.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblMissionType, gridBagConstraints);

        txtMissionType.setName("txtMissionType");
        txtMissionType.setText(contract.getType());
        txtMissionType.setEditable(false);
        txtMissionType.setLineWrap(true);
        txtMissionType.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtMissionType, gridBagConstraints);

        lblLocation.setName("lblLocation");
        lblLocation.setText(resourceMap.getString("lblLocation.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblLocation, gridBagConstraints);

        txtLocation.setName("txtLocation");
        String systemName = contract.getSystemName(Utilities.getDateTimeDay(campaign.getCalendar()));
        txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
        txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtLocation.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Display where it is on the interstellar map
                gui.getMapTab().switchSystemsMap(contract.getSystem());
                gui.setSelectedTab(GuiTabType.MAP);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtLocation, gridBagConstraints);

        if (Systems.getInstance().getSystems().get(contract.getSystemId()) != null) {
            lblDistance.setName("lblDistance");
            lblDistance.setText(resourceMap.getString("lblDistance.text"));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(lblDistance, gridBagConstraints);

            txtDistance.setName("txtDistance");
            JumpPath path = campaign.calculateJumpPath(campaign.getCurrentSystem(), contract.getSystem());
            int days = (int)Math.ceil((path).getTotalTime(Utilities.getDateTimeDay(contract.getStartDate()),
                    campaign.getLocation().getTransitTime()));
            int jumps = path.getJumps();
            if (campaign.getCurrentSystem().getId().equals(contract.getSystemId())
                    && campaign.getLocation().isOnPlanet()) {
                days = 0;
                jumps = 0;
            }
            txtDistance.setText(days + "(" + jumps + ")");
            txtDistance.setEditable(false);
            txtDistance.setLineWrap(true);
            txtDistance.setWrapStyleWord(true);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(txtDistance, gridBagConstraints);
        }

        lblAllyRating.setName("lblAllyRating");
        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblAllyRating, gridBagConstraints);

        if (contract instanceof AtBContract) {
            txtAllyRating.setName("txtAllyRating");
            txtAllyRating.setText(skillNames[((AtBContract)contract).getAllySkill()] + "/"
                    + ratingNames[((AtBContract)contract).getAllyQuality()]);
            txtAllyRating.setEditable(false);
            txtAllyRating.setLineWrap(true);
            txtAllyRating.setWrapStyleWord(true);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(txtAllyRating, gridBagConstraints);

            lblEnemyRating.setName("lblEnemyRating");
            lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text"));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(lblEnemyRating, gridBagConstraints);

            txtEnemyRating.setName("txtEnemyRating");
            txtEnemyRating.setText(skillNames[((AtBContract)contract).getEnemySkill()] + "/" +
                    ratingNames[((AtBContract)contract).getEnemyQuality()]);
            txtEnemyRating.setEditable(false);
            txtEnemyRating.setLineWrap(true);
            txtEnemyRating.setWrapStyleWord(true);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(txtEnemyRating, gridBagConstraints);
        }

        lblStartDate.setName("lblStartDate");
        lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblStartDate, gridBagConstraints);

        txtStartDate.setName("txtStartDate");
        txtStartDate.setText(shortDateFormat.format(contract.getStartDate()));
        txtStartDate.setEditable(false);
        txtStartDate.setLineWrap(true);
        txtStartDate.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtStartDate, gridBagConstraints);

        lblLength.setName("lblLength");
        lblLength.setText(resourceMap.getString("lblLength.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblLength, gridBagConstraints);

        txtLength.setName("txtLength");
        txtLength.setText(Integer.toString(contract.getLength()));
        txtLength.setEditable(false);
        txtLength.setLineWrap(true);
        txtLength.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtLength, gridBagConstraints);

        lblOverhead.setName("lblOverhead");
        lblOverhead.setText(resourceMap.getString("lblOverhead.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblOverhead, gridBagConstraints);

        txtOverhead.setName("txtOverhead");
        txtOverhead.setText(Contract.getOverheadCompName(contract.getOverheadComp()));
        txtOverhead.setEditable(false);
        txtOverhead.setLineWrap(true);
        txtOverhead.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtOverhead, gridBagConstraints);

        lblCommand.setName("lblCommand");
        lblCommand.setText(resourceMap.getString("lblCommand.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblCommand, gridBagConstraints);

        txtCommand.setName("txtCommand");
        txtCommand.setText(Contract.getCommandRightsName(contract.getCommandRights()));
        txtCommand.setEditable(false);
        txtCommand.setLineWrap(true);
        txtCommand.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        Box commandBox = Box.createHorizontalBox();
        commandBox.add(txtCommand);

        /* Only allow command clause rerolls for mercenaries and pirates; house units are always integrated */
        if (hasCommandRerolls()) {
            JButton btnCommand = new JButton();
            setCommandRerollButtonText(btnCommand);
            commandBox.add(btnCommand);

            btnCommand.addActionListener(ev -> {
                JButton btn = null;
                if (ev.getSource() instanceof JButton) {
                    btn = (JButton)ev.getSource();
                }
                if (null == btn) {
                    return;
                }
                if (contract instanceof AtBContract) {
                    campaign.getContractMarket().rerollClause((AtBContract) contract,
                            ContractMarket.CLAUSE_COMMAND, campaign);
                    setCommandRerollButtonText((JButton) ev.getSource());
                    txtCommand.setText(Contract.getCommandRightsName(contract.getCommandRights()));
                    if (campaign.getContractMarket().getRerollsUsed(contract,
                            ContractMarket.CLAUSE_COMMAND) >= cmdRerolls) {
                        btn.setEnabled(false);
                    }
                    refreshAmounts();
                }
            });
        }
        mainPanel.add(commandBox, gridBagConstraints);
        y++;

        lblTransport.setName("lblTransport");
        lblTransport.setText(resourceMap.getString("lblTransport.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblTransport, gridBagConstraints);

        txtTransport.setName("txtTransport");
        txtTransport.setText(contract.getTransportComp() + "%");
        txtTransport.setEditable(false);
        txtTransport.setLineWrap(true);
        txtTransport.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        Box transportBox = Box.createHorizontalBox();
        transportBox.add(txtTransport);

        if (hasTransportRerolls()) {
            JButton btnTransport = new JButton();
            setTransportRerollButtonText(btnTransport);
            transportBox.add(btnTransport);
            btnTransport.addActionListener(ev -> {
                JButton btn = null;
                if (ev.getSource() instanceof JButton) {
                    btn = (JButton) ev.getSource();
                }
                if (null == btn) {
                    return;
                }
                if (contract instanceof AtBContract) {
                    campaign.getContractMarket().rerollClause((AtBContract) contract,
                            ContractMarket.CLAUSE_TRANSPORT, campaign);
                    setTransportRerollButtonText((JButton) ev.getSource());
                    txtTransport.setText(contract.getTransportComp() + "%");
                    if (campaign.getContractMarket().getRerollsUsed(contract,
                            ContractMarket.CLAUSE_TRANSPORT) >= tranRerolls) {
                        btn.setEnabled(false);
                    }
                    refreshAmounts();
                }
            });
        }
        mainPanel.add(transportBox, gridBagConstraints);
        y++;

        lblSalvageRights.setName("lblSalvageRights");
        lblSalvageRights.setText(resourceMap.getString("lblSalvageRights.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblSalvageRights, gridBagConstraints);

        txtSalvageRights.setName("txtSalvageRights");
        txtSalvageRights.setText(contract.getSalvagePct() + "%"
                + (contract.isSalvageExchange() ? " (Exchange)" : ""));
        txtSalvageRights.setEditable(false);
        txtSalvageRights.setLineWrap(true);
        txtSalvageRights.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtSalvageRights, gridBagConstraints);

        lblStraightSupport.setName("lblStraightSupport");
        lblStraightSupport.setText(resourceMap.getString("lblStraightSupport.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblStraightSupport, gridBagConstraints);

        txtStraightSupport.setName("txtStraightSupport");
        txtStraightSupport.setText(contract.getStraightSupport() + "%");
        txtStraightSupport.setEditable(false);
        txtStraightSupport.setLineWrap(true);
        txtStraightSupport.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;

        Box supportBox = Box.createHorizontalBox();
        supportBox.add(txtStraightSupport);

        if (hasSupportRerolls()) {
            JButton btnSupport = new JButton();
            setSupportRerollButtonText(btnSupport);
            supportBox.add(btnSupport);
            btnSupport.addActionListener(ev -> {
                JButton btn = null;
                if (ev.getSource() instanceof JButton) {
                    btn = (JButton) ev.getSource();
                }
                if (null == btn) {
                    return;
                }
                if (contract instanceof AtBContract) {
                    campaign.getContractMarket().rerollClause((AtBContract) contract,
                            ContractMarket.CLAUSE_SUPPORT, campaign);
                    setSupportRerollButtonText((JButton)ev.getSource());
                    txtStraightSupport.setText(contract.getStraightSupport() + "%");
                    txtBattleLossComp.setText(contract.getBattleLossComp() + "%");
                    if (campaign.getContractMarket().getRerollsUsed(contract,
                            ContractMarket.CLAUSE_SUPPORT) >= logRerolls) {
                        btn.setEnabled(false);
                    }
                    refreshAmounts();
                }
            });
        }
        mainPanel.add(supportBox, gridBagConstraints);
        y++;

        lblBattleLossComp.setName("lblBattleLossComp");
        lblBattleLossComp.setText(resourceMap.getString("lblBattleLossComp.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(lblBattleLossComp, gridBagConstraints);

        txtBattleLossComp.setName("txtBattleLossComp");
        txtBattleLossComp.setText(contract.getBattleLossComp() + "%");
        txtBattleLossComp.setEditable(false);
        txtBattleLossComp.setLineWrap(true);
        txtBattleLossComp.setWrapStyleWord(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        mainPanel.add(txtBattleLossComp, gridBagConstraints);

        if (contract instanceof AtBContract) {
            lblRequiredLances.setName("lblRequiredLances");
            lblRequiredLances.setText(resourceMap.getString("lblRequiredLances.text"));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(lblRequiredLances, gridBagConstraints);

            txtRequiredLances.setName("txtRequiredLances");
            txtRequiredLances.setText(((AtBContract)contract).getRequiredLances() + " Lance(s)");
            txtRequiredLances.setEditable(false);
            txtRequiredLances.setLineWrap(true);
            txtRequiredLances.setWrapStyleWord(true);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            mainPanel.add(txtRequiredLances, gridBagConstraints);
        }

        contractPaymentBreakdown.display(y);
    }

    private boolean hasTransportRerolls() {
        return allowRerolls && (campaign.getContractMarket().getRerollsUsed(contract,
                ContractMarket.CLAUSE_TRANSPORT) < tranRerolls);
    }

    private boolean hasCommandRerolls() {
        return allowRerolls
                && (campaign.getFactionCode().equals("MERC")
                    || campaign.getFactionCode().equals("PIR"))
                && (campaign.getContractMarket().getRerollsUsed(contract,
                ContractMarket.CLAUSE_COMMAND) < cmdRerolls);
    }

    private boolean hasSupportRerolls(){
        return allowRerolls && (campaign.getContractMarket().getRerollsUsed(contract,
                ContractMarket.CLAUSE_SUPPORT) < logRerolls);
    }

    private void setCommandRerollButtonText(JButton rerollButton){
        int rerolls = (cmdRerolls - campaign.getContractMarket().getRerollsUsed(contract,
                ContractMarket.CLAUSE_COMMAND));
        rerollButton.setText(generateRerollText(rerolls));
    }

    private void setTransportRerollButtonText(JButton rerollButton){
        int rerolls = (tranRerolls - campaign.getContractMarket().getRerollsUsed(contract,
                ContractMarket.CLAUSE_TRANSPORT));
        rerollButton.setText(generateRerollText(rerolls));
    }

    private void setSupportRerollButtonText(JButton rerollButton){
        int rerolls = (logRerolls - campaign.getContractMarket().getRerollsUsed(contract,
                ContractMarket.CLAUSE_SUPPORT));
        rerollButton.setText(generateRerollText(rerolls));
    }

    private String generateRerollText(int rerolls){
        return resourceMap.getString("lblRenegotiate.text") + " (" + rerolls + ")";
    }

    public void refreshAmounts() {
        contractPaymentBreakdown.refresh();
    }

    public String getContractName() {
        return txtName.getText();
    }
}
