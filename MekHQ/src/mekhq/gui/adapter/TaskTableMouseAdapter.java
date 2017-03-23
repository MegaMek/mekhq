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

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

import megamek.common.TargetRoll;
import mekhq.MekHQ;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.PartModeChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.CampaignGUI;
import mekhq.gui.model.TaskTableModel;

public class TaskTableMouseAdapter extends MouseInputAdapter implements ActionListener {
    
    private CampaignGUI gui;
    private JTable taskTable;
    private TaskTableModel taskModel;

    public TaskTableMouseAdapter(CampaignGUI gui, JTable taskTable,
            TaskTableModel taskModel) {
        super();
        this.gui = gui;
        this.taskTable = taskTable;
        this.taskModel = taskModel;
    }        

    @Override
    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        Part part = (Part)taskModel.getTaskAt(taskTable.convertRowIndexToModel(taskTable.getSelectedRow()));
        if (null == part) {
            return;
        }
        if (command.equalsIgnoreCase("SCRAP")) {
            if (null != part.checkScrappable()) {
                JOptionPane.showMessageDialog(gui.getFrame(), part.checkScrappable(), "Cannot scrap part",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            Unit u = part.getUnit();
            gui.getCampaign().addReport(part.scrap());
            u.refreshPodSpace();
            if (null != u && !u.isRepairable() && u.getSalvageableParts().size() == 0) {
                gui.getCampaign().removeUnit(u.getId());
            }
            MekHQ.triggerEvent(new UnitChangedEvent(u));
        } else if (command.contains("SWAP_AMMO")) {
            /*
             * WorkItem task =
             * taskModel.getTaskAt(taskTable.getSelectedRow()); if (task
             * instanceof ReloadItem) { ReloadItem reload = (ReloadItem)
             * task; Entity en = reload.getUnit().getEntity(); Mounted m =
             * reload.getMounted(); if (null == m) { return; } AmmoType
             * curType = (AmmoType) m.getType(); String sel =
             * command.split(":")[1]; int selType = Integer.parseInt(sel);
             * AmmoType newType = Utilities.getMunitionsFor(en, curType)
             * .get(selType); reload.swapAmmo(newType); refreshTaskList();
             * refreshUnitView(); refreshAcquireList(); }
             */
        } else if (command.contains("CHANGE_MODE")) {
            String sel = command.split(":")[1];
            part.setMode(WorkTime.of(sel));
            MekHQ.triggerEvent(new PartModeChangedEvent(part));
        } else if (command.contains("UNASSIGN")) {
            /*
             * for (WorkItem task : tasks) { task.resetTimeSpent();
             * task.setTeam(null); refreshServicedUnitList();
             * refreshUnitList(); refreshTaskList(); refreshAcquireList();
             * refreshCargo(); refreshOverview(); }
             */
        } else if (command.contains("FIX")) {
            /*
             * for (WorkItem task : tasks) { if (task.checkFixable() ==
             * null) { if ((task instanceof ReplacementItem) &&
             * !((ReplacementItem) task).hasPart()) { ReplacementItem
             * replace = (ReplacementItem) task; Part part =
             * replace.partNeeded(); replace.setPart(part);
             * getCampaign().addPart(part); } task.succeed(); if
             * (task.isCompleted()) { getCampaign().removeTask(task); } }
             * refreshServicedUnitList(); refreshUnitList();
             * refreshTaskList(); refreshAcquireList(); refreshPartsList();
             * refreshCargo(); refreshOverview(); }
             */
            if (part.checkFixable() == null) {
                gui.getCampaign().addReport(part.succeed());
                if (part.getUnit() != null) {
                    part.getUnit().refreshPodSpace();
                }
                MekHQ.triggerEvent(new PartChangedEvent(part));
            }
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
            int row = taskTable.getSelectedRow();
            if (row < 0) {
                return;
            }
            Part part = (Part)taskModel.getTaskAt(taskTable.convertRowIndexToModel(row));
            JMenuItem menuItem = null;
            JMenu menu = null;
            JCheckBoxMenuItem cbMenuItem = null;
            // Mode (extra time, rush job, ...
            // dont allow automatic success jobs to change mode
            if (part.getAllMods(null).getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                menu = new JMenu("Mode");
                for (WorkTime wt : WorkTime.DEFAULT_TIMES) {
                    cbMenuItem = new JCheckBoxMenuItem(wt.name);
                    if (part.getMode() == wt) {
                        cbMenuItem.setSelected(true);
                    } else {
                        cbMenuItem.setActionCommand("CHANGE_MODE:" + wt.id);
                        cbMenuItem.addActionListener(this);
                    }
                    cbMenuItem.setEnabled(!part.isBeingWorkedOn());
                    menu.add(cbMenuItem);
                }
                popup.add(menu);
            }
            // Scrap component
            if (!part.canNeverScrap()) {
                menuItem = new JMenuItem("Scrap component");
                menuItem.setActionCommand("SCRAP");
                menuItem.addActionListener(this);
                menuItem.setEnabled(!part.isBeingWorkedOn());
                popup.add(menuItem);
            }

            menu = new JMenu("GM Mode");
            popup.add(menu);
            // Auto complete task

            menuItem = new JMenuItem("Complete Task");
            menuItem.setActionCommand("FIX");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM() && (null == part.checkFixable()));
            menu.add(menuItem);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
