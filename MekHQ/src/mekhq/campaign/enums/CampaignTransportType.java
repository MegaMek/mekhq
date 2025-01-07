package mekhq.campaign.enums;

import megamek.common.*;
import mekhq.campaign.unit.*;

import java.util.HashSet;
import java.util.Set;

public enum CampaignTransportType
{
    //region Enum declarations
    SHIP_TRANSPORT(TransportShipAssignment.class, ShipTransportedUnitsSummary.class) {
         @Override
         public Set<Class<? extends Transporter>> mapEntityToTransporters(Entity unit) {
            Set<Class<? extends Transporter>> transporters = super.mapEntityToTransporters(unit);
            transporters.remove(InfantryCompartment.class);
            transporters.remove(BattleArmorHandles.class);
            transporters.remove(BattleArmorHandlesTank.class);
            transporters.remove(ClampMountMek.class);
            transporters.remove(ClampMountTank.class);

            return transporters;
        }

    },
    TACTICAL_TRANSPORT(TransportAssignment.class, TacticalTransportedUnitsSummary.class);
    // endregion Enum declarations


    // region Variable declarations
    private final Class<? extends ITransportAssignment> transportAssignmentType;
    private final Class<? extends AbstractTransportedUnitsSummary> transportedUnitsSummaryType;
    // endregion Variable declarations

    // region Constructors
    CampaignTransportType(Class<? extends ITransportAssignment> transportAssignmentType, Class<? extends AbstractTransportedUnitsSummary> transportedUnitsSummaryType) {
        this.transportAssignmentType = transportAssignmentType;
        this.transportedUnitsSummaryType = transportedUnitsSummaryType;
    }
    // endregion Constructors


    // region Boolean Comparison Methods
    public boolean isShipTransport() { return this == SHIP_TRANSPORT; }
    public boolean isTacticalTransport() { return this == TACTICAL_TRANSPORT; }
    // endregion Boolean Comparison Methods

    // region Getters
    public Class<? extends ITransportAssignment> getTransportAssignmentType() { return transportAssignmentType; }
    public Class<? extends AbstractTransportedUnitsSummary> getTransportedUnitsSummaryType() { return transportedUnitsSummaryType; }
    // endregion Getters


    // region Static Helpers
    /**
     * Helps the menus need to check less when generating
     *
     * @see Bay and its subclass's canLoad(Entity unit) methods
     * @param unit the unit we want to get the Transporter types that could potentially hold it
     * @return the transporter types that could potentially transport this entity
     */
    public Set<Class<? extends Transporter>> mapEntityToTransporters(Entity unit) {
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


    /**
     * Most slots are 1:1, infantry use their tonnage in some cases
     *
     * @param transporterType type (Class) of Transporter
     * @param transportedUnit Entity we want the capacity usage of
     * @return how much capacity this unit uses when being transported in this kind of transporter
     */
    public static double transportCapacityUsage(Class< ? extends Transporter> transporterType, Entity transportedUnit) {
        if (InfantryBay.class.isAssignableFrom(transporterType)
            || InfantryCompartment.class.isAssignableFrom(transporterType)) {
            if (Infantry.class.isAssignableFrom(transportedUnit.getClass())) {
                return calcInfantryBayWeight(transportedUnit);
            }
        }
        return 1.0;
    }

    /**
     * Calculates transport bay space required by an infantry platoon,
     * which is not the same as the flat weight of that platoon
     *
     * @param unit The Entity that we need the weight for
     */
    public static double calcInfantryBayWeight(Entity unit) {
        InfantryBay.PlatoonType type = InfantryBay.PlatoonType.getPlatoonType(unit);
        if ((unit instanceof Infantry) && (type == InfantryBay.PlatoonType.MECHANIZED)) {
            return type.getWeight() * ((Infantry) unit).getSquadCount();
        } else {
            return type.getWeight();
        }
    }
    // endregion Static Helpers

    public String getName() {
        return this.toString();
    }
}
