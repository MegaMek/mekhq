/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.force.Force.FORCE_NONE;
import static mekhq.campaign.force.Force.FORCE_ORIGIN;
import static mekhq.utilities.EntityUtilities.isUnsupportedEntity;

import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.JScrollableMenu;
import mekhq.utilities.MHQInternationalization;

/**
 * A dynamic, scrollable menu for assigning one or more {@link Unit} objects to forces within a {@link Campaign}'s TO&E
 * structure.
 *
 * <p>This menu provides:</p>
 * <ul>
 *     <li>A "clear assignment" option that moves units to {@link Force#FORCE_NONE}</li>
 *     <li>A hierarchical tree of all forces descending from {@link Force#FORCE_ORIGIN}</li>
 *     <li>Per-force assignment actions for each force in the hierarchy</li>
 * </ul>
 *
 * <p>The menu is built only when all selected units are valid for assignment. Invalid inputs—such as unavailable
 * units, units with {@code null} entities, or units with unsupported entity types—cause the menu to short-circuit
 * and display nothing.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class AssignUnitToForceMenu extends JScrollableMenu {
    private static final MMLogger LOGGER = MMLogger.create(AssignUnitToForceMenu.class);

    /**
     * Constructs a new {@code AssignUnitToForceMenu} and initializes all force assignment options based on the given
     * campaign and units.
     *
     * @param campaign the campaign whose forces are being modified
     * @param units    the units eligible for force reassignment
     *
     * @author Illiani
     * @since 0.50.10
     */
    public AssignUnitToForceMenu(final Campaign campaign, final Unit... units) {
        super("AssignUnitToForceMenu");
        initialize(campaign, units);
    }

    /**
     * Performs initial validation and populates the menu if all selected units are eligible for assignment.
     *
     * <p>If any unit fails validation, the menu remains empty.</p>
     *
     * @param campaign the active campaign
     * @param units    the units being evaluated for assignment
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void initialize(final Campaign campaign, final Unit... units) {
        // Immediate return for invalid states
        // 1) No unit is selected
        // 2) Any unit is unavailable
        // 3) Any entity is null
        // 4) Any entity is unsupported (such as turrets)
        if (units.length == 0 ||
                  Arrays.stream(units).anyMatch(unit -> !unit.isAvailable() ||
                                                              unit.getEntity() == null ||
                                                              isUnsupportedEntity(unit.getEntity()))) {
            return;
        }

        setText(MHQInternationalization.getText("AssignUnitToForceMenu.title"));
        createForceAssignmentMenus(campaign, units);
    }


    /**
     * Creates the top-level force assignment structure, including:
     *
     * <ul>
     *     <li>A "clear assignment" option that removes units from all forces</li>
     *     <li>A recursive force hierarchy beginning with {@link Force#FORCE_ORIGIN}</li>
     * </ul>
     *
     * @param campaign the campaign whose forces are available
     * @param units    the units being reassigned
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void createForceAssignmentMenus(final Campaign campaign,
          final Unit... units) {

        JMenuItem clearAssignment = new JMenuItem(MHQInternationalization.getText("AssignUnitToForceMenu.clear"));
        clearAssignment.addActionListener(ev -> {
            for (Unit unit : units) {
                campaign.addUnitToForce(unit, FORCE_NONE);
            }
        });
        add(clearAssignment);

        Force originForce = campaign.getForce(FORCE_ORIGIN); // All other forces descend from this force
        addForceMenu(this, campaign, units, originForce);
    }

    /**
     * Recursively builds a subtree of assignment options for the given force and all of its descendants.
     *
     * <p>Each force is represented as a submenu with two capabilities:</p>
     * <ul>
     *     <li>Selecting the submenu header assigns units to that force</li>
     *     <li>A direct "assign here" menu item performs the same action</li>
     * </ul>
     *
     * <p>The submenu then recursively includes all subordinate forces.</p>
     *
     * @param parent   the UI component (menu or submenu) to append to
     * @param campaign the campaign context
     * @param units    the units that will be assigned upon selection
     * @param force    the force represented by this submenu
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void addForceMenu(JComponent parent, final Campaign campaign, final Unit[] units, final Force force) {
        // A submenu for this force
        JMenu forceMenu = new JMenu(force.getName());
        forceMenu.addActionListener(ev -> addToForce(campaign, units, force));

        // Option to assign directly to this force
        JMenuItem assignHere = new JMenuItem(MHQInternationalization.getFormattedText("AssignUnitToForceMenu.subMenu",
              force.getName()));
        assignHere.addActionListener(ev -> addToForce(campaign, units, force));
        forceMenu.add(assignHere);

        // Recurse for all children
        for (Force child : force.getSubForces()) {
            addForceMenu(forceMenu, campaign, units, child);
        }

        parent.add(forceMenu);
    }

    /**
     * Assigns all provided units to the specified force.
     *
     * <p>This method wraps a call to {@link Campaign#addUnitToForce(Unit, int)}, ensuring that each unit is moved to
     * the provided force ID.</p>
     *
     * @param campaign    the campaign receiving the assignment update
     * @param units       the units to move
     * @param originForce the force to assign units to
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void addToForce(Campaign campaign, Unit[] units, Force originForce) {
        for (Unit unit : units) {
            campaign.addUnitToForce(unit, originForce.getId());
        }
    }
}
