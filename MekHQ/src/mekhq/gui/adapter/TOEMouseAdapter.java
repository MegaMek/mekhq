/*
 * Copyright (c) 2018-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.dialogs.CamoChooserDialog;
import megamek.common.EntityWeightClass;
import megamek.common.GunEmplacement;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.NetworkChangedEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.HangarSorter;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.dialog.ForceTemplateAssignmentDialog;
import mekhq.gui.dialog.MarkdownEditorDialog;
import mekhq.gui.dialog.iconDialogs.LayeredForceIconDialog;
import mekhq.gui.menus.ExportUnitSpriteMenu;
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.gui.utilities.StaticChecks;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.util.*;

public class TOEMouseAdapter extends JPopupMenuAdapter {
    private static final MMLogger logger = MMLogger.create(TOEMouseAdapter.class);

    private final CampaignGUI gui;
    private final JTree tree;

    protected TOEMouseAdapter(CampaignGUI gui, JTree tree) {
        this.gui = gui;
        this.tree = tree;
    }

    public static void connect(CampaignGUI gui, JTree tree) {
        new TOEMouseAdapter(gui, tree)
                .connect(tree);
    }

    // Named Constants for various commands
    // Force-related
    private static final String FORCE = "FORCE";
    private static final String ADD_FORCE = "ADD_FORCE";
    private static final String REMOVE_FORCE = "REMOVE_FORCE";
    private static final String DEPLOY_FORCE = "DEPLOY_FORCE";
    private static final String UNDEPLOY_FORCE = "UNDEPLOY_FORCE";

    private static final String COMMAND_ADD_FORCE = "ADD_FORCE|FORCE|empty|";
    private static final String COMMAND_DEPLOY_FORCE = "DEPLOY_FORCE|FORCE|";
    private static final String COMMAND_REMOVE_FORCE = "REMOVE_FORCE|FORCE|empty|";
    private static final String COMMAND_UNDEPLOY_FORCE = "UNDEPLOY_FORCE|FORCE|empty|";

    // Unit-related
    private static final String UNIT = "UNIT";
    private static final String ADD_UNIT = "ADD_UNIT";
    private static final String ASSIGN_TO_SHIP = "ASSIGN_TO_SHIP";
    private static final String DEPLOY_UNIT = "DEPLOY_UNIT";
    private static final String GOTO_UNIT = "GOTO_UNIT";
    private static final String REMOVE_UNIT = "REMOVE_UNIT";
    private static final String UNASSIGN_FROM_SHIP = "UNASSIGN_FROM_SHIP";
    private static final String UNDEPLOY_UNIT = "UNDEPLOY_UNIT";

    private static final String COMMAND_ADD_UNIT = "ADD_UNIT|FORCE|";
    private static final String COMMAND_ASSIGN_TO_SHIP = "ASSIGN_TO_SHIP|UNIT|";
    private static final String COMMAND_REMOVE_UNIT = "REMOVE_UNIT|UNIT|empty|";
    private static final String COMMAND_DEPLOY_UNIT = "DEPLOY_UNIT|UNIT|";
    private static final String COMMAND_UNASSIGN_FROM_SHIP = "UNASSIGN_FROM_SHIP|UNIT|empty|";
    private static final String COMMAND_UNDEPLOY_UNIT = "UNDEPLOY_UNIT|UNIT|empty|";
    private static final String COMMAND_GOTO_UNIT = "GOTO_UNIT|UNIT|empty|";

    // Tech-Related
    private static final String ADD_LANCE_TECH = "ADD_LANCE_TECH";
    private static final String REMOVE_LANCE_TECH = "REMOVE_LANCE_TECH";

    private static final String COMMAND_ADD_LANCE_TECH = "ADD_LANCE_TECH|FORCE|";
    private static final String COMMAND_REMOVE_LANCE_TECH = "REMOVE_LANCE_TECH|FORCE|";

    // Commander-Related
    private static final String SET_LANCE_COMMANDER = "SET_LANCE_COMMANDER";
    private static final String COMMAND_SET_LANCE_COMMANDER = "SET_LANCE_COMMANDER|FORCE|";

    // Icons and Descriptions
    private static final String CHANGE_CAMO = "CHANGE_CAMO";
    private static final String CHANGE_DESC = "CHANGE_DESC";
    private static final String CHANGE_ICON = "CHANGE_ICON";
    private static final String COPY_ICON = "COPY_ICON";
    private static final String PASTE_ICON = "PASTE_ICON";
    private static final String SUBFORCES_PASTE_ICON = "SUBFORCES_PASTE_ICON";
    private static final String CHANGE_NAME = "CHANGE_NAME";
    private static final String CHANGE_COMBAT_STATUS = "CHANGE_COMBAT_STATUS";
    private static final String CHANGE_COMBAT_STATUSES = "CHANGE_COMBAT_STATUSES";
    private static final String CHANGE_CONVOY_STATUS = "CHANGE_CONVOY_STATUS";

    private static final String COMMAND_CHANGE_FORCE_CAMO = "CHANGE_CAMO|FORCE|empty|";
    private static final String COMMAND_CHANGE_FORCE_DESC = "CHANGE_DESC|FORCE|empty|";
    private static final String COMMAND_CHANGE_FORCE_ICON = "CHANGE_ICON|FORCE|empty|";
    private static final String COMMAND_COPY_FORCE_ICON = "COPY_ICON|FORCE|empty|";
    private static final String COMMAND_PASTE_FORCE_ICON = "PASTE_ICON|FORCE|empty|";
    private static final String COMMAND_SUBFORCES_PASTE_FORCE_ICON = "SUBFORCES_PASTE_ICON|FORCE|empty|";
    private static final String COMMAND_CHANGE_FORCE_NAME = "CHANGE_NAME|FORCE|empty|";
    private static final String COMMAND_CHANGE_FORCE_COMBAT_STATUS = "CHANGE_COMBAT_STATUS|FORCE|empty|";
    private static final String COMMAND_CHANGE_FORCE_COMBAT_STATUSES = "CHANGE_COMBAT_STATUSES|FORCE|empty|";
    private static final String COMMAND_CHANGE_FORCE_CONVOY_STATUS = "CHANGE_CONVOY_STATUS|FORCE|empty|";

    private static final String COMMAND_OVERRIDE_FORCE_FORMATION_LEVEL = "OVERRIDE_FORMATION_LEVEL|FORCE|FORMATION_LEVEL|";

    // C3 Network-related
    private static final String C3I = "C3I";
    private static final String NC3 = "NC3";
    private static final String ADD_NETWORK = "ADD_NETWORK";
    private static final String ADD_SLAVES = "ADD_SLAVES";
    private static final String DISBAND_NETWORK = "DISBAND_NETWORK";
    private static final String REMOVE_C3 = "REMOVE_C3";
    private static final String REMOVE_NETWORK = "REMOVE_NETWORK";
    private static final String SET_MM = "SET_MM";
    private static final String SET_IND_M = "SET_IND_M";

    private static final String COMMAND_ADD_SLAVE = "ADD_SLAVES|UNIT|";
    private static final String COMMAND_REMOVE_C3 = "REMOVE_C3|UNIT|empty|";
    private static final String COMMAND_SET_CO_MASTER = "SET_MM|UNIT|empty|";
    private static final String COMMAND_SET_IND_MASTER = "SET_IND_M|UNIT|empty|";
    private static final String COMMAND_CREATE_C3I = "C3I|UNIT|empty|";
    private static final String COMMAND_CREATE_NC3 = "NC3|UNIT|empty|";
    private static final String COMMAND_ADD_TO_NETWORK = "ADD_NETWORK|UNIT|";
    private static final String COMMAND_DISBAND_NETWORK = "DISBAND_NETWORK|UNIT|empty|";
    private static final String COMMAND_REMOVE_FROM_NETWORK = "REMOVE_NETWORK|UNIT|empty|";

    // Other
    private static final String GOTO_PILOT = "GOTO_PILOT";
    private static final String COMMAND_GOTO_PILOT = "GOTO_PILOT|UNIT|empty|";

    // String tokens for dialog boxes used for transport loading
    private static final String LOAD_UNITS_DIALOG_TEXT = "You are deploying a Transport with units assigned to it. \n"
            + "Would you also like to deploy these units?";
    private static final String LOAD_UNITS_DIALOG_TITLE = "Also deploy transported units?";

    private static final String ASSIGN_FORCE_TRN_TITLE = "Assign Force to Transport Ship";
    private static final String UNASSIGN_FORCE_TRN_TITLE = "Unassign Force from Transport Ship";
    private static final String MEK_CARRIERS = "Mek Transports";
    private static final String PROTOMEK_CARRIERS = "ProtoMek Transports";
    private static final String LVEE_CARRIERS = "Light Vehicle Transports";
    private static final String HVEE_CARRIERS = "Heavy Vehicle Transports";
    private static final String SHVEE_CARRIERS = "SuperHeavy Vehicle Transports";
    private static final String BA_CARRIERS = "Battle Armor Transports";
    private static final String INFANTRY_CARRIERS = "Infantry Transports";
    private static final String ASF_CARRIERS = "Aerospace Fighter Transports";
    private static final String SC_CARRIERS = "Small Craft Transports";
    private static final String DS_CARRIERS = "DropShip Transports";
    private static final String VARIABLE_TRANSPORT = "%s Transports";

    @Override
    public void actionPerformed(ActionEvent action) {
        StringTokenizer st = new StringTokenizer(action.getActionCommand(), "|");
        String command = st.nextToken();
        String type = st.nextToken();
        String target = st.nextToken();
        String forceId = st.nextToken();

        Vector<Force> forces = new Vector<>();
        Vector<Unit> units = new Vector<>();

        if (type.equals(TOEMouseAdapter.FORCE)) {
            Force force = gui.getCampaign().getForce(Integer.parseInt(forceId));
            if (null != force) {
                forces.add(force);
            }
        }

        if (type.equals(TOEMouseAdapter.UNIT)) {
            Unit unit = gui.getCampaign().getUnit(UUID.fromString(forceId));
            if (null != unit) {
                units.add(unit);
            }
        }

        if (type.equals(TOEMouseAdapter.FORCE)) {
            Vector<Force> newForces = new Vector<>();
            for (Force force : forces) {
                boolean duplicate = false;
                for (Force otherForce : forces) {
                    if (otherForce.getId() == force.getId()) {
                        continue;
                    }

                    if (otherForce.isAncestorOf(force)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    newForces.add(force);
                }
            }
            forces = newForces;
        }

        // TODO : eliminate any forces that are descendants of other forces in the
        // vector
        final Force singleForce = forces.isEmpty() ? null : forces.get(0);
        final Unit singleUnit = units.isEmpty() ? null : units.get(0);

        if (command.contains(TOEMouseAdapter.ADD_FORCE)) {
            if (null != singleForce) {
                String name = (String) JOptionPane.showInputDialog(null,
                        "Enter the force name", "Force Name",
                        JOptionPane.PLAIN_MESSAGE, null, null, "My Lance");
                if (null != name) {
                    Force f = new Force(name);
                    gui.getCampaign().addForce(f, singleForce);

                    MekHQ.triggerEvent(new OrganizationChangedEvent(gui.getCampaign(), f));
                }
            }
        } else if (command.contains(TOEMouseAdapter.ADD_LANCE_TECH)) {
            if (null != singleForce) {
                Person tech = gui.getCampaign().getPerson(UUID.fromString(target));
                if (null != tech) {
                    if (singleForce.getTechID() != null) {
                        Person oldTech = gui.getCampaign().getPerson(singleForce.getTechID());
                        oldTech.clearTechUnits();
                        ServiceLogger.removedFrom(oldTech, gui.getCampaign().getLocalDate(), singleForce.getName());
                    }
                    singleForce.setTechID(tech.getId());

                    ServiceLogger.assignedTo(tech, gui.getCampaign().getLocalDate(), singleForce.getName());

                    if (singleForce.getAllUnits(false) != null) {
                        StringBuilder cantTech = new StringBuilder();
                        for (UUID uuid : singleForce.getAllUnits(false)) {
                            Unit u = gui.getCampaign().getUnit(uuid);
                            if (u != null) {
                                if (tech.canTech(u.getEntity())) {
                                    if (null != u.getTech()) {
                                        u.removeTech();
                                    }

                                    u.setTech(tech);
                                } else {
                                    cantTech.append(tech.getFullName()).append(" cannot maintain ").append(u.getName())
                                            .append('\n');
                                }
                            }
                        }

                        if (!cantTech.toString().isBlank()) {
                            cantTech.append("You will need to assign a tech manually.");
                            JOptionPane.showMessageDialog(null, cantTech.toString(), "Warning",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }

                MekHQ.triggerEvent(new OrganizationChangedEvent(gui.getCampaign(), singleForce));
            }
        } else if (command.contains(TOEMouseAdapter.SET_LANCE_COMMANDER)) {
            if (null != singleForce) {
                singleForce.setOverrideForceCommanderID(UUID.fromString(target));
                singleForce.updateCommander(gui.getCampaign());
                gui.getTOETab().refreshForceView();
            }
        } else if (command.contains(TOEMouseAdapter.ASSIGN_TO_SHIP)) {
            Unit ship = gui.getCampaign().getUnit(UUID.fromString(target));
            if ((!units.isEmpty()) && (ship != null)) {
                StringJoiner cantLoad = new StringJoiner(", ");
                String cantLoadReasons = StaticChecks.canTransportShipCarry(units, ship);
                if (cantLoadReasons != null) {
                    cantLoad.add(ship.getName() + " cannot load selected units for the following reasons: \n" +
                            cantLoadReasons);
                    // If the ship can't load the selected units, display a nag with the reasons why
                    JOptionPane.showMessageDialog(null, cantLoad, "Warning", JOptionPane.WARNING_MESSAGE);
                } else {
                    // First, remove the units from any other Transport they might be on
                    for (Unit u : units) {
                        if (u.hasTransportShipAssignment()) {
                            Unit oldShip = u.getTransportShipAssignment().getTransportShip();
                            oldShip.unloadFromTransportShip(u);
                            MekHQ.triggerEvent(new UnitChangedEvent(oldShip));
                        }
                    }
                    // now load the units
                    ship.loadTransportShip(units);
                    MekHQ.triggerEvent(new UnitChangedEvent(ship));
                    gui.getTOETab().refreshForceView();
                }
            }
        } else if (command.contains(UNASSIGN_FROM_SHIP)) {
            for (Unit u : units) {
                if (u.hasTransportShipAssignment()) {
                    Unit oldShip = u.getTransportShipAssignment().getTransportShip();
                    oldShip.unloadFromTransportShip(u);
                    MekHQ.triggerEvent(new UnitChangedEvent(oldShip));
                }
            }
            gui.getTOETab().refreshForceView();
        } else if (command.contains(TOEMouseAdapter.ADD_UNIT)) {
            if (null != singleForce) {
                Unit u = gui.getCampaign().getUnit(UUID.fromString(target));
                if (null != u) {
                    gui.getCampaign().addUnitToForce(u, singleForce.getId());
                }
            }
        } else if (command.contains(TOEMouseAdapter.UNDEPLOY_FORCE)) {
            for (Force force : forces) {
                gui.undeployForce(force);
                // Event triggered from undeployForce
            }
        } else if (command.contains(TOEMouseAdapter.DEPLOY_FORCE)) {
            int sid = Integer.parseInt(target);
            Scenario scenario = gui.getCampaign().getScenario(sid);

            if (scenario instanceof AtBDynamicScenario) {
                new ForceTemplateAssignmentDialog(gui, forces, null, (AtBDynamicScenario) scenario);
            } else {
                for (Force force : forces) {
                    gui.undeployForce(force);
                    force.clearScenarioIds(gui.getCampaign(), true);
                    if (null != scenario) {
                        scenario.addForces(force.getId());
                        force.setScenarioId(scenario.getId(), gui.getCampaign());
                    }
                    MekHQ.triggerEvent(new DeploymentChangedEvent(force, scenario));
                }
            }
        } else if (command.contains(CHANGE_ICON)) {
            if (singleForce != null) {
                final LayeredForceIconDialog layeredForceIconDialog = new LayeredForceIconDialog(
                        gui.getFrame(), singleForce.getForceIcon());
                if (layeredForceIconDialog.showDialog().isConfirmed()
                        && (layeredForceIconDialog.getSelectedItem() != null)) {
                    singleForce.setForceIcon(layeredForceIconDialog.getSelectedItem());
                    MekHQ.triggerEvent(new OrganizationChangedEvent(gui.getCampaign(), singleForce));
                }
            }
        } else if (command.contains(COPY_ICON)) {
            if (singleForce != null) {
                gui.setCopyForceIcon(singleForce.getForceIcon().clone());
            }
        } else if (command.contains(PASTE_ICON)) {
            if (gui.getCopyForceIcon() == null) {
                return;
            }

            final boolean subforces = command.contains(SUBFORCES_PASTE_ICON);
            for (final Force force : forces) {
                force.setForceIcon(gui.getCopyForceIcon().clone(), subforces);
            }
            gui.getTOETab().refreshForceView();
        } else if (command.contains(CHANGE_CAMO)) {
            if (singleForce != null) {
                CamoChooserDialog ccd = new CamoChooserDialog(gui.getFrame(),
                        singleForce.getCamouflageOrElse(gui.getCampaign().getCamouflage()), true);
                if (ccd.showDialog().isCancelled()) {
                    return;
                }
                singleForce.setCamouflage(ccd.getSelectedItem());
                MekHQ.triggerEvent(new OrganizationChangedEvent(gui.getCampaign(), singleForce));
            }
        } else if (command.contains(CHANGE_NAME)) {
            if (null != singleForce) {
                String name = (String) JOptionPane.showInputDialog(null,
                        "Enter the force name", "Force Name",
                        JOptionPane.PLAIN_MESSAGE, null, null,
                        singleForce.getName());
                if (name != null) {
                    singleForce.setName(name);
                }
                MekHQ.triggerEvent(new OrganizationChangedEvent(gui.getCampaign(), singleForce));
            }
        } else if (action.getActionCommand().startsWith(COMMAND_OVERRIDE_FORCE_FORMATION_LEVEL)) {
            if (singleForce == null) {
                return;
            }

            FormationLevel formationLevel = FormationLevel.parseFromString(st.nextToken());
            singleForce.setOverrideFormationLevel(formationLevel);

            Force.populateFormationLevelsFromOrigin(gui.getCampaign());
        } else if (command.contains(TOEMouseAdapter.CHANGE_DESC)) {
            if (null != singleForce) {
                MarkdownEditorDialog tad = new MarkdownEditorDialog(gui.getFrame(), true,
                        "Edit Force Description",
                        singleForce.getDescription());
                tad.setVisible(true);
                if (tad.wasChanged()) {
                    singleForce.setDescription(tad.getText());
                    MekHQ.triggerEvent(new OrganizationChangedEvent(gui.getCampaign(), singleForce));
                }
            }
        } else if (command.contains(TOEMouseAdapter.CHANGE_COMBAT_STATUS)) {
            if (singleForce == null) {
                return;
            }

            final boolean combatForce = !singleForce.isCombatForce();
            final boolean subforces = command.contains(TOEMouseAdapter.CHANGE_COMBAT_STATUSES);
            for (final Force force : forces) {
                force.setCombatForce(combatForce, subforces);
                force.setConvoyForce(!combatForce, subforces);
            }
            gui.getTOETab().refreshForceView();
        } else if (command.contains(TOEMouseAdapter.CHANGE_CONVOY_STATUS)) {
            if (singleForce == null) {
                return;
            }

            final boolean convoyForce = !singleForce.isConvoyForce();
            for (final Force force : forces) {
                force.setConvoyForce(convoyForce, true);
            }
            gui.getTOETab().refreshForceView();
        } else if (command.contains(TOEMouseAdapter.REMOVE_FORCE)) {
            for (Force force : forces) {
                if (null != force && null != force.getParentForce()) {
                    if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(
                            null,
                            "Are you sure you want to delete "
                                    + force.getFullName() + '?',
                            "Delete Force?", JOptionPane.YES_NO_OPTION)) {
                        return;
                    }
                    // Clear any transport assignments of units in the deleted force
                    clearTransportAssignment(force.getAllUnits(false));

                    gui.getCampaign().removeForce(force);
                }
            }
        } else if (command.contains(TOEMouseAdapter.REMOVE_LANCE_TECH)) {
            if (null != singleForce && singleForce.getTechID() != null) {
                Person oldTech = gui.getCampaign().getPerson(singleForce.getTechID());
                oldTech.clearTechUnits();

                ServiceLogger.removedFrom(oldTech, gui.getCampaign().getLocalDate(), singleForce.getName());

                if (singleForce.getAllUnits(false) != null) {
                    for (UUID uuid : singleForce.getAllUnits(false)) {
                        Unit u = gui.getCampaign().getUnit(uuid);
                        if (null != u.getTech()) {
                            u.removeTech();
                        }
                    }
                }
                singleForce.setTechID(null);
                MekHQ.triggerEvent(new OrganizationChangedEvent(gui.getCampaign(), singleForce));
            }
        } else if (command.contains(TOEMouseAdapter.REMOVE_UNIT)) {
            for (Unit unit : units) {
                if (null != unit) {
                    Force parentForce = gui.getCampaign().getForceFor(unit);
                    if (null != parentForce) {
                        gui.getCampaign().removeUnitFromForce(unit);
                        if (null != parentForce.getTechID()) {
                            unit.removeTech();
                        }
                    }
                    // Clear any transport assignments of units in the deleted force
                    clearTransportAssignment(unit);

                    MekHQ.triggerEvent(new OrganizationChangedEvent(gui.getCampaign(), parentForce, unit));
                }
            }
        } else if (command.contains(TOEMouseAdapter.UNDEPLOY_UNIT)) {
            for (Unit unit : units) {
                gui.undeployUnit(unit);
                // Event triggered from undeployUnit
            }
        } else if (command.contains(TOEMouseAdapter.GOTO_UNIT)) {
            if (null != singleUnit) {
                gui.focusOnUnit(singleUnit.getId());
            }
        } else if (command.contains(TOEMouseAdapter.GOTO_PILOT)) {
            if (null != singleUnit && null != singleUnit.getCommander()) {
                gui.focusOnPerson(singleUnit.getCommander().getId());
            }
        } else if (command.contains(TOEMouseAdapter.DEPLOY_UNIT)) {
            int sid = Integer.parseInt(target);
            Scenario scenario = gui.getCampaign().getScenario(sid);
            if (scenario instanceof AtBDynamicScenario) {
                new ForceTemplateAssignmentDialog(gui, null, units, (AtBDynamicScenario) scenario);
            } else {
                HashSet<Unit> extraUnits = new HashSet<>();
                for (Unit unit : units) {
                    if (null != unit && null != scenario) {
                        if (unit.hasTransportedUnits()) {
                            // Prompt the player to also deploy any units transported by this one
                            int optionChoice = JOptionPane.showConfirmDialog(null,
                                    TOEMouseAdapter.LOAD_UNITS_DIALOG_TEXT,
                                    TOEMouseAdapter.LOAD_UNITS_DIALOG_TITLE, JOptionPane.YES_NO_OPTION);
                            if (optionChoice == JOptionPane.YES_OPTION) {
                                extraUnits.addAll(unit.getTransportedUnits());
                            }
                        }
                        scenario.addUnit(unit.getId());
                        unit.setScenarioId(scenario.getId());
                        MekHQ.triggerEvent(new DeploymentChangedEvent(unit, scenario));
                    }
                }
                // Now add the extras, if there are any
                for (Unit extra : extraUnits) {
                    if (null != extra && null != scenario) {
                        scenario.addUnit(extra.getId());
                        extra.setScenarioId(scenario.getId());
                        MekHQ.triggerEvent(new DeploymentChangedEvent(extra, scenario));
                    }
                }
            }
        } else if (command.contains(TOEMouseAdapter.C3I)) {
            // don't set them directly, set the C3i UUIDs and then
            // run gui.refreshNetworks on the campaign
            // TODO: is that too costly?
            Vector<String> uuids = new Vector<>();
            for (Unit unit : units) {
                if (null == unit.getEntity()) {
                    continue;
                }
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }
            for (int pos = 0; pos < uuids.size(); pos++) {
                for (Unit unit : units) {
                    if (null == unit.getEntity()) {
                        continue;
                    }
                    unit.getEntity().setC3iNextUUIDAsString(pos,
                            uuids.get(pos));
                }
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains(TOEMouseAdapter.NC3)) {
            Vector<String> uuids = new Vector<>();
            for (Unit unit : units) {
                if (null == unit.getEntity()) {
                    continue;
                }
                uuids.add(unit.getEntity().getC3UUIDAsString());
            }

            for (int pos = 0; pos < uuids.size(); pos++) {
                for (Unit unit : units) {
                    if (null == unit.getEntity()) {
                        continue;
                    }
                    unit.getEntity().setNC3NextUUIDAsString(pos, uuids.get(pos));
                }
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains(TOEMouseAdapter.REMOVE_NETWORK)) {
            gui.getCampaign().removeUnitsFromNetwork(units);
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains(TOEMouseAdapter.DISBAND_NETWORK)) {
            if (null != singleUnit) {
                gui.getCampaign().disbandNetworkOf(singleUnit);
            }
        } else if (command.contains(TOEMouseAdapter.ADD_NETWORK)) {
            gui.getCampaign().addUnitsToNetwork(units, target);
        } else if (command.contains(TOEMouseAdapter.ADD_SLAVES)) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(target);
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains(TOEMouseAdapter.SET_MM)) {
            for (Unit u : units) {
                gui.getCampaign().removeUnitsFromC3Master(u);
                u.getEntity().setC3MasterIsUUIDAsString(u.getEntity().getC3UUIDAsString());
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains(TOEMouseAdapter.SET_IND_M)) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
                gui.getCampaign().removeUnitsFromC3Master(u);
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        } else if (command.contains(TOEMouseAdapter.REMOVE_C3)) {
            for (Unit u : units) {
                u.getEntity().setC3MasterIsUUIDAsString(null);
                u.getEntity().setC3Master(null, true);
            }
            gui.getCampaign().refreshNetworks();
            MekHQ.triggerEvent(new NetworkChangedEvent(units));
        }
    }

    @Override
    protected Optional<JPopupMenu> createPopupMenu() {
        if (tree.getSelectionPaths() == null) {
            return Optional.empty();
        }

        JPopupMenu popup = new JPopupMenu();
        JMenuItem menuItem;
        JMenu menu;

        // this is a little tricky because we want to
        // distinguish forces and units, but the user can
        // select multiple items of both types
        // we will allow multiple selection of either units or forces
        // but not both - if both are selected then default to
        // unit and deselect all forces
        Vector<Force> forces = new Vector<>();
        Vector<Unit> unitsInForces = new Vector<>();
        Vector<Unit> units = new Vector<>();
        Vector<TreePath> uPath = new Vector<>();
        for (TreePath path : tree.getSelectionPaths()) {
            Object node = path.getLastPathComponent();
            if (node instanceof Force) {
                forces.add((Force) node);
            }
            if (node instanceof Unit) {
                units.add((Unit) node);
                uPath.add(path);
            }
        }
        for (Force force : forces) {
            for (UUID id : force.getAllUnits(false)) {
                Unit u = gui.getCampaign().getUnit(id);
                if (null != u) {
                    unitsInForces.add(u);
                }
            }
        }
        boolean forcesSelected = !forces.isEmpty();
        boolean unitsSelected = !units.isEmpty();
        // if both are selected then we prefer units
        // and will deselect forces
        if (forcesSelected && unitsSelected) {
            forcesSelected = false;
            TreePath[] paths = new TreePath[uPath.size()];
            int i = 0;
            for (TreePath p : uPath) {
                paths[i] = p;
                i++;
            }
            tree.setSelectionPaths(paths);
        }
        boolean multipleSelection = (forcesSelected && forces.size() > 1) || (unitsSelected && units.size() > 1);
        if (forcesSelected) {
            Force force = forces.get(0);
            StringBuilder forceIds = new StringBuilder("" + force.getId());
            for (int i = 1; i < forces.size(); i++) {
                forceIds.append('|').append(forces.get(i).getId());
            }

            if (!multipleSelection) {
                if (force.getId() == 0) {
                    menu = new JMenu("Override Formation Level");
                    menu.setActionCommand(TOEMouseAdapter.COMMAND_CHANGE_FORCE_NAME + forceIds);
                    menu.addActionListener(this);
                    menu.setEnabled(true);
                    popup.add(menu);

                    Faction faction = gui.getCampaign().getFaction();

                    for (FormationLevel formationLevel : FormationLevel.values()) {
                        boolean addItem = isAddFormationLevel(formationLevel, faction);

                        if (addItem) {
                            menuItem = new JMenuItem(formationLevel.toString());
                            menuItem.setToolTipText(formationLevel.getDescription());
                            menuItem.setActionCommand(TOEMouseAdapter.COMMAND_OVERRIDE_FORCE_FORMATION_LEVEL
                                    + force.getId() + '|' + formationLevel);
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(true);
                            menu.add(menuItem);
                        }
                    }
                }

                menuItem = new JMenuItem("Change Name...");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_CHANGE_FORCE_NAME + forceIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                menuItem = new JMenuItem("Change Description...");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_CHANGE_FORCE_DESC + forceIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
                menuItem = new JMenuItem("Add New Force...");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_ADD_FORCE + forceIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);

                if (force.getTechID() == null) {
                    menu = new JMenu("Add Tech to Force");

                    JMenu mekTechs = new JMenu("Mek Techs");
                    JMenu aeroTechs = new JMenu("Aero Techs");
                    JMenu mechanics = new JMenu("Mechanics");
                    JMenu baTechs = new JMenu("BA Techs");

                    PersonnelRole role;
                    PersonnelRole previousRole = PersonnelRole.MEK_TECH;

                    JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                            SkillLevel.LEGENDARY.toString());
                    JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                    JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                    JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                    JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                    JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                    JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                            SkillLevel.ULTRA_GREEN.toString());
                    JMenu currentMenu = mekTechs;

                    // Get the list of techs, then sort them based on their tech role
                    List<Person> techList = gui.getCampaign().getTechs();
                    techList.sort((o1, o2) -> {
                        PersonnelRole r1 = o1.getPrimaryRole().isTech() ? o1.getPrimaryRole() : o1.getSecondaryRole();
                        PersonnelRole r2 = o2.getPrimaryRole().isTech() ? o2.getPrimaryRole() : o2.getSecondaryRole();
                        return r1.compareTo(r2);
                    });
                    for (Person tech : techList) {
                        if ((tech.getMaintenanceTimeUsing() == 0) && !tech.isEngineer()) {
                            role = tech.getPrimaryRole().isTech() ? tech.getPrimaryRole() : tech.getSecondaryRole();

                            // We need to add all the non-empty menus to the current menu, then
                            // the current menu must be added to the main menu if the role changes
                            // This enables us to use significantly less code to do the same thing
                            if (role.ordinal() > previousRole.ordinal()) {
                                previousRole = role;

                                // Adding menus if they aren't empty and adding scrollbars if they
                                // contain more than MAX_POPUP_ITEMS items
                                JMenuHelpers.addMenuIfNonEmpty(currentMenu, legendaryMenu);
                                JMenuHelpers.addMenuIfNonEmpty(currentMenu, heroicMenu);
                                JMenuHelpers.addMenuIfNonEmpty(currentMenu, eliteMenu);
                                JMenuHelpers.addMenuIfNonEmpty(currentMenu, veteranMenu);
                                JMenuHelpers.addMenuIfNonEmpty(currentMenu, regularMenu);
                                JMenuHelpers.addMenuIfNonEmpty(currentMenu, greenMenu);
                                JMenuHelpers.addMenuIfNonEmpty(currentMenu, ultraGreenMenu);
                                JMenuHelpers.addMenuIfNonEmpty(menu, currentMenu);

                                legendaryMenu = new JScrollableMenu("legendaryMenu", SkillLevel.LEGENDARY.toString());
                                heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                                eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                                veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                                regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                                greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                                ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                                        SkillLevel.ULTRA_GREEN.toString());
                                switch (role) {
                                    case MECHANIC:
                                        currentMenu = mechanics;
                                        break;
                                    case AERO_TEK:
                                        currentMenu = aeroTechs;
                                        break;
                                    case BA_TECH:
                                        currentMenu = baTechs;
                                        break;
                                    default:
                                        break;
                                }
                            }

                            menuItem = new JMenuItem(tech.getFullTitle() + " (" + tech.getRoleDesc() + ')');
                            menuItem.setActionCommand(COMMAND_ADD_LANCE_TECH + tech.getId() + '|' + forceIds);
                            menuItem.addActionListener(this);

                            switch (tech.getSkillLevel(gui.getCampaign(), !tech.getPrimaryRole().isTech())) {
                                case LEGENDARY:
                                    legendaryMenu.add(menuItem);
                                    break;
                                case HEROIC:
                                    heroicMenu.add(menuItem);
                                    break;
                                case ELITE:
                                    eliteMenu.add(menuItem);
                                    break;
                                case VETERAN:
                                    veteranMenu.add(menuItem);
                                    break;
                                case REGULAR:
                                    regularMenu.add(menuItem);
                                    break;
                                case GREEN:
                                    greenMenu.add(menuItem);
                                    break;
                                case ULTRA_GREEN:
                                    ultraGreenMenu.add(menuItem);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }

                    // We need to add the last role to the menu after we assign the last tech
                    JMenuHelpers.addMenuIfNonEmpty(currentMenu, legendaryMenu);
                    JMenuHelpers.addMenuIfNonEmpty(currentMenu, heroicMenu);
                    JMenuHelpers.addMenuIfNonEmpty(currentMenu, eliteMenu);
                    JMenuHelpers.addMenuIfNonEmpty(currentMenu, veteranMenu);
                    JMenuHelpers.addMenuIfNonEmpty(currentMenu, regularMenu);
                    JMenuHelpers.addMenuIfNonEmpty(currentMenu, greenMenu);
                    JMenuHelpers.addMenuIfNonEmpty(currentMenu, ultraGreenMenu);
                    JMenuHelpers.addMenuIfNonEmpty(menu, currentMenu);
                    JMenuHelpers.addMenuIfNonEmpty(popup, menu);
                } else {
                    menuItem = new JMenuItem("Remove Tech from Force");
                    menuItem.setActionCommand(
                            TOEMouseAdapter.COMMAND_REMOVE_LANCE_TECH + force.getTechID() + '|' + forceIds);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                menu = new JMenu("Add Unit");
                HashMap<String, JMenu> unitTypeMenus = new HashMap<>();
                HashMap<String, JMenu> weightClassForUnitType = new HashMap<>();
                final List<Integer> svTypes = Arrays.asList(UnitType.TANK,
                        UnitType.VTOL, UnitType.NAVAL, UnitType.CONV_FIGHTER);

                for (int i = 0; i < UnitType.SIZE; i++) {
                    String unittype = UnitType.getTypeName(i);
                    String displayname = UnitType.getTypeDisplayableName(i);
                    unitTypeMenus.put(unittype, new JMenu(displayname));
                    unitTypeMenus.get(unittype).setName(unittype);
                    unitTypeMenus.get(unittype).setEnabled(false);
                    for (int j = 0; j < EntityWeightClass.getWeightLimitByType(unittype).length; j++) {
                        double tonnage = EntityWeightClass.getWeightLimitByType(unittype)[j];
                        // Skip over the padding 0s
                        if (tonnage == 0) {
                            continue;
                        }

                        int weightClass = EntityWeightClass.getWeightClass(tonnage, unittype);
                        String displayname2 = EntityWeightClass.getClassName(weightClass, unittype, false);
                        String weightClassMenuName = unittype + '_'
                                + EntityWeightClass.getClassName(weightClass, unittype, false);
                        weightClassForUnitType.put(weightClassMenuName, new JMenu(displayname2));
                        weightClassForUnitType.get(weightClassMenuName).setName(weightClassMenuName);
                        weightClassForUnitType.get(weightClassMenuName).setEnabled(false);
                    }
                }

                for (int wc = EntityWeightClass.WEIGHT_SMALL_SUPPORT; wc <= EntityWeightClass.WEIGHT_LARGE_SUPPORT; wc++) {
                    for (int ut : svTypes) {
                        String typeName = UnitType.getTypeName(ut);
                        String wcName = EntityWeightClass.getClassName(wc, typeName, true);
                        String menuName = typeName + '_' + wcName;
                        JMenu m = new JMenu(wcName);
                        m.setName(menuName);
                        m.setEnabled(false);
                        weightClassForUnitType.put(menuName, m);
                    }
                }

                // Only add units that have commanders
                // Or Gun Emplacements!
                // TODO: Or Robotic Systems!
                JMenu unsorted = new JMenu("Unsorted");

                HangarSorter.weightSorted().forEachUnit(gui.getCampaign().getHangar(), u -> {
                    String type = UnitType.getTypeName(u.getEntity().getUnitType());
                    String className = u.getEntity().getWeightClassName();
                    if (null != u.getCommander()) {
                        Person p = u.getCommander();
                        if (p.getStatus().isActive() && (u.getForceId() < 1) && u.isPresent()) {
                            JMenuItem menuItem0 = new JMenuItem(p.getFullTitle() + ", " + u.getName());
                            menuItem0.setActionCommand(TOEMouseAdapter.COMMAND_ADD_UNIT + u.getId() + '|' + forceIds);
                            menuItem0.addActionListener(this);
                            menuItem0.setEnabled(u.isAvailable());
                            if (null != weightClassForUnitType.get(type + '_' + className)) {
                                weightClassForUnitType.get(type + '_' + className).add(menuItem0);
                                weightClassForUnitType.get(type + '_' + className).setEnabled(true);
                            } else {
                                unsorted.add(menuItem0);
                            }
                            unitTypeMenus.get(type).setEnabled(true);
                        }
                    }

                    if (u.getEntity() instanceof GunEmplacement) {
                        if (u.getForceId() < 1 && u.isPresent()) {
                            JMenuItem menuItem0 = new JMenuItem("AutoTurret, " + u.getName());
                            menuItem0.setActionCommand(TOEMouseAdapter.COMMAND_ADD_UNIT + u.getId() + '|' + forceIds);
                            menuItem0.addActionListener(this);
                            menuItem0.setEnabled(u.isAvailable());
                            if (null != weightClassForUnitType.get(type + '_' + className)) {
                                weightClassForUnitType.get(type + '_' + className).add(menuItem0);
                                weightClassForUnitType.get(type + '_' + className).setEnabled(true);
                            } else {
                                unsorted.add(menuItem0);
                            }
                            unitTypeMenus.get(type).setEnabled(true);
                        }
                    }
                });

                for (int i = 0; i < UnitType.SIZE; i++) {
                    String unittype = UnitType.getTypeName(i);
                    JMenu tmp = unitTypeMenus.get(UnitType.getTypeName(i));
                    if (tmp.isEnabled()) {
                        for (int j = 0; j < EntityWeightClass.getWeightLimitByType(unittype).length; j++) {
                            double tonnage = EntityWeightClass.getWeightLimitByType(unittype)[j];
                            // Skip over the padding 0s
                            if (tonnage == 0) {
                                continue;
                            }

                            int weightClass = EntityWeightClass.getWeightClass(tonnage, unittype);
                            JMenu tmp2 = weightClassForUnitType.get(unittype + '_'
                                    + EntityWeightClass.getClassName(weightClass, unittype, false));
                            if (tmp2.isEnabled()) {
                                tmp.add(tmp2);
                            }
                        }
                        menu.add(tmp);
                    }
                }

                for (int ut : svTypes) {
                    String unittype = UnitType.getTypeName(ut);
                    JMenu tmp = unitTypeMenus.get(UnitType.getTypeName(ut));
                    if (tmp.isEnabled()) {
                        for (int wc = EntityWeightClass.WEIGHT_SMALL_SUPPORT; wc <= EntityWeightClass.WEIGHT_LARGE_SUPPORT; wc++) {
                            JMenu tmp2 = weightClassForUnitType.get(unittype + '_'
                                    + EntityWeightClass.getClassName(wc, unittype, true));
                            if (tmp2.isEnabled()) {
                                tmp.add(tmp2);
                            }
                            menu.add(tmp);
                        }
                    }
                }
                JMenuHelpers.addMenuIfNonEmpty(menu, unsorted);
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);

                // still in the multiple selection block
                List<UUID> eligibleCommanders = force.getEligibleCommanders(gui.getCampaign());
                if (!eligibleCommanders.isEmpty()) {
                    menuItem = new JScrollableMenu("setCommanderMenu", "Set Commander");

                    for (UUID personID : eligibleCommanders) {
                        Person person = gui.getCampaign().getPerson(personID);

                        JMenuItem commanderOption = new JMenuItem(
                                person.getFullTitle() + " (" + person.getRoleDesc() + ')');
                        commanderOption.setActionCommand(COMMAND_SET_LANCE_COMMANDER + personID + '|' + forceIds);
                        commanderOption.addActionListener(this);
                        menuItem.add(commanderOption);
                    }

                    popup.add(menuItem);
                }
            }

            menu = new JMenu("Force Icon");
            if (!multipleSelection) {
                menuItem = new JMenuItem("Change Force Icon...");
                menuItem.setActionCommand(COMMAND_CHANGE_FORCE_ICON + forceIds);
                menuItem.addActionListener(this);
                menu.add(menuItem);

                menuItem = new JMenuItem("Copy Force Icon");
                menuItem.setName("miCopyForceIcon");
                menuItem.setActionCommand(COMMAND_COPY_FORCE_ICON + forceIds);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }

            if (gui.getCopyForceIcon() != null) {
                menuItem = new JMenuItem("Paste Force Icon");
                menuItem.setName("miPasteForceIcon");
                menuItem.setActionCommand(COMMAND_PASTE_FORCE_ICON + forceIds);
                menuItem.addActionListener(this);
                menu.add(menuItem);

                menuItem = new JMenuItem("Paste Force Icon to Force and Subforces");
                menuItem.setName("miSubforcesPasteForceIcon");
                menuItem.setActionCommand(COMMAND_SUBFORCES_PASTE_FORCE_ICON + forceIds);
                menuItem.addActionListener(this);
                menu.add(menuItem);
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);

            if (!multipleSelection) {
                menuItem = new JMenuItem("Force Camouflage...");
                menuItem.setActionCommand(COMMAND_CHANGE_FORCE_CAMO + forceIds);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            menuItem = new JMenuItem(force.isCombatForce() ? "Make Non-Combat Force" : "Make Combat Force");
            menuItem.setActionCommand(COMMAND_CHANGE_FORCE_COMBAT_STATUS + forceIds);
            menuItem.addActionListener(this);
            popup.add(menuItem);

            menuItem = new JMenuItem(force.isCombatForce() ? "Make Force and Subforces Non-Combat Forces"
                    : "Make Force and Subforces Combat Forces");
            menuItem.setActionCommand(COMMAND_CHANGE_FORCE_COMBAT_STATUSES + forceIds);
            menuItem.addActionListener(this);
            popup.add(menuItem);

            menuItem = new JMenuItem(!force.isConvoyForce() ? "Make Force and Subforces Resupply Convoys"
                : "Remove Resupply Convoy Status from Force and Subforces");
            menuItem.setActionCommand(COMMAND_CHANGE_FORCE_CONVOY_STATUS + forceIds);
            menuItem.addActionListener(this);
            popup.add(menuItem);

            if (StaticChecks.areAllForcesUndeployed(gui.getCampaign(), forces)
                    && StaticChecks.areAllCombatForces(forces)) {
                menu = new JMenu("Deploy Force");

                JMenu missionMenu;
                for (final Mission mission : gui.getCampaign().getActiveMissions(true)) {
                    missionMenu = new JMenu(mission.getName());
                    for (final Scenario scenario : mission.getCurrentScenarios()) {
                        if (scenario.isCloaked()
                                || !scenario.canDeployForces(forces, gui.getCampaign())) {
                            continue;
                        }
                        menuItem = new JMenuItem(scenario.getName());
                        menuItem.setActionCommand(
                                TOEMouseAdapter.COMMAND_DEPLOY_FORCE + scenario.getId() + '|' + forceIds);
                        menuItem.addActionListener(this);
                        missionMenu.add(menuItem);
                    }
                    JMenuHelpers.addMenuIfNonEmpty(menu, missionMenu);
                }
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }

            if (StaticChecks.areAllForcesDeployed(forces)) {
                menuItem = new JMenuItem("Undeploy Force");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_UNDEPLOY_FORCE + forceIds);
                menuItem.addActionListener(this);
                popup.add(menuItem);
            }

            menuItem = new JMenuItem("Remove Force");
            menuItem.setActionCommand(TOEMouseAdapter.COMMAND_REMOVE_FORCE + forceIds);
            menuItem.addActionListener(this);
            menuItem.setEnabled(
                    !StaticChecks.areAnyForcesDeployed(forces) && !StaticChecks.areAnyUnitsDeployed(unitsInForces));
            popup.add(menuItem);

            // Attempt to Assign all units in the selected force(s) to a transport ship.
            // This checks to see if the ship is in a basic state that can accept units.
            // Capacity gets checked once the action is submitted.
            menu = new JMenu(TOEMouseAdapter.ASSIGN_FORCE_TRN_TITLE);
            // Add submenus for different types of transports
            JMenu m_trn = new JMenu(TOEMouseAdapter.MEK_CARRIERS);
            JMenu pm_trn = new JMenu(TOEMouseAdapter.PROTOMEK_CARRIERS);
            JMenu lv_trn = new JMenu(TOEMouseAdapter.LVEE_CARRIERS);
            JMenu hv_trn = new JMenu(TOEMouseAdapter.HVEE_CARRIERS);
            JMenu shv_trn = new JMenu(TOEMouseAdapter.SHVEE_CARRIERS);
            JMenu ba_trn = new JMenu(TOEMouseAdapter.BA_CARRIERS);
            JMenu i_trn = new JMenu(TOEMouseAdapter.INFANTRY_CARRIERS);
            JMenu a_trn = new JMenu(TOEMouseAdapter.ASF_CARRIERS);
            JMenu sc_trn = new JMenu(TOEMouseAdapter.SC_CARRIERS);
            JMenu ds_trn = new JMenu(TOEMouseAdapter.DS_CARRIERS);
            JMenu singleUnitMenu = new JMenu();

            if (!unitsInForces.isEmpty()) {
                Unit unit = unitsInForces.get(0);
                StringBuilder unitIds = new StringBuilder(unit.getId().toString());
                boolean allUnitsSameType = false;
                double unitWeight = 0;
                int singleUnitType = -1;
                for (int i = 1; i < unitsInForces.size(); i++) {
                    unitIds.append('|').append(unitsInForces.get(i).getId().toString());
                }

                // Check to see if all selected units are of the same type
                for (int i = 0; i < UnitType.SIZE; i++) {
                    if (StaticChecks.areAllUnitsSameType(unitsInForces, i)) {
                        singleUnitType = i;
                        allUnitsSameType = true;
                        singleUnitMenu.setText(String.format(TOEMouseAdapter.VARIABLE_TRANSPORT,
                                UnitType.getTypeName(singleUnitType)));
                        break;
                    }
                }

                // Only display the Assign to Ship command if your command has at least 1 valid
                // transport
                // and if your selection does not include a transport
                if (!gui.getCampaign().getTransportShips().isEmpty()) {
                    for (Unit ship : gui.getCampaign().getTransportShips()) {
                        if (ship.isSalvage() || (ship.getCommander() == null)) {
                            continue;
                        }

                        UUID id = ship.getId();
                        if (allUnitsSameType) {
                            double capacity = ship.getCorrectBayCapacity(singleUnitType, unitWeight);
                            if (capacity > 0) {
                                JMenuItem shipMenuItem = new JMenuItem(
                                        ship.getName() + " , Space available: " + capacity);
                                shipMenuItem
                                        .setActionCommand(TOEMouseAdapter.COMMAND_ASSIGN_TO_SHIP + id + '|' + unitIds);
                                shipMenuItem.addActionListener(this);
                                shipMenuItem.setEnabled(true);
                                singleUnitMenu.add(shipMenuItem);
                                singleUnitMenu.setEnabled(true);
                            }
                        } else {
                            // Add this ship to the appropriate submenu(s). Most transports will fit into
                            // multiple
                            // categories
                            if (ship.getASFCapacity() > 0) {
                                a_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentASFCapacity()));
                                a_trn.setEnabled(true);
                            }

                            if (ship.getBattleArmorCapacity() > 0) {
                                ba_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentBattleArmorCapacity()));
                                ba_trn.setEnabled(true);
                            }

                            if (ship.getInfantryCapacity() > 0) {
                                i_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentInfantryCapacity()));
                                i_trn.setEnabled(true);
                            }

                            if (ship.getMekCapacity() > 0) {
                                m_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentMekCapacity()));
                                m_trn.setEnabled(true);
                            }

                            if (ship.getProtoMekCapacity() > 0) {
                                pm_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentProtoMekCapacity()));
                                pm_trn.setEnabled(true);
                            }

                            if (ship.getSmallCraftCapacity() > 0) {
                                sc_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentSmallCraftCapacity()));
                                sc_trn.setEnabled(true);
                            }

                            if (ship.getDocks() > 0) {
                                ds_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentDocks()));
                                ds_trn.setEnabled(true);
                            }

                            if (ship.getLightVehicleCapacity() > 0) {
                                lv_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentLightVehicleCapacity()));
                                lv_trn.setEnabled(true);
                            }

                            if (ship.getHeavyVehicleCapacity() > 0) {
                                hv_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentHeavyVehicleCapacity()));
                                hv_trn.setEnabled(true);
                            }

                            if (ship.getSuperHeavyVehicleCapacity() > 0) {
                                shv_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentSuperHeavyVehicleCapacity()));
                                shv_trn.setEnabled(true);
                            }
                        }
                    }
                }
                JMenuHelpers.addMenuIfNonEmpty(menu, a_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, ba_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, i_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, m_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, pm_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, sc_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, ds_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, lv_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, hv_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, shv_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, singleUnitMenu);
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);

                if (StaticChecks.areAllUnitsTransported(unitsInForces)) {
                    menuItem = new JMenuItem(TOEMouseAdapter.UNASSIGN_FORCE_TRN_TITLE);
                    menuItem.setActionCommand(TOEMouseAdapter.COMMAND_UNASSIGN_FROM_SHIP + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    popup.add(menuItem);
                }
            }
        } else if (unitsSelected) {
            Unit unit = units.get(0);
            StringBuilder unitIds = new StringBuilder(unit.getId().toString());
            for (int i = 1; i < units.size(); i++) {
                unitIds.append('|').append(units.get(i).getId().toString());
            }
            JMenu networkMenu = new JMenu("Network");
            JMenu availMenu;
            if (StaticChecks.areAllUnitsC3Slaves(units)) {
                availMenu = new JMenu("Slave to");
                for (String[] network : gui.getCampaign().getAvailableC3MastersForSlaves()) {
                    final int nodesFree;
                    try {
                        nodesFree = Integer.parseInt(network[1]);
                    } catch (Exception ex) {
                        logger.error("", ex);
                        continue;
                    }

                    if (nodesFree >= units.size()) {
                        menuItem = new JMenuItem(network[2] + ": " + network[1] + " nodes free");
                        menuItem.setActionCommand(TOEMouseAdapter.COMMAND_ADD_SLAVE
                                + network[0] + '|' + unitIds);
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(true);
                        availMenu.add(menuItem);
                    }
                }
                JMenuHelpers.addMenuIfNonEmpty(networkMenu, availMenu);
            }

            if (StaticChecks.areAllUnitsIndependentC3Masters(units)) {
                menuItem = new JMenuItem("Set as Company Level Master");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_SET_CO_MASTER + unitIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                networkMenu.add(menuItem);
                availMenu = new JMenu("Slave to");
                for (String[] network : gui.getCampaign().getAvailableC3MastersForMasters()) {
                    final int nodesFree;
                    try {
                        nodesFree = Integer.parseInt(network[1]);
                    } catch (Exception ex) {
                        logger.error("", ex);
                        continue;
                    }

                    if (nodesFree >= units.size()) {
                        menuItem = new JMenuItem(network[2] + ": " + network[1] + " nodes free");
                        menuItem.setActionCommand(TOEMouseAdapter.COMMAND_ADD_SLAVE
                                + network[0] + '|' + unitIds);
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(true);
                        availMenu.add(menuItem);
                    }
                }
                JMenuHelpers.addMenuIfNonEmpty(networkMenu, availMenu);
            }

            if (StaticChecks.areAllUnitsCompanyLevelMasters(units)) {
                menuItem = new JMenuItem("Set as Independent Master");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_SET_IND_MASTER + unitIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                networkMenu.add(menuItem);
            }

            if (StaticChecks.doAllUnitsHaveC3Master(units)) {
                menuItem = new JMenuItem("Remove from network");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_REMOVE_C3 + unitIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                networkMenu.add(menuItem);
            }
            // Naval C3 checks
            if (StaticChecks.doAllUnitsHaveNC3(units)) {
                if (multipleSelection && StaticChecks.areAllUnitsNotNC3Networked(units)
                        && (units.size() < 7)) {
                    menuItem = new JMenuItem("Create new NC3 network");
                    menuItem.setActionCommand(TOEMouseAdapter.COMMAND_CREATE_NC3 + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    networkMenu.add(menuItem);
                }

                if (StaticChecks.areAllUnitsNotNC3Networked(units)) {
                    availMenu = new JMenu("Add to network");
                    for (String[] network : gui.getCampaign().getAvailableNC3Networks()) {
                        final int nodesFree;
                        try {
                            nodesFree = Integer.parseInt(network[1]);
                        } catch (Exception ex) {
                            logger.error("", ex);
                            continue;
                        }

                        if (nodesFree >= units.size()) {
                            menuItem = new JMenuItem(network[0] + ": " + network[1] + " nodes free");
                            menuItem.setActionCommand(TOEMouseAdapter.COMMAND_ADD_TO_NETWORK
                                    + network[0] + '|' + unitIds);
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(true);
                            availMenu.add(menuItem);
                        }
                    }
                    JMenuHelpers.addMenuIfNonEmpty(networkMenu, availMenu);
                }

                if (StaticChecks.areAllUnitsNC3Networked(units)) {
                    menuItem = new JMenuItem("Remove from network");
                    menuItem.setActionCommand(TOEMouseAdapter.COMMAND_REMOVE_FROM_NETWORK + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    networkMenu.add(menuItem);
                    if (StaticChecks.areAllUnitsOnSameNC3Network(units)) {
                        menuItem = new JMenuItem("Disband this network");
                        menuItem.setActionCommand(TOEMouseAdapter.COMMAND_DISBAND_NETWORK + unitIds);
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(true);
                        networkMenu.add(menuItem);
                    }
                }
            }

            if (StaticChecks.doAllUnitsHaveC3i(units)) {
                if (multipleSelection && StaticChecks.areAllUnitsNotC3iNetworked(units)
                        && (units.size() < 7)) {
                    menuItem = new JMenuItem("Create new C3i network");
                    menuItem.setActionCommand(TOEMouseAdapter.COMMAND_CREATE_C3I + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    networkMenu.add(menuItem);
                }

                if (StaticChecks.areAllUnitsNotC3iNetworked(units)) {
                    availMenu = new JMenu("Add to network");
                    for (String[] network : gui.getCampaign().getAvailableC3iNetworks()) {
                        final int nodesFree;
                        try {
                            nodesFree = Integer.parseInt(network[1]);
                        } catch (Exception ex) {
                            logger.error("", ex);
                            continue;
                        }

                        if (nodesFree >= units.size()) {
                            menuItem = new JMenuItem(network[0] + ": " + network[1] + " nodes free");
                            menuItem.setActionCommand(TOEMouseAdapter.COMMAND_ADD_TO_NETWORK
                                    + network[0] + '|' + unitIds);
                            menuItem.addActionListener(this);
                            menuItem.setEnabled(true);
                            availMenu.add(menuItem);
                        }
                    }
                    JMenuHelpers.addMenuIfNonEmpty(networkMenu, availMenu);
                }

                if (StaticChecks.areAllUnitsC3iNetworked(units)) {
                    menuItem = new JMenuItem("Remove from network");
                    menuItem.setActionCommand(TOEMouseAdapter.COMMAND_REMOVE_FROM_NETWORK + unitIds);
                    menuItem.addActionListener(this);
                    menuItem.setEnabled(true);
                    networkMenu.add(menuItem);
                    if (StaticChecks.areAllUnitsOnSameC3iNetwork(units)) {
                        menuItem = new JMenuItem("Disband this network");
                        menuItem.setActionCommand(TOEMouseAdapter.COMMAND_DISBAND_NETWORK + unitIds);
                        menuItem.addActionListener(this);
                        menuItem.setEnabled(true);
                        networkMenu.add(menuItem);
                    }
                }
            }
            JMenuHelpers.addMenuIfNonEmpty(popup, networkMenu);

            menuItem = new JMenuItem("Remove Unit from TO&E");
            menuItem.setActionCommand(TOEMouseAdapter.COMMAND_REMOVE_UNIT + unitIds);
            menuItem.addActionListener(this);
            menuItem.setEnabled(!StaticChecks.areAnyUnitsDeployed(units));
            popup.add(menuItem);
            if (StaticChecks.areAllUnitsAvailable(units)) {
                // Deploy unit to a scenario - includes submenus for scenario selection
                menu = new JMenu("Deploy Unit");
                JMenu missionMenu;
                for (final Mission mission : gui.getCampaign().getActiveMissions(true)) {
                    missionMenu = new JMenu(mission.getName());
                    for (final Scenario scenario : mission.getCurrentScenarios()) {
                        if (scenario.isCloaked() ||
                                !scenario.canDeployUnits(units, gui.getCampaign())) {
                            continue;
                        }
                        menuItem = new JMenuItem(scenario.getName());
                        menuItem.setActionCommand(TOEMouseAdapter.COMMAND_DEPLOY_UNIT
                                + scenario.getId() + '|' + unitIds);
                        menuItem.addActionListener(this);
                        missionMenu.add(menuItem);
                    }
                    JMenuHelpers.addMenuIfNonEmpty(menu, missionMenu);
                }
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);

                // First, only display the Assign to Ship command if your command has at least 1
                // valid transport
                boolean allUnitsSameType = false;
                double unitWeight = 0;
                int singleUnitType = -1;

                // Add submenus for different types of transports
                JMenu m_trn = new JMenu(TOEMouseAdapter.MEK_CARRIERS);
                JMenu pm_trn = new JMenu(TOEMouseAdapter.PROTOMEK_CARRIERS);
                JMenu lv_trn = new JMenu(TOEMouseAdapter.LVEE_CARRIERS);
                JMenu hv_trn = new JMenu(TOEMouseAdapter.HVEE_CARRIERS);
                JMenu shv_trn = new JMenu(TOEMouseAdapter.SHVEE_CARRIERS);
                JMenu ba_trn = new JMenu(TOEMouseAdapter.BA_CARRIERS);
                JMenu i_trn = new JMenu(TOEMouseAdapter.INFANTRY_CARRIERS);
                JMenu a_trn = new JMenu(TOEMouseAdapter.ASF_CARRIERS);
                JMenu sc_trn = new JMenu(TOEMouseAdapter.SC_CARRIERS);
                JMenu ds_trn = new JMenu(TOEMouseAdapter.DS_CARRIERS);
                JMenu singleUnitMenu = new JMenu();

                // Check to see if all selected units are of the same type
                for (int i = 0; i < UnitType.SIZE; i++) {
                    if (StaticChecks.areAllUnitsSameType(units, i)) {
                        singleUnitType = i;
                        allUnitsSameType = true;
                        singleUnitMenu.setText(String.format(TOEMouseAdapter.VARIABLE_TRANSPORT,
                                UnitType.getTypeName(singleUnitType)));
                        break;
                    }
                }

                if (!gui.getCampaign().getTransportShips().isEmpty()) {
                    // Attempt to Assign unit to a transport ship. This checks to see if the ship
                    // is in a basic state that can accept units. Capacity gets checked once the
                    // action
                    // is submitted.
                    menu = new JMenu("Assign Unit to Transport Ship");
                    for (Unit ship : gui.getCampaign().getTransportShips()) {
                        if (ship.isSalvage() || (ship.getCommander() == null)) {
                            continue;
                        }

                        UUID id = ship.getId();
                        if (allUnitsSameType) {
                            double capacity = ship.getCorrectBayCapacity(singleUnitType, unitWeight);
                            if (capacity > 0) {
                                JMenuItem shipMenuItem = new JMenuItem(
                                        ship.getName() + " , Space available: " + capacity);
                                shipMenuItem
                                        .setActionCommand(TOEMouseAdapter.COMMAND_ASSIGN_TO_SHIP + id + '|' + unitIds);
                                shipMenuItem.addActionListener(this);
                                shipMenuItem.setEnabled(true);
                                singleUnitMenu.add(shipMenuItem);
                                singleUnitMenu.setEnabled(true);
                            }
                        } else {
                            // Add this ship to the appropriate submenu(s). Most transports will fit into
                            // multiple
                            // categories
                            if (ship.getASFCapacity() > 0) {
                                a_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentASFCapacity()));
                                a_trn.setEnabled(true);
                            }

                            if (ship.getBattleArmorCapacity() > 0) {
                                ba_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentBattleArmorCapacity()));
                                ba_trn.setEnabled(true);
                            }

                            if (ship.getInfantryCapacity() > 0) {
                                i_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentInfantryCapacity()));
                                i_trn.setEnabled(true);
                            }

                            if (ship.getMekCapacity() > 0) {
                                m_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentMekCapacity()));
                                m_trn.setEnabled(true);
                            }

                            if (ship.getProtoMekCapacity() > 0) {
                                pm_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentProtoMekCapacity()));
                                pm_trn.setEnabled(true);
                            }

                            if (ship.getSmallCraftCapacity() > 0) {
                                sc_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentSmallCraftCapacity()));
                                sc_trn.setEnabled(true);
                            }

                            if (ship.getDocks() > 0) {
                                ds_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentDocks()));
                                ds_trn.setEnabled(true);
                            }

                            if (ship.getLightVehicleCapacity() > 0) {
                                lv_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentLightVehicleCapacity()));
                                lv_trn.setEnabled(true);
                            }

                            if (ship.getHeavyVehicleCapacity() > 0) {
                                hv_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentHeavyVehicleCapacity()));
                                hv_trn.setEnabled(true);
                            }

                            if (ship.getSuperHeavyVehicleCapacity() > 0) {
                                shv_trn.add(transportMenuItem(ship.getName(), id, unitIds.toString(),
                                        ship.getCurrentSuperHeavyVehicleCapacity()));
                                shv_trn.setEnabled(true);
                            }
                        }
                    }
                }

                JMenuHelpers.addMenuIfNonEmpty(menu, a_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, ba_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, i_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, m_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, pm_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, sc_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, ds_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, lv_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, hv_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, shv_trn);
                JMenuHelpers.addMenuIfNonEmpty(menu, singleUnitMenu);
                JMenuHelpers.addMenuIfNonEmpty(popup, menu);
            }

            if (StaticChecks.areAllUnitsDeployed(units)) {
                menuItem = new JMenuItem("Undeploy Unit");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_UNDEPLOY_UNIT + unitIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }

            if (StaticChecks.areAllUnitsTransported(units)) {
                menuItem = new JMenuItem("Unassign Unit from Transport Ship");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_UNASSIGN_FROM_SHIP + unitIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }

            if (!multipleSelection) {
                popup.add(new ExportUnitSpriteMenu(gui.getFrame(), gui.getCampaign(), unit));

                menuItem = new JMenuItem("Go to Unit in Hangar");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_GOTO_UNIT + unitIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);

                menuItem = new JMenuItem("Go to Pilot/Commander in Personnel");
                menuItem.setActionCommand(TOEMouseAdapter.COMMAND_GOTO_PILOT + unitIds);
                menuItem.addActionListener(this);
                menuItem.setEnabled(true);
                popup.add(menuItem);
            }
        }

        return Optional.of(popup);
    }

    /**
     * Determines whether a given formation level can be added to a force.
     *
     * @param formationLevel The formation level to check.
     * @param faction        The faction to check against.
     * @return true if the formation level can be added to the faction, false
     *         otherwise.
     */
    private static boolean isAddFormationLevel(FormationLevel formationLevel, Faction faction) {
        if (formationLevel.isNone() || formationLevel.isInvalid()) {
            return false;
        }

        if (formationLevel.isClan() && faction.isClan()) {
            return true;
        } else if (formationLevel.isComStar() && faction.isComStarOrWoB()) {
            return true;
        } else if (!faction.isClan() && !faction.isComStarOrWoB()) {
            return formationLevel.isInnerSphere();
        }

        return false;
    }

    /**
     * Worker function to make sure transport assignment data gets cleared out when
     * unit(s) are removed from the TO&E
     *
     * @param unitsToUpdate A vector of UUIDs of the units that we need to update.
     *                      This can be either a collection that the player
     *                      has selected or all units in a given force
     */
    private void clearTransportAssignment(Vector<UUID> unitsToUpdate) {
        for (UUID id : unitsToUpdate) {
            Unit unit = gui.getCampaign().getUnit(id);
            if (unit != null) {
                clearTransportAssignment(unit);
            }
        }
    }

    /**
     * Worker function to make sure transport assignment data gets cleared out when
     * unit(s) are removed from the TO&E
     *
     * @param currentUnit The unit currently being processed
     */
    private void clearTransportAssignment(@Nullable Unit currentUnit) {
        if (currentUnit != null) {
            if (currentUnit.hasTransportShipAssignment()) {
                currentUnit.getTransportShipAssignment()
                        .getTransportShip()
                        .unloadFromTransportShip(currentUnit);
            }
            // If the unit IS a transport, unassign all units from it
            if (currentUnit.hasTransportedUnits()) {
                currentUnit.unloadTransportShip();
            }
        }
    }

    /**
     * Worker function that creates a new instance of a JMenuItem for a set of
     * transport ship characteristics
     * Used to have a single ship appear on multiple menu entries defined by type of
     * unit transported
     * Displays the remaining capacity in bays of the specified type
     *
     * @param shipName String name of this ship.
     * @param shipId   Unique id of this ship. Used to fill out
     *                 actionPerformed(ActionEvent)
     * @param unitIds  String of units delimited by | used to fill out
     *                 actionPerformed(ActionEvent)
     * @param capacity Double representing the capacity of the designated bay type
     */

    private JMenuItem transportMenuItem(String shipName, UUID shipId, String unitIds, double capacity) {
        JMenuItem menuItem = new JMenuItem(shipName + " , Space available: " + capacity);
        menuItem.setActionCommand(TOEMouseAdapter.COMMAND_ASSIGN_TO_SHIP + shipId + '|' + unitIds);
        menuItem.addActionListener(this);

        return menuItem;
    }
}
