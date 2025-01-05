package mekhq.campaign.unit;

import megamek.common.Transporter;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TacticalTransportDetail extends AbstractTransportDetail {

    public TacticalTransportDetail(Unit transport) {
        super(transport);
    }

    @Override
    protected void initializeTransportDetail() {
        if (hasTransportedUnits() && transport.getEntity() != null) {
            //Let's remove capacity for what we're already transporting
            for (Unit transportedUnit : getTransportedUnits()) {
                if (transportedUnit.hasTransportAssignment()) {
                    ITransportAssignment transportAssignment = transportedUnit.getTacticalTransportAssignment();
                    if (Objects.equals(transportAssignment.getTransport(), transport)) {
                        setCurrentTransportCapacity(transportAssignment.getTransporterType(),
                            getCurrentTransportCapacity(transportAssignment.getTransporterType())
                                - transportedUnit.transportCapacityUsage(transportAssignment.getTransporterType()));
                    }
                }

            }
        }
    }

    /** TODO comment fixing
     * Bay loading utility used when assigning units to bay-equipped transport units
     * For each passed-in unit, this will find the first available, transport bay
     * and set
     * both the target bay and the UUID of the transport ship. Once in the MM lobby,
     * this data
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param units Vector of units that we wish to load into this transport
     * @return the old transports of the units, or an empty set if none
     */
    public Set<Unit> loadTransport(Class<? extends Transporter> transporterType, Unit... units) {
        return loadTransport(units, null, transporterType);
    }

    /** TODO comment fixing
     * Bay loading utility used when assigning units to bay-equipped transport units
     * For each passed-in unit, this will find the first available, transport bay
     * and set
     * both the target bay and the UUID of the transport ship. Once in the MM lobby,
     * this data
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param units Vector of units that we wish to load into this transport
     * @return the old transports of the units, or an empty set if none
     */
    public Set<Unit> loadTransport(Unit[] units, @Nullable Transporter transportedLocation, Class<? extends Transporter> transporterType) {
        Set<Unit> oldTransports = new HashSet<>();
        for (Unit transportedUnit : units) {
            Unit oldTransport = loadTransport(transportedUnit, transportedLocation, transporterType);
            if (oldTransport != null) {
                oldTransports.add(oldTransport);
            }
        }
        return oldTransports;
    }

    /** TODO comment fixing
     * Bay loading utility used when assigning units to bay-equipped transport units
     * For each passed-in unit, this will find the first available, transport bay
     * and set
     * both the target bay and the UUID of the transport ship. Once in the MM lobby,
     * this data
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param unit Unit we wish to load
     * @return the old transport of the unit, or an empty set if none
     */
    public Unit loadTransport(Unit transportedUnit, @Nullable Transporter transportedLocation, Class<? extends Transporter> transporterType) {
        Unit oldTransport = null;

        if (transportedUnit.hasTransportAssignment() && !Objects.equals(transportedUnit.getTacticalTransportAssignment().getTransport(), transport)) {
            oldTransport = transportedUnit.getTacticalTransportAssignment().getTransport();
            if (transport.getEntity() != null) {
                oldTransport.unloadFromTransport(transportedUnit);
            }
        }
        if (transportedLocation != null) {
            transportedUnit.setTacticalTransportAssignment(new TransportAssignment(transport, transportedLocation));
        }
        else if (transporterType != null){
            transportedUnit.setTacticalTransportAssignment(new TransportAssignment(transport, transporterType));
        } else {
            logger.error(String.format("Cannot load transport (%s) with unit (%s) without a transported location or transporter!", transport.getId(), transportedUnit.getId()));
            return oldTransport;
        }
        addTransportedUnit(Objects.requireNonNull(transportedUnit));

        setCurrentTransportCapacity(transporterType,
            getCurrentTransportCapacity(transporterType) - transportedUnit.transportCapacityUsage(transporterType));
        return oldTransport;
    }

    /**
     * Bay unloading utility used when removing units from bay-equipped transport
     * units
     * and/or moving them to a new transport
     *
     * @param transportedUnit The unit that we wish to unload from this transport
     */
    public void unloadFromTransport(Unit transportedUnit) {
        Objects.requireNonNull(transportedUnit);

        // Remove this unit from our collection of transported units.
        removeTransportedUnit(transportedUnit);
        if (transport.getEntity() != null) {
            transport.getEntity().unload(transportedUnit.getEntity());
            initializeTransportCapacity(transport.getEntity().getTransports());
        }

        // And if the unit is being transported by us,
        // then update its transport  assignment (provided the
        // assignment is actually to us!).
        if (transportedUnit.hasTransportAssignment()
            && transportedUnit.getTacticalTransportAssignment().getTransport().equals(transport)) {
            transportedUnit.setTacticalTransportAssignment(null);
        }
    }

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit
     * This removes all units assigned to the transport from it
     *
     * @param campaign used to remove this unit as a transport from any other units in the campaign
     */
    @Override
    public void unloadTransportedUnits(Campaign campaign) {
        clearTransportedUnits();

        // And now reset the Transported values for all the units we just booted
        campaign.getHangar().forEachUnit(u -> {
            if (u.hasTransportAssignment()
                && Objects.equals(transport, u.getTacticalTransportAssignment().getTransport())) {
                u.setTacticalTransportAssignment(null);
            }
        });

        initializeTransportCapacity(transport.getEntity().getTransports());
    }

    /**
     * TransportDetails are meant to be used with transportAssignment
     * @return the TransportAssignement used by the class
     */
    static Class<?> getRelatedTransportAssignmentType() {
        return TransportAssignment.class;
    }
}
