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

import megamek.MegaMek;
import megamek.common.Aero;
import megamek.common.ConvFighter;
import megamek.common.EntityWeightClass;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.UnitType;
import megamek.common.VTOL;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.unit.HangarSorter;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a standard menu that takes either a person or multiple people, and allows the user to
 * assign them to a unit or remove them from their unit(s), including tech assignments.
 */
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

        // Default Return for Illegal or Impossible Assignments
        // 1) No people to be assigned
        // 2) All people must be active
        // 3) All people must be non-prisoners (bondsmen should be assignable to units)
        // 4) All people cannot be currently deployed
        // 5) All people must not be primary civilians
        // 6) All people must share one of their non-civilian professions
        if ((people.length == 0) || Stream.of(people).allMatch(person -> person.getStatus().isActive()
                && !person.getPrisonerStatus().isPrisoner() && !person.isDeployed()
                && !Profession.getProfessionFromPersonnelRole(person.getPrimaryRole()).isCivilian())) {
            return;
        }
        final Profession basePrimaryProfession = Profession.getProfessionFromPersonnelRole(people[0].getPrimaryRole());
        final Profession baseSecondaryProfession = Profession.getProfessionFromPersonnelRole(people[0].getPrimaryRole());
        for (final Person person : people) {
            final Profession primaryProfession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());
            if ((primaryProfession == basePrimaryProfession) || (primaryProfession == baseSecondaryProfession)) {
                continue;
            }
            final Profession secondaryProfession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());
            if (secondaryProfession.isCivilian()
                    || ((secondaryProfession != basePrimaryProfession) && (secondaryProfession != baseSecondaryProfession))) {
                return;
            }
        }

        // Person Assignment Menus
        // Parsing variables
        final JMenu pilotMenu = new JScrollableMenu("pilotMenu", resources.getString("asPilotMenu.text"));
        JMenu pilotUnitTypeMenu = new JScrollableMenu("pilotUnitTypeMenu");
        JMenu pilotEntityWeightMenu = new JMenu();
        final JMenu driverMenu = new JScrollableMenu("driverMenu", resources.getString("asDriverMenu.text"));
        JMenu driverUnitTypeMenu = new JScrollableMenu("driverUnitTypeMenu");
        JMenu driverEntityWeightMenu = new JMenu();
        final JMenu gunnerMenu = new JScrollableMenu("gunnerMenu", resources.getString("asGunnerMenu.text"));
        JMenu gunnerUnitTypeMenu = new JScrollableMenu("gunnerUnitTypeMenu");
        JMenu gunnerEntityWeightMenu = new JMenu();
        final JMenu crewmemberMenu = new JScrollableMenu("crewmemberMenu", resources.getString("asCrewmemberMenu.text"));
        JMenu crewmemberUnitTypeMenu = new JScrollableMenu("crewmemberUnitTypeMenu");
        JMenu crewmemberEntityWeightMenu = new JMenu();
        final JMenu techOfficerMenu = new JScrollableMenu("techOfficerMenu", resources.getString("asTechOfficerMenu.text"));
        JMenu techOfficerUnitTypeMenu = new JScrollableMenu("techOfficerUnitTypeMenu");
        JMenu techOfficerEntityWeightMenu = new JMenu();
        final JMenu consoleCommanderMenu = new JScrollableMenu("consoleCommanderMenu", resources.getString("asConsoleCommanderMenu.text"));
        JMenu consoleCommanderUnitTypeMenu = new JScrollableMenu("consoleCommanderUnitTypeMenu");
        JMenu consoleCommanderEntityWeightMenu = new JMenu();
        final JMenu soldierMenu = new JScrollableMenu("soldierMenu", resources.getString("asSoldierMenu.text"));
        JMenu soldierUnitTypeMenu = new JScrollableMenu("soldierUnitTypeMenu");
        JMenu soldierEntityWeightMenu = new JMenu();
        final JMenu navigatorMenu = new JScrollableMenu("navigatorMenu", resources.getString("asNavigatorMenu.text"));
        JMenu navigatorUnitTypeMenu = new JScrollableMenu("navigatorUnitTypeMenu");
        JMenu navigatorEntityWeightMenu = new JMenu();
        int unitType = -1;
        int weightClass = -1;

        final boolean selected = (people[0].getUnit() != null)
                && Stream.of(people).allMatch(person -> people[0].getUnit().equals(person.getUnit()));

        final List<Unit> units = HangarSorter.defaultSorting()
                .sort(campaign.getHangar().getUnitsStream().filter(Unit::isAvailable))
                .collect(Collectors.toList());
        for (final Unit unit : units) {
            if (unit.getEntity().getUnitType() != unitType) {
                // Add the current menus, first the Entity Weight Class menu to the related Unit
                // Type menu, then the Unit Type menu to the grouping menu
                pilotUnitTypeMenu.add(pilotEntityWeightMenu);
                pilotMenu.add(pilotUnitTypeMenu);
                driverUnitTypeMenu.add(driverEntityWeightMenu);
                driverMenu.add(driverUnitTypeMenu);
                gunnerUnitTypeMenu.add(gunnerEntityWeightMenu);
                gunnerMenu.add(gunnerUnitTypeMenu);
                crewmemberUnitTypeMenu.add(crewmemberEntityWeightMenu);
                crewmemberMenu.add(crewmemberUnitTypeMenu);
                techOfficerUnitTypeMenu.add(techOfficerEntityWeightMenu);
                techOfficerMenu.add(techOfficerUnitTypeMenu);
                consoleCommanderUnitTypeMenu.add(consoleCommanderEntityWeightMenu);
                consoleCommanderMenu.add(consoleCommanderUnitTypeMenu);
                soldierUnitTypeMenu.add(soldierEntityWeightMenu);
                soldierMenu.add(soldierUnitTypeMenu);
                navigatorUnitTypeMenu.add(navigatorEntityWeightMenu);
                navigatorMenu.add(navigatorUnitTypeMenu);

                // Update parsing variables
                unitType = unit.getEntity().getUnitType();
                weightClass = unit.getEntity().getWeightClass();

                // And create the new menus
                final String unitTypeName = UnitType.getTypeDisplayableName(unitType);
                final String entityWeightClassName = EntityWeightClass.getClassName(weightClass, unit.getEntity());
                pilotUnitTypeMenu = new JScrollableMenu("pilotUnitTypeMenu", unitTypeName);
                pilotEntityWeightMenu = new JScrollableMenu("pilotEntityWeightMenu", entityWeightClassName);
                driverUnitTypeMenu = new JScrollableMenu("driverUnitTypeMenu", unitTypeName);
                driverEntityWeightMenu = new JScrollableMenu("driverEntityWeightMenu", entityWeightClassName);
                gunnerUnitTypeMenu = new JScrollableMenu("gunnerUnitTypeMenu", unitTypeName);
                gunnerEntityWeightMenu = new JScrollableMenu("gunnerEntityWeightMenu", entityWeightClassName);
                crewmemberUnitTypeMenu = new JScrollableMenu("crewmemberUnitTypeMenu", unitTypeName);
                crewmemberEntityWeightMenu = new JScrollableMenu("crewmemberEntityWeightMenu", entityWeightClassName);
                techOfficerUnitTypeMenu = new JScrollableMenu("techOfficerUnitTypeMenu", unitTypeName);
                techOfficerEntityWeightMenu = new JScrollableMenu("techOfficerEntityWeightMenu", entityWeightClassName);
                consoleCommanderUnitTypeMenu = new JScrollableMenu("consoleCommanderUnitTypeMenu", unitTypeName);
                consoleCommanderEntityWeightMenu = new JScrollableMenu("consoleCommanderEntityWeightMenu", entityWeightClassName);
                soldierUnitTypeMenu = new JScrollableMenu("soldierUnitTypeMenu", unitTypeName);
                soldierEntityWeightMenu = new JScrollableMenu("soldierEntityWeightMenu", entityWeightClassName);
                navigatorUnitTypeMenu = new JScrollableMenu("navigatorUnitTypeMenu", unitTypeName);
                navigatorEntityWeightMenu = new JScrollableMenu("navigatorEntityWeightMenu", entityWeightClassName);
            } else if (unit.getEntity().getWeightClass() != weightClass) {
                // Add the current Entity Weight Class menu to the Unit Type menu
                pilotUnitTypeMenu.add(pilotEntityWeightMenu);
                driverUnitTypeMenu.add(driverEntityWeightMenu);
                gunnerUnitTypeMenu.add(gunnerEntityWeightMenu);
                crewmemberUnitTypeMenu.add(crewmemberEntityWeightMenu);
                techOfficerUnitTypeMenu.add(techOfficerEntityWeightMenu);
                consoleCommanderUnitTypeMenu.add(consoleCommanderEntityWeightMenu);
                soldierUnitTypeMenu.add(soldierEntityWeightMenu);
                navigatorUnitTypeMenu.add(navigatorEntityWeightMenu);

                // Update parsing variable
                weightClass = unit.getEntity().getWeightClass();

                // And create the new Entity Weight Class menus
                final String entityWeightClassName = EntityWeightClass.getClassName(weightClass, unit.getEntity());
                pilotEntityWeightMenu = new JScrollableMenu("pilotEntityWeightMenu", entityWeightClassName);
                driverEntityWeightMenu = new JScrollableMenu("driverEntityWeightMenu", entityWeightClassName);
                gunnerEntityWeightMenu = new JScrollableMenu("gunnerEntityWeightMenu", entityWeightClassName);
                techOfficerUnitTypeMenu = new JScrollableMenu("techOfficerUnitTypeMenu", entityWeightClassName);
                crewmemberEntityWeightMenu = new JScrollableMenu("crewmemberEntityWeightMenu", entityWeightClassName);
                consoleCommanderEntityWeightMenu = new JScrollableMenu("consoleCommanderEntityWeightMenu", entityWeightClassName);
                soldierEntityWeightMenu = new JScrollableMenu("soldierEntityWeightMenu", entityWeightClassName);
                navigatorEntityWeightMenu = new JScrollableMenu("navigatorEntityWeightMenu", entityWeightClassName);
            }

            // Pilot Menu
            if (unit.canTakeMoreDrivers() || selected) {
                // Pilot Menu - Solo Pilot and VTOL Pilot Assignment
                if ((people.length == 1)
                        && (selected || unit.usesSoloPilot() || (unit.getEntity() instanceof VTOL))) {
                    final boolean valid;
                    if (unit.getEntity() instanceof Mech) {
                        valid = Stream.of(people).allMatch(person -> person.getPrimaryRole().isMechWarriorGrouping()
                                || person.getSecondaryRole().isMechWarriorGrouping());
                    } else if (unit.getEntity() instanceof Protomech) {
                        valid = Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.PROTOMECH_PILOT));
                    } else if (unit.getEntity() instanceof ConvFighter) {
                        valid = Stream.of(people).allMatch(person -> person.getPrimaryRole().isConventionalAirGrouping()
                                || person.getSecondaryRole().isConventionalAirGrouping());
                    } else if (unit.getEntity() instanceof Aero) {
                        valid = Stream.of(people).allMatch(person -> person.getPrimaryRole().isAerospaceGrouping()
                                || person.getSecondaryRole().isAerospaceGrouping());
                    } else if (unit.getEntity() instanceof VTOL) {
                        valid = Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.VTOL_PILOT));
                    } else {
                        valid = false;
                    }

                    if (valid) {
                        final JMenuItem cbPilot = new JCheckBoxMenuItem(unit.getName());
                        cbPilot.setName("cbPilot");
                        cbPilot.setSelected(selected);
                        cbPilot.addActionListener(evt -> {
                            if (selected) {
                                unit.remove(people[0], true);
                            } else {
                                final Unit oldUnit = people[0].getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(people[0], !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                unit.addPilotOrSoldier(people[0], useTransfers);
                            }
                        });
                        pilotEntityWeightMenu.add(cbPilot);
                    }
                }

                // Pilot Menu - Small Craft and JumpShip Vessel Pilot Assignment
                if (((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship))
                        && Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.VESSEL_PILOT))) {
                    final JMenuItem cbVesselPilot = new JCheckBoxMenuItem(unit.getName());
                    cbVesselPilot.setName("cbVesselPilot");
                    cbVesselPilot.setSelected(selected);
                    cbVesselPilot.addActionListener(evt -> {
                        for (final Person person : people) {
                            if (selected) {
                                unit.remove(person, true);
                            } else if (!unit.canTakeMoreDrivers()) {
                                return;
                            } else if (!unit.equals(person.getUnit())) {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                unit.addDriver(person, useTransfers);
                            }
                        }
                    });
                    pilotEntityWeightMenu.add(cbVesselPilot);
                }

                // Driver Menu - Non-VTOL Tank Driver Assignments
                if ((people.length == 1) && (unit.getEntity() instanceof Tank)) {
                    final boolean valid;
                    if (unit.getEntity() instanceof VTOL) {
                        valid = false;
                    } else {
                        switch (unit.getEntity().getMovementMode()) {
                            case NAVAL:
                            case HYDROFOIL:
                            case SUBMARINE:
                                valid = Stream.of(people).allMatch(person ->
                                        person.hasRole(PersonnelRole.NAVAL_VEHICLE_DRIVER));
                                break;
                            default:
                                valid = Stream.of(people).allMatch(person ->
                                        person.hasRole(PersonnelRole.GROUND_VEHICLE_DRIVER));
                                break;
                        }
                    }

                    if (valid) {
                        final JMenuItem cbDriver = new JCheckBoxMenuItem(unit.getName());
                        cbDriver.setName("cbDriver");
                        cbDriver.setSelected(selected);
                        cbDriver.addActionListener(evt -> {
                            if (selected) {
                                unit.remove(people[0], true);
                            } else {
                                final Unit oldUnit = people[0].getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(people[0], !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                unit.addDriver(people[0], useTransfers);
                            }
                        });
                        driverEntityWeightMenu.add(cbDriver);
                    }
                }
            }

            // Gunnery Menu
            if (unit.canTakeMoreGunners() || selected) {
                final boolean valid;
                if (unit.getEntity() instanceof Tank) {
                    valid = Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.VEHICLE_GUNNER));
                } else if ((unit.getEntity() instanceof SmallCraft) || (unit.getEntity() instanceof Jumpship)) {
                    valid = Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.VESSEL_GUNNER));
                } else {
                    valid = false;
                }

                if (valid) {
                    final JMenuItem cbGunner = new JCheckBoxMenuItem(unit.getName());
                    cbGunner.setName("cbGunner");
                    cbGunner.setSelected(selected);
                    cbGunner.addActionListener(evt -> {
                        for (final Person person : people) {
                            if (selected) {
                                unit.remove(person, true);
                            } else if (!unit.canTakeMoreGunners()) {
                                return;
                            } else if (!unit.equals(person.getUnit())) {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                unit.addPilotOrSoldier(person, useTransfers);
                            }
                        }
                    });
                    gunnerEntityWeightMenu.add(cbGunner);
                }
            }

            // Crewmember Menu
            // TODO : Rename the method to canTakeMoreCrewmembers, and update the variable names to
            // TODO : also be based on crewmembers
            if (unit.canTakeMoreVesselCrew() || selected) {
                final boolean valid;
                if (unit.getEntity() instanceof Aero) {
                    valid = Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.VESSEL_CREW));
                } else if (unit.getEntity().isSupportVehicle()) {
                    // TODO : Expand for Command and Control, Medical, Technician, and Salvage Assignments
                    valid = Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.VEHICLE_CREW));
                } else {
                    valid = false;
                }

                if (valid) {
                    final JMenuItem cbCrewmember = new JCheckBoxMenuItem(unit.getName());
                    cbCrewmember.setName("cbCrewmember");
                    cbCrewmember.setSelected(selected);
                    cbCrewmember.addActionListener(evt -> {
                        for (final Person person : people) {
                            if (selected) {
                                unit.remove(person, true);
                            } else if (!unit.canTakeMoreVesselCrew()) {
                                return;
                            } else if (!unit.equals(person.getUnit())) {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                unit.addVesselCrew(person, useTransfers);
                            }
                        }
                    });
                    pilotEntityWeightMenu.add(cbCrewmember);
                }
            }

            // Tech Officer and Console Commander Menu, currently combined as required by the current setup
            // TODO : Our implementation for Console Commanders in MekHQ makes this a necessity, but
            // TODO : I find that really terrible. We should be able to separate out tech officers
            // TODO : and Console Commanders properly. Because of this, I'm leaving the base code
            // TODO : here as the older style for now.
            if ((people.length == 1) && (unit.canTakeTechOfficer() || selected)) {
                // For a vehicle command console we will require the commander to be a driver
                // or a gunner, but not necessarily both
                if (unit.getEntity() instanceof Tank) {
                    if (people[0].canDrive(unit.getEntity()) || people[0].canGun(unit.getEntity())) {
                        final JMenuItem cbConsoleCommander = new JCheckBoxMenuItem(unit.getName());
                        cbConsoleCommander.setName("cbConsoleCommander");
                        cbConsoleCommander.setSelected(selected);
                        cbConsoleCommander.addActionListener(evt -> {
                            if (selected) {
                                unit.remove(people[0], true);
                            } else {
                                final Unit oldUnit = people[0].getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(people[0], !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                unit.setTechOfficer(people[0], useTransfers);
                            }
                        });
                        consoleCommanderEntityWeightMenu.add(cbConsoleCommander);
                    }
                } else if (people[0].canDrive(unit.getEntity()) && people[0].canGun(unit.getEntity())) {
                    final JMenuItem cbTechOfficer = new JCheckBoxMenuItem(unit.getName());
                    cbTechOfficer.setName("cbTechOfficer");
                    cbTechOfficer.setSelected(selected);
                    cbTechOfficer.addActionListener(evt -> {
                        if (selected) {
                            unit.remove(people[0], true);
                        } else {
                            final Unit oldUnit = people[0].getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(people[0], !campaign.getCampaignOptions().useTransfers());
                                useTransfers = campaign.getCampaignOptions().useTransfers();
                            }
                            unit.setTechOfficer(people[0], useTransfers);
                        }
                    });
                    techOfficerEntityWeightMenu.add(cbTechOfficer);
                }
            }

            // Soldier Menu
            if (unit.usesSoldiers() && (unit.canTakeMoreGunners() || selected)) {
                final boolean valid;
                if (unit.isConventionalInfantry()) {
                    valid = Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.SOLDIER));
                } else {
                    valid = Stream.of(people).allMatch(person -> person.hasRole(PersonnelRole.BATTLE_ARMOUR));
                }

                if (valid) {
                    final JMenuItem cbSoldier = new JCheckBoxMenuItem(unit.getName());
                    cbSoldier.setName("cbSoldier");
                    cbSoldier.setSelected(selected);
                    cbSoldier.addActionListener(evt -> {
                        for (final Person person : people) {
                            if (selected) {
                                unit.remove(person, true);
                            } else if (!unit.canTakeMoreGunners()) {
                                return;
                            } else if (!unit.equals(person.getUnit())) {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                unit.addPilotOrSoldier(person, useTransfers);
                            }
                        }
                    });
                    soldierEntityWeightMenu.add(cbSoldier);
                }
            }

            // Navigator Menu
            if ((people.length == 1) && (unit.canTakeNavigator() || selected)
                    && people[0].hasRole(PersonnelRole.VESSEL_NAVIGATOR)) {
                final JMenuItem cbNavigator = new JCheckBoxMenuItem(unit.getName());
                cbNavigator.setName("cbNavigator");
                cbNavigator.setSelected(selected);
                cbNavigator.addActionListener(evt -> {
                    if (selected) {
                        unit.remove(people[0], true);
                    } else {
                        final Unit oldUnit = people[0].getUnit();
                        boolean useTransfers = false;
                        if (oldUnit != null) {
                            oldUnit.remove(people[0], !campaign.getCampaignOptions().useTransfers());
                            useTransfers = campaign.getCampaignOptions().useTransfers();
                        }
                        unit.setNavigator(people[0], useTransfers);
                    }
                });
                navigatorEntityWeightMenu.add(cbNavigator);
            }
        }

        // Add the created menus to this
        pilotUnitTypeMenu.add(pilotEntityWeightMenu);
        pilotMenu.add(pilotUnitTypeMenu);
        add(pilotMenu);
        driverUnitTypeMenu.add(driverEntityWeightMenu);
        driverMenu.add(driverUnitTypeMenu);
        add(driverMenu);
        gunnerUnitTypeMenu.add(gunnerEntityWeightMenu);
        gunnerMenu.add(gunnerUnitTypeMenu);
        add(gunnerMenu);
        crewmemberUnitTypeMenu.add(crewmemberEntityWeightMenu);
        crewmemberMenu.add(crewmemberUnitTypeMenu);
        add(crewmemberMenu);
        techOfficerUnitTypeMenu.add(techOfficerEntityWeightMenu);
        techOfficerMenu.add(techOfficerUnitTypeMenu);
        add(techOfficerMenu);
        consoleCommanderUnitTypeMenu.add(consoleCommanderEntityWeightMenu);
        consoleCommanderMenu.add(consoleCommanderUnitTypeMenu);
        add(consoleCommanderMenu);
        soldierUnitTypeMenu.add(soldierEntityWeightMenu);
        soldierMenu.add(soldierUnitTypeMenu);
        add(soldierMenu);
        navigatorUnitTypeMenu.add(navigatorEntityWeightMenu);
        navigatorMenu.add(navigatorUnitTypeMenu);
        add(navigatorMenu);

        // Add the tech menu if there is only a single person to assign
        if (people.length == 1) {
            add(new AssignTechToUnitMenu(campaign, people[0]));
        }

        // And finally add the ability to simply unassign
        final JMenuItem miUnassignPerson = new JMenuItem(resources.getString("None.text"));
        miUnassignPerson.setName("miUnassignPerson");
        miUnassignPerson.addActionListener(evt -> {
            for (final Person person : people) {
                if (person.getUnit() != null) {
                    person.getUnit().remove(person, true);
                }

                if (!person.getTechUnits().isEmpty()) {
                    for (final Unit unit : new ArrayList<>(person.getTechUnits())) {
                        unit.remove(person, true);
                    }
                    person.clearTechUnits();
                }
            }
        });
        add(miUnassignPerson);
    }
    //endregion Initialization
}
