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
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.DateTime;

import mekhq.MekHQ;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;
import mekhq.online.events.WaitingToAdvanceDayEvent;

/**
 * Manages the timeline of a {@link Campaign}.
 */
public class CampaignController {
    private final Campaign localCampaign;
    private final ConcurrentHashMap<UUID, RemoteCampaign> remoteCampaigns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> activeCampaigns = new ConcurrentHashMap<>();

    private boolean isHost;
    private UUID host;
    private DateTime hostDate;
    private String hostName;
    private PlanetarySystem hostLocation;
    private boolean hostIsGM;

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

    public String getHostName() {
        return hostName;
    }

	public void setHostLocation(String planetarySystemId) {
        hostLocation = Systems.getInstance().getSystemById(planetarySystemId);
    }

    public PlanetarySystem getHostLocation() {
        return hostLocation;
    }

	public void setHostIsGMMode(boolean isGMMode) {
        hostIsGM = isGMMode;
    }

    public boolean getHostIsGMMode() {
        return hostIsGM;
    }

	public void addRemoteCampaign(UUID id, String name, DateTime date, String locationId, boolean isGMMode) {
        PlanetarySystem planetarySystem = Systems.getInstance().getSystemById(locationId);
        remoteCampaigns.put(id, new RemoteCampaign(id, name, date, planetarySystem, isGMMode));
    }

	public Collection<RemoteCampaign> getRemoteCampaigns() {
		return remoteCampaigns.values();
    }

	public void setRemoteCampaignDate(UUID campaignId, DateTime campaignDate) {
        // We only update this if the remote campaign is actually present
        // otherwise the next PING-PONG will catch us up.
        remoteCampaigns.computeIfPresent(campaignId, (key, remoteCampaign) -> remoteCampaign.withDate(campaignDate));
    }

    public void setRemoteCampaignLocation(UUID campaignId, String locationId) {
        PlanetarySystem system = Systems.getInstance().getSystemById(locationId);

        // We only update this if the remote campaign is actually present
        // otherwise the next PING-PONG will catch us up.
        remoteCampaigns.computeIfPresent(campaignId, (key, remoteCampaign) -> remoteCampaign.withLocation(system));
    }

    public void setRemoteCampaignGMMode(UUID campaignId, boolean isGMMode) {
        // We only update this if the remote campaign is actually present
        // otherwise the next PING-PONG will catch us up.
        remoteCampaigns.computeIfPresent(campaignId, (key, remoteCampaign) -> remoteCampaign.withGMMode(isGMMode));
    }

    public void addActiveCampaign(UUID id) {
        activeCampaigns.put(id, id);
    }

	public void removeActiveCampaign(UUID id) {
        activeCampaigns.remove(id);
    }

    public boolean isCampaignActive(UUID id) {
        return Objects.equals(getHost(), id) || activeCampaigns.containsKey(id);
    }

    public Set<UUID> getActiveCampaigns() {
        return Collections.unmodifiableSet(activeCampaigns.keySet());
    }

    /**
     * Advances the local {@link Campaign} to the next day.
     */
    public boolean advanceDay() {
        if (isHost) {
            return getLocalCampaign().newDay();
        } else {
            if (getLocalCampaign().getDateTime().isBefore(getHostDate())) {
                return getLocalCampaign().newDay();
            }
            else {
                MekHQ.triggerEvent(new WaitingToAdvanceDayEvent());
                return false;
            }
        }
    }
}
