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

package mekhq.campaign.enums;

import megamek.common.*;
import mekhq.campaign.unit.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Enum for the different transport types used in MekHQ.
 * Campaign Transports allow a unit to be
 * assigned a transport (another unit).
 * The different transport types primarily differ
 * in the Transporters they allow.
 * @see Unit
 * @see Transporter
 */
public enum CampaignTransportType
{
    //region Enum declarations
    /**
     * Units assigned a ship transport, if both units are in the battle
     * the unit will attempt to load onto the transport when deployed into battle.
     * Ship transports are intended to be used for long-term travel or space combat
     * and only allow units to be transported in long-term Transporters like Bays or
     * Docking Collars.
     * @see Bay
     * @see DockingCollar
     */
    SHIP_TRANSPORT(TransportShipAssignment.class, ShipTransportedUnitsSummary.class) {

        /**
         * Helps the menus need to check less when generating. Ship Transports can't use short-term
         * transport types like InfantryCompartments or BattleArmorHandles. Use a Bay! Or DockingCollar
         * @param unit the unit we want to get the Transporter types that could potentially hold it
         * @return Transporter types that could potentially transport this entity
         */
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
    /**
     * Units assigned a tactical transport, if both units are in the battle
     * the unit will attempt to load onto the transport when deployed into battle.
     * Tactical Transporters are meant to represent short-term transport - Infantry in
     * an Infantry compartment, or Battle Armor on Battle Armor Handles. It still allows
     * units to be loaded into bays though for tactical Dropship assaults.
     * @see InfantryCompartment
     * @see BattleArmorHandles
     */
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

    /**
     * Is this a Ship Transport?
     * @return true if this is a SHIP_TRANSPORT
     */
    public boolean isShipTransport() { return this == SHIP_TRANSPORT; }

    /**
     * Is this a Tactical Transport?
     * @return true if this is a TACTICAL_TRANSPORT
     */
    public boolean isTacticalTransport() { return this == TACTICAL_TRANSPORT; }
    // endregion Boolean Comparison Methods

    // region Getters

    /**
     * Different Transport Types use different transport assignments.
     * @return Transport Assignment class used by this transport type
     */
    public Class<? extends ITransportAssignment> getTransportAssignmentType() { return transportAssignmentType; }

    /**
     * Different Transport Types use different transported units summaries.
     * @return Transported Unit Summary used by this transport type
     */
    public Class<? extends AbstractTransportedUnitsSummary> getTransportedUnitsSummaryType() { return transportedUnitsSummaryType; }
    // endregion Getters


    // region Static Helpers
    /**
     * Helps the menus need to check less when generating
     *
     * @see Transporter
     * @param unit unit we want to get the Transporter types that could potentially hold it
     * @return Transporter types that could potentially transport this entity
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

            if (unit.getWeight() <= 150) {
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
