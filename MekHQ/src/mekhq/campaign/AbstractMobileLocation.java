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
package mekhq.campaign;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.IPlace;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * A {@link AbstractLocation} that can move, i.e. one that carries a remaining <em>transit time</em>
 * which counts down day-by-day until the location arrives.
 *
 * <p>This is the shared base for any travelling location. It owns only the generic "has transit
 * time" state; how that transit advances, what arrival means, and whether the location is
 * interplanetary or planetside are left to subclasses. {@link CurrentLocation} models interplanetary
 * JumpShip travel.</p>
 */
public abstract class AbstractMobileLocation extends AbstractLocation {
    /** Remaining transit time, in days. */
    protected double transitTime;

    // Populated during XML load; drained by CampaignXmlParser to reconnect ILocations after load.
    protected transient List<UUID>    pendingPersonIds = new ArrayList<>();
    protected transient List<UUID>    pendingUnitIds   = new ArrayList<>();
    protected transient List<Integer> pendingPartIds   = new ArrayList<>();

    protected AbstractMobileLocation(PlanetarySystem system, double transitTime) {
        super(system);
        this.transitTime = transitTime;
    }

    /** Returns true if {@code personId} is in the pending reconnection list (non-destructive). */
    public boolean containsPendingPersonId(UUID personId) {
        return pendingPersonIds.contains(personId);
    }

    /** Returns and clears the person UUIDs read from XML, for use during post-load reconnection. */
    public List<UUID> drainPendingPersonIds() {
        List<UUID> ids = new ArrayList<>(pendingPersonIds);
        pendingPersonIds.clear();
        return ids;
    }

    /** Returns and clears the unit UUIDs read from XML, for use during post-load reconnection. */
    public List<UUID> drainPendingUnitIds() {
        List<UUID> ids = new ArrayList<>(pendingUnitIds);
        pendingUnitIds.clear();
        return ids;
    }

    /** Returns and clears the part IDs read from XML, for use during post-load reconnection. */
    public List<Integer> drainPendingPartIds() {
        List<Integer> ids = new ArrayList<>(pendingPartIds);
        pendingPartIds.clear();
        return ids;
    }

    @Override
    public double getTransitTime() {
        return transitTime;
    }

    @Override
    public void setTransitTime(double time) {
        transitTime = time;
    }

    /**
     * @return {@code true} once this travelling location has reached its destination, i.e. no transit time remains.
     *       For an interplanetary {@link CurrentLocation} this coincides with being on-planet.
     */
    public boolean hasArrived() {
        return transitTime <= 0;
    }

    /**
     * Notifies each child {@link IPlace} that this location has arrived, so it can run its own
     * place-specific arrival behavior.
     *
     * @param campaign           the active campaign
     * @param isSilentProcessing {@code true} when processing happens without user interaction (e.g.
     *                           fast-forward), which suppresses dialog prompts
     */
    protected void notifyChildrenArrived(Campaign campaign, boolean isSilentProcessing) {
        for (ILocation child : getChildLocations()) {
            if (child instanceof IPlace place) {
                place.onArrival(campaign, isSilentProcessing);
            }
        }
    }
}
