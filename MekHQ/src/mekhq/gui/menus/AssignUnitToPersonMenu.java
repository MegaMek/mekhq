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

import megamek.common.*;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.sorter.PersonTitleSorter;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a standard menu that takes either a unit or multiple units that require the same tech
 * type, and allows the user to assign or remove a tech from them.
 */
public class AssignUnitToPersonMenu extends JScrollableMenu {
    //region Constructors
    public AssignUnitToPersonMenu(final Campaign campaign, final Unit... units) {
        super("AssignUnitToPersonMenu");
        initialize(campaign, units);
    }
    //endregion Constructors

    //region Initialization
    private void initialize(final Campaign campaign, final Unit... units) {
        // Immediate Return for Illegal Assignments:
        // 1) No units to assign
        // 2) Any units are currently deployed
        if ((units.length == 0) || (units.)) {
            return;
        }

        // Initialize Menu
        setText(resources.getString("AssignUnitToPersonMenu.title"));

        // Only assign non-tech personnel if the following is met:
        // 1) Only a single unit is selected
        // 2)
        if (units.length == 1) {
            // Person Assignment Menus
            final JScrollableMenu pilotMenu = new JScrollableMenu("pilotMenu", resources.getString("pilotMenu.text"));
            final JScrollableMenu driverMenu = new JScrollableMenu("driverMenu", resources.getString("driverMenu.text"));
            final JScrollableMenu gunnerMenu = new JScrollableMenu("gunnerMenu", resources.getString("gunnerMenu.text"));
            final JScrollableMenu crewmemberMenu = new JScrollableMenu("crewmemberMenu", resources.getString("crewmemberMenu.text"));
            final JScrollableMenu techOfficerMenu = new JScrollableMenu("techOfficerMenu", resources.getString("techOfficerMenu.text"));
            final JScrollableMenu consoleCommanderMenu = new JScrollableMenu("consoleCommanderMenu", resources.getString("consoleCommanderMenu.text"));
            final JScrollableMenu soldierMenu = new JScrollableMenu("soldierMenu", resources.getString("soldierMenu.text"));
            final JScrollableMenu navigatorMenu = new JScrollableMenu("navigatorMenu", resources.getString("navigatorMenu.text"));

            // Parsing Booleans
            final boolean canTakeMoreDrivers = units[0].canTakeMoreDrivers();
            final boolean usesSoloPilot = units[0].usesSoloPilot();
            final boolean isVTOL = units[0].getEntity() instanceof VTOL;
            final boolean isMech = units[0].getEntity() instanceof Mech;
            final boolean isProtoMech = units[0].getEntity() instanceof Protomech;
            final boolean isConventionalAircraft = units[0].getEntity() instanceof ConvFighter;
            final boolean isSmallCraftOrJumpShip = (units[0].getEntity() instanceof SmallCraft)
                    || (units[0].getEntity() instanceof Jumpship);
            final boolean isTank = units[0].getEntity() instanceof Tank;
            final boolean canTakeMoreGunners = units[0].canTakeMoreGunners();
            final boolean isAero = units[0].getEntity() instanceof Aero;
            final boolean isSupportVehicle = units[0].getEntity().isSupportVehicle();
            final boolean canTakeTechOfficer = units[0].canTakeTechOfficer();
            final boolean usesSoldiers = units[0].usesSoldiers();
            final boolean isConventionalInfantry = units[0].isConventionalInfantry();
            final boolean canTakeNavigator = units[0].canTakeNavigator();

            // Skip People (by filtering them out) if they are:
            // 1) Already assigned to this unit
            // 2) Dependent Primary Role
            // 3) Astech Primary role with the Medical, Administrator, or None Secondary Roles
            // 4) Medical Primary role with the Astech, Administrator, or None Secondary Roles
            // 5) Administrator Primary Role with Astech, Medical, Administrator, or None Secondary Roles
            // Then sorts the remainder based on their full title
            final List<Person> personnel = campaign.getPersonnel().stream()
                    .filter(person -> !units[0].equals(person.getUnit()))
                    .filter(person -> person.getPrimaryRole().isDependent())
                    .filter(person -> !person.getPrimaryRole().isAstech()
                            || !(person.getSecondaryRole().isMedicalStaff()
                            || person.getSecondaryRole().isAdministrator()
                            || person.getSecondaryRole().isNone()))
                    .filter(person -> !person.getPrimaryRole().isMedicalStaff()
                            || !(person.getSecondaryRole().isAstech()
                            || person.getSecondaryRole().isAdministrator()
                            || person.getSecondaryRole().isNone()))
                    .filter(person -> !person.getPrimaryRole().isAdministrator()
                            || !(person.getSecondaryRole().isAstech()
                            || person.getSecondaryRole().isMedicalStaff()
                            || person.getSecondaryRole().isAdministrator()
                            || person.getSecondaryRole().isNone()))
                    .sorted(new PersonTitleSorter().reversed())
                    .collect(Collectors.toList());
            for (final Person person : personnel) {
                // Pilot Menu
                if (canTakeMoreDrivers) {
                    // Pilot Menu - Solo Pilot and VTOL Pilot Assignment
                    if (usesSoloPilot || isVTOL) {
                        final boolean valid;
                        if (isMech) {
                            valid = person.getPrimaryRole().isMechWarriorGrouping()
                                    || person.getSecondaryRole().isMechWarriorGrouping();
                        } else if (isProtoMech) {
                            valid = person.hasRole(PersonnelRole.PROTOMECH_PILOT);
                        } else if (isConventionalAircraft) {
                            valid = person.getPrimaryRole().isConventionalAirGrouping()
                                    || person.getSecondaryRole().isConventionalAirGrouping();
                        } else if (isAero) {
                            valid = person.getPrimaryRole().isAerospaceGrouping()
                                    || person.getSecondaryRole().isAerospaceGrouping();
                        } else if (isVTOL) {
                            valid = person.hasRole(PersonnelRole.VTOL_PILOT);
                        } else {
                            valid = false;
                        }

                        if (valid) {
                            final JMenuItem miPilot = new JMenuItem(person.getFullTitle());
                            miPilot.setName("miPilot");
                            miPilot.addActionListener(evt -> {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                units[0].addPilotOrSoldier(person, useTransfers);
                            });
                            pilotMenu.add(miPilot);
                        }
                    }

                    // Pilot Menu - Small Craft and JumpShip Vessel Pilot Assignment
                    if (isSmallCraftOrJumpShip && person.hasRole(PersonnelRole.VESSEL_PILOT)) {
                        final JMenuItem miVesselPilot = new JMenuItem(person.getFullTitle());
                        miVesselPilot.setName("miVesselPilot");
                        miVesselPilot.addActionListener(evt -> {
                            final Unit oldUnit = person.getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                useTransfers = campaign.getCampaignOptions().useTransfers();
                            }
                            units[0].addDriver(person, useTransfers);
                        });
                        pilotMenu.add(miVesselPilot);
                    }

                    // Driver Menu - Non-VTOL Tank Driver Assignments
                    if (isTank && !isVTOL) {
                        final boolean valid;
                        switch (units[0].getEntity().getMovementMode()) {
                            case NAVAL:
                            case HYDROFOIL:
                            case SUBMARINE:
                                valid = person.hasRole(PersonnelRole.NAVAL_VEHICLE_DRIVER);
                                break;
                            default:
                                valid = person.hasRole(PersonnelRole.GROUND_VEHICLE_DRIVER);
                                break;
                        }

                        if (valid) {
                            final JMenuItem miDriver = new JMenuItem(person.getFullTitle());
                            miDriver.setName("miDriver");
                            miDriver.addActionListener(evt -> {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                units[0].addDriver(person, useTransfers);
                            });
                            driverMenu.add(miDriver);
                        }
                    }
                }

                // Gunnery Menu
                if (canTakeMoreGunners) {
                    final boolean valid;
                    if (isTank) {
                        valid = person.hasRole(PersonnelRole.VEHICLE_GUNNER);
                    } else if (isSmallCraftOrJumpShip) {
                        valid = person.hasRole(PersonnelRole.VESSEL_GUNNER);
                    } else {
                        valid = false;
                    }

                    if (valid) {
                        final JMenuItem miGunner = new JMenuItem(person.getFullTitle());
                        miGunner.setName("miGunner");
                        miGunner.addActionListener(evt -> {
                            final Unit oldUnit = person.getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                useTransfers = campaign.getCampaignOptions().useTransfers();
                            }
                            units[0].addPilotOrSoldier(person, useTransfers);
                        });
                        gunnerMenu.add(miGunner);
                    }
                }

                // Crewmember Menu
                // TODO : Rename the method to canTakeMoreCrewmembers, and update the variable names to
                // TODO : also be based on crewmembers
                if (units[0].canTakeMoreVesselCrew()) {
                    final boolean valid;
                    if (isAero) {
                        valid = person.hasRole(PersonnelRole.VEHICLE_CREW);
                    } else if (isSupportVehicle) {
                        // TODO : Expand for Command and Control, Medical, Technician, and Salvage Assignments
                        valid = person.hasRole(PersonnelRole.VEHICLE_CREW);
                    } else {
                        valid = false;
                    }

                    if (valid) {
                        final JMenuItem miCrewmember = new JMenuItem(person.getFullTitle());
                        miCrewmember.setName("miCrewmember");
                        miCrewmember.addActionListener(evt -> {
                            final Unit oldUnit = person.getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                useTransfers = campaign.getCampaignOptions().useTransfers();
                            }
                            units[0].addVesselCrew(person, useTransfers);
                        });
                        crewmemberMenu.add(miCrewmember);
                    }
                }

                // Tech Officer and Console Commander Menu, currently combined as required by the current setup
                // TODO : Our implementation for Console Commanders in MekHQ makes this a necessity, but
                // TODO : I find that really terrible. We should be able to separate out tech officers
                // TODO : and Console Commanders properly. Because of this, I'm leaving the base code
                // TODO : here as the older style for now.
                if (canTakeTechOfficer) {
                    // For a vehicle command console we will require the commander to be a driver
                    // or a gunner, but not necessarily both
                    if (isTank) {
                        if (person.canDrive(units[0].getEntity()) || person.canGun(units[0].getEntity())) {
                            final JMenuItem miConsoleCommander = new JMenuItem(person.getFullTitle());
                            miConsoleCommander.setName("miConsoleCommander");
                            miConsoleCommander.addActionListener(evt -> {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                    useTransfers = campaign.getCampaignOptions().useTransfers();
                                }
                                units[0].setTechOfficer(person, useTransfers);
                            });
                            consoleCommanderMenu.add(miConsoleCommander);
                        }
                    } else if (person.canDrive(units[0].getEntity()) && person.canGun(units[0].getEntity())) {
                        final JMenuItem miTechOfficer = new JMenuItem(person.getFullTitle());
                        miTechOfficer.setName("miTechOfficer");
                        miTechOfficer.addActionListener(evt -> {
                            final Unit oldUnit = person.getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                                useTransfers = campaign.getCampaignOptions().useTransfers();
                            }
                            units[0].setTechOfficer(person, useTransfers);
                        });
                        techOfficerMenu.add(miTechOfficer);
                    }
                }

                // Soldier Menu
                if (usesSoldiers && canTakeMoreGunners
                        && person.hasRole(isConventionalInfantry ? PersonnelRole.SOLDIER : PersonnelRole.BATTLE_ARMOUR)) {
                    final JMenuItem miSoldier = new JMenuItem(person.getFullTitle());
                    miSoldier.setName("miSoldier");
                    miSoldier.addActionListener(evt -> {
                        final Unit oldUnit = person.getUnit();
                        boolean useTransfers = false;
                        if (oldUnit != null) {
                            oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                            useTransfers = campaign.getCampaignOptions().useTransfers();
                        }
                        units[0].addPilotOrSoldier(person, useTransfers);
                    });
                    soldierMenu.add(miSoldier);
                }

                // Navigator Menu
                if (canTakeNavigator && person.hasRole(PersonnelRole.VESSEL_NAVIGATOR)) {
                    final JMenuItem miNavigator = new JMenuItem(person.getFullTitle());
                    miNavigator.setName("miNavigator");
                    miNavigator.addActionListener(evt -> {
                        final Unit oldUnit = person.getUnit();
                        boolean useTransfers = false;
                        if (oldUnit != null) {
                            oldUnit.remove(person, !campaign.getCampaignOptions().useTransfers());
                            useTransfers = campaign.getCampaignOptions().useTransfers();
                        }
                        units[0].setNavigator(person, useTransfers);
                    });
                    navigatorMenu.add(miNavigator);
                }
            }

            add(pilotMenu);
            add(driverMenu);
            add(gunnerMenu);
            add(crewmemberMenu);
            add(techOfficerMenu);
            add(consoleCommanderMenu);
            add(soldierMenu);
            add(navigatorMenu);
        }

        // Assign Tech to Unit Menu
        add(new AssignUnitToTechMenu(campaign, units));

        // Finally, add the ability to simply unassign... provided at least one of the units has
        // any crew or a tech
        if (Stream.of(units).anyMatch(unit -> (unit.getTech() != null) || !unit.getCrew().isEmpty())) {
            final JMenuItem miUnassignCrew = new JMenuItem(resources.getString("miUnassignCrew.text"));
            miUnassignCrew.setName("miUnassignCrew");
            miUnassignCrew.addActionListener(evt -> {
                for (final Unit unit : units) {
                    unit.clearCrew();
                }
            });
            add(miUnassignCrew);
        }
    }
    //endregion Initialization
}
