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

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

/**
 * Manages the timeline of a {@link Campaign}.
 */
public class CampaignController {
    private final Campaign localCampaign;
    private final Map<UUID, RemoteCampaign> remoteCampaigns = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activeCampaigns = new ConcurrentHashMap<>();
    private boolean isHost;
    private UUID host;
    private DateTime hostDate;
    private String hostName;
    private PlanetarySystem hostLocation;

    /**
     * Creates a new {@code CampaignController} for
     * the given {@link Campaign}
     * @param c The {@link Campaign} being used locally.
     */
    public CampaignController(Campaign c) {
        localCampaign = c;
    }

    /**
     * Gets the local {@link Campaign}.
     * @return The local {@link Campaign}.
     */
    public Campaign getLocalCampaign() {
        return localCampaign;
    }

    /**
     * Gets the unique identifier of the campaign hosting
     * this session.
     * @return The unique identifier of the host campaign.
     */
    public UUID getHost() {
        return host;
    }

    /**
     * Sets the unique identifier of the campaign hosting
     * this session.
     * @param id The unique identifier of the host compaign.
     */
    public void setHost(UUID id) {
        host = id;
        isHost = getLocalCampaign().getId().equals(id);
    }

    /**
     * Gets a value indicating whether or not the local Campaign
     * is hosting this session.
     * @return {@code true} if the local campaign is hosting
     *         this session, otherwise {@code false}.
     */
    public boolean isHost() {
        return isHost;
    }

	public void setHostDate(DateTime date) {
        hostDate = date;
    }

    public DateTime getHostDate() {
        return hostDate;
    }

    public void setHostName(String name) {
        hostName = name;
    }

	public void setHostLocation(String planetarySystemId) {
        hostLocation = Systems.getInstance().getSystemById(planetarySystemId);
	}

	public void addRemoteCampaign(UUID id, String name, DateTime date, String locationId) {
        PlanetarySystem planetarySystem = Systems.getInstance().getSystemById(locationId);
        remoteCampaigns.put(id, new RemoteCampaign(id, name, date, planetarySystem));
	}

	public Collection<RemoteCampaign> getRemoteCampaigns() {
		return remoteCampaigns.values();
    }

    public void addActiveCampaign(UUID id) {
        activeCampaigns.put(id, id);
    }

	public void removeActiveCampaign(UUID id) {
        activeCampaigns.remove(id);
    }

    /**
     * Advances the local {@link Campaign} to the next day.
     */
    public void advanceDay() {
        if (isHost) {
            if (getLocalCampaign().newDay()) {
                // TODO: notifyDayChangedEvent();
            }
        } else {
            if (getLocalCampaign().getDateTime().isBefore(getHostDate())) {
                if (getLocalCampaign().newDay()) {
                    // TODO: notifyDayChangedEvent
                }
            }
            else {
                // TODO: requestNewDay();
            }
        }
    }
}
