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
 */
package mekhq.gui.view;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.JScrollablePanel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * A custom panel that gets filled in with goodies from a JumpPath record
 * @author  Jay Lawson (jaylawson39 at yahoo.com)
 */
public class JumpPathViewPanel extends JScrollablePanel {
    private JumpPath path;
    private Campaign campaign;

    private JPanel pnlPath;
    private JPanel pnlStats;

    private JLabel lblJumps;
    private JLabel txtJumps;
    private JLabel lblTimeStart;
    private JLabel txtTimeStart;
    private JLabel lblTimeEnd;
    private JLabel txtTimeEnd;
    private JLabel lblRechargeTime;
    private JLabel txtRechargeTime;
    private JLabel lblTotalTime;
    private JLabel txtTotalTime;
    private JLabel lblCost;
    private JLabel txtCost;

    public JumpPathViewPanel(JumpPath p, Campaign c) {
        super();
        this.path = p;
        this.campaign = c;
        initComponents();
    }

    private void initComponents() {

        pnlStats = new JPanel();
        pnlPath = new JPanel();

        setLayout(new GridBagLayout());


        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder("Summary"));
        fillStats();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        pnlPath.setName("pnlPath");
        pnlPath.setBorder(BorderFactory.createTitledBorder("Full Path"));
        getPath();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(pnlPath, gridBagConstraints);
    }

    private void getPath() {
        GridBagConstraints gridBagConstraints;
        pnlPath.setLayout(new GridBagLayout());
        int i = 0;
        JLabel lblPlanet;
        LocalDate currentDate = campaign.getLocalDate();
        for (PlanetarySystem system : path.getSystems()) {
            lblPlanet = new JLabel(system.getPrintableName(currentDate) + " (" + system.getRechargeTimeText(currentDate) + ")");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            if (i >= (path.getSystems().size() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(0, 0, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlPath.add(lblPlanet, gridBagConstraints);
            i++;
        }
    }

    private void fillStats() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.JumpPathViewPanel",
                MekHQ.getMHQOptions().getLocale());

        lblJumps = new JLabel();
        txtJumps = new JLabel();
        lblTimeStart = new JLabel();
        txtTimeStart = new JLabel();
        lblTimeEnd = new JLabel();
        txtTimeEnd = new JLabel();
        lblRechargeTime = new JLabel();
        txtRechargeTime = new JLabel();
        lblTotalTime = new JLabel();
        txtTotalTime = new JLabel();
        lblCost = new JLabel();
        txtCost = new JLabel();

        LocalDate currentDate = campaign.getLocalDate();
        String startName = (path.getFirstSystem() == null) ? "?" : path.getFirstSystem().getPrintableName(currentDate);
        String endName = (path.getLastSystem() == null) ? "?" : path.getLastSystem().getPrintableName(currentDate);

        GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new GridBagLayout());

        lblJumps.setName("lblJumps");
        lblJumps.setText(resourceMap.getString("lblJumps1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblJumps, gridBagConstraints);

        txtJumps.setName("lblJumps2");
        txtJumps.setText("<html>" + path.getJumps() + " jumps" + "</html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtJumps, gridBagConstraints);

        lblTimeStart.setName("lblTimeStart");
        lblTimeStart.setText(resourceMap.getString("lblTimeStart1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTimeStart, gridBagConstraints);

        txtTimeStart.setName("lblTimeStart2");
        txtTimeStart.setText("<html>" + Math.round(path.getStartTime(campaign.getLocation().getTransitTime())*100.0)/100.0 + " days from "+ startName + " to jump point" + "</html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTimeStart, gridBagConstraints);

        lblTimeEnd.setName("lblTimeEnd");
        lblTimeEnd.setText(resourceMap.getString("lblTimeEnd1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTimeEnd, gridBagConstraints);

        txtTimeEnd.setName("lblTimeEnd2");
        txtTimeEnd.setText("<html>" + Math.round(path.getEndTime()*100.0)/100.0 + " days from final jump point to " + endName + "</html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTimeEnd, gridBagConstraints);

        lblRechargeTime.setName("lblRechargeTime1");
        lblRechargeTime.setText(resourceMap.getString("lblRechargeTime1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblRechargeTime, gridBagConstraints);

        txtRechargeTime.setName("lblRechargeTime2");
        txtRechargeTime.setText("<html>" + Math.round(path.getTotalRechargeTime(currentDate)*100.0)/100.0 + " days" + "</html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtRechargeTime, gridBagConstraints);

        lblTotalTime.setName("lblTotalTime1");
        lblTotalTime.setText(resourceMap.getString("lblTotalTime1.text"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTotalTime, gridBagConstraints);

        txtTotalTime.setName("lblTotalTime2");
        txtTotalTime.setText("<html>" + Math.round(path.getTotalTime(currentDate, campaign.getLocation().getTransitTime())*100.0)/100.0 + " days" + "</html>");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTotalTime, gridBagConstraints);

        if (campaign.getCampaignOptions().isPayForTransport()) {
            lblCost.setName("lblCost1");
            lblCost.setText(resourceMap.getString("lblCost1.text"));
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 6;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCost, gridBagConstraints);

            txtCost.setName("lblCost2");
            txtCost.setText("<html>" +
                    campaign.calculateCostPerJump(
                                true,
                                campaign.getCampaignOptions().isEquipmentContractBase())
                            .multipliedBy(path.getJumps())
                                .toAmountAndSymbolString() + "</html>");
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 6;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new Insets(0, 10, 0, 0);
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            pnlStats.add(txtCost, gridBagConstraints);
        }
    }
}
