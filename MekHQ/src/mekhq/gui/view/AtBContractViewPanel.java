/*
 * AtBContractViewPanel.java
 *
 * Copyright (C) 2014-2016 MegaMek team
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

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import megamek.common.util.EncodeControl;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;
import mekhq.gui.utilities.MarkdownRenderer;

/**
 * @author Neoancient
 *
 * A version of ContractViewPanel with additional details for
 * Against the Bot
 *
 */
public class AtBContractViewPanel extends ScrollablePanel {
    private static final long serialVersionUID = -9190665158803529105L;
    private Campaign campaign;
    private AtBContract contract;
    private CampaignGUI gui;

    private JPanel pnlStats;
    private JTextPane txtDesc;

    private JLabel lblStatus;
    private JLabel lblLocation;
    private JLabel txtLocation;
    private JLabel lblType;
    private JLabel txtType;
    private JLabel lblEmployer;
    private JLabel txtEmployer;
    private JLabel lblEnemy;
    private JLabel txtEnemy;
    private JLabel lblAllyRating;
    private JLabel txtAllyRating;
    private JLabel lblEnemyRating;
    private JLabel txtEnemyRating;
    private JLabel lblStartDate;
    private JLabel txtStartDate;
    private JLabel lblEndDate;
    private JLabel txtEndDate;
    private JLabel lblPayout;
    private JLabel txtPayout;
    private JLabel lblCommand;
    private JLabel txtCommand;
    private JLabel lblBLC;
    private JLabel txtBLC;
    private JLabel lblSalvageValueMerc;
    private JLabel txtSalvageValueMerc;
    private JLabel lblSalvageValueEmployer;
    private JLabel txtSalvageValueEmployer;
    private JLabel lblSalvagePct;
    private JLabel txtSalvagePct;
    private JLabel lblMorale;
    private JLabel txtMorale;
    private JLabel lblScore;
    private JLabel txtScore;
    private JLabel lblSharePct;
    private JLabel txtSharePct;

    public AtBContractViewPanel(AtBContract contract, Campaign campaign, CampaignGUI gui) {
        this.contract = contract;
        this.campaign = campaign;
        this.gui = gui;
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        pnlStats = new JPanel();
        txtDesc = new JTextPane();

        setLayout(new GridBagLayout());

        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder(contract.getName()));
        fillStats();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);
    }

    private void fillStats() {
        String[] skillNames = {"Green", "Regular", "Veteran", "Elite"};
        String[] ratingNames = {"F", "D", "C", "B", "A"};

        lblStatus = new JLabel();
        lblLocation = new JLabel();
        txtLocation = new JLabel();
        lblEmployer = new JLabel();
        txtEmployer = new JLabel();
        lblEnemy = new JLabel();
        txtEnemy = new JLabel();
        lblType = new JLabel();
        txtType = new JLabel();
        lblStartDate = new JLabel();
        txtStartDate = new JLabel();
        lblEndDate = new JLabel();
        txtEndDate = new JLabel();
        lblPayout = new JLabel();
        txtPayout = new JLabel();
        lblCommand = new JLabel();
        txtCommand = new JLabel();
        lblBLC = new JLabel();
        txtBLC = new JLabel();
        lblAllyRating = new JLabel();
        txtAllyRating = new JLabel();
        lblEnemyRating = new JLabel();
        txtEnemyRating = new JLabel();
        lblMorale = new JLabel();
        txtMorale = new JLabel();
        lblSharePct = new JLabel();
        txtSharePct = new JLabel();
        lblScore = new JLabel();
        txtScore = new JLabel();

        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractViewPanel", new EncodeControl()); //$NON-NLS-1$

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        SimpleDateFormat shortDateFormat = new SimpleDateFormat("MM/dd/yyyy");

        int y = 0;

        lblStatus.setName("lblOwner"); // NOI18N
        lblStatus.setText("<html><b>" + contract.getStatusName() + "</b></html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStatus, gridBagConstraints);


        lblLocation.setName("lblLocation"); // NOI18N
        lblLocation.setText(resourceMap.getString("lblLocation.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblLocation, gridBagConstraints);

        txtLocation.setName("txtLocation"); // NOI18N
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
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtLocation, gridBagConstraints);

        lblEmployer.setName("lblEmployer"); // NOI18N
        lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEmployer, gridBagConstraints);

        txtEmployer.setName("txtEmployer"); // NOI18N
        txtEmployer.setText(contract.getEmployerName(campaign.getGameYear()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEmployer, gridBagConstraints);

        lblEnemy.setName("lblEnemy"); // NOI18N
        lblEnemy.setText(resourceMap.getString("lblEnemy.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEnemy, gridBagConstraints);

        txtEnemy.setName("txtEnemy"); // NOI18N
        txtEnemy.setText(contract.getEnemyName(campaign.getGameYear()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEnemy, gridBagConstraints);

        lblType.setName("lblType"); // NOI18N
        lblType.setText(resourceMap.getString("lblType.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblType, gridBagConstraints);

        txtType.setName("txtType"); // NOI18N
        txtType.setText(contract.getType());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtType, gridBagConstraints);

        lblAllyRating.setName("lblAllyRating"); // NOI18N
        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblAllyRating, gridBagConstraints);

        txtAllyRating.setName("txtAllyRating"); // NOI18N
        txtAllyRating.setText(skillNames[contract.getAllySkill()] + "/" +
                ratingNames[contract.getAllyQuality()]);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtAllyRating, gridBagConstraints);

        lblEnemyRating.setName("lblEnemyRating"); // NOI18N
        lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEnemyRating, gridBagConstraints);

        txtEnemyRating.setName("txtEnemyRating"); // NOI18N
        txtEnemyRating.setText(skillNames[contract.getEnemySkill()] + "/" +
                ratingNames[contract.getEnemyQuality()]);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEnemyRating, gridBagConstraints);

        lblStartDate.setName("lblStartDate"); // NOI18N
        lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStartDate, gridBagConstraints);

        txtStartDate.setName("txtStartDate"); // NOI18N
        txtStartDate.setText(shortDateFormat.format(contract.getStartDate()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtStartDate, gridBagConstraints);

        lblEndDate.setName("lblEndDate"); // NOI18N
        lblEndDate.setText(resourceMap.getString("lblEndDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEndDate, gridBagConstraints);

        txtEndDate.setName("txtEndDate"); // NOI18N
        txtEndDate.setText(shortDateFormat.format(contract.getEndingDate()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEndDate, gridBagConstraints);

        lblPayout.setName("lblPayout"); // NOI18N
        lblPayout.setText(resourceMap.getString("lblPayout.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblPayout, gridBagConstraints);

        txtPayout.setName("txtPayout"); // NOI18N
        txtPayout.setText(contract.getMonthlyPayOut().toAmountAndSymbolString());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtPayout, gridBagConstraints);

        lblCommand.setName("lblCommand"); // NOI18N
        lblCommand.setText(resourceMap.getString("lblCommand.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCommand, gridBagConstraints);

        txtCommand.setName("txtCommand"); // NOI18N
        txtCommand.setText(Contract.getCommandRightsName(contract.getCommandRights()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtCommand, gridBagConstraints);

        lblBLC.setName("lblBLC"); // NOI18N
        lblBLC.setText(resourceMap.getString("lblBLC.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBLC, gridBagConstraints);

        txtBLC.setName("txtBLC"); // NOI18N
        txtBLC.setText(contract.getBattleLossComp() + "%");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtBLC, gridBagConstraints);

        if(contract.getSalvagePct() > 0 && !contract.isSalvageExchange()) {
            lblSalvageValueMerc = new JLabel(resourceMap.getString("lblSalvageValueMerc.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSalvageValueMerc, gridBagConstraints);
            txtSalvageValueMerc = new JLabel();
            txtSalvageValueMerc.setText(contract.getSalvagedByUnit().toAmountAndSymbolString());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSalvageValueMerc, gridBagConstraints);

            lblSalvageValueEmployer = new JLabel(resourceMap.getString("lblSalvageValueEmployer.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSalvageValueEmployer, gridBagConstraints);
            txtSalvageValueEmployer = new JLabel();
            txtSalvageValueEmployer.setText(contract.getSalvagedByEmployer().toAmountAndSymbolString());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSalvageValueEmployer, gridBagConstraints);
        }
        lblSalvagePct = new JLabel(resourceMap.getString("lblSalvage.text"));
        txtSalvagePct = new JLabel();
        txtSalvagePct.setName("txtSalvagePct"); // NOI18N

        if(contract.isSalvageExchange()) {
            txtSalvagePct.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePct() + "%)");
        } else if(contract.getSalvagePct() == 0) {
            txtSalvagePct.setText(resourceMap.getString("none"));
        } else {
            lblSalvagePct.setText(resourceMap.getString("lblSalvagePct.text"));
            int maxSalvagePct = contract.getSalvagePct();

            int currentSalvagePct = 0;
            if (contract.getSalvagedByUnit().plus(contract.getSalvagedByEmployer()).isPositive()) {
                currentSalvagePct = contract.getSalvagedByUnit()
                        .multipliedBy(100)
                        .dividedBy(contract.getSalvagedByUnit().plus(contract.getSalvagedByEmployer()))
                        .getAmount()
                        .intValue();
            }

            txtSalvagePct.setText(currentSalvagePct + "% (max " + maxSalvagePct + "%)");
        }

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtSalvagePct, gridBagConstraints);

        lblMorale.setName("lblMorale"); // NOI18N
        lblMorale.setText(resourceMap.getString("lblMorale.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblMorale, gridBagConstraints);

        txtMorale.setName("txtMorale"); // NOI18N
        txtMorale.setText(contract.getMoraleLevelName());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtMorale, gridBagConstraints);

        if (campaign.getCampaignOptions().getUseShareSystem()) {
            lblSharePct.setName("lblSharePct"); // NOI18N
            lblSharePct.setText(resourceMap.getString("lblSharePct.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSharePct, gridBagConstraints);

            txtSharePct.setName("txtSharePct"); // NOI18N
            txtSharePct.setText(contract.getSharesPct() + "%");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSharePct, gridBagConstraints);
        }

        lblScore.setName("lblScore"); // NOI18N
        lblScore.setText(resourceMap.getString("lblScore.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblScore, gridBagConstraints);

        txtScore.setName("txtScore"); // NOI18N
        txtScore.setText(Integer.toString(contract.getScore()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtScore, gridBagConstraints);

        txtDesc.setName("txtDesc");
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        txtDesc.setText(MarkdownRenderer.getRenderedHtml(contract.getDescription()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtDesc, gridBagConstraints);
    }
}
