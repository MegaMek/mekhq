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

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.event.RepairStatusChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.dialog.LargeCraftAmmoSwapDialog;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.utilities.MenuScroller;
import mekhq.gui.utilities.StaticChecks;
import mekhq.service.MassRepairService;

public class ServicedUnitsTableMouseAdapter extends MouseInputAdapter
        implements ActionListener {
    
    private CampaignGUI gui;
    private JTable servicedUnitTable;
    private UnitTableModel servicedUnitModel;

    public ServicedUnitsTableMouseAdapter(CampaignGUI gui, JTable servicedUnitTable,
            UnitTableModel servicedUnitModel) {
        super();
        this.gui = gui;
        this.servicedUnitTable = servicedUnitTable;
        this.servicedUnitModel = servicedUnitModel;
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        @SuppressWarnings("unused")
        Unit selectedUnit = servicedUnitModel
                .getUnit(servicedUnitTable
                        .convertRowIndexToModel(servicedUnitTable
                                .getSelectedRow()));
        int[] rows = servicedUnitTable.getSelectedRows();
        Unit[] units = new Unit[rows.length];
        for (int i = 0; i < rows.length; i++) {
            units[i] = servicedUnitModel.getUnit(servicedUnitTable
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
        } else if (command.contains("LC_SWAP_AMMO")) {
            LargeCraftAmmoSwapDialog dialog = new LargeCraftAmmoSwapDialog(gui.getFrame(), selectedUnit);
            dialog.setVisible(true);
            if (!dialog.wasCanceled()) {
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.contains("SWAP_AMMO")) {
            String sel = command.split(":")[1];
            int selAmmoId = Integer.parseInt(sel);
            Part part = gui.getCampaign().getPart(selAmmoId);
            if (null == part || !(part instanceof AmmoBin)) {
                return;
            }
            AmmoBin ammo = (AmmoBin) part;
            sel = command.split(":")[2];
            EquipmentType etype = EquipmentType.get(sel);
            ammo.changeMunition(etype);
            MekHQ.triggerEvent(new UnitChangedEvent(part.getUnit()));
        } else if (command.contains("CHANGE_SITE")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    String sel = command.split(":")[1];
                    int selected = Integer.parseInt(sel);
                    if ((selected > -1) && (selected < Unit.SITE_N)) {
                        unit.setSite(selected);
                        MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
                    }
                }
            }
        } else if (command.equalsIgnoreCase("SALVAGE")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    unit.setSalvage(true);
                    MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
                }
            }
        } else if (command.equalsIgnoreCase("REPAIR")) {
            for (Unit unit : units) {
                if (!unit.isDeployed() && unit.isRepairable()) {
                    unit.setSalvage(false);
                    MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
                }
            }
        } else if (command.contains("MASS_REPAIR_SALVAGE")) {
            Unit unit = null;
            
            if ((null != units) && (units.length > 0)) {
            	unit = units[0];
            }
            
            if (unit.isDeployed()) {
    			JOptionPane.showMessageDialog(gui.getFrame(),
    					"Unit is currently deployed and can not be repaired.",
    					"Unit is deployed", JOptionPane.ERROR_MESSAGE);
            } else {
				MassRepairService.performSingleUnitMassRepairOrSalvage(gui, unit);
				MekHQ.triggerEvent(new UnitChangedEvent(unit));
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
            if (servicedUnitTable.getSelectedRowCount() == 0) {
                return;
            }
            int[] rows = servicedUnitTable.getSelectedRows();
            int row = servicedUnitTable.getSelectedRow();
            boolean oneSelected = servicedUnitTable.getSelectedRowCount() == 1;
            Unit unit = servicedUnitModel.getUnit(servicedUnitTable
                    .convertRowIndexToModel(row));
            Unit[] units = new Unit[rows.length];
            for (int i = 0; i < rows.length; i++) {
                units[i] = servicedUnitModel.getUnit(servicedUnitTable
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
                if (unit.getEntity().usesWeaponBays()) {
                    menuItem = new JMenuItem("Swap ammo...");
                    menuItem.setActionCommand("LC_SWAP_AMMO");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                } else {
                    menu = new JMenu("Swap ammo");
                    JMenu ammoMenu = null;
                    for (AmmoBin ammo : unit.getWorkingAmmoBins()) {
                        ammoMenu = new JMenu(ammo.getType().getDesc());
                        AmmoType curType = (AmmoType) ammo.getType();
                        for (AmmoType atype : Utilities.getMunitionsFor(unit
                                .getEntity(), curType, gui.getCampaign()
                                .getCampaignOptions().getTechLevel())) {
                            cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
                            if (atype.equals(curType)
                            		&& atype.getMunitionType() == curType.getMunitionType()) {
                                cbMenuItem.setSelected(true);
                            } else {
                                cbMenuItem.setActionCommand("SWAP_AMMO:"
                                        + ammo.getId() + ":"
                                        + atype.getInternalName());
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
                
                if (!unit.isSelfCrewed() && unit.isAvailable() && !unit.isDeployed()) {
                	String title = String.format("Mass %s", unit.isSalvage() ? "Salvage" : "Repair");
                	
                    menuItem = new JMenuItem(title);
                    menuItem.setActionCommand("MASS_REPAIR_SALVAGE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable());
                    popup.add(menuItem);
                }
                
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
    
}