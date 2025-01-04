package mekhq.campaign.unit;

import megamek.common.Transporter;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

import java.util.*;

public abstract class AbstractTransportDetail implements ITransportDetail {
    protected final MMLogger logger = MMLogger.create(this.getClass());

    protected Unit transport;
    private Set<Unit> transportedUnits = new HashSet<>();
    private Map<Class<? extends Transporter>, Double> transportCapacity = new HashMap<>();

    AbstractTransportDetail(Unit transport) {
        this.transport = transport;
        if (transport.getEntity() != null) {
            initializeTransportCapacity(transport.getEntity().getTransports());
        }
    }



    /**
     * Recalculates transport capacity
     * @param transporters What transporters are we tracking the details of?
     */
    @Override
    public void initializeTransportCapacity(Vector<Transporter> transporters) {
        transportCapacity.clear();
        if (transporters != null && !transporters.isEmpty()) {
            for (Transporter transporter : transporters) {
                if (transportCapacity.containsKey(transporter.getClass())) {
                    transportCapacity.replace(transporter.getClass(), transportCapacity.get(transporter.getClass()) + transporter.getUnused());
                }
                else {
                    transportCapacity.put(transporter.getClass(), transporter.getUnused());
                }
            }

            initializeTransportDetail();
        }
    }

    protected abstract void initializeTransportDetail();

    /**
     * If this unit is capable of transporting another unit, return true
     *
     * @return true if the unit can transport another unit
     */
    @Override
    public boolean hasTransportCapacity() {
        return !transportCapacity.isEmpty();
    }

    /**
     * Gets the different kinds of transporters the transport has
     *
     * @return Set of Transporter classes
     */
    @Override
    public Set<Class<? extends Transporter>> getTransportCapabilities() {
        return transportCapacity.keySet();
    }

    /**
     * Returns true if the unit has capacity left for a transporter type
     *
     * @param transporterType Does the unit have free capacity in this type?
     * @return True if the unit has capacity, false if not
     */
    @Override
    public boolean hasTransportCapacity(Class<? extends Transporter> transporterType) {
        return transportCapacity.containsKey(transporterType);
    }

    /**
     * Returns the current capacity of a transporter type
     *
     * @param transporterType What kind of transporter types are we checking?
     * @return The current capacity of the transporter, or 0
     */
    @Override
    public double getCurrentTransportCapacity(Class<? extends Transporter> transporterType) {
        return transportCapacity.getOrDefault(transporterType, 0.0);
    }

    /**
     * Sets the current transport capacity for the provided transport type
     *
     * @param transporterType What kind of transporter are we changing the capacity of?
     * @param capacity        What is the new capacity?
     */
    @Override
    public void setCurrentTransportCapacity(Class<? extends Transporter> transporterType, double capacity) {
        transportCapacity.replace(transporterType, capacity);
    }

    /**
     * Gets a value indicating whether or not this unit is
     * transporting units.
     *
     * @return true if the unit has any transported units
     */
    @Override
    public boolean hasTransportedUnits() {
        return !transportedUnits.isEmpty();
    }

    /**
     * @return the set of units being transported by this unit.
     */
    @Override
    public Set<Unit> getTransportedUnits() {
        return Collections.unmodifiableSet(transportedUnits);
    }

    /**
     * Adds a unit to our set of transported units.
     *
     * @param unit The unit being transported by this instance.
     */
    @Override
    public void addTransportedUnit(Unit unit) {
        transportedUnits.add(Objects.requireNonNull(unit));
    }

    /**
     * Removes a unit from our set of transported units.
     *
     * @param unit The unit to remove from our set of transported units.
     * @return True if the unit was removed, otherwise false.
     */
    @Override
    public boolean removeTransportedUnit(Unit unit) {
        return transportedUnits.remove(unit);
    }

    /**
     * Clears the set of units being transported by this unit.
     */
    @Override
    public void clearTransportedUnits() {
        if (!transportedUnits.isEmpty()) {
            transportedUnits.clear();
        }
    }

    /**
     * When fixing references we need to replace the transported units
     * @param newTransportedUnits The units that should be transported
     */
    @Override
    public void replaceTransportedUnits(Set<Unit> newTransportedUnits) {
        clearTransportedUnits();
        for (Unit newUnit : newTransportedUnits) {
            addTransportedUnit(newUnit);
        }
    }
}
