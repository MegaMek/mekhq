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

import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.HangarSorter;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;

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

        // Initialize Menu
        setText(resources.getString("AssignTechToUnitMenu.title"));

        // Person Assignment Menus
        JScrollableMenu unitTypeMenu = new JScrollableMenu("unitTypeMenu");
        JScrollableMenu entityWeightClassMenu = new JScrollableMenu("entityWeightClassMenu");

        // Parsing variables
        int unitType = -1;
        int weightClass = -1;

        // Get all units that are:
        // 1) Available
        // 2) Potentially maintained by the person
        // 3) The unit can take a tech and the person can afford the time to maintain the unit
        final List<Unit> units = HangarSorter.defaultSorting()
                .sort(campaign.getHangar().getUnitsStream().filter(Unit::isAvailable)
                        .filter(unit -> person.canTech(unit.getEntity()))
                        .filter(unit -> unit.canTakeTech()
                                && (person.getMaintenanceTimeUsing() + unit.getMaintenanceTime() <= Person.PRIMARY_ROLE_SUPPORT_TIME)))
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
                unitTypeMenu = new JScrollableMenu("unitTypeMenu",
                        UnitType.getTypeDisplayableName(unitType));
                entityWeightClassMenu = new JScrollableMenu("entityWeightClassMenu",
                        EntityWeightClass.getClassName(weightClass, unit.getEntity()));
            } else if (unit.getEntity().getWeightClass() != weightClass) {
                // Add the current Entity Weight Class menu to the Unit Type menu
                unitTypeMenu.add(entityWeightClassMenu);

                // Update parsing variable
                weightClass = unit.getEntity().getWeightClass();

                // And create the new Entity Weight Class menu
                entityWeightClassMenu = new JScrollableMenu("entityWeightClassMenu",
                        EntityWeightClass.getClassName(weightClass, unit.getEntity()));
            }

            final JMenuItem miUnit = new JMenuItem(unit.getName());
            miUnit.setName("miUnit");
            miUnit.addActionListener(evt -> unit.setTech(person));
            entityWeightClassMenu.add(miUnit);
        }

        unitTypeMenu.add(entityWeightClassMenu);
        add(unitTypeMenu);

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
}
