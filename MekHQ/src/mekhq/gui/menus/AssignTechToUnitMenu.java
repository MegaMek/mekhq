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
import mekhq.gui.baseComponents.JScrollableMenu;

import javax.swing.*;

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

        // Default Return for Illegal Assignments - Null/Empty Skill Name or Self-Crewed Units
        // don't need techs, and if the total maintenance time is longer than the maximum for a
        // person we can just skip too
        // TODO :Finish me


        // Person Assignment Menus
        // TODO :Finish me


        // And finally add the ability to simply unassign
        final JMenuItem miUnassignPerson = new JMenuItem(resources.getString("None.text"));
        miUnassignPerson.setName("miUnassignTech");
        miUnassignPerson.addActionListener(evt -> {
            // TODO :Finish me
        });
        add(miUnassignPerson);
    }
    //endregion Initialization
}
