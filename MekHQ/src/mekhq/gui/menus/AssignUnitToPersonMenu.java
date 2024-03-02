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
import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.sorter.PersonTitleSorter;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.util.ArrayList;
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
        // 2) Any units are currently unavailable
        if ((units.length == 0) || Stream.of(units).anyMatch(unit -> !unit.isAvailable())) {
            return;
        }

        // Initialize Menu
        setText(resources.getString("AssignUnitToPersonMenu.title"));

        // Only assign non-tech personnel if the following is met:
        // 1) Only a single unit is selected
        if (units.length == 1) {
            createPersonAssignmentMenus(campaign, units);
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

    private void createPersonAssignmentMenus(final Campaign campaign, final Unit... units) {
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
        final Entity entity = units[0].getEntity();
        final boolean canTakeMoreDrivers = units[0].canTakeMoreDrivers();
        final boolean usesSoloPilot = units[0].usesSoloPilot();
        final boolean isVTOL = entity instanceof VTOL;
        final boolean isMech = entity instanceof Mech;
        final boolean isTripod = entity instanceof TripodMech;
        final boolean isSuperHeavyMech = isMech && entity.isSuperHeavy();
        final boolean isProtoMech = entity instanceof Protomech;
        final boolean isConventionalAircraft = entity instanceof ConvFighter;
        final boolean isSmallCraftOrJumpShip = (entity instanceof SmallCraft)
                || (entity instanceof Jumpship);
        final boolean isTank = entity instanceof Tank;
        final boolean canTakeMoreGunners = units[0].canTakeMoreGunners();
        final boolean isAero = entity instanceof Aero;
        final boolean canTakeTechOfficer = units[0].canTakeTechOfficer();
        final boolean usesSoldiers = units[0].usesSoldiers();
        final boolean isConventionalInfantry = units[0].isConventionalInfantry();

        // Skip People (by filtering them out) if they are:
        // 1) Inactive
        // 2) A Prisoner
        // 3) Already assigned to a unit
        // 4) Civilian Primary Role
        // 5) Astech Primary role with the Medical, Administrator, or None Secondary Roles
        // 6) Medical Primary role with the Astech, Administrator, or None Secondary Roles
        // 7) Administrator Primary Role with Astech, Medical, Administrator, or None Secondary Roles
        // Then sorts the remainder based on their full title
        List<Person> personnel = campaign.getPersonnel().stream()
                .filter(person -> person.getStatus().isActive())
                .filter(person -> !person.getPrisonerStatus().isCurrentPrisoner())
                .filter(person -> person.getUnit() == null)
                .filter(person -> !Profession.getProfessionFromPersonnelRole(person.getPrimaryRole()).isCivilian())
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

        if (personnel.isEmpty()) {
            return;
        }

        // The order of this if statement is required to properly filter based on the unit type
        if (isMech) {
            personnel = personnel.stream()
                    .filter(person -> person.getPrimaryRole().isMechWarriorGrouping()
                            || person.getSecondaryRole().isMechWarriorGrouping())
                    .collect(Collectors.toList());
        } else if (isVTOL) {
            personnel = personnel.stream()
                    .filter(person -> person.getPrimaryRole().isVTOLCrew()
                            || person.getSecondaryRole().isVTOLCrew())
                    .collect(Collectors.toList());
        } else if (isTank) {
            personnel = personnel.stream()
                    .filter(person -> person.getPrimaryRole().isVehicleCrewmember()
                            || person.getSecondaryRole().isVehicleCrewmember())
                    .collect(Collectors.toList());
        } else if (isSmallCraftOrJumpShip) {
            personnel = personnel.stream()
                    .filter(person -> person.getPrimaryRole().isVesselCrewmember()
                            || person.getSecondaryRole().isVesselCrewmember())
                    .collect(Collectors.toList());
        } else if (isConventionalAircraft) {
            personnel = personnel.stream()
                    .filter(person -> person.getPrimaryRole().isConventionalAirGrouping()
                            || person.getSecondaryRole().isConventionalAirGrouping())
                    .collect(Collectors.toList());
        } else if (isAero) {
            personnel = personnel.stream()
                    .filter(person -> person.getPrimaryRole().isAerospaceGrouping()
                            || person.getSecondaryRole().isAerospaceGrouping())
                    .collect(Collectors.toList());
        } else if (isProtoMech) {
            personnel = personnel.stream()
                    .filter(person -> person.hasRole(PersonnelRole.PROTOMECH_PILOT))
                    .collect(Collectors.toList());
        } else if (usesSoldiers) {
            personnel = personnel.stream()
                    .filter(person -> person.hasRole(isConventionalInfantry
                            ? PersonnelRole.SOLDIER : PersonnelRole.BATTLE_ARMOUR))
                    .collect(Collectors.toList());
        } else {
            LogManager.getLogger().error("Unhandled entity type of " + units[0].getEntity().getClass().toString());
            return;
        }

        if (personnel.isEmpty()) {
            return;
        }

        List<Person> filteredPersonnel;

        // Pilot Menu
        if (canTakeMoreDrivers) {
            // Pilot Menu
            if (usesSoloPilot || isVTOL || isSmallCraftOrJumpShip || isSuperHeavyMech || isTripod) {
                if (isMech) {
                    filteredPersonnel = personnel.stream()
                            .filter(person -> person.getPrimaryRole().isMechWarriorGrouping()
                                    || person.getSecondaryRole().isMechWarriorGrouping())
                            .collect(Collectors.toList());
                } else if (isProtoMech) {
                    filteredPersonnel = personnel.stream()
                            .filter(person -> person.hasRole(PersonnelRole.PROTOMECH_PILOT))
                            .collect(Collectors.toList());
                } else if (isSmallCraftOrJumpShip) {
                    filteredPersonnel = personnel.stream()
                            .filter(person -> person.hasRole(PersonnelRole.VESSEL_PILOT))
                            .collect(Collectors.toList());
                } else if (isConventionalAircraft) {
                    filteredPersonnel = personnel.stream()
                            .filter(person -> person.getPrimaryRole().isConventionalAirGrouping()
                                    || person.getSecondaryRole().isConventionalAirGrouping())
                            .collect(Collectors.toList());
                } else if (isAero) {
                    filteredPersonnel = personnel.stream()
                            .filter(person -> person.getPrimaryRole().isAerospaceGrouping()
                                    || person.getSecondaryRole().isAerospaceGrouping())
                            .collect(Collectors.toList());
                } else if (isVTOL) {
                    filteredPersonnel = personnel.stream()
                            .filter(person -> person.hasRole(PersonnelRole.VTOL_PILOT))
                            .collect(Collectors.toList());
                } else {
                    LogManager.getLogger().warn("Attempting to assign pilot to unknown unit type of " + units[0].getEntity().getClass());
                    filteredPersonnel = new ArrayList<>();
                }


                if (!filteredPersonnel.isEmpty()) {
                    // Create the SkillLevel Submenus
                    final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu", SkillLevel.LEGENDARY.toString());
                    final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                    final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                    final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                    final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                    final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                    final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu", SkillLevel.ULTRA_GREEN.toString());

                    // Add the person to the proper menu
                    for (final Person person : filteredPersonnel) {
                        final JScrollableMenu subMenu;
                        final SkillLevel skillLevel;
                        if (isMech) {
                            skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isMechWarriorGrouping());
                        } else if (isProtoMech) {
                            skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isProtoMechPilot());
                        } else if (isSmallCraftOrJumpShip) {
                            skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isVesselPilot());
                        } else if (isConventionalAircraft) {
                            skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isConventionalAirGrouping());
                        } else if (isAero) {
                            skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isAerospaceGrouping());
                        } else { // it's a VTOL
                            skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isVTOLPilot());
                        }

                        switch (skillLevel) {
                            case LEGENDARY:
                                subMenu = legendaryMenu;
                                break;
                            case HEROIC:
                                subMenu = heroicMenu;
                                break;
                            case ELITE:
                                subMenu = eliteMenu;
                                break;
                            case VETERAN:
                                subMenu = veteranMenu;
                                break;
                            case REGULAR:
                                subMenu = regularMenu;
                                break;
                            case GREEN:
                                subMenu = greenMenu;
                                break;
                            case ULTRA_GREEN:
                                subMenu = ultraGreenMenu;
                                break;
                            default:
                                subMenu = null;
                                break;
                        }

                        if (subMenu != null) {
                            final JMenuItem miPilot = new JMenuItem(person.getFullTitle());
                            miPilot.setName("miPilot");
                            miPilot.addActionListener(evt -> {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                    useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                }

                                if (isVTOL || isSmallCraftOrJumpShip) {
                                    units[0].addDriver(person, useTransfers);
                                } else {
                                    units[0].addPilotOrSoldier(person, useTransfers);
                                }
                            });
                            subMenu.add(miPilot);
                        }
                    }

                    pilotMenu.add(eliteMenu);
                    pilotMenu.add(veteranMenu);
                    pilotMenu.add(regularMenu);
                    pilotMenu.add(greenMenu);
                    pilotMenu.add(ultraGreenMenu);
                }
            }

            // Driver Menu
            if (isTank && !isVTOL) {
                final boolean isNaval = units[0].getEntity().getMovementMode().isMarine();
                filteredPersonnel = personnel.stream()
                        .filter(person -> person.hasRole(isNaval
                                ? PersonnelRole.NAVAL_VEHICLE_DRIVER : PersonnelRole.GROUND_VEHICLE_DRIVER))
                        .collect(Collectors.toList());
                if (!filteredPersonnel.isEmpty()) {
                    // Create the SkillLevel Submenus
                    final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu", SkillLevel.LEGENDARY.toString());
                    final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                    final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                    final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                    final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                    final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                    final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu", SkillLevel.ULTRA_GREEN.toString());

                    // Add the person to the proper menu
                    for (final Person person : filteredPersonnel) {
                        final JScrollableMenu subMenu;
                        switch (person.getSkillLevel(campaign, isNaval
                                ? !person.getPrimaryRole().isNavalVehicleDriver() : !person.getPrimaryRole().isGroundVehicleDriver())) {
                            case LEGENDARY:
                                subMenu = legendaryMenu;
                                break;
                            case HEROIC:
                                subMenu = heroicMenu;
                                break;
                            case ELITE:
                                subMenu = eliteMenu;
                                break;
                            case VETERAN:
                                subMenu = veteranMenu;
                                break;
                            case REGULAR:
                                subMenu = regularMenu;
                                break;
                            case GREEN:
                                subMenu = greenMenu;
                                break;
                            case ULTRA_GREEN:
                                subMenu = ultraGreenMenu;
                                break;
                            default:
                                subMenu = null;
                                break;
                        }

                        if (subMenu != null) {
                            final JMenuItem miDriver = new JMenuItem(person.getFullTitle());
                            miDriver.setName("miDriver");
                            miDriver.addActionListener(evt -> {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                    useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                }
                                units[0].addDriver(person, useTransfers);
                            });
                            subMenu.add(miDriver);
                        }
                    }

                    driverMenu.add(eliteMenu);
                    driverMenu.add(veteranMenu);
                    driverMenu.add(regularMenu);
                    driverMenu.add(greenMenu);
                    driverMenu.add(ultraGreenMenu);
                }
            }
        }

        // Gunners Menu
        if (canTakeMoreGunners && (isTank || isSmallCraftOrJumpShip || isSuperHeavyMech || isTripod)) {
            filteredPersonnel = personnel.stream()
                    .filter(person -> (isSmallCraftOrJumpShip && person.hasRole(PersonnelRole.VESSEL_GUNNER)) ||
                            (isTank && person.hasRole(PersonnelRole.VEHICLE_GUNNER)) ||
                            ((isSuperHeavyMech || isTripod) && person.getPrimaryRole().isMechWarriorGrouping()
                                || person.getSecondaryRole().isMechWarriorGrouping()))
                    .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu", SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu", SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu;

                    SkillLevel skillLevel = SkillLevel.NONE;
                    // determine skill level based on unit and person's role
                    if (isSmallCraftOrJumpShip) {
                        skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isVesselGunner());
                    } else if (isTank) {
                        skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isVehicleGunner());
                    } else if (isSuperHeavyMech || isTripod) {
                        skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isMechWarriorGrouping());
                    }

                    switch (skillLevel) {
                        case LEGENDARY:
                            subMenu = legendaryMenu;
                            break;
                        case HEROIC:
                            subMenu = heroicMenu;
                            break;
                        case ELITE:
                            subMenu = eliteMenu;
                            break;
                        case VETERAN:
                            subMenu = veteranMenu;
                            break;
                        case REGULAR:
                            subMenu = regularMenu;
                            break;
                        case GREEN:
                            subMenu = greenMenu;
                            break;
                        case ULTRA_GREEN:
                            subMenu = ultraGreenMenu;
                            break;
                        default:
                            subMenu = null;
                            break;
                    }

                    if (subMenu != null) {
                        final JMenuItem miGunner = new JMenuItem(person.getFullTitle());
                        miGunner.setName("miGunner");
                        miGunner.addActionListener(evt -> {
                            final Unit oldUnit = person.getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                useTransfers = campaign.getCampaignOptions().isUseTransfers();
                            }
                            units[0].addGunner(person, useTransfers);
                        });
                        subMenu.add(miGunner);
                    }
                }

                gunnerMenu.add(eliteMenu);
                gunnerMenu.add(veteranMenu);
                gunnerMenu.add(regularMenu);
                gunnerMenu.add(greenMenu);
                gunnerMenu.add(ultraGreenMenu);
            }
        }

        // Crewmember Menu
        if (units[0].canTakeMoreVesselCrew() && (isAero || units[0].getEntity().isSupportVehicle())) {
            filteredPersonnel = personnel.stream()
                    .filter(person -> person.hasRole(isAero ? PersonnelRole.VESSEL_CREW : PersonnelRole.VEHICLE_CREW))
                    .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu", SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu", SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu;
                    switch (person.getSkillLevel(campaign, isAero
                            ? !person.getPrimaryRole().isVesselCrew() : !person.getPrimaryRole().isVehicleCrew())) {
                        case LEGENDARY:
                            subMenu = legendaryMenu;
                            break;
                        case HEROIC:
                            subMenu = heroicMenu;
                            break;
                        case ELITE:
                            subMenu = eliteMenu;
                            break;
                        case VETERAN:
                            subMenu = veteranMenu;
                            break;
                        case REGULAR:
                            subMenu = regularMenu;
                            break;
                        case GREEN:
                            subMenu = greenMenu;
                            break;
                        case ULTRA_GREEN:
                            subMenu = ultraGreenMenu;
                            break;
                        default:
                            subMenu = null;
                            break;
                    }

                    if (subMenu != null) {
                        final JMenuItem miCrewmember = new JMenuItem(person.getFullTitle());
                        miCrewmember.setName("miCrewmember");
                        miCrewmember.addActionListener(evt -> {
                            final Unit oldUnit = person.getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                useTransfers = campaign.getCampaignOptions().isUseTransfers();
                            }
                            units[0].addVesselCrew(person, useTransfers);
                        });
                        subMenu.add(miCrewmember);
                    }
                }

                crewmemberMenu.add(eliteMenu);
                crewmemberMenu.add(veteranMenu);
                crewmemberMenu.add(regularMenu);
                crewmemberMenu.add(greenMenu);
                crewmemberMenu.add(ultraGreenMenu);
            }
        }

        // Tech Officer and Console Commander Menu, currently combined as required by the current setup
        // TODO : Our implementation for Console Commanders in MekHQ makes this a necessity, but
        // TODO : I find that really terrible. We should be able to separate out tech officers
        // TODO : and Console Commanders properly. Because of this, I'm leaving the base code
        // TODO : here as the older style for now.
        if (canTakeTechOfficer) {
            for (final Person person : personnel) {
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
                                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                useTransfers = campaign.getCampaignOptions().isUseTransfers();
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
                            oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                            useTransfers = campaign.getCampaignOptions().isUseTransfers();
                        }
                        units[0].setTechOfficer(person, useTransfers);
                    });
                    techOfficerMenu.add(miTechOfficer);
                }
            }
        }

        // Soldier Menu
        if (units[0].usesSoldiers() && canTakeMoreGunners) {
            filteredPersonnel = personnel.stream()
                    .filter(person -> person.hasRole(isConventionalInfantry
                            ? PersonnelRole.SOLDIER : PersonnelRole.BATTLE_ARMOUR))
                    .collect(Collectors.toList());

            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu", SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu", SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu;
                    switch (person.getSkillLevel(campaign, isConventionalInfantry
                            ? !person.getPrimaryRole().isSoldier() : !person.getPrimaryRole().isBattleArmour())) {
                        case LEGENDARY:
                            subMenu = legendaryMenu;
                            break;
                        case HEROIC:
                            subMenu = heroicMenu;
                            break;
                        case ELITE:
                            subMenu = eliteMenu;
                            break;
                        case VETERAN:
                            subMenu = veteranMenu;
                            break;
                        case REGULAR:
                            subMenu = regularMenu;
                            break;
                        case GREEN:
                            subMenu = greenMenu;
                            break;
                        case ULTRA_GREEN:
                            subMenu = ultraGreenMenu;
                            break;
                        default:
                            subMenu = null;
                            break;
                    }

                    if (subMenu != null) {
                        final JMenuItem miSoldier = new JMenuItem(person.getFullTitle());
                        miSoldier.setName("miSoldier");
                        miSoldier.addActionListener(evt -> {
                            final Unit oldUnit = person.getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                useTransfers = campaign.getCampaignOptions().isUseTransfers();
                            }
                            units[0].addPilotOrSoldier(person, useTransfers);
                        });
                        subMenu.add(miSoldier);
                    }
                }

                soldierMenu.add(eliteMenu);
                soldierMenu.add(veteranMenu);
                soldierMenu.add(regularMenu);
                soldierMenu.add(greenMenu);
                soldierMenu.add(ultraGreenMenu);
            }
        }

        // Navigator Menu
        if (units[0].canTakeNavigator()) {
            // Navigator personnel filter
            filteredPersonnel = personnel.stream()
                    .filter(person -> person.hasRole(PersonnelRole.VESSEL_NAVIGATOR))
                    .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu", SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu", SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu;
                    switch (person.getSkillLevel(campaign, !person.getPrimaryRole().isVesselNavigator())) {
                        case LEGENDARY:
                            subMenu = legendaryMenu;
                            break;
                        case HEROIC:
                            subMenu = heroicMenu;
                            break;
                        case ELITE:
                            subMenu = eliteMenu;
                            break;
                        case VETERAN:
                            subMenu = veteranMenu;
                            break;
                        case REGULAR:
                            subMenu = regularMenu;
                            break;
                        case GREEN:
                            subMenu = greenMenu;
                            break;
                        case ULTRA_GREEN:
                            subMenu = ultraGreenMenu;
                            break;
                        default:
                            subMenu = null;
                            break;
                    }

                    if (subMenu != null) {
                        final JMenuItem miNavigator = new JMenuItem(person.getFullTitle());
                        miNavigator.setName("miNavigator");
                        miNavigator.addActionListener(evt -> {
                            final Unit oldUnit = person.getUnit();
                            boolean useTransfers = false;
                            if (oldUnit != null) {
                                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                useTransfers = campaign.getCampaignOptions().isUseTransfers();
                            }
                            units[0].setNavigator(person, useTransfers);
                        });
                        subMenu.add(miNavigator);
                    }
                }

                navigatorMenu.add(eliteMenu);
                navigatorMenu.add(veteranMenu);
                navigatorMenu.add(regularMenu);
                navigatorMenu.add(greenMenu);
                navigatorMenu.add(ultraGreenMenu);
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
    //endregion Initialization
}
