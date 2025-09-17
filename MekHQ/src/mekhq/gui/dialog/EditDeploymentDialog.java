/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import static megamek.client.ui.util.UIUtil.uiGreen;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import megamek.client.ui.GBC;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.TipButton;
import megamek.common.Player;
import megamek.common.interfaces.IStartingPositions;
import mekhq.MekHQ;

public class EditDeploymentDialog extends JDialog {

    Player player;

    private int currentStartPos;

    private final JPanel panStartButtons = new JPanel();
    private final TipButton[] butStartPos = new TipButton[11];
    private final NumberFormatter numFormatter = new NumberFormatter(NumberFormat.getIntegerInstance());
    private final DefaultFormatterFactory formatterFactory = new DefaultFormatterFactory(numFormatter);
    private final JFormattedTextField txtOffset = new JFormattedTextField(formatterFactory, 0);
    private final JFormattedTextField txtWidth = new JFormattedTextField(formatterFactory, 3);
    private JSpinner spinStartingAnyNWx;
    private JSpinner spinStartingAnyNWy;
    private JSpinner spinStartingAnySEx;
    private JSpinner spinStartingAnySEy;

    public EditDeploymentDialog(JFrame parent, boolean modal, Player player) {
        super(parent, modal);
        this.player = player;
        currentStartPos = player.getStartingPos();
        initComponents();
        setLocationRelativeTo(parent);
        pack();
    }

    private void initComponents() {

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.EditDeploymentDialog",
              MekHQ.getMHQOptions().getLocale());

        panStartButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < 11; i++) {
            butStartPos[i] = new TipButton("");
            butStartPos[i].addActionListener(startListener);
        }
        panStartButtons.setLayout(new GridLayout(4, 3));
        panStartButtons.add(butStartPos[1]);
        panStartButtons.add(butStartPos[2]);
        panStartButtons.add(butStartPos[3]);
        panStartButtons.add(butStartPos[8]);
        panStartButtons.add(butStartPos[10]);
        panStartButtons.add(butStartPos[4]);
        panStartButtons.add(butStartPos[7]);
        panStartButtons.add(butStartPos[6]);
        panStartButtons.add(butStartPos[5]);
        panStartButtons.add(butStartPos[0]);
        panStartButtons.add(butStartPos[9]);
        updateStartGrid();

        SpinnerNumberModel mStartingAnyNWx = new SpinnerNumberModel(player.getStartingAnyNWx() + 1, 0,
              null, 1);
        spinStartingAnyNWx = new JSpinner(mStartingAnyNWx);
        SpinnerNumberModel mStartingAnyNWy = new SpinnerNumberModel(player.getStartingAnyNWy() + 1, 0,
              null, 1);
        spinStartingAnyNWy = new JSpinner(mStartingAnyNWy);
        SpinnerNumberModel mStartingAnySEx = new SpinnerNumberModel(player.getStartingAnySEx() + 1, 0,
              null, 1);
        spinStartingAnySEx = new JSpinner(mStartingAnySEx);
        SpinnerNumberModel mStartingAnySEy = new SpinnerNumberModel(player.getStartingAnySEy() + 1, 0,
              null, 1);
        spinStartingAnySEy = new JSpinner(mStartingAnySEy);

        GridBagLayout gbl = new GridBagLayout();
        JPanel main = new JPanel(gbl);

        JLabel lblOffset = new JLabel(resourceMap.getString("labDeploymentOffset.text"));
        lblOffset.setToolTipText(resourceMap.getString("labDeploymentOffset.tip"));
        JLabel lblWidth = new JLabel(resourceMap.getString("labDeploymentWidth.text"));
        lblWidth.setToolTipText(resourceMap.getString("labDeploymentWidth.tip"));

        txtOffset.setColumns(4);
        txtWidth.setColumns(4);
        txtWidth.setText(Integer.toString(player.getStartWidth()));
        txtOffset.setText(Integer.toString(player.getStartOffset()));

        main.add(lblOffset, GBC.std());
        main.add(txtOffset, GBC.eol());
        main.add(lblWidth, GBC.std());
        main.add(txtWidth, GBC.eol());

        main.add(new JLabel(resourceMap.getString("labDeploymentAnyNW.text")), GBC.std());
        main.add(spinStartingAnyNWx, GBC.std());
        main.add(spinStartingAnyNWy, GBC.eol());
        main.add(new JLabel(resourceMap.getString("labDeploymentAnySE.text")), GBC.std());
        main.add(spinStartingAnySEx, GBC.std());
        main.add(spinStartingAnySEy, GBC.eol());

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(panStartButtons, BorderLayout.PAGE_START);

        JPanel panButtons = new JPanel(new FlowLayout());
        JButton btnOK = new JButton(resourceMap.getString("btnOK.text"));
        btnOK.addActionListener(this::done);
        JButton btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.addActionListener(this::cancel);
        panButtons.add(btnOK);
        panButtons.add(btnCancel);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);

    }

    private void updateStartGrid() {
        StringBuilder[] butText = new StringBuilder[11];
        StringBuilder[] butTT = new StringBuilder[11];

        for (int i = 0; i < 11; i++) {
            butText[i] = new StringBuilder();
            butTT[i] = new StringBuilder();
        }

        for (int i = 0; i < 11; i++) {
            butText[i].append("<HTML><P ALIGN=CENTER>");
            // butTT[i].append(Messages.getString("PlayerSettingsDialog.invalidStartPosTT"));
            butText[i].append(IStartingPositions.START_LOCATION_NAMES[i]).append("</FONT><BR>");
        }

        butText[currentStartPos].append(UIUtil.fontHTML(uiGreen()));
        butText[currentStartPos].append("\u2B24</FONT>");

        for (int i = 0; i < 11; i++) {
            butStartPos[i].setText(butText[i].toString());
            if (!butTT[i].isEmpty()) {
                butStartPos[i].setToolTipText(butTT[i].toString());
            }
        }
    }

    ActionListener startListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Deployment buttons
            for (int i = 0; i < 11; i++) {
                if (butStartPos[i].equals(e.getSource())) {
                    currentStartPos = i;
                    updateStartGrid();
                }
            }
        }
    };

    private void done(ActionEvent evt) {
        player.setStartingPos(currentStartPos);
        player.setStartWidth(parseField(txtWidth));
        player.setStartOffset(parseField(txtOffset));
        player.setStartingAnyNWx(
              Math.min((Integer) spinStartingAnyNWx.getValue(), (Integer) spinStartingAnySEx.getValue()) - 1);
        player.setStartingAnyNWy(
              Math.min((Integer) spinStartingAnyNWy.getValue(), (Integer) spinStartingAnySEy.getValue()) - 1);
        player.setStartingAnySEx(
              Math.max((Integer) spinStartingAnyNWx.getValue(), (Integer) spinStartingAnySEx.getValue()) - 1);
        player.setStartingAnySEy(
              Math.max((Integer) spinStartingAnyNWy.getValue(), (Integer) spinStartingAnySEy.getValue()) - 1);
        this.setVisible(false);
    }

    private void cancel(ActionEvent evt) {
        this.setVisible(false);
    }

    /**
     * Parse the given field and return the integer it contains or 0, if the field cannot be parsed.
     */
    private int parseField(JTextField field) {
        try {
            return Integer.parseInt(field.getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}
