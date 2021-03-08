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
package mekhq.gui.panels;

import javax.swing.*;
import java.time.LocalDate;

public class DateSelectionPanel extends JPanel {
    //region Variable Declarations
    private LocalDate date;
    //endregion Variable Declarations

    //region Constructors
    public DateSelectionPanel(final LocalDate date) {
        initialize(date);
        this.date = date; // remove
    }
    //endregion Constructors

    //region Getters/Setters
    public LocalDate getDate() {
        return date;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize(final LocalDate date) {

    }
    //endregion Initialization
}
