/*
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.adapter;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.common.enums.SkillLevel.ELITE;
import static megamek.common.enums.SkillLevel.GREEN;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.enums.SkillLevel.ULTRA_GREEN;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;
import static mekhq.campaign.personnel.PersonUtility.overrideSkills;
import static mekhq.campaign.unit.Unit.SITE_FIELD_WORKSHOP;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Stream;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;

import megamek.client.ui.dialogs.UnitEditorDialog;
import megamek.client.ui.dialogs.abstractDialogs.BVDisplayDialog;
import megamek.client.ui.dialogs.iconChooser.CamoChooserDialog;
import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.icons.Camouflage;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.loaders.EntitySavingException;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.IBomber;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.RepairStatusChangedEvent;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.rentals.FacilityRentals;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.unit.Maintenance;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.ActivateUnitAction;
import mekhq.campaign.unit.actions.CancelMothballUnitAction;
import mekhq.campaign.unit.actions.HirePersonnelUnitAction;
import mekhq.campaign.unit.actions.IUnitAction;
import mekhq.campaign.unit.actions.MothballUnitAction;
import mekhq.campaign.unit.actions.RestoreUnitAction;
import mekhq.campaign.unit.actions.StripUnitAction;
import mekhq.campaign.unit.actions.SwapAmmoTypeAction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.HangarTab;
import mekhq.gui.MekLabTab;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.dialog.BombsDialog;
import mekhq.gui.dialog.ChooseRefitDialog;
import mekhq.gui.dialog.LargeCraftAmmoSwapDialog;
import mekhq.gui.dialog.MarkdownEditorDialog;
import mekhq.gui.dialog.MassMothballDialog;
import mekhq.gui.dialog.QuirksDialog;
import mekhq.gui.dialog.SmallSVAmmoSwapDialog;
import mekhq.gui.dialog.reportDialogs.MaintenanceReportDialog;
import mekhq.gui.dialog.reportDialogs.MonthlyUnitCostReportDialog;
import mekhq.gui.dialog.reportDialogs.PartQualityReportDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.menus.AssignUnitToForceMenu;
import mekhq.gui.menus.AssignUnitToPersonMenu;
import mekhq.gui.menus.ExportUnitSpriteMenu;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.gui.utilities.StaticChecks;

public class UnitTableMouseAdapter extends JPopupMenuAdapter {
    private static final MMLogger LOGGER = MMLogger.create(UnitTableMouseAdapter.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GUI";

    // region Variable Declarations
    private final CampaignGUI gui;
    private final JTable unitTable;
    private final UnitTableModel unitModel;

    // region Commands
    // region Standard Commands
    // region Undelivered Unit Commands
    public static final String COMMAND_CANCEL_ORDER = "CANCEL_ORDER";
    public static final String COMMAND_ARRIVE = "ARRIVE";
    // endregion Undelivered Unit Commands

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
    public static final String COMMAND_PARTS_REPORT = "PARTS_REPORT";
    public static final String COMMAND_TAG_CUSTOM = "TAG_CUSTOM";
    public static final String COMMAND_INDI_CAMO = "INDI_CAMO";
    public static final String COMMAND_REMOVE_INDI_CAMO = "REMOVE_INDI_CAMO";
    public static final String COMMAND_CUSTOMIZE = "CUSTOMIZE";
    public static final String COMMAND_CANCEL_CUSTOMIZE = "CANCEL_CUSTOMIZE";
    public static final String COMMAND_REFIT_GM_COMPLETE = "REFIT_GM_COMPLETE";
    public static final String COMMAND_REFURBISH = "REFURBISH";
    public static final String COMMAND_REFIT_KIT = "REFIT_KIT";
    public static final String COMMAND_FLUFF_NAME = "FLUFF_NAME";
    public static final String COMMAND_CHANGE_MAINTENANCE_MULTI = "CHANGE_MAINTENANCE_MULTI";
    public static final String COMMAND_PERFORM_AD_HOC_MAINTENANCE = "PERFORM_AD_HOC_MAINTENANCE";
    // endregion Standard Commands

    // region GM Commands
    public static final String COMMAND_GM = "_GM"; // do NOT use as a command, just to create commands
    public static final String COMMAND_REMOVE = "REMOVE";
    public static final String COMMAND_STRIP_UNIT = "STRIP_UNIT";
    public static final String COMMAND_GM_MOTHBALL = COMMAND_MOTHBALL + COMMAND_GM;
    public static final String COMMAND_GM_ACTIVATE = COMMAND_ACTIVATE + COMMAND_GM;
    public static final String COMMAND_UNDEPLOY = "UNDEPLOY";
    public static final String COMMAND_HIRE_FULL_GM = COMMAND_HIRE_FULL + COMMAND_GM;
    public static final String COMMAND_HIRE_FULL_GM_ELITE = COMMAND_HIRE_FULL + COMMAND_GM + "ELITE";
    public static final String COMMAND_HIRE_FULL_GM_VETERAN = COMMAND_HIRE_FULL + COMMAND_GM + "VETERAN";
    public static final String COMMAND_HIRE_FULL_GM_REGULAR = COMMAND_HIRE_FULL + COMMAND_GM + "REGULAR";
    public static final String COMMAND_HIRE_FULL_GM_GREEN = COMMAND_HIRE_FULL + COMMAND_GM + "GREEN";
    public static final String COMMAND_HIRE_FULL_GM_ULTRA_GREEN = COMMAND_HIRE_FULL + COMMAND_GM + "ULTRA_GREEN";
    public static final String COMMAND_EDIT_DAMAGE = "EDIT_DAMAGE";
    public static final String COMMAND_RESTORE_UNIT = "RESTORE_UNIT";
    public static final String COMMAND_SET_QUALITY = "SET_QUALITY";
    // endregion GM Commands
    // endregion Commands

    @Deprecated(since = "0.50.11", forRemoval = true)
    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

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
        } else if (command.equals(COMMAND_PARTS_REPORT)) { // Single Unit only
            new PartQualityReportDialog(gui.getFrame(), selectedUnit).setVisible(true);
        } else if (command.equals(COMMAND_SET_QUALITY)) {
            boolean reverse = gui.getCampaign().getCampaignOptions().isReverseQualityNames();
            Object[] possibilities = { PartQuality.QUALITY_A.toName(reverse), PartQuality.QUALITY_B.toName(reverse),
                                       PartQuality.QUALITY_C.toName(reverse), PartQuality.QUALITY_D.toName(reverse),
                                       PartQuality.QUALITY_E.toName(reverse), PartQuality.QUALITY_F.toName(reverse) };
            String quality = (String) JOptionPane.showInputDialog(gui.getFrame(),
                  "Choose the new quality level",
                  "Set Quality",
                  JOptionPane.PLAIN_MESSAGE,
                  null,
                  possibilities,
                  PartQuality.QUALITY_D.toName(reverse));

            if (quality == null || quality.isBlank()) {
                return;
            }

            PartQuality q = PartQuality.fromName(quality, reverse);
            for (Unit unit : units) {
                if (null != unit) {
                    unit.setQuality(q);
                    MekHQ.triggerEvent(new UnitChangedEvent(unit));
                }
            }
        } else if (command.equals(COMMAND_SELL)) {
            Money totalValue = Money.zero();
            List<Unit> sellableUnits = new ArrayList<>();
            for (Unit unit : units) {
                if (unit == null) {
                    continue;
                }
                if (!unit.isDeployed()) {
                    sellableUnits.add(unit);
                    totalValue = totalValue.plus(unit.getSellValue());
                }
            }

            if (sellableUnits.isEmpty()) {
                return;
            }

            Campaign campaign = gui.getCampaign();
            String commanderAddress = campaign.getCommanderAddress();
            Person logisticsAdmin = campaign.getSeniorAdminPerson(LOGISTICS);

            // Cancel is first (index 0), so closing dialog via X defaults to cancel
            List<String> buttons = List.of(
                  getFormattedTextAt(RESOURCE_BUNDLE, "sellUnit.buttonCancel"),
                  getFormattedTextAt(RESOURCE_BUNDLE, "sellUnit.buttonConfirm"));

            final int confirmDialogIndex = 1;

            String message;
            if (sellableUnits.size() == 1) {
                Unit unit = sellableUnits.get(0);
                message = getFormattedTextAt(
                      RESOURCE_BUNDLE,
                      "sellUnit.message.single",
                      commanderAddress,
                      unit.getName(),
                      unit.getSellValueBreakdown());
            } else {
                message = getFormattedTextAt(
                      RESOURCE_BUNDLE,
                      "sellUnit.message.multiple",
                      commanderAddress,
                      totalValue.toAmountString());
            }

            ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(
                  campaign,
                  logisticsAdmin,
                  null,
                  message,
                  buttons,
                  null,
                  null,
                  true);

            boolean wasConfirmed = dialog.getDialogChoice() == confirmDialogIndex;

            if (wasConfirmed) {
                for (Unit unit : sellableUnits) {
                    campaign.getQuartermaster().sellUnit(unit);
                }
            }
        } else if (command.equals(COMMAND_LOSS)) {
            for (Unit unit : units) {
                if (0 ==
                          JOptionPane.showConfirmDialog(null,
                                "Do you really want to consider " + unit.getName() + " a combat loss?",
                                "Remove Unit?",
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
            int selected = MathUtility.parseInt(command.split(":")[1], SITE_FIELD_WORKSHOP);
            boolean selectedIsValid = selected > -1 && selected < Unit.SITE_UNKNOWN;
            if (!selectedIsValid) {
                return;
            }

            boolean wasSiteChangeSuccessful = true;
            Campaign campaign = gui.getCampaign();
            if (selected >= Unit.SITE_FACILITY_MAINTENANCE &&
                      campaign.getCampaignOptions().getRentedFacilitiesCostRepairBays() > 0) {
                wasSiteChangeSuccessful = FacilityRentals.processBayChangeRequest(campaign, units, selected);
            }

            if (wasSiteChangeSuccessful) {
                for (Unit unit : units) {
                    if (!unit.isDeployed()) {
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

            if (!toRemove.isEmpty()) {
                String title = String.format(resources.getString("deleteUnitsCount.text"), toRemove.size());
                if (toRemove.size() == 1) {
                    title = toRemove.get(0).getName();
                }

                if (0 ==
                          JOptionPane.showConfirmDialog(null,
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
                    if (0 ==
                              JOptionPane.showConfirmDialog(null,
                                    "Do you really want to disband this unit " + unit.getName() + '?',
                                    "Disband Unit?",
                                    JOptionPane.YES_NO_OPTION)) {
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
            boolean isGM = command.contains("GM");

            HirePersonnelUnitAction hireAction = new HirePersonnelUnitAction(isGM);

            for (Unit unit : units) {
                List<Person> preExistingCrew = unit.getCrew();

                hireAction.execute(gui.getCampaign(), unit);

                boolean fixSkillLevels = false;

                SkillLevel skillLevel = REGULAR;
                if (command.contains("ELITE")) {
                    skillLevel = ELITE;
                    fixSkillLevels = true;
                } else if (command.contains("VETERAN")) {
                    skillLevel = VETERAN;
                    fixSkillLevels = true;
                } else if (command.contains("ULTRA_GREEN")) {
                    skillLevel = ULTRA_GREEN;
                    fixSkillLevels = true;
                } else if (command.contains("GREEN")) {
                    skillLevel = GREEN;
                    fixSkillLevels = true;
                }

                if (fixSkillLevels) {
                    for (Person person : unit.getCrew()) {
                        if (preExistingCrew.contains(person)) {
                            continue;
                        }

                        Campaign campaign = gui.getCampaign();
                        RandomSkillPreferences randomSkillPreferences = campaign.getRandomSkillPreferences();
                        boolean useExtraRandomness = randomSkillPreferences.randomizeSkill();

                        // We don't care about admin, doctor or tech settings, as they're not going to spawn here
                        overrideSkills(false,
                              false,
                              false,
                              campaign.getCampaignOptions().isUseArtillery(),
                              useExtraRandomness,
                              person,
                              person.getPrimaryRole(),
                              skillLevel);
                    }
                }
            }
        } else if (command.equals(COMMAND_CUSTOMIZE)) { // Single Unit only
            ((MekLabTab) gui.getTab(MHQTabType.MEK_LAB)).loadUnit(selectedUnit);
            gui.getTabMain().setSelectedIndex(gui.getTabMain().getTabCount() - 1);
        } else if (command.equals(COMMAND_CANCEL_CUSTOMIZE)) {
            Stream.of(units).filter(Unit::isRefitting).forEach(unit -> unit.getRefit().cancel());
        } else if (command.equals(COMMAND_REFIT_GM_COMPLETE)) {
            Stream.of(units).filter(Unit::isRefitting).forEach(unit -> unit.getRefit().succeed());
        } else if (command.equals(COMMAND_REFURBISH)) {
            for (Unit unit : units) {
                Refit refit = new Refit(unit, unit.getEntity(), false, true, false);
                gui.refitUnit(refit, false);
            }
        } else if (command.equals(COMMAND_REFIT_KIT)) { // Single Unit or Multiple of Units of the same type only
            ChooseRefitDialog crd = new ChooseRefitDialog(gui.getFrame(), true, gui.getCampaign(), selectedUnit);
            crd.setVisible(true);
            if (crd.isConfirmed()) {
                MekSummary summary = MekSummaryCache.getInstance()
                                           .getMek(crd.getSelectedRefit().getNewEntity().getShortNameRaw());
                if (summary != null) {
                    for (Unit unit : units) {
                        try {
                            Entity refitEntity = new MekFileParser(summary.getSourceFile(),
                                  summary.getEntryName()).getEntity();
                            if (refitEntity != null) {
                                Refit refit = new Refit(unit, refitEntity, crd.isCustomize(), false, false);
                                if (refit.checkFixable() == null) {
                                    gui.refitUnit(refit, false);
                                }
                            }
                        } catch (EntityLoadingException ex) {
                            LOGGER.error("", ex);
                        }
                    }
                }
            }
        } else if (command.equals(COMMAND_CHANGE_HISTORY)) { // Single Unit only
            MarkdownEditorDialog tad = new MarkdownEditorDialog(gui.getFrame(),
                  true,
                  "Edit Unit History",
                  selectedUnit.getHistory());
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
                  selectedUnit.getUtilizedCamouflage(gui.getCampaign()),
                  true);
            if (ccd.showDialog().isCancelled()) {
                return;
            }
            for (final Unit unit : units) {
                unit.getEntity().setCamouflage(ccd.getSelectedItem());
                MekHQ.triggerEvent(new UnitChangedEvent(unit));
            }
        } else if (command.equals(COMMAND_CANCEL_ORDER)) {
            for (Unit u : units) {
                Money refundAmount = u.getBuyCost()
                                           .multipliedBy(gui.getCampaign()
                                                               .getCampaignOptions()
                                                               .getCancelledOrderRefundMultiplier());
                gui.getCampaign().removeUnit(u.getId());
                gui.getCampaign()
                      .getFinances()
                      .credit(TransactionType.EQUIPMENT_PURCHASE,
                            gui.getCampaign().getLocalDate(),
                            refundAmount,
                            "refund for cancelled equipment sale");
            }
        } else if (command.equals(COMMAND_ARRIVE)) {
            for (Unit u : units) {
                u.setDaysToArrival(0);
            }
        } else if (command.equals(COMMAND_MOTHBALL)) {
            if (units.length > 1) {
                new MassMothballDialog(gui.getFrame(), units, gui.getCampaign(), false).setVisible(true);
            } else {
                Person tech = pickTechForMothballOrActivation(selectedUnit, "mothballing");
                MothballUnitAction mothballUnitAction = new MothballUnitAction(tech, false);
                mothballUnitAction.execute(gui.getCampaign(), selectedUnit);
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
        } else if (command.equals(COMMAND_ACTIVATE)) {
            if (units.length > 1) {
                new MassMothballDialog(gui.getFrame(), units, gui.getCampaign(), true).setVisible(true);
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
            BombsDialog dialog = new BombsDialog((IBomber) selectedUnit.getEntity(), gui.getCampaign(), gui.getFrame());
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
            String fluffName = (String) JOptionPane.showInputDialog(gui.getFrame(),
                  "Name for this unit?",
                  "Unit Name",
                  JOptionPane.QUESTION_MESSAGE,
                  null,
                  null,
                  selectedUnit.getFluffName());
            String oldFluffName = selectedUnit.getFluffName();
            selectedUnit.setFluffName((fluffName != null) ? fluffName : oldFluffName);
            if (fluffName != null) {
                MekHQ.triggerEvent(new UnitChangedEvent(selectedUnit));
            }
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
                // this is so we can have this show with a mixture of Mothballed and
                // non-Mothballed units
                if (!u.isMothballed()) {
                    mothballUnitAction.execute(gui.getCampaign(), u);
                    MekHQ.triggerEvent(new UnitChangedEvent(u));
                }
            }
        } else if (command.equals(COMMAND_GM_ACTIVATE)) {
            ActivateUnitAction activateUnitAction = new ActivateUnitAction(null, true);
            for (Unit u : units) {
                // this is so we can have this show with a mixture of Mothballed and
                // non-Mothballed units
                if (u.isMothballed()) {
                    activateUnitAction.execute(gui.getCampaign(), u);
                    MekHQ.triggerEvent(new UnitChangedEvent(u));
                }
            }
        } else if (command.startsWith(COMMAND_CHANGE_MAINTENANCE_MULTI)) {
            try {
                int multiplier = Integer.parseInt(command.substring(COMMAND_CHANGE_MAINTENANCE_MULTI.length() + 1));

                for (Unit u : units) {
                    if (!u.isSelfCrewed()) {
                        u.setMaintenanceMultiplier(multiplier);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        } else if (command.startsWith(COMMAND_PERFORM_AD_HOC_MAINTENANCE)) {
            final Campaign campaign = gui.getCampaign();
            final CampaignOptions campaignOptions = campaign.getCampaignOptions();
            final boolean isUseMaintenance = campaignOptions.isCheckMaintenance();
            final boolean techsUseAdmin = campaign.getCampaignOptions().isTechsUseAdministration();

            if (!isUseMaintenance) {
                return;
            }

            for (Unit unit : units) {
                if (!unit.requiresMaintenance()) {
                    campaign.addReport(TECHNICAL, String.format(resources.getString("maintenanceAdHoc.noNeed"),
                          unit.getHyperlinkedName()));
                    continue;
                }

                Maintenance.performImmediateMaintenance(campaign, unit);
            }
        }
    }

    private @Nullable Person pickTechForMothballOrActivation(Unit unit, String description) {
        Person tech = null;

        if (unit.isConventionalInfantry()) {
            return null;
        }

        if (unit.isSelfCrewed()) {
            if (unit.engineerResponsible().isPresent()) {
                tech = unit.engineerResponsible().get();
                return tech;
            }
        }

        if (!unit.isSelfCrewed() || isSelfCrewedButHasNoTech(unit)) {
            UUID id = gui.selectTech(unit, description, true);
            if (null != id) {
                tech = gui.getCampaign().getPerson(id);
                if (!tech.getTechUnits().isEmpty()) {
                    if (JOptionPane.YES_OPTION !=
                              JOptionPane.showConfirmDialog(gui.getFrame(),
                                    tech.getFullName() +
                                          " will not be able to perform maintenance on " +
                                          tech.getTechUnits().size() +
                                          " assigned units. Proceed?",
                                    "Unmaintained unit warning",
                                    JOptionPane.YES_NO_OPTION)) {
                        tech = null;
                    }
                }
            }
        }

        return tech;
    }

    private boolean isSelfCrewedButHasNoTech(Unit unit) {
        return unit.isSelfCrewed() && unit.engineerResponsible().isEmpty();
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        if (unitTable.getSelectedRowCount() == 0) {
            return Optional.empty();
        }

        // region Variable Declarations and Instantiations
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
        // endregion Variable Declarations and Instantiations

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
            // region Determine if to Display
            // this is used to determine whether to show parts of the GUI, especially for bulk selections
            boolean oneMothballed = false; // If at least one unit is mothballed, we want to show unit activation
            boolean oneMothballing = false; // If at least one unit is mothballing, we want to be able to cancel it
            boolean oneActive = false; // If at least one unit is active, we want to enable mothballing
            boolean oneDeployed = false; // If at least one unit is deployed, we want to show the remove deployment to
            // GMs
            boolean allDeployed = true; // don't show sell dialog if all units are deployed
            boolean oneAvailableUnitBelowMaxCrew = false; // If one unit isn't fully crewed, enable bulk hiring
            boolean oneNotPresent = false; // If a unit isn't present, enable instant arrival for GMs
            boolean oneHasIndividualCamo = false; // If a unit has a unique camo, allow it to be removed
            boolean allUnitsAreRepairable = true; // If all units can be repaired, allow the repair flag to be selected
            boolean areAllConventionalInfantry = true; // Conventional infantry can be disbanded, but no others
            boolean noConventionalInfantry = true; // Conventional infantry can't be repaired/salvaged
            boolean areAllRepairFlagged = true; // If everyone has the repair flag, then we show the repair flag box as
            // selected
            boolean areAllSalvageFlagged = true; // Same as above, but with the salvage flag
            boolean allSameModel = true; // If everyone is the exact same unit and model of that unit
            boolean oneRefitting = false; // If any one selected unit is refitting
            boolean allAvailable = true; // If everyone is available
            boolean allAvailableIgnoreRefit = true; // If everyone is available
            final String model = unit.getEntity().getShortNameRaw();

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
            // endregion Determine if to Display

            // change the location
            menu = new JMenu("Change Site");
            boolean allSameSite = StaticChecks.areAllSameSite(units);

            for (int i = 0; i < Unit.SITE_UNKNOWN; i++) {
                cbMenuItem = new JCheckBoxMenuItem(Unit.getSiteName(i));
                cbMenuItem.setToolTipText(wordWrap(Unit.getSiteToolTipText(i)));
                if (allSameSite && unit.getSite() == i) {
                    cbMenuItem.setSelected(true);
                } else {
                    cbMenuItem.setActionCommand(COMMAND_CHANGE_SITE + ':' + i);
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
                } else if (unit.getEntity().isSupportVehicle() &&
                                 (unit.getEntity().getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT)) {
                    // Small SVs can configure ammo only if they have weapons that have
                    // inferno ammo available
                    if (unit.getEntity()
                              .getWeaponList()
                              .stream()
                              .anyMatch(m -> (m.getType() instanceof InfantryWeapon) &&
                                                   ((InfantryWeapon) m.getType()).hasInfernoAmmo())) {
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
                        for (AmmoType ammoType : Utilities.getMunitionsFor(unit.getEntity(),
                              curType,
                              gui.getCampaign().getCampaignOptions().getTechLevel())) {
                            cbMenuItem = new JCheckBoxMenuItem(ammoType.getDesc());
                            if (ammoType.equals(curType)) {
                                cbMenuItem.setSelected(true);
                            } else {
                                cbMenuItem.addActionListener(evt -> {
                                    IUnitAction swapAmmoTypeAction = new SwapAmmoTypeAction(ammo, ammoType);
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

                cbMenuItem = new JCheckBoxMenuItem("Strip");
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

            JMenuHelpers.addMenuIfNonEmpty(popup, new AssignUnitToForceMenu(gui.getCampaign(), units));

            // if we're using maintenance and have selected something that requires
            // maintenance and
            // isn't mothballed or being mothballed
            if (gui.getCampaign().getCampaignOptions().isCheckMaintenance()) {
                menuItem = new JMenu(resources.getString("maintenanceExtraTime.text"));

                for (int x = 1; x <= 4; x++) {
                    JMenuItem maintenanceMultiplierItem = new JCheckBoxMenuItem("x" + x);

                    // if we've got just one unit selected,
                    // have the courtesy to show the multiplier if relevant
                    if (oneSelected && (unit.getMaintenanceMultiplier() == x)) {
                        maintenanceMultiplierItem.setSelected(true);
                    }

                    maintenanceMultiplierItem.setActionCommand(COMMAND_CHANGE_MAINTENANCE_MULTI + ':' + x);
                    maintenanceMultiplierItem.addActionListener(this);
                    maintenanceMultiplierItem.setEnabled(!(oneSelected && unit.isSelfCrewed()));
                    menuItem.add(maintenanceMultiplierItem);
                }

                popup.add(menuItem);

                menuItem = new JMenuItem(resources.getString("maintenanceAdHoc.text"));
                menuItem.setActionCommand(COMMAND_PERFORM_AD_HOC_MAINTENANCE);
                menuItem.addActionListener(this);
                menuItem.setEnabled(oneSelected && unit.getDaysSinceMaintenance() != 0);

                popup.add(menuItem);
            }

            if (oneSelected && unit.requiresMaintenance()) {
                menuItem = new JMenuItem("Show Last Maintenance Report");
                menuItem.setActionCommand(COMMAND_MAINTENANCE_REPORT);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            if (oneSelected && !unit.isMothballed() && gui.getCampaign().getCampaignOptions().isUsePeacetimeCost()) {
                menuItem = new JMenuItem("Show Monthly Supply Cost Report");
                menuItem.setActionCommand(COMMAND_SUPPLY_COST);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            if (oneSelected && gui.getCampaign().getCampaignOptions().isCheckMaintenance()) {
                menuItem = new JMenuItem("Show Part Quality Report");
                menuItem.setActionCommand(COMMAND_PARTS_REPORT);
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
                if (allSameModel &&
                          allAvailable &&
                          ((unit.getEntity() instanceof Mek) ||
                                 (unit.getEntity() instanceof Tank) ||
                                 (unit.getEntity() instanceof Aero) ||
                                 ((unit.getEntity() instanceof Infantry)))) {
                    menuItem = new JMenuItem(unit.getEntity().isOmni() ?
                                                   "Choose configuration..." :
                                                   "Refit/Customize...");
                    menuItem.setActionCommand(COMMAND_REFIT_KIT);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }

                if (allSameModel &&
                          allAvailable &&
                          ((unit.getEntity() instanceof Mek) ||
                                 (unit.getEntity() instanceof Tank) ||
                                 (unit.getEntity() instanceof Aero) ||
                                 ((unit.getEntity() instanceof Infantry) || (unit.getEntity() instanceof ProtoMek)))) {
                    menuItem = new JMenuItem("Refurbish Unit");
                    menuItem.setActionCommand(COMMAND_REFURBISH);
                    menuItem.addActionListener(this);
                    menu.add(menuItem);
                }

                if (oneSelected && gui.hasTab(MHQTabType.MEK_LAB)) {
                    menuItem = new JMenuItem("Customize in Mek Lab...");
                    menuItem.setActionCommand(COMMAND_CUSTOMIZE);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(allAvailable && !(unit.getEntity() instanceof GunEmplacement));
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
            if (gui.getCampaign().getCampaignOptions().getPersonnelMarketStyle() != MEKHQ) {
                if (oneAvailableUnitBelowMaxCrew) {
                    menuItem = new JMenuItem(resources.getString("hireMinimumComplement.text"));
                    menuItem.setActionCommand(COMMAND_HIRE_FULL);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }
            }

            if (Stream.of(units).allMatch(u -> u.getCamouflage().equals(units[0].getCamouflage()))) {
                menuItem = new JMenuItem(gui.getResourceMap().getString("customizeMenu.individualCamo.text"));
                menuItem.setActionCommand(COMMAND_INDI_CAMO);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            if (!oneSelected && oneHasIndividualCamo) {
                menuItem = new JMenuItem(gui.getResourceMap().getString("customizeMenu.removeIndividualCamo.text"));
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

            if (oneSelected && gui.getCampaign().getCampaignOptions().isUseQuirks()) {
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

                popup.add(new ExportUnitSpriteMenu(gui.getFrame(), gui.getCampaign(), unit));
            }

            // sell unit
            if (!allDeployed && gui.getCampaign().getCampaignOptions().isSellUnits()) {
                popup.addSeparator();
                menuItem = new JMenuItem("Sell Unit");
                menuItem.setActionCommand(COMMAND_SELL);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            // region GM Mode
            //  - only show to GMs
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

                    boolean enable = true;
                    int scenarioId = unit.getScenarioId();
                    Scenario scenario = gui.getCampaign().getScenario(scenarioId);

                    if (scenario != null && scenario.getHasTrack()) {
                        enable = false;
                    }

                    menuItem.setEnabled(enable);
                    menu.add(menuItem);
                }

                if (oneAvailableUnitBelowMaxCrew) {
                    JMenu menuMinimumComplement = new JMenu(resources.getString("addMinimumComplement.text"));

                    menuItem = new JMenuItem(resources.getString("addMinimumComplementRandom.text"));
                    menuItem.setActionCommand(COMMAND_HIRE_FULL_GM);
                    menuItem.addActionListener(this);
                    menuMinimumComplement.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("addMinimumComplementElite.text"));
                    menuItem.setActionCommand(COMMAND_HIRE_FULL_GM_ELITE);
                    menuItem.addActionListener(this);
                    menuMinimumComplement.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("addMinimumComplementVeteran.text"));
                    menuItem.setActionCommand(COMMAND_HIRE_FULL_GM_VETERAN);
                    menuItem.addActionListener(this);
                    menuMinimumComplement.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("addMinimumComplementRegular.text"));
                    menuItem.setActionCommand(COMMAND_HIRE_FULL_GM_REGULAR);
                    menuItem.addActionListener(this);
                    menuMinimumComplement.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("addMinimumComplementGreen.text"));
                    menuItem.setActionCommand(COMMAND_HIRE_FULL_GM_GREEN);
                    menuItem.addActionListener(this);
                    menuMinimumComplement.add(menuItem);

                    menuItem = new JMenuItem(resources.getString("addMinimumComplementUltraGreen.text"));
                    menuItem.setActionCommand(COMMAND_HIRE_FULL_GM_ULTRA_GREEN);
                    menuItem.addActionListener(this);
                    menuMinimumComplement.add(menuItem);

                    menu.add(menuMinimumComplement);
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
            // endregion GM Mode
        }

        return Optional.of(popup);
    }

    private void addCustomUnitTag(Unit... units) {
        String sCustomsDirCampaign = MHQConstants.CUSTOM_MEKFILES_DIRECTORY_PATH + gui.getCampaign().getName() + '/';
        File customsDir = new File(MHQConstants.CUSTOM_MEKFILES_DIRECTORY_PATH);
        if (!customsDir.exists()) {
            if (!customsDir.mkdir()) {
                LOGGER.error("Unable to create directory {} to hold custom units, cannot assign custom unit tag",
                      MHQConstants.CUSTOM_MEKFILES_DIRECTORY_PATH);
                return;
            }
        }
        File customsDirCampaign = new File(sCustomsDirCampaign);
        if (!customsDirCampaign.exists()) {
            if (!customsDir.mkdir()) {
                LOGGER.error("Unable to create directory {} to hold custom units, cannot assign custom unit tag",
                      sCustomsDirCampaign);
                return;
            }
        }
        for (Unit unit : units) {
            Entity entity = unit.getEntity();
            String unitName = entity.getShortNameRaw();
            String fileExtension = entity instanceof Mek ? ".mtf" : ".blk";
            String fileOutName = MHQConstants.CUSTOM_MEKFILES_DIRECTORY_PATH +
                                       File.separator +
                                       unitName +
                                       fileExtension;
            String fileNameCampaign = sCustomsDirCampaign + File.separator + unitName + fileExtension;

            // if this file already exists then don't overwrite it, or we will end up with a bunch of copies
            if ((new File(fileOutName)).exists() || (new File(fileNameCampaign)).exists()) {
                JOptionPane.showMessageDialog(null,
                      "A file already exists for this unit, cannot tag as custom. (Unit name and model)",
                      "File Already Exists",
                      JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (entity instanceof Mek) {
                try (OutputStream os = new FileOutputStream(fileNameCampaign); PrintStream p = new PrintStream(os)) {

                    p.println(((Mek) entity).getMtf());
                } catch (Exception e) {
                    LOGGER.error("", e);
                }
            } else {
                try {
                    BLKFile.encode(fileNameCampaign, entity);
                } catch (EntitySavingException e) {
                    LOGGER.error("Error encoding unit {}", unit.getName(), e);
                    return;
                }
            }
            gui.getCampaign().addCustom(unitName);
        }
        MekSummaryCache.refreshUnitData(false);
    }
}
