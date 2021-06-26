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
                pilotUnitTypeMenu = new JScrollableMenu("pilotUnitTypeMenu", unitTypeName);
                pilotEntityWeightMenu = new JScrollableMenu("pilotEntityWeightMenu", entityWeightClassName);
                consoleCommanderUnitTypeMenu = new JScrollableMenu("pilotUnitTypeMenu", unitTypeName);
                consoleCommanderEntityWeightMenu = new JScrollableMenu("pilotEntityWeightMenu", entityWeightClassName);
                soldierUnitTypeMenu = new JScrollableMenu("pilotUnitTypeMenu", unitTypeName);
                soldierEntityWeightMenu = new JScrollableMenu("pilotEntityWeightMenu", entityWeightClassName);
                navigatorUnitTypeMenu = new JScrollableMenu("pilotUnitTypeMenu", unitTypeName);
                navigatorEntityWeightMenu = new JScrollableMenu("pilotEntityWeightMenu", entityWeightClassName);
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
