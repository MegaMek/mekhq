/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.stratcon;

import static mekhq.campaign.icons.enums.OperationalStatus.NOT_OPERATIONAL;
import static mekhq.campaign.icons.enums.OperationalStatus.determineLayeredForceIconOperationalStatus;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;

/**
 * Handles rendering individual units in lists in the StratCon scenario wizard.
 *
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
                  ReportingUtilities.getNegativeColor());
            case SUBSTANTIALLY_OPERATIONAL -> spanOpeningWithCustomColor(
                  ReportingUtilities.getWarningColor());
            case FULLY_OPERATIONAL, FACTORY_FRESH -> spanOpeningWithCustomColor(
                  ReportingUtilities.getPositiveColor());
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
        setText(String.format(
              "<html><b>%s%s%s (%s/%s)</b> - Base BV: %d<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>%s</i></html>",
              statusOpenFormat,
              unit.getName(),
              statusCloseFormat,
              unit.getEntity().getCrew().getGunnery(),
              unit.getEntity().getCrew().getPiloting(),
              unit.getEntity().calculateBattleValue(true, true),
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
