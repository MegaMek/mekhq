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

import static mekhq.campaign.market.contractMarket.ContractAutomation.performAutomatedActivation;

import java.io.PrintWriter;
import java.util.ArrayList;

import jakarta.annotation.Nonnull;
import mekhq.MekHQ;
import mekhq.campaign.events.LocationChangedEvent;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationDispatch;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.Inoculations;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Manages a {@link Detachment}'s position in the {@link LocationNode} tree.
 *
 * <p>This class owns the {@link LocationNode} that represents its {@link Detachment} in the location hierarchy —
 * keeping the location plumbing separate from the detachment's own state — and provides methods for moving the
 * detachment to a new location, processing arrivals at its position, and serializing the node's children.</p>
 */
public class DetachmentLocationManager {

    private final Detachment detachment;
    private final LocationNode locationNode;

    public DetachmentLocationManager(Detachment detachment) {
        this.detachment = detachment;
        this.locationNode = new LocationNode(detachment);
    }

    @Nonnull
    public LocationNode getLocationNode() {
        return locationNode;
    }

    /**
     * Updates the detachment's position in the location tree to {@code location}.
     *
     * <p>If {@code location} is not yet tracked by the campaign's location collection, it is added. The previous
     * location is removed from the collection when it no longer has any children.</p>
     */
    public void setLocation(CampaignLocationManager locationManager, AbstractLocation location) {
        AbstractLocation old = detachment.getCurrentLocation();
        if (location != null && !locationManager.getLocations().contains(location)) {
            locationManager.addLocation(location);
        }
        detachment.setParent(location);
        if (old != null && old != location && old.getChildLocations().isEmpty()) {
            locationManager.removeLocation(old);
        }
    }

    /**
     * Processes {@link AbstractMobileLocation} travel nodes (interplanetary {@link CurrentLocation} or on-planet
     * {@link GroundTransitLocation}) that are parented directly to the detachment and have completed their journey,
     * landing their passengers into the detachment's resources.
     */
    public void processArrivals(Campaign campaign) {
        for (ILocation child : new ArrayList<>(detachment.getChildLocations())) {
            if (!(child instanceof AbstractMobileLocation travelLocation)) {
                continue;
            }
            if (!travelLocation.hasArrived()) {
                continue;
            }
            LocationDispatch.landFromTravelNode(travelLocation,
                  detachment.getPersonnel(),
                  detachment.getHangar(),
                  detachment.getWarehouse(),
                  detachment,
                  campaign.getCampaignLocationManager());
        }
    }

    /**
     * Relocates the detachment immediately to the specified {@link PlanetarySystem}, updating the current location and
     * firing any associated events or automated behaviors.
     *
     * <p>This method performs the following actions:</p>
     * <ul>
     *     <li>Updates the detachment's {@link CurrentLocation} to the given planetary system.</li>
     *     <li>Triggers a {@link LocationChangedEvent} to notify listeners of the move.</li>
     *     <li>If there are no units in automated mothball mode, performs automated activation.</li>
     *     <li>If enabled by campaign options, checks for possible inoculation prompts related to the Random Diseases
     *     and Alternative Advanced Medical systems.</li>
     * </ul>
     *
     * @param planetarySystem the destination {@link PlanetarySystem} to move to
     */
    public void moveToPlanetarySystem(Campaign campaign, PlanetarySystem planetarySystem) {
        setLocation(campaign.getCampaignLocationManager(), new CurrentLocation(planetarySystem, 0.0));
        MekHQ.triggerEvent(new LocationChangedEvent(detachment.getCurrentLocation(), false));

        if (campaign.getPlayerForce().getAutomatedMothballUnits().isEmpty()) {
            performAutomatedActivation(campaign);
        }

        if (campaign.getCampaignOptions().isUseRandomDiseases()
                  && campaign.getCampaignOptions().isUseAlternativeAdvancedMedical()) {
            Inoculations.triggerInoculationPrompt(campaign, false);
        }
    }

    /**
     * Writes the {@code <location>} block for the detachment's current location and the
     * {@code <locationNodeChildren>} block for the detachment's direct children in the location tree.
     */
    public void writeToXML(PrintWriter pw, int indent) {
        AbstractLocation currentLocation = locationNode.getNearestAbstractLocation();
        if (currentLocation != null) {
            currentLocation.writeToXML(pw, indent);
        }
        locationNode.writeToXML(pw, indent);
    }
}
