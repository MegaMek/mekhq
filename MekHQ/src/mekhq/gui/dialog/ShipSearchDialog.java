/*
 * ShipSearchDialog.java
 *
 * Copyright (c) 2016 Carl Spain. All rights reserved.
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ResourceBundle;

import javax.swing.*;

import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.TargetRoll;
import megamek.common.UnitType;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.finances.Money;
import mekhq.gui.CampaignGUI;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

/**
 * Manages searches for DropShips or JumpShips for Against the Bot.
 *
 * @author Neoancient
 */
public class ShipSearchDialog extends JDialog {
    private static final long serialVersionUID = -5200817760228732045L;

    private JRadioButton btnDropship = new JRadioButton();
    private JRadioButton btnJumpship = new JRadioButton();
    private JRadioButton btnWarship = new JRadioButton();

    private JLabel lblDropshipTarget = new JLabel();
    private JLabel lblJumpshipTarget = new JLabel();
    private JLabel lblWarshipTarget = new JLabel();

    CampaignGUI gui;

    public ShipSearchDialog(JFrame frame, CampaignGUI gui) {
        super(frame, true);
        this.gui = gui;

        init();
        setUserPreferences();
    }

    private void init() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ShipSearchDialog",
                new EncodeControl()); //$NON-NLS-1$
        setTitle(resourceMap.getString("title.text"));

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JLabel lblInstructions = new JLabel();
        lblInstructions.setText("<html>" + String.format(resourceMap.getString("instructions.text"),
                gui.getCampaign().getAtBConfig().getShipSearchCost().toAmountAndSymbolString())
                + "</html>");
        contentPane.add(lblInstructions, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        ButtonGroup group = new ButtonGroup();
        if (gui.getCampaign().getAtBConfig().shipSearchTargetBase(UnitType.DROPSHIP) != null) {
            btnDropship.setText(resourceMap.getString("btnDropship.text"));
            group.add(btnDropship);
            gbc.gridx = 0;
            gbc.gridy = 0;
            mainPanel.add(btnDropship, gbc);

            TargetRoll target = gui.getCampaign().getAtBConfig().shipSearchTargetRoll(UnitType.DROPSHIP,
                    gui.getCampaign());
            lblDropshipTarget.setText("Target: " + target.getValue() + " ["
                    + target.getDesc() + "]");
            gbc.gridx = 0;
            gbc.gridy = 1;
            mainPanel.add(lblDropshipTarget, gbc);
        }

        if (gui.getCampaign().getAtBConfig().shipSearchTargetBase(UnitType.JUMPSHIP) != null) {
            btnJumpship.setText(resourceMap.getString("btnJumpship.text"));
            group.add(btnJumpship);
            gbc.gridx = 0;
            gbc.gridy = 2;
            mainPanel.add(btnJumpship, gbc);

            TargetRoll target = gui.getCampaign().getAtBConfig().shipSearchTargetRoll(UnitType.JUMPSHIP,
                    gui.getCampaign());
            lblJumpshipTarget.setText("Target: " + target.getValue() + " ["
                    + target.getDesc() + "]");
            gbc.gridx = 0;
            gbc.gridy = 3;
            mainPanel.add(lblJumpshipTarget, gbc);
        }

        if (gui.getCampaign().getAtBConfig().shipSearchTargetBase(UnitType.WARSHIP) != null) {
            btnWarship.setText(resourceMap.getString("btnWarship.text"));
            group.add(btnWarship);
            gbc.gridx = 0;
            gbc.gridy = 4;
            mainPanel.add(btnWarship, gbc);

            TargetRoll target = gui.getCampaign().getAtBConfig().shipSearchTargetRoll(UnitType.WARSHIP,
                    gui.getCampaign());
            lblWarshipTarget.setText("Target: " + target.getValue() + " ["
                    + target.getDesc() + "]");
            gbc.gridx = 0;
            gbc.gridy = 5;
            mainPanel.add(lblWarshipTarget, gbc);
        }

        if (gui.getCampaign().hasActiveContract() && !isInSearch()) {
            JLabel lblInContract = new JLabel(resourceMap.getString("lblInContract.text"));
            gbc.gridx = 0;
            gbc.gridy = 6;
            mainPanel.add(lblInContract, gbc);
        }

        if (gui.getCampaign().hasActiveContract() || isInSearch()) {
            btnDropship.setEnabled(false);
            lblDropshipTarget.setEnabled(false);
            btnJumpship.setEnabled(false);
            lblJumpshipTarget.setEnabled(false);
            btnWarship.setEnabled(false);
            lblWarshipTarget.setEnabled(false);
        }

        if (gui.getCampaign().getAtBConfig().shipSearchTargetBase(UnitType.DROPSHIP) != null) {
            btnDropship.setSelected(true);
        } else if (gui.getCampaign().getAtBConfig().shipSearchTargetBase(UnitType.JUMPSHIP) != null) {
            btnJumpship.setSelected(true);
        } else if (gui.getCampaign().getAtBConfig().shipSearchTargetBase(UnitType.WARSHIP) != null) {
            btnWarship.setSelected(true);
        } else {
            JLabel label = new JLabel(resourceMap.getString("lblNoSearch.text"));
            gbc.gridx = 0;
            gbc.gridy = 7;
            mainPanel.add(label, gbc);
        }

        contentPane.add(mainPanel, BorderLayout.CENTER);

        JPanel panButtons = new JPanel();

        JButton button;

        if (gui.getCampaign().getShipSearchResult() != null) {
            MechSummary ms = MechSummaryCache.getInstance().getMech(gui.getCampaign().getShipSearchResult());

            if ((ms != null) || (gui.getCampaign().getShipSearchResult() != null)) {
                JLabel lblAvailable = new JLabel();
                if ((ms == null) && (gui.getCampaign().getShipSearchResult() != null)) {
                    lblAvailable.setText("Cannot find entry for " + gui.getCampaign().getShipSearchResult());
                } else {
                    lblAvailable.setText(resourceMap.getString("lblAvailable.text")
                            + gui.getCampaign().getShipSearchResult());
                }
                gbc.gridx = 0;
                gbc.gridy = 8;
                mainPanel.add(lblAvailable);
            }

            button = new JButton(resourceMap.getString("btnPurchase.text"));
            panButtons.add(button);
            button.addActionListener(ev -> purchase());
            button.setEnabled(ms != null && gui.getCampaign().getFunds().isGreaterThan(Money.of(ms.getCost())));
        }

        if (isInSearch()) {
            button = new JButton(resourceMap.getString("btnEndSearch.text"));
            button.setToolTipText(resourceMap.getString("btnEndSearch.toolTipText"));
            button.addActionListener(ev -> endSearch());
        } else {
            button = new JButton(resourceMap.getString("btnStartSearch.text"));
            button.setToolTipText(String.format(resourceMap.getString("btnStartSearch.toolTipText"),
                    gui.getCampaign().getAtBConfig().shipSearchCostPerWeek().toAmountAndSymbolString(),
                    gui.getCampaign().getAtBConfig().getShipSearchLengthWeeks()));
            button.addActionListener(ev -> startSearch());
            button.setEnabled(!gui.getCampaign().hasActiveContract());
        }
        panButtons.add(button);

        button = new JButton(resourceMap.getString("btnCancel.text"));
        button.addActionListener(ev -> setVisible(false));
        panButtons.add(button);

        contentPane.add(panButtons, BorderLayout.SOUTH);

        pack();
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(ShipSearchDialog.class);

        btnDropship.setName("dropship");
        preferences.manage(new JToggleButtonPreference(btnDropship));

        btnJumpship.setName("jumpship");
        preferences.manage(new JToggleButtonPreference(btnJumpship));

        btnWarship.setName("warship");
        preferences.manage(new JToggleButtonPreference(btnWarship));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    public TargetRoll getDSTarget() {
        return gui.getCampaign().getAtBConfig().shipSearchTargetRoll(UnitType.DROPSHIP,
                gui.getCampaign());
    }

    public TargetRoll getJSTarget() {
        return gui.getCampaign().getAtBConfig().shipSearchTargetRoll(UnitType.JUMPSHIP,
                gui.getCampaign());
    }

    public TargetRoll getWSTarget() {
        return gui.getCampaign().getAtBConfig().shipSearchTargetRoll(UnitType.WARSHIP,
                gui.getCampaign());
    }

    private int getUnitType() {
        if (btnJumpship.isSelected()) {
            return UnitType.JUMPSHIP;
        } else if (btnWarship.isSelected()) {
            return UnitType.WARSHIP;
        } else {
            return UnitType.DROPSHIP;
        }
    }

    private boolean isInSearch() {
        return gui.getCampaign().getShipSearchStart() != null;
    }

    private void startSearch() {
        gui.getCampaign().startShipSearch(getUnitType());
        setVisible(false);
    }

    private void endSearch() {
        gui.getCampaign().setShipSearchStart(null);
        setVisible(false);
    }

    private void purchase() {
        gui.getCampaign().purchaseShipSearchResult();
        setVisible(false);
    }
}
