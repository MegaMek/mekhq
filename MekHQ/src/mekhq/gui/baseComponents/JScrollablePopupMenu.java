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
 */
package mekhq.gui.baseComponents;

import megamek.client.ui.swing.util.MenuScroller;
import mekhq.MekHQ;
import mekhq.MHQConstants;

import java.util.ResourceBundle;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

/**
 * JScrollablePopupMenu is an extension of JPopupMenu that expands the add functionality so that it
 * adds child menus only if they are not empty, and then adds a scroller to them if they are of the
 * specified size or larger.
 *
 * WARNING: When using this menu always have it be strictly declared to the max abstraction of this
 * WARNING: class, or the menu addition will be assumed to be using the base JMenu add
 * WARNING: e.g. use JScrollablePopupMenu menu = new JScrollablePopupMenu("menu"),
 * WARNING: never JPopupMenu menu = new JScrollablePopupMenu("menu")
 */
public class JScrollablePopupMenu extends JPopupMenu {
    //region Variable Declarations
    protected final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Constructors
    public JScrollablePopupMenu(final String name) {
        super();
        setName(name);
    }

    public JScrollablePopupMenu(final String name, final String text) {
        super(text);
        setName(name);
    }
    //endregion Constructors

    /**
     * This is used to add a JMenu to this, provided the former isn't empty, and then add a scroller
     * to the child if it is above the default minimum threshold
     * @param child the JMenu to add
     */
    public void add(final JMenu child) {
        add(child, MHQConstants.BASE_SCROLLER_THRESHOLD);
    }

    /**
     * This is used to add a JMenu this, provided the former isn't empty, and then add a scroller
     * to the child if it is above the provided threshold
     * @param child the JMenu to add
     * @param scrollerThreshold the threshold for adding a scroller
     */
    public void add(final JMenu child, final int scrollerThreshold) {
        if (child.getItemCount() > 0) {
            super.add(child);
            if (child.getItemCount() > scrollerThreshold) {
                MenuScroller.setScrollerFor(child, scrollerThreshold);
            }
        }
    }
}
