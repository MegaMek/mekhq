/*
 * ContractSummaryPanel.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (c) 2020 - The MegaMek Team
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

    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractMarketDialog",
            new EncodeControl());
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
        setLayout(new GridBagLayout());

        mainPanel = new JPanel();
        mainPanel.setName("pnlStats");
        mainPanel.setBorder(BorderFactory.createTitledBorder(contract.getName()));
        contractPaymentBreakdown = new ContractPaymentBreakdown(mainPanel, contract, campaign);

        fillStats();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        add(mainPanel, gbc);
    }

    private void fillStats() {
        //region Variable Declarations
        // TODO : Remove Inline date
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy/MM/dd/");

        GridBagConstraints gridBagConstraints;
        int y = 0;

        // TODO : These two arrays should probably be pulled from elsewhere
        String[] skillNames = {"Green", "Regular", "Veteran", "Elite"};
        String[] ratingNames = {"F", "D", "C", "B", "A"};
        //endregion Variable Declarations

        mainPanel.setLayout(new GridBagLayout());

        JLabel lblName = new JLabel(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblName, gridBagConstraints);

        txtName = new JTextField(contract.getName());
        txtName.setName("txtName");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtName, gridBagConstraints);

        JLabel lblEmployer = new JLabel(resourceMap.getString("lblEmployer.text"));
        lblEmployer.setName("lblEmployer");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblEmployer, gridBagConstraints);

        JTextArea txtEmployer = new JTextArea(contract.getEmployer());
        txtEmployer.setName("txtEmployer");
        txtEmployer.setEditable(false);
        txtEmployer.setLineWrap(true);
        txtEmployer.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtEmployer, gridBagConstraints);

        if (contract instanceof AtBContract) {
            JLabel lblEnemy = new JLabel(resourceMap.getString("lblEnemy.text"));
            lblEnemy.setName("lblEnemy");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            mainPanel.add(lblEnemy, gridBagConstraints);

            JTextArea txtEnemy = new JTextArea(((AtBContract)contract).getEnemyName(campaign.getGameYear()));
            txtEnemy.setName("txtEnemy");
            txtEnemy.setEditable(false);
            txtEnemy.setLineWrap(true);
            txtEnemy.setWrapStyleWord(true);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            mainPanel.add(txtEnemy, gridBagConstraints);
        }

        JLabel lblMissionType = new JLabel(resourceMap.getString("lblMissionType.text"));
        lblMissionType.setName("lblMissionType");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblMissionType, gridBagConstraints);

        JTextArea txtMissionType = new JTextArea(contract.getType());
        txtMissionType.setName("txtMissionType");
        txtMissionType.setEditable(false);
        txtMissionType.setLineWrap(true);
        txtMissionType.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtMissionType, gridBagConstraints);

        JLabel lblLocation = new JLabel(resourceMap.getString("lblLocation.text"));
        lblLocation.setName("lblLocation");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblLocation, gridBagConstraints);

        JLabel txtLocation = new JLabel(String.format("<html><a href='#'>%s</a></html>",
                contract.getSystemName(Utilities.getDateTimeDay(campaign.getCalendar()))));
        txtLocation.setName("txtLocation");
        txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtLocation.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Display where it is on the interstellar map
                gui.getMapTab().switchSystemsMap(contract.getSystem());
                gui.setSelectedTab(GuiTabType.MAP);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtLocation, gridBagConstraints);

        if (Systems.getInstance().getSystems().get(contract.getSystemId()) != null) {
            JLabel lblDistance = new JLabel(resourceMap.getString("lblDistance.text"));
            lblDistance.setName("lblDistance");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            mainPanel.add(lblDistance, gridBagConstraints);

            JumpPath path = campaign.calculateJumpPath(campaign.getCurrentSystem(), contract.getSystem());
            int days = (int)Math.ceil((path).getTotalTime(Utilities.getDateTimeDay(contract.getStartDate()),
                    campaign.getLocation().getTransitTime()));
            int jumps = path.getJumps();
            if (campaign.getCurrentSystem().getId().equals(contract.getSystemId())
                    && campaign.getLocation().isOnPlanet()) {
                days = 0;
                jumps = 0;
            }
            JTextArea txtDistance = new JTextArea(days + "(" + jumps + ")");
            txtDistance.setName("txtDistance");
            txtDistance.setEditable(false);
            txtDistance.setLineWrap(true);
            txtDistance.setWrapStyleWord(true);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            mainPanel.add(txtDistance, gridBagConstraints);
        }

        JLabel lblAllyRating = new JLabel(resourceMap.getString("lblAllyRating.text"));
        lblAllyRating.setName("lblAllyRating");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblAllyRating, gridBagConstraints);

        if (contract instanceof AtBContract) {
            JTextArea txtAllyRating = new JTextArea(skillNames[((AtBContract)contract).getAllySkill()]
                    + "/" + ratingNames[((AtBContract)contract).getAllyQuality()]);
            txtAllyRating.setName("txtAllyRating");
            txtAllyRating.setEditable(false);
            txtAllyRating.setLineWrap(true);
            txtAllyRating.setWrapStyleWord(true);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            mainPanel.add(txtAllyRating, gridBagConstraints);

            JLabel lblEnemyRating = new JLabel(resourceMap.getString("lblEnemyRating.text"));
            lblEnemyRating.setName("lblEnemyRating");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            mainPanel.add(lblEnemyRating, gridBagConstraints);

            JTextArea txtEnemyRating = new JTextArea(skillNames[((AtBContract)contract).getEnemySkill()]
                    + "/" + ratingNames[((AtBContract)contract).getEnemyQuality()]);
            txtEnemyRating.setName("txtEnemyRating");
            txtEnemyRating.setEditable(false);
            txtEnemyRating.setLineWrap(true);
            txtEnemyRating.setWrapStyleWord(true);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            mainPanel.add(txtEnemyRating, gridBagConstraints);
        }

        JLabel lblStartDate = new JLabel(resourceMap.getString("lblStartDate.text"));
        lblStartDate.setName("lblStartDate");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblStartDate, gridBagConstraints);

        JTextArea txtStartDate = new JTextArea(shortDateFormat.format(contract.getStartDate()));
        txtStartDate.setName("txtStartDate");
        txtStartDate.setEditable(false);
        txtStartDate.setLineWrap(true);
        txtStartDate.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtStartDate, gridBagConstraints);

        JLabel lblLength = new JLabel(resourceMap.getString("lblLength.text"));
        lblLength.setName("lblLength");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblLength, gridBagConstraints);

        JTextArea txtLength = new JTextArea(Integer.toString(contract.getLength()));
        txtLength.setName("txtLength");
        txtLength.setEditable(false);
        txtLength.setLineWrap(true);
        txtLength.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtLength, gridBagConstraints);

        JLabel lblOverhead = new JLabel(resourceMap.getString("lblOverhead.text"));
        lblOverhead.setName("lblOverhead");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblOverhead, gridBagConstraints);

        JTextArea txtOverhead = new JTextArea(Contract.getOverheadCompName(contract.getOverheadComp()));
        txtOverhead.setName("txtOverhead");
        txtOverhead.setEditable(false);
        txtOverhead.setLineWrap(true);
        txtOverhead.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtOverhead, gridBagConstraints);

        JLabel lblCommand = new JLabel(resourceMap.getString("lblCommand.text"));
        lblCommand.setName("lblCommand");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblCommand, gridBagConstraints);

        txtCommand = new JTextArea(Contract.getCommandRightsName(contract.getCommandRights()));
        txtCommand.setName("txtCommand");
        txtCommand.setEditable(false);
        txtCommand.setLineWrap(true);
        txtCommand.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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
                    btn = (JButton) ev.getSource();
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

        JLabel lblTransport = new JLabel(resourceMap.getString("lblTransport.text"));
        lblTransport.setName("lblTransport");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblTransport, gridBagConstraints);

        txtTransport = new JTextArea(contract.getTransportComp() + "%");
        txtTransport.setName("txtTransport");
        txtTransport.setEditable(false);
        txtTransport.setLineWrap(true);
        txtTransport.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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

        JLabel lblSalvageRights = new JLabel(resourceMap.getString("lblSalvageRights.text"));
        lblSalvageRights.setName("lblSalvageRights");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblSalvageRights, gridBagConstraints);

        JTextArea txtSalvageRights = new JTextArea(contract.getSalvagePct() + "%"
                + (contract.isSalvageExchange() ? " (Exchange)" : ""));
        txtSalvageRights.setName("txtSalvageRights");
        txtSalvageRights.setEditable(false);
        txtSalvageRights.setLineWrap(true);
        txtSalvageRights.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtSalvageRights, gridBagConstraints);

        JLabel lblStraightSupport = new JLabel(resourceMap.getString("lblStraightSupport.text"));
        lblStraightSupport.setName("lblStraightSupport");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblStraightSupport, gridBagConstraints);

        txtStraightSupport = new JTextArea(contract.getStraightSupport() + "%");
        txtStraightSupport.setName("txtStraightSupport");
        txtStraightSupport.setEditable(false);
        txtStraightSupport.setLineWrap(true);
        txtStraightSupport.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

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
                    setSupportRerollButtonText((JButton) ev.getSource());
                    txtStraightSupport.setText(contract.getStraightSupport() + "%");
                    txtBattleLossComp = new JTextArea(contract.getBattleLossComp() + "%");
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

        JLabel lblBattleLossComp = new JLabel(resourceMap.getString("lblBattleLossComp.text"));
        lblBattleLossComp.setName("lblBattleLossComp");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(lblBattleLossComp, gridBagConstraints);

        txtBattleLossComp.setName("txtBattleLossComp");
        txtBattleLossComp.setText(contract.getBattleLossComp() + "%");
        txtBattleLossComp.setEditable(false);
        txtBattleLossComp.setLineWrap(true);
        txtBattleLossComp.setWrapStyleWord(true);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(txtBattleLossComp, gridBagConstraints);

        if (contract instanceof AtBContract) {
            JLabel lblRequiredLances = new JLabel(resourceMap.getString("lblRequiredLances.text"));
            lblRequiredLances.setName("lblRequiredLances");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            mainPanel.add(lblRequiredLances, gridBagConstraints);

            JTextArea txtRequiredLances = new JTextArea(((AtBContract) contract).getRequiredLances()
                    + " Lance(s)");
            txtRequiredLances.setName("txtRequiredLances");
            txtRequiredLances.setEditable(false);
            txtRequiredLances.setLineWrap(true);
            txtRequiredLances.setWrapStyleWord(true);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
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
