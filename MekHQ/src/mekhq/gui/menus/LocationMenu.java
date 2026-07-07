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
package mekhq.gui.menus;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.List;
import javax.swing.JFrame;
import javax.swing.JMenuItem;

import mekhq.campaign.Campaign;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationUtils;
import mekhq.gui.baseComponents.JScrollableMenu;

/**
 * Top-level "Location" context menu for selected {@link ILocation} items (persons, units, or spare
 * parts). It always offers a "Send To..." destination picker ({@link SendToLocationMenu}) that queues
 * travel for the next day-advance.
 *
 * <p>When GM mode is enabled it additionally offers two GM overrides, marked with a " (GM)" label
 * suffix per the codebase convention:</p>
 * <ul>
 *   <li><b>Teleport To...</b> — the same destination picker, but the items are dispatched and land at
 *       the destination immediately (zero transit).</li>
 *   <li><b>Complete Travel</b> — force-completes travel for the selected items already in transit or
 *       queued; disabled when none of the selection is traveling or queued.</li>
 * </ul>
 */
public class LocationMenu extends JScrollableMenu {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.SendToLocationMenu";

    /**
     * @param campaign the active campaign
     * @param frame    parent frame for any dialogs
     * @param items    the {@link ILocation} objects to act on (persons, units, or parts)
     */
    public LocationMenu(Campaign campaign, JFrame frame, List<? extends ILocation> items) {
        super("LocationMenu");
        initialize(campaign, frame, items);
    }

    private void initialize(Campaign campaign, JFrame frame, List<? extends ILocation> items) {
        if (items.isEmpty()) {
            return;
        }

        setText(getTextAt(RESOURCE_BUNDLE, "menu.location.text"));

        add(new SendToLocationMenu(campaign, frame, items,
              destination -> campaign.getCampaignLocationManager().queueTravel(items, destination)));

        if (campaign.isGM()) {
            add(new SendToLocationMenu(campaign, frame, items,
                  destination -> campaign.getCampaignLocationManager().gmTeleport(campaign, items, destination),
                  "menu.teleportTo.text"));

            JMenuItem completeTravel = new JMenuItem(completeTravelLabel(campaign, items));
            completeTravel.setEnabled(anyTravelingOrQueued(campaign, items));
            completeTravel.addActionListener(e -> campaign.getCampaignLocationManager().gmCompleteTravel(campaign, items));
            add(completeTravel);
        }
    }

    /**
     * Builds the "Complete Travel" label, appending a "(+ n others)" hint when collapsing the selection's shared travel
     * node(s) would also arrive co-travelers that are not part of the selection.
     */
    private static String completeTravelLabel(Campaign campaign, List<? extends ILocation> items) {
        String label = getTextAt(RESOURCE_BUNDLE, "menuItem.completeTravel.text");
        int others = campaign.getCampaignLocationManager().countUnselectedCoTravelers(items);
        if (others > 0) {
            label += " " + getFormattedTextAt(RESOURCE_BUNDLE, "menuItem.completeTravel.others.text", others);
        }
        return label;
    }

    private static boolean anyTravelingOrQueued(Campaign campaign, List<? extends ILocation> items) {
        return items.stream().anyMatch(item -> LocationUtils.isInTransit(item)
              || campaign.getCampaignLocationManager().isQueuedForTravel(item));
    }
}
