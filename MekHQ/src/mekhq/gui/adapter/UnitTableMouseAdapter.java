package mekhq.gui.adapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;

import megamek.client.ui.swing.UnitEditorDialog;
import megamek.client.ui.swing.util.MenuScroller;
import megamek.common.*;
import megamek.common.loaders.BLKFile;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.event.RepairStatusChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.MissingThrusters;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.StripUnitAction;
import mekhq.campaign.unit.actions.ActivateUnitAction;
import mekhq.campaign.unit.actions.CancelMothballUnitAction;
import mekhq.campaign.unit.actions.IUnitAction;
import mekhq.campaign.unit.actions.MothballUnitAction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;
import mekhq.gui.MekLabTab;
import mekhq.gui.dialog.BombsDialog;
import mekhq.gui.dialog.CamoChoiceDialog;
import mekhq.gui.dialog.ChooseRefitDialog;
import mekhq.gui.dialog.LargeCraftAmmoSwapDialog;
import mekhq.gui.dialog.MarkdownEditorDialog;
import mekhq.gui.dialog.QuirksDialog;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.utilities.StaticChecks;

public class UnitTableMouseAdapter extends MouseInputAdapter implements
        ActionListener {

    private CampaignGUI gui;
    private JTable unitTable;
    private UnitTableModel unitModel;
    private ResourceBundle resourceMap;

    public final static String COMMAND_MOTHBALL = "MOTHBALL";
    public final static String COMMAND_ACTIVATE = "ACTIVATE";
    public final static String COMMAND_CANCEL_MOTHBALL = "CANCEL_MOTHBALL";
    public final static String COMMAND_GM_MOTHBALL = "GM_MOTHBALL";
    public final static String COMMAND_GM_ACTIVATE = "GM_ACTIVATE";

    public UnitTableMouseAdapter(CampaignGUI gui, JTable unitTable,
            UnitTableModel unitModel) {
        super();
        this.gui = gui;
        this.unitTable = unitTable;
        this.unitModel = unitModel;
        resourceMap = ResourceBundle.getBundle("mekhq.resources.UnitTableMouseAdapter", new EncodeControl()); //$NON-NLS-1$
    }

    public void actionPerformed(ActionEvent action) {
        String command = action.getActionCommand();
        Unit selectedUnit = unitModel.getUnit(unitTable
                .convertRowIndexToModel(unitTable.getSelectedRow()));
        int[] rows = unitTable.getSelectedRows();
        Unit[] units = new Unit[rows.length];
        for (int i = 0; i < rows.length; i++) {
            units[i] = unitModel.getUnit(unitTable
                    .convertRowIndexToModel(rows[i]));
        }
        if (command.equalsIgnoreCase("REMOVE_ALL_PERSONNEL")) {
            for (Unit unit : units) {
                if (unit.isDeployed()) {
                    continue;
                }

                for (Person p : unit.getCrew()) {
                    unit.remove(p, true);
                }

                unit.removeTech();

                Person engineer = unit.getEngineer();

                if (null != engineer) {
                    unit.remove(engineer, true);
                }
            }
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
        } else if (command.contains("SUPPLY_COST")) {
            gui.showUnitCostReport(selectedUnit.getId());
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
                    Money sellValue = unit.getSellValue();
                    String text = sellValue.toAmountAndSymbolString();
                    if (0 == JOptionPane.showConfirmDialog(null,
                            "Do you really want to sell " + unit.getName()
                                    + " for " + text, "Sell Unit?",
                            JOptionPane.YES_NO_OPTION)) {
                        gui.getCampaign().sellUnit(unit.getId());
                    }
                }
            }
        } else if (command.equalsIgnoreCase("LOSS")) {
            for (Unit unit : units) {
                if (0 == JOptionPane.showConfirmDialog(null,
                        "Do you really want to consider " + unit.getName()
                                + " a combat loss?", "Remove Unit?",
                        JOptionPane.YES_NO_OPTION)) {
                    gui.getCampaign().removeUnit(unit.getId());
                }
            }
        } else if (command.contains("LC_SWAP_AMMO")) {
            LargeCraftAmmoSwapDialog dialog = new LargeCraftAmmoSwapDialog(gui.getFrame(), selectedUnit);
            dialog.setVisible(true);
            if (!dialog.wasCanceled()) {
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.contains("SWAP_AMMO")) {
            String[] fields = command.split(":");
            int selAmmoId = Integer.parseInt(fields[1]);
            Part part = gui.getCampaign().getPart(selAmmoId);
            if (null == part || !(part instanceof AmmoBin)) {
                return;
            }
            AmmoBin ammo = (AmmoBin) part;
            AmmoType atype = (AmmoType) EquipmentType.get(fields[2]);
            ammo.changeMunition(atype);
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
                } catch (Exception e) {
                    MekHQ.getLogger().error(getClass(), "actionPerformed", e);
                }
                gui.getCampaign().addCustom(
                        unit.getEntity().getChassis() + " "
                                + unit.getEntity().getModel());
            }
            MechSummaryCache.getInstance().loadMechData();
        } else if (command.equalsIgnoreCase("REMOVE")) {
            List<Unit> toRemove = new ArrayList<>();
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    toRemove.add(unit);
                }
            }
            if (toRemove.size() > 0) {
                String title = String.format(resourceMap.getString("deleteUnitsCount.text"), toRemove.size()); //$NON-NLS-1$
                if (toRemove.size() == 1) {
                    title = toRemove.get(0).getName();
                }
                if (0 == JOptionPane.showConfirmDialog(
                        null,
                        String.format(resourceMap.getString("confirmRemove.format"), title), //$NON-NLS-1$
                        resourceMap.getString("removeQ.text"), //$NON-NLS-1$
                        JOptionPane.YES_NO_OPTION)) {
                    for (Unit unit : toRemove) {
                        gui.getCampaign().removeUnit(unit.getId());
                    }
                }
            }
        } else if (command.equalsIgnoreCase("DISBAND")) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    if (0 == JOptionPane.showConfirmDialog(null,
                            "Do you really want to disband this unit "
                                    + unit.getName() + "?",
                            "Disband Unit?", JOptionPane.YES_NO_OPTION)) {
                        Vector<Part> parts = new Vector<>();
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
        } else if (command.equalsIgnoreCase("UNDEPLOY")) {
            for (Unit unit : units) {
                if (unit.isDeployed()) {
                    gui.undeployUnit(unit);
                    //Event triggered from undeployUnit
                }
            }
        } else if (command.contains("HIRE_FULL")) {
            boolean isGM = command.contains("_GM");
            for (Unit unit : units) {
                gui.getCampaign().hirePersonnelFor(unit.getId(), isGM);
            }
        } else if (command.contains("CUSTOMIZE")
                && !command.contains("CANCEL")) {
            if (gui.hasTab(GuiTabType.MEKLAB)) {
                ((MekLabTab)gui.getTab(GuiTabType.MEKLAB))
                    .loadUnit(selectedUnit);
            }
            gui.getTabMain().setSelectedIndex(8);
        } else if (command.contains("CANCEL_CUSTOMIZE")) {
            if (selectedUnit.isRefitting()) {
                selectedUnit.getRefit().cancel();
            }
        } else if (command.contains("REFIT_GM_COMPLETE")) {
            if (selectedUnit.isRefitting()) {
                gui.getCampaign().addReport(selectedUnit.getRefit().succeed());
            }
        } else if (command.contains("REFURBISH")) {
            Refit r = new Refit(selectedUnit, selectedUnit.getEntity(),false, true);
            gui.refitUnit(r, false);
        } else if (command.contains("REFIT_KIT")) {
            ChooseRefitDialog crd = new ChooseRefitDialog(gui.getFrame(), true,
                    gui.getCampaign(), selectedUnit, gui);
            crd.setVisible(true);
        } else if (command.contains("CHANGE_HISTORY")) {
            if (null != selectedUnit) {
                MarkdownEditorDialog tad = new MarkdownEditorDialog(gui.getFrame(), true,
                        "Edit Unit History", selectedUnit.getHistory());
                tad.setVisible(true);
                if (tad.wasChanged()) {
                    selectedUnit.setHistory(tad.getText());
                    MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
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
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equalsIgnoreCase("CANCEL_ORDER")) {
            double refund = gui.getCampaign().getCampaignOptions()
                    .GetCanceledOrderReimbursement();
            if (null != selectedUnit) {
                Money refundAmount = selectedUnit.getBuyCost().multipliedBy(refund);
                gui.getCampaign().removeUnit(selectedUnit.getId());
                gui.getCampaign().getFinances().credit(
                        refundAmount,
                        Transaction.C_EQUIP,
                        "refund for cancelled equipment sale",
                        gui.getCampaign().getDate());
            }
        } else if (command.equalsIgnoreCase("ARRIVE")) {
            if (null != selectedUnit) {
                selectedUnit.setDaysToArrival(0);
            }
        } else if (command.equalsIgnoreCase(COMMAND_MOTHBALL)) {
            if(units.length > 1) {
                gui.showMassMothballDialog(units, false);
                return;
            }

            if (null != selectedUnit) {
                UUID techId = pickTechForMothballOrActivation(selectedUnit);
                MothballUnitAction mothballUnitAction = new MothballUnitAction(techId, false);
                mothballUnitAction.Execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equalsIgnoreCase(COMMAND_ACTIVATE)) {
            if(units.length > 1) {
                gui.showMassMothballDialog(units, true);
                return;
            }

            if (null != selectedUnit) {
                UUID techId = pickTechForMothballOrActivation(selectedUnit);
                ActivateUnitAction activateUnitAction = new ActivateUnitAction(techId, false);
                activateUnitAction.Execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equalsIgnoreCase(COMMAND_CANCEL_MOTHBALL)) {
            if (null != selectedUnit) {
                CancelMothballUnitAction cancelAction = new CancelMothballUnitAction();
                cancelAction.Execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equalsIgnoreCase("BOMBS")) {
            if (null != selectedUnit
                    && selectedUnit.getEntity().isBomber()) {
                BombsDialog dialog = new BombsDialog(
                        (IBomber)selectedUnit.getEntity(), gui.getCampaign(),
                        gui.getFrame());
                dialog.setVisible(true);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equalsIgnoreCase("QUIRKS")) {
            if (null != selectedUnit) {
                QuirksDialog dialog = new QuirksDialog(
                        selectedUnit.getEntity(), gui.getFrame());
                dialog.setVisible(true);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equalsIgnoreCase("EDIT_DAMAGE")) {
            if (null != selectedUnit) {
                Entity entity = selectedUnit.getEntity();
                UnitEditorDialog med = new UnitEditorDialog(gui.getFrame(), entity);
                med.setVisible(true);
                selectedUnit.runDiagnostic(false);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equalsIgnoreCase("FLUFF_NAME")) {
            if (selectedUnit != null) {
                String fluffName = (String) JOptionPane.showInputDialog(
                        gui.getFrame(), "Name for this unit?", "Unit Name",
                        JOptionPane.QUESTION_MESSAGE, null, null,
                        selectedUnit.getFluffName() == null ? ""
                                : selectedUnit.getFluffName());
                selectedUnit.setFluffName(fluffName);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if(command.equalsIgnoreCase("RESTORE_UNIT")) {
            for (Unit unit : units) {
                unit.setSalvage(false);

                boolean needsCheck = true;
                while(unit.isAvailable() && needsCheck) {
                    needsCheck = false;
                    for(int x = 0; x < unit.getParts().size(); x++) {
                        Part part = unit.getParts().get(x);
                        if (part instanceof MissingPart) {
                            //Make sure we restore both left and right thrusters
                            if (part instanceof MissingThrusters) {
                                if (((Aero)unit.getEntity()).getLeftThrustHits() > 0) {
                                    ((MissingThrusters)part).setLeftThrusters(true);
                                }
                            }
                            // We magically acquire a replacement part, then fix the missing one.
                            part.getCampaign().addPart(((MissingPart) part).getNewPart(), 0);
                            part.fix();
                            part.resetTimeSpent();
                            part.resetOvertime();
                            part.setTeamId(null);
                            part.cancelReservation();
                            part.remove(false);
                            needsCheck = true;
                        } else {
                            if(part.needsFixing()) {
                                needsCheck = true;
                                part.fix();
                            } else {
                                part.resetRepairSettings();
                            }
                            part.resetTimeSpent();
                            part.resetOvertime();
                            part.setTeamId(null);
                            part.cancelReservation();
                        }

                        // replace damaged armor and reload ammo bins after fixing their respective locations
                        if(part instanceof Armor) {
                            final Armor armor = (Armor) part;
                            armor.setAmount(armor.getTotalAmount());
                        } else if(part instanceof AmmoBin) {
                            final AmmoBin ammoBin = (AmmoBin) part;

                            // we magically find the ammo we need, then load the bin
                            // we only want to get the amount of ammo the bin actually needs
                            if(ammoBin.getShotsNeeded() > 0) {
                                ammoBin.setShotsNeeded(0);
                                ammoBin.updateConditionFromPart();
                            }
                        }

                    }

                    // TODO: Make this less painful. We just want to fix hips and shoulders.
                    Entity entity = unit.getEntity();
                    if(entity instanceof Mech) {
                        for(int loc : new int[]{
                            Mech.LOC_CLEG, Mech.LOC_LLEG, Mech.LOC_RLEG, Mech.LOC_LARM, Mech.LOC_RARM}) {
                            int numberOfCriticals = entity.getNumberOfCriticals(loc);
                            for(int crit = 0; crit < numberOfCriticals; ++ crit) {
                                CriticalSlot slot = entity.getCritical(loc, crit);
                                if(null != slot) {
                                    slot.setHit(false);
                                    slot.setDestroyed(false);
                                }
                            }
                        }
                    }
                }
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        }
        else if (command.equalsIgnoreCase("STRIP_UNIT")) {
            IUnitAction stripUnitAction = new StripUnitAction();
            for (Unit unit : units) {
                stripUnitAction.Execute(gui.getCampaign(), unit);
            }
        } else if (command.equalsIgnoreCase(COMMAND_GM_MOTHBALL)) {
            if (null != selectedUnit) {
                MothballUnitAction mothballUnitAction = new MothballUnitAction(null, true);
                mothballUnitAction.Execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equalsIgnoreCase(COMMAND_GM_ACTIVATE)) {
            if (null != selectedUnit) {
                ActivateUnitAction activateUnitAction = new ActivateUnitAction(null, true);
                activateUnitAction.Execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        }
    }

    private UUID pickTechForMothballOrActivation(Unit unit) {
        UUID id = null;
        if (!unit.isSelfCrewed()) {
            id = gui.selectTech(unit, "activation", true);
            if (null != id) {
                Person tech = gui.getCampaign().getPerson(id);
                if (tech.getTechUnitIDs().size() > 0) {
                    if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(gui.getFrame(),
                            tech.getName() + " will not be able to perform maintenance on "
                                    + tech.getTechUnitIDs().size() + " assigned units. Proceed?",
                                    "Unmaintained unit warning",
                                    JOptionPane.YES_NO_OPTION)) {
                        id = null;
                    }
                }
            }
        }
        return id;
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
            if (unitTable.getSelectedRowCount() == 0) {
                return;
            }
            int[] rows = unitTable.getSelectedRows();
            int row = unitTable.getSelectedRow();
            boolean oneSelected = unitTable.getSelectedRowCount() == 1;
            Unit unit = unitModel.getUnit(unitTable
                    .convertRowIndexToModel(row));
            Unit[] units = new Unit[rows.length];
            for (int i = 0; i < rows.length; i++) {
                units[i] = unitModel.getUnit(unitTable
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
            int i;
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
                            if (atype == curType) {
                                cbMenuItem.setSelected(true);
                            } else {
                                cbMenuItem.setActionCommand("SWAP_AMMO:"
                                        + ammo.getId() + ":"
                                        + atype.getInternalName());
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
            }
            // Select bombs.
            if (oneSelected && (unit.getEntity().isBomber())) {
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
                    menuItem.setActionCommand(COMMAND_CANCEL_MOTHBALL);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                } else if (unit.isMothballed()) {
                    menuItem = new JMenuItem("Activate Unit");
                    menuItem.setActionCommand(COMMAND_ACTIVATE);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                } else {
                    menuItem = new JMenuItem("Mothball Unit");
                    menuItem.setActionCommand(COMMAND_MOTHBALL);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable()
                            && (!unit.isSelfCrewed() || null != unit
                                    .getEngineer()));
                    popup.add(menuItem);
                }
            }

            if(unitTable.getSelectedRowCount() > 1) {
                boolean allMothballed = true;
                boolean allAvailable = true;

                for(int x = 0; x < units.length; x++) {
                    // infantry, jumpships and warships cannot be mothballed
                    if(units[x].isSelfCrewed()) {
                        allMothballed = false;
                        allAvailable = false;
                        break;
                    }

                    if(!units[x].isMothballed()) {
                        allMothballed = false;
                    }

                    if(!units[x].isAvailable()) {
                        allAvailable = false;
                    }
                }

                if(allAvailable) {
                    menuItem = new JMenuItem("Mass Mothball");
                    menuItem.setActionCommand(COMMAND_MOTHBALL);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                } else if(allMothballed) {
                    menuItem = new JMenuItem("Mass Activate");
                    menuItem.setActionCommand(COMMAND_ACTIVATE);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }
            }

            if (oneSelected && unit.requiresMaintenance()
                    && !unit.isSelfCrewed() && unit.isAvailable()) {
                menu = new JMenu("Assign Tech");
                JMenu menuElite = new JMenu(SkillType.ELITE_NM);
                JMenu menuVeteran = new JMenu(SkillType.VETERAN_NM);
                JMenu menuRegular = new JMenu(SkillType.REGULAR_NM);
                JMenu menuGreen = new JMenu(SkillType.GREEN_NM);
                JMenu menuUltraGreen = new JMenu(SkillType.ULTRA_GREEN_NM);

                int techsFound = 0;
                for (Person tech : gui.getCampaign().getTechs()) {
                    if (tech.canTech(unit.getEntity())
                            && (tech.getMaintenanceTimeUsing()
                                    + unit.getMaintenanceTime()) <= 480) {
                        String skillLvl = "Unknown";
                        if (null != tech.getSkillForWorkingOn(unit)) {
                            skillLvl = SkillType.getExperienceLevelName(
                                tech.getSkillForWorkingOn(unit)
                                    .getExperienceLevel());
                        }

                        cbMenuItem = new JCheckBoxMenuItem(
                                tech.getFullTitle()
                                    + " ("
                                    + tech.getMaintenanceTimeUsing()
                                    + "m)");
                        cbMenuItem.setActionCommand("ASSIGN:" + tech.getId());
                        cbMenuItem.setEnabled(true);
                        if (null != unit.getTechId()
                                && unit.getTechId().equals(tech.getId())) {
                            cbMenuItem.setSelected(true);
                        } else {
                            cbMenuItem.addActionListener(this);
                        }

                        JMenu subMenu = null;
                        switch (skillLvl) {
                        case SkillType.ELITE_NM:
                            subMenu = menuElite;
                            break;
                        case SkillType.VETERAN_NM:
                            subMenu = menuVeteran;
                            break;
                        case SkillType.REGULAR_NM:
                            subMenu = menuRegular;
                            break;
                        case SkillType.GREEN_NM:
                            subMenu = menuGreen;
                            break;
                        case SkillType.ULTRA_GREEN_NM:
                            subMenu = menuUltraGreen;
                            break;
                        }
                        if (subMenu != null) {
                            subMenu.add(cbMenuItem);
                            if (cbMenuItem.isSelected()) {
                                subMenu.setIcon(UIManager.getIcon("CheckBoxMenuItem.checkIcon"));
                            }
                            techsFound++;
                        }
                    }
                }
                if (techsFound > 0) {
                    addMenuIfNonEmpty(menu, menuElite, 20);
                    addMenuIfNonEmpty(menu, menuVeteran, 20);
                    addMenuIfNonEmpty(menu, menuRegular, 20);
                    addMenuIfNonEmpty(menu, menuGreen, 20);
                    addMenuIfNonEmpty(menu, menuUltraGreen, 20);

                    popup.add(menu);
                }
            }
            if (oneSelected && unit.requiresMaintenance()) {
                menuItem = new JMenuItem("Show Last Maintenance Report");
                menuItem.setActionCommand("MAINTENANCE_REPORT");
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }
            if (oneSelected && !unit.isMothballed() && gui.getCampaign().getCampaignOptions().usePeacetimeCost()) {
                menuItem = new JMenuItem("Show Monthly Supply Cost Report");
                menuItem.setActionCommand("SUPPLY_COST");
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
                if (unit.getEntity().isOmni()) {
                    menuItem = new JMenuItem("Choose configuration...");
                } else {
                    menuItem = new JMenuItem("Choose Refit Kit...");
                }
                menuItem.setActionCommand("REFIT_KIT");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.isAvailable()
                        && (unit.getEntity() instanceof megamek.common.Mech
                                || unit.getEntity() instanceof megamek.common.Tank
                                || unit.getEntity() instanceof megamek.common.Aero || (unit
                                    .getEntity() instanceof Infantry)));
                menu.add(menuItem);
                menuItem = new JMenuItem("Refurbish Unit");
                menuItem.setActionCommand("REFURBISH");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.isAvailable()
                        && (unit.getEntity() instanceof megamek.common.Mech
                                || unit.getEntity() instanceof megamek.common.Tank
                                || unit.getEntity() instanceof megamek.common.Aero
                                || unit.getEntity() instanceof BattleArmor
                                || unit.getEntity() instanceof megamek.common.Protomech));
                menu.add(menuItem);
                if (gui.hasTab(GuiTabType.MEKLAB)) {
                    menuItem = new JMenuItem("Customize in Mek Lab...");
                    menuItem.setActionCommand("CUSTOMIZE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable()
                            && !(unit.getEntity() instanceof GunEmplacement));
                    menu.add(menuItem);
                }
                if (unit.isRefitting()) {
                    menuItem = new JMenuItem("Cancel Customization");
                    menuItem.setActionCommand("CANCEL_CUSTOMIZE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Complete Refit (GM)");
                    menuItem.setActionCommand("REFIT_GM_COMPLETE");
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(gui.getCampaign().isGM() && unit.isRefitting());
                    menu.add(menuItem);
                }
                menu.setEnabled(unit.isAvailable(true) && unit.isRepairable());
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
            }

            // remove all personnel
            popup.addSeparator();
            menuItem = new JMenuItem("Remove all personnel");
            menuItem.setActionCommand("REMOVE_ALL_PERSONNEL");
            menuItem.addActionListener(this);
            menuItem.setEnabled(!(unit.isUnmanned() && (null == unit.getTech()))
                    && !unit.isDeployed());
            popup.add(menuItem);

            if (oneSelected) {
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
            menuItem = new JMenuItem("Strip Unit");
            menuItem.setActionCommand("STRIP_UNIT");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            if (oneSelected) {
                menuItem = new JMenuItem(unit.isMothballed() ? "Activate Unit" : "Mothball Unit");
                menuItem.setActionCommand(unit.isMothballed() ? COMMAND_GM_ACTIVATE : COMMAND_GM_MOTHBALL);
                menuItem.addActionListener(this);
                menuItem.setEnabled(gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            menuItem = new JMenuItem("Undeploy Unit");
            menuItem.setActionCommand("UNDEPLOY");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM() && unit.isDeployed());
            menu.add(menuItem);
            if (unit.getCrew().size() < unit.getFullCrewSize()) {
                menuItem = new JMenuItem("Add full complement");
                menuItem.setActionCommand("HIRE_FULL_GM");
                menuItem.addActionListener(this);
                menuItem.setEnabled(unit.isAvailable() && gui.getCampaign().isGM());
                menu.add(menuItem);
            }
            menuItem = new JMenuItem("Edit Damage...");
            menuItem.setActionCommand("EDIT_DAMAGE");
            menuItem.addActionListener(this);
            menuItem.setEnabled(gui.getCampaign().isGM());
            menu.add(menuItem);
            menuItem = new JMenuItem("Restore Unit");
            menuItem.setActionCommand("RESTORE_UNIT");
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

    private static void addMenuIfNonEmpty(JMenu menu, JMenu child, int scrollerThreshold) {
        if (child.getItemCount() > 0) {
            menu.add(child);
            if (child.getItemCount() > scrollerThreshold) {
                MenuScroller.setScrollerFor(child, scrollerThreshold);
            }
        }
    }
}
