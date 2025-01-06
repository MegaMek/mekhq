package mekhq.campaign.unit;

import megamek.common.*;
import mekhq.Utilities;
import mekhq.campaign.Campaign;

import java.util.*;

public class ShipTransportedUnitsSummary extends AbstractTransportedUnitsSummary {

    /**
     * Initialize the transport details for a transport ship
     * @param transporters
     */
    public ShipTransportedUnitsSummary(Unit transport) {
        super(transport);
    }

    @Override
    protected void initializeTransportDetail() {
        if (hasTransportedUnits() && transport.getEntity() != null) {
            //Let's remove capacity for what we're already transporting
            for (Unit transportedUnit : getTransportedUnits()) {
                if (transportedUnit.hasTransportShipAssignment()) {
                    TransportShipAssignment transportAssignment = transportedUnit.getTransportShipAssignment();
                    if (Objects.equals(transportAssignment.getTransportShip(), transport)) {
                        try {
                            Class<? extends Transporter> transporterType;
                            if (transportedUnit.getEntity() != null && transportedUnit.getEntity().getUnitType() == UnitType.DROPSHIP) {
                                transporterType = transport.getEntity().getCollarById(transportAssignment.getBayNumber()).getClass();
                            }
                            else {
                                transporterType = transport.getEntity().getBayById(transportAssignment.getBayNumber()).getClass();
                            }
                            setCurrentTransportCapacity(transporterType,
                                getCurrentTransportCapacity(transporterType)
                                    - transportedUnit.transportCapacityUsage(transporterType));
                        }
                        catch (NullPointerException e) {
                            logger.error(String.format("NullPointerException: Unable to get bays by number. Transport: %s Bay Assignment: %s)", transport.getName(), transportAssignment.getBayNumber()));
                            for (Bay bay : transport.getEntity().getTransportBays()) {
                                logger.error(String.format("Bay Number: %s Bay Type: %s", bay.getBayNumber(), bay.getClass()));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Bay loading utility used when assigning units to bay-equipped transport units
     * For each passed-in unit, this will find the first available, transport bay
     * and set
     * both the target bay and the UUID of the transport ship. Once in the MM lobby,
     * this data
     * will be used to actually load the unit into a bay on the transport.
     *
     * @param units Vector of units that we wish to load into this transport
     */
    public Set<Unit> loadTransportShip(Vector<Unit> units, Class<? extends Transporter> transporterType) {
        Set<Unit> oldTransports = new HashSet<>();
        for (Unit unit : units) {
            int bayNumber = Utilities.selectBestBayFor(unit.getEntity(), transport.getEntity());
            addTransportedUnit(unit);
            if(unit.hasTransportShipAssignment() && !Objects.equals(unit.getTransportShipAssignment().getTransportShip(), transport)) {
                oldTransports.add(unit.getTransportShipAssignment().getTransportShip());
            }
            unit.setTransportShipAssignment(new TransportShipAssignment(transport, bayNumber));

            if ((unit.getEntity() != null)) {
                // This shouldn't happen, but it'd be really annoying to debug if it did
                if ((unit.getEntity().getBayById(bayNumber) != null && unit.getEntity().getBayById(bayNumber).getClass() != transporterType)) {
                    logger.warn(String.format("Unit was assigned a bay number for a different transport type than the unit is assigned! " +
                        "Transport: %s Unit: %s Assigned Transporter: %s Assigned Bay ID: %s",
                        transport.getName(), unit.getName(), transporterType, bayNumber));
                }
            }

            setCurrentTransportCapacity(transporterType,
                getCurrentTransportCapacity(transporterType) - unit.transportCapacityUsage(transporterType));
        }

        return oldTransports;
    }

    /**
     * Bay unloading utility used when removing units from bay-equipped transport
     * units
     * and/or moving them to a new transport
     *
     * @param transportedUnit The unit that we wish to unload from this transport
     */
    public void unloadFromTransportShip(Unit transportedUnit) {
        Objects.requireNonNull(transportedUnit);

        // Remove this unit from our collection of transported units.
        removeTransportedUnit(transportedUnit);
        if (transport.getEntity() != null) {
            initializeTransportCapacity(transport.getEntity().getTransports());
        }

        // And if the unit is being transported by us,
        // then update its transport ship assignment (provided the
        // assignment is actually to us!).
        if (transportedUnit.hasTransportShipAssignment()
            && transportedUnit.getTransportShipAssignment().getTransportShip().equals(transport)) {

            transportedUnit.setTransportShipAssignment(null);
            initializeTransportCapacity(transport.getEntity().getTransports());
        }
    }

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit
     * This removes all units assigned to the transport from it
     */
    @Override
    public void unloadTransportedUnits(Campaign campaign) {
        clearTransportedUnits();

        // And now reset the Transported values for all the units we just booted
        campaign.getHangar().forEachUnit(u -> {
            if (u.hasTransportShipAssignment()
                && Objects.equals(transport, u.getTransportShipAssignment().getTransportShip())) {
                u.setTransportShipAssignment(null);
            }
        });

        initializeTransportCapacity(transport.getEntity().getTransports());
    }

    /**
     * TransportDetails are meant to be used with transportAssignment
     * @return the TransportAssignement used by the class
     */
    static Class<?> getRelatedTransportAssignmentType() {
        return TransportShipAssignment.class;
    }

    public static Set<Class<? extends Transporter>> mapEntityToTransporters(Entity unit) {
        Set<Class<? extends Transporter>> transporters = AbstractTransportedUnitsSummary.mapEntityToTransporters(unit);
        transporters.remove(InfantryCompartment.class);
        transporters.remove(BattleArmorHandles.class);
        transporters.remove(BattleArmorHandlesTank.class);
        transporters.remove(ClampMountMek.class);
        transporters.remove(ClampMountTank.class);

        return transporters;
    }

}
