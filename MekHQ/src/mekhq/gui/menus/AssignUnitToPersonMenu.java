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

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.gui.sorter.PersonTitleSorter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a standard menu that takes either a unit or multiple units that require the same tech
 * type, and allows the user to assign or remove a tech from them.
 */
public class AssignUnitToPersonMenu extends JScrollableMenu {
    //region Constructors
    public AssignUnitToPersonMenu(final Campaign campaign, final Unit unit) {
        super("AssignUnitToPersonMenu");
        initialize(campaign, unit);
    }
    //endregion Constructors

    //region Initialization
    private void initialize(final Campaign campaign, final Unit unit) {
        // Initialize Menu
        setText(resources.getString("AssignUnitToPersonMenu.title"));

        // Default Return for Illegal or Impossible Assignments
        // 1) No people to be assigned
        // 2) All people must be active non-prisoners (bondsmen should be assignable to units)
        // 3) All people must not be primary civilians
        // 4) All people must share one of their non-civilian professions

        // Person Assignment Menus
        final JMenu pilotMenu = new JScrollableMenu("pilotMenu", resources.getString("pilotMenu.text"));
        final JMenu driverMenu = new JScrollableMenu("driverMenu", resources.getString("driverMenu.text"));
        final JMenu gunnerMenu = new JScrollableMenu("gunnerMenu", resources.getString("gunnerMenu.text"));
        final JMenu crewmemberMenu = new JScrollableMenu("crewmemberMenu", resources.getString("crewmemberMenu.text"));
        final JMenu techOfficerMenu = new JScrollableMenu("techOfficerMenu", resources.getString("techOfficerMenu.text"));
        final JMenu consoleCommanderMenu = new JScrollableMenu("consoleCommanderMenu", resources.getString("consoleCommanderMenu.text"));
        final JMenu soldierMenu = new JScrollableMenu("soldierMenu", resources.getString("soldierMenu.text"));
        final JMenu navigatorMenu = new JScrollableMenu("navigatorMenu", resources.getString("navigatorMenu.text"));

        final List<Person> personnel = new ArrayList<>(campaign.getPersonnel());
        personnel.sort(new PersonTitleSorter());
        for (final Person person : personnel) {
            // Skip People if they are:
            // 1) Astech Primary role with the Medical, Administrator, or None Secondary Roles
            // 2) Medical Primary role with the Astech, Administrator, or None Secondary Roles
            // 3) Administrator Primary Role with Astech, Medical, Administrator, or None Secondary Roles
            // 4) Dependent Primary Role
            if (person.getPrimaryRole().isAstech() && (person.getSecondaryRole().isMedicalStaff()
                    || person.getSecondaryRole().isAdministrator() || person.getSecondaryRole().isNone())) {
                continue;
            } else if (person.getPrimaryRole().isMedicalStaff() && (person.getSecondaryRole().isAstech()
                    || person.getSecondaryRole().isAdministrator() || person.getSecondaryRole().isNone())) {
                continue;
            } else if (person.getPrimaryRole().isAdministrator() && (person.getSecondaryRole().isAstech()
                    || person.getSecondaryRole().isMedicalStaff() || person.getSecondaryRole().isAdministrator()
                    || person.getSecondaryRole().isNone())) {
                continue;
            } else if (person.getPrimaryRole().isDependent()) {
                continue;
            }

            // TODO : Finish me
            if (unit.usesSoloPilot()) {

            } else if (unit.usesSoldiers()) {

            } else {

            }
        }

        // Assign Tech to Unit Menu
        add(new AssignUnitToTechMenu(campaign, unit));

        // And finally add the ability to simply unassign
        final JMenuItem miUnassignPerson = new JMenuItem(resources.getString("None.text"));
        miUnassignPerson.setName("miUnassignPerson");
        miUnassignPerson.addActionListener(evt -> {
            // TODO : Finish me
        });
        add(miUnassignPerson);
    }
    //endregion Initialization
}
