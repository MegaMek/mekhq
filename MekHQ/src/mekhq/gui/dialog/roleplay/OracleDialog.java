/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.roleplay;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import mekhq.campaign.Campaign;
import mekhq.campaign.roleplay.FateChart;
import mekhq.campaign.roleplay.FateChartOdds;
import mekhq.campaign.roleplay.FateChartResult;
import mekhq.campaign.roleplay.Roleplay;

/**
 * Lets the player ask the {@link FateChart} a yes/no question. The player chooses the odds, adjusts the campaign's
 * chaos factor (stored in the campaign's {@link Roleplay}), and rolls.
 */
public class OracleDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final Roleplay roleplay;

    private JComboBox<FateChartOdds> cboOdds;
    private JLabel lblChaosValue;
    private JButton btnDecreaseChaos;
    private JButton btnIncreaseChaos;
    private JLabel lblResult;

    public OracleDialog(final JFrame frame, final Campaign campaign) {
        super(frame, getTextAt(RESOURCE_BUNDLE, "OracleDialog.title"), true);
        this.roleplay = campaign.getRoleplay();
        initialize();
        pack();
        setLocationRelativeTo(frame);
    }

    private void initialize() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel pnlMain = new JPanel(new GridBagLayout());
        pnlMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        // Odds
        JLabel lblOdds = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleDialog.odds.label"));
        cboOdds = new JComboBox<>(FateChartOdds.values());
        cboOdds.setSelectedItem(FateChartOdds.FIFTY_FIFTY);
        cboOdds.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleDialog.odds.toolTipText"));
        lblOdds.setLabelFor(cboOdds);
        constraints.gridx = 0;
        constraints.gridy = 0;
        pnlMain.add(lblOdds, constraints);
        constraints.gridx = 1;
        pnlMain.add(cboOdds, constraints);

        // Chaos factor
        JLabel lblChaos = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.label"));
        lblChaos.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.toolTipText"));
        constraints.gridx = 0;
        constraints.gridy = 1;
        pnlMain.add(lblChaos, constraints);
        constraints.gridx = 1;
        pnlMain.add(createChaosPanel(), constraints);

        // Result
        lblResult = new JLabel(getTextAt(RESOURCE_BUNDLE, "OracleDialog.prompt"), SwingConstants.CENTER);
        lblResult.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        pnlMain.add(lblResult, constraints);

        // Buttons
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnRoll = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.roll"));
        btnRoll.addActionListener(event -> roll());
        JButton btnClose = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.close"));
        btnClose.addActionListener(event -> dispose());
        pnlButtons.add(btnRoll);
        pnlButtons.add(btnClose);
        getRootPane().setDefaultButton(btnRoll);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(pnlMain, BorderLayout.CENTER);
        getContentPane().add(pnlButtons, BorderLayout.SOUTH);
    }

    private JPanel createChaosPanel() {
        JPanel pnlChaos = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        btnDecreaseChaos = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.decrease"));
        btnDecreaseChaos.addActionListener(event -> {
            roleplay.decreaseChaosFactor();
            refreshChaos();
        });

        lblChaosValue = new JLabel();
        lblChaosValue.setFont(lblChaosValue.getFont().deriveFont(Font.BOLD));
        lblChaosValue.setToolTipText(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.toolTipText"));

        btnIncreaseChaos = new JButton(getTextAt(RESOURCE_BUNDLE, "OracleDialog.chaos.increase"));
        btnIncreaseChaos.addActionListener(event -> {
            roleplay.increaseChaosFactor();
            refreshChaos();
        });

        pnlChaos.add(btnDecreaseChaos);
        pnlChaos.add(lblChaosValue);
        pnlChaos.add(btnIncreaseChaos);
        refreshChaos();
        return pnlChaos;
    }

    private void refreshChaos() {
        int chaosFactor = roleplay.getChaosFactor();
        lblChaosValue.setText(String.valueOf(chaosFactor));
        btnDecreaseChaos.setEnabled(chaosFactor > FateChart.MINIMUM_CHAOS_FACTOR);
        btnIncreaseChaos.setEnabled(chaosFactor < FateChart.MAXIMUM_CHAOS_FACTOR);
    }

    private void roll() {
        FateChartOdds odds = (FateChartOdds) cboOdds.getSelectedItem();
        if (odds == null) {
            return;
        }

        int chaosFactor = roleplay.getChaosFactor();
        FateChartResult result = FateChart.consult(odds, chaosFactor);

        if (result.hasRandomEvent()) {
            lblResult.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleDialog.result.randomEvent",
                  result.answer().getLabel(), result.roll(), odds.getLabel(), chaosFactor,
                  result.randomEventFocus().getLabel(), result.randomEventRoll(),
                  result.randomEventFocus().getDescription()));
        } else {
            lblResult.setText(getFormattedTextAt(RESOURCE_BUNDLE, "OracleDialog.result",
                  result.answer().getLabel(), result.roll(), odds.getLabel(), chaosFactor));
        }
        pack();
    }
}
