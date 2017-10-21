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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.swing.BombChoicePanel;
import megamek.common.BombType;
import megamek.common.EquipmentType;
import megamek.common.IBomber;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;

/**
 * @author Deric Page (dericpage@users.sourceforge.net)
 * @version %I% %G%
 * @since 4/7/2012
 * Modified 8/27/2017 by Joshua Bartz <jbartz at sbcglobal.net>
 */
public class BombsDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -8333650539542692225L;
    private BombChoicePanel bombPanel;
    private IBomber bomber;
    private Campaign campaign;
    
    private int[] bombChoices;
    private int[] bombCatalog = new int[BombType.B_NUM];
    private int[] availBombs = new int[BombType.B_NUM];
    private int[] typeMax = new int[BombType.B_NUM];

    private JButton okayButton;
    private JButton cancelButton;

    public BombsDialog(IBomber iBomber, Campaign campaign, JFrame parent) {
        super(parent, "Select Bombs", true);
        this.bomber = iBomber;
        this.campaign = campaign;
        bombChoices = bomber.getBombChoices();
        
        initGUI();
        validate();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initGUI() {
        //Using bombCatalog to store the part ID's of the bombs so don't have to keep full spare list in memory
        //and for ease of access later
        List<Part> spareParts = campaign.getSpareParts();
        for(Part spare : spareParts) {
            if(spare instanceof AmmoStorage && ((EquipmentPart)spare).getType() instanceof BombType && spare.isPresent()) {
                int bombType = (BombType.getBombTypeFromInternalName(((AmmoStorage)spare).getType().getInternalName()));
                bombCatalog[bombType] = spare.getId();
                availBombs[bombType] = ((AmmoStorage)spare).getShots();
            }
        }
        
        for (int type = 0; type < BombType.B_NUM; type++) {
            typeMax[type] = availBombs[type] + bombChoices[type];
        }
        
        bombPanel = new BombChoicePanel(bomber, campaign.getGameOptions().booleanOption("at2_nukes"),
                campaign.getGameOptions().booleanOption("allow_advanced_ammo"), typeMax);

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
            int[] newLoadout = bombPanel.getChoice();
            
            //Get difference between starting bomb load and new bomb load
            for (int type = 0; type < BombType.B_NUM; type++) {
                if(bombChoices[type] != newLoadout[type]) {
                    newLoadout[type] = bombChoices[type] - newLoadout[type];
                } else {
                    newLoadout[type] = 0;
                }
            }
            
            for (int type = 0; type < BombType.B_NUM; type++) {
                if (newLoadout[type] != 0) {
                    //IF there are bombs of this TYPE in the warehouse
                    if (bombCatalog[type] > 0) {
                        AmmoStorage storedBombs = (AmmoStorage) campaign.getPart(bombCatalog[type]);
                        storedBombs.changeShots(newLoadout[type]);
                        if(storedBombs.getShots() == 0) {
                            campaign.removePart(storedBombs);
                        }
                    //No bombs of this type in warehouse, add bombs
                    //In this case newLoadout should always be greater than 0, but check to be sure
                    } else if (bombCatalog[type] == 0 && newLoadout[type] > 0) {
                        AmmoStorage excessBombs = new AmmoStorage(0, EquipmentType.get(BombType.getBombInternalName(type)), newLoadout[type], campaign);
                        campaign.addPart(excessBombs, 0);
                    }
                }
            }
            
            setVisible(false);
        } else if (cancelButton.equals(e.getSource())) {
            setVisible(false);
        }
    }

}
