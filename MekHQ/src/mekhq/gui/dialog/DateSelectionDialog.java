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
package mekhq.gui.dialog;

import mekhq.gui.view.DateSelectionPanel;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;

public class DateSelectionDialog extends BaseDialog {
    //region Variable Declarations
    private LocalDate initialDate;

    private DateSelectionPanel dateSelectionPanel;
    //endregion Variable Declarations

    //region Constructors
    public DateSelectionDialog(final JFrame frame, final LocalDate initialDate) {
        super(frame, "DateSelectionDialog.title");
        setInitialDate(initialDate);
        initialize("DateSelectionDialog");
    }
    //endregion Constructors

    //region Getters/Setters
    public LocalDate getInitialDate() {
        return initialDate;
    }

    public void setInitialDate(LocalDate initialDate) {
        this.initialDate = initialDate;
    }

    public DateSelectionPanel getDateSelectionPanel() {
        return dateSelectionPanel;
    }

    public void setDateSelectionPanel(final DateSelectionPanel dateSelectionPanel) {
        this.dateSelectionPanel = dateSelectionPanel;
    }

    /**
     * @return the specified date
     */
    public LocalDate getDate() {
        return getResult().isConfirmed() ? getDateSelectionPanel().getDate() : getInitialDate();
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setDateSelectionPanel(new DateSelectionPanel(getInitialDate()));
        return getDateSelectionPanel();
    }
    //endregion Initialization

    @Override
    protected void okAction() {

    }

    @Override
    protected void cancelAction() {

    }
}
