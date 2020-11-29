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
import mekhq.campaign.work.IPartWork;
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
        IPartWork partWork = taskModel.getTaskAt(taskTable.convertRowIndexToModel(taskTable.getSelectedRow()));
        if (null == partWork) {
            return;
        }

        int[] rows = taskTable.getSelectedRows();
        IPartWork[] parts = new IPartWork[rows.length];
        for (int i = 0; i < rows.length; i++) {
            parts[i] = taskModel.getTaskAt(taskTable.convertRowIndexToModel(rows[i]));
        }

        if (command.equalsIgnoreCase("SCRAP")) {
            for (IPartWork p : parts) {
            	if (!(p instanceof Part)) {
            		continue;
		        }
                if (null != ((Part)p).checkScrappable()) {
                    JOptionPane.showMessageDialog(gui.getFrame(), ((Part)p).checkScrappable(), "Cannot scrap part",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Unit u = p.getUnit();
                gui.getCampaign().addReport(((Part)p).scrap());
                if (null != u && !u.isRepairable() && !u.hasSalvageableParts()) {
                    gui.getCampaign().removeUnit(u.getId());
                }
                MekHQ.triggerEvent(new UnitChangedEvent(u));
            }
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
            for (IPartWork p : parts) {
                if (p instanceof Part && p.getAllMods(null).getValue() != TargetRoll.AUTOMATIC_SUCCESS) {
                    ((Part)p).setMode(WorkTime.of(sel));
                    MekHQ.triggerEvent(new PartModeChangedEvent((Part)p));
                }
            }
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
             * getCampaign().getQuartermaster().addPart(part); } task.succeed(); if
             * (task.isCompleted()) { getCampaign().removeTask(task); } }
             * refreshServicedUnitList(); refreshUnitList();
             * refreshTaskList(); refreshAcquireList(); refreshPartsList();
             * refreshCargo(); refreshOverview(); }
             */
            if (partWork.checkFixable() == null) {
                for (IPartWork p : parts) {
                    gui.getCampaign().addReport(p.succeed());
                    if (p.getUnit() != null) {
                        p.getUnit().refreshPodSpace();
                    }
                    //PodSpace triggers event for each child part
                    if (p instanceof Part) {
                        MekHQ.triggerEvent(new PartChangedEvent((Part)p));
                    }
                }
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
            IPartWork partWork = taskModel.getTaskAt(taskTable.convertRowIndexToModel(row));
            if (null == partWork) {
                return;
            }

            int[] rows = taskTable.getSelectedRows();
            IPartWork[] parts = new IPartWork[rows.length];
            for (int i = 0; i < rows.length; i++) {
                parts[i] = taskModel.getTaskAt(taskTable.convertRowIndexToModel(rows[i]));
            }

            JMenuItem menuItem = null;
            JMenu menu = null;
            JCheckBoxMenuItem cbMenuItem = null;
            // Mode (extra time, rush job, ...
            // dont allow automatic success jobs to change mode
            // dont allow pod space or pod-mounted equipment to change mode when removing or replacing
            boolean canChangeMode = true;
            boolean isScrappable = true;
            boolean isBeingWorked = false;
            boolean isFixable = true;
            for (IPartWork p : parts) {
                canChangeMode &= p.canChangeWorkMode()
                        && p.getAllMods(null).getValue() != TargetRoll.AUTOMATIC_SUCCESS;
                isScrappable &= (p instanceof Part) && !((Part)p).canNeverScrap();
                isBeingWorked |= (p instanceof Part) && ((Part)p).isBeingWorkedOn();
                isFixable &= (null == p.checkFixable());
            }
            if (canChangeMode) {
                menu = new JMenu("Mode");
                for (WorkTime wt : WorkTime.DEFAULT_TIMES) {
                    cbMenuItem = new JCheckBoxMenuItem(wt.name);
                    if (partWork.getMode() == wt) {
                        cbMenuItem.setSelected(true);
                    } else {
                        cbMenuItem.setActionCommand("CHANGE_MODE:" + wt.id);
                        cbMenuItem.addActionListener(this);
                    }
                    cbMenuItem.setEnabled(!isBeingWorked);
                    menu.add(cbMenuItem);
                }
                popup.add(menu);
            }
            // Scrap component
            if (isScrappable) {
                menuItem = new JMenuItem("Scrap component");
                menuItem.setActionCommand("SCRAP");
                menuItem.addActionListener(this);
                menuItem.setEnabled(!isBeingWorked);
                popup.add(menuItem);
            }

            menu = new JMenu("GM Mode");
            popup.add(menu);
            // Auto complete task

            menuItem = new JMenuItem("Complete Task");
            menuItem.setActionCommand("FIX");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM() && isFixable);
            menu.add(menuItem);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
