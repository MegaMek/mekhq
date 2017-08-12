/*
 * BombsDialog.java
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.swing.BombChoicePanel;
import megamek.common.IBomber;
import mekhq.campaign.Campaign;

/**
 * @author Deric Page (dericpage@users.sourceforge.net)
 * @version %I% %G%
 * @since 4/7/2012
 */
public class BombsDialog extends JDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -8333650539542692225L;
    private BombChoicePanel bombPanel;
    private IBomber bomber;
    private Campaign campaign;

    private JButton okayButton;
    private JButton cancelButton;

    public BombsDialog(IBomber iBomber, Campaign campaign, JFrame parent) {
        super(parent, "Select Bombs", true);
        this.bomber = iBomber;
        this.campaign = campaign;
        initGUI();
        validate();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initGUI() {

        bombPanel = new BombChoicePanel(bomber, campaign.getGameOptions().booleanOption("at2_nukes"),
                campaign.getGameOptions().booleanOption("allow_advanced_ammo"));

        //Set up the display of this dialog.
        JScrollPane scroller = new JScrollPane(bombPanel);
        scroller.setPreferredSize(new Dimension(300, 200));
        setLayout(new BorderLayout());
        add(scroller, BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

        okayButton = new JButton("Okay");
        okayButton.setMnemonic('o');
        okayButton.addActionListener(this);
        panel.add(okayButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setMnemonic('c');
        cancelButton.addActionListener(this);
        panel.add(cancelButton);

        return panel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (okayButton.equals(e.getSource())) {
            bombPanel.applyChoice();
            setVisible(false);
        } else if (cancelButton.equals(e.getSource())) {
            setVisible(false);
        }
    }

}
