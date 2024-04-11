/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.Mission;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

public class CustomizeBotForceDialog  extends JDialog {

    private BotForce botForce;
    private Campaign campaign;

    //gui components
    private JTextField txtName;
    private JComboBox<String> choiceTeam;
    private JButton btnClose;
    private JButton btnOK;

    public CustomizeBotForceDialog(JFrame parent, boolean modal, BotForce bf, Campaign c) {
        super(parent, modal);
        if (null == bf) {
            botForce = new BotForce();
            botForce.setName("New Bot Force");
            // assume enemy by default
            botForce.setTeam(2);
        } else {
            botForce = bf;
        }
        campaign = c;
        initComponents();
        setLocationRelativeTo(parent);
        pack();
    }

    private void initComponents() {

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizeBotForceDialog",
                MekHQ.getMHQOptions().getLocale());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(resourceMap.getString("title"));

        getContentPane().setLayout(new BorderLayout());
        JPanel panMain = new JPanel(new GridLayout(0, 2));
        JPanel panLeft = new JPanel(new GridBagLayout());
        JPanel panRight = new JPanel(new GridBagLayout());
        panMain.add(panLeft);
        panMain.add(panRight);
        getContentPane().add(panMain, BorderLayout.CENTER);

        JPanel panButtons = new JPanel(new GridLayout(0, 2));
        btnOK = new JButton(resourceMap.getString("btnOK.text"));
        btnOK.addActionListener(this::done);
        btnClose = new JButton(resourceMap.getString("btnClose.text"));
        btnClose.addActionListener(this::cancel);
        panButtons.add(btnOK);
        panButtons.add(btnClose);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);

        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        panLeft.add(new JLabel(resourceMap.getString("lblName.text")), gbc);

        txtName = new JTextField(botForce.getName());
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panLeft.add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panLeft.add(new JLabel(resourceMap.getString("lblTeam.text")), gbc);

        choiceTeam = new JComboBox();
        for (int i = 1; i < 6; i++) {
            String choice = resourceMap.getString("choiceTeam.text") + " " + i;
            if (i ==1) {
                choice = choice + " (" + resourceMap.getString("choiceAllied.text") + ")";
            }
            choiceTeam.addItem(choice);
        }
        choiceTeam.setSelectedIndex(botForce.getTeam() - 1);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panLeft.add(choiceTeam, gbc);

    }

    public BotForce getBotForce() {
        return botForce;
    }

    private void done(ActionEvent evt) {
        botForce.setName(txtName.getText());
        botForce.setTeam(choiceTeam.getSelectedIndex()+1);
        this.setVisible(false);
    }
    private void cancel(ActionEvent evt) {
        botForce = null;
        this.setVisible(false);
    }

}
