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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nonnull;
import mekhq.MekHQ;
import mekhq.campaign.events.LocationChangedEvent;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.IPlace;
import mekhq.campaign.location.LocationDispatch;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.market.contractMarket.ContractAutomation;
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

    /**
     * Temporary {@link IPlace} facade exposing only the force-level operations {@link ForceLocationManager} needs
     * from the {@link Campaign} that currently doubles as the player's main force.
     *
     * <p><strong>Remove this class once {@link Campaign} is properly split into a standalone {@code Force} class.
     * </strong> Right now {@code Force} is just a thin wrapper around {@link Campaign}: it has no {@link LocationNode}
     * of its own and simply borrows the wrapped campaign's node and resources. Because of that,
     * {@code getCurrentLocation()}, {@code setParent(...)} and {@code getChildLocations()} are inherited from the
     * {@link IPlace}/{@link ILocation} defaults operating on that shared node, and only the node and the owned
     * resources are overridden here.</p>
     *
     * <p>Anything at the whole-campaign level (campaign options, the universe, the shared location collection) is
     * intentionally <em>not</em> exposed here and is passed in as a {@link Campaign} argument instead. Keeping the
     * surface this small documents exactly which operations belong to the force and keeps the eventual split
     * mechanical.</p>
     */
    private static final class Force implements IPlace {
        private final Campaign campaign;

        Force(Campaign campaign) {
            this.campaign = campaign;
        }

        @Override
        @Nonnull
        public LocationNode getLocationNode() {
            return campaign.getLocationNode();
        }

        @Override
        @Nonnull
        public Hangar getHangar() {
            return campaign.getHangar();
        }

        @Override
        @Nonnull
        public Warehouse getWarehouse() {
            return campaign.getWarehouse();
        }

        @Override
        @Nonnull
        public Personnel getPersonnel() {
            return campaign.getMainForcePersonnel();
        }

        List<UUID> getAutomatedMothballUnits() {
            return campaign.getAutomatedMothballUnits();
        }

        void performAutomatedActivation() {
            ContractAutomation.performAutomatedActivation(campaign);
        }
    }

    private final Force mainForce;
    private final LocationNode locationNode;

    public ForceLocationManager(Campaign campaign) {
        this.mainForce = new Force(campaign);
        this.locationNode = new LocationNode(campaign);
    }

    @Nonnull
    public LocationNode getLocationNode() {
        return locationNode;
    }

    /**
     * Updates the campaign's position in the location tree to {@code location}.
     *
     * <p>If {@code location} is not yet tracked by the campaign's location collection, it is added. The previous
     * location is removed from the collection when it no longer has any children.</p>
     */
    public void setLocation(CampaignLocationManager locationManager, AbstractLocation location) {
        AbstractLocation old = mainForce.getCurrentLocation();
        if (location != null && !locationManager.getLocations().contains(location)) {
            locationManager.addLocation(location);
        }
        mainForce.setParent(location);
        if (old != null && old != location && old.getChildLocations().isEmpty()) {
            locationManager.removeLocation(old);
        }
    }

    /**
     * Processes {@link CurrentLocation} travel nodes that are parented directly to the campaign and have completed
     * their journey (i.e. are on-planet), landing their passengers into the campaign's main force resources.
     */
    public void processArrivals(Campaign campaign) {
        for (ILocation child : new ArrayList<>(mainForce.getChildLocations())) {
            if (!(child instanceof CurrentLocation travelLocation)) {
                continue;
            }
            if (!travelLocation.isOnPlanet()) {
                continue;
            }
            LocationDispatch.landFromTravelNode(travelLocation,
                  mainForce.getPersonnel(),
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
    public void moveToPlanetarySystem(Campaign campaign, PlanetarySystem planetarySystem) {
        setLocation(campaign.getCampaignLocationManager(), new CurrentLocation(planetarySystem, 0.0));
        MekHQ.triggerEvent(new LocationChangedEvent(mainForce.getCurrentLocation(), false));

        if (mainForce.getAutomatedMothballUnits().isEmpty()) {
            mainForce.performAutomatedActivation();
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
