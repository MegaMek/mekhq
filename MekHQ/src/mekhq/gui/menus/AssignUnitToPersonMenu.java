/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.menus;

import static mekhq.utilities.EntityUtilities.isUnsupportedEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JMenuItem;

import megamek.common.enums.SkillLevel;
import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.sorter.PersonTitleSorter;

/**
 * This is a standard menu that takes either a unit or multiple units that require the same tech type, and allows the
 * user to assign or remove a tech from them.
 */
public class AssignUnitToPersonMenu extends JScrollableMenu {
    private static final MMLogger LOGGER = MMLogger.create(AssignPersonToUnitMenu.class);

    // region Constructors
    public AssignUnitToPersonMenu(final Campaign campaign, final Unit... units) {
        super("AssignUnitToPersonMenu");
        initialize(campaign, units);
    }
    // endregion Constructors

    // region Initialization
    private void initialize(final Campaign campaign, final Unit... units) {
        // Immediate Return for Illegal Assignments:
        // 1) No units to assign
        // 2) Any units are currently unavailable
        if ((units.length == 0) || Stream.of(units).anyMatch(unit -> !unit.isAvailable())) {
            return;
        }

        for (Unit unit : units) {
            Entity entity = unit.getEntity();

            if (entity == null || isUnsupportedEntity(entity)) {
                return;
            }
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

        // Finally, add the ability to simply unassign... provided at least one of the
        // units has
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
        final JScrollableMenu genericCrewMenu = new JScrollableMenu("genericCrewMenu",
              resources.getString("genericCrewMenu.text"));
        final JScrollableMenu communicationsCrewMenu = new JScrollableMenu("communicationsCrewMenu",
              resources.getString("communicationsCrewMenu.text"));
        final JScrollableMenu doctorCrewMenu = new JScrollableMenu("doctorCrewMenu",
              resources.getString("doctorCrewMenu.text"));
        final JScrollableMenu medicCrewMenu = new JScrollableMenu("medicCrewMenu",
              resources.getString("medicCrewMenu.text"));
        final JScrollableMenu combatTechCrewMenu = new JScrollableMenu("combatTechCrewMenu",
              resources.getString("combatTechCrewMenu.text"));
        final JScrollableMenu astechCrewMenu = new JScrollableMenu("astechCrewMenu",
              resources.getString("astechCrewMenu.text"));
        final JScrollableMenu techOfficerMenu = new JScrollableMenu("techOfficerMenu",
              resources.getString("techOfficerMenu.text"));
        final JScrollableMenu consoleCommanderMenu = new JScrollableMenu("consoleCommanderMenu",
              resources.getString("consoleCommanderMenu.text"));
        final JScrollableMenu soldierMenu = new JScrollableMenu("soldierMenu", resources.getString("soldierMenu.text"));
        final JScrollableMenu navigatorMenu = new JScrollableMenu("navigatorMenu",
              resources.getString("navigatorMenu.text"));

        // Parsing Booleans
        final Entity entity = units[0].getEntity();
        final boolean canTakeMoreDrivers = units[0].canTakeMoreDrivers();
        final boolean usesSoloPilot = units[0].usesSoloPilot();
        final boolean isNaval = entity instanceof Tank && entity.getMovementMode().isMarine();
        final boolean isVTOL = entity instanceof VTOL;
        final boolean isMek = entity instanceof Mek;
        final boolean isMekWithGunner = (!usesSoloPilot && isMek);
        final boolean isProtoMek = entity instanceof ProtoMek;
        final boolean isConventionalAircraftCrew = entity instanceof ConvFighter;
        final boolean isSmallCraftOrJumpShip = (entity instanceof SmallCraft) || (entity instanceof Jumpship);
        final boolean isTank = entity instanceof Tank;
        final boolean canTakeMoreGunners = units[0].canTakeMoreGunners();
        final boolean isAero = entity instanceof Aero;
        final boolean canTakeTechOfficer = units[0].canTakeTechOfficer();
        final boolean usesSoldiers = units[0].usesSoldiers();
        final boolean isConventionalInfantry = units[0].isConventionalInfantry();
        final boolean isUseAltAdvancedMedical = campaign.getCampaignOptions().isUseAlternativeAdvancedMedical();
        final boolean isUseImplants = campaign.getCampaignOptions().isUseImplants();

        // Skip People (by filtering them out) if they are:
        // 1) Inactive
        // 2) A Prisoner
        // 3) Already assigned to a unit
        // 4) Civilian Primary Role
        // 5) Astech Primary role with the Medical, Administrator, or None Secondary
        // Roles
        // 6) Medical Primary role with the Astech, Administrator, or None Secondary
        // Roles
        // 7) Administrator Primary Role with Astech, Medical, Administrator, or None
        // Secondary Roles
        // Then sorts the remainder based on their full title
        List<Person> personnel = campaign.getPersonnel()
                                       .stream()
                                       .filter(person -> person.getStatus().isActive())
                                       .filter(person -> !person.getPrisonerStatus().isCurrentPrisoner())
                                       .filter(Person::isEmployed)
                                       .filter(person -> person.getUnit() == null)
                                       .filter(person -> !Profession.getProfessionFromPersonnelRole(person.getPrimaryRole())
                                                                .isCivilian())
                                       .filter(person -> !person.getPrimaryRole().isAstech() ||
                                                               !(person.getSecondaryRole().isMedicalStaff() ||
                                                                       person.getSecondaryRole().isAdministrator() ||
                                                                       person.getSecondaryRole().isNone()))
                                       .filter(person -> !person.getPrimaryRole().isMedicalStaff() ||
                                                               !(person.getSecondaryRole().isAstech() ||
                                                                       person.getSecondaryRole().isAdministrator() ||
                                                                       person.getSecondaryRole().isNone()))
                                       .filter(person -> !person.getPrimaryRole().isAdministrator() ||
                                                               !(person.getSecondaryRole().isAstech() ||
                                                                       person.getSecondaryRole().isMedicalStaff() ||
                                                                       person.getSecondaryRole().isAdministrator() ||
                                                                       person.getSecondaryRole().isNone()))
                                       .sorted(new PersonTitleSorter().reversed())
                                       .collect(Collectors.toList());

        if (personnel.isEmpty()) {
            return;
        }

        // The order of this if statement is required to properly filter based on the
        // unit type
        if (isMek) {
            personnel = personnel.stream()
                              .filter(person -> person.getPrimaryRole().isMekWarriorGrouping() ||
                                                      person.getSecondaryRole().isMekWarriorGrouping())
                              .collect(Collectors.toList());
        } else if (isVTOL) {
            personnel = personnel.stream()
                              .filter(person -> person.getPrimaryRole().isVTOLCrew() ||
                                                      person.getSecondaryRole().isVTOLCrew() ||
                                                      person.getPrimaryRole().isVehicleCrewExtended() ||
                                                      person.getSecondaryRole().isVehicleCrewExtended())
                              .collect(Collectors.toList());
        } else if (isTank) {
            personnel = personnel.stream()
                              .filter(person -> person.getPrimaryRole().isVehicleCrewMember() ||
                                                      person.getSecondaryRole().isVehicleCrewMember() ||
                                                      person.getPrimaryRole().isVehicleCrewExtended() ||
                                                      person.getSecondaryRole().isVehicleCrewExtended())
                              .collect(Collectors.toList());
        } else if (isSmallCraftOrJumpShip) {
            personnel = personnel.stream()
                              .filter(person -> person.getPrimaryRole().isVesselCrewMember() ||
                                                      person.getSecondaryRole().isVesselCrewMember())
                              .collect(Collectors.toList());
        } else if (isConventionalAircraftCrew) {
            personnel = personnel.stream()
                              .filter(person -> person.getPrimaryRole().isConventionalAircraftPilot() ||
                                                      person.getSecondaryRole().isConventionalAircraftPilot() ||
                                                      person.getPrimaryRole().isVehicleCrewExtended() ||
                                                      person.getSecondaryRole().isVehicleCrewExtended())
                              .collect(Collectors.toList());
        } else if (isAero) {
            personnel = personnel.stream()
                              .filter(person -> person.getPrimaryRole().isAerospaceGrouping() ||
                                                      person.getSecondaryRole().isAerospaceGrouping())
                              .collect(Collectors.toList());
        } else if (isProtoMek) {
            personnel = personnel.stream()
                              .filter(person -> person.hasRole(PersonnelRole.PROTOMEK_PILOT))
                              .collect(Collectors.toList());
        } else if (usesSoldiers) {
            personnel = personnel.stream()
                              .filter(person -> person.hasRole(isConventionalInfantry ?
                                                                     PersonnelRole.SOLDIER :
                                                                     PersonnelRole.BATTLE_ARMOUR))
                              .collect(Collectors.toList());
        } else {
            LOGGER.error("Unhandled entity type of {}", units[0].getEntity().getClass());
            return;
        }

        if (personnel.isEmpty()) {
            return;
        }

        List<Person> filteredPersonnel;

        // Pilot Menu
        if (canTakeMoreDrivers) {
            // Pilot Menu
            if (isMek || usesSoloPilot || isVTOL || isSmallCraftOrJumpShip || isConventionalAircraftCrew) {
                if (isMek) {
                    filteredPersonnel = personnel.stream()
                                              .filter(person -> person.getPrimaryRole().isMekWarriorGrouping() ||
                                                                      person.getSecondaryRole().isMekWarriorGrouping())
                                              .collect(Collectors.toList());
                } else if (isProtoMek) {
                    filteredPersonnel = personnel.stream()
                                              .filter(person -> person.hasRole(PersonnelRole.PROTOMEK_PILOT))
                                              .collect(Collectors.toList());
                } else if (isSmallCraftOrJumpShip) {
                    filteredPersonnel = personnel.stream()
                                              .filter(person -> person.hasRole(PersonnelRole.VESSEL_PILOT))
                                              .collect(Collectors.toList());
                } else if (isConventionalAircraftCrew) {
                    filteredPersonnel = personnel.stream()
                                              .filter(person -> person.getPrimaryRole().isConventionalAircraftPilot() ||
                                                                      person.getSecondaryRole()
                                                                            .isConventionalAircraftPilot())
                                              .collect(Collectors.toList());
                } else if (isAero) {
                    filteredPersonnel = personnel.stream()
                                              .filter(person -> person.getPrimaryRole().isAerospaceGrouping() ||
                                                                      person.getSecondaryRole().isAerospaceGrouping())
                                              .collect(Collectors.toList());
                } else if (isVTOL) {
                    filteredPersonnel = personnel.stream()
                                              .filter(person -> person.hasRole(PersonnelRole.VEHICLE_CREW_VTOL))
                                              .collect(Collectors.toList());
                } else {
                    LOGGER.warn("Attempting to assign pilot to unknown unit type of {}",
                          units[0].getEntity().getClass());
                    filteredPersonnel = new ArrayList<>();
                }

                if (!filteredPersonnel.isEmpty()) {
                    // Create the SkillLevel Submenus
                    final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                          SkillLevel.LEGENDARY.toString());
                    final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                    final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                    final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu",
                          SkillLevel.VETERAN.toString());
                    final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu",
                          SkillLevel.REGULAR.toString());
                    final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                    final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                          SkillLevel.ULTRA_GREEN.toString());

                    // Add the person to the proper menu
                    for (final Person person : filteredPersonnel) {
                        final JScrollableMenu subMenu;
                        final SkillLevel skillLevel;
                        if (isMek) {
                            skillLevel = person.getSkillLevel(campaign,
                                  !person.getPrimaryRole().isMekWarriorGrouping(), true);
                        } else if (isProtoMek) {
                            skillLevel = person.getSkillLevel(campaign,
                                  !person.getPrimaryRole().isProtoMekPilot(),
                                  true);
                        } else if (isSmallCraftOrJumpShip) {
                            skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isVesselPilot(), true);
                        } else if (isConventionalAircraftCrew) {
                            skillLevel = person.getSkillLevel(campaign,
                                  !person.getPrimaryRole().isConventionalAircraftPilot(), true);
                        } else if (isAero) {
                            skillLevel = person.getSkillLevel(campaign,
                                  !person.getPrimaryRole().isAerospaceGrouping(),
                                  true);
                        } else { // it's a VTOL
                            skillLevel = person.getSkillLevel(campaign,
                                  !person.getPrimaryRole().isVehicleCrewVTOL(),
                                  true);
                        }

                        subMenu = switch (skillLevel) {
                            case LEGENDARY -> legendaryMenu;
                            case HEROIC -> heroicMenu;
                            case ELITE -> eliteMenu;
                            case VETERAN -> veteranMenu;
                            case REGULAR -> regularMenu;
                            case GREEN -> greenMenu;
                            case ULTRA_GREEN -> ultraGreenMenu;
                            default -> null;
                        };

                        if (subMenu != null) {
                            final JMenuItem miPilot = new JMenuItem(person.getFullTitleAndProfessions());
                            miPilot.setName("miPilot");
                            miPilot.addActionListener(evt -> {
                                final Unit oldUnit = person.getUnit();
                                boolean useTransfers = false;
                                if (oldUnit != null) {
                                    oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                                    useTransfers = campaign.getCampaignOptions().isUseTransfers();
                                }

                                ensureRecruitmentDate(campaign.getLocalDate(), person);

                                if (isVTOL || isSmallCraftOrJumpShip || isConventionalAircraftCrew) {
                                    units[0].addDriver(person, useTransfers);
                                } else {
                                    units[0].addPilotOrSoldier(person, useTransfers);
                                }
                            });
                            subMenu.add(miPilot);
                        }
                    }

                    pilotMenu.add(legendaryMenu);
                    pilotMenu.add(heroicMenu);
                    pilotMenu.add(eliteMenu);
                    pilotMenu.add(veteranMenu);
                    pilotMenu.add(regularMenu);
                    pilotMenu.add(greenMenu);
                    pilotMenu.add(ultraGreenMenu);
                }
            }

            // Driver Menu
            if (isTank && !isVTOL) {
                filteredPersonnel = personnel.stream()
                                          .filter(person -> person.hasRole(isNaval ?
                                                                                 PersonnelRole.VEHICLE_CREW_NAVAL :
                                                                                 PersonnelRole.VEHICLE_CREW_GROUND))
                                          .collect(Collectors.toList());
                if (!filteredPersonnel.isEmpty()) {
                    // Create the SkillLevel Submenus
                    final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                          SkillLevel.LEGENDARY.toString());
                    final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                    final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                    final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu",
                          SkillLevel.VETERAN.toString());
                    final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu",
                          SkillLevel.REGULAR.toString());
                    final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                    final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                          SkillLevel.ULTRA_GREEN.toString());

                    // Add the person to the proper menu
                    for (final Person person : filteredPersonnel) {
                        final JScrollableMenu subMenu = switch (person.getSkillLevel(campaign,
                              isNaval ?
                                    !person.getPrimaryRole().isVehicleCrewNaval() :
                                    !person.getPrimaryRole().isVehicleCrewGround())) {
                            case LEGENDARY -> legendaryMenu;
                            case HEROIC -> heroicMenu;
                            case ELITE -> eliteMenu;
                            case VETERAN -> veteranMenu;
                            case REGULAR -> regularMenu;
                            case GREEN -> greenMenu;
                            case ULTRA_GREEN -> ultraGreenMenu;
                            default -> null;
                        };

                        if (subMenu != null) {
                            final JMenuItem miDriver = getMiDriver(campaign, units, person);
                            subMenu.add(miDriver);
                        }
                    }

                    driverMenu.add(legendaryMenu);
                    driverMenu.add(heroicMenu);
                    driverMenu.add(eliteMenu);
                    driverMenu.add(veteranMenu);
                    driverMenu.add(regularMenu);
                    driverMenu.add(greenMenu);
                    driverMenu.add(ultraGreenMenu);
                }
            }
        }

        // Gunners Menu
        if (canTakeMoreGunners && (isTank || isSmallCraftOrJumpShip || isMekWithGunner || isConventionalAircraftCrew)) {
            filteredPersonnel = new ArrayList<>();

            for (Person person : personnel) {
                boolean shouldInclude = false;

                if (isSmallCraftOrJumpShip && person.hasRole(PersonnelRole.VESSEL_GUNNER)) {
                    shouldInclude = true;
                } else if (isTank) {
                    if (isVTOL) {
                        shouldInclude = person.hasRole(PersonnelRole.VEHICLE_CREW_VTOL);
                    } else if (isNaval) {
                        shouldInclude = person.hasRole(PersonnelRole.VEHICLE_CREW_NAVAL);
                    } else {
                        shouldInclude = person.hasRole(PersonnelRole.VEHICLE_CREW_GROUND);
                    }
                } else if (isConventionalAircraftCrew && person.hasRole(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT)) {
                    shouldInclude = true;
                } else if (isMekWithGunner &&
                                 (person.getPrimaryRole().isMekWarriorGrouping() ||
                                        person.getSecondaryRole().isMekWarriorGrouping())) {
                    shouldInclude = true;
                }

                if (shouldInclude) {
                    filteredPersonnel.add(person);
                }
            }
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu;

                    SkillLevel skillLevel = SkillLevel.NONE;
                    // determine skill level based on unit and person's role
                    if (isSmallCraftOrJumpShip) {
                        skillLevel = person.getSkillLevel(campaign, !person.getPrimaryRole().isVesselGunner(), true);
                    } else if (isTank) {
                        if (isVTOL) {
                            skillLevel = person.getSkillLevel(campaign,
                                  !person.getPrimaryRole().isVehicleCrewVTOL(), true);
                        } else if (isNaval) {
                            skillLevel = person.getSkillLevel(campaign,
                                  !person.getPrimaryRole().isVehicleCrewNaval(), true);
                        } else {
                            skillLevel = person.getSkillLevel(campaign,
                                  !person.getPrimaryRole().isVehicleCrewGround(),
                                  true);
                        }
                    } else if (isConventionalAircraftCrew) {
                        skillLevel = person.getSkillLevel(campaign,
                              !person.getPrimaryRole().isConventionalAircraftPilot(), true);
                    } else if (isMekWithGunner) {
                        skillLevel = person.getSkillLevel(campaign,
                              !person.getPrimaryRole().isMekWarriorGrouping(),
                              true);
                    }

                    subMenu = switch (skillLevel) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miGunner = getMiGunner(campaign, units, person);
                        subMenu.add(miGunner);
                    }
                }

                gunnerMenu.add(legendaryMenu);
                gunnerMenu.add(heroicMenu);
                gunnerMenu.add(eliteMenu);
                gunnerMenu.add(veteranMenu);
                gunnerMenu.add(regularMenu);
                gunnerMenu.add(greenMenu);
                gunnerMenu.add(ultraGreenMenu);
            }
        }

        // Generic Crew Menu
        if (units[0].canTakeMoreGenericCrew()) {
            filteredPersonnel = personnel.stream()
                                      .filter(person -> {
                                          if (isAero && !isConventionalAircraftCrew) {
                                              return person.hasRole(PersonnelRole.VESSEL_CREW);
                                          } else {
                                              return person.getPrimaryRole().isVehicleCrewExtended() ||
                                                           person.getSecondaryRole().isVehicleCrewExtended();
                                          }
                                      })
                                      .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu = switch (person.getSkillLevel(
                          campaign,
                          isAero && !isConventionalAircraftCrew
                                ?
                                !person.hasRole(PersonnelRole.VESSEL_CREW)
                                :
                                !(person.getPrimaryRole().isVehicleCrewExtended() ||
                                        person.getSecondaryRole().isVehicleCrewExtended()), true)) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miCrewmember = getMiGenericCrew(campaign, units, person);
                        subMenu.add(miCrewmember);
                    }
                }

                genericCrewMenu.add(legendaryMenu);
                genericCrewMenu.add(heroicMenu);
                genericCrewMenu.add(eliteMenu);
                genericCrewMenu.add(veteranMenu);
                genericCrewMenu.add(regularMenu);
                genericCrewMenu.add(greenMenu);
                genericCrewMenu.add(ultraGreenMenu);
            }
        }

        // Communications Crew Menu
        if (units[0].canTakeMoreCommunicationsCrew()) {
            filteredPersonnel = personnel.stream()
                                      .filter(person -> {
                                          if (isAero && !isConventionalAircraftCrew) {
                                              return person.hasRole(PersonnelRole.VESSEL_CREW);
                                          } else {
                                              return person.hasRole(PersonnelRole.COMMS_OPERATOR);
                                          }
                                      })
                                      .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu = switch (person.getSkillLevel(
                          campaign,
                          isAero && !isConventionalAircraftCrew
                                ?
                                !person.hasRole(PersonnelRole.VESSEL_CREW)
                                :
                                !(person.getPrimaryRole().isCommsOperator() ||
                                        person.getSecondaryRole().isCommsOperator()), true)) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miCommunicationsCrew = getMiCommunicationsCrew(campaign, units, person);
                        subMenu.add(miCommunicationsCrew);
                    }
                }

                communicationsCrewMenu.add(legendaryMenu);
                communicationsCrewMenu.add(heroicMenu);
                communicationsCrewMenu.add(eliteMenu);
                communicationsCrewMenu.add(veteranMenu);
                communicationsCrewMenu.add(regularMenu);
                communicationsCrewMenu.add(greenMenu);
                communicationsCrewMenu.add(ultraGreenMenu);
            }
        }

        // Doctor Crew Menu
        if (units[0].canTakeMoreDoctorCrew()) {
            filteredPersonnel = personnel.stream()
                                      .filter(person -> {
                                          if (isAero && !isConventionalAircraftCrew) {
                                              return person.hasRole(PersonnelRole.VESSEL_CREW);
                                          } else {
                                              return person.hasRole(PersonnelRole.DOCTOR);
                                          }
                                      })
                                      .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu = switch (person.getSkillLevel(
                          campaign,
                          isAero && !isConventionalAircraftCrew
                                ?
                                !person.hasRole(PersonnelRole.VESSEL_CREW)
                                :
                                !(person.getPrimaryRole().isDoctor() ||
                                        person.getSecondaryRole().isDoctor()), true)) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miDoctorCrew = getMiDoctorCrew(campaign, units, person);
                        subMenu.add(miDoctorCrew);
                    }
                }

                doctorCrewMenu.add(legendaryMenu);
                doctorCrewMenu.add(heroicMenu);
                doctorCrewMenu.add(eliteMenu);
                doctorCrewMenu.add(veteranMenu);
                doctorCrewMenu.add(regularMenu);
                doctorCrewMenu.add(greenMenu);
                doctorCrewMenu.add(ultraGreenMenu);
            }
        }

        // Medic Crew Menu
        if (units[0].canTakeMoreMedicCrew()) {
            filteredPersonnel = personnel.stream()
                                      .filter(person -> {
                                          if (isAero && !isConventionalAircraftCrew) {
                                              return person.hasRole(PersonnelRole.VESSEL_CREW);
                                          } else {
                                              return person.hasRole(PersonnelRole.MEDIC);
                                          }
                                      })
                                      .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu = switch (person.getSkillLevel(
                          campaign,
                          isAero && !isConventionalAircraftCrew
                                ?
                                !person.hasRole(PersonnelRole.VESSEL_CREW)
                                :
                                !(person.getPrimaryRole().isMedic() ||
                                        person.getSecondaryRole().isMedic()), true)) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miMedicCrew = getMiMedicCrew(campaign, units, person);
                        subMenu.add(miMedicCrew);
                    }
                }

                medicCrewMenu.add(legendaryMenu);
                medicCrewMenu.add(heroicMenu);
                medicCrewMenu.add(eliteMenu);
                medicCrewMenu.add(veteranMenu);
                medicCrewMenu.add(regularMenu);
                medicCrewMenu.add(greenMenu);
                medicCrewMenu.add(ultraGreenMenu);
            }
        }

        // Combat Tech Crew Menu
        if (units[0].canTakeMoreCombatTechCrew()) {
            filteredPersonnel = personnel.stream()
                                      .filter(person -> {
                                          if (isAero && !isConventionalAircraftCrew) {
                                              return person.hasRole(PersonnelRole.VESSEL_CREW);
                                          } else {
                                              return person.hasRole(PersonnelRole.COMBAT_TECHNICIAN);
                                          }
                                      })
                                      .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu = switch (person.getSkillLevel(
                          campaign,
                          isAero && !isConventionalAircraftCrew
                                ?
                                !person.hasRole(PersonnelRole.VESSEL_CREW)
                                :
                                !(person.getPrimaryRole().isCombatTechnician() ||
                                        person.getSecondaryRole().isCombatTechnician()), true)) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miCombatTechCrew = getMiCombatTechCrew(campaign, units, person);
                        subMenu.add(miCombatTechCrew);
                    }
                }

                combatTechCrewMenu.add(legendaryMenu);
                combatTechCrewMenu.add(heroicMenu);
                combatTechCrewMenu.add(eliteMenu);
                combatTechCrewMenu.add(veteranMenu);
                combatTechCrewMenu.add(regularMenu);
                combatTechCrewMenu.add(greenMenu);
                combatTechCrewMenu.add(ultraGreenMenu);
            }
        }

        // Astech Crew Menu
        if (units[0].canTakeMoreAstechCrew()) {
            filteredPersonnel = personnel.stream()
                                      .filter(person -> {
                                          if (isAero && !isConventionalAircraftCrew) {
                                              return person.hasRole(PersonnelRole.VESSEL_CREW);
                                          } else {
                                              return person.hasRole(PersonnelRole.ASTECH);
                                          }
                                      })
                                      .collect(Collectors.toList());
            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu = switch (person.getSkillLevel(
                          campaign,
                          isAero && !isConventionalAircraftCrew
                                ?
                                !person.hasRole(PersonnelRole.VESSEL_CREW)
                                :
                                !(person.getPrimaryRole().isAstech() ||
                                        person.getSecondaryRole().isAstech()), true)) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miAstechCrew = getMiAstechCrew(campaign, units, person);
                        subMenu.add(miAstechCrew);
                    }
                }

                astechCrewMenu.add(legendaryMenu);
                astechCrewMenu.add(heroicMenu);
                astechCrewMenu.add(eliteMenu);
                astechCrewMenu.add(veteranMenu);
                astechCrewMenu.add(regularMenu);
                astechCrewMenu.add(greenMenu);
                astechCrewMenu.add(ultraGreenMenu);
            }
        }

        // Tech Officer and Console Commander Menu, currently combined as required by
        // the current setup
        // TODO : Our implementation for Console Commanders in MekHQ makes this a
        // necessity, but
        // TODO : I find that really terrible. We should be able to separate out tech
        // officers
        // TODO : and Console Commanders properly. Because of this, I'm leaving the base
        // code
        // TODO : here as the older style for now.
        if (canTakeTechOfficer) {
            for (final Person person : personnel) {
                // For a vehicle command console we will require the commander to be a driver
                // or a gunner, but not necessarily both
                if (isTank) {
                    if (person.canDrive(units[0].getEntity(), isUseAltAdvancedMedical, isUseImplants) ||
                              person.canGun(units[0].getEntity())) {
                        final JMenuItem miConsoleCommander = getMiConsoleCommander(person,
                              "miConsoleCommander",
                              campaign,
                              units);
                        consoleCommanderMenu.add(miConsoleCommander);
                    }
                } else if (person.canDrive(units[0].getEntity(), isUseAltAdvancedMedical, isUseImplants) &&
                                 person.canGun(units[0].getEntity())) {
                    final JMenuItem miTechOfficer = getMiConsoleCommander(person, "miTechOfficer", campaign, units);
                    techOfficerMenu.add(miTechOfficer);
                }
            }
        }

        // Soldier Menu
        if (units[0].usesSoldiers() && canTakeMoreGunners) {
            filteredPersonnel = personnel.stream()
                                      .filter(person -> person.hasRole(isConventionalInfantry ?
                                                                             PersonnelRole.SOLDIER :
                                                                             PersonnelRole.BATTLE_ARMOUR))
                                      .collect(Collectors.toList());

            if (!filteredPersonnel.isEmpty()) {
                // Create the SkillLevel Submenus
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu = switch (person.getSkillLevel(campaign,
                          isConventionalInfantry ?
                                !person.getPrimaryRole().isSoldier() :
                                !person.getPrimaryRole().isBattleArmour(), true)) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miSoldier = getMiSoldier(campaign, units, person);
                        subMenu.add(miSoldier);
                    }
                }

                soldierMenu.add(legendaryMenu);
                soldierMenu.add(heroicMenu);
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
                final JScrollableMenu legendaryMenu = new JScrollableMenu("legendaryMenu",
                      SkillLevel.LEGENDARY.toString());
                final JScrollableMenu heroicMenu = new JScrollableMenu("heroicMenu", SkillLevel.HEROIC.toString());
                final JScrollableMenu eliteMenu = new JScrollableMenu("eliteMenu", SkillLevel.ELITE.toString());
                final JScrollableMenu veteranMenu = new JScrollableMenu("veteranMenu", SkillLevel.VETERAN.toString());
                final JScrollableMenu regularMenu = new JScrollableMenu("regularMenu", SkillLevel.REGULAR.toString());
                final JScrollableMenu greenMenu = new JScrollableMenu("greenMenu", SkillLevel.GREEN.toString());
                final JScrollableMenu ultraGreenMenu = new JScrollableMenu("ultraGreenMenu",
                      SkillLevel.ULTRA_GREEN.toString());

                // Add the person to the proper menu
                for (final Person person : filteredPersonnel) {
                    final JScrollableMenu subMenu = switch (person.getSkillLevel(campaign,
                          !person.getPrimaryRole().isVesselNavigator(), true)) {
                        case LEGENDARY -> legendaryMenu;
                        case HEROIC -> heroicMenu;
                        case ELITE -> eliteMenu;
                        case VETERAN -> veteranMenu;
                        case REGULAR -> regularMenu;
                        case GREEN -> greenMenu;
                        case ULTRA_GREEN -> ultraGreenMenu;
                        default -> null;
                    };

                    if (subMenu != null) {
                        final JMenuItem miNavigator = getMiNavigator(campaign, units, person);
                        subMenu.add(miNavigator);
                    }
                }

                navigatorMenu.add(legendaryMenu);
                navigatorMenu.add(heroicMenu);
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
        add(genericCrewMenu);
        add(communicationsCrewMenu);
        add(doctorCrewMenu);
        add(medicCrewMenu);
        add(combatTechCrewMenu);
        add(astechCrewMenu);
        add(techOfficerMenu);
        add(consoleCommanderMenu);
        add(soldierMenu);
        add(navigatorMenu);
    }

    private static JMenuItem getMiSoldier(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miSoldier = new JMenuItem(person.getFullTitleAndProfessions());
        miSoldier.setName("miSoldier");
        miSoldier.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addPilotOrSoldier(person, useTransfers);
        });
        return miSoldier;
    }

    private static JMenuItem getMiConsoleCommander(Person person, String consoleCommander, Campaign campaign,
          Unit[] units) {
        final JMenuItem miConsoleCommander = new JMenuItem(person.getFullTitleAndProfessions());
        miConsoleCommander.setName(consoleCommander);
        miConsoleCommander.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].setTechOfficer(person, useTransfers);
        });
        return miConsoleCommander;
    }

    private static JMenuItem getMiGenericCrew(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miGenericCrew = new JMenuItem(person.getFullTitleAndProfessions());
        miGenericCrew.setName("miGenericCrew");
        miGenericCrew.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addGenericCrew(person, useTransfers);
        });
        return miGenericCrew;
    }

    private static JMenuItem getMiCommunicationsCrew(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miCrewmember = new JMenuItem(person.getFullTitleAndProfessions());
        miCrewmember.setName("miCommunicationsCrew");
        miCrewmember.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addCommunicationsCrew(person, useTransfers);
        });
        return miCrewmember;
    }

    private static JMenuItem getMiDoctorCrew(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miCrewmember = new JMenuItem(person.getFullTitleAndProfessions());
        miCrewmember.setName("miDoctorCrew");
        miCrewmember.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addDoctorCrew(person, useTransfers);
        });
        return miCrewmember;
    }

    private static JMenuItem getMiMedicCrew(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miCrewmember = new JMenuItem(person.getFullTitleAndProfessions());
        miCrewmember.setName("miMedicCrew");
        miCrewmember.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addMedicCrew(person, useTransfers);
        });
        return miCrewmember;
    }

    private static JMenuItem getMiCombatTechCrew(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miCrewmember = new JMenuItem(person.getFullTitleAndProfessions());
        miCrewmember.setName("miCombatTechCrew");
        miCrewmember.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addCombatTechCrew(person, useTransfers);
        });
        return miCrewmember;
    }

    private static JMenuItem getMiAstechCrew(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miCrewmember = new JMenuItem(person.getFullTitleAndProfessions());
        miCrewmember.setName("miAstechCrew");
        miCrewmember.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addAstechCrew(person, useTransfers);
        });
        return miCrewmember;
    }

    private static JMenuItem getMiNavigator(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miNavigator = new JMenuItem(person.getFullTitleAndProfessions());
        miNavigator.setName("miNavigator");
        miNavigator.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].setNavigator(person, useTransfers);
        });
        return miNavigator;
    }

    private static JMenuItem getMiGunner(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miGunner = new JMenuItem(person.getFullTitleAndProfessions());
        miGunner.setName("miGunner");
        miGunner.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addGunner(person, useTransfers);
        });
        return miGunner;
    }

    private static JMenuItem getMiDriver(Campaign campaign, Unit[] units, Person person) {
        final JMenuItem miDriver = new JMenuItem(person.getFullTitleAndProfessions());
        miDriver.setName("miDriver");
        miDriver.addActionListener(evt -> {
            final Unit oldUnit = person.getUnit();
            boolean useTransfers = false;
            if (oldUnit != null) {
                oldUnit.remove(person, !campaign.getCampaignOptions().isUseTransfers());
                useTransfers = campaign.getCampaignOptions().isUseTransfers();
            }

            ensureRecruitmentDate(campaign.getLocalDate(), person);

            units[0].addDriver(person, useTransfers);
        });
        return miDriver;
    }

    /**
     * Ensures that the given person's recruitment date is set.
     *
     * <p>If the {@code Person} does not already have a recruitment date assigned, this method assigns the provided
     * date as their recruitment date.</p>
     *
     * @param today  the {@link LocalDate} to set as the recruitment date if not already set
     * @param person the {@link Person} whose recruitment date is to be checked and possibly updated
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void ensureRecruitmentDate(LocalDate today, Person person) {
        if (person.getRecruitment() == null) {
            person.setRecruitment(today);
        }
    }
    // endregion Initialization
}
