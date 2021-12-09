/*
 * ContractSummaryPanel.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.market.ContractMarket;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Systems;
import mekhq.gui.GuiTabType;

/**
 * Contract summary view for ContractMarketDialog
 *
 * @author Neoancient
 */
public class ContractSummaryPanel extends JPanel {
    //region Variable Declarations
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

    private ResourceBundle resourceMap = ResourceBundle.getBundle(
            "mekhq.resources.ContractMarketDialog", new EncodeControl());
    private ContractPaymentBreakdown contractPaymentBreakdown;

    // These three are used locally to ensure consistent formatting
    private static final int LABEL_COLUMN = 0;
    private static final int TEXT_COLUMN = 1;
    private static final int BUTTON_COLUMN = 2;
    //endregion Variable Declarations


    public ContractSummaryPanel(Contract contract, Campaign campaign, boolean allowRerolls) {
        this.contract = contract;
        this.campaign = campaign;
        this.allowRerolls = allowRerolls;
        if (allowRerolls) {
            Person admin = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_COMMAND, SkillType.S_NEG, SkillType.S_ADMIN);
            cmdRerolls = (admin == null || admin.getSkill(SkillType.S_NEG) == null)
                    ? 0 : admin.getSkill(SkillType.S_NEG).getLevel();
            admin = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_LOGISTICS, SkillType.S_NEG, SkillType.S_ADMIN);
            logRerolls = (admin == null || admin.getSkill(SkillType.S_NEG) == null)
                    ? 0 : admin.getSkill(SkillType.S_NEG).getLevel();
            admin = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_TRANSPORT, SkillType.S_NEG, SkillType.S_ADMIN);
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
        // TODO : Switch me to use IUnitRating
        String[] ratingNames = {"F", "D", "C", "B", "A"};

        // Initializing the GridBagConstraint used for Labels
        // To use this you MUST AND ONLY overwrite gridy
        GridBagConstraints gridBagConstraintsLabels = new GridBagConstraints();
        gridBagConstraintsLabels.gridx = LABEL_COLUMN;
        gridBagConstraintsLabels.fill = GridBagConstraints.NONE;
        gridBagConstraintsLabels.anchor = GridBagConstraints.LINE_START;

        // Initializing the GridBagConstraint used for the Panels
        // To use this you MUST AND ONLY overwrite gridy
        GridBagConstraints gridBagConstraintsText = new GridBagConstraints();
        gridBagConstraintsText.gridx = TEXT_COLUMN;
        gridBagConstraintsText.weightx = 0.5;
        gridBagConstraintsText.gridwidth = 2; // this is used to properly separate the buttons, if they show up
        gridBagConstraintsText.insets = new Insets(0, 10, 0, 0);
        gridBagConstraintsText.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraintsText.anchor = GridBagConstraints.LINE_START;

        // Initializing the GridBagConstraint used for the Buttons
        // To use this you MUST AND ONLY overwrite gridx and/or gridy
        GridBagConstraints gridBagConstraintsButtons = new GridBagConstraints();
        gridBagConstraintsButtons.weightx = 0.5;
        gridBagConstraintsButtons.insets = new Insets(0, 10, 0, 0);
        gridBagConstraintsButtons.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraintsButtons.anchor = GridBagConstraints.LINE_START;

        int y = 0;
        //endregion Variable Declarations

        JLabel lblName = new JLabel(resourceMap.getString("lblName.text"));
        lblName.setName("lblName");
        gridBagConstraintsLabels.gridy = y;
        mainPanel.add(lblName, gridBagConstraintsLabels);

        txtName = new JTextField(contract.getName());
        txtName.setName("txtName");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtName, gridBagConstraintsText);

        JLabel lblEmployer = new JLabel(resourceMap.getString("lblEmployer.text"));
        lblEmployer.setName("lblEmployer");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblEmployer, gridBagConstraintsLabels);

        JLabel txtEmployer = new JLabel(contract.getEmployer());
        txtEmployer.setName("txtEmployer");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtEmployer, gridBagConstraintsText);

        if (contract instanceof AtBContract) {
            JLabel lblEnemy = new JLabel(resourceMap.getString("lblEnemy.text"));
            lblEnemy.setName("lblEnemy");
            gridBagConstraintsLabels.gridy = ++y;
            mainPanel.add(lblEnemy, gridBagConstraintsLabels);

            JLabel txtEnemy = new JLabel(((AtBContract) contract).getEnemyName(campaign.getGameYear()));
            txtEnemy.setName("txtEnemy");
            gridBagConstraintsText.gridy = y;
            mainPanel.add(txtEnemy, gridBagConstraintsText);
        }

        JLabel lblMissionType = new JLabel(resourceMap.getString("lblMissionType.text"));
        lblMissionType.setName("lblMissionType");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblMissionType, gridBagConstraintsLabels);

        JLabel txtMissionType = new JLabel(contract.getType());
        txtMissionType.setName("txtMissionType");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtMissionType, gridBagConstraintsText);

        JLabel lblLocation = new JLabel(resourceMap.getString("lblLocation.text"));
        lblLocation.setName("lblLocation");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblLocation, gridBagConstraintsLabels);

        JLabel txtLocation = new JLabel(String.format("<html><a href='#'>%s</a></html>",
                contract.getSystemName(campaign.getLocalDate())));
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
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtLocation, gridBagConstraintsText);

        if (Systems.getInstance().getSystems().get(contract.getSystemId()) != null) {
            JLabel lblDistance = new JLabel(resourceMap.getString("lblDistance.text"));
            lblDistance.setName("lblDistance");
            gridBagConstraintsLabels.gridy = ++y;
            mainPanel.add(lblDistance, gridBagConstraintsLabels);

            JumpPath path = campaign.calculateJumpPath(campaign.getCurrentSystem(), contract.getSystem());

            int days = (int) Math.ceil(path.getTotalTime(contract.getStartDate(),
                    campaign.getLocation().getTransitTime()));
            int jumps = path.getJumps();
            if (campaign.getCurrentSystem().getId().equals(contract.getSystemId())
                    && campaign.getLocation().isOnPlanet()) {
                days = 0;
                jumps = 0;
            }
            JLabel txtDistance = new JLabel(days + "(" + jumps + ")");
            txtDistance.setName("txtDistance");
            gridBagConstraintsText.gridy = y;
            mainPanel.add(txtDistance, gridBagConstraintsText);
        }

        JLabel lblAllyRating = new JLabel(resourceMap.getString("lblAllyRating.text"));
        lblAllyRating.setName("lblAllyRating");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblAllyRating, gridBagConstraintsLabels);

        if (contract instanceof AtBContract) {
            JLabel txtAllyRating = new JLabel(((AtBContract) contract).getAllySkill()
                    + "/" + ratingNames[((AtBContract) contract).getAllyQuality()]);
            txtAllyRating.setName("txtAllyRating");
            gridBagConstraintsText.gridy = y;
            mainPanel.add(txtAllyRating, gridBagConstraintsText);

            JLabel lblEnemyRating = new JLabel(resourceMap.getString("lblEnemyRating.text"));
            lblEnemyRating.setName("lblEnemyRating");
            gridBagConstraintsLabels.gridy = ++y;
            mainPanel.add(lblEnemyRating, gridBagConstraintsLabels);

            JLabel txtEnemyRating = new JLabel(((AtBContract) contract).getEnemySkill()
                    + "/" + ratingNames[((AtBContract) contract).getEnemyQuality()]);
            txtEnemyRating.setName("txtEnemyRating");
            gridBagConstraintsText.gridy = y;
            mainPanel.add(txtEnemyRating, gridBagConstraintsText);
        }

        JLabel lblStartDate = new JLabel(resourceMap.getString("lblStartDate.text"));
        lblStartDate.setName("lblStartDate");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblStartDate, gridBagConstraintsLabels);

        JLabel txtStartDate = new JLabel(MekHQ.getMekHQOptions().getDisplayFormattedDate(contract.getStartDate()));
        txtStartDate.setName("txtStartDate");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtStartDate, gridBagConstraintsText);

        JLabel lblLength = new JLabel(resourceMap.getString("lblLength.text"));
        lblLength.setName("lblLength");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblLength, gridBagConstraintsLabels);

        JLabel txtLength = new JLabel(Integer.toString(contract.getLength()));
        txtLength.setName("txtLength");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtLength, gridBagConstraintsText);

        JLabel lblOverhead = new JLabel(resourceMap.getString("lblOverhead.text"));
        lblOverhead.setName("lblOverhead");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblOverhead, gridBagConstraintsLabels);

        JLabel txtOverhead = new JLabel(Contract.getOverheadCompName(contract.getOverheadComp()));
        txtOverhead.setName("txtOverhead");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtOverhead, gridBagConstraintsText);

        JLabel lblCommand = new JLabel(resourceMap.getString("lblCommand.text"));
        lblCommand.setName("lblCommand");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblCommand, gridBagConstraintsLabels);

        txtCommand = new JLabel(contract.getCommandRights().toString());
        txtCommand.setToolTipText(contract.getCommandRights().getToolTipText());
        txtCommand.setName("txtCommand");

        // Then we determine if we just add it to the main panel, or if we combine it with a button
        // to reroll the value
        if (!hasCommandRerolls()) {
            // just add it to the main panel, using the normal gridBagConstraints for text
            gridBagConstraintsText.gridy = y;
            mainPanel.add(txtCommand, gridBagConstraintsText);
        } else {
            gridBagConstraintsButtons.gridy = y;
            gridBagConstraintsButtons.gridx = TEXT_COLUMN;
            mainPanel.add(txtCommand, gridBagConstraintsButtons);

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
                    txtCommand.setText(contract.getCommandRights().toString());
                    txtCommand.setToolTipText(contract.getCommandRights().getToolTipText());
                    if (campaign.getContractMarket().getRerollsUsed(contract,
                            ContractMarket.CLAUSE_COMMAND) >= cmdRerolls) {
                        btn.setEnabled(false);
                    }
                    refreshAmounts();
                }
            });

            gridBagConstraintsButtons.gridx = BUTTON_COLUMN;
            mainPanel.add(btnCommand, gridBagConstraintsButtons);
        }

        JLabel lblTransport = new JLabel(resourceMap.getString("lblTransport.text"));
        lblTransport.setName("lblTransport");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblTransport, gridBagConstraintsLabels);

        txtTransport = new JLabel(contract.getTransportComp() + "%");
        txtTransport.setName("txtTransport");

        // Then we determine if we just add it to the main panel, or if we combine it with a button
        // to reroll the value
        if (!hasTransportRerolls()) {
            // just add it to the main panel, using the normal gridBagConstraints for text
            gridBagConstraintsText.gridy = y;
            mainPanel.add(txtTransport, gridBagConstraintsText);
        } else {
            gridBagConstraintsButtons.gridy = y;
            gridBagConstraintsButtons.gridx = TEXT_COLUMN;
            mainPanel.add(txtTransport, gridBagConstraintsButtons);

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

            gridBagConstraintsButtons.gridx = BUTTON_COLUMN;
            mainPanel.add(btnTransport, gridBagConstraintsButtons);
        }

        JLabel lblSalvageRights = new JLabel(resourceMap.getString("lblSalvageRights.text"));
        lblSalvageRights.setName("lblSalvageRights");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblSalvageRights, gridBagConstraintsLabels);

        JLabel txtSalvageRights = new JLabel(contract.getSalvagePct() + "%"
                + (contract.isSalvageExchange() ? " (Exchange)" : ""));
        txtSalvageRights.setName("txtSalvageRights");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtSalvageRights, gridBagConstraintsText);

        JLabel lblStraightSupport = new JLabel(resourceMap.getString("lblStraightSupport.text"));
        lblStraightSupport.setName("lblStraightSupport");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblStraightSupport, gridBagConstraintsLabels);

        txtStraightSupport = new JLabel(contract.getStraightSupport() + "%");
        txtStraightSupport.setName("txtStraightSupport");

        // Then we determine if we just add it to the main panel, or if we combine it with a button
        // to reroll the value
        if (!hasSupportRerolls()) {
            // just add it to the main panel, can't use a reroll
            gridBagConstraintsText.gridy = y;
            mainPanel.add(txtStraightSupport, gridBagConstraintsText);
        } else {
            gridBagConstraintsButtons.gridy = y;
            gridBagConstraintsButtons.gridx = TEXT_COLUMN;
            mainPanel.add(txtStraightSupport, gridBagConstraintsButtons);

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

            gridBagConstraintsButtons.gridx = BUTTON_COLUMN;
            mainPanel.add(btnSupport, gridBagConstraintsButtons);
        }

        JLabel lblBattleLossComp = new JLabel(resourceMap.getString("lblBattleLossComp.text"));
        lblBattleLossComp.setName("lblBattleLossComp");
        gridBagConstraintsLabels.gridy = ++y;
        mainPanel.add(lblBattleLossComp, gridBagConstraintsLabels);

        txtBattleLossComp = new JLabel(contract.getBattleLossComp() + "%");
        txtBattleLossComp.setName("txtBattleLossComp");
        gridBagConstraintsText.gridy = y;
        mainPanel.add(txtBattleLossComp, gridBagConstraintsText);

        if (contract instanceof AtBContract) {
            JLabel lblRequiredLances = new JLabel(resourceMap.getString("lblRequiredLances.text"));
            lblRequiredLances.setName("lblRequiredLances");
            gridBagConstraintsLabels.gridy = ++y;
            mainPanel.add(lblRequiredLances, gridBagConstraintsLabels);

            JLabel txtRequiredLances = new JLabel(((AtBContract) contract).getRequiredLances()
                    + " Lance(s)");
            txtRequiredLances.setName("txtRequiredLances");
            gridBagConstraintsText.gridy = y;
            mainPanel.add(txtRequiredLances, gridBagConstraintsText);
        }

        contractPaymentBreakdown.display(++y, 2);
    }

    private boolean hasTransportRerolls() {
        return allowRerolls && (campaign.getContractMarket().getRerollsUsed(contract,
                ContractMarket.CLAUSE_TRANSPORT) < tranRerolls);
    }

    private boolean hasCommandRerolls() {
        // Only allow command clause rerolls for mercenaries and pirates; house units are always integrated
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
