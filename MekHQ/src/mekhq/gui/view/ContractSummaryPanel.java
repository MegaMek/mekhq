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
import mekhq.gui.GuiTabType;

/**
 * Contract summary view for ContractMarketDialog
 *
 * @author Neoancient
 */
public class ContractSummaryPanel extends JPanel {
    private static final long serialVersionUID = 8773615661962644614L;

    private Campaign campaign;
    private Contract contract;
    private boolean allowRerolls;
    private int cmdRerolls;
    private int logRerolls;
    private int tranRerolls;

    private JPanel mainPanel;

    private JTextField txtName;
    private JLabel txtCommand;
    private JLabel txtTransport;
    private JLabel txtStraightSupport;
    private JLabel txtBattleLossComp;

    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractMarketDialog",
            new EncodeControl());
    private ContractPaymentBreakdown contractPaymentBreakdown;

    public ContractSummaryPanel(Contract contract, Campaign campaign, boolean allowRerolls) {
        this.contract = contract;
        this.campaign = campaign;
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

        mainPanel = new JPanel(new GridBagLayout());
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
        gbc.anchor = GridBagConstraints.LINE_START;

        add(mainPanel, gbc);
    }

    private void fillStats() {
        //region Variable Initialization
        // TODO : Remove Inline date
        SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy/MM/dd");

        // TODO : These two arrays should probably be pulled from elsewhere
        String[] skillNames = {"Green", "Regular", "Veteran", "Elite"};
        String[] ratingNames = {"F", "D", "C", "B", "A"};

        // Initializing the GridBagConstraint used for Labels
        GridBagConstraints gridBagConstraintsA = new GridBagConstraints();
        gridBagConstraintsA.gridx = 0;
        gridBagConstraintsA.fill = GridBagConstraints.NONE;
        gridBagConstraintsA.anchor = GridBagConstraints.LINE_START;

        // Initializing GridBagConstraint used for the Panels
        GridBagConstraints gridBagConstraintsB = new GridBagConstraints();
        gridBagConstraintsB.gridx = 1;
        gridBagConstraintsB.weightx = 0.5;
        gridBagConstraintsB.insets = new Insets(0, 10, 0, 0);
        gridBagConstraintsB.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraintsB.anchor = GridBagConstraints.LINE_END;

        // Initializing GridBagConstraint used for the Buttons
        GridBagConstraints gridBagConstraintsC = new GridBagConstraints();
        gridBagConstraintsC.gridx = 2;
        gridBagConstraintsC.weightx = 0.5;
        gridBagConstraintsC.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraintsC.anchor = GridBagConstraints.LINE_START;

        int y = 0;
        //endregion Variable Declarations

        JLabel lblName = new JLabel(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gridBagConstraintsA.gridy = y;
        mainPanel.add(lblName, gridBagConstraintsA);

        txtName = new JTextField(contract.getName());
        txtName.setName("txtName");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtName, gridBagConstraintsB);

        JLabel lblEmployer = new JLabel(resourceMap.getString("lblEmployer.text"));
        lblEmployer.setName("lblEmployer");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblEmployer, gridBagConstraintsA);

        JLabel txtEmployer = new JLabel(contract.getEmployer());
        txtEmployer.setName("txtEmployer");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtEmployer, gridBagConstraintsB);

        if (contract instanceof AtBContract) {
            JLabel lblEnemy = new JLabel(resourceMap.getString("lblEnemy.text"));
            lblEnemy.setName("lblEnemy");
            gridBagConstraintsA.gridy = ++y;
            mainPanel.add(lblEnemy, gridBagConstraintsA);

            JLabel txtEnemy = new JLabel(((AtBContract) contract).getEnemyName(campaign.getGameYear()));
            txtEnemy.setName("txtEnemy");
            gridBagConstraintsB.gridy = y;
            mainPanel.add(txtEnemy, gridBagConstraintsB);
        }

        JLabel lblMissionType = new JLabel(resourceMap.getString("lblMissionType.text"));
        lblMissionType.setName("lblMissionType");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblMissionType, gridBagConstraintsA);

        JLabel txtMissionType = new JLabel(contract.getType());
        txtMissionType.setName("txtMissionType");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtMissionType, gridBagConstraintsB);

        JLabel lblLocation = new JLabel(resourceMap.getString("lblLocation.text"));
        lblLocation.setName("lblLocation");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblLocation, gridBagConstraintsA);

        JLabel txtLocation = new JLabel(String.format("<html><a href='#'>%s</a></html>",
                contract.getSystemName(Utilities.getDateTimeDay(campaign.getCalendar()))));
        txtLocation.setName("txtLocation");
        txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtLocation.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Display where it is on the interstellar map
                campaign.getApp().getCampaigngui().getMapTab().switchSystemsMap(contract.getSystem());
                campaign.getApp().getCampaigngui().setSelectedTab(GuiTabType.MAP);
            }
        });
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtLocation, gridBagConstraintsB);

        if (Systems.getInstance().getSystems().get(contract.getSystemId()) != null) {
            JLabel lblDistance = new JLabel(resourceMap.getString("lblDistance.text"));
            lblDistance.setName("lblDistance");
            gridBagConstraintsA.gridy = ++y;
            mainPanel.add(lblDistance, gridBagConstraintsA);

            JumpPath path = campaign.calculateJumpPath(campaign.getCurrentSystem(), contract.getSystem());
            int days = (int) Math.ceil(path.getTotalTime(Utilities.getDateTimeDay(contract.getStartDate()),
                    campaign.getLocation().getTransitTime()));
            int jumps = path.getJumps();
            if (campaign.getCurrentSystem().getId().equals(contract.getSystemId())
                    && campaign.getLocation().isOnPlanet()) {
                days = 0;
                jumps = 0;
            }
            JLabel txtDistance = new JLabel(days + "(" + jumps + ")");
            txtDistance.setName("txtDistance");
            gridBagConstraintsB.gridy = y;
            mainPanel.add(txtDistance, gridBagConstraintsB);
        }

        JLabel lblAllyRating = new JLabel(resourceMap.getString("lblAllyRating.text"));
        lblAllyRating.setName("lblAllyRating");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblAllyRating, gridBagConstraintsA);

        if (contract instanceof AtBContract) {
            JLabel txtAllyRating = new JLabel(skillNames[((AtBContract) contract).getAllySkill()]
                    + "/" + ratingNames[((AtBContract) contract).getAllyQuality()]);
            txtAllyRating.setName("txtAllyRating");
            gridBagConstraintsB.gridy = y;
            mainPanel.add(txtAllyRating, gridBagConstraintsB);

            JLabel lblEnemyRating = new JLabel(resourceMap.getString("lblEnemyRating.text"));
            lblEnemyRating.setName("lblEnemyRating");
            gridBagConstraintsA.gridy = ++y;
            mainPanel.add(lblEnemyRating, gridBagConstraintsA);

            JLabel txtEnemyRating = new JLabel(skillNames[((AtBContract) contract).getEnemySkill()]
                    + "/" + ratingNames[((AtBContract) contract).getEnemyQuality()]);
            txtEnemyRating.setName("txtEnemyRating");
            gridBagConstraintsB.gridy = y;
            mainPanel.add(txtEnemyRating, gridBagConstraintsB);
        }

        JLabel lblStartDate = new JLabel(resourceMap.getString("lblStartDate.text"));
        lblStartDate.setName("lblStartDate");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblStartDate, gridBagConstraintsA);

        JLabel txtStartDate = new JLabel(shortDateFormat.format(contract.getStartDate()));
        txtStartDate.setName("txtStartDate");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtStartDate, gridBagConstraintsB);

        JLabel lblLength = new JLabel(resourceMap.getString("lblLength.text"));
        lblLength.setName("lblLength");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblLength, gridBagConstraintsA);

        JLabel txtLength = new JLabel(Integer.toString(contract.getLength()));
        txtLength.setName("txtLength");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtLength, gridBagConstraintsB);

        JLabel lblOverhead = new JLabel(resourceMap.getString("lblOverhead.text"));
        lblOverhead.setName("lblOverhead");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblOverhead, gridBagConstraintsA);

        JLabel txtOverhead = new JLabel(Contract.getOverheadCompName(contract.getOverheadComp()));
        txtOverhead.setName("txtOverhead");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtOverhead, gridBagConstraintsB);

        JLabel lblCommand = new JLabel(resourceMap.getString("lblCommand.text"));
        lblCommand.setName("lblCommand");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblCommand, gridBagConstraintsA);

        txtCommand = new JLabel(Contract.getCommandRightsName(contract.getCommandRights()));
        txtCommand.setName("txtCommand");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtCommand, gridBagConstraintsB);

        // Only allow command clause rerolls for mercenaries and pirates; house units are always integrated
        if (hasCommandRerolls()) {
            JButton btnCommand = new JButton();
            setCommandRerollButtonText(btnCommand);

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

            gridBagConstraintsC.gridy = y;
            mainPanel.add(btnCommand, gridBagConstraintsC);
        }

        JLabel lblTransport = new JLabel(resourceMap.getString("lblTransport.text"));
        lblTransport.setName("lblTransport");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblTransport, gridBagConstraintsA);

        txtTransport = new JLabel(contract.getTransportComp() + "%");
        txtTransport.setName("txtTransport");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtTransport, gridBagConstraintsB);

        if (hasTransportRerolls()) {
            JButton btnTransport = new JButton();
            setTransportRerollButtonText(btnTransport);

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

            gridBagConstraintsC.gridy = y;
            mainPanel.add(btnTransport, gridBagConstraintsC);
        }

        JLabel lblSalvageRights = new JLabel(resourceMap.getString("lblSalvageRights.text"));
        lblSalvageRights.setName("lblSalvageRights");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblSalvageRights, gridBagConstraintsA);

        JLabel txtSalvageRights = new JLabel(contract.getSalvagePct() + "%"
                + (contract.isSalvageExchange() ? " (Exchange)" : ""));
        txtSalvageRights.setName("txtSalvageRights");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtSalvageRights, gridBagConstraintsB);

        JLabel lblStraightSupport = new JLabel(resourceMap.getString("lblStraightSupport.text"));
        lblStraightSupport.setName("lblStraightSupport");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblStraightSupport, gridBagConstraintsA);

        txtStraightSupport = new JLabel(contract.getStraightSupport() + "%");
        txtStraightSupport.setName("txtStraightSupport");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtStraightSupport, gridBagConstraintsB);

        if (hasSupportRerolls()) {
            JButton btnSupport = new JButton();
            setSupportRerollButtonText(btnSupport);

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
                    txtBattleLossComp.setText(contract.getBattleLossComp() + "%");
                    if (campaign.getContractMarket().getRerollsUsed(contract,
                            ContractMarket.CLAUSE_SUPPORT) >= logRerolls) {
                        btn.setEnabled(false);
                    }
                    refreshAmounts();
                }
            });

            gridBagConstraintsC.gridy = y;
            mainPanel.add(btnSupport, gridBagConstraintsC);
        }

        JLabel lblBattleLossComp = new JLabel(resourceMap.getString("lblBattleLossComp.text"));
        lblBattleLossComp.setName("lblBattleLossComp");
        gridBagConstraintsA.gridy = ++y;
        mainPanel.add(lblBattleLossComp, gridBagConstraintsA);

        txtBattleLossComp = new JLabel(contract.getBattleLossComp() + "%");
        txtBattleLossComp.setName("txtBattleLossComp");
        gridBagConstraintsB.gridy = y;
        mainPanel.add(txtBattleLossComp, gridBagConstraintsB);

        if (contract instanceof AtBContract) {
            JLabel lblRequiredLances = new JLabel(resourceMap.getString("lblRequiredLances.text"));
            lblRequiredLances.setName("lblRequiredLances");
            gridBagConstraintsA.gridy = ++y;
            mainPanel.add(lblRequiredLances, gridBagConstraintsA);

            JLabel txtRequiredLances = new JLabel(((AtBContract) contract).getRequiredLances()
                    + " Lance(s)");
            txtRequiredLances.setName("txtRequiredLances");
            gridBagConstraintsB.gridy = y;
            mainPanel.add(txtRequiredLances, gridBagConstraintsB);
        }

        contractPaymentBreakdown.display(++y);
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
