/*
 * Copyright (c) 2014, 2020 The MegaMek Team. All rights reserved.
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
import megamek.common.util.StringUtil;
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
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.gui.utilities.StaticChecks;

public class UnitTableMouseAdapter extends MouseInputAdapter implements ActionListener {
    //region Variable Declarations
    private CampaignGUI gui;
    private JTable unitTable;
    private UnitTableModel unitModel;
    private ResourceBundle resourceMap = ResourceBundle
            .getBundle("mekhq.resources.UnitTableMouseAdapter", new EncodeControl());

    //region Commands
    //region Standard Commands
    //region Undelivered Unit Commands
    public static final String COMMAND_CANCEL_ORDER = "CANCEL_ORDER";
    public static final String COMMAND_ARRIVE = "ARRIVE";
    //endregion Undelivered Unit Commands

    public static final String COMMAND_CHANGE_SITE = "CHANGE_SITE";
    // Ammo Swap Commands
    public static final String COMMAND_LC_SWAP_AMMO = "LC_SWAP_AMMO";
    public static final String COMMAND_SWAP_AMMO = "SWAP_AMMO";
    // Repair Commands
    public static final String COMMAND_REPAIR = "REPAIR";
    public static final String COMMAND_SALVAGE = "SALVAGE";
    // Mothball Commands
    public static final String COMMAND_MOTHBALL = "MOTHBALL";
    public static final String COMMAND_ACTIVATE = "ACTIVATE";
    public static final String COMMAND_CANCEL_MOTHBALL = "CANCEL_MOTHBALL";
    // Assign Tech Commands
    public static final String COMMAND_ASSIGN_TECH = "ASSIGN";
    // Unit History Commands
    public static final String COMMAND_CHANGE_HISTORY = "CHANGE_HISTORY";
    // Remove All Personnel Commands
    public static final String COMMAND_REMOVE_ALL_PERSONNEL = "REMOVE_ALL_PERSONNEL";

    public static final String COMMAND_HIRE_FULL = "HIRE_FULL";
    public static final String COMMAND_DISBAND = "DISBAND";
    public static final String COMMAND_SELL = "SELL";
    public static final String COMMAND_LOSS = "LOSS";
    public static final String COMMAND_MAINTENANCE_REPORT = "MAINTENANCE_REPORT";
    public static final String COMMAND_QUIRKS = "QUIRKS";
    public static final String COMMAND_BOMBS = "BOMBS";
    public static final String COMMAND_SUPPLY_COST = "SUPPLY_COST";
    public static final String COMMAND_TAG_CUSTOM = "TAG_CUSTOM";
    public static final String COMMAND_INDI_CAMO = "INDI_CAMO";
    public static final String COMMAND_REMOVE_INDI_CAMO = "REMOVE_INDI_CAMO";
    public static final String COMMAND_CUSTOMIZE = "CUSTOMIZE";
    public static final String COMMAND_CANCEL_CUSTOMIZE = "CANCEL_CUSTOMIZE";
    public static final String COMMAND_REFIT_GM_COMPLETE = "REFIT_GM_COMPLETE";
    public static final String COMMAND_REFURBISH = "REFURBISH";
    public static final String COMMAND_REFIT_KIT = "REFIT_KIT";
    public static final String COMMAND_FLUFF_NAME = "FLUFF_NAME";
    //endregion Standard Commands

    //region GM Commands
    public static final String COMMAND_GM = "_GM"; // do NOT use as a command, just to create commands
    public static final String COMMAND_REMOVE = "REMOVE";
    public static final String COMMAND_STRIP_UNIT = "STRIP_UNIT";
    public static final String COMMAND_GM_MOTHBALL = COMMAND_MOTHBALL + COMMAND_GM;
    public static final String COMMAND_GM_ACTIVATE = COMMAND_ACTIVATE + COMMAND_GM;
    public static final String COMMAND_UNDEPLOY = "UNDEPLOY";
    public static final String COMMAND_HIRE_FULL_GM = COMMAND_HIRE_FULL + COMMAND_GM;
    public static final String COMMAND_EDIT_DAMAGE = "EDIT_DAMAGE";
    public static final String COMMAND_RESTORE_UNIT = "RESTORE_UNIT";
    public static final String COMMAND_SET_QUALITY = "SET_QUALITY";
    //endregion GM Commands
    //endregion Commands
    //endregion Variable Declarations

    public UnitTableMouseAdapter(CampaignGUI gui, JTable unitTable, UnitTableModel unitModel) {
        super();
        this.gui = gui;
        this.unitTable = unitTable;
        this.unitModel = unitModel;
    }

    public void actionPerformed(ActionEvent action) {
        // First make sure we actually get a row of data
        int[] rows = unitTable.getSelectedRows();
        if (rows.length < 1) { // if we don't, return
            return;
        }

        String command = action.getActionCommand();

        Unit[] units = new Unit[rows.length];
        for (int i = 0; i < rows.length; i++) {
            units[i] = unitModel.getUnit(unitTable.convertRowIndexToModel(rows[i]));
        }
        Unit selectedUnit = units[0];

        if (command.equals(COMMAND_REMOVE_ALL_PERSONNEL)) {
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
        } else if (command.equals(COMMAND_MAINTENANCE_REPORT)) { // Single Unit only
            gui.showMaintenanceReport(selectedUnit.getId());
        } else if (command.equals(COMMAND_SUPPLY_COST)) { // Single Unit only
            gui.showUnitCostReport(selectedUnit.getId());
        } else if (command.contains(COMMAND_ASSIGN_TECH)) {
            Person tech = gui.getCampaign().getPerson(UUID.fromString(command.split(":")[1]));
            if (tech != null) {
                // remove any existing techs
                for (Unit u : units) {
                    if (u.getTech() != null) {
                        u.remove(u.getTech(), true);
                    }
                    u.setTech(tech);
                }
            }
        } else if (command.equals(COMMAND_SET_QUALITY)) {
            int q;
            Object[] possibilities = { "F", "E", "D", "C", "B", "A" }; // TODO : this probably shouldn't be inline
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
        } else if (command.equals(COMMAND_SELL)) {
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
        } else if (command.equals(COMMAND_LOSS)) {
            for (Unit unit : units) {
                if (0 == JOptionPane.showConfirmDialog(null,
                        "Do you really want to consider " + unit.getName()
                                + " a combat loss?", "Remove Unit?",
                        JOptionPane.YES_NO_OPTION)) {
                    gui.getCampaign().removeUnit(unit.getId());
                }
            }
        } else if (command.equals(COMMAND_LC_SWAP_AMMO)) { // Single Unit only
            LargeCraftAmmoSwapDialog dialog = new LargeCraftAmmoSwapDialog(gui.getFrame(), selectedUnit);
            dialog.setVisible(true);
            if (!dialog.wasCanceled()) {
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.contains(COMMAND_SWAP_AMMO)) { // Single Unit only
            String[] fields = command.split(":");
            int selAmmoId = Integer.parseInt(fields[1]);
            Part part = gui.getCampaign().getPart(selAmmoId);
            if (!(part instanceof AmmoBin)) {
                return;
            }
            AmmoBin ammo = (AmmoBin) part;
            AmmoType atype = (AmmoType) EquipmentType.get(fields[2]);
            ammo.changeMunition(atype);
            MekHQ.triggerEvent(new UnitChangedEvent(part.getUnit()));
        } else if (command.contains(COMMAND_CHANGE_SITE)) {
            int selected = Integer.parseInt(command.split(":")[1]);
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    if ((selected > -1) && (selected < Unit.SITE_N)) {
                        unit.setSite(selected);
                        MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
                    }
                }
            }
        } else if (command.equals(COMMAND_SALVAGE)) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    unit.setSalvage(true);
                    MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
                }
            }
        } else if (command.equals(COMMAND_REPAIR)) {
            for (Unit unit : units) {
                if (!unit.isDeployed() && unit.isRepairable()) {
                    unit.setSalvage(false);
                    MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
                }
            }
        } else if (command.equals(COMMAND_TAG_CUSTOM)) {
            addCustomUnitTag(units);
        } else if (command.equals(COMMAND_REMOVE)) {
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
        } else if (command.equals(COMMAND_DISBAND)) {
            for (Unit unit : units) {
                if (!unit.isDeployed()) {
                    if (0 == JOptionPane.showConfirmDialog(null,
                            "Do you really want to disband this unit "
                                    + unit.getName() + "?",
                            "Disband Unit?", JOptionPane.YES_NO_OPTION)) {
                        Vector<Part> parts = new Vector<>(unit.getParts());
                        for (Part p : parts) {
                            p.remove(true);
                        }
                        gui.getCampaign().removeUnit(unit.getId());
                    }
                }
            }
        } else if (command.equals(COMMAND_UNDEPLOY)) {
            for (Unit unit : units) {
                if (unit.isDeployed()) {
                    gui.undeployUnit(unit); // Event is triggered from undeployUnit
                }
            }
        } else if (command.contains(COMMAND_HIRE_FULL)) {
            boolean isGM = command.equals(COMMAND_HIRE_FULL_GM);
            for (Unit unit : units) {
                gui.getCampaign().hirePersonnelFor(unit.getId(), isGM);
            }
        } else if (command.equals(COMMAND_CUSTOMIZE)) { // Single Unit only
            ((MekLabTab) gui.getTab(GuiTabType.MEKLAB)).loadUnit(selectedUnit);
            gui.getTabMain().setSelectedIndex(GuiTabType.MEKLAB.getDefaultPos());
        } else if (command.equals(COMMAND_CANCEL_CUSTOMIZE)) { // Single Unit only
            selectedUnit.getRefit().cancel();
        } else if (command.equals(COMMAND_REFIT_GM_COMPLETE)) { // Single Unit only
            gui.getCampaign().addReport(selectedUnit.getRefit().succeed());
        } else if (command.equals(COMMAND_REFURBISH)) { // Single Unit only
            Refit r = new Refit(selectedUnit, selectedUnit.getEntity(),false, true);
            gui.refitUnit(r, false);
        } else if (command.equals(COMMAND_REFIT_KIT)) { // Single Unit only
            ChooseRefitDialog crd = new ChooseRefitDialog(gui.getFrame(), true,
                    gui.getCampaign(), selectedUnit, gui);
            crd.setVisible(true);
        } else if (command.equals(COMMAND_CHANGE_HISTORY)) { // Single Unit only
            MarkdownEditorDialog tad = new MarkdownEditorDialog(gui.getFrame(), true,
                    "Edit Unit History", selectedUnit.getHistory());
            tad.setVisible(true);
            if (tad.wasChanged()) {
                selectedUnit.setHistory(tad.getText());
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equals(COMMAND_REMOVE_INDI_CAMO)) {
            for (Unit u : units) {
                if (u.isEntityCamo()) {
                    u.getEntity().setCamoCategory(null);
                    u.getEntity().setCamoFileName(null);
                }
            }
        } else if (command.equals(COMMAND_INDI_CAMO)) { // Single Unit only
            String category = selectedUnit.getCamoCategory();
            if (StringUtil.isNullOrEmpty(category)) {
                category = Player.ROOT_CAMO;
            }
            CamoChoiceDialog ccd = new CamoChoiceDialog(gui.getFrame(), true,
                    category, selectedUnit.getCamoFileName(), gui.getCampaign()
                            .getColorIndex(), gui.getIconPackage().getCamos());
            ccd.setLocationRelativeTo(gui.getFrame());
            ccd.setVisible(true);

            if (ccd.clickedSelect()) {
                selectedUnit.getEntity().setCamoCategory(ccd.getCategory());
                selectedUnit.getEntity().setCamoFileName(ccd.getFileName());
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equals(COMMAND_CANCEL_ORDER)) {
            double refund = gui.getCampaign().getCampaignOptions().GetCanceledOrderReimbursement();
            for (Unit u : units) {
                Money refundAmount = u.getBuyCost().multipliedBy(refund);
                gui.getCampaign().removeUnit(u.getId());
                gui.getCampaign().getFinances().credit(refundAmount, Transaction.C_EQUIP,
                        "refund for cancelled equipment sale", gui.getCampaign().getDate());
            }
        } else if (command.equals(COMMAND_ARRIVE)) {
            for (Unit u : units) {
                u.setDaysToArrival(0);
            }
        } else if (command.equals(COMMAND_MOTHBALL)) {
            if (units.length > 1) {
                gui.showMassMothballDialog(units, false);
            } else {
                UUID techId = pickTechForMothballOrActivation(selectedUnit, "mothballing");
                MothballUnitAction mothballUnitAction = new MothballUnitAction(techId, false);
                mothballUnitAction.Execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equals(COMMAND_ACTIVATE)) {
            if (units.length > 1) {
                gui.showMassMothballDialog(units, true);
            } else {
                UUID techId = pickTechForMothballOrActivation(selectedUnit, "activation");
                ActivateUnitAction activateUnitAction = new ActivateUnitAction(techId, false);
                activateUnitAction.Execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equals(COMMAND_CANCEL_MOTHBALL)) {
            CancelMothballUnitAction cancelAction = new CancelMothballUnitAction();
            for (Unit u : units) {
                if (u.isMothballing()) {
                    cancelAction.Execute(gui.getCampaign(), u);
                    MekHQ.triggerEvent(new UnitChangedEvent(u));
                }
            }
        } else if (command.equals(COMMAND_BOMBS)) { // Single Unit only
            BombsDialog dialog = new BombsDialog((IBomber) selectedUnit.getEntity(),
                    gui.getCampaign(), gui.getFrame());
            dialog.setVisible(true);
            MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
        } else if (command.equals(COMMAND_QUIRKS)) { // Single Unit only
            QuirksDialog dialog = new QuirksDialog(selectedUnit.getEntity(), gui.getFrame());
            dialog.setVisible(true);
            MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
        } else if (command.equals(COMMAND_EDIT_DAMAGE)) { // Single Unit only
            UnitEditorDialog med = new UnitEditorDialog(gui.getFrame(), selectedUnit.getEntity());
            med.setVisible(true);
            selectedUnit.runDiagnostic(false);
            MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
        } else if (command.equals(COMMAND_FLUFF_NAME)) { // Single Unit only
            String fluffName = (String) JOptionPane.showInputDialog(
                    gui.getFrame(), "Name for this unit?", "Unit Name",
                    JOptionPane.QUESTION_MESSAGE, null, null,
                    selectedUnit.getFluffName() == null ? "" : selectedUnit.getFluffName());
            selectedUnit.setFluffName(fluffName);
            MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
        } else if (command.equals(COMMAND_RESTORE_UNIT)) {
            for (Unit unit : units) {
                unit.setSalvage(false);

                boolean needsCheck = true;
                while (unit.isAvailable() && needsCheck) {
                    needsCheck = false;
                    for (int x = 0; x < unit.getParts().size(); x++) {
                        Part part = unit.getParts().get(x);
                        if (part instanceof MissingPart) {
                            //Make sure we restore both left and right thrusters
                            if (part instanceof MissingThrusters) {
                                if (((Aero) unit.getEntity()).getLeftThrustHits() > 0) {
                                    ((MissingThrusters) part).setLeftThrusters(true);
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
                            if (part.needsFixing()) {
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
                        if (part instanceof Armor) {
                            final Armor armor = (Armor) part;
                            armor.setAmount(armor.getTotalAmount());
                        } else if (part instanceof AmmoBin) {
                            final AmmoBin ammoBin = (AmmoBin) part;

                            // we magically find the ammo we need, then load the bin
                            // we only want to get the amount of ammo the bin actually needs
                            if (ammoBin.getShotsNeeded() > 0) {
                                ammoBin.setShotsNeeded(0);
                                ammoBin.updateConditionFromPart();
                            }
                        }

                    }

                    // TODO: Make this less painful. We just want to fix hips and shoulders.
                    Entity entity = unit.getEntity();
                    if (entity instanceof Mech) {
                        for(int loc : new int[]{
                            Mech.LOC_CLEG, Mech.LOC_LLEG, Mech.LOC_RLEG, Mech.LOC_LARM, Mech.LOC_RARM}) {
                            int numberOfCriticals = entity.getNumberOfCriticals(loc);
                            for (int crit = 0; crit < numberOfCriticals; ++ crit) {
                                CriticalSlot slot = entity.getCritical(loc, crit);
                                if (null != slot) {
                                    slot.setHit(false);
                                    slot.setDestroyed(false);
                                }
                            }
                        }
                    }
                }
                MekHQ.triggerEvent(new UnitChangedEvent(unit));
            }
        } else if (command.equals(COMMAND_STRIP_UNIT)) {
            IUnitAction stripUnitAction = new StripUnitAction();
            for (Unit u : units) {
                stripUnitAction.Execute(gui.getCampaign(), u);
            }
        } else if (command.equals(COMMAND_GM_MOTHBALL)) {
            MothballUnitAction mothballUnitAction = new MothballUnitAction(null, true);
            for (Unit u : units) {
                // this is so we can have this show with a mixture of Mothballed and non-Mothballed units
                if (!u.isMothballed()) {
                    mothballUnitAction.Execute(gui.getCampaign(), u);
                    MekHQ.triggerEvent(new UnitChangedEvent(u));
                }
            }
        } else if (command.equals(COMMAND_GM_ACTIVATE)) {
            ActivateUnitAction activateUnitAction = new ActivateUnitAction(null, true);
            for (Unit u : units) {
                // this is so we can have this show with a mixture of Mothballed and non-Mothballed units
                if (u.isMothballed()) {
                    activateUnitAction.Execute(gui.getCampaign(), u);
                    MekHQ.triggerEvent(new UnitChangedEvent(u));
                }
            }
        }
    }

    private UUID pickTechForMothballOrActivation(Unit unit, String description) {
        UUID id = null;
        if (!unit.isSelfCrewed()) {
            id = gui.selectTech(unit, description, true);
            if (null != id) {
                Person tech = gui.getCampaign().getPerson(id);
                if (tech.getTechUnitIDs().size() > 0) {
                    if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(gui.getFrame(),
                            tech.getFullName() + " will not be able to perform maintenance on "
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
        if (e.isPopupTrigger()) {
            // Immediately return if there are no units selected
            if (unitTable.getSelectedRowCount() == 0) {
                return;
            }

            //region Variable Declarations and Instantiations
            JPopupMenu popup = new JPopupMenu();
            JMenuItem menuItem;
            JMenu menu;
            JCheckBoxMenuItem cbMenuItem;

            boolean isGM = gui.getCampaign().isGM();

            int[] rows = unitTable.getSelectedRows();
            boolean oneSelected = unitTable.getSelectedRowCount() == 1;
            Unit[] units = new Unit[rows.length];
            for (int i = 0; i < rows.length; i++) {
                units[i] = unitModel.getUnit(unitTable.convertRowIndexToModel(rows[i]));
            }
            Unit unit = units[0];

            boolean nonePresent = true; // different menu if there is at least one present unit
            for (Unit u : units) {
                if (u.isPresent()) {
                    nonePresent = false;
                    break;
                }
            }
            //endregion Variable Declarations and Instantiations

            if (nonePresent) {
                menuItem = new JMenuItem("Cancel This Delivery");
                menuItem.setActionCommand(COMMAND_CANCEL_ORDER);
                menuItem.addActionListener(this);
                popup.add(menuItem);
                if (isGM) {
                    menu = new JMenu("GM Mode");
                    menuItem = new JMenuItem("Unit Arrives Immediately");
                    menuItem.setActionCommand(COMMAND_ARRIVE);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    popup.addSeparator();
                    popup.add(menu);
                }
            } else {
                //region Determine if to Display
                // this is used to determine whether or not to show parts of the GUI, especially for
                // bulk selections
                boolean oneMothballed = false; // If at least one unit is mothballed, we want to show unit activation
                boolean oneMothballing = false; // If at least one unit is mothballing, we want to be able to cancel it
                boolean oneActive = false; // If at least one unit is active, we want to enable mothballing
                boolean oneDeployed = false; // If at least one unit is deployed, we want to show the remove deployment to GMs
                boolean allDeployed = true; // don't show sell dialog if all units are deployed
                boolean oneAvailableUnitBelowMaxCrew = false; // If one unit isn't fully crewed, enable bulk hiring
                boolean oneNotPresent = false; // If a unit isn't present, enable instant arrival for GMs
                boolean oneHasIndividualCamo = false; // If a unit has a unique camo, allow it to be removed
                boolean oneHasCrew = false; // If a unit has crew, enable removing it
                boolean allUnitsAreRepairable = true;  // If all units can be repaired, allow the repair flag to be selected
                boolean areAllConventionalInfantry = true; // Conventional infantry can be disbanded, but no others
                boolean noConventionalInfantry = true; // Conventional infantry can't be repaired/salvaged
                boolean areAllRepairFlagged = true; // If everyone has the repair flag, then we show the repair flag box as selected
                boolean areAllSalvageFlagged = true;  // Same as above, but with the salvage flag
                boolean allRequireSameTechType = true; // If everyone requires the same tech type, we can allow bulk tech assignment
                String skill = units[0].determineUnitTechSkillType();
                int maintenanceTime = 0;
                for (Unit u : units) {
                    if (u.isMothballed()) {
                        oneMothballed = true;
                    } else if (u.isMothballing()) {
                        oneMothballing = true;
                    } else {
                        oneActive = true;
                    }

                    if ((u.getCrew().size() < u.getFullCrewSize()) && u.isAvailable()) {
                        oneAvailableUnitBelowMaxCrew = true;
                    }

                    if (u.isDeployed()) {
                        oneDeployed = true;
                    } else {
                        allDeployed = false;
                    }

                    if (!u.isPresent()) {
                        oneNotPresent = true;
                    }

                    if (u.isEntityCamo()) {
                        oneHasIndividualCamo = true;
                    }

                    if (u.getCrew().size() > 0) {
                        oneHasCrew = true;
                    }

                    if (!u.isRepairable()) {
                        allUnitsAreRepairable = false;
                    }

                    if (u.isSalvage()) {
                        areAllRepairFlagged = false;
                    } else {
                        areAllSalvageFlagged = false;
                    }

                    if (u.getEntity().isConventionalInfantry()) {
                        noConventionalInfantry = false;
                    } else {
                        areAllConventionalInfantry = false;
                    }

                    if (!StringUtil.isNullOrEmpty(skill)) {
                        if (!skill.equals(u.determineUnitTechSkillType())) {
                            allRequireSameTechType = false;
                            skill = ""; //little performance saving hack
                            continue;
                        }
                        maintenanceTime += u.getMaintenanceTime();
                        if (maintenanceTime > Person.PRIMARY_ROLE_SUPPORT_TIME) {
                            skill = ""; //little performance saving hack
                        }
                    }
                }
                //endregion Determine if to Display

                // change the location
                menu = new JMenu("Change site");
                boolean allSameSite = StaticChecks.areAllSameSite(units);
                for (int i = 0; i < Unit.SITE_N; i++) {
                    cbMenuItem = new JCheckBoxMenuItem(Unit.getSiteName(i));
                    if (allSameSite && unit.getSite() == i) {
                        cbMenuItem.setSelected(true);
                    } else {
                        cbMenuItem.setActionCommand(COMMAND_CHANGE_SITE + ":" + i);
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
                        menuItem.setActionCommand(COMMAND_LC_SWAP_AMMO);
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    } else {
                        menu = new JMenu("Swap ammo");
                        JMenu ammoMenu;
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
                                    cbMenuItem.setActionCommand(COMMAND_SWAP_AMMO + ":" + ammo.getId()
                                            + ":" + atype.getInternalName());
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

                // Select bombs
                if (oneSelected && unit.getEntity().isBomber()) {
                    menuItem = new JMenuItem("Select Bombs");
                    menuItem.setActionCommand(COMMAND_BOMBS);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                // Salvage / Repair
                if (noConventionalInfantry) {
                    menu = new JMenu("Repair Status");
                    cbMenuItem = new JCheckBoxMenuItem("Repair");
                    cbMenuItem.setSelected(areAllRepairFlagged);
                    cbMenuItem.setActionCommand(COMMAND_REPAIR);
                    cbMenuItem.addActionListener(this);
                    cbMenuItem.setEnabled(allUnitsAreRepairable);
                    menu.add(cbMenuItem);
                    cbMenuItem = new JCheckBoxMenuItem("Salvage");
                    cbMenuItem.setSelected(areAllSalvageFlagged);
                    cbMenuItem.setActionCommand(COMMAND_SALVAGE);
                    cbMenuItem.addActionListener(this);
                    menu.add(cbMenuItem);
                    popup.add(menu);
                }

                if (oneActive) {
                    menuItem = new JMenuItem(oneSelected ? "Mothball" : "Mass Mothball");
                    menuItem.setActionCommand(COMMAND_MOTHBALL);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (oneMothballed) {
                    menuItem = new JMenuItem(oneSelected ? "Activate" : "Mass Activate");
                    menuItem.setActionCommand(COMMAND_ACTIVATE);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (oneMothballing) {
                    menuItem = new JMenuItem("Cancel Mothballing/Activation");
                    menuItem.setActionCommand(COMMAND_CANCEL_MOTHBALL);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (allRequireSameTechType && !StringUtil.isNullOrEmpty(skill)) {
                    menu = new JMenu("Assign Tech");
                    JMenu menuElite = new JMenu(SkillType.ELITE_NM);
                    JMenu menuVeteran = new JMenu(SkillType.VETERAN_NM);
                    JMenu menuRegular = new JMenu(SkillType.REGULAR_NM);
                    JMenu menuGreen = new JMenu(SkillType.GREEN_NM);
                    JMenu menuUltraGreen = new JMenu(SkillType.ULTRA_GREEN_NM);

                    int techsFound = 0;
                    for (Person tech : gui.getCampaign().getTechs()) {
                        if (tech.hasSkill(skill)
                                && (tech.getMaintenanceTimeUsing() + maintenanceTime)
                                        <= Person.PRIMARY_ROLE_SUPPORT_TIME) {

                            String skillLvl = "Unknown";
                            if (null != tech.getSkillForWorkingOn(unit)) {
                                skillLvl = SkillType.getExperienceLevelName(
                                        tech.getSkillForWorkingOn(unit).getExperienceLevel());
                            }

                            JMenu subMenu;
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
                                default:
                                    subMenu = null;
                                    break;
                            }

                            if (subMenu != null) {
                                cbMenuItem = new JCheckBoxMenuItem(tech.getFullTitle()
                                        + " (" + tech.getMaintenanceTimeUsing() + "m)");
                                cbMenuItem.setActionCommand(COMMAND_ASSIGN_TECH + ":" + tech.getId());

                                if (tech.getId().equals(unit.getTechId())) {
                                    cbMenuItem.setSelected(true);
                                } else {
                                    cbMenuItem.addActionListener(this);
                                }

                                subMenu.add(cbMenuItem);
                                if (cbMenuItem.isSelected()) {
                                    subMenu.setIcon(UIManager.getIcon("CheckBoxMenuItem.checkIcon"));
                                }
                                techsFound++;
                            }
                        }
                    }
                    if (techsFound > 0) {
                        JMenuHelpers.addMenuIfNonEmpty(menu, menuElite, 20);
                        JMenuHelpers.addMenuIfNonEmpty(menu, menuVeteran, 20);
                        JMenuHelpers.addMenuIfNonEmpty(menu, menuRegular, 20);
                        JMenuHelpers.addMenuIfNonEmpty(menu, menuGreen, 20);
                        JMenuHelpers.addMenuIfNonEmpty(menu, menuUltraGreen, 20);

                        popup.add(menu);
                    }
                }

                if (oneSelected && unit.requiresMaintenance()) {
                    menuItem = new JMenuItem("Show Last Maintenance Report");
                    menuItem.setActionCommand(COMMAND_MAINTENANCE_REPORT);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (oneSelected && !unit.isMothballed()
                        && gui.getCampaign().getCampaignOptions().usePeacetimeCost()) {
                    menuItem = new JMenuItem("Show Monthly Supply Cost Report");
                    menuItem.setActionCommand(COMMAND_SUPPLY_COST);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (areAllConventionalInfantry) {
                    menuItem = new JMenuItem("Disband");
                    menuItem.setActionCommand(COMMAND_DISBAND);
                    menuItem.addActionListener(this);
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
                    menuItem.setActionCommand(COMMAND_REFIT_KIT);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable()
                            && (unit.getEntity() instanceof Mech
                            || unit.getEntity() instanceof Tank
                            || unit.getEntity() instanceof Aero
                            || (unit.getEntity() instanceof Infantry)));
                    //TODO : Should ProtoMech be included in the above?
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Refurbish Unit");
                    menuItem.setActionCommand(COMMAND_REFURBISH);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(unit.isAvailable()
                            && (unit.getEntity() instanceof Mech
                            || unit.getEntity() instanceof Tank
                            || unit.getEntity() instanceof Aero
                            || unit.getEntity() instanceof BattleArmor
                            || unit.getEntity() instanceof Protomech));
                    menu.add(menuItem);
                    if (gui.hasTab(GuiTabType.MEKLAB)) {
                        menuItem = new JMenuItem("Customize in Mek Lab...");
                        menuItem.setActionCommand(COMMAND_CUSTOMIZE);
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(unit.isAvailable()
                                && !(unit.getEntity() instanceof GunEmplacement));
                        menu.add(menuItem);
                    }
                    if (unit.isRefitting()) {
                        menuItem = new JMenuItem("Cancel Customization");
                        menuItem.setActionCommand(COMMAND_CANCEL_CUSTOMIZE);
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                        if (isGM) {
                            menuItem = new JMenuItem("Complete Refit (GM)");
                            menuItem.setActionCommand(COMMAND_REFIT_GM_COMPLETE);
                            menuItem.addActionListener(this);
                            menu.add(menuItem);
                        }
                    }
                    menu.setEnabled(unit.isAvailable(true) && unit.isRepairable());
                    popup.add(menu);
                }

                // fill with personnel
                if (oneAvailableUnitBelowMaxCrew) {
                    menuItem = new JMenuItem("Hire full complement");
                    menuItem.setActionCommand(COMMAND_HIRE_FULL);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                // Camo
                if (oneSelected && !unit.isEntityCamo()) {
                    menuItem = new JMenuItem(gui.getResourceMap()
                            .getString("customizeMenu.individualCamo.text"));
                    menuItem.setActionCommand(COMMAND_INDI_CAMO);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }
                if (oneHasIndividualCamo) {
                    menuItem = new JMenuItem(gui.getResourceMap()
                            .getString("customizeMenu.removeIndividualCamo.text"));
                    menuItem.setActionCommand(COMMAND_REMOVE_INDI_CAMO);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (oneSelected && !gui.getCampaign().isCustom(unit)) {
                    menuItem = new JMenuItem("Tag as a custom unit");
                    menuItem.setActionCommand(COMMAND_TAG_CUSTOM);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (oneSelected && gui.getCampaign().getCampaignOptions().useQuirks()) {
                    menuItem = new JMenuItem("Edit Quirks");
                    menuItem.setActionCommand(COMMAND_QUIRKS);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (oneSelected) {
                    menuItem = new JMenuItem("Edit Unit History...");
                    menuItem.setActionCommand(COMMAND_CHANGE_HISTORY);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                // remove all personnel
                if (oneHasCrew) {
                    popup.addSeparator();
                    menuItem = new JMenuItem("Remove all personnel");
                    menuItem.setActionCommand(COMMAND_REMOVE_ALL_PERSONNEL);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (oneSelected) {
                    menuItem = new JMenuItem("Name Unit");
                    menuItem.setActionCommand(COMMAND_FLUFF_NAME);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                // sell unit
                if (!allDeployed && gui.getCampaign().getCampaignOptions().canSellUnits()) {
                    popup.addSeparator();
                    menuItem = new JMenuItem("Sell Unit");
                    menuItem.setActionCommand(COMMAND_SELL);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                //region GM Mode
                // GM mode - only show to GMs
                if (isGM) {
                    menu = new JMenu("GM Mode");
                    menuItem = new JMenuItem("Remove Unit");
                    menuItem.setActionCommand(COMMAND_REMOVE);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Strip Unit");
                    menuItem.setActionCommand(COMMAND_STRIP_UNIT);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    if (oneActive) {
                        menuItem = new JMenuItem(oneSelected ? "Mothball Units" : "Mass Mothball Unit");
                        menuItem.setActionCommand(COMMAND_GM_MOTHBALL);
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }
                    if (oneMothballed) {
                        menuItem = new JMenuItem(oneSelected ? "Activate Units" : "Mass Activate Unit");
                        menuItem.setActionCommand(COMMAND_GM_ACTIVATE);
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }
                    if (oneDeployed) {
                        menuItem = new JMenuItem("Undeploy Unit");
                        menuItem.setActionCommand(COMMAND_UNDEPLOY);
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }
                    if (oneAvailableUnitBelowMaxCrew) {
                        menuItem = new JMenuItem("Add full complement");
                        menuItem.setActionCommand(COMMAND_HIRE_FULL_GM);
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }
                    if (oneSelected) {
                        menuItem = new JMenuItem("Edit Damage...");
                        menuItem.setActionCommand(COMMAND_EDIT_DAMAGE);
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }
                    menuItem = new JMenuItem("Restore Unit");
                    menuItem.setActionCommand(COMMAND_RESTORE_UNIT);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    menuItem = new JMenuItem("Set Quality...");
                    menuItem.setActionCommand(COMMAND_SET_QUALITY);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                    if (oneNotPresent) {
                        menuItem = new JMenuItem("Unit Arrives Immediately");
                        menuItem.setActionCommand(COMMAND_ARRIVE);
                        menuItem.addActionListener(this);
                        menu.add(menuItem);
                    }
                    popup.addSeparator();
                    popup.add(menu);
                }
                //endregion GM Mode
            }
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void addCustomUnitTag(Unit[] units) {
        String sCustomsDir = "data/mechfiles/customs/";
        String sCustomsDirCampaign = sCustomsDir + gui.getCampaign().getName() + "/";
        File customsDir = new File(sCustomsDir);
        if (!customsDir.exists()) {
            if (!customsDir.mkdir()) {
                MekHQ.getLogger().error(getClass(), "addCustomUnitTag",
                        "Unable to create directory " + sCustomsDir +
                                " to hold custom units, cannot assign custom unit tag");
                return;
            }
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            if (!customsDir.mkdir()) {
                MekHQ.getLogger().error(getClass(), "addCustomUnitTag",
                        "Unable to create directory " + sCustomsDirCampaign
                                + "to hold custom units, cannot assign custom unit tag");
                return;
            }
        }
        for (Unit unit : units) {
            String fileName = unit.getEntity().getChassis() + " " + unit.getEntity().getModel();
            if (unit.getEntity() instanceof Mech) {
                // if this file already exists then don't overwrite
                // it or we will end up with a bunch of copies
                String fileOutName = sCustomsDir + File.separator + fileName + ".mtf";
                String fileNameCampaign = sCustomsDirCampaign + File.separator + fileName + ".mtf";
                if ((new File(fileOutName)).exists() || (new File(fileNameCampaign)).exists()) {
                    JOptionPane.showMessageDialog(null,
                            "A file already exists for this unit, cannot tag as custom. (Unit name and model)",
                            "File Already Exists", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try (OutputStream os = new FileOutputStream(fileNameCampaign);
                     PrintStream p = new PrintStream(os)) {

                    p.println(((Mech) unit.getEntity()).getMtf());
                } catch (Exception e) {
                    MekHQ.getLogger().error(getClass(), "addCustomUnitTag", e);
                }
            } else {
                // if this file already exists then don't overwrite
                // it or we will end up with a bunch of copies
                String fileOutName = sCustomsDir + File.separator + fileName + ".blk";
                String fileNameCampaign = sCustomsDirCampaign + File.separator + fileName + ".blk";
                if ((new File(fileOutName)).exists() || (new File(fileNameCampaign)).exists()) {
                    JOptionPane.showMessageDialog(null,
                            "A file already exists for this unit, cannot tag as custom. (Unit name and model)",
                            "File Already Exists", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                BLKFile.encode(fileNameCampaign, unit.getEntity());
            }
            gui.getCampaign().addCustom(unit.getEntity().getChassis() + " "
                    + unit.getEntity().getModel());
        }
        MechSummaryCache.getInstance().loadMechData();
    }
}
