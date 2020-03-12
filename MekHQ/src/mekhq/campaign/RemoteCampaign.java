/*
 * Copyright (c) 2020 The MegaMek Team.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import java.util.UUID;

import org.joda.time.DateTime;

import mekhq.campaign.universe.PlanetarySystem;

public class RemoteCampaign {

    private final UUID id;
    private final String name;
    private final DateTime date;
    private final PlanetarySystem location;
    private final boolean isGMMode;
    private final boolean isActive;

    public RemoteCampaign(UUID id, String name, DateTime date, PlanetarySystem location, boolean isGMMode, boolean isActive) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.location = location;
        this.isGMMode = isGMMode;
        this.isActive = isActive;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public DateTime getDate() {
		return date;
    }

    public PlanetarySystem getLocation() {
        return location;
    }

    public boolean isGMMode() {
        return isGMMode;
    }

    public boolean isActive() {
        return isActive;
    }

    public RemoteCampaign withDate(DateTime newDate) {
        return new RemoteCampaign(id, name, newDate, location, isGMMode, isActive);
    }

    public RemoteCampaign withLocation(PlanetarySystem newLocation) {
        return new RemoteCampaign(id, name, date, newLocation, isGMMode, isActive);
    }

    public RemoteCampaign withGMMode(boolean isGMMode) {
        return new RemoteCampaign(id, name, date, location, isGMMode, isActive);
    }

    public RemoteCampaign withActive(boolean isActive) {
        return new RemoteCampaign(id, name, date, location, isGMMode, isActive);
    }
}
