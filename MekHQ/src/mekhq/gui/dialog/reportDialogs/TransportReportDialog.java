/*
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.reportDialogs;

import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import mekhq.MHQConstants;
import mekhq.campaign.report.TransportReport;

public class TransportReportDialog extends AbstractReportDialog {
    //region Variable Declarations
    private final TransportReport transportReport;
    //endregion Variable Declarations

    //region Constructors
    public TransportReportDialog(final JFrame frame, final TransportReport transportReport) {
        super(frame, "TransportReportDialog", "TransportReportDialog.title");
        this.transportReport = transportReport;
        initialize();
    }
    //endregion Constructors

    //region Getters
    public TransportReport getTransportReport() {
        return transportReport;
    }

    @Override
    protected JTextPane createTxtReport() {
        final JTextPane txtReport = new JTextPane();
        String reportText = getTransportReport().getTransportDetails();
        txtReport.setName("txtReport");
        txtReport.setFont(new Font(MHQConstants.FONT_COURIER_NEW, Font.PLAIN, 12));
        txtReport.setText(reportText);
        applyHeaderStyle(txtReport, reportText);
        txtReport.setEditable(false);
        txtReport.setCaretPosition(0);
        return txtReport;
    }

    private static void applyHeaderStyle(final JTextPane txtReport, final String reportText) {
        int headerEnd = reportText.indexOf('\n');
        if (headerEnd < 0) {
            return;
        }

        SimpleAttributeSet headerAttributes = new SimpleAttributeSet();
        StyleConstants.setBold(headerAttributes, true);

        StyledDocument document = txtReport.getStyledDocument();
        document.setCharacterAttributes(0, headerEnd, headerAttributes, false);
    }
    //endregion Getters
}
