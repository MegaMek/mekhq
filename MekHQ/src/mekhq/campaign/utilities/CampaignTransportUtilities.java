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

package mekhq.campaign.utilities;

import megamek.common.*;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;

import java.util.HashSet;
import java.util.Set;

import static mekhq.campaign.unit.enums.TransporterType.*;

public class CampaignTransportUtilities {
    // region Static Helpers
    /**
     * Helps the menus need to check less when generating Ship Transports can't use short-term
     * transport types like InfantryCompartments or BattleArmorHandles. Use a Bay! Or DockingCollar
     *
     * @see Transporter
     * @param unit unit we want to get the Transporter types that could potentially hold it
     * @return Transporter types that could potentially transport this entity
     */
    public static Set<TransporterType> mapEntityToTransporters(CampaignTransportType campaignTransportType, Entity unit) {
        Set<TransporterType> transporters = new HashSet<>();

        Class<? extends Entity> entityType = unit.getClass();
        if (ProtoMek.class.isAssignableFrom(entityType)) {
            transporters.add(PROTO_MEK_BAY);
            transporters.add(PROTO_MEK_CLAMP_MOUNT);
        }
        else if (Aero.class.isAssignableFrom(entityType)) {
            if ((unit.isFighter())) {
                transporters.add(ASF_BAY);
            }
            if ((unit.isFighter()) || unit.isSmallCraft()) {
                transporters.add(SMALL_CRAFT_BAY);
            }
            if (unit.hasETypeFlag(Entity.ETYPE_DROPSHIP) && (unit.getWeight() <= 5000)) {
                transporters.add(DROPSHUTTLE_BAY);
            }
            if (unit.hasETypeFlag(Entity.ETYPE_DROPSHIP) || unit.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                transporters.add(NAVAL_REPAIR_FACILITY);
                transporters.add(REINFORCED_REPAIR_FACILITY);
            }
            if (unit instanceof Dropship && !((Dropship) unit).isDockCollarDamaged()) {
                transporters.add(DOCKING_COLLAR);
            }
        }
        else if (Tank.class.isAssignableFrom(entityType)) {
            if (unit.getWeight() <= 50) {
                transporters.add(LIGHT_VEHICLE_BAY);
            }

            if (unit.getWeight() <= 100) {
                transporters.add(HEAVY_VEHICLE_BAY);
            }

            if (unit.getWeight() <= 150) {
                transporters.add(SUPER_HEAVY_VEHICLE_BAY);
            }
        }
        else if (Mek.class.isAssignableFrom(entityType)) {
            boolean loadableQuadVee = (unit instanceof QuadVee) && (unit.getConversionMode() == QuadVee.CONV_MODE_MEK);
            boolean loadableLAM = (unit instanceof LandAirMek) && (unit.getConversionMode() != LandAirMek.CONV_MODE_FIGHTER);
            boolean loadableOtherMek = (unit instanceof Mek) && !(unit instanceof QuadVee) && !(unit instanceof LandAirMek);
            if (loadableQuadVee || loadableLAM || loadableOtherMek) {
                transporters.add(MEK_BAY);

            } else {
                if ((unit instanceof QuadVee) && (unit.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) {
                    if (unit.getWeight() <= 50) {
                        transporters.add(LIGHT_VEHICLE_BAY);
                    }

                    if (unit.getWeight() <= 100) {
                        transporters.add(HEAVY_VEHICLE_BAY);
                    }

                    if (unit.getWeight() <= 100) {
                        transporters.add(SUPER_HEAVY_VEHICLE_BAY);
                    }
                }
            }
        }
        else if (Infantry.class.isAssignableFrom(entityType)) {
            transporters.add(INFANTRY_BAY);

            //Ship transports can't use some transport types
            if (!(campaignTransportType.isShipTransport())) {
                transporters.add(INFANTRY_COMPARTMENT);

                if (BattleArmor.class.isAssignableFrom(entityType)) {
                    transporters.add(BATTLE_ARMOR_BAY);
                    BattleArmor baUnit = (BattleArmor) unit;

                    if (baUnit.canDoMechanizedBA()) {
                        transporters.add(BATTLE_ARMOR_HANDLES);
                        transporters.add(BATTLE_ARMOR_HANDLES_TANK);

                        if (baUnit.hasMagneticClamps()) {
                            transporters.add(CLAMP_MOUNT_MEK);
                            transporters.add(CLAMP_MOUNT_TANK);
                        }
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
    public static double transportCapacityUsage(TransporterType transporterType, Entity transportedUnit) {
        if (transporterType == INFANTRY_BAY || transporterType == INFANTRY_COMPARTMENT) {
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
}
