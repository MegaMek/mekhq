/*
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.view;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.estimateCargoRequirements;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.MarkdownRenderer;
import mekhq.utilities.ReportingUtilities;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissionViewPanel extends JScrollablePanel {
    private final Mission mission;
    protected CampaignGUI gui;

    protected JPanel pnlStats;
    protected JPanel pnlTutorial;
    protected JTextPane txtDesc;

    /* Basic Mission Parameters */
    private JLabel lblStatus;
    private JPanel lblBelligerents;
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
        pnlTutorial = new JPanel();
        txtDesc = new JTextPane();

        setLayout(new GridBagLayout());

        pnlStats.setMaximumSize(UIUtil.scaleForGUI(200, Integer.MAX_VALUE));
        pnlStats.setName("pnlStats");
        pnlStats.setBorder(RoundedLineBorder.createRoundedLineBorder(mission.getName()));
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

        if (mission instanceof AtBContract) {
            pnlStats.setName("pnlTutorial");
            pnlStats.setBorder(RoundedLineBorder.createRoundedLineBorder(mission.getName()));
            fillTutorial();

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.weightx = 2.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            add(pnlTutorial, gridBagConstraints);
        }

        JScrollPane scrollScenarioTable = new JScrollPaneWithSpeed(scenarioTable);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(10, 10, 10, 10);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
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
        lblBelligerents = new JPanel();
        lblLocation = new JLabel();
        txtLocation = new JLabel();
        lblType = new JLabel();
        txtType = new JLabel();

        pnlStats.setLayout(new GridBagLayout());

        lblStatus.setName("lblOwner");
        lblStatus.setText("<html><b>" + mission.getStatus() + "</b></html>");
        lblStatus.setToolTipText(mission.getStatus().getToolTipText());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
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
        txtCommand.setToolTipText(wordWrap(contract.getCommandRights().getToolTipText()));
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

        JLabel lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvage.text"));
        JLabel lblSalvagePct2 = new JLabel();

        if (contract.isSalvageExchange()) {
            lblSalvagePct2.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePct() + "%)");
        } else if (contract.getSalvagePct() == 0) {
            lblSalvagePct2.setText(resourceMap.getString("none"));
        } else {
            lblSalvagePct1.setText(resourceMap.getString("lblSalvagePct.text"));
            int maxSalvagePct = contract.getSalvagePct();

            int currentSalvagePct = getCurrentSalvagePct(contract, contract.getSalvagedByUnit());

            String lead = "<html><font>";
            if (currentSalvagePct > maxSalvagePct) {
                lead = "<html><font color='" + ReportingUtilities.getNegativeColor() + "'>";
            }
            lblSalvagePct2.setText(lead +
                                         currentSalvagePct +
                                         "%</font> <span>(max " +
                                         maxSalvagePct +
                                         "%)</span></html>");
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

    private static int getCurrentSalvagePct(Contract contract, Money contract1) {
        int currentSalvagePct = 0;
        if (contract.getSalvagedByUnit().plus(contract1).isPositive()) {
            currentSalvagePct = contract.getSalvagedByUnit()
                                      .multipliedBy(100)
                                      .dividedBy(contract.getSalvagedByUnit().plus(contract.getSalvagedByEmployer()))
                                      .getAmount()
                                      .intValue();
        }
        return currentSalvagePct;
    }

    private void fillStatsAtBContract() {
        AtBContract contract = (AtBContract) mission;
        Campaign campaign = gui.getCampaign();

        // TODO : Switch me to use IUnitRating
        String[] ratingNames = { "F", "D", "C", "B", "A" };
        lblStatus = new JLabel();
        lblLocation = new JLabel();
        txtLocation = new JLabel();
        lblEmployer = new JLabel();
        txtEmployer = new JLabel();
        /* AtB Contract Parameters */
        JLabel lblEnemy = new JLabel();
        JLabel txtEnemy = new JLabel();
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
        JLabel lblAllyRating = new JLabel();
        JLabel txtAllyRating = new JLabel();
        JLabel lblEnemyRating = new JLabel();
        JLabel txtEnemyRating = new JLabel();
        JLabel lblMorale = new JLabel();
        JLabel txtMorale = new JLabel();
        JLabel lblSharePct = new JLabel();
        JLabel txtSharePct = new JLabel();
        JLabel lblCargoRequirement = new JLabel();
        JLabel txtCargoRequirement = new JLabel();
        JLabel lblScore = new JLabel();
        JLabel txtScore = new JLabel();

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

        lblBelligerents = contract.getBelligerentsPanel(gui.getCampaign().getGameYear());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy++;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        pnlStats.add(lblBelligerents, gridBagConstraints);

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
        txtCommand.setToolTipText(wordWrap(contract.getCommandRights().getToolTipText()));
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

        JLabel lblSalvagePct = new JLabel(resourceMap.getString("lblSalvage.text"));
        JLabel txtSalvagePct = new JLabel();
        txtSalvagePct.setName("txtSalvagePct");

        if (contract.isSalvageExchange()) {
            txtSalvagePct.setText(resourceMap.getString("exchange") + " (" + contract.getSalvagePct() + "%)");
        } else if (contract.getSalvagePct() == 0) {
            txtSalvagePct.setText(resourceMap.getString("none"));
        } else {
            lblSalvagePct.setText(resourceMap.getString("lblSalvagePct.text"));
            int maxSalvagePct = contract.getSalvagePct();

            int currentSalvagePct = getCurrentSalvagePct(contract, contract.getSalvagedByEmployer());

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

        if (contract.getContractType().isGarrisonDuty() && contract.getMoraleLevel().isRouted()) {
            txtMorale.setText(resourceMap.getString("txtGarrisonMoraleRouted.text"));
            txtMorale.setToolTipText(wordWrap(resourceMap.getString("txtGarrisonMoraleRouted.tooltip")));
        } else {
            txtMorale.setText(contract.getMoraleLevel().toString());
            txtMorale.setToolTipText(wordWrap(contract.getMoraleLevel().getToolTipText()));
        }
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
            lblSharePct.setToolTipText(wordWrap(contract.getMoraleLevel().getToolTipText()));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblSharePct, gridBagConstraints);

            txtSharePct.setName("txtSharePct");
            txtSharePct.setText(contract.getSharesPercent() + "%");
            txtSharePct.setToolTipText(wordWrap(contract.getMoraleLevel().getToolTipText()));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtSharePct, gridBagConstraints);
        }

        if (campaign.getCampaignOptions().isUseStratCon()) {
            lblCargoRequirement.setName("lblCargoRequirement");
            lblCargoRequirement.setText(resourceMap.getString("lblCargoRequirement.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCargoRequirement, gridBagConstraints);

            txtCargoRequirement.setName("txtCargoRequirement");
            txtCargoRequirement.setText("~" + estimateCargoRequirements(campaign, contract) + 't');
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtCargoRequirement, gridBagConstraints);
        }

        lblScore.setName("lblScore");
        lblScore.setText(resourceMap.getString("lblScore.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblScore, gridBagConstraints);

        txtScore.setName("txtScore");
        txtScore.setText(Integer.toString(contract.getContractScore(campaign.getCampaignOptions()
                                                                          .isUseStratConMaplessMode())));
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
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtDesc, gridBagConstraints);
    }

    /**
     * Initializes and populates the tutorial panel with formatted HTML content inside a {@link JEditorPane}, applies
     * font scaling and styling, wraps the editor in a scroll pane with appropriate padding, and adds it to the main
     * tutorial panel with a visual border and size constraints.
     *
     * <p>The content is sourced from a resource bundle and displayed using an HTML/CSS styled {@code JEditorPane}
     * for enhanced presentation.</p>
     *
     * <p>The method ensures the scroll position starts at the top of the content.</p>
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void fillTutorial() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setFocusable(false);
        editorPane.setBorder(BorderFactory.createEmptyBorder());

        String fontStyle = "font-family: Noto Sans;";
        editorPane.setText(String.format("<div style='width: %s; %s padding:%spx;'>%s</div>",
              scaleForGUI(590),
              fontStyle,
              scaleForGUI(5),
              resourceMap.getString("txtStratConTutorial.text")));
        setFontScaling(editorPane, false, 1.1);

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        SwingUtilities.invokeLater(() -> scrollPane.getViewport().setViewPosition(new Point(0, 0)));

        JPanel scrollPaneContainer = new JPanel(new BorderLayout());
        scrollPaneContainer.add(scrollPane, BorderLayout.CENTER);

        pnlTutorial = new JPanel(new BorderLayout());

        pnlTutorial.setBorder(RoundedLineBorder.createRoundedLineBorder());
        pnlTutorial.setPreferredSize(new Dimension(600, 0));
        pnlTutorial.setMinimumSize(new Dimension(600, 0));
        pnlTutorial.add(scrollPane, BorderLayout.CENTER);
    }
}
