package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;

import megamek.client.ui.swing.UnitEditorDialog;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechSummaryCache;
import megamek.common.Player;
import megamek.common.loaders.BLKFile;
import mekhq.Utilities;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.MenuScroller;
import mekhq.gui.dialog.BombsDialog;
import mekhq.gui.dialog.CamoChoiceDialog;
import mekhq.gui.dialog.ChooseRefitDialog;
import mekhq.gui.dialog.QuirksDialog;
import mekhq.gui.dialog.TextAreaDialog;
import mekhq.gui.utilities.StaticChecks;

public class UnitTableMouseAdapter extends MouseInputAdapter implements
        ActionListener {

    private CampaignGUI gui;

    public UnitTableMouseAdapter(CampaignGUI gui) {
        super();
        this.gui = gui;
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        Unit selectedUnit = gui.getUnitModel().getUnit(gui.getUnitTable()
                .convertRowIndexToModel(gui.getUnitTable().getSelectedRow()));
        int[] rows = gui.getUnitTable().getSelectedRows();
        Unit[] units = new Unit[rows.length];
        for (int i = 0; i < rows.length; i++) {
            units[i] = gui.getUnitModel().getUnit(gui.getUnitTable()
                    .convertRowIndexToModel(rows[i]));
        }
        if (command.equalsIgnoreCase("REMOVE_PILOT")) {
            for (Unit unit : units) {
                for (Person p : unit.getCrew()) {
                    unit.remove(p, true);
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshOrganization();
            gui.refreshOverview();
        }/* else if (command.contains("QUIRK")) {
            String sel = command.split(":")[1];
                selectedUnit.acquireQuirk(sel, true);
                gui.refreshServicedUnitList();
                gui.refreshUnitList();
                gui.refreshTechsList();
                gui.refreshReport();
                gui.refreshCargo();
                gui.refreshOverview();
        }*/ else if (command.contains("MAINTENANCE_REPORT")) {
            gui.showMaintenanceReport(selectedUnit.getId());
        } else if (command.contains("ASSIGN")) {
            String sel = command.split(":")[1];
            UUID id = UUID.fromString(sel);
            Person tech = gui.getCampaign().getPerson(id);
            if (null != tech) {
                // remove any existing techs
                if (null != selectedUnit.getTech()) {
                    selectedUnit.remove(selectedUnit.getTech(), true);
                }
                selectedUnit.setTech(tech);
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshTechsList();
            gui.refreshPersonnelList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("SET_QUALITY")) {
            int q = -1;
            Object[] possibilities = { "F", "E", "D", "C", "B", "A" };
            String quality = (String) JOptionPane.showInputDialog(gui.getFrame(),
                    "Choose the new quality level", "Set Quality",
                    JOptionPane.PLAIN_MESSAGE, null, possibilities, "F");
            switch (quality) {
                case "A":
                    q = 0;
                    break;
                case "B":
                    q = 1;
                    break;
                case "C":
                    q = 2;
                    break;
                case "D":
                    q = 3;
                    break;
                case "E":
                    q = 4;
                    break;
                case "F":
                    q = 5;
                    break;
                default:
                    q = -1;
                    break;
            }
            if (q != -1) {
                for (Unit unit : units) {
                    unit.setQuality(q);
                }
            }
        } else if (command.equalsIgnoreCase("SELL")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    long sellValue = unit.getSellValue();
                    NumberFormat numberFormat = NumberFormat
                            .getNumberInstance();
                    String text = numberFormat.format(sellValue) + " "
                            + (sellValue != 0 ? "CBills" : "CBill");
                    if (0 == JOptionPane.showConfirmDialog(null,
                            "Do you really want to sell " + unit.getName()
                                    + " for " + text, "Sell Unit?",
                            JOptionPane.YES_NO_OPTION)) {
                        gui.getCampaign().sellUnit(unit.getId());
                    }
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshReport();
            gui.refreshFunds();
            gui.refreshFinancialTransactions();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("LOSS")) {
            for (Unit unit : units) {
                if (0 == JOptionPane.showConfirmDialog(null,
                        "Do you really want to consider " + unit.getName()
                                + " a combat loss?", "Remove Unit?",
                        JOptionPane.YES_NO_OPTION)) {
                    gui.getCampaign().removeUnit(unit.getId());
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshReport();
            gui.refreshOverview();
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
        } else if (command.equalsIgnoreCase("TAG_CUSTOM")) {
            String sCustomsDir = "data/mechfiles/customs/";
            String sCustomsDirCampaign = sCustomsDir
                    + gui.getCampaign().getName() + "/";
            File customsDir = new File(sCustomsDir);
            if (!customsDir.exists()) {
                customsDir.mkdir();
            }
            File customsDirCampaign = new File(sCustomsDirCampaign);
            if (!customsDirCampaign.exists()) {
                customsDir.mkdir();
            }
            for (Unit unit : units) {
                String fileName = unit.getEntity().getChassis() + " "
                        + unit.getEntity().getModel();
                try {
                    if (unit.getEntity() instanceof Mech) {
                        // if this file already exists then don't overwrite
                        // it or we will end up with a bunch of copies
                        String fileOutName = sCustomsDir + File.separator
                                + fileName + ".mtf";
                        String fileNameCampaign = sCustomsDirCampaign
                                + File.separator + fileName + ".mtf";
                        if ((new File(fileOutName)).exists()
                                || (new File(fileNameCampaign)).exists()) {
                            JOptionPane
                                    .showMessageDialog(
                                            null,
                                            "A file already exists for this unit, cannot tag as custom. (Unit name and model)",
                                            "File Already Exists",
                                            JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        FileOutputStream out = new FileOutputStream(
                                fileNameCampaign);
                        PrintStream p = new PrintStream(out);
                        p.println(((Mech) unit.getEntity()).getMtf());
                        p.close();
                        out.close();
                    } else {
                        // if this file already exists then don't overwrite
                        // it or we will end up with a bunch of copies
                        String fileOutName = sCustomsDir + File.separator
                                + fileName + ".blk";
                        String fileNameCampaign = sCustomsDirCampaign
                                + File.separator + fileName + ".blk";
                        if ((new File(fileOutName)).exists()
                                || (new File(fileNameCampaign)).exists()) {
                            JOptionPane
                                    .showMessageDialog(
                                            null,
                                            "A file already exists for this unit, cannot tag as custom. (Unit name and model)",
                                            "File Already Exists",
                                            JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        BLKFile.encode(fileNameCampaign, unit.getEntity());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                gui.getCampaign().addCustom(
                        unit.getEntity().getChassis() + " "
                                + unit.getEntity().getModel());
            }
            MechSummaryCache.getInstance().loadMechData();
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
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("DISBAND")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    if (0 == JOptionPane.showConfirmDialog(null,
                            "Do you really want to disband this unit "
                                    + unit.getName() + "?",
                            "Disband Unit?", JOptionPane.YES_NO_OPTION)) {
                        Vector<Part> parts = new Vector<Part>();
                        for (Part p : unit.getParts()) {
                            parts.add(p);
                        }
                        for (Part p : parts) {
                            p.remove(true);
                        }
                        gui.getCampaign().removeUnit(unit.getId());
                    }
                }
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPartsList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
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
        } else if (command.contains("HIRE_FULL")) {
            for (Unit unit : units) {
                gui.getCampaign().hirePersonnelFor(unit.getId());
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshPersonnelList();
            gui.refreshOrganization();
            gui.refreshFinancialTransactions();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.contains("CUSTOMIZE")
                && !command.contains("CANCEL")) {
            gui.getPanMekLab().loadUnit(selectedUnit);
            gui.getTabMain().setSelectedIndex(8);
        } else if (command.contains("CANCEL_CUSTOMIZE")) {
            if (selectedUnit.isRefitting()) {
                selectedUnit.getRefit().cancel();
            }
            gui.refreshServicedUnitList();
            gui.refreshUnitList();
            gui.refreshForceView();
            gui.refreshOrganization();
            gui.refreshPartsList();
            gui.refreshOverview();
        } else if (command.contains("REFIT_KIT")) {
            ChooseRefitDialog crd = new ChooseRefitDialog(gui.getFrame(), true,
                    gui.getCampaign(), selectedUnit, gui);
            crd.setVisible(true);
        } else if (command.contains("CHANGE_HISTORY")) {
            if (null != selectedUnit) {
                TextAreaDialog tad = new TextAreaDialog(gui.getFrame(), true,
                        "Edit Unit History", selectedUnit.getHistory());
                tad.setVisible(true);
                if (tad.wasChanged()) {
                    selectedUnit.setHistory(tad.getText());
                    gui.refreshServicedUnitList();
                    gui.refreshUnitList();
                    gui.refreshForceView();
                    gui.refreshOrganization();
                    gui.refreshOverview();
                }
            }
        } else if (command.contains("REMOVE_INDI_CAMO")) {
            selectedUnit.getEntity().setCamoCategory(null);
            selectedUnit.getEntity().setCamoFileName(null);
        } else if (command.contains("INDI_CAMO")) {
            String category = selectedUnit.getCamoCategory();
            if ("".equals(category)) {
                category = Player.ROOT_CAMO;
            }
            CamoChoiceDialog ccd = new CamoChoiceDialog(gui.getFrame(), true,
                    category, selectedUnit.getCamoFileName(), gui.getCampaign()
                            .getColorIndex(), gui.getIconPackage().getCamos());
            ccd.setLocationRelativeTo(gui.getFrame());
            ccd.setVisible(true);

            if (ccd.clickedSelect() == true) {
                selectedUnit.getEntity().setCamoCategory(ccd.getCategory());
                selectedUnit.getEntity().setCamoFileName(ccd.getFileName());

                gui.refreshForceView();
                gui.refreshUnitView();
            }
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("CANCEL_ORDER")) {
            double refund = gui.getCampaign().getCampaignOptions()
                    .GetCanceledOrderReimbursement();
            if (null != selectedUnit) {
                long refundAmount = (long) (refund * selectedUnit
                        .getBuyCost());
                gui.getCampaign().removeUnit(selectedUnit.getId());
                gui.getCampaign().getFinances().credit(refundAmount,
                        Transaction.C_EQUIP,
                        "refund for cancelled equipmemt sale",
                        gui.getCampaign().getDate());

            }
            gui.refreshFinancialTransactions();
            gui.refreshUnitList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("ARRIVE")) {
            if (null != selectedUnit) {
                selectedUnit.setDaysToArrival(0);
            }
            gui.refreshUnitList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("MOTHBALL")) {
            UUID id = null;
            if (!selectedUnit.isSelfCrewed()) {
                id = gui.selectTech(selectedUnit, "mothball");
                if (null == id) {
                    return;
                }
            }
            if (null != selectedUnit) {
                selectedUnit.startMothballing(id);
            }
            gui.refreshUnitList();
            gui.refreshServicedUnitList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("ACTIVATE")) {
            UUID id = null;
            if (!selectedUnit.isSelfCrewed()) {
                id = gui.selectTech(selectedUnit, "activation");
                if (null == id) {
                    return;
                }
            }
            if (null != selectedUnit) {
                selectedUnit.startMothballing(id);
            }
            gui.refreshUnitList();
            gui.refreshServicedUnitList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("CANCEL_MOTHBALL")) {
            if (null != selectedUnit) {
                selectedUnit.setMothballTime(0);
            }
            gui.refreshUnitList();
            gui.refreshServicedUnitList();
            gui.refreshReport();
            gui.refreshOverview();
        } else if (command.equalsIgnoreCase("BOMBS")) {
            if (null != selectedUnit
                    && selectedUnit.getEntity() instanceof Aero) {
                BombsDialog dialog = new BombsDialog(
                        (Aero) selectedUnit.getEntity(), gui.getCampaign(),
                        gui.getFrame());
                dialog.setVisible(true);
                gui.refreshUnitList();
            }
        } else if (command.equalsIgnoreCase("QUIRKS")) {
            if (null != selectedUnit) {
                QuirksDialog dialog = new QuirksDialog(
                        selectedUnit.getEntity(), gui.getFrame());
                dialog.setVisible(true);
                gui.refreshUnitList();
            }
        } else if (command.equalsIgnoreCase("EDIT_DAMAGE")) {
            if (null != selectedUnit) {
                Entity entity = selectedUnit.getEntity();
                UnitEditorDialog med = new UnitEditorDialog(gui.getFrame(), entity);
                med.setVisible(true);
                selectedUnit.runDiagnostic(false);
                gui.refreshServicedUnitList();
                gui.refreshUnitList();
                gui.refreshTaskList();
                gui.refreshUnitView();
                gui.refreshAcquireList();
                gui.refreshOrganization();
            }
        } else if (command.equalsIgnoreCase("FLUFF_NAME")) {
            if (selectedUnit != null) {
                String fluffName = (String) JOptionPane.showInputDialog(
                        gui.getFrame(), "Name for this unit?", "Unit Name",
                        JOptionPane.QUESTION_MESSAGE, null, null,
                        selectedUnit.getFluffName() == null ? ""
                                : selectedUnit.getFluffName());
                selectedUnit.setFluffName(fluffName);
                gui.refreshServicedUnitList();
                gui.refreshUnitList();
                gui.refreshTaskList();
                gui.refreshUnitView();
                gui.refreshOrganization();
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            if ((gui.getSplitUnit().getSize().width - gui.getSplitUnit().getDividerLocation() + gui.getSplitUnit()
                    .getDividerSize()) < CampaignGUI.UNIT_VIEW_WIDTH) {
                // expand
                gui.getSplitUnit().resetToPreferredSizes();
            } else {
                // collapse
                gui.getSplitUnit().setDividerLocation(1.0);
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
            if (gui.getUnitTable().getSelectedRowCount() == 0) {
                return;
            }
            int[] rows = gui.getUnitTable().getSelectedRows();
            int row = gui.getUnitTable().getSelectedRow();
            boolean oneSelected = gui.getUnitTable().getSelectedRowCount() == 1;
            Unit unit = gui.getUnitModel().getUnit(gui.getUnitTable()
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
            if (oneSelected && !unit.isPresent()) {
                menuItem = new JMenuItem("Cancel This Delivery");
                menuItem.setActionCommand("CANCEL_ORDER");
                menuItem.addActionListener(this);
                popup.add(menuItem);
                // GM mode
                menu = new JMenu("GM Mode");
                menuItem = new JMenuItem("Deliver Part Now");
                menuItem.setActionCommand("ARRIVE");
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
                popup.addSeparator();
                popup.add(menu);
                popup.show(e.getComponent(), e.getX(), e.getY());
                return;
            }
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
                    }
                    if (ammoMenu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(ammoMenu, 20);
                    }
                    menu.add(ammoMenu);
                }
                menu.setEnabled(unit.isAvailable());
                if (menu.getItemCount() > 20) {
                    MenuScroller.setScrollerFor(menu, 20);
                }
                popup.add(menu);
            }
            // Select bombs.
            if (oneSelected && (unit.getEntity() instanceof Aero)) {
                menuItem = new JMenuItem("Select Bombs");
                menuItem.setActionCommand("BOMBS");
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }
            // Salvage / Repair
            if (oneSelected
                    && !(unit.getEntity() instanceof Infantry && !(unit
                            .getEntity() instanceof BattleArmor))) {
                menu = new JMenu("Repair Status");
                menu.setEnabled(unit.isAvailable());
                cbMenuItem = new JCheckBoxMenuItem("Repair");
                if (!unit.isSalvage()) {
                    cbMenuItem.setSelected(true);
                }
                cbMenuItem.setActionCommand("REPAIR");
                cbMenuItem.addActionListener(this);
                cbMenuItem.setEnabled(unit.isAvailable()
                        && unit.isRepairable());
                menu.add(cbMenuItem);
                cbMenuItem = new JCheckBoxMenuItem("Salvage");
                if (unit.isSalvage()) {
                    cbMenuItem.setSelected(true);
                }
                cbMenuItem.setActionCommand("SALVAGE");
                cbMenuItem.addActionListener(this);
                cbMenuItem.setEnabled(unit.isAvailable());
                menu.add(cbMenuItem);
                popup.add(menu);
            }
            if (oneSelected
                    && !(unit.getEntity() instanceof Infantry && !(unit
                            .getEntity() instanceof BattleArmor))) {
                if (unit.isMothballing()) {
                    menuItem = new JMenuItem(
                            "Cancel Mothballing/Activation");
                    menuItem.setActionCommand("CANCEL_MOTHBALL");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                } else if (unit.isMothballed()) {
                    menuItem = new JMenuItem("Activate Unit");
                    menuItem.setActionCommand("ACTIVATE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(!unit.isSelfCrewed()
                            || null != unit.getEngineer());
                    popup.add(menuItem);
                } else {
                    menuItem = new JMenuItem("Mothball Unit");
                    menuItem.setActionCommand("MOTHBALL");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable()
                            && (!unit.isSelfCrewed() || null != unit
                                    .getEngineer()));
                    popup.add(menuItem);
                }
            }
            if (oneSelected && unit.requiresMaintenance()
                    && !unit.isSelfCrewed() && unit.isAvailable()) {
                menu = new JMenu("Assign Tech");
                for (Person tech : gui.getCampaign().getTechs()) {
                    if (tech.canTech(unit.getEntity())
                            && (tech.getMaintenanceTimeUsing() + unit
                                    .getMaintenanceTime()) <= 480) {
                        String skillLvl = "Unknown";
                        if (null != tech.getSkillForWorkingOn(unit)) {
                            skillLvl = SkillType
                                    .getExperienceLevelName(tech
                                            .getSkillForWorkingOn(unit)
                                            .getExperienceLevel());
                        }
                        cbMenuItem = new JCheckBoxMenuItem(
                                tech.getFullTitle() + " (" + skillLvl
                                        + ", "
                                        + tech.getMaintenanceTimeUsing()
                                        + "m)");
                        cbMenuItem.setActionCommand("ASSIGN:"
                                + tech.getId());
                        cbMenuItem.setEnabled(true);
                        if (null != unit.getTechId()
                                && unit.getTechId().equals(tech.getId())) {
                            cbMenuItem.setSelected(true);
                        } else {
                            cbMenuItem.addActionListener(this);
                        }
                        menu.add(cbMenuItem);
                    }
                }
                if (menu.getItemCount() > 0) {
                    popup.add(menu);
                    if (menu.getItemCount() > 20) {
                        MenuScroller.setScrollerFor(menu, 20);
                    }
                }
            }
            if (oneSelected && unit.requiresMaintenance()) {
                menuItem = new JMenuItem("Show Last Maintenance Report");
                menuItem.setActionCommand("MAINTENANCE_REPORT");
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }
            if (oneSelected && unit.getEntity() instanceof Infantry
                    && !(unit.getEntity() instanceof BattleArmor)) {
                menuItem = new JMenuItem("Disband");
                menuItem.setActionCommand("DISBAND");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.isAvailable());
                popup.add(menuItem);
            }
            // Customize
            if (oneSelected) {
                menu = new JMenu("Customize");
                menuItem = new JMenuItem("Choose Refit Kit...");
                menuItem.setActionCommand("REFIT_KIT");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.isAvailable()
                        && (unit.getEntity() instanceof megamek.common.Mech
                                || unit.getEntity() instanceof megamek.common.Tank
                                || unit.getEntity() instanceof megamek.common.Aero || (unit
                                    .getEntity() instanceof Infantry)));
                menu.add(menuItem);
                menuItem = new JMenuItem("Customize in Mek Lab...");
                menuItem.setActionCommand("CUSTOMIZE");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.isAvailable()
                        && (unit.getEntity() instanceof megamek.common.Mech
                                || unit.getEntity() instanceof megamek.common.Tank
                                || (unit.getEntity() instanceof megamek.common.Aero && unit
                                        .getEntity().getClass() == Aero.class) || (unit
                                    .getEntity() instanceof Infantry)));
                menu.add(menuItem);
                if (unit.isRefitting()) {
                    menuItem = new JMenuItem("Cancel Customization");
                    menuItem.setActionCommand("CANCEL_CUSTOMIZE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    menu.add(menuItem);
                }
                menu.setEnabled(unit.isAvailable() && unit.isRepairable());
                popup.add(menu);
            }
            // fill with personnel
            if (unit.getCrew().size() < unit.getFullCrewSize()) {
                menuItem = new JMenuItem("Hire full complement");
                menuItem.setActionCommand("HIRE_FULL");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.isAvailable());
                popup.add(menuItem);
            }
            // Camo
            if (oneSelected) {
                if (!unit.isEntityCamo()) {
                    menuItem = new JMenuItem(
                            gui.getResourceMap()
                                    .getString("customizeMenu.individualCamo.text"));
                    menuItem.setActionCommand("INDI_CAMO");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                } else {
                    menuItem = new JMenuItem(
                            gui.getResourceMap()
                                    .getString("customizeMenu.removeIndividualCamo.text"));
                    menuItem.setActionCommand("REMOVE_INDI_CAMO");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                }
            }
            if (oneSelected && !gui.getCampaign().isCustom(unit)) {
                menuItem = new JMenuItem("Tag as a custom unit");
                menuItem.setActionCommand("TAG_CUSTOM");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }
            if (oneSelected
                    && gui.getCampaign().getCampaignOptions().useQuirks()) {
                menuItem = new JMenuItem("Edit Quirks");
                menuItem.setActionCommand("QUIRKS");
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }
            if (oneSelected) {
                menuItem = new JMenuItem("Edit Unit History...");
                menuItem.setActionCommand("CHANGE_HISTORY");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                // remove pilot
                popup.addSeparator();
                menuItem = new JMenuItem("Remove all personnel");
                menuItem.setActionCommand("REMOVE_PILOT");
                menuItem.addActionListener(this);
                menuItem.setEnabled(!unit.isUnmanned()
                        && !unit.isDeployed());
                popup.add(menuItem);
                menuItem = new JMenuItem("Name Unit");
                menuItem.setActionCommand("FLUFF_NAME");
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }
            // sell unit
            if (gui.getCampaign().getCampaignOptions().canSellUnits()) {
                popup.addSeparator();
                menuItem = new JMenuItem("Sell Unit");
                menuItem.setActionCommand("SELL");
                menuItem.addActionListener(this);
                menuItem.setEnabled(!unit.isDeployed());
                popup.add(menuItem);
            }
            // GM mode
            menu = new JMenu("GM Mode");
            menuItem = new JMenuItem("Remove Unit");
            menuItem.setActionCommand("REMOVE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            menuItem = new JMenuItem("Undeploy Unit");
            menuItem.setActionCommand("UNDEPLOY");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM() && unit.isDeployed());
            menu.add(menuItem);
            menuItem = new JMenuItem("Edit Damage...");
            menuItem.setActionCommand("EDIT_DAMAGE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            menuItem = new JMenuItem("Set Quality...");
            menuItem.setActionCommand("SET_QUALITY");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            popup.addSeparator();
            popup.add(menu);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
