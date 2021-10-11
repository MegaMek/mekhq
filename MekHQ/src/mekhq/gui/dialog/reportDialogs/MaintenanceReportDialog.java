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
package mekhq.gui.dialog.reportDialogs;

import mekhq.campaign.unit.Unit;

import javax.swing.*;

public class MaintenanceReportDialog extends AbstractReportDialog {
    //region Variable Declarations
    private final Unit unit;
    //endregion Variable Declarations

    //region Constructors
    public MaintenanceReportDialog(final JFrame frame, final Unit unit) {
        super(frame, "MaintenanceReportDialog", "MaintenanceReportDialog.title");
        this.unit = unit;
        setTitle(String.format(resources.getString("MaintenanceReportDialog.Unit.title"), unit.getName()));
        initialize();
    }
    //endregion Constructors

    //region Getters
    public Unit getUnit() {
        return unit;
    }

    @Override
    protected JTextPane createTxtReport() {
        final JTextPane txtReport = new JTextPane();
        txtReport.setContentType("text/html");
        txtReport.setText(getUnit().getLastMaintenanceReport());
        txtReport.setName("txtReport");
        txtReport.setEditable(false);
        txtReport.setCaretPosition(0);
        return txtReport;
    }
    //endregion Getters
}
