package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;

import megamek.common.TargetRoll;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import mekhq.gui.RepairTab;

public class TaskTableMouseAdapter extends MouseInputAdapter implements ActionListener {
    
    private RepairTab repairTab;

    public TaskTableMouseAdapter(RepairTab repairTab) {
        super();
        this.repairTab = repairTab;
    }        

    @Override
    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        Part part = repairTab.getTaskModel().getTaskAt(repairTab.getTaskTable().convertRowIndexToModel(repairTab.getTaskTable().getSelectedRow()));
        if (null == part) {
            return;
        }
        if (command.equalsIgnoreCase("SCRAP")) {
            if (null != part.checkScrappable()) {
                JOptionPane.showMessageDialog(repairTab.getFrame(), part.checkScrappable(), "Cannot scrap part",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            Unit u = part.getUnit();
            repairTab.getCampaign().addReport(part.scrap());
            if (null != u && !u.isRepairable() && u.getSalvageableParts().size() == 0) {
                repairTab.getCampaign().removeUnit(u.getId());
            }
            repairTab.refreshServicedUnitList();
            repairTab.getCampaignGui().refreshUnitList();
            repairTab.refreshTaskList();
            repairTab.getCampaignGui().refreshUnitView();
            repairTab.getCampaignGui().refreshPartsList();
            repairTab.refreshAcquireList();
            repairTab.getCampaignGui().refreshReport();
            repairTab.getCampaignGui().refreshOverview();
            repairTab.filterTasks();
        } else if (command.contains("SWAP_AMMO")) {
            /*
             * WorkItem task =
             * repairTab.getTaskModel().getTaskAt(repairTab.getTaskTable().getSelectedRow()); if (task
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
            repairTab.refreshServicedUnitList();
            repairTab.getCampaignGui().refreshUnitList();
            repairTab.refreshTaskList();
            repairTab.getCampaignGui().refreshUnitView();
            repairTab.refreshAcquireList();
            repairTab.getCampaignGui().refreshOverview();
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
                repairTab.getCampaign().addReport(part.succeed());

                repairTab.refreshServicedUnitList();
                repairTab.getCampaignGui().refreshUnitList();
                repairTab.refreshTaskList();
                repairTab.refreshAcquireList();
                repairTab.getCampaignGui().refreshPartsList();
                repairTab.getCampaignGui().refreshOverview();
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
            int row = repairTab.getTaskTable().getSelectedRow();
            if (row < 0) {
                return;
            }
            Part part = repairTab.getTaskModel().getTaskAt(repairTab.getTaskTable().convertRowIndexToModel(row));
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
            menuItem.setEnabled(repairTab.getCampaign().isGM() && (null == part.checkFixable()));
            menu.add(menuItem);

            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
