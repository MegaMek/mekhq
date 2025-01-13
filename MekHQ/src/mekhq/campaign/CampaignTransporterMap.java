/**
 * Copyright (c) 2025-2025 The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;

import megamek.common.Transporter;
import megamek.logging.MMLogger;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.AbstractTransportedUnitsSummary;
import mekhq.campaign.unit.Unit;

import java.util.*;

/**
 * It is time consuming to determine what transporter types we can load a unit into when
 * in a popup menu. This class is for keeping just enough information in memory that we
 * can quickly determine which transporters can fit a unit.
 *
 * @see CampaignTransportType
 */
public class CampaignTransporterMap {
    private static final MMLogger logger = MMLogger.create(CampaignTransporterMap.class);

    private final Campaign campaign;
    private final Map<Class<? extends Transporter>, Map<Double, Set<UUID>>> transportersMap = new HashMap<>();
    private CampaignTransportType campaignTransportType;

    public CampaignTransporterMap(Campaign campaign, CampaignTransportType campaignTransportType){
        this.campaign = campaign;
        this.campaignTransportType = campaignTransportType;
    }

    /**
     * Adds an entry to the list of transporters . We'll use this
     * to assign units later
     *
     * @param transport - The unit we want to add to this Map
     */
    public void addTransporter(Unit transport) {
        for (Class<? extends Transporter> transporterType : transport.getTransportedUnitsSummary(campaignTransportType).getTransportCapabilities()) {
            addTransporterToCapacityMap(transport, transporterType);
        }
    }

    private void addTransporterToCapacityMap(Unit transport, Class<? extends Transporter> transporterType) {
        double capacity = transport.getTransportedUnitsSummary(campaignTransportType).getCurrentTransportCapacity(transporterType);
        Map<Double, Set<UUID>> capacityMap = transportersMap.getOrDefault(transporterType, new HashMap<>());
        Set<UUID> unitIds = capacityMap.getOrDefault(capacity, new HashSet<>());
        unitIds.add(transport.getId());
        capacityMap.put(capacity, unitIds);
        transportersMap.put(transporterType, capacityMap);
    }

    /**
     * This will update the transport in the transport capacity
     * map with new capacities
     * @param transport Unit to get update our stored capacity
     */
    public void updateTransportInTransporterMap(Unit transport) {
        AbstractTransportedUnitsSummary transportedUnitsSummary = transport.getTransportedUnitsSummary(campaignTransportType);
        for (Class<? extends Transporter> transporterType : transportedUnitsSummary.getTransportCapabilities()) {
            if (transportersMap.containsKey(transporterType)) {
                Set<Double> oldCapacities = transportersMap.get(transporterType).keySet();
                Double newCapacity = transportedUnitsSummary.getCurrentTransportCapacity(transporterType);
                //First, if this is a new capacity for the map, let's manually add it
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
            }
            else {
                logger.error(String.format("Invalid transporter type %s", transporterType));
            }
        }
    }

    public boolean hasTransporters() {
        return !transportersMap.isEmpty();
    }

    /**
     * true if this transport map contians the unit, false if not
     * @param unit is in this transport map as a UUID?
     * @return true if the unit is, false if not
     */
    public boolean hasTransport(Unit unit) {
        for (Class<? extends Transporter> transporterType : transportersMap.keySet()){
            for (Double capacity : transportersMap.get(transporterType).keySet()) {
                if (transportersMap.get(transporterType).get(capacity).contains(unit.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a Map that maps Transporter types to another
     * Map that maps capacity (Double) to UUID of transports
     *
     * @return units that have space for that transport type
     */
    public Map<Class<? extends Transporter>, Map<Double, Set<UUID>>> getTransporters() {
        return Collections.unmodifiableMap(transportersMap);
    }

    /**
     * Returns list of transports that can transport a unit of given size
     *
     * @param transporterType class of Transporter
     * @param unitSize the size of the unit (usually 1)
     * @return units that have space for that transport type
     */
    public Set<Unit> getTransportsByType(Class<? extends Transporter> transporterType, double unitSize) {
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
     * Deletes an entry from the list of transit-capable transport ships. This gets
     * updated when
     * the unit is removed from the campaign for one reason or another
     *
     * @param transport - The unit we want to remove from this Set
     */
    public void removeTransport(Unit transport) {
        for ( Class<? extends Transporter> transporterType : transportersMap.keySet()) {
            for (Double capacity : transportersMap.get(transporterType).keySet()) {
                transportersMap.get(transporterType).get(capacity).remove(transport.getId());
                if (transportersMap.get(transporterType).get(capacity).isEmpty()) {
                    transportersMap.get(transporterType).remove(capacity);
                }
            }
            if (transportersMap.get(transporterType).isEmpty()) {
                transportersMap.remove(transporterType);
            }
        }

        // If we remove a transport ship from the campaign,
        // we need to remove any transported units from it
        if (transport.getTransportedUnitsSummary(campaignTransportType).hasTransportedUnits()) {
            List<Unit> transportedUnits = new ArrayList<>(transport.getTransportedUnitsSummary(campaignTransportType).getTransportedUnits());
            for (Unit transportedUnit : transportedUnits) {
                transport.getTransportedUnitsSummary(campaignTransportType).removeTransportedUnit(transportedUnit);
            }
        }
    }
}
