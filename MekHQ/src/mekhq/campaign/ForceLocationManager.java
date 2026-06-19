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

import mekhq.MekHQ;
import mekhq.campaign.events.LocationChangedEvent;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.LocationDispatch;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.Inoculations;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Manages the campaign's own position in the {@link LocationNode} tree — the local location of the main force.
 *
 * <p>This class owns the {@link LocationNode} that represents {@link Campaign} itself in the location hierarchy, and
 * provides methods for moving the main force to a new location, processing arrivals at the campaign's position, and
 * serializing the node's children.</p>
 */
public class ForceLocationManager {

    private final Campaign campaign;
    /**
     * Splitting from {@code campaign} to seamlessly support future refactor of {@link Campaign} into two classes
     */
    private final Campaign mainForce;
    private final LocationNode locationNode;

    public ForceLocationManager(Campaign campaign) {
        this.campaign = campaign;
        this.mainForce = campaign;
        this.locationNode = new LocationNode(campaign);
    }

    public LocationNode getLocationNode() {
        return locationNode;
    }

    /**
     * Updates the campaign's position in the location tree to {@code location}.
     *
     * <p>If {@code location} is not yet tracked by the campaign's location collection, it is added. The previous
     * location is removed from the collection when it no longer has any children.</p>
     */
    public void setLocation(AbstractLocation location) {
        AbstractLocation old = mainForce.getCurrentLocation();
        if (location != null && !campaign.getCampaignLocationManager().getLocations().contains(location)) {
            campaign.addLocation(location);
        }
        mainForce.setParent(location);
        if (old != null && old != location && old.getChildLocations().isEmpty()) {
            campaign.removeLocation(old);
        }
    }

    /**
     * Processes {@link CurrentLocation} travel nodes that are parented directly to the campaign and have completed
     * their journey (i.e. are on-planet), landing their passengers into the campaign's main force resources.
     */
    public void processArrivals() {
        for (ILocation child : new ArrayList<>(campaign.getChildLocations())) {
            if (!(child instanceof CurrentLocation travelLocation)) {
                continue;
            }
            if (!travelLocation.isOnPlanet()) {
                continue;
            }
            LocationDispatch.landFromTravelNode(travelLocation,
                  mainForce.getMainForcePersonnel(),
                  mainForce.getHangar(),
                  mainForce.getWarehouse(),
                  campaign);
        }
    }

    /**
     * Relocates the campaign immediately to the specified {@link PlanetarySystem}, updating the current location and
     * firing any associated events or automated behaviors.
     *
     * <p>This method performs the following actions:</p>
     * <ul>
     *     <li>Updates the campaign's {@link CurrentLocation} to the given planetary system.</li>
     *     <li>Triggers a {@link LocationChangedEvent} to notify listeners of the move.</li>
     *     <li>If there are no units in automated mothball mode, performs automated activation.</li>
     *     <li>If enabled by campaign options, checks for possible inoculation prompts related to the Random Diseases
     *     and Alternative Advanced Medical systems.</li>
     * </ul>
     *
     * @param planetarySystem the destination {@link PlanetarySystem} to move the campaign to
     */
    public void moveToPlanetarySystem(PlanetarySystem planetarySystem) {
        setLocation(new CurrentLocation(planetarySystem, 0.0));
        MekHQ.triggerEvent(new LocationChangedEvent(mainForce.getCurrentLocation(), false));

        if (mainForce.getAutomatedMothballUnits().isEmpty()) {
            performAutomatedActivation(mainForce);
        }

        if (campaign.getCampaignOptions().isUseRandomDiseases()
                  && campaign.getCampaignOptions().isUseAlternativeAdvancedMedical()) {
            Inoculations.triggerInoculationPrompt(campaign, false);
        }
    }

    /**
     * Writes the {@code <location>} block for the campaign's current location and the
     * {@code <locationNodeChildren>} block for Campaign's direct children in the location tree.
     */
    public void writeToXML(PrintWriter pw, int indent) {
        AbstractLocation currentLocation = locationNode.getNearestAbstractLocation();
        if (currentLocation != null) {
            currentLocation.writeToXML(pw, indent);
        }
        locationNode.writeToXML(pw, indent);
    }
}
