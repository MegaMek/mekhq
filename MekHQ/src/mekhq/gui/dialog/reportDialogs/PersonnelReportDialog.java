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

import mekhq.campaign.report.PersonnelReport;

import javax.swing.*;
import java.awt.*;

public class PersonnelReportDialog extends AbstractReportDialog {
    //region Variable Declarations
    private final PersonnelReport personnelReport;
    //endregion Variable Declarations

    //region Constructors
    public PersonnelReportDialog(final JFrame frame, final PersonnelReport personnelReport) {
        super(frame, "PersonnelReportDialog", "PersonnelReportDialog.title");
        this.personnelReport = personnelReport;
        initialize();
    }
    //endregion Constructors

    //region Getters
    public PersonnelReport getPersonnelReport() {
        return personnelReport;
    }

    @Override
    protected JTextPane createTxtReport() {
        final JTextPane txtCombatPersonnel = new JTextPane();
        txtCombatPersonnel.setText(getPersonnelReport().getCombatPersonnelDetails());
        txtCombatPersonnel.setName("txtCombatPersonnel");
        txtCombatPersonnel.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtCombatPersonnel.setEditable(false);

        final JTextPane txtSupportPersonnel = new JTextPane();
        txtSupportPersonnel.setText(getPersonnelReport().getSupportPersonnelDetails());
        txtSupportPersonnel.setName("txtSupportPersonnel");
        txtSupportPersonnel.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtSupportPersonnel.setEditable(false);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                txtCombatPersonnel, txtSupportPersonnel);
        splitPane.setName("personnelReportPane");
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.5);

        final JTextPane txtReport = new JTextPane();
        txtReport.setName("txtReport");
        txtReport.setAlignmentY(1.0f);
        txtReport.setEditable(false);
        txtReport.setCaretPosition(0);
        txtReport.insertComponent(splitPane);
        return txtReport;
    }
    //endregion Getters
}
