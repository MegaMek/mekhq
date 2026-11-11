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
package mekhq.campaign.events;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;

/**
 * Triggered once when a bulk force generation finishes adding its formations, units, personnel and parts.
 *
 * <p>While a generation runs, {@link Campaign#isBulkGenerationInProgress()} is true and the tabs skip their
 * refreshes, so the tabs that only need to catch up afterwards (Personnel, Infirmary, Repair, Warehouse, Finances and
 * the Command Center) subscribe to this event rather than to every {@link OrganizationChangedEvent}. An ordinary
 * TO&amp;E change then no longer rebuilds all of them, which on a large campaign froze the GUI after each move.</p>
 *
 * <p>It is an {@link OrganizationChangedEvent}, so the tabs that follow the TO&amp;E (TO&amp;E, Hangar, Briefing)
 * still refresh when it fires.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class BulkGenerationCompletedEvent extends OrganizationChangedEvent {
    /**
     * @param campaign  the campaign the generation filled
     * @param formation the top-level formation; formation levels are recalculated from it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public BulkGenerationCompletedEvent(Campaign campaign, Formation formation) {
        super(campaign, formation);
    }
}
