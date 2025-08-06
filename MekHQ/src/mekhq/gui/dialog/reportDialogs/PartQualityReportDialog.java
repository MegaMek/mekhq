/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JTextPane;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;

/**
 * Represents a dialog for generating a part quality report. Extends the {@link AbstractReportDialog} class.
 */
public class PartQualityReportDialog extends AbstractReportDialog {
    //region Variable Declarations
    private final Unit unit;
    //endregion Variable Declarations

    //region Constructors

    /**
     * Constructs a new instance of {@link PartQualityReportDialog}.
     *
     * @param frame the parent {@link JFrame}
     * @param unit  the unit for which the parts quality report is being generated
     */
    public PartQualityReportDialog(final JFrame frame, final Unit unit) {
        super(frame, "PartQualityReportDialog", "PartQualityReportDialog.title");
        this.unit = unit;
        setTitle(String.format(resources.getString("PartQualityReportDialog.Unit.title"),
              unit.getName()));
        initialize();
        pack();
        setModal(true);
    }
    //endregion Constructors

    //region Getters

    /**
     * @return the unit associated with this object
     */
    public Unit getUnit() {
        return unit;
    }

    @Override
    protected JTextPane createTxtReport() {
        final JTextPane txtReport = new JTextPane();
        txtReport.setContentType("text/html");
        txtReport.setText(getPartsReport(getUnit()));
        txtReport.setName("txtReport");
        txtReport.setEditable(false);
        txtReport.setCaretPosition(0);
        return txtReport;
    }
    //endregion Getters

    /**
     * Produces a Part Quality report for the given unit. The report includes each part's location, name, and quality.
     *
     * @param unit The unit to generate a report for.
     *
     * @return An HTML string displaying the status of each part in the unit.
     */
    private String getPartsReport(Unit unit) {
        // This map will hold part lists, keyed by the location on the unit.
        Map<String, List<Part>> reportMap = new HashMap<>();

        // Iterate over parts, assigning each to its location in the map.
        for (Part part : unit.getParts()) {
            if (part instanceof AmmoBin) {
                continue;
            }

            String location = part.getLocationName() != null ? part.getLocationName() : unit.getName();
            reportMap.computeIfAbsent(location, k -> new ArrayList<>()).add(part);
        }

        // Create a sorted list of locations, excluding the unit's name.
        List<String> locations = new ArrayList<>();
        for (String location : reportMap.keySet()) {
            if (!location.equals(unit.getName())) {
                locations.add(location);
            }
        }
        Collections.sort(locations);

        // Add the unit's name to the start of the sorted locations list.
        locations.add(0, unit.getName());

        // Begin the HTML report.
        StringBuilder report = new StringBuilder("<html>");

        // For each location, add reported details about that location's parts.
        for (String location : locations) {
            report.append("<b>");
            if (location.equals(unit.getName())) {
                String colorCode = unit.getQuality().getHexColor();

                // Add the location and its colored quality rating to the report.
                int headerFontSize = UIUtil.scaleForGUI(18);
                report.append("<span style=\"font-size: ").append(headerFontSize).append("px;\">")
                      .append(location)
                      .append(" - ");
                report.append("<span style=\"color: ").append(colorCode).append(";\">")
                      .append(unit.getQualityName())
                      .append("</span>");
                report.append("</span>");
            } else {
                int headerFontSize = UIUtil.scaleForGUI(12);
                report.append("<span style=\"font-size: ").append(headerFontSize).append("px;\">")
                      .append(location).append("</span>");
            }
            report.append("</b><br>");

            // For each part in the current location, add it to the report.
            for (Part part : reportMap.get(location)) {
                report.append(part.getName()).append(" - ");

                String colorCode = part.getQuality().getHexColor();

                report.append(ReportingUtilities.spanOpeningWithCustomColor(colorCode))
                      .append(part.getQualityName()).append(CLOSING_SPAN_TAG).append("<br>");
            }

            // Add a line break between locations.
            report.append("<br>");
        }

        // Finish the HTML report.
        report.append("</html>");

        return report.toString();
    }

}
