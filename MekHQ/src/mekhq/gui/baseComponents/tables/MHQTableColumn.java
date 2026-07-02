/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package mekhq.gui.baseComponents.tables;

import java.util.Comparator;

/**
 * Defines the contract for column definitions used within an {@link MHQTableModel} and displayed by an
 * {@link MHQTable}. Implementations of this interface encapsulate the methods required to render, sort, and align a
 * specific column in the UI.
 *
 * @author Hokk
 * @since 0.51.01
 */
public interface MHQTableColumn {

    /**
     * Returns the index of this column model.
     */
    int getIndex();

    /**
     * Generates string representation of the column cell.
     *
     * @param model The underlying data object for a cell
     *
     * @return The formatted string to be displayed in the UI for this cell
     */
    String getText(Object model);

    /**
     * Returns the comparator used to sort the data in this column.
     */
    Comparator<?> getComparator();

    /**
     * Retrieves the preferred visual width of the column in pixels.
     *
     * @return The preferred width as an {@code Integer}, or null if the column should use default sizing logic
     */
    Integer getPreferredWidth();

    /**
     * Retrieves the horizontal text alignment for the cells within this column.
     *
     * @return An integer representing a {@link javax.swing.SwingConstants} value
     */
    int getAlignment();

}
