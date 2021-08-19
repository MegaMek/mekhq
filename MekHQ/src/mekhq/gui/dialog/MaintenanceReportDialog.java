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

import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.AbstractMHQDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MaintenanceReportDialog extends AbstractMHQDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 3624327778807359294L;

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
    //endregion Getters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        final JTextPane txtReport = new JTextPane();
        txtReport.setContentType("text/html");
        txtReport.setText(getUnit().getLastMaintenanceReport());
        txtReport.setName("txtReport");
        txtReport.setEditable(false);
        txtReport.setCaretPosition(0);

        final JScrollPane scrollPane = new JScrollPane(txtReport);
        scrollPane.setBorder(new EmptyBorder(2, 10, 2, 2));
        scrollPane.setName("reportPane");

        return scrollPane;
    }
    //endregion Initialization
}
