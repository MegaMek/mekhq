/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.AbstractTransportedUnitsSummary;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.TransporterType;

/**
 * It is time-consuming to determine what transporter types we can load a unit into when in a popup menu. This class is
 * for keeping just enough information that we can quickly determine which transporters can fit a unit.
 *
 * @see CampaignTransportType
 */
public class CampaignTransporterMap {
    private static final MMLogger logger = MMLogger.create(CampaignTransporterMap.class);

    private final Campaign campaign;
    private final Map<TransporterType, Map<Double, Set<UUID>>> transportersMap = new HashMap<>();
    private CampaignTransportType campaignTransportType;

    public CampaignTransporterMap(Campaign campaign, CampaignTransportType campaignTransportType) {
        this.campaign = campaign;
        this.campaignTransportType = campaignTransportType;
    }

    /**
     * Adds an entry to the list of transporters . We'll use this to assign units later
     *
     * @param transport - The unit we want to add to this Map
     */
    public void addTransporter(Unit transport) {
        for (TransporterType transporterType : transport.getTransportedUnitsSummary(campaignTransportType)
                                                     .getTransportCapabilities()) {
            addTransporterToCapacityMap(transport, transporterType);
        }
    }

    private void addTransporterToCapacityMap(Unit transport, TransporterType transporterType) {
        double capacity = transport.getTransportedUnitsSummary(campaignTransportType)
                                .getCurrentTransportCapacity(transporterType);
        Map<Double, Set<UUID>> capacityMap = transportersMap.getOrDefault(transporterType, new HashMap<>());
        Set<UUID> unitIds = capacityMap.getOrDefault(capacity, new HashSet<>());
        unitIds.add(transport.getId());
        capacityMap.put(capacity, unitIds);
        transportersMap.put(transporterType, capacityMap);
    }

    /**
     * This will update the transport in the transport capacity map with new capacities
     *
     * @param transport Unit to get update our stored capacity
     */
    public void updateTransportInTransporterMap(Unit transport) {
        // If this unit is mothballed, let's remove it from the map and return.
        if (transport.isMothballed()) {
            removeTransport(transport);
            return;
        }
        AbstractTransportedUnitsSummary transportedUnitsSummary = transport.getTransportedUnitsSummary(
              campaignTransportType);

        // Let's make a list of all the transportTypes in the map, and all the transportTypes the unit has
        Set<TransporterType> transporterTypes = new HashSet<>();
        transporterTypes.addAll(transportedUnitsSummary.getTransportCapabilities());
        transporterTypes.addAll(transportersMap.keySet());

        // Now let's update the current transporterTypes
        for (TransporterType transporterType : transporterTypes) {
            if (transportersMap.containsKey(transporterType)) {
                Set<Double> oldCapacities = transportersMap.get(transporterType).keySet();
                Double newCapacity = transportedUnitsSummary.getCurrentTransportCapacity(transporterType);
                // First, if this is a new capacity for the map, let's manually add it
                if (!oldCapacities.contains(newCapacity)) {
                    addTransporterToCapacityMap(transport, transporterType);
                }
                //Then we iterate through the existing capacities in the map, and either remove or add this transport as needed
                for (Double capacity : oldCapacities) {
                    if (transportersMap.get(transporterType).get(capacity).contains(transport.getId())) {
                        if (!Objects.equals(capacity, newCapacity)) { // If it's correct, we don't need to change it
                            transportersMap.get(transporterType).get(capacity).remove(transport.getId());
                        }
                    } else if (Objects.equals(capacity, newCapacity)) {
                        addTransporterToCapacityMap(transport, transporterType);
                    }
                }

                // Finally, let's remove this from the map & get out if the transport doesn't have this transporterType
                if (!transportedUnitsSummary.getTransportCapabilities().contains(transporterType)) {
                    removeTransportFromCapacityMap(transport, transporterType, 0);
                }
            } else {
                addTransporterToCapacityMap(transport, transporterType);
            }
        }
    }

    public boolean hasTransporters() {
        return !transportersMap.isEmpty();
    }

    /**
     * true if this transport map contains the unit, false if not
     *
     * @param unit is in this transport map as a UUID?
     *
     * @return true if the unit is, false if not
     */
    public boolean hasTransport(Unit unit) {
        for (TransporterType transporterType : transportersMap.keySet()) {
            for (Double capacity : transportersMap.get(transporterType).keySet()) {
                if (transportersMap.get(transporterType).get(capacity).contains(unit.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a Map that maps Transporter types to another Map that maps capacity (Double) to UUID of transports
     *
     * @return units that have space for that transport type
     */
    public Map<TransporterType, Map<Double, Set<UUID>>> getTransporters() {
        return Collections.unmodifiableMap(transportersMap);
    }

    /**
     * Returns a list of transports that can transport a unit of given size. For example, getTransportsByType(MEK_BAY,
     * 3.0) would return all transports that have 3 or more Mek Bay slots open.
     *
     * @param transporterType class of Transporter
     * @param unitSize        the size of the unit (usually 1)
     *
     * @return units that have space for that transport type
     */
    public Set<Unit> getTransportsByType(TransporterType transporterType, double unitSize) {
        Set<Unit> units = new HashSet<>();
        Map<Double, Set<UUID>> capacityMap = getTransporters().get(transporterType);
        for (Double capacity : capacityMap.keySet()) {
            if (Double.compare(capacity, unitSize) >= 0) {
                for (UUID uuid : capacityMap.get(capacity)) {
                    units.add(campaign.getUnit(uuid));
                }
            }
        }
        return units;
    }

    /**
     * Deletes an entry from the list of transit-capable transport ships. This gets updated when the unit is removed
     * from the campaign for one reason or another
     *
     * @param transport - The unit we want to remove from this Set
     */
    public void removeTransport(Unit transport) {
        Map<TransporterType, Double> toRemoveMap = new HashMap<>();
        for (TransporterType transporterType : transportersMap.keySet()) {
            for (Double capacity : transportersMap.get(transporterType).keySet()) {
                if (transportersMap.get(transporterType).get(capacity).contains(transport.getId())) {
                    toRemoveMap.put(transporterType, capacity);
                }
            }
        }

        for (TransporterType transporterTypeToRemove : toRemoveMap.keySet()) {
            double capacity = toRemoveMap.get(transporterTypeToRemove);
            removeTransportFromCapacityMap(transport, transporterTypeToRemove, capacity);
        }
    }

    private void removeTransportFromCapacityMap(Unit transport, TransporterType transporterTypeToRemove,
          double capacity) {

        transportersMap.get(transporterTypeToRemove).get(capacity).remove(transport.getId());
        if (transportersMap.get(transporterTypeToRemove).get(capacity).isEmpty()) {
            transportersMap.get(transporterTypeToRemove).remove(capacity);
        }
        if (transportersMap.get(transporterTypeToRemove).isEmpty()) {
            transportersMap.remove(transporterTypeToRemove);
        }
    }
}
