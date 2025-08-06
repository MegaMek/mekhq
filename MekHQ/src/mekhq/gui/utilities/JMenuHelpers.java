/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import megamek.client.ui.util.MenuScroller;
import mekhq.MHQConstants;

public class JMenuHelpers {
    /**
     * This is used to add a JMenu to another JMenu, provided it isn't empty, and then add a scroller to it if it is
     * above the default minimum threshold
     *
     * @param menu  the JMenu to add the child to
     * @param child the JMenu to add
     *
     * @deprecated since 0.50.04, replaced by {@link mekhq.gui.baseComponents.JScrollableMenu}
     */
    @Deprecated(since = "0.50.04")
    public static void addMenuIfNonEmpty(JMenu menu, JMenu child) {
        addMenuIfNonEmpty(menu, child, MHQConstants.BASE_SCROLLER_THRESHOLD);
    }

    /**
     * This is used to add a JMenu to another JMenu, provided it isn't empty, and then add a scroller to it if it is
     * above the minimum threshold
     *
     * @param menu              the JMenu to add the child to
     * @param child             the JMenu to add
     * @param scrollerThreshold the threshold for adding a scroller
     *
     * @deprecated since 0.50.04, replaced by {@link mekhq.gui.baseComponents.JScrollableMenu}
     */
    @Deprecated(since = "0.50.04")
    public static void addMenuIfNonEmpty(JMenu menu, JMenu child, int scrollerThreshold) {
        if (child.getItemCount() > 0) {
            menu.add(child);
            if (child.getItemCount() > scrollerThreshold) {
                MenuScroller.setScrollerFor(child, scrollerThreshold);
            }
        }
    }

    /**
     * This is used to add a JMenu to a JPopupMenu, provided it isn't empty, and then add a scroller to it if it is
     * above the default minimum threshold
     *
     * @param menu  the JPopupMenu to add the child to
     * @param child the JMenu to add
     *
     * @deprecated since 0.50.04, replaced by {@link mekhq.gui.baseComponents.JScrollablePopupMenu}
     */
    @Deprecated(since = "0.50.04")
    public static void addMenuIfNonEmpty(JPopupMenu menu, JMenu child) {
        addMenuIfNonEmpty(menu, child, MHQConstants.BASE_SCROLLER_THRESHOLD);
    }

    /**
     * This is used to add a JMenu to a JPopupMenu, provided it isn't empty, and then add a scroller to it if it is
     * above the minimum threshold
     *
     * @param menu              the JPopupMenu to add the child to
     * @param child             the JMenu to add
     * @param scrollerThreshold the threshold for adding a scroller
     *
     * @deprecated since 0.50.04, replaced by {@link mekhq.gui.baseComponents.JScrollablePopupMenu}
     */
    @Deprecated(since = "0.50.04")
    public static void addMenuIfNonEmpty(JPopupMenu menu, JMenu child, int scrollerThreshold) {
        if (child.getItemCount() > 0) {
            menu.add(child);
            if (child.getItemCount() > scrollerThreshold) {
                MenuScroller.setScrollerFor(child, scrollerThreshold);
            }
        }
    }
}
