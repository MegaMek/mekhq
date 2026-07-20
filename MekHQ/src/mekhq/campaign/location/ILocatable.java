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
package mekhq.campaign.location;

import mekhq.campaign.CampaignLocationManager;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * An {@link ILocation} that the player can pick up and move around the location tree — a concrete thing that lives
 * somewhere, as opposed to a place that hosts other locations. Implemented by {@link Unit}, {@link Person}, and
 * {@link Part}.
 */
public interface ILocatable extends ILocation {

    /**
     * Whether the player may manually dispatch this item to a new location (via the "Send To..." context menu).
     *
     * <p>Each implementation forbids dispatch while the item is committed elsewhere — e.g. deployed to a scenario,
     * enrolled as a student, reserved for work, or still in transit from a purchase.</p>
     *
     * @return {@code true} if the item is free to be dispatched, otherwise {@code false}
     */
    boolean canBeManuallyDispatched();

    /**
     * Whether this item is currently sitting in the pending-travel queue, awaiting dispatch on the next new day.
     *
     * <p>This is the pre-dispatch state — the item is still at its origin, queued for travel but not yet moved into
     * active transit (contrast with {@link #isInTransit()}). The queue lives on the
     * {@link CampaignLocationManager} rather than in the location tree, so the manager must be supplied.</p>
     *
     * @param locationManager the campaign's location manager holding the pending-travel queue
     *
     * @return {@code true} if this item is queued for travel but not yet dispatched, otherwise {@code false}
     */
    default boolean isQueuedForTravel(CampaignLocationManager locationManager) {
        return locationManager.isQueuedForTravel(this);
    }

    /**
     * A {@link Unit}, {@link Person}, or {@link Part} is a real occupant, so a location node hosting one is always in
     * use and must not be pruned.
     */
    @Override
    default boolean isInUse() {
        return true;
    }
}
