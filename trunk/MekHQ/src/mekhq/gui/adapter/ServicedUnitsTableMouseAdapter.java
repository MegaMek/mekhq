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

import megamek.common.AmmoType;
import mekhq.Utilities;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.MenuScroller;
import mekhq.gui.utilities.StaticChecks;

public class ServicedUnitsTableMouseAdapter extends MouseInputAdapter
        implements ActionListener {
    private CampaignGUI gui;

    public ServicedUnitsTableMouseAdapter(CampaignGUI gui) {
        super();
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        @SuppressWarnings("unused")
        Unit selectedUnit = gui.getServicedUnitModel()
                .getUnit(gui.getServicedUnitTable()
                        .convertRowIndexToModel(gui.getServicedUnitTable()
                                .getSelectedRow()));
        int[] rows = gui.getServicedUnitTable().getSelectedRows();
        Unit[] units = new Unit[rows.length];
        for (int i = 0; i < rows.length; i++) {
            units[i] = gui.getServicedUnitModel().getUnit(gui.getServicedUnitTable()
                    .convertRowIndexToModel(rows[i]));
        }
        if (command.contains("ASSIGN_TECH")) {
            /*
             * String sel = command.split(":")[1]; int selected =
             * Integer.parseInt(sel); if ((selected > -1) && (selected <
             * gui.getCampaign().getTechTeams().size())) { SupportTeam team =
             * gui.getCampaign().getTechTeams().get(selected); if (null != team)
             * { for (WorkItem task : gui.getCampaign()
             * .getTasksForUnit(selectedUnit.getId())) { if
             * (team.getTargetFor(task).getValue() != TargetRoll.IMPOSSIBLE)
             * { gui.getCampaign().processTask(task, team); } } } }
             * gui.refreshServicedUnitList(); gui.refreshUnitList();
             * gui.refreshTaskList(); gui.refreshAcquireList(); gui.refreshTechsList();
             * gui.refreshReport(); gui.refreshPartsList(); gui.refreshOverview();
             */
        } else if (command.contains("SWAP_AMMO")) {
            String sel = command.split(":")[1];
            int selAmmoId = Integer.parseInt(sel);
            Part part = gui.getCampaign().getPart(selAmmoId);
            if (null == part || !(part instanceof AmmoBin)) {
                return;
            }
            AmmoBin ammo = (AmmoBin) part;
            sel = command.split(":")[2];
            long munition = Long.parseLong(sel);
            ammo.changeMunition(munition);
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOverview();
            gui.filterTasks();
        } else if (command.contains("CHANGE_SITE")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    String sel = command.split(":")[1];
                    int selected = Integer.parseInt(sel);
                    if ((selected > -1) && (selected < Unit.SITE_N)) {
                        unit.setSite(selected);
                    }
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshTaskList();
            gui.refreshAcquireList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("SALVAGE")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    unit.setSalvage(true);
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("REPAIR")) {
            for (Unit unit : units) {
                if (!unit.isDeployed() && unit.isRepairable()) {
                    unit.setSalvage(false);
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("REMOVE")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    if (0 == JOptionPane.showConfirmDialog(
                            null,
                            "Do you really want to remove "
                                    + unit.getName() + "?", "Remove Unit?",
                            JOptionPane.YES_NO_OPTION)) {
                        gui.getCampaign().removeUnit(unit.getId());
                    }
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("UNDEPLOY")) {
            for (Unit unit : units) {
                if (unit.isDeployed()) {
                    gui.undeployUnit(unit);
                }
            }
            gui.refreshPersonnelList();
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOrganization();
            gui.refreshTaskList();
            gui.refreshUnitView();
            gui.refreshPartsList();
            gui.refreshAcquireList();
            gui.refreshReport();
            gui.refreshPatientList();
            gui.refreshScenarioList();
            gui.refreshOverview();
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
            if (gui.getServicedUnitTable().getSelectedRowCount() == 0) {
                return;
            }
            int[] rows = gui.getServicedUnitTable().getSelectedRows();
            int row = gui.getServicedUnitTable().getSelectedRow();
            boolean oneSelected = gui.getServicedUnitTable().getSelectedRowCount() == 1;
            Unit unit = gui.getServicedUnitModel().getUnit(gui.getServicedUnitTable()
                    .convertRowIndexToModel(row));
            Unit[] units = new Unit[rows.length];
            for (int i = 0; i < rows.length; i++) {
                units[i] = gui.getUnitModel().getUnit(gui.getUnitTable()
                        .convertRowIndexToModel(rows[i]));
            }
            JMenuItem menuItem = null;
            JMenu menu = null;
            JCheckBoxMenuItem cbMenuItem = null;
            // **lets fill the pop up menu**//
            // change the location
            menu = new JMenu("Change site");
            int i = 0;
            for (i = 0; i < Unit.SITE_N; i++) {
                cbMenuItem = new JCheckBoxMenuItem(Unit.getSiteName(i));
                if (StaticChecks.areAllSameSite(units) && unit.getSite() == i) {
                    cbMenuItem.setSelected(true);
                } else {
                    cbMenuItem.setActionCommand("CHANGE_SITE:" + i);
                    cbMenuItem.addActionListener(this);
                }
                menu.add(cbMenuItem);
            }
            menu.setEnabled(unit.isAvailable());
            popup.add(menu);
            // assign all tasks to a certain tech
            /*
             * menu = new JMenu("Assign all tasks"); i = 0; for (Person tech
             * : gui.getCampaign().getTechs()) { menuItem = new
             * JMenuItem(tech.getFullName());
             * menuItem.setActionCommand("ASSIGN_TECH:" + i);
             * menuItem.addActionListener(this);
             * menuItem.setEnabled(tech.getMinutesLeft() > 0);
             * menu.add(menuItem); i++; }
             * menu.setEnabled(unit.isAvailable()); if (menu.getItemCount()
             * > 20) { MenuScroller.setScrollerFor(menu, 20); }
             * popup.add(menu);
             */
            // swap ammo
            if (oneSelected) {
                menu = new JMenu("Swap ammo");
                JMenu ammoMenu = null;
                for (AmmoBin ammo : unit.getWorkingAmmoBins()) {
                    ammoMenu = new JMenu(ammo.getType().getDesc());
                    AmmoType curType = (AmmoType) ammo.getType();
                    for (AmmoType atype : Utilities.getMunitionsFor(unit
                            .getEntity(), curType, gui.getCampaign()
                            .getCampaignOptions().getTechLevel())) {
                        cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
                        if (atype.equals(curType)) {
                            cbMenuItem.setSelected(true);
                        } else {
                            cbMenuItem.setActionCommand("SWAP_AMMO:"
                                    + ammo.getId() + ":"
                                    + atype.getMunitionType());
                            cbMenuItem.addActionListener(this);
                        }
                        ammoMenu.add(cbMenuItem);
                        i++;
                    }
                    if (menu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(menu, 20);
                    }
                    menu.add(ammoMenu);
                }
                menu.setEnabled(unit.isAvailable());
                popup.add(menu);
                // Salvage / Repair
                if (unit.isSalvage()) {
                    menuItem = new JMenuItem("Repair");
                    menuItem.setActionCommand("REPAIR");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable()
                            && unit.isRepairable());
                    popup.add(menuItem);
                } else {
                    menuItem = new JMenuItem("Salvage");
                    menuItem.setActionCommand("SALVAGE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable());
                    popup.add(menuItem);
                }
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}