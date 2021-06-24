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

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.sorter.PersonTitleSorter;
import mekhq.gui.utilities.JMenuHelpers;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AssignUnitToPersonMenu extends JMenu {
    //region Variable Declarations
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public AssignUnitToPersonMenu(final Campaign campaign, final Unit unit) {
        super();
        initialize(campaign, unit);
    }
    //endregion Constructors

    //region Initialization
    private void initialize(final Campaign campaign, final Unit unit) {
        // Initialize Menu
        setText(resources.getString("AssignUnitToPersonMenu.title"));
        setName("AssignUnitToPersonMenu");

        // Person Assignment Menus
        final JMenu pilotMenu = new JMenu(resources.getString("pilotMenu.text"));
        pilotMenu.setName("pilotMenu");
        final JMenu driverMenu = new JMenu(resources.getString("driverMenu.text"));
        driverMenu.setName("driverMenu");
        final JMenu gunnerMenu = new JMenu(resources.getString("gunnerMenu.text"));
        gunnerMenu.setName("gunnerMenu");
        final JMenu crewmemberMenu = new JMenu(resources.getString("crewmemberMenu.text"));
        crewmemberMenu.setName("crewmemberMenu");
        final JMenu techOfficerMenu = new JMenu(resources.getString("techOfficerMenu.text"));
        techOfficerMenu.setName("techOfficerMenu");
        final JMenu consoleCommanderMenu = new JMenu(resources.getString("consoleCommanderMenu.text"));
        consoleCommanderMenu.setName("consoleCommanderMenu");
        final JMenu soldierMenu = new JMenu(resources.getString("soldierMenu.text"));
        soldierMenu.setName("soldierMenu");
        final JMenu navigatorMenu = new JMenu(resources.getString("navigatorMenu.text"));
        navigatorMenu.setName("navigatorMenu");

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

            if (unit.usesSoloPilot()) {

            } else if (unit.usesSoldiers()) {

            } else {

            }
        }

        // Assign Tech to Unit Menu
        JMenuHelpers.addMenuIfNonEmpty(this, new AssignUnitToTechMenu(campaign, unit));
    }
    //endregion Initialization
}
