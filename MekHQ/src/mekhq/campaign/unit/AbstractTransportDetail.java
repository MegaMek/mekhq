package mekhq.campaign.unit;

import megamek.common.*;
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

    /**
     *
     * @see Bay and its subclass's canLoad(Entity unit) methods
     * @param entity
     * @return the transporter types that could potentially transport this entity
     */
    public static Set<Class<? extends Transporter>> mapEntityToTransporters(Entity unit) {
        Set<Class<? extends Transporter>> transporters = new HashSet<>();

        Class<? extends Entity> entityType = unit.getClass();
        if (ProtoMek.class.isAssignableFrom(entityType)) {
            transporters.add(ProtoMekBay.class);
            transporters.add(ProtoMekClampMount.class);
        }
        else if (Aero.class.isAssignableFrom(entityType)) {
            if ((unit.isFighter())) {
                transporters.add(ASFBay.class);
            }
            if ((unit.isFighter()) || unit.isSmallCraft()) {
                transporters.add(SmallCraftBay.class);
            }
            if (unit.hasETypeFlag(Entity.ETYPE_DROPSHIP) && (unit.getWeight() <= 5000)) {
                transporters.add(DropshuttleBay.class);
            }
            if (unit.hasETypeFlag(Entity.ETYPE_DROPSHIP) || unit.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                transporters.add(NavalRepairFacility.class);
                transporters.add(ReinforcedRepairFacility.class);
            }
            if (unit instanceof Dropship && !((Dropship) unit).isDockCollarDamaged()) {
                transporters.add(DockingCollar.class);
            }
        }
        else if (Tank.class.isAssignableFrom(entityType)) {
            if (unit.getWeight() <= 50) {
                transporters.add(LightVehicleBay.class);
            }

            if (unit.getWeight() <= 100) {
                transporters.add(HeavyVehicleBay.class);
            }

            if (unit.getWeight() <= 100) {
                transporters.add(SuperHeavyVehicleBay.class);
            }
        }
        else if (Mek.class.isAssignableFrom(entityType)) {
            boolean loadableQuadVee = (unit instanceof QuadVee) && (unit.getConversionMode() == QuadVee.CONV_MODE_MEK);
            boolean loadableLAM = (unit instanceof LandAirMek) && (unit.getConversionMode() != LandAirMek.CONV_MODE_FIGHTER);
            boolean loadableOtherMek = (unit instanceof Mek) && !(unit instanceof QuadVee) && !(unit instanceof LandAirMek);
            if (loadableQuadVee || loadableLAM || loadableOtherMek) {
                transporters.add(MekBay.class);

            } else {
                if ((unit instanceof QuadVee) && (unit.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) {
                    if (unit.getWeight() <= 50) {
                        transporters.add(LightVehicleBay.class);
                    }

                    if (unit.getWeight() <= 100) {
                        transporters.add(HeavyVehicleBay.class);
                    }

                    if (unit.getWeight() <= 100) {
                        transporters.add(SuperHeavyVehicleBay.class);
                    }
                }
            }
        }
        else if (Infantry.class.isAssignableFrom(entityType)) {
            transporters.add(InfantryBay.class);
            transporters.add(InfantryCompartment.class);

            if (BattleArmor.class.isAssignableFrom(entityType)) {
                transporters.add(BattleArmorBay.class);
                BattleArmor baUnit = (BattleArmor) unit;

                if (baUnit.canDoMechanizedBA()) {
                    transporters.add(BattleArmorHandles.class);
                    transporters.add(BattleArmorHandlesTank.class);

                    if (baUnit.hasMagneticClamps()) {
                        transporters.add(ClampMountMek.class);
                        transporters.add(ClampMountTank.class);
                    }
                }
            }
        }
        return transporters;
    }
}
