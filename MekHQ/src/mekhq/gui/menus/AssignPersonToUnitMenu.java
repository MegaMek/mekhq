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
        // Immediate Return for Illegal Assignment
        // 1) No people to assign
        // 2) Any of the people you are trying to assign are currently deployed
        if ((people.length == 0) || Stream.of(people).anyMatch(Person::isDeployed)) {
            return;
        }

        // Initialize Menu
        setText(resources.getString("AssignPersonToUnitMenu.title"));

        // Impossible Assignments:
        // 1) All people must be active
        // 2) All people must be non-prisoners (bondsmen should be assignable to units)
        // 3) All people must not be primary civilians
        // 4) All people must share one of their non-civilian professions
        boolean assign = Stream.of(people).noneMatch(person -> !person.getStatus().isActive()
                || person.getPrisonerStatus().isCurrentPrisoner()
                || Profession.getProfessionFromPersonnelRole(person.getPrimaryRole()).isCivilian());

        if (assign) {
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
                    assign = false;
                    break;
                }
            }
        }

        if (assign) {
            // Person Assignment Menus
            final JScrollableMenu pilotMenu = new JScrollableMenu("pilotMenu", resources.getString("asPilotMenu.text"));
            JScrollableMenu pilotUnitTypeMenu = new JScrollableMenu("pilotUnitTypeMenu");
            JMenu pilotEntityWeightMenu = new JMenu();
            final JScrollableMenu driverMenu = new JScrollableMenu("driverMenu", resources.getString("asDriverMenu.text"));
            JScrollableMenu driverUnitTypeMenu = new JScrollableMenu("driverUnitTypeMenu");
            JMenu driverEntityWeightMenu = new JMenu();
            final JScrollableMenu gunnerMenu = new JScrollableMenu("gunnerMenu", resources.getString("asGunnerMenu.text"));
            JScrollableMenu gunnerUnitTypeMenu = new JScrollableMenu("gunnerUnitTypeMenu");
            JMenu gunnerEntityWeightMenu = new JMenu();
            final JScrollableMenu crewmemberMenu = new JScrollableMenu("crewmemberMenu", resources.getString("asCrewmemberMenu.text"));
            JScrollableMenu crewmemberUnitTypeMenu = new JScrollableMenu("crewmemberUnitTypeMenu");
            JMenu crewmemberEntityWeightMenu = new JMenu();
            final JScrollableMenu techOfficerMenu = new JScrollableMenu("techOfficerMenu", resources.getString("asTechOfficerMenu.text"));
            JScrollableMenu techOfficerUnitTypeMenu = new JScrollableMenu("techOfficerUnitTypeMenu");
            JMenu techOfficerEntityWeightMenu = new JMenu();
            final JScrollableMenu consoleCommanderMenu = new JScrollableMenu("consoleCommanderMenu", resources.getString("asConsoleCommanderMenu.text"));
            JScrollableMenu consoleCommanderUnitTypeMenu = new JScrollableMenu("consoleCommanderUnitTypeMenu");
            JMenu consoleCommanderEntityWeightMenu = new JMenu();
            final JScrollableMenu soldierMenu = new JScrollableMenu("soldierMenu", resources.getString("asSoldierMenu.text"));
            JScrollableMenu soldierUnitTypeMenu = new JScrollableMenu("soldierUnitTypeMenu");
            JMenu soldierEntityWeightMenu = new JMenu();
            final JScrollableMenu navigatorMenu = new JScrollableMenu("navigatorMenu", resources.getString("asNavigatorMenu.text"));
            JScrollableMenu navigatorUnitTypeMenu = new JScrollableMenu("navigatorUnitTypeMenu");
            JMenu navigatorEntityWeightMenu = new JMenu();

            // Parsing Booleans
            final boolean singlePerson = people.length == 1;
            final boolean areAllBattleMechPilots = Stream.of(people)
                    .allMatch(person -> person.getPrimaryRole().isMechWarriorGrouping()
                            || person.getSecondaryRole().isMechWarriorGrouping());
            final boolean areAllProtoMechPilots = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.PROTOMECH_PILOT));
            final boolean areAllConventionalAerospacePilots = Stream.of(people)
                    .allMatch(person -> person.getPrimaryRole().isConventionalAirGrouping()
                            || person.getSecondaryRole().isConventionalAirGrouping());
            final boolean areAllAerospacePilots = Stream.of(people)
                    .allMatch(person -> person.getPrimaryRole().isAerospaceGrouping()
                            || person.getSecondaryRole().isAerospaceGrouping());
            final boolean areAllVTOLPilots = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.VTOL_PILOT));
            final boolean areAllVesselPilots = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.VESSEL_PILOT));
            final boolean areAllNavalVehicleDrivers = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.NAVAL_VEHICLE_DRIVER));
            final boolean areAllGroundVehicleDrivers = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.GROUND_VEHICLE_DRIVER));
            final boolean areAllVehicleGunners = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.VEHICLE_GUNNER));
            final boolean areAllVesselGunners = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.VESSEL_GUNNER));
            final boolean areAllVesselCrew = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.VESSEL_CREW));
            final boolean areAllVehicleCrew = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.VEHICLE_CREW));
            final boolean areAllSoldiers = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.SOLDIER));
            final boolean areAllBattleArmourPilots = Stream.of(people)
                    .allMatch(person -> person.hasRole(PersonnelRole.BATTLE_ARMOUR));

            // Parsing Variables
            int unitType = -1;
            int weightClass = -1;

            final List<Unit> units = HangarSorter.defaultSorting()
                    .sort(campaign.getHangar().getUnitsStream().filter(Unit::isAvailable))
                    .collect(Collectors.toList());
            for (final Unit unit : units) {
                Entity entity = unit.getEntity();
                if (entity.getUnitType() != unitType) {
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
                    unitType = entity.getUnitType();
                    weightClass = entity.getWeightClass();

                    // And create the new menus
                    final String unitTypeName = UnitType.getTypeDisplayableName(unitType);
                    final String entityWeightClassName = EntityWeightClass.getClassName(weightClass, entity);
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
                } else if (entity.getWeightClass() != weightClass) {
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
                    weightClass = entity.getWeightClass();

                    // And create the new Entity Weight Class menus
                    final String entityWeightClassName = EntityWeightClass.getClassName(weightClass, entity);
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
                if (unit.canTakeMoreDrivers()) {
                    // Pilot Menu - Solo Pilot and VTOL Pilot Assignment
                    if (singlePerson && (unit.usesSoloPilot() || (entity instanceof VTOL) || entity.isSuperHeavy() || entity.isTripodMek())) {
                        final boolean valid;
                        if (entity instanceof Mech) {
                            valid = areAllBattleMechPilots;
                        } else if (entity instanceof Protomech) {
                            valid = areAllProtoMechPilots;
                        } else if (entity instanceof ConvFighter) {
                            valid = areAllConventionalAerospacePilots;
                        } else if (entity instanceof Aero) {
                            valid = areAllAerospacePilots;
                        } else if (entity instanceof VTOL) {
                            valid = areAllVTOLPilots;
                        } else {
                            valid = false;
                        }

                        if (valid) {
                            final JMenuItem miPilot = new JMenuItem(unit.getName());
                            miPilot.setName("miPilot");
                            miPilot.setForeground(unit.determineForegroundColor("Menu"));
                            miPilot.setBackground(unit.determineBackgroundColor("Menu"));
                            miPilot.addActionListener(evt -> {
                                final Unit oldUnit = people[0].getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(people[0], !campaign.getCampaignOptions().isUseTransfers());
                                    useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                }

                                if (entity instanceof VTOL) {
                                    unit.addDriver(people[0], useTransfers);
                                } else {
                                    unit.addPilotOrSoldier(people[0], useTransfers);
                                }
                            });
                            pilotEntityWeightMenu.add(miPilot);
                        }
                    }

                    // Pilot Menu - Small Craft and JumpShip Vessel Pilot Assignment
                    if (((entity instanceof SmallCraft) || (entity instanceof Jumpship))
                            && areAllVesselPilots) {
                        final JMenuItem miVesselPilot = new JMenuItem(unit.getName());
                        miVesselPilot.setName("miVesselPilot");
                        miVesselPilot.setForeground(unit.determineForegroundColor("Menu"));
                        miVesselPilot.setBackground(unit.determineBackgroundColor("Menu"));
                        miVesselPilot.addActionListener(evt -> {
                            for (final Person person : people) {
                                if (!unit.canTakeMoreDrivers()) {
                                    return;
                                } else if (!unit.equals(person.getUnit())) {
                                    final Unit oldUnit = person.getUnit();
                                    boolean useTransfers = false;
                                    if (oldUnit != null) {
                                        oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                        useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                    }
                                    unit.addDriver(person, useTransfers);
                                }
                            }
                        });
                        pilotEntityWeightMenu.add(miVesselPilot);
                    }

                    // Driver Menu - Non-VTOL Tank Driver Assignments
                    if (singlePerson && (entity instanceof Tank)
                            && !(entity instanceof VTOL)) {
                        if (entity.getMovementMode().isMarine()
                                ? areAllNavalVehicleDrivers : areAllGroundVehicleDrivers) {
                            final JMenuItem miDriver = new JMenuItem(unit.getName());
                            miDriver.setName("miDriver");
                            miDriver.setForeground(unit.determineForegroundColor("Menu"));
                            miDriver.setBackground(unit.determineBackgroundColor("Menu"));
                            miDriver.addActionListener(evt -> {
                                final Unit oldUnit = people[0].getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(people[0], !campaign.getCampaignOptions().isUseTransfers());
                                    useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                }
                                unit.addDriver(people[0], useTransfers);
                            });
                            driverEntityWeightMenu.add(miDriver);
                        }
                    }
                }

                // Gunnery Menu
                if (unit.canTakeMoreGunners()) {
                    final boolean valid;
                    if (entity instanceof Tank) {
                        valid = areAllVehicleGunners;
                    } else if ((entity instanceof SmallCraft)
                            || (entity instanceof Jumpship)) {
                        valid = areAllVesselGunners;
                    } else if (entity.isTripodMek() || entity.isSuperHeavy()) {
                        valid = areAllBattleMechPilots;
                    } else {
                        valid = false;
                    }

                    if (valid) {
                        final JMenuItem miGunner = new JMenuItem(unit.getName());
                        miGunner.setName("miGunner");
                        miGunner.setForeground(unit.determineForegroundColor("Menu"));
                        miGunner.setBackground(unit.determineBackgroundColor("Menu"));
                        miGunner.addActionListener(evt -> {
                            for (final Person person : people) {
                                if (!unit.canTakeMoreGunners()) {
                                    return;
                                } else if (!unit.equals(person.getUnit())) {
                                    final Unit oldUnit = person.getUnit();
                                    boolean useTransfers = false;
                                    if (oldUnit != null) {
                                        oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                        useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                    }
                                    unit.addGunner(person, useTransfers);
                                }
                            }
                        });
                        gunnerEntityWeightMenu.add(miGunner);
                    }
                }

                // Crewmember Menu
                // TODO : Rename the method to canTakeMoreCrewmembers, and update the variable names to
                // TODO : also be based on crewmembers
                if (unit.canTakeMoreVesselCrew()) {
                    final boolean valid;
                    if (entity instanceof Aero) {
                        valid = areAllVesselCrew;
                    } else if (entity.isSupportVehicle()) {
                        // TODO : Expand for Command and Control, Medical, Technician, and Salvage Assignments
                        valid = areAllVehicleCrew;
                    } else {
                        valid = false;
                    }

                    if (valid) {
                        final JMenuItem miCrewmember = new JMenuItem(unit.getName());
                        miCrewmember.setName("miCrewmember");
                        miCrewmember.setForeground(unit.determineForegroundColor("Menu"));
                        miCrewmember.setBackground(unit.determineBackgroundColor("Menu"));
                        miCrewmember.addActionListener(evt -> {
                            for (final Person person : people) {
                                if (!unit.canTakeMoreVesselCrew()) {
                                    return;
                                } else if (!unit.equals(person.getUnit())) {
                                    final Unit oldUnit = person.getUnit();
                                    boolean useTransfers = false;
                                    if (oldUnit != null) {
                                        oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                        useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                    }
                                    unit.addVesselCrew(person, useTransfers);
                                }
                            }
                        });
                        pilotEntityWeightMenu.add(miCrewmember);
                    }
                }

                // Tech Officer and Console Commander Menu, currently combined as required by the current setup
                // TODO : Our implementation for Console Commanders in MekHQ makes this a necessity, but
                // TODO : I find that really terrible. We should be able to separate out tech officers
                // TODO : and Console Commanders properly. Because of this, I'm leaving the base code
                // TODO : here as the older style for now.
                if (singlePerson && unit.canTakeTechOfficer()) {
                    // For a vehicle command console we will require the commander to be a driver
                    // or a gunner, but not necessarily both
                    if (entity instanceof Tank) {
                        if (people[0].canDrive(entity) || people[0].canGun(entity)) {
                            final JMenuItem miConsoleCommander = new JMenuItem(unit.getName());
                            miConsoleCommander.setName("miConsoleCommander");
                            miConsoleCommander.setForeground(unit.determineForegroundColor("Menu"));
                            miConsoleCommander.setBackground(unit.determineBackgroundColor("Menu"));
                            miConsoleCommander.addActionListener(evt -> {
                                final Unit oldUnit = people[0].getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(people[0], !campaign.getCampaignOptions().isUseTransfers());
                                    useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                }
                                unit.setTechOfficer(people[0], useTransfers);
                            });
                            consoleCommanderEntityWeightMenu.add(miConsoleCommander);
                        }
                    } else if (people[0].canDrive(entity) && people[0].canGun(entity)) {
                        final JMenuItem miTechOfficer = new JMenuItem(unit.getName());
                        miTechOfficer.setName("miTechOfficer");
                        miTechOfficer.setForeground(unit.determineForegroundColor("Menu"));
                        miTechOfficer.setBackground(unit.determineBackgroundColor("Menu"));
                        miTechOfficer.addActionListener(evt -> {
                            final Unit oldUnit = people[0].getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(people[0], !campaign.getCampaignOptions().isUseTransfers());
                                useTransfers = campaign.getCampaignOptions().isUseTransfers();
                            }
                            unit.setTechOfficer(people[0], useTransfers);
                        });
                        techOfficerEntityWeightMenu.add(miTechOfficer);
                    }
                }

                // Soldier Menu
                if (unit.usesSoldiers() && unit.canTakeMoreGunners()) {
                    final boolean valid = unit.isConventionalInfantry() ? areAllSoldiers : areAllBattleArmourPilots;

                    if (valid) {
                        final JMenuItem miSoldier = new JMenuItem(unit.getName());
                        miSoldier.setName("miSoldier");
                        miSoldier.setForeground(unit.determineForegroundColor("Menu"));
                        miSoldier.setBackground(unit.determineBackgroundColor("Menu"));
                        miSoldier.addActionListener(evt -> {
                            for (final Person person : people) {
                                if (!unit.canTakeMoreGunners()) {
                                    return;
                                } else if (!unit.equals(person.getUnit())) {
                                    final Unit oldUnit = person.getUnit();
                                    boolean useTransfers = false;
                                    if (oldUnit != null) {
                                        oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                        useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                    }
                                    unit.addPilotOrSoldier(person, useTransfers);
                                }
                            }
                        });
                        soldierEntityWeightMenu.add(miSoldier);
                    }
                }

                // Navigator Menu
                if (singlePerson && unit.canTakeNavigator()
                        && people[0].hasRole(PersonnelRole.VESSEL_NAVIGATOR)) {
                    final JMenuItem miNavigator = new JMenuItem(unit.getName());
                    miNavigator.setName("miNavigator");
                    miNavigator.setForeground(unit.determineForegroundColor("Menu"));
                    miNavigator.setBackground(unit.determineBackgroundColor("Menu"));
                    miNavigator.addActionListener(evt -> {
                        final Unit oldUnit = people[0].getUnit();
                        boolean useTransfers = false;
                        if (oldUnit != null) {
                            oldUnit.remove(people[0], !campaign.getCampaignOptions().isUseTransfers());
                            useTransfers = campaign.getCampaignOptions().isUseTransfers();
                        }
                        unit.setNavigator(people[0], useTransfers);
                    });
                    navigatorEntityWeightMenu.add(miNavigator);
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
            if (singlePerson) {
                add(new AssignTechToUnitMenu(campaign, people[0]));
            }
        }

        // Finally, add the ability to simply unassign if there's a person assigned to anything
        if (Stream.of(people).anyMatch(person -> (person.getUnit() != null)
                || !person.getTechUnits().isEmpty())) {
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
    }
    //endregion Initialization
}
