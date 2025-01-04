package mekhq.campaign.unit;

import megamek.common.Entity;
import megamek.common.Transporter;
import megamek.common.UnitType;
import mekhq.Utilities;
import mekhq.campaign.Campaign;

import java.util.Objects;
import java.util.Vector;

public class ShipTransportDetail extends AbstractTransportDetail {

    /**
     * Initialize the transport details for a transport ship
     * @param transporters
     */
    public ShipTransportDetail(Unit transport) {
        super(transport);
    }

    @Override
    protected void initializeTransportDetail() {
        if (hasTransportedUnits()) {
            //Let's remove capacity for what we're already transporting
            for (Unit transportedUnit : getTransportedUnits()) {
                if (transportedUnit.hasTransportShipAssignment()) {
                    TransportShipAssignment transportAssignment = transportedUnit.getTransportShipAssignment();
                    if (Objects.equals(transportAssignment.getTransportShip(), transport)) {
                        Class<? extends Transporter> transporterType = transport.getEntity().getBayById(transportAssignment.getBayNumber()).getClass();
                        setCurrentTransportCapacity(transporterType,
                            getCurrentTransportCapacity(transporterType)
                                - transportedUnit.transportCapacityUsage(transporterType));
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
     * @param entity Entity of the unit we're loading
     */
    public void loadTransportShip(Vector<Unit> units) {
        for (Unit unit : units) {
            int unitType = unit.getEntity().getUnitType();
            double unitWeight;
            if (unit.getEntity().getUnitType() == UnitType.INFANTRY) {
                unitWeight = ITransportDetail.calcInfantryBayWeight(unit.getEntity());
            } else {
                unitWeight = unit.getEntity().getWeight();
            }

            int bayNumber = Utilities.selectBestBayFor(unit.getEntity(), transport.getEntity());
            addTransportedUnit(unit);
            unit.setTransportShipAssignment(new TransportShipAssignment(transport, bayNumber));

            Class<? extends Transporter> transporterType = transport.getEntity().getBayById(bayNumber).getClass();
            setCurrentTransportCapacity(transporterType,
                getCurrentTransportCapacity(transporterType) - unit.transportCapacityUsage(transporterType));
        }
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
}
