/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
package mekhq.gui.dialog.reportDialogs;

import mekhq.MHQConstants;
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
        txtCombatPersonnel.setFont(new Font(MHQConstants.FONT_COURIER_NEW, Font.PLAIN, 12));
        txtCombatPersonnel.setEditable(false);

        final JTextPane txtSupportPersonnel = new JTextPane();
        txtSupportPersonnel.setText(getPersonnelReport().getSupportPersonnelDetails());
        txtSupportPersonnel.setName("txtSupportPersonnel");
        txtSupportPersonnel.setFont(new Font(MHQConstants.FONT_COURIER_NEW, Font.PLAIN, 12));
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
