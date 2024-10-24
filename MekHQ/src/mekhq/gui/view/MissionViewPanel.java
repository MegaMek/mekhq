/*
 * Copyright (c) 2009-2024 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissionViewPanel extends JScrollablePanel {
    private Mission mission;
    protected CampaignGUI gui;

    protected JPanel pnlStats;
    protected JTextPane txtDesc;

    /* Basic Mission Parameters */
    private JLabel lblStatus;
    private JPanel lblChallenge;
    private JLabel lblLocation;
    private JLabel txtLocation;
    private JLabel lblType;
    private JLabel txtType;

    /* Contract Parameters */
    private JLabel lblEmployer;
    private JLabel txtEmployer;
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
    private JLabel lblSalvagePct1;
    private JLabel lblSalvagePct2;

    /* AtB Contract Parameters */
    private JLabel lblEnemy;
    private JLabel txtEnemy;
    private JLabel lblAllyRating;
    private JLabel txtAllyRating;
    private JLabel lblEnemyRating;
    private JLabel txtEnemyRating;
    private JLabel lblSalvagePct;
    private JLabel txtSalvagePct;
    private JLabel lblMorale;
    private JLabel txtMorale;
    private JLabel lblScore;
    private JLabel txtScore;
    private JLabel lblSharePct;
    private JLabel txtSharePct;

    protected JTable scenarioTable;

    private final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ContractViewPanel",
            MekHQ.getMHQOptions().getLocale());

    public MissionViewPanel(Mission m, JTable scenarioTable, CampaignGUI gui) {
        super();
        this.mission = m;
        this.scenarioTable = scenarioTable;
        this.gui = gui;
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        pnlStats = new JPanel();
        txtDesc = new JTextPane();

        setLayout(new GridBagLayout());

        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder(mission.getName()));
        fillStats();

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        JScrollPane scrollScenarioTable = new JScrollPaneWithSpeed(scenarioTable);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(10, 10, 10, 10);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(scrollScenarioTable, gridBagConstraints);
    }

    private void fillStats() {
        if (mission instanceof AtBContract) {
            fillStatsAtBContract();
        } else if (mission instanceof Contract) {
            fillStatsContract();
        } else {
            fillStatsBasic();
        }
    }

    private void fillStatsBasic() {
        lblStatus = new JLabel();
        lblLocation = new JLabel();
        txtLocation = new JLabel();
        lblType = new JLabel();
        txtType = new JLabel();

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        lblStatus.setName("lblOwner");
        lblStatus.setText("<html><b>" + mission.getStatus() + "</b></html>");
        lblStatus.setToolTipText(mission.getStatus().getToolTipText());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStatus, gridBagConstraints);

        if ((null != mission.getSystemName(null)) && !mission.getSystemName(null).isEmpty()) {
            lblLocation.setName("lblLocation");
            lblLocation.setText(resourceMap.getString("lblLocation.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblLocation, gridBagConstraints);

            txtLocation.setName("txtLocation");
            String systemName = mission.getSystemName(null);
            txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
            txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            txtLocation.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Display where it is on the interstellar map
                    gui.getMapTab().switchSystemsMap(mission.getSystem());
                    gui.setSelectedTab(MHQTabType.INTERSTELLAR_MAP);
                }
            });
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtLocation, gridBagConstraints);
        }

        if ((null != mission.getType()) && !mission.getType().isEmpty()) {
            lblType.setName("lblType");
            lblType.setText(resourceMap.getString("lblType.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblType, gridBagConstraints);

            txtType.setName("txtType");
            txtType.setText(mission.getType());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtType, gridBagConstraints);
        }

        txtDesc.setName("txtDesc");
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        txtDesc.setText(MarkdownRenderer.getRenderedHtml(mission.getDescription()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtDesc, gridBagConstraints);
    }

    private void fillStatsContract() {
        Contract contract = (Contract) mission;

        lblStatus = new JLabel();
        lblLocation = new JLabel();
        txtLocation = new JLabel();
        lblEmployer = new JLabel();
        txtEmployer = new JLabel();
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

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        lblStatus.setName("lblOwner");
        lblStatus.setText("<html><b>" + contract.getStatus() + "</b></html>");
        lblStatus.setToolTipText(contract.getStatus().getToolTipText());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStatus, gridBagConstraints);

        if ((null != contract.getSystemName(null)) && !contract.getSystemName(null).isEmpty()) {
            lblLocation.setName("lblLocation");
            lblLocation.setText(resourceMap.getString("lblLocation.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblLocation, gridBagConstraints);

            txtLocation.setName("txtLocation");
            String systemName = contract.getSystemName(null);
            txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
            txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            txtLocation.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Display where it is on the interstellar map
                    gui.getMapTab().switchSystemsMap(contract.getSystem());
                    gui.setSelectedTab(MHQTabType.INTERSTELLAR_MAP);
                }
            });
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtLocation, gridBagConstraints);
        }

        if ((null != contract.getEmployer()) && !contract.getEmployer().isEmpty()) {
            lblEmployer.setName("lblEmployer");
            lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblEmployer, gridBagConstraints);

            txtEmployer.setName("txtEmployer");
            txtEmployer.setText(contract.getEmployer());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtEmployer, gridBagConstraints);
        }

        if ((null != contract.getType()) && !contract.getType().isEmpty()) {
            lblType.setName("lblType");
            lblType.setText(resourceMap.getString("lblType.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblType, gridBagConstraints);

            txtType.setName("txtType");
            txtType.setText(contract.getType());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtType, gridBagConstraints);
        }

        lblStartDate.setName("lblStartDate");
        lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStartDate, gridBagConstraints);

        txtStartDate.setName("txtStartDate");
        txtStartDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(contract.getStartDate()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtStartDate, gridBagConstraints);

        lblEndDate.setName("lblEndDate");
        lblEndDate.setText(resourceMap.getString("lblEndDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEndDate, gridBagConstraints);

        txtEndDate.setName("txtEndDate");
        txtEndDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(contract.getEndingDate()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEndDate, gridBagConstraints);

        lblPayout.setName("lblPayout");
        lblPayout.setText(resourceMap.getString("lblPayout.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblPayout, gridBagConstraints);

        txtPayout.setName("txtPayout");
        txtPayout.setText(contract.getMonthlyPayOut().toAmountAndSymbolString());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtPayout, gridBagConstraints);

        lblCommand.setName("lblCommand");
        lblCommand.setText(resourceMap.getString("lblCommand.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCommand, gridBagConstraints);

        txtCommand.setName("txtCommand");
        txtCommand.setText(contract.getCommandRights().toString());
        txtCommand.setToolTipText(contract.getCommandRights().getToolTipText());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtCommand, gridBagConstraints);

        lblBLC.setName("lblBLC");
        lblBLC.setText(resourceMap.getString("lblBLC.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBLC, gridBagConstraints);

        txtBLC.setName("txtBLC");
        txtBLC.setText(contract.getBattleLossComp() + "%");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtBLC, gridBagConstraints);

        int i = 9;
        if ((contract.getSalvagePct() > 0) && !contract.isSalvageExchange()) {
            lblSalvageValueMerc = new JLabel(resourceMap.getString("lblSalvageValueMerc.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSalvageValueMerc, gridBagConstraints);
            txtSalvageValueMerc = new JLabel();
            txtSalvageValueMerc.setText(contract.getSalvagedByUnit().toAmountAndSymbolString());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSalvageValueMerc, gridBagConstraints);
            i++;
            lblSalvageValueEmployer = new JLabel(resourceMap.getString("lblSalvageValueEmployer.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSalvageValueEmployer, gridBagConstraints);
            txtSalvageValueEmployer = new JLabel();
            txtSalvageValueEmployer.setText(contract.getSalvagedByEmployer().toAmountAndSymbolString());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = i;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSalvageValueEmployer, gridBagConstraints);
            i++;
        }
        lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvage.text"));
        lblSalvagePct2 = new JLabel();

        if (contract.isSalvageExchange()) {
            lblSalvagePct2.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePct() + "%)");
        } else if (contract.getSalvagePct() == 0) {
            lblSalvagePct2.setText(resourceMap.getString("none"));
        } else {
            lblSalvagePct1.setText(resourceMap.getString("lblSalvagePct.text"));
            int maxSalvagePct = contract.getSalvagePct();

            int currentSalvagePct = 0;
            if (contract.getSalvagedByUnit().plus(contract.getSalvagedByUnit()).isPositive()) {
                currentSalvagePct = contract.getSalvagedByUnit()
                        .multipliedBy(100)
                        .dividedBy(contract.getSalvagedByUnit().plus(contract.getSalvagedByEmployer()))
                        .getAmount()
                        .intValue();
            }

            String lead = "<html><font>";
            if (currentSalvagePct > maxSalvagePct) {
                lead = "<html><font color='" + MekHQ.getMHQOptions().getFontColorNegativeHexColor() + "'>";
            }
            lblSalvagePct2.setText(lead + currentSalvagePct + "%</font> <span>(max " + maxSalvagePct + "%)</span></html>");
        }

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = i;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct1, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = i;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblSalvagePct2, gridBagConstraints);
        i++;
        txtDesc.setName("txtDesc");
        txtDesc.setEditable(false);
        txtDesc.setContentType("text/html");
        txtDesc.setText(MarkdownRenderer.getRenderedHtml(contract.getDescription()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = i;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtDesc, gridBagConstraints);

    }

    private void fillStatsAtBContract() {
        AtBContract contract = (AtBContract) mission;
        Campaign campaign = gui.getCampaign();

        // TODO : Switch me to use IUnitRating
        String[] ratingNames = {"F", "D", "C", "B", "A"};
        lblStatus = new JLabel();
        lblChallenge = new JPanel();
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

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        int y = 0;

        lblStatus.setName("lblOwner");
        lblStatus.setText("<html><b>" + contract.getStatus() + "</b></html>");
        lblStatus.setToolTipText(contract.getStatus().getToolTipText());
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

        lblChallenge = contract.getContractDifficultySkulls(campaign);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblChallenge, gridBagConstraints);

        lblLocation.setName("lblLocation");
        lblLocation.setText(resourceMap.getString("lblLocation.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblLocation, gridBagConstraints);

        txtLocation.setName("txtLocation");
        String systemName = contract.getSystemName(campaign.getLocalDate());
        txtLocation.setText(String.format("<html><a href='#'>%s</a></html>", systemName));
        txtLocation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        txtLocation.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Display where it is on the interstellar map
                gui.getMapTab().switchSystemsMap(contract.getSystem());
                gui.setSelectedTab(MHQTabType.INTERSTELLAR_MAP);
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

        lblEmployer.setName("lblEmployer");
        lblEmployer.setText(resourceMap.getString("lblEmployer.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEmployer, gridBagConstraints);

        txtEmployer.setName("txtEmployer");
        txtEmployer.setText(contract.getEmployerName(campaign.getGameYear()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEmployer, gridBagConstraints);

        lblEnemy.setName("lblEnemy");
        lblEnemy.setText(resourceMap.getString("lblEnemy.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEnemy, gridBagConstraints);

        txtEnemy.setName("txtEnemy");
        txtEnemy.setText(contract.getEnemyBotName());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEnemy, gridBagConstraints);

        lblType.setName("lblType");
        lblType.setText(resourceMap.getString("lblType.text"));
        lblType.setToolTipText(contract.getContractType().getToolTipText());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblType, gridBagConstraints);

        txtType.setName("txtType");
        txtType.setText(contract.getType());
        txtType.setToolTipText(contract.getContractType().getToolTipText());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtType, gridBagConstraints);

        lblAllyRating.setName("lblAllyRating");
        lblAllyRating.setText(resourceMap.getString("lblAllyRating.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblAllyRating, gridBagConstraints);

        txtAllyRating.setName("txtAllyRating");
        txtAllyRating.setText(contract.getAllySkill() + "/" + ratingNames[contract.getAllyQuality()]);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtAllyRating, gridBagConstraints);

        lblEnemyRating.setName("lblEnemyRating");
        lblEnemyRating.setText(resourceMap.getString("lblEnemyRating.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEnemyRating, gridBagConstraints);

        txtEnemyRating.setName("txtEnemyRating");
        txtEnemyRating.setText(contract.getEnemySkill() + "/" + ratingNames[contract.getEnemyQuality()]);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEnemyRating, gridBagConstraints);

        lblStartDate.setName("lblStartDate");
        lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStartDate, gridBagConstraints);

        txtStartDate.setName("txtStartDate");
        txtStartDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(contract.getStartDate()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtStartDate, gridBagConstraints);

        lblEndDate.setName("lblEndDate");
        lblEndDate.setText(resourceMap.getString("lblEndDate.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblEndDate, gridBagConstraints);

        txtEndDate.setName("txtEndDate");
        txtEndDate.setText(MekHQ.getMHQOptions().getDisplayFormattedDate(contract.getEndingDate()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtEndDate, gridBagConstraints);

        lblPayout.setName("lblPayout");
        lblPayout.setText(resourceMap.getString("lblPayout.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblPayout, gridBagConstraints);

        txtPayout.setName("txtPayout");
        txtPayout.setText(contract.getMonthlyPayOut().toAmountAndSymbolString());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtPayout, gridBagConstraints);

        lblCommand.setName("lblCommand");
        lblCommand.setText(resourceMap.getString("lblCommand.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblCommand, gridBagConstraints);

        txtCommand.setName("txtCommand");
        txtCommand.setText(contract.getCommandRights().toString());
        txtCommand.setToolTipText(contract.getCommandRights().getToolTipText());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtCommand, gridBagConstraints);

        lblBLC.setName("lblBLC");
        lblBLC.setText(resourceMap.getString("lblBLC.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblBLC, gridBagConstraints);

        txtBLC.setName("txtBLC");
        txtBLC.setText(contract.getBattleLossComp() + "%");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtBLC, gridBagConstraints);

        if ((contract.getSalvagePct() > 0) && !contract.isSalvageExchange()) {
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
        txtSalvagePct.setName("txtSalvagePct");

        if (contract.isSalvageExchange()) {
            txtSalvagePct.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePct() + "%)");
        } else if (contract.getSalvagePct() == 0) {
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

        lblMorale.setName("lblMorale");
        lblMorale.setText(resourceMap.getString("lblMorale.text"));
        lblMorale.setToolTipText(wordWrap(contract.getMoraleLevel().getToolTipText()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblMorale, gridBagConstraints);

        txtMorale.setName("txtMorale");
        txtMorale.setText(contract.getMoraleLevel().toString());
        txtMorale.setToolTipText(wordWrap(contract.getMoraleLevel().getToolTipText()));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtMorale, gridBagConstraints);

        if (campaign.getCampaignOptions().isUseShareSystem()) {
            lblSharePct.setName("lblSharePct");
            lblSharePct.setText(resourceMap.getString("lblSharePct.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSharePct, gridBagConstraints);

            txtSharePct.setName("txtSharePct");
            txtSharePct.setText(contract.getSharesPercent() + "%");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSharePct, gridBagConstraints);
        }

        // for StratCon, contract score is irrelevant and only leads to confusion, so we
        // do not display it in that situation
        boolean showContractScore = !gui.getCampaign().getCampaignOptions().isUseStratCon()
                && (mission instanceof AtBContract)
                && (((AtBContract) mission).getStratconCampaignState() == null);

        if (showContractScore) {
            lblScore.setName("lblScore");
            lblScore.setText(resourceMap.getString("lblScore.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblScore, gridBagConstraints);

            txtScore.setName("txtScore");
            txtScore.setText(Integer.toString(contract.getScore()));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtScore, gridBagConstraints);
        }

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
