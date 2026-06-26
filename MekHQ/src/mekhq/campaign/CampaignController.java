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
package mekhq.campaign;

import java.util.UUID;
import javax.swing.SwingUtilities;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.events.StoryFinishedEvent;
import mekhq.campaign.market.PersonnelMarket;

/**
 * Manages the timeline of a {@link Campaign}.
 */
public class CampaignController {
    private final MekHQ app;
    private final Campaign localCampaign;
    private boolean isHost;
    private UUID host;
    private final CampaignEventProcessor campaignEventProcessor;

    /**
     * Creates a new {@code CampaignController} for the given {@link Campaign}
     *
     * @param campaign The {@link Campaign} being used locally.
     */
    public CampaignController(MekHQ app, Campaign campaign) {
        this.app = app;
        localCampaign = campaign;
        campaignEventProcessor = new CampaignEventProcessor(campaign);
    }

    /**
     * Manually registers campaign-related event bus listeners.
     */
    public void activate() {
        PersonnelMarket personnelMarket = localCampaign.getHumanResources().getPersonnelMarket();
        if (personnelMarket != null) {
            MekHQ.registerHandler(personnelMarket);
        }
        MekHQ.registerHandler(campaignEventProcessor);
        MekHQ.registerHandler(this);
    }

    /**
     * Manually unregister campaign-related event bus listeners.
     */
    public void deactivate() {
        if (localCampaign.getStoryArc() != null) {
            MekHQ.unregisterHandler(localCampaign.getStoryArc());
        }
        PersonnelMarket personnelMarket = localCampaign.getHumanResources().getPersonnelMarket();
        if (personnelMarket != null) {
            MekHQ.unregisterHandler(personnelMarket);
        }
        MekHQ.unregisterHandler(campaignEventProcessor);
        MekHQ.unregisterHandler(this);
        CampaignNewDayManager newDayManager = localCampaign.getNewDayManager();
        if (newDayManager != null) {
            MekHQ.unregisterHandler(newDayManager);
        }
    }

    /**
     * Gets the local {@link Campaign}.
     *
     * @return The local {@link Campaign}.
     */
    public Campaign getLocalCampaign() {
        return localCampaign;
    }

    /**
     * Gets the unique identifier of the campaign hosting this session.
     *
     * @return The unique identifier of the host campaign.
     */
    public UUID getHost() {
        return host;
    }

    /**
     * Sets the unique identifier of the campaign hosting this session.
     *
     * @param id The unique identifier of the host campaign.
     */
    public void setHost(UUID id) {
        host = id;
        isHost = getLocalCampaign().getId().equals(id);
    }

    /**
     * Gets a value indicating whether the local Campaign is hosting this session.
     *
     * @return {@code true} if the local campaign is hosting this session, otherwise {@code false}.
     */
    public boolean isHost() {
        return isHost;
    }

    /**
     * Advances the local {@link Campaign} to the next day.
     */
    public void advanceDay() {
        getLocalCampaign().newDay();
    }

    @Subscribe
    public void handle(StoryFinishedEvent event) {
        // do on a different thread, because restart will trigger event bus registrations which can
        // lead to ConcurrentModificationException if done on the event bus trigger thread
        SwingUtilities.invokeLater(app::restart);
    }

}
