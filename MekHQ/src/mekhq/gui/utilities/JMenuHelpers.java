/*
 * Copyright (C) 2020 - The MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.utilities;

import megamek.client.ui.swing.util.MenuScroller;

import javax.swing.*;

public class JMenuHelpers {
    public static void addMenuIfNonEmpty(JMenu menu, JMenu child, int scrollerThreshold) {
        if (child.getItemCount() > 0) {
            menu.add(child);
            if (child.getItemCount() > scrollerThreshold) {
                MenuScroller.setScrollerFor(child, scrollerThreshold);
            }
        }
    }
}
