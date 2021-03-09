/*
 * Copyright (c) 2017-2021 - The MegaMek Team. All rights reserved.
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
package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.util.Optional;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;

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

public class TaskTableMouseAdapter extends JPopupMenuAdapter {
    //region Variable Declarations
    private CampaignGUI gui;
    private JTable taskTable;
    private TaskTableModel taskModel;
    //endregion Variable Declaration

    //region Constructors
    protected TaskTableMouseAdapter(CampaignGUI gui, JTable taskTable, TaskTableModel taskModel) {
        this.gui = gui;
        this.taskTable = taskTable;
        this.taskModel = taskModel;
    }
    //endregion Constructors

    public static void connect(CampaignGUI gui, JTable taskTable, TaskTableModel taskModel) {
        new TaskTableMouseAdapter(gui, taskTable, taskModel).connect(taskTable);
    }

    @Override
    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        IPartWork partWork = taskModel.getTaskAt(taskTable.convertRowIndexToModel(taskTable.getSelectedRow()));
        if (partWork == null) {
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

                if (((Part) p).checkScrappable() != null) {
                    JOptionPane.showMessageDialog(gui.getFrame(), ((Part) p).checkScrappable(), "Cannot scrap part",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Unit u = p.getUnit();
                gui.getCampaign().addReport(((Part) p).scrap());
                if ((u != null) && !u.isRepairable() && !u.hasSalvageableParts()) {
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
                if ((p instanceof Part) && (p.getAllMods(null).getValue() != TargetRoll.AUTOMATIC_SUCCESS)) {
                    ((Part) p).setMode(WorkTime.of(sel));
                    MekHQ.triggerEvent(new PartModeChangedEvent((Part) p));
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
                    gui.getCampaign().addReport(String.format("GM Repair, %s %s", p.getPartName(), p.succeed()));
                    if (p.getUnit() != null) {
                        p.getUnit().refreshPodSpace();
                    }
                    //PodSpace triggers event for each child part
                    if (p instanceof Part) {
                        MekHQ.triggerEvent(new PartChangedEvent((Part) p));
                    }
                }
            }
        }
    }


    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        int row = taskTable.getSelectedRow();
        if (row < 0) {
            return Optional.empty();
        }

        IPartWork partWork = taskModel.getTaskAt(taskTable.convertRowIndexToModel(row));
        if (partWork == null) {
            return Optional.empty();
        }

        JPopupMenu popup = new JPopupMenu();

        int[] rows = taskTable.getSelectedRows();
        IPartWork[] parts = new IPartWork[rows.length];
        for (int i = 0; i < rows.length; i++) {
            parts[i] = taskModel.getTaskAt(taskTable.convertRowIndexToModel(rows[i]));
        }

        JMenuItem menuItem;
        JMenu menu;
        JCheckBoxMenuItem cbMenuItem;
        // Mode (extra time, rush job, ...)
        // don't allow automatic success jobs to change mode
        // don't allow pod space or pod-mounted equipment to change mode when removing or replacing
        boolean canChangeMode = true;
        boolean isScrappable = true;
        boolean isBeingWorked = false;
        boolean isFixable = true;
        for (IPartWork p : parts) {
            canChangeMode &= p.canChangeWorkMode()
                    && p.getAllMods(null).getValue() != TargetRoll.AUTOMATIC_SUCCESS;
            isScrappable &= (p instanceof Part) && !((Part) p).canNeverScrap();
            isBeingWorked |= (p instanceof Part) && p.isBeingWorkedOn();
            isFixable &= (p.checkFixable() == null);
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

        if (gui.getCampaign().isGM()) {
            popup.addSeparator();
            menu = new JMenu("GM Mode");

            // Auto complete task
            menuItem = new JMenuItem("Complete Task");
            menuItem.setActionCommand("FIX");
            menuItem.addActionListener(this);
            menuItem.setEnabled(isFixable);
            menu.add(menuItem);

            popup.add(menu);
        }

        return Optional.of(popup);
    }
}
