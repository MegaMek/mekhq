/*
 * DragoonsRatingDialog.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import mekhq.campaign.Campaign;
import mekhq.campaign.rating.IUnitRating;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 3/15/2012
 */
public class UnitRatingDialog extends JDialog implements ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 5644262476831277348L;

    private static final String TITLE = "Dragoon's Rating";

    private JButton okayButton;
    private JButton aboutButton;
    private Campaign campaign;
    private IUnitRating rating;

    public UnitRatingDialog(Frame parent, boolean modal, Campaign campaign) {
        super(parent, TITLE, modal);
        this.campaign = campaign;
        initGUI();
    }

    private void initGUI() {
        getDragoonsRating();

        setLayout(new BorderLayout());
        add(getButtonPanel(), BorderLayout.SOUTH);
        add(getMainPanel(), BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(getParent());
        setResizable(false);
    }

    private JPanel getMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JTextArea descriptionArea = new JTextArea(20, 60);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        descriptionArea.setText(rating.getDetails());
        descriptionArea.setEditable(false);

        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        panel.add(descriptionScroll, BorderLayout.CENTER);
        descriptionArea.setCaretPosition(0);
        return panel;
    }

    private void getDragoonsRating() {
        rating = campaign.getUnitRating();
    }

    private JPanel getButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));

        okayButton = new JButton("OK");
        okayButton.setMnemonic('o');
        okayButton.addActionListener(this);
        panel.add(okayButton);

        aboutButton = new JButton("?");
        aboutButton.setMnemonic('?');
        aboutButton.addActionListener(this);
        panel.add(aboutButton);

        return panel;
    }

    public void actionPerformed(ActionEvent e) {
        if (okayButton.equals(e.getSource())) {
            this.setVisible(false);
        }
        if (aboutButton.equals(e.getSource())) {
            JOptionPane.showMessageDialog(this, rating.getHelpText(), "About Dragoons Rating", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
