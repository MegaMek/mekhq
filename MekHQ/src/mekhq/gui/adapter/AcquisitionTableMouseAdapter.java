/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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

package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.AcquisitionTableModel;

public class AcquisitionTableMouseAdapter extends MouseInputAdapter implements ActionListener {
    
    private CampaignGUI gui;
    private JTable acquisitionTable;
    private AcquisitionTableModel acquireModel;
    
    public AcquisitionTableMouseAdapter(CampaignGUI gui, JTable acquisitionTable,
            AcquisitionTableModel acquireModel) {
        super();
        this.gui = gui;
        this.acquisitionTable = acquisitionTable;
        this.acquireModel = acquireModel;
    }
    
    @Override
    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        IAcquisitionWork acquisitionWork = acquireModel
                .getAcquisitionAt(acquisitionTable.convertRowIndexToModel(acquisitionTable.getSelectedRow()));
        if (acquisitionWork instanceof AmmoBin) {
            acquisitionWork = ((AmmoBin) acquisitionWork).getAcquisitionWork();
        }
        if (null == acquisitionWork) {
            return;
        }
        if (command.contains("FIX")) {
            gui.getCampaign().addReport(acquisitionWork.find(0));

            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshPartsList();
            gui.refreshOverview();
            gui.filterTasks();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        maybeShowPopup(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        maybeShowPopup(e);
    }

    private void maybeShowPopup(MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        if (e.isPopupTrigger()) {
            int row = acquisitionTable.getSelectedRow();
            if (row < 0) {
                return;
            }
            JMenuItem menuItem = null;
            JMenu menu = null;
            menu = new JMenu("GM Mode");
            popup.add(menu);
            // Auto complete task

            menuItem = new JMenuItem("Complete Task");
            menuItem.setActionCommand("FIX");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
