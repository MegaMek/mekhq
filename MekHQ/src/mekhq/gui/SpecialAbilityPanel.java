/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import mekhq.MekHQ;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.gui.dialog.EditSpecialAbilityDialog;
import mekhq.gui.panes.CampaignOptionsPane;

/**
 * An extension of JPanel that displays information about special abilities
 * 
 * @author Jay Lawson
 */
public class SpecialAbilityPanel extends JPanel {
    private SpecialAbility abil;
    private JButton btnRemove;
    private JButton btnEdit;
    private final CampaignOptionsPane cop;

    public SpecialAbilityPanel(SpecialAbility a, CampaignOptionsPane cop) {
        this.abil = a;
        this.cop = cop;

        btnEdit = new JButton("Edit");
        btnRemove = new JButton("Remove");

        btnEdit.addActionListener(evt -> editSPA());

        btnRemove.addActionListener(evt -> remove());

        setLayout(new GridBagLayout());

        initComponents();
    }

    private void initComponents() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.SpecialAbilityPanel",
                MekHQ.getMHQOptions().getLocale());

        GridBagConstraints c = new GridBagConstraints();

        JTextArea txtDesc = new JTextArea(abil.getDescription());
        txtDesc.setEditable(false);
        txtDesc.setBackground(this.getBackground());

        JPanel pnlButton = new JPanel(new GridLayout(0, 2));
        pnlButton.add(btnEdit);
        pnlButton.add(btnRemove);

        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0.0;
        c.gridwidth = 4;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHWEST;
        add(pnlButton, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weighty = 0.0;
        c.gridwidth = 4;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        add(txtDesc, c);

        c.gridx = 0;
        c.gridy = 2;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        add(new JLabel("<html><b>XP Cost</b></html>"), c);
        c.gridx = 0;
        c.gridy = 3;
        c.weighty = 1.0;
        add(new JLabel(Integer.toString(abil.getCost())), c);

        c.gridx = 1;
        c.gridy = 2;
        c.weighty = 0.0;
        JLabel lblPrereq = new JLabel("<html><b>Prerequisites</b></html>");
        lblPrereq.setToolTipText(resourceMap.getString("lblPrereq.toolTipText"));
        add(lblPrereq, c);
        c.gridx = 1;
        c.gridy = 3;
        c.weighty = 1.0;
        add(new JLabel("<html>" + abil.getAllPrereqDesc() + "</html>"), c);

        c.gridx = 2;
        c.gridy = 2;
        c.weighty = 0.0;
        JLabel lblIncompatible = new JLabel("<html><b>Incompatible</b></html>");
        lblIncompatible.setToolTipText(resourceMap.getString("lblIncompatible.toolTipText"));
        add(lblIncompatible, c);
        c.gridx = 2;
        c.gridy = 3;
        c.weighty = 1.0;
        add(new JLabel("<html>" + abil.getInvalidDesc() + "</html>"), c);

        c.gridx = 3;
        c.gridy = 2;
        c.weighty = 0.0;
        JLabel lblRemove = new JLabel("<html><b>Removes</b></html>");
        lblRemove.setToolTipText(resourceMap.getString("lblRemove.toolTipText"));
        add(lblRemove, c);
        c.gridx = 3;
        c.gridy = 3;
        c.weighty = 1.0;
        add(new JLabel("<html>" + abil.getRemovedDesc() + "</html>"), c);

        this.setBorder(BorderFactory.createTitledBorder(abil.getDisplayName()));
    }

    private void remove() {
        cop.btnRemoveSPA(abil.getName());
    }

    private void editSPA() {
        EditSpecialAbilityDialog esad = new EditSpecialAbilityDialog(null, abil, cop.getCurrentSPA());
        esad.setVisible(true);
        this.removeAll();
        initComponents();
        this.revalidate();
        this.repaint();
    }
}
