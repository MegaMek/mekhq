package mekhq.campaign.unit;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.enums.CampaignTransportType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TacticalTransportedUnitsSummary extends AbstractTransportedUnitsSummary {

    public TacticalTransportedUnitsSummary(Unit transport) {
        super(transport);
    }

    /**
     * Main method to be used for loading units onto a transport
     *
     * @param transportedUnits Units we wish to load
     * @return the old transports the transportedUnits were assigned to, or an empty set
     */
    @Override
    public Set<Unit> loadTransport(Unit... transportedUnits) {
        return super.loadTransport(transportedUnits);
    }

    /**
     * Main method to be used for unloading units from a transport
     *
     * @param transportedUnits Units we wish to unload
     */
    @Override
    public void unloadTransport(Unit... transportedUnits) {
        super.unloadTransport(transportedUnits);

    }

    @Override
    protected void unloadTransport(Unit transportedUnit) {
        super.unloadTransport(transportedUnit);

        // And if the unit is being transported by us,
        // then update its transport  assignment (provided the
        // assignment is actually to us!).
        if (transportedUnit.hasTacticalTransportAssignment()
            && transportedUnit.getTacticalTransportAssignment().getTransport().equals(transport)) {
            transportedUnit.setTacticalTransportAssignment(null);
        }
    }

    /**
     * Transporter loading utility used when assigning units to transport units
     * For each passed-in unit, this will assign the unit to the type of
     * Transporter if one isn't provided. Once in the MM lobby,
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param transporterType type (Class) of bay or Transporter
     * @param units units being loaded
     * @return old transports; what were  the units' previous transport, if they had one
     */
    public Set<Unit> loadTransport(Class<? extends Transporter> transporterType, Unit... units) {
        return loadTransport(units, null, transporterType);
    }


    /**
     * Transporter loading utility used when assigning units to transport units
     * For each passed-in unit, this will assign the unit to the specified bay,
     * or the type of Transporter if one isn't provided. Once in the MM lobby,
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param units units being loaded
     * @param transportedLocation specific bay (Transporter), or null
     * @param transporterType type (Class) of bay or Transporter
     * @return old transports; what were  the units' previous transport, if they had one
     */
    public Set<Unit> loadTransport(Unit[] units, @Nullable Transporter transportedLocation, Class<? extends Transporter> transporterType) {
        Set<Unit> oldTransports = new HashSet<>();
        //Set<Entity> oldTransportedEntities = clearTransportedEntities();
        for (Unit transportedUnit : units) {
            Unit oldTransport = loadTransport(transportedLocation, transporterType, transportedUnit);
            if (oldTransport != null) {
                oldTransports.add(oldTransport);
            }
        }
        transport.initializeTacticalTransportSpace();
        return oldTransports;
    }


    /**
     * Transporter loading utility used when assigning units to transport units
     * For the passed in unit, this will assign the unit to the specified bay,
     * or the type of Transporter if one isn't provided. Once in the MM lobby,
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param transportedLocation specific bay, or null
     * @param transporterType type (Class) of bay or Transporter
     * @param transportedUnit unit being loaded
     * @return old transport; what was the unit's previous transport, if it had one
     */
    public Unit loadTransport(@Nullable Transporter transportedLocation, Class<? extends Transporter> transporterType, Unit transportedUnit) {
        Unit oldTransport = null;
        Class<? extends Transporter> oldTransporterType = null;

        if (transportedUnit.hasTacticalTransportAssignment()) {
            oldTransport = transportedUnit.getTacticalTransportAssignment().getTransport();
            oldTransporterType = transportedUnit.getTacticalTransportAssignment().getTransporterType();
            if (oldTransport.getEntity() != null) {
                oldTransport.unloadFromTacticalTransport(transportedUnit);
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

        //if ((transportedUnit.getEntity() != null)) {
        //    if (transport.getEntity() != null) {
        //        loadEntity(transportedUnit.getEntity());
        //    }
        //}

        // If the old transport was TODO
        if (!Objects.equals(oldTransport, transport)
            && (transportedUnit.getTacticalTransportAssignment().getTransporterType() != oldTransporterType)) {
            setCurrentTransportCapacity(transporterType,
                getCurrentTransportCapacity(transporterType) - CampaignTransportType.transportCapacityUsage(transporterType,transportedUnit.getEntity()));
        }
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
        unloadTransport(transportedUnit);
    }

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit
     * This removes all units assigned to the transport from it
     *
     * @param campaign used to remove this unit as a transport from any other units in the campaign
     */
    @Override
    public void clearTransportedUnits(Campaign campaign) {
        clearTransportedUnits();

        // And now reset the Transported values for all the units we just booted
        campaign.getHangar().forEachUnit(u -> {
            if (u.hasTacticalTransportAssignment()
                && Objects.equals(transport, u.getTacticalTransportAssignment().getTransport())) {
                u.setTacticalTransportAssignment(null);
            }
        });

        initializeTransportCapacity(transport.getEntity().getTransports());
    }

    /**
     * Fixes references after loading
     */
    @Override
    public void fixReferences(Campaign campaign, Unit unit) {
        Set<Unit> oldTransportedUnits = new HashSet<>(getTransportedUnits());
        clearTransportedUnits();
        for (Unit tacticalTransportedUnit : oldTransportedUnits) {
            if (tacticalTransportedUnit instanceof Unit.UnitRef) {
                Unit realUnit = campaign.getHangar().getUnit(tacticalTransportedUnit.getId());
                if (realUnit != null) {
                    if (realUnit.hasTacticalTransportAssignment()) {
                        loadTransport(realUnit.getTacticalTransportAssignment().getTransporterType(), realUnit);
                    } else {
                        logger.error(
                            String.format("Unit %s ('%s') references tactical transported unit %s which has no transport assignment",
                                unit.getId(), unit.getName(), tacticalTransportedUnit.getId()));
                    }
                } else {
                    logger.error(
                        String.format("Unit %s ('%s') references missing tactical transported unit %s",
                            unit.getId(), unit.getName(), tacticalTransportedUnit.getId()));
                }
            } else {
                loadTransport(tacticalTransportedUnit.getTacticalTransportAssignment().getTransporterType(), tacticalTransportedUnit);
            }
        }
    }
}
