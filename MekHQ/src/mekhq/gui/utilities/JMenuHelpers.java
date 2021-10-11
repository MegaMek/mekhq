/*
 * Copyright (C) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.gui.utilities;

import megamek.client.ui.swing.util.MenuScroller;
import mekhq.MekHqConstants;

import javax.swing.*;

public class JMenuHelpers {
    /**
     * This is used to add a JMenu to another JMenu, provided it isn't empty, and then add a scroller
     * to it if it is above the default minimum threshold
     * @param menu the JMenu to add the child to
     * @param child the JMenu to add
     */
    @Deprecated // Replaced by JScrollableMenu
    public static void addMenuIfNonEmpty(JMenu menu, JMenu child) {
        addMenuIfNonEmpty(menu, child, MekHqConstants.BASE_SCROLLER_THRESHOLD);
    }

    /**
     * This is used to add a JMenu to another JMenu, provided it isn't empty, and then add a scroller
     * to it if it is above the minimum threshold
     * @param menu the JMenu to add the child to
     * @param child the JMenu to add
     * @param scrollerThreshold the threshold for adding a scroller
     */
    @Deprecated // Replaced by JScrollableMenu
    public static void addMenuIfNonEmpty(JMenu menu, JMenu child, int scrollerThreshold) {
        if (child.getItemCount() > 0) {
            menu.add(child);
            if (child.getItemCount() > scrollerThreshold) {
                MenuScroller.setScrollerFor(child, scrollerThreshold);
            }
        }
    }

    /**
     * This is used to add a JMenu to a JPopupMenu, provided it isn't empty, and then add a scroller
     * to it if it is above the default minimum threshold
     * @param menu the JPopupMenu to add the child to
     * @param child the JMenu to add
     */
    @Deprecated // Replaced by JScrollablePopupMenu
    public static void addMenuIfNonEmpty(JPopupMenu menu, JMenu child) {
        addMenuIfNonEmpty(menu, child, MekHqConstants.BASE_SCROLLER_THRESHOLD);
    }

    /**
     * This is used to add a JMenu to a JPopupMenu, provided it isn't empty, and then add a scroller
     * to it if it is above the minimum threshold
     *
     * @param menu the JPopupMenu to add the child to
     * @param child the JMenu to add
     * @param scrollerThreshold the threshold for adding a scroller
     */
    @Deprecated // Replaced by JScrollablePopupMenu
    public static void addMenuIfNonEmpty(JPopupMenu menu, JMenu child, int scrollerThreshold) {
        if (child.getItemCount() > 0) {
            menu.add(child);
            if (child.getItemCount() > scrollerThreshold) {
                MenuScroller.setScrollerFor(child, scrollerThreshold);
            }
        }
    }
}
