package mekhq.campaign;

import megamek.common.Transporter;
import megamek.logging.MMLogger;
import mekhq.campaign.unit.AbstractTransportDetail;
import mekhq.campaign.unit.Unit;

import java.util.*;

public class CampaignTransporterMap {
    private static final MMLogger logger = MMLogger.create(CampaignTransporterMap.class);

    private final Campaign campaign;
    private final Map<Class<? extends Transporter>, Map<Double, Set<UUID>>> transportersMap = new HashMap<>();
    private Class<? extends AbstractTransportDetail> transportDetailType;

    public CampaignTransporterMap(Campaign campaign, Class<? extends AbstractTransportDetail> transportDetailType){
        this.campaign = campaign;
        this.transportDetailType = transportDetailType;
    }

    /**
     * Adds an entry to the list of transporters . We'll use this
     * to assign units later
     *
     * @param transport - The unit we want to add to this Map
     */
    public void addTransporter(Unit transport) {
        for (Class<? extends Transporter> transporterType : transport.getTransportDetail(transportDetailType).getTransportCapabilities()) {
            addTransporterToCapacityMap(transport, transporterType);
        }
    }

    private void addTransporterToCapacityMap(Unit transport, Class<? extends Transporter> transporterType) {
        double capacity = transport.getTransportDetail(transportDetailType).getCurrentTransportCapacity(transporterType);
        Map<Double, Set<UUID>> capacityMap = transportersMap.getOrDefault(transporterType, new HashMap<Double, Set<UUID>>());
        Set<UUID> unitIds = capacityMap.getOrDefault(capacity, new HashSet<UUID>());
        unitIds.add(transport.getId());
        capacityMap.put(capacity, unitIds);
        transportersMap.put(transporterType, capacityMap);
    }

    /**
     * This will update the transport in the transports list with new capacities
     * @param transport
     */
    public void updateTransportInTransporterMap(Unit transport) {
        AbstractTransportDetail transportDetail = transport.getTransportDetail(transportDetailType);
        for (Class<? extends Transporter> transporterType : transportDetail.getTransportCapabilities()) {
            if (transportersMap.containsKey(transporterType)) {
                for (Double capacity : transportersMap.get(transporterType).keySet()) {
                    if (transportersMap.get(transporterType).get(capacity).contains(transport.getId())) {
                        if (capacity == transportDetail.getCurrentTransportCapacity(transporterType)) {
                            break; // The transport is already stored with the correct capacity
                        } else {
                            transportersMap.get(transporterType).get(capacity).remove(transport.getId());
                        }
                    } else if (capacity == transportDetail.getCurrentTransportCapacity(transporterType)) {
                        addTransporterToCapacityMap(transport, transporterType);
                    }
                }
            }
            else {
                logger.error("Invalid transporter type: " + transporterType);
            }
        }
    }

    public boolean hasTransporters() {
        return !transportersMap.isEmpty();
    }

    /**
     * Returns list of transports
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
        for (Map<Double, Set<UUID>> capacityMap : transportersMap.values()) {
            for (Double capacity : capacityMap.keySet()) {
                capacityMap.get(capacity).remove(transport.getId());
            }
        }

        // If we remove a transport ship from the campaign,
        // we need to remove any transported units from it
        if (transport.getTransportDetail(transportDetailType).hasTransportedUnits()) {
            List<Unit> transportedUnits = new ArrayList<>(transport.getTransportDetail(transportDetailType).getTransportedUnits());
            for (Unit transportedUnit : transportedUnits) {
                transport.getTransportDetail(transportDetailType).removeTransportedUnit(transportedUnit);
            }
        }
    }
}
