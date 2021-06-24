/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.menus;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.HangarSorter;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.utilities.JMenuHelpers;
import mekhq.gui.utilities.StaticChecks;

import javax.swing.*;
import java.util.ArrayList;
import java.util.UUID;

public class AssignPersonToUnitMenu extends JScrollableMenu {
    //region Constructors
    public AssignPersonToUnitMenu(final Campaign campaign, final Person... people) {
        super("AssignPersonToUnitMenu");
        initialize(campaign, people);
    }
    //endregion Constructors

    //region Initialization
    private void initialize(final Campaign campaign, final Person... people) {
        // Initialize Menu
        setText(resources.getString("AssignPersonToUnitMenu.title"));

        // Default Return for Illegal Assignments - Null/Empty Skill Name or Self-Crewed Units
        // don't need techs, and if the total maintenance time is longer than the maximum for a
        // person we can just skip too


        // Person Assignment Menus

        // and we always include the None checkbox
        cbMenuItem = new JCheckBoxMenuItem(resourceMap.getString("none.text"));
        cbMenuItem.setActionCommand(makeCommand(CMD_REMOVE_UNIT, "-1"));
        cbMenuItem.addActionListener(this);
        menu.add(cbMenuItem);

        if ((menu.getItemCount() > 1) || (person.getUnit() != null)
                || !person.getTechUnits().isEmpty()) {
            JMenuHelpers.addMenuIfNonEmpty(popup, menu);
        }

        // And finally add the ability to simply unassign
        final JMenuItem miUnassignPerson = new JMenuItem(resources.getString("None.text"));
        miUnassignPerson.setName("miUnassignPerson");
        miUnassignPerson.addActionListener(evt -> {

        });
        add(miUnassignPerson);
    }
    //endregion Initialization



    private void old() {
        if (!person.isDeployed()) {
            // Assign pilot to unit/none
            menu = new JMenu(resourceMap.getString("assignToUnit.text"));
            JMenu pilotMenu = new JMenu(resourceMap.getString("assignAsPilot.text"));
            JMenu pilotUnitTypeMenu = new JMenu();
            JMenu pilotEntityWeightMenu = new JMenu();
            JMenu driverMenu = new JMenu(resourceMap.getString("assignAsDriver.text"));
            JMenu driverUnitTypeMenu = new JMenu();
            JMenu driverEntityWeightMenu = new JMenu();
            JMenu crewMenu = new JMenu(resourceMap.getString("assignAsCrewmember.text"));
            JMenu crewUnitTypeMenu = new JMenu();
            JMenu crewEntityWeightMenu = new JMenu();
            JMenu gunnerMenu = new JMenu(resourceMap.getString("assignAsGunner.text"));
            JMenu gunnerUnitTypeMenu = new JMenu();
            JMenu gunnerEntityWeightMenu = new JMenu();
            JMenu navMenu = new JMenu(resourceMap.getString("assignAsNavigator.text"));
            JMenu navUnitTypeMenu = new JMenu();
            JMenu navEntityWeightMenu = new JMenu();
            JMenu soldierMenu = new JMenu(resourceMap.getString("assignAsSoldier.text"));
            JMenu soldierUnitTypeMenu = new JMenu();
            JMenu soldierEntityWeightMenu = new JMenu();
            JMenu techOfficerMenu = new JMenu(resourceMap.getString("assignAsTechOfficer.text"));
            JMenu techOfficerUnitTypeMenu = new JMenu();
            JMenu techOfficerEntityWeightMenu = new JMenu();
            JMenu consoleCmdrMenu = new JMenu(resourceMap.getString("assignAsConsoleCmdr.text"));
            JMenu consoleCmdrUnitTypeMenu = new JMenu();
            JMenu consoleCmdrEntityWeightMenu = new JMenu();

            int unitType = -1;
            int weightClass = -1;

            if (oneSelected && person.getStatus().isActive() && person.getPrisonerStatus().isFree()) {
                for (Unit unit : HangarSorter.defaultSorting().getUnits(gui.getCampaign().getHangar())) {
                    if (!unit.isAvailable()) {
                        continue;
                    } else if (unit.getEntity().getUnitType() != unitType) {
                        unitType = unit.getEntity().getUnitType();
                        String unitTypeName = UnitType.getTypeName(unitType);
                        weightClass = unit.getEntity().getWeightClass();
                        String weightClassName = unit.getEntity().getWeightClassName();

                        // Add Weight Menus to Unit Type Menus
                        JMenuHelpers.addMenuIfNonEmpty(pilotUnitTypeMenu, pilotEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(driverUnitTypeMenu, driverEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(crewUnitTypeMenu, crewEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(gunnerUnitTypeMenu, gunnerEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(navUnitTypeMenu, navEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(soldierUnitTypeMenu, soldierEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(techOfficerUnitTypeMenu, techOfficerEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(consoleCmdrUnitTypeMenu, consoleCmdrEntityWeightMenu);

                        // Then add the Unit Type Menus to the Role Menus
                        JMenuHelpers.addMenuIfNonEmpty(pilotMenu, pilotUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(driverMenu, driverUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(crewMenu, crewUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(gunnerMenu, gunnerUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(navMenu, navUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(soldierMenu, soldierUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(techOfficerMenu, techOfficerUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(consoleCmdrMenu, consoleCmdrUnitTypeMenu);

                        // Create new UnitType and EntityWeight Menus
                        pilotUnitTypeMenu = new JMenu(unitTypeName);
                        pilotEntityWeightMenu = new JMenu(weightClassName);
                        driverUnitTypeMenu = new JMenu(unitTypeName);
                        driverEntityWeightMenu = new JMenu(weightClassName);
                        crewUnitTypeMenu = new JMenu(unitTypeName);
                        crewEntityWeightMenu = new JMenu(weightClassName);
                        gunnerUnitTypeMenu = new JMenu(unitTypeName);
                        gunnerEntityWeightMenu = new JMenu(weightClassName);
                        navUnitTypeMenu = new JMenu(unitTypeName);
                        navEntityWeightMenu = new JMenu(weightClassName);
                        soldierUnitTypeMenu = new JMenu(unitTypeName);
                        soldierEntityWeightMenu = new JMenu(weightClassName);
                        techOfficerUnitTypeMenu = new JMenu(unitTypeName);
                        techOfficerEntityWeightMenu = new JMenu(weightClassName);
                        consoleCmdrUnitTypeMenu = new JMenu(unitTypeName);
                        consoleCmdrEntityWeightMenu = new JMenu(weightClassName);
                    } else if (unit.getEntity().getWeightClass() != weightClass) {
                        weightClass = unit.getEntity().getWeightClass();
                        String weightClassName = unit.getEntity().getWeightClassName();

                        JMenuHelpers.addMenuIfNonEmpty(pilotUnitTypeMenu, pilotEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(driverUnitTypeMenu, driverEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(crewUnitTypeMenu, crewEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(gunnerUnitTypeMenu, gunnerEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(navUnitTypeMenu, navEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(soldierUnitTypeMenu, soldierEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(techOfficerUnitTypeMenu, techOfficerEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(consoleCmdrUnitTypeMenu, consoleCmdrEntityWeightMenu);

                        pilotEntityWeightMenu = new JMenu(weightClassName);
                        driverEntityWeightMenu = new JMenu(weightClassName);
                        crewEntityWeightMenu = new JMenu(weightClassName);
                        gunnerEntityWeightMenu = new JMenu(weightClassName);
                        navEntityWeightMenu = new JMenu(weightClassName);
                        soldierEntityWeightMenu = new JMenu(weightClassName);
                        techOfficerEntityWeightMenu = new JMenu(weightClassName);
                        consoleCmdrEntityWeightMenu = new JMenu(weightClassName);
                    }

                    if (unit.usesSoloPilot()) {
                        if (unit.canTakeMoreDrivers() && person.canDrive(unit.getEntity())
                                && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_PILOT, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            pilotEntityWeightMenu.add(cbMenuItem);
                        }
                    } else if (unit.usesSoldiers()) {
                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_SOLDIER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            soldierEntityWeightMenu.add(cbMenuItem);
                        }
                    } else {
                        if (unit.canTakeMoreDrivers() && person.canDrive(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_DRIVER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            if (unit.getEntity() instanceof Aero || unit.getEntity() instanceof Mech) {
                                pilotEntityWeightMenu.add(cbMenuItem);
                            } else {
                                driverEntityWeightMenu.add(cbMenuItem);
                            }
                        }

                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_GUNNER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            gunnerEntityWeightMenu.add(cbMenuItem);
                        }

                        if (unit.canTakeMoreVesselCrew()
                                && ((unit.getEntity().isAero() && person.hasSkill(SkillType.S_TECH_VESSEL))
                                || ((unit.getEntity().isSupportVehicle() && person.hasSkill(SkillType.S_TECH_MECHANIC))))) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_CREW, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            crewEntityWeightMenu.add(cbMenuItem);
                        }

                        if (unit.canTakeNavigator() && person.hasSkill(SkillType.S_NAV)) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_NAVIGATOR, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            navEntityWeightMenu.add(cbMenuItem);
                        }

                        if (unit.canTakeTechOfficer()) {
                            //For a vehicle command console we will require the commander to be a driver or a gunner, but not necessarily both
                            if (unit.getEntity() instanceof Tank) {
                                if (person.canDrive(unit.getEntity()) || person.canGun(unit.getEntity())) {
                                    cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                                    cbMenuItem.setSelected(unit.equals(person.getUnit()));
                                    cbMenuItem.setActionCommand(makeCommand(CMD_ADD_TECH_OFFICER, unit.getId().toString()));
                                    cbMenuItem.addActionListener(this);
                                    consoleCmdrEntityWeightMenu.add(cbMenuItem);
                                }
                            } else if (person.canDrive(unit.getEntity()) && person.canGun(unit.getEntity())) {
                                cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                                cbMenuItem.setSelected(unit.equals(person.getUnit()));
                                cbMenuItem.setActionCommand(makeCommand(CMD_ADD_TECH_OFFICER, unit.getId().toString()));
                                cbMenuItem.addActionListener(this);
                                techOfficerEntityWeightMenu.add(cbMenuItem);
                            }
                        }
                    }
                }
            } else if (StaticChecks.areAllActive(selected) && StaticChecks.areAllEligible(selected)) {
                for (Unit unit : HangarSorter.defaultSorting().getUnits(gui.getCampaign().getHangar())) {
                    if (!unit.isAvailable()) {
                        continue;
                    } else if (unit.getEntity().getUnitType() != unitType) {
                        unitType = unit.getEntity().getUnitType();
                        String unitTypeName = UnitType.getTypeName(unitType);
                        weightClass = unit.getEntity().getWeightClass();
                        String weightClassName = unit.getEntity().getWeightClassName();

                        // Add Weight Menus to Unit Type Menus
                        JMenuHelpers.addMenuIfNonEmpty(pilotUnitTypeMenu, pilotEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(driverUnitTypeMenu, driverEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(crewUnitTypeMenu, crewEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(gunnerUnitTypeMenu, gunnerEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(navUnitTypeMenu, navEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(soldierUnitTypeMenu, soldierEntityWeightMenu);

                        // Then add the Unit Type Menus to the Role Menus
                        JMenuHelpers.addMenuIfNonEmpty(pilotMenu, pilotUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(driverMenu, driverUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(crewMenu, crewUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(gunnerMenu, gunnerUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(navMenu, navUnitTypeMenu);
                        JMenuHelpers.addMenuIfNonEmpty(soldierMenu, soldierUnitTypeMenu);

                        // Create new UnitType and EntityWeight Menus
                        pilotUnitTypeMenu = new JMenu(unitTypeName);
                        pilotEntityWeightMenu = new JMenu(weightClassName);
                        driverUnitTypeMenu = new JMenu(unitTypeName);
                        driverEntityWeightMenu = new JMenu(weightClassName);
                        crewUnitTypeMenu = new JMenu(unitTypeName);
                        crewEntityWeightMenu = new JMenu(weightClassName);
                        gunnerUnitTypeMenu = new JMenu(unitTypeName);
                        gunnerEntityWeightMenu = new JMenu(weightClassName);
                        navUnitTypeMenu = new JMenu(unitTypeName);
                        navEntityWeightMenu = new JMenu(weightClassName);
                        soldierUnitTypeMenu = new JMenu(unitTypeName);
                        soldierEntityWeightMenu = new JMenu(weightClassName);
                    } else if (unit.getEntity().getWeightClass() != weightClass) {
                        weightClass = unit.getEntity().getWeightClass();
                        String weightClassName = unit.getEntity().getWeightClassName();

                        JMenuHelpers.addMenuIfNonEmpty(pilotUnitTypeMenu, pilotEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(driverUnitTypeMenu, driverEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(crewUnitTypeMenu, crewEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(gunnerUnitTypeMenu, gunnerEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(navUnitTypeMenu, navEntityWeightMenu);
                        JMenuHelpers.addMenuIfNonEmpty(soldierUnitTypeMenu, soldierEntityWeightMenu);

                        pilotEntityWeightMenu = new JMenu(weightClassName);
                        driverEntityWeightMenu = new JMenu(weightClassName);
                        crewEntityWeightMenu = new JMenu(weightClassName);
                        gunnerEntityWeightMenu = new JMenu(weightClassName);
                        navEntityWeightMenu = new JMenu(weightClassName);
                        soldierEntityWeightMenu = new JMenu(weightClassName);
                    }

                    if (StaticChecks.areAllSoldiers(selected)) {
                        if (!unit.isConventionalInfantry()) {
                            continue;
                        }

                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_SOLDIER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            soldierEntityWeightMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllBattleArmor(selected)) {
                        if (!(unit.getEntity() instanceof BattleArmor)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_SOLDIER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            soldierEntityWeightMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVehicleGunners(selected)) {
                        if (!(unit.getEntity() instanceof Tank)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_GUNNER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            gunnerEntityWeightMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselGunners(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeMoreGunners() && person.canGun(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_GUNNER, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            gunnerEntityWeightMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselCrew(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeMoreVesselCrew()
                                && ((unit.getEntity().isAero() && person.hasSkill(SkillType.S_TECH_VESSEL))
                                || ((unit.getEntity().isSupportVehicle() && person.hasSkill(SkillType.S_TECH_MECHANIC))))) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_CREW, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            crewEntityWeightMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselPilots(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeMoreDrivers() && person.canDrive(unit.getEntity())) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_VESSEL_PILOT, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            pilotEntityWeightMenu.add(cbMenuItem);
                        }
                    } else if (StaticChecks.areAllVesselNavigators(selected)) {
                        if (!(unit.getEntity() instanceof Aero)) {
                            continue;
                        }
                        if (unit.canTakeNavigator() && person.hasSkill(SkillType.S_NAV)) {
                            cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                            cbMenuItem.setSelected(unit.equals(person.getUnit()));
                            cbMenuItem.setActionCommand(makeCommand(CMD_ADD_NAVIGATOR, unit.getId().toString()));
                            cbMenuItem.addActionListener(this);
                            navEntityWeightMenu.add(cbMenuItem);
                        }
                    }
                }
            }

            // Add the last grouping of entity weight menus to the last grouping of entity menus
            JMenuHelpers.addMenuIfNonEmpty(pilotUnitTypeMenu, pilotEntityWeightMenu);
            JMenuHelpers.addMenuIfNonEmpty(driverUnitTypeMenu, driverEntityWeightMenu);
            JMenuHelpers.addMenuIfNonEmpty(crewUnitTypeMenu, crewEntityWeightMenu);
            JMenuHelpers.addMenuIfNonEmpty(gunnerUnitTypeMenu, gunnerEntityWeightMenu);
            JMenuHelpers.addMenuIfNonEmpty(navUnitTypeMenu, navEntityWeightMenu);
            JMenuHelpers.addMenuIfNonEmpty(soldierUnitTypeMenu, soldierEntityWeightMenu);
            JMenuHelpers.addMenuIfNonEmpty(techOfficerUnitTypeMenu, techOfficerEntityWeightMenu);
            JMenuHelpers.addMenuIfNonEmpty(consoleCmdrUnitTypeMenu, consoleCmdrEntityWeightMenu);

            // then add the last grouping of entity menus to the primary menus
            JMenuHelpers.addMenuIfNonEmpty(pilotMenu, pilotUnitTypeMenu);
            JMenuHelpers.addMenuIfNonEmpty(driverMenu, driverUnitTypeMenu);
            JMenuHelpers.addMenuIfNonEmpty(crewMenu, crewUnitTypeMenu);
            JMenuHelpers.addMenuIfNonEmpty(gunnerMenu, gunnerUnitTypeMenu);
            JMenuHelpers.addMenuIfNonEmpty(navMenu, navUnitTypeMenu);
            JMenuHelpers.addMenuIfNonEmpty(soldierMenu, soldierUnitTypeMenu);
            JMenuHelpers.addMenuIfNonEmpty(techOfficerMenu, techOfficerUnitTypeMenu);
            JMenuHelpers.addMenuIfNonEmpty(consoleCmdrMenu, consoleCmdrUnitTypeMenu);

            // and finally add any non-empty menus to the primary menu
            JMenuHelpers.addMenuIfNonEmpty(menu, pilotMenu);
            JMenuHelpers.addMenuIfNonEmpty(menu, driverMenu);
            JMenuHelpers.addMenuIfNonEmpty(menu, crewMenu);
            JMenuHelpers.addMenuIfNonEmpty(menu, gunnerMenu);
            JMenuHelpers.addMenuIfNonEmpty(menu, navMenu);
            JMenuHelpers.addMenuIfNonEmpty(menu, soldierMenu);
            JMenuHelpers.addMenuIfNonEmpty(menu, techOfficerMenu);
            JMenuHelpers.addMenuIfNonEmpty(menu, consoleCmdrMenu);
        }
    }

    private void oldNote() {
        switch (old) {
            case CMD_REMOVE_UNIT: {
                for (Person person : people) {
                    Unit u = person.getUnit();
                    if (null != u) {
                        u.remove(person, true);
                    }
                    // check for tech unit assignments
                    if (!person.getTechUnits().isEmpty()) {
                        for (Unit unitWeTech : new ArrayList<>(person.getTechUnits())) {
                            unitWeTech.remove(person, true);
                        }

                        // Incase there's still some assignments for this tech,
                        // clear them out. This can happen if the target unit
                        // above is null. The tech will still have the pointer
                        // but to a null unit and it will never go away
                        // otherwise
                        person.clearTechUnits();
                    }
                }
                break;
            }
            case CMD_ADD_PILOT: {
                final Unit unit = gui.getCampaign().getUnit(UUID.fromString(data[1]));
                final Unit oldUnit = selectedPerson.getUnit();
                if (oldUnit != null) {
                    oldUnit.remove(selectedPerson, !gui.getCampaign().getCampaignOptions().useTransfers());
                }

                if (unit != null) {
                    unit.addPilotOrSoldier(selectedPerson, gui.getCampaign().getCampaignOptions().useTransfers());
                }
                break;
            }
            case CMD_ADD_SOLDIER: {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeMoreGunners()) {
                            Unit oldUnit = p.getUnit();
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.addPilotOrSoldier(p, useTransfers);
                        }
                    }
                }
                break;
            }
            case CMD_ADD_DRIVER: {
                final Unit unit = gui.getCampaign().getUnit(UUID.fromString(data[1]));
                final Unit oldUnit = selectedPerson.getUnit();
                if (oldUnit != null) {
                    oldUnit.remove(selectedPerson, !gui.getCampaign().getCampaignOptions().useTransfers());
                }

                if (unit != null) {
                    unit.addDriver(selectedPerson, gui.getCampaign().getCampaignOptions().useTransfers());
                }
                break;
            }
            case CMD_ADD_VESSEL_PILOT: {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeMoreDrivers()) {
                            Unit oldUnit = p.getUnit();
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.addDriver(p, useTransfers);
                        }
                    }
                }
                break;
            }
            case CMD_ADD_GUNNER: {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeMoreGunners()) {
                            Unit oldUnit = p.getUnit();
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.addGunner(p, useTransfers);
                        }
                    }
                }
                break;
            }
            case CMD_ADD_CREW: {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeMoreVesselCrew()) {
                            Unit oldUnit = p.getUnit();
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.addVesselCrew(p, useTransfers);
                        }
                    }
                }
                break;
            }
            case CMD_ADD_NAVIGATOR: {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeNavigator()) {
                            Unit oldUnit = p.getUnit();
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.setNavigator(p, useTransfers);
                        }
                    }
                }
                break;
            }
            case CMD_ADD_TECH_OFFICER: {
                UUID selected = UUID.fromString(data[1]);
                Unit u = gui.getCampaign().getUnit(selected);
                if (null != u) {
                    for (Person p : people) {
                        if (u.canTakeTechOfficer()) {
                            Unit oldUnit = p.getUnit();
                            boolean useTransfers = false;
                            boolean transferLog = !gui.getCampaign().getCampaignOptions().useTransfers();
                            if (null != oldUnit) {
                                oldUnit.remove(p, transferLog);
                                useTransfers = gui.getCampaign().getCampaignOptions().useTransfers();
                            }
                            u.setTechOfficer(p, useTransfers);
                        }
                    }
                }
                break;
            }
            case CMD_ADD_TECH: {
                UUID selected = UUID.fromString(data[1]);
                Unit unit = gui.getCampaign().getUnit(selected);
                if (unit != null) {
                    if (unit.canTakeTech()) {
                        unit.setTech(selectedPerson);
                    }
                }
                break;
            }
        }
    }
}
