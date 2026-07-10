/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.time.LocalDate;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.campaign.Campaign;
import mekhq.campaign.location.ILocation;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.model.LocationDisplay;

/**
 * A small, reusable "Location" block for any {@link ILocation} (unit, person, etc.). It shows the item's current
 * location and, when the item is in transit, its destination. Each line reads
 * {@code Location Name (Location Planet, Location System)}.
 *
 * <p>All display strings come from {@link LocationDisplay}, the same tree-walking helper used by
 * {@link LocationPlacePanel}, so the output stays consistent across the location views.</p>
 */
public class LocationSummaryPanel extends JPanel {

    public LocationSummaryPanel(ILocation location, Campaign campaign) {
        LocalDate today = campaign.getLocalDate();

        StringBuilder html = new StringBuilder("<html>");
        html.append(line("LocationSummaryPanel.label.current",
              LocationDisplay.getLocationName(location, campaign, today),
              LocationDisplay.getLocationPlanet(location, today, campaign),
              LocationDisplay.getLocationSystem(location, today, campaign)));
        ILocation queuedDestination = campaign.getCampaignLocationManager().getQueuedDestination(location);
        if (queuedDestination != null) {
            html.append("<br>").append(line("LocationSummaryPanel.label.queued",
                  LocationDisplay.getLocationName(queuedDestination, campaign, today),
                  LocationDisplay.getLocationPlanet(queuedDestination, today, campaign),
                  LocationDisplay.getLocationSystem(queuedDestination, today, campaign)));
        }
        if (location.isInTransit()) {
            html.append("<br>").append(line("LocationSummaryPanel.label.destination",
                  LocationDisplay.getDestinationName(location, campaign, today),
                  LocationDisplay.getDestinationPlanet(location, today),
                  LocationDisplay.getDestinationSystem(location, today)));
        }
        html.append("</html>");

        setLayout(new BorderLayout());
        setBorder(RoundedLineBorder.createRoundedLineBorder(getText("LocationSummaryPanel.title")));
        add(new JLabel(html.toString()), BorderLayout.CENTER);
    }

    /** Builds a labeled location line: {@code <label> Name (Planet, System)}. */
    private static String line(String labelKey, String name, String planet, String system) {
        return getText(labelKey) + ' ' + escapeHtml(name) + " (" + escapeHtml(planet) + ", " + escapeHtml(system) + ')';
    }

    /** Escapes the HTML metacharacters that would otherwise mis-render inside the Swing {@code <html>} label. */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
