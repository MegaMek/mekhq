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
import megamek.common.EntityWeightClass;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.HangarSorter;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.utilities.StaticChecks;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a standard menu that takes a person and lets the user assign a unit for them to tech
 */
public class AssignTechToUnitMenu extends JScrollableMenu {
    //region Constructors
    public AssignTechToUnitMenu(final Campaign campaign, final Person person) {
        super("AssignTechToUnitMenu");
        initialize(campaign, person);
    }
    //endregion Constructors

    //region Initialization
    private void initialize(final Campaign campaign, final Person person) {
        // Initialize Menu
        setText(resources.getString("AssignTechToUnitMenu.title"));

        // Default Return for Illegal Assignments
        // 1) Person must be active
        // 2) Person must be free
        // 3) Person cannot be deployed
        // 4) Person must be a tech
        // 5) Person must have free maintenance time
        if (!person.getStatus().isActive() || !person.getPrisonerStatus().isFree()
                || person.isDeployed() || !person.isTech()
                || (person.getMaintenanceTimeUsing() >= Person.PRIMARY_ROLE_SUPPORT_TIME)) {
            return;
        }

        // Person Assignment Menus
        // Parsing variables
        JMenu unitTypeMenu = new JScrollableMenu("unitTypeMenu"); // ensures no empty additions
        JMenu entityWeightClassMenu = new JMenu();
        int unitType = -1;
        int weightClass = -1;

        // Get all units that are:
        // 1) Available
        // 2) Potentially maintained by the person
        // 3) One of:
        //      a) Currently maintained by the person
        //      b) The unit can take a tech and the person can afford the time to maintain the unit
        final List<Unit> units = HangarSorter.defaultSorting()
                .sort(campaign.getHangar().getUnitsStream().filter(Unit::isAvailable)
                        .filter(unit -> person.canTech(unit.getEntity()))
                        .filter(unit -> person.equals(unit.getTech()) || (unit.canTakeTech()
                                && (person.getMaintenanceTimeUsing() + unit.getMaintenanceTime() <= Person.PRIMARY_ROLE_SUPPORT_TIME))))
                .collect(Collectors.toList());
        for (final Unit unit : units) {
            if (unit.getEntity().getUnitType() != unitType) {
                // Add the current menus, first the Entity Weight Class menu to the Unit Type menu,
                // then the Unit Type menu to this menu
                unitTypeMenu.add(entityWeightClassMenu);
                add(unitTypeMenu);

                // Update parsing variables
                unitType = unit.getEntity().getUnitType();
                weightClass = unit.getEntity().getWeightClass();

                // And create the new menus
                unitTypeMenu = new JScrollableMenu("unitTypeMenu", UnitType.getTypeDisplayableName(unitType));
                entityWeightClassMenu = new JScrollableMenu("entityWeightClassMenu", EntityWeightClass.getClassName(weightClass, unit.getEntity()));
            } else if (unit.getEntity().getWeightClass() != weightClass) {
                // Add the current Entity Weight Class menu to the Unit Type menu
                unitTypeMenu.add(entityWeightClassMenu);

                // Update parsing variable
                weightClass = unit.getEntity().getWeightClass();

                // And create the new Entity Weight Class menu
                entityWeightClassMenu = new JScrollableMenu("entityWeightClassMenu", EntityWeightClass.getClassName(weightClass, unit.getEntity()));
            }

            final JMenuItem cbUnit = new JCheckBoxMenuItem(unit.getName());
            cbUnit.setName("cbUnit");
            cbUnit.setSelected(person.equals(unit.getTech()));
            cbUnit.addActionListener(evt -> {
                if (person.equals(unit.getTech())) {
                    unit.remove(person, true);
                } else {
                    unit.setTech(person);
                }
            });
            entityWeightClassMenu.add(cbUnit);
        }

        // And finally add the ability to simply unassign from all tech assignments
        final JMenuItem miUnassignPerson = new JMenuItem(resources.getString("None.text"));
        miUnassignPerson.setName("miUnassignTech");
        miUnassignPerson.addActionListener(evt -> {
            for (final Unit unit : new ArrayList<>(person.getTechUnits())) {
                unit.remove(person, true);
                unit.resetEngineer();
            }
        });
        add(miUnassignPerson);
    }
    //endregion Initialization


    private void old() {
        if (!person.isDeployed()) {
            if (unit.canTakeMoreVesselCrew()
                    && ((unit.getEntity().isAero() && person.hasSkill(SkillType.S_TECH_VESSEL))
                    || ((unit.getEntity().isSupportVehicle() && person.hasSkill(SkillType.S_TECH_MECHANIC))))) {
                cbMenuItem = new JCheckBoxMenuItem(unit.getName());
                cbMenuItem.setSelected(unit.equals(person.getUnit()));
                cbMenuItem.setActionCommand(makeCommand(CMD_ADD_CREW, unit.getId().toString()));
                cbMenuItem.addActionListener(this);
                crewEntityWeightMenu.add(cbMenuItem);
            }
        } else if (StaticChecks.areAllActive(selected)&&StaticChecks.areAllEligible(selected)) {
            for (Unit unit : HangarSorter.defaultSorting().getUnits(gui.getCampaign().getHangar())) {
                if (StaticChecks.areAllVesselCrew(selected)) {
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
                }
            }
        }
    }

    private void oldNote() {
        switch (old) {
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
        }
    }
}
