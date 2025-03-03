/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/
package mekhq.gui.stratcon;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.awt.*;

import static mekhq.campaign.icons.enums.OperationalStatus.NOT_OPERATIONAL;
import static mekhq.campaign.icons.enums.OperationalStatus.determineLayeredForceIconOperationalStatus;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * Handles rendering individual units in lists in the StratCon scenario wizard.
 * @author NickAragua
 */
public class ScenarioWizardUnitRenderer extends JLabel implements ListCellRenderer<Unit> {
    public ScenarioWizardUnitRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Unit> list, Unit unit, int index,
            boolean isSelected, boolean cellHasFocus) {
        Campaign campaign = unit.getCampaign();

        int valueForceId = unit.getForceId();
        Force force = campaign.getForce(valueForceId);

        // Determine name color
        OperationalStatus operationalStatus = determineLayeredForceIconOperationalStatus(unit);

        String statusOpenFormat = switch (operationalStatus) {
            case NOT_OPERATIONAL -> "<s>";
            case MARGINALLY_OPERATIONAL -> spanOpeningWithCustomColor(
                MekHQ.getMHQOptions().getFontColorNegativeHexColor());
            case SUBSTANTIALLY_OPERATIONAL -> spanOpeningWithCustomColor(
                MekHQ.getMHQOptions().getFontColorWarningHexColor());
            case FULLY_OPERATIONAL, FACTORY_FRESH -> spanOpeningWithCustomColor(
                MekHQ.getMHQOptions().getFontColorPositiveHexColor());
        };

        String statusCloseFormat = operationalStatus == NOT_OPERATIONAL ? "</s>" : CLOSING_SPAN_TAG;

        // Adjust force name to remove unnecessary information
        String forceName = "";
        if (force != null) {
            forceName = force.getFullName();
            String originNodeName = ", " + campaign.getForce(0).getName();
            forceName = forceName.replaceAll(originNodeName, "");
        }

        // Format string
        setText(String.format("<html><b>%s%s%s (%s/%s)</b> - Base BV: %d<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>%s</i></html>",
            statusOpenFormat, unit.getName(), statusCloseFormat, unit.getEntity().getCrew().getGunnery(),
            unit.getEntity().getCrew().getPiloting(), unit.getEntity().calculateBattleValue(true, true),
            forceName));

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }

}
