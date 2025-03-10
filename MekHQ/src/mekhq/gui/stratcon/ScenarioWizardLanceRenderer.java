/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.stratcon;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.icons.enums.OperationalStatus;

import static mekhq.campaign.icons.enums.OperationalStatus.NOT_OPERATIONAL;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Handles rendering of individual lances in the StratCon scenario wizard.
 * @author NickAragua
 */
public class ScenarioWizardLanceRenderer extends JLabel implements ListCellRenderer<Force> {
    private final Campaign campaign;

    public ScenarioWizardLanceRenderer(Campaign campaign) {
        this.campaign = campaign;
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends Force> list, final Force force,
                                                  final int index, final boolean isSelected,
                                                  final boolean cellHasFocus) {
        // JTextArea::setForeground and JTextArea::setBackground don't work properly with the
        // default return on all themes, but by recreating the colour it works properly
        final Color foreground = new Color((isSelected
                ? list.getSelectionForeground() : list.getForeground()).getRGB());
        final Color background = new Color((isSelected
                ? list.getSelectionBackground() : list.getBackground()).getRGB());
        setForeground(foreground);
        setBackground(background);

        // Determine name color
        OperationalStatus operationalStatus = force.updateForceIconOperationalStatus(campaign).get(0);

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

        // Get combat role
        CombatTeam combatTeam = campaign.getCombatTeamsTable().get(force.getId());
        String roleString = "";
        if (combatTeam != null) {
            roleString = combatTeam.getRole().toString() + ", ";
        }

        // Adjust force name to remove unnecessary information
        String forceName = force.getFullName();
        String originNodeName = ", " + campaign.getForce(0).getName();
        forceName = forceName.replaceAll(originNodeName, "");

        // Format string
        setText(String.format("<html>%s<b>%s%s, %s</b> - BV %s<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>%s</i></html>",
            statusOpenFormat, force.getName(), statusCloseFormat, roleString,
            force.getTotalBV(campaign, true), forceName));

        return this;
    }
}
