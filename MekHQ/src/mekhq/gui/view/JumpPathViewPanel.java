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

import static java.lang.Math.ceil;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.gui.baseComponents.JScrollablePanel;

/**
 * A custom panel that gets filled in with goodies from a JumpPath record
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class JumpPathViewPanel extends JScrollablePanel {
    private final JumpPath path;
    private final Campaign campaign;

    private JPanel pnlPath;
    private JPanel pnlStats;

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

        boolean isUseCommandCircuit =
              FactionStandingUtilities.isUseCommandCircuit(campaign.isOverridingCommandCircuitRequirements(),
                    campaign.isGM(),
                    campaign.getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
                    campaign.getFactionStandings(), campaign.getFutureAtBContracts());

        for (PlanetarySystem system : path.getSystems()) {
            lblPlanet =
                  new JLabel(system.getPrintableName(currentDate) + " (" + system.getRechargeTimeText(currentDate,
                        isUseCommandCircuit) + ")");
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

        JLabel lblJumps = new JLabel();
        JLabel txtJumps = new JLabel();
        JLabel lblTimeStart = new JLabel();
        JLabel txtTimeStart = new JLabel();
        JLabel lblTimeEnd = new JLabel();
        JLabel txtTimeEnd = new JLabel();
        JLabel lblRechargeTime = new JLabel();
        JLabel txtRechargeTime = new JLabel();
        JLabel lblTotalTime = new JLabel();
        JLabel txtTotalTime = new JLabel();
        JLabel lblCost = new JLabel();
        JLabel txtCost = new JLabel();

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
        txtTimeStart.setText("<html>" +
                                   Math.round(path.getStartTime(campaign.getLocation().getTransitTime()) * 100.0) /
                                         100.0 +
                                   " days from " +
                                   startName +
                                   " to jump point" +
                                   "</html>");
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
        txtTimeEnd.setText("<html>" +
                                 Math.round(path.getEndTime() * 100.0) / 100.0 +
                                 " days from final jump point to " +
                                 endName +
                                 "</html>");
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

        boolean isUseCommandCircuit = FactionStandingUtilities.isUseCommandCircuit(campaign.isOverridingCommandCircuitRequirements(),
              campaign.isGM(), campaign.getCampaignOptions().isUseFactionStandingCommandCircuitSafe(),
              campaign.getFactionStandings(), campaign.getFutureAtBContracts());

        txtRechargeTime.setName("lblRechargeTime2");
        txtRechargeTime.setText("<html>" +
                                      Math.round(path.getTotalRechargeTime(currentDate, isUseCommandCircuit) * 100.0) /
                                            100.0 +
                                      " days" +
                                      "</html>");
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
        txtTotalTime.setText("<html>" + Math.round(path.getTotalTime(currentDate,
              campaign.getLocation().getTransitTime(), isUseCommandCircuit) * 100.0) / 100.0 + " days" + "</html>");
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

            TransportCostCalculations transportCostCalculations = campaign.getTransportCostCalculation(EXP_REGULAR);
            int duration = (int) ceil(path.getTotalTime(currentDate, campaign.getLocation().getTransitTime(),
                  isUseCommandCircuit));
            Money journeyCost = transportCostCalculations.calculateJumpCostForEntireJourney(duration);

            txtCost.setName("lblCost2");
            txtCost.setText("<html>" + journeyCost.toAmountAndSymbolString() + "</html>");
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
