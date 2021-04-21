/*
 * Copyright (C) 2009-2020 - The MegaMek Team. All Rights Reserved.
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

import java.time.LocalDate;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.JScrollablePanel;

/**
 * A custom panel that gets filled in with goodies from a JumpPath record
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class JumpPathViewPanel extends JScrollablePanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private JumpPath path;
    private Campaign campaign;

    private javax.swing.JPanel pnlPath;
    private javax.swing.JPanel pnlStats;

    private javax.swing.JLabel lblJumps;
    private javax.swing.JLabel txtJumps;
    private javax.swing.JLabel lblTimeStart;
    private javax.swing.JLabel txtTimeStart;
    private javax.swing.JLabel lblTimeEnd;
    private javax.swing.JLabel txtTimeEnd;
    private javax.swing.JLabel lblRechargeTime;
    private javax.swing.JLabel txtRechargeTime;
    private javax.swing.JLabel lblTotalTime;
    private javax.swing.JLabel txtTotalTime;
    private javax.swing.JLabel lblCost;
    private javax.swing.JLabel txtCost;

    public JumpPathViewPanel(JumpPath p, Campaign c) {
        super();
        this.path = p;
        this.campaign = c;
        initComponents();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        pnlStats = new javax.swing.JPanel();
        pnlPath = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());


        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder("Summary"));
        fillStats();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        pnlPath.setName("pnlPath");
        pnlPath.setBorder(BorderFactory.createTitledBorder("Full Path"));
        getPath();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(pnlPath, gridBagConstraints);
    }

    private void getPath() {
        java.awt.GridBagConstraints gridBagConstraints;
        pnlPath.setLayout(new java.awt.GridBagLayout());
        int i = 0;
        javax.swing.JLabel lblPlanet;
        LocalDate currentDate = campaign.getLocalDate();
        for (PlanetarySystem system : path.getSystems()) {
            lblPlanet = new javax.swing.JLabel(system.getPrintableName(currentDate) + " (" + system.getRechargeTimeText(currentDate) + ")");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            if (i >= (path.getSystems().size() - 1)) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlPath.add(lblPlanet, gridBagConstraints);
            i++;
        }
    }

    private void fillStats() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.JumpPathViewPanel", new EncodeControl()); //$NON-NLS-1$

        lblJumps = new javax.swing.JLabel();
        txtJumps = new javax.swing.JLabel();
        lblTimeStart = new javax.swing.JLabel();
        txtTimeStart = new javax.swing.JLabel();
        lblTimeEnd = new javax.swing.JLabel();
        txtTimeEnd = new javax.swing.JLabel();
        lblRechargeTime = new javax.swing.JLabel();
        txtRechargeTime = new javax.swing.JLabel();
        lblTotalTime = new javax.swing.JLabel();
        txtTotalTime = new javax.swing.JLabel();
        lblCost = new javax.swing.JLabel();
        txtCost = new javax.swing.JLabel();

        LocalDate currentDate = campaign.getLocalDate();
        String startName = (path.getFirstSystem() == null) ? "?" : path.getFirstSystem().getPrintableName(currentDate);
        String endName = (path.getLastSystem() == null) ? "?" : path.getLastSystem().getPrintableName(currentDate);

        java.awt.GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new java.awt.GridBagLayout());

        lblJumps.setName("lblJumps"); // NOI18N
        lblJumps.setText(resourceMap.getString("lblJumps1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblJumps, gridBagConstraints);

        txtJumps.setName("lblJumps2"); // NOI18N
        txtJumps.setText("<html>" + path.getJumps() + " jumps" + "</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtJumps, gridBagConstraints);

        lblTimeStart.setName("lblTimeStart"); // NOI18N
        lblTimeStart.setText(resourceMap.getString("lblTimeStart1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTimeStart, gridBagConstraints);

        txtTimeStart.setName("lblTimeStart2"); // NOI18N
        txtTimeStart.setText("<html>" + Math.round(path.getStartTime(campaign.getLocation().getTransitTime())*100.0)/100.0 + " days from "+ startName + " to jump point" + "</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTimeStart, gridBagConstraints);

        lblTimeEnd.setName("lblTimeEnd"); // NOI18N
        lblTimeEnd.setText(resourceMap.getString("lblTimeEnd1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTimeEnd, gridBagConstraints);

        txtTimeEnd.setName("lblTimeEnd2"); // NOI18N
        txtTimeEnd.setText("<html>" + Math.round(path.getEndTime()*100.0)/100.0 + " days from final jump point to " + endName + "</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTimeEnd, gridBagConstraints);

        lblRechargeTime.setName("lblRechargeTime1"); // NOI18N
        lblRechargeTime.setText(resourceMap.getString("lblRechargeTime1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblRechargeTime, gridBagConstraints);

        txtRechargeTime.setName("lblRechargeTime2"); // NOI18N
        txtRechargeTime.setText("<html>" + Math.round(path.getTotalRechargeTime(currentDate)*100.0)/100.0 + " days" + "</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtRechargeTime, gridBagConstraints);

        lblTotalTime.setName("lblTotalTime1"); // NOI18N
        lblTotalTime.setText(resourceMap.getString("lblTotalTime1.text"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblTotalTime, gridBagConstraints);

        txtTotalTime.setName("lblTotalTime2"); // NOI18N
        txtTotalTime.setText("<html>" + Math.round(path.getTotalTime(currentDate, campaign.getLocation().getTransitTime())*100.0)/100.0 + " days" + "</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(txtTotalTime, gridBagConstraints);

        if (campaign.getCampaignOptions().payForTransport()) {
            lblCost.setName("lblCost1"); // NOI18N
            lblCost.setText(resourceMap.getString("lblCost1.text"));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 6;
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(lblCost, gridBagConstraints);

            txtCost.setName("lblCost2"); // NOI18N
            txtCost.setText("<html>" +
                    campaign.calculateCostPerJump(
                                true,
                                campaign.getCampaignOptions().useEquipmentContractBase())
                            .multipliedBy(path.getJumps())
                                .toAmountAndSymbolString() + "</html>");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 6;
            gridBagConstraints.weightx = 0.5;
            gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(txtCost, gridBagConstraints);
        }
    }
}
