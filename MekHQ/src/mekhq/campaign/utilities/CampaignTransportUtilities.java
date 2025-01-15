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

import java.util.*;

import static mekhq.campaign.unit.enums.TransporterType.*;

public class CampaignTransportUtilities {
    // region Static Helpers

    interface Visitor<T extends Entity> {
        boolean isInterestedIn(Entity entity);

        EnumSet<TransporterType> getTransporterTypes(T entity, CampaignTransportType campaignTransportType);
    }

    /**
     * Helps the menus need to check less when generating Transports. Let's get a list of
     * TransporterTypes that this Entity could potentially be transported in. This will make
     * it much easier to determine what Transporters we should even look at.
     * In addition, CampaignTransportTypes that can't use certain TransporterTypes is handled,
     * like Ship Transports not being able to use InfantryCompartments or BattleArmorHandles.
     * Use a Bay! Or DockingCollar.
     *
     * @see TransporterType
     * @param campaignTransportType type (enum) of campaign transport - some transport types can't use certain transporters
     * @param unit unit we want to get the Transporter types that could potentially hold it
     * @return Transporter types that could potentially transport this entity
     */
    public static EnumSet<TransporterType> mapEntityToTransporters(CampaignTransportType campaignTransportType, Entity unit) {
        return getTransportTypeClassifier(unit).map(v -> v.getTransporterTypes(unit, campaignTransportType)).orElse(EnumSet.noneOf(TransporterType.class));
    }


    /**
     * Most slots are 1:1, infantry use their tonnage in some cases
     *
     * @param transporterType type (Enum) of Transporter
     * @param transportedUnit Entity we want the capacity usage of
     * @return how much capacity this unit uses when being transported in this kind of transporter
     */
    public static double transportCapacityUsage(TransporterType transporterType, Entity transportedUnit) {
        if (transporterType == INFANTRY_BAY || transporterType == INFANTRY_COMPARTMENT) {
            if (transportedUnit instanceof Infantry) {
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


    private static final List<Visitor> visitors = List.of(

        new Visitor<ProtoMek>() {
            @Override
            public boolean isInterestedIn(Entity entity) {
                return entity instanceof ProtoMek;
            }

            @Override
            public EnumSet<TransporterType> getTransporterTypes(ProtoMek entity, CampaignTransportType campaignTransportType) {
                EnumSet<TransporterType> transporters = EnumSet.noneOf(TransporterType.class);
                transporters.add(PROTO_MEK_BAY);

                //Ship transports can't use some transport types
                if (!(campaignTransportType.isShipTransport())) {
                    transporters.add(PROTO_MEK_CLAMP_MOUNT);
                }

                return transporters;
            }
        },
        new Visitor<Aero>() {

            @Override
            public boolean isInterestedIn(Entity entity) {
                return entity instanceof Aero;
            }

            @Override
            public EnumSet<TransporterType> getTransporterTypes(Aero entity, CampaignTransportType campaignTransportType) {
                EnumSet<TransporterType> transporters = EnumSet.noneOf(TransporterType.class);
                if (entity.isFighter()) {
                    transporters.add(ASF_BAY);
                }
                if ((entity.isFighter()) || entity.isSmallCraft()) {
                    transporters.add(SMALL_CRAFT_BAY);
                }
                if (entity.hasETypeFlag(Entity.ETYPE_DROPSHIP) && (entity.getWeight() <= 5000)) {
                    transporters.add(DROPSHUTTLE_BAY);
                }
                if (entity.hasETypeFlag(Entity.ETYPE_DROPSHIP) || entity.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                    transporters.add(NAVAL_REPAIR_FACILITY);
                    transporters.add(REINFORCED_REPAIR_FACILITY);
                }
                if (entity instanceof Dropship && !((Dropship) entity).isDockCollarDamaged()) {
                    transporters.add(DOCKING_COLLAR);
                }

                return transporters;
            }
        },
        new Visitor<Tank>() {

            @Override
            public boolean isInterestedIn(Entity entity) {
                return entity instanceof Tank;
            }

            @Override
            public EnumSet<TransporterType> getTransporterTypes(Tank entity, CampaignTransportType campaignTransportType) {

                EnumSet<TransporterType> transporters = EnumSet.noneOf(TransporterType.class);

                if (entity.getWeight() <= 50) {
                    transporters.add(LIGHT_VEHICLE_BAY);
                }

                if (entity.getWeight() <= 100) {
                    transporters.add(HEAVY_VEHICLE_BAY);
                }

                if (entity.getWeight() <= 150) {
                    transporters.add(SUPER_HEAVY_VEHICLE_BAY);
                }
                return transporters;
            }
        },
        new Visitor<Mek>() {

            @Override
            public boolean isInterestedIn(Entity entity) {
                return entity instanceof Mek;
            }

            @Override
            public EnumSet<TransporterType> getTransporterTypes(Mek entity, CampaignTransportType campaignTransportType) {
                EnumSet<TransporterType> transporters = EnumSet.noneOf(TransporterType.class);
                boolean loadableQuadVee = (entity instanceof QuadVee) && (entity.getConversionMode() == QuadVee.CONV_MODE_MEK);
                boolean loadableLAM = (entity instanceof LandAirMek) && (entity.getConversionMode() != LandAirMek.CONV_MODE_FIGHTER);
                boolean loadableOtherMek = (entity != null) && !(entity instanceof QuadVee) && !(entity instanceof LandAirMek);
                if (loadableQuadVee || loadableLAM || loadableOtherMek) {
                    transporters.add(MEK_BAY);

                } else {
                    if ((entity instanceof QuadVee) && (entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE)) {
                        if (entity.getWeight() <= 50) {
                            transporters.add(LIGHT_VEHICLE_BAY);
                        }

                        if (entity.getWeight() <= 100) {
                            transporters.add(HEAVY_VEHICLE_BAY);
                        }

                        if (entity.getWeight() <= 100) {
                            transporters.add(SUPER_HEAVY_VEHICLE_BAY);
                        }
                    }
                }
                return transporters;
            }
        },
        new Visitor<Infantry>() {

            @Override
            public boolean isInterestedIn(Entity entity) {
                return entity instanceof Infantry;
            }

            @Override
            public EnumSet<TransporterType> getTransporterTypes(Infantry entity, CampaignTransportType campaignTransportType) {
                EnumSet<TransporterType> transporters = EnumSet.noneOf(TransporterType.class);

                //Ship transports can't use some transport types
                if (!(campaignTransportType.isShipTransport())) {
                    transporters.add(INFANTRY_COMPARTMENT);
                }

                if (entity instanceof BattleArmor baEntity) {
                    transporters.add(BATTLE_ARMOR_BAY);

                    //Ship transports can't use some transport types
                    if (baEntity.canDoMechanizedBA() && !campaignTransportType.isShipTransport()) {
                        transporters.add(BATTLE_ARMOR_HANDLES);
                        transporters.add(BATTLE_ARMOR_HANDLES_TANK);

                        if (baEntity.hasMagneticClamps()) {
                            transporters.add(CLAMP_MOUNT_MEK);
                            transporters.add(CLAMP_MOUNT_TANK);
                        }

                    }

                } else {
                    transporters.add(INFANTRY_BAY);
                }

                return transporters;
            }
        });

    private static Optional<Visitor> getTransportTypeClassifier(Entity entity) {
        return visitors.stream().filter(v -> v.isInterestedIn(entity)).findFirst();
    }
    // endregion Static Helpers
}
