/*
 * Copyright (c) 2014-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.dialogs.BVDisplayDialog;
import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.client.ui.swing.UnitEditorDialog;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.util.EncodeControl;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHQ;
import mekhq.MekHqConstants;
import mekhq.Utilities;
import mekhq.campaign.event.RepairStatusChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.*;
import mekhq.gui.CampaignGUI;
import mekhq.gui.GuiTabType;
import mekhq.gui.HangarTab;
import mekhq.gui.MekLabTab;
import mekhq.gui.dialog.*;
import mekhq.gui.dialog.reportDialogs.MaintenanceReportDialog;
import mekhq.gui.dialog.reportDialogs.MonthlyUnitCostReportDialog;
import mekhq.gui.menus.AssignUnitToPersonMenu;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.gui.utilities.StaticChecks;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Stream;

public class UnitTableMouseAdapter extends JPopupMenuAdapter {
    //region Variable Declarations
    private CampaignGUI gui;
    private JTable unitTable;
    private UnitTableModel unitModel;

    //region Commands
    //region Standard Commands
    //region Undelivered Unit Commands
    public static final String COMMAND_CANCEL_ORDER = "CANCEL_ORDER";
    public static final String COMMAND_ARRIVE = "ARRIVE";
    //endregion Undelivered Unit Commands

    public static final String COMMAND_CHANGE_SITE = "CHANGE_SITE";
    // Ammo Swap Commands
    public static final String COMMAND_LC_SWAP_AMMO = "LC_SWAP_AMMO";
    public static final String COMMAND_SMALL_SV_SWAP_AMMO = "SMALL_SV_SWAP_AMMO";
    // Repair Commands
    public static final String COMMAND_REPAIR = "REPAIR";
    public static final String COMMAND_SALVAGE = "SALVAGE";
    // Mothball Commands
    public static final String COMMAND_MOTHBALL = "MOTHBALL";
    public static final String COMMAND_ACTIVATE = "ACTIVATE";
    public static final String COMMAND_CANCEL_MOTHBALL = "CANCEL_MOTHBALL";
    // Unit History Commands
    public static final String COMMAND_CHANGE_HISTORY = "CHANGE_HISTORY";

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
    public static final String COMMAND_CHANGE_MAINT_MULTI = "CHANGE_MAINT_MULT";
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

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    protected UnitTableMouseAdapter(CampaignGUI gui, JTable unitTable, UnitTableModel unitModel) {
        this.gui = gui;
        this.unitTable = unitTable;
        this.unitModel = unitModel;
    }

    public static void connect(CampaignGUI gui, JTable unitTable, UnitTableModel unitModel, JSplitPane splitUnit) {
        new UnitTableMouseAdapter(gui, unitTable, unitModel) {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    int width = splitUnit.getSize().width;
                    int location = splitUnit.getDividerLocation();
                    int size = splitUnit.getDividerSize();
                    if ((width - location + size) < HangarTab.UNIT_VIEW_WIDTH) {
                        // expand
                        splitUnit.resetToPreferredSizes();
                    } else {
                        // collapse
                        splitUnit.setDividerLocation(1.0);
                    }
                }
            }
        }.connect(unitTable);
    }

    @Override
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

        if (command.equals(COMMAND_MAINTENANCE_REPORT)) { // Single Unit only
            new MaintenanceReportDialog(gui.getFrame(), selectedUnit).setVisible(true);
        } else if (command.equals(COMMAND_SUPPLY_COST)) { // Single Unit only
            new MonthlyUnitCostReportDialog(gui.getFrame(), selectedUnit).setVisible(true);
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
                        gui.getCampaign().getQuartermaster().sellUnit(unit);
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
        } else if (command.equals(COMMAND_SMALL_SV_SWAP_AMMO)) {
            SmallSVAmmoSwapDialog dialog = new SmallSVAmmoSwapDialog(gui.getFrame(), selectedUnit);
            dialog.setVisible(true);
            if (!dialog.wasCanceled()) {
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.contains(COMMAND_CHANGE_SITE)) {
            try {
                int selected = Integer.parseInt(command.split(":")[1]);
                for (Unit unit : units) {
                    if (!unit.isDeployed()) {
                        if ((selected > -1) && (selected < Unit.SITE_N)) {
                            unit.setSite(selected);
                            MekHQ.triggerEvent(new RepairStatusChangedEvent(unit));
                        }
                    }
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
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
                String title = String.format(resources.getString("deleteUnitsCount.text"), toRemove.size());
                if (toRemove.size() == 1) {
                    title = toRemove.get(0).getName();
                }
                if (0 == JOptionPane.showConfirmDialog(
                        null,
                        String.format(resources.getString("confirmRemove.text"), title),
                        resources.getString("removeQ.title"),
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
            HirePersonnelUnitAction hireAction = new HirePersonnelUnitAction(isGM);
            for (Unit unit : units) {
                hireAction.execute(gui.getCampaign(), unit);
            }
        } else if (command.equals(COMMAND_CUSTOMIZE)) { // Single Unit only
            ((MekLabTab) gui.getTab(GuiTabType.MEKLAB)).loadUnit(selectedUnit);
            gui.getTabMain().setSelectedIndex(GuiTabType.MEKLAB.getDefaultPos());
        } else if (command.equals(COMMAND_CANCEL_CUSTOMIZE)) {
            Stream.of(units).filter(Unit::isRefitting).forEach(unit -> unit.getRefit().cancel());
        } else if (command.equals(COMMAND_REFIT_GM_COMPLETE)) {
            Stream.of(units).filter(Unit::isRefitting).forEach(unit -> unit.getRefit().succeed());
        } else if (command.equals(COMMAND_REFURBISH)) {
            for (Unit unit : units) {
                Refit refit = new Refit(unit, unit.getEntity(), false, true);
                gui.refitUnit(refit, false);
            }
        } else if (command.equals(COMMAND_REFIT_KIT)) { // Single Unit or Multiple of Units of the same type only
            ChooseRefitDialog crd = new ChooseRefitDialog(gui.getFrame(), true, gui.getCampaign(),
                    selectedUnit);
            crd.setVisible(true);
            if (crd.isConfirmed()) {
                MechSummary summary = MechSummaryCache.getInstance().getMech(crd.getSelectedRefit()
                        .getNewEntity().getShortNameRaw());
                if (summary != null) {
                    for (Unit unit : units) {
                        try {
                            Entity refitEntity = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
                            if (refitEntity != null) {
                                Refit refit = new Refit(unit, refitEntity, false, false);
                                if (refit.checkFixable() == null) {
                                    gui.refitUnit(refit, false);
                                }
                            }
                        } catch (EntityLoadingException ex) {
                            MekHQ.getLogger().error(ex);
                        }
                    }
                }
            }
        } else if (command.equals(COMMAND_CHANGE_HISTORY)) { // Single Unit only
            MarkdownEditorDialog tad = new MarkdownEditorDialog(gui.getFrame(), true,
                    "Edit Unit History", selectedUnit.getHistory());
            tad.setVisible(true);
            if (tad.wasChanged()) {
                selectedUnit.setHistory(tad.getText());
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equals(COMMAND_REMOVE_INDI_CAMO)) {
            for (final Unit unit : units) {
                unit.getEntity().setCamouflage(new Camouflage());
                MekHQ.triggerEvent(new UnitChangedEvent(unit));
            }
        } else if (command.equals(COMMAND_INDI_CAMO)) {
            final CamoChooserDialog ccd = new CamoChooserDialog(gui.getFrame(),
                    selectedUnit.getUtilizedCamouflage(gui.getCampaign()), true);
            if (ccd.showDialog().isCancelled()) {
                return;
            }
            for (final Unit unit : units) {
                unit.getEntity().setCamouflage(ccd.getSelectedItem());
                MekHQ.triggerEvent(new UnitChangedEvent(unit));
            }
        } else if (command.equals(COMMAND_CANCEL_ORDER)) {
            for (Unit u : units) {
                Money refundAmount = u.getBuyCost().multipliedBy(
                        gui.getCampaign().getCampaignOptions().getCancelledOrderRefundMultiplier());
                gui.getCampaign().removeUnit(u.getId());
                gui.getCampaign().getFinances().credit(TransactionType.EQUIPMENT_PURCHASE,
                        gui.getCampaign().getLocalDate(), refundAmount,
                        "refund for cancelled equipment sale");
            }
        } else if (command.equals(COMMAND_ARRIVE)) {
            for (Unit u : units) {
                u.setDaysToArrival(0);
            }
        } else if (command.equals(COMMAND_MOTHBALL)) {
            if (units.length > 1) {
                gui.showMassMothballDialog(units, false);
            } else {
                Person tech = pickTechForMothballOrActivation(selectedUnit, "mothballing");
                MothballUnitAction mothballUnitAction = new MothballUnitAction(tech, false);
                mothballUnitAction.execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equals(COMMAND_ACTIVATE)) {
            if (units.length > 1) {
                gui.showMassMothballDialog(units, true);
            } else {
                Person tech = pickTechForMothballOrActivation(selectedUnit, "activation");
                ActivateUnitAction activateUnitAction = new ActivateUnitAction(tech, false);
                activateUnitAction.execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equals(COMMAND_CANCEL_MOTHBALL)) {
            CancelMothballUnitAction cancelAction = new CancelMothballUnitAction();
            for (Unit u : units) {
                if (u.isMothballing()) {
                    cancelAction.execute(gui.getCampaign(), u);
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
                    selectedUnit.getFluffName());
            selectedUnit.setFluffName((fluffName != null) ? fluffName : "");
            MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
        } else if (command.equals(COMMAND_RESTORE_UNIT)) {
            IUnitAction restoreUnitAction = new RestoreUnitAction();
            for (Unit u : units) {
                restoreUnitAction.execute(gui.getCampaign(), u);
            }
        } else if (command.equals(COMMAND_STRIP_UNIT)) {
            IUnitAction stripUnitAction = new StripUnitAction();
            for (Unit u : units) {
                stripUnitAction.execute(gui.getCampaign(), u);
            }
        } else if (command.equals(COMMAND_GM_MOTHBALL)) {
            MothballUnitAction mothballUnitAction = new MothballUnitAction(null, true);
            for (Unit u : units) {
                // this is so we can have this show with a mixture of Mothballed and non-Mothballed units
                if (!u.isMothballed()) {
                    mothballUnitAction.execute(gui.getCampaign(), u);
                    MekHQ.triggerEvent(new UnitChangedEvent(u));
                }
            }
        } else if (command.equals(COMMAND_GM_ACTIVATE)) {
            ActivateUnitAction activateUnitAction = new ActivateUnitAction(null, true);
            for (Unit u : units) {
                // this is so we can have this show with a mixture of Mothballed and non-Mothballed units
                if (u.isMothballed()) {
                    activateUnitAction.execute(gui.getCampaign(), u);
                    MekHQ.triggerEvent(new UnitChangedEvent(u));
                }
            }
        } else if (command.startsWith(COMMAND_CHANGE_MAINT_MULTI)) {
            try {
                int multiplier = Integer.parseInt(command.substring(COMMAND_CHANGE_MAINT_MULTI.length() + 1));

                for (Unit u : units) {
                    if (!u.isSelfCrewed()) {
                        u.setMaintenanceMultiplier(multiplier);
                    }
                }
            } catch (Exception e) {
                MekHQ.getLogger().error(e);
            }
        }
    }

    private @Nullable Person pickTechForMothballOrActivation(Unit unit, String description) {
        Person tech = null;
        if (!unit.isSelfCrewed()) {
            UUID id = gui.selectTech(unit, description, true);
            if (null != id) {
                tech = gui.getCampaign().getPerson(id);
                if (!tech.getTechUnits().isEmpty()) {
                    if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(gui.getFrame(),
                            tech.getFullName() + " will not be able to perform maintenance on "
                                    + tech.getTechUnits().size() + " assigned units. Proceed?",
                                    "Unmaintained unit warning",
                                    JOptionPane.YES_NO_OPTION)) {
                        tech = null;
                    }
                }
            }
        }
        return tech;
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        if (unitTable.getSelectedRowCount() == 0) {
            return Optional.empty();
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
                popup.addSeparator();
                menu = new JMenu("GM Mode");
                menuItem = new JMenuItem("Unit Arrives Immediately");
                menuItem.setActionCommand(COMMAND_ARRIVE);
                menuItem.addActionListener(this);
                menu.add(menuItem);
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
            boolean allUnitsAreRepairable = true;  // If all units can be repaired, allow the repair flag to be selected
            boolean areAllConventionalInfantry = true; // Conventional infantry can be disbanded, but no others
            boolean noConventionalInfantry = true; // Conventional infantry can't be repaired/salvaged
            boolean areAllRepairFlagged = true; // If everyone has the repair flag, then we show the repair flag box as selected
            boolean areAllSalvageFlagged = true;  // Same as above, but with the salvage flag
            boolean allSameModel = true; // If everyone is the exact same unit and model of that unit
            boolean oneRefitting = false; // If any one selected unit is refitting
            boolean allAvailable = true; // If everyone is available
            boolean allAvailableIgnoreRefit = true; // If everyone is available
            final String model = unit.getEntity().getShortNameRaw();
            int maintenanceTime = 0;

            for (Unit u : units) {
                maintenanceTime += u.getMaintenanceTime();

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

                if (allAvailableIgnoreRefit && !u.isAvailable(true)) {
                    allAvailableIgnoreRefit = false;
                    allAvailable = false;
                } else if (allAvailable && !u.isAvailable()) {
                    allAvailable = false;
                }

                if (!u.getCamouflage().hasDefaultCategory()) {
                    oneHasIndividualCamo = true;
                }

                if (!u.isRepairable()) {
                    allUnitsAreRepairable = false;
                }

                if (u.isSalvage()) {
                    areAllRepairFlagged = false;
                } else {
                    areAllSalvageFlagged = false;
                }

                if (u.isConventionalInfantry()) {
                    noConventionalInfantry = false;
                } else {
                    areAllConventionalInfantry = false;
                }

                if (!model.equals(u.getEntity().getShortNameRaw())) {
                    allSameModel = false;
                }

                if (u.isRefitting()) {
                    oneRefitting = true;
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
            menu.setEnabled(allAvailable);
            popup.add(menu);

            // swap ammo
            if (oneSelected) {
                if (unit.getEntity().usesWeaponBays()) {
                    menuItem = new JMenuItem("Swap ammo...");
                    menuItem.setActionCommand(COMMAND_LC_SWAP_AMMO);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                } else if (unit.getEntity().isSupportVehicle()
                            && (unit.getEntity().getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT)) {
                    // Small SVs can configure ammo only if they have weapons that have
                    // inferno ammo available
                    if (unit.getEntity().getWeaponList().stream()
                            .anyMatch(m -> (m.getType() instanceof InfantryWeapon)
                                    && ((InfantryWeapon) m.getType()).hasInfernoAmmo())) {
                        menuItem = new JMenuItem("Swap ammo...");
                        menuItem.setActionCommand(COMMAND_SMALL_SV_SWAP_AMMO);
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }
                } else {
                    menu = new JMenu("Swap ammo");
                    for (AmmoBin ammo : unit.getWorkingAmmoBins()) {
                        JMenu ammoMenu = new JMenu(ammo.getType().getDesc());
                        AmmoType curType = ammo.getType();
                        for (AmmoType atype : Utilities.getMunitionsFor(unit.getEntity(), curType,
                                gui.getCampaign().getCampaignOptions().getTechLevel())) {
                            cbMenuItem = new JCheckBoxMenuItem(atype.getDesc());
                            if (atype.equals(curType)) {
                                cbMenuItem.setSelected(true);
                            } else {
                                cbMenuItem.addActionListener(evt -> {
                                    IUnitAction swapAmmoTypeAction = new SwapAmmoTypeAction(ammo, atype);
                                    swapAmmoTypeAction.execute(gui.getCampaign(), unit);
                                });
                            }
                            ammoMenu.add(cbMenuItem);
                        }
                        JMenuHelpers.addMenuIfNonEmpty(menu, ammoMenu);
                    }
                    menu.setEnabled(allAvailable);
                    JMenuHelpers.addMenuIfNonEmpty(popup, menu);
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

            JMenuHelpers.addMenuIfNonEmpty(popup, new AssignUnitToPersonMenu(gui.getCampaign(), units));

            // if we're using maintenance and have selected something that requires maintenance
            if (gui.getCampaign().getCampaignOptions().checkMaintenance() && (maintenanceTime > 0)) {
                menuItem = new JMenu("Set Maintenance Extra Time");

                for (int x = 1; x <= 4; x++) {
                    JMenuItem maintenanceMultiplierItem = new JCheckBoxMenuItem("x" + x);

                    // if we've got just one unit selected,
                    // have the courtesy to show the multiplier if relevant
                    if (oneSelected && (unit.getMaintenanceMultiplier() == x)
                            && !unit.isSelfCrewed()) {
                        maintenanceMultiplierItem.setSelected(true);
                    }

                    maintenanceMultiplierItem.setActionCommand(COMMAND_CHANGE_MAINT_MULTI + ":" + x);
                    maintenanceMultiplierItem.addActionListener(this);
                    menuItem.add(maintenanceMultiplierItem);
                }

                popup.add(menuItem);
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

            // Customize unit menu
            if (allUnitsAreRepairable && allAvailableIgnoreRefit) {
                menu = new JMenu("Customize");

                // TODO : Should I be able to refit BA?
                if (allSameModel && allAvailable
                        && ((unit.getEntity() instanceof Mech)
                        || (unit.getEntity() instanceof Tank)
                        || (unit.getEntity() instanceof Aero)
                        || ((unit.getEntity() instanceof Infantry)))) {
                    menuItem = new JMenuItem(unit.getEntity().isOmni() ? "Choose configuration..."
                            : "Choose Refit Kit...");
                    menuItem.setActionCommand(COMMAND_REFIT_KIT);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }

                if (allSameModel && allAvailable
                        && ((unit.getEntity() instanceof Mech)
                        || (unit.getEntity() instanceof Tank)
                        || (unit.getEntity() instanceof Aero)
                        || ((unit.getEntity() instanceof Infantry)
                        || (unit.getEntity() instanceof Protomech)))) {
                    menuItem = new JMenuItem("Refurbish Unit");
                    menuItem.setActionCommand(COMMAND_REFURBISH);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }

                if (oneSelected && gui.hasTab(GuiTabType.MEKLAB)) {
                    menuItem = new JMenuItem("Customize in Mek Lab...");
                    menuItem.setActionCommand(COMMAND_CUSTOMIZE);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(allAvailable
                            && !(unit.getEntity() instanceof GunEmplacement));
                    menu.add(menuItem);
                }

                if (oneRefitting) {
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
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }

            // fill with personnel
            if (oneAvailableUnitBelowMaxCrew) {
                menuItem = new JMenuItem(resources.getString("hireMinimumComplement.text"));
                menuItem.setActionCommand(COMMAND_HIRE_FULL);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            if (Stream.of(units).allMatch(u -> u.getCamouflage().equals(units[0].getCamouflage()))) {
                menuItem = new JMenuItem(gui.getResourceMap()
                        .getString("customizeMenu.individualCamo.text"));
                menuItem.setActionCommand(COMMAND_INDI_CAMO);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            if (!oneSelected && oneHasIndividualCamo) {
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

            if (oneSelected) {
                menuItem = new JMenuItem("Name Unit");
                menuItem.setActionCommand(COMMAND_FLUFF_NAME);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            if (oneSelected) {
                menuItem = new JMenuItem("Show BV Calculation");
                menuItem.addActionListener(evt -> {
                    if (unit.getEntity() != null) {
                        unit.getEntity().calculateBattleValue();
                        new BVDisplayDialog(gui.getFrame(), unit.getEntity()).setVisible(true);
                    }
                });
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
                popup.addSeparator();
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
                    menuItem = new JMenuItem(oneSelected ? "Mothball Unit" : "Mass Mothball Units");
                    menuItem.setActionCommand(COMMAND_GM_MOTHBALL);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }

                if (oneMothballed) {
                    menuItem = new JMenuItem(oneSelected ? "Activate Unit" : "Mass Activate Units");
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
                    menuItem = new JMenuItem(resources.getString("addMinimumComplement.text"));
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

                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }
            //endregion GM Mode
        }

        return Optional.of(popup);
    }

    private void addCustomUnitTag(Unit... units) {
        String sCustomsDirCampaign = MekHqConstants.CUSTOM_MECHFILES_DIRECTORY_PATH
                + gui.getCampaign().getName() + "/";
        File customsDir = new File(MekHqConstants.CUSTOM_MECHFILES_DIRECTORY_PATH);
        if (!customsDir.exists()) {
            if (!customsDir.mkdir()) {
                MekHQ.getLogger().error("Unable to create directory "
                        + MekHqConstants.CUSTOM_MECHFILES_DIRECTORY_PATH
                        + " to hold custom units, cannot assign custom unit tag");
                return;
            }
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            if (!customsDir.mkdir()) {
                MekHQ.getLogger().error("Unable to create directory " + sCustomsDirCampaign
                        + "to hold custom units, cannot assign custom unit tag");
                return;
            }
        }
        for (Unit unit : units) {
            String fileName = unit.getEntity().getChassis() + " " + unit.getEntity().getModel();
            if (unit.getEntity() instanceof Mech) {
                // if this file already exists then don't overwrite
                // it or we will end up with a bunch of copies
                String fileOutName = MekHqConstants.CUSTOM_MECHFILES_DIRECTORY_PATH + File.separator
                        + fileName + ".mtf";
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
                    MekHQ.getLogger().error(e);
                }
            } else {
                // if this file already exists then don't overwrite
                // it or we will end up with a bunch of copies
                String fileOutName = MekHqConstants.CUSTOM_MECHFILES_DIRECTORY_PATH + File.separator
                        + fileName + ".blk";
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
