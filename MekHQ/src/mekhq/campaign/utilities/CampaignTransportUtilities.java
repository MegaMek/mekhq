/**
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 */

package mekhq.campaign.utilities;

import static mekhq.campaign.unit.enums.TransporterType.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import megamek.common.*;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.utilities.MHQInternationalization;
import org.apache.commons.math3.util.Pair;

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
        if (campaignTransportType.isTowTransport()) {
            if (unit.isTrailer()) {
                return EnumSet.of(TANK_TRAILER_HITCH);
            }
            else {
                return EnumSet.noneOf(TransporterType.class);
            }
        }
        return getTransportTypeClassifier(unit).map(v -> v.getTransporterTypes(unit, campaignTransportType)).orElse(EnumSet.noneOf(TransporterType.class));
    }


    /**
     * Most slots are 1:1, infantry use their tonnage in some cases
     * TANK_TRAILER_HITCH use the maximum pulling capacity of its
     * tractor so return the transported unit's weight
     *
     * @param transporterType type (Enum) of Transporter
     * @param transportedUnit Entity we want the capacity usage of
     * @return how much capacity this unit uses when being transported in this kind of transporter
     */
    public static double transportCapacityUsage(TransporterType transporterType, Entity transportedUnit) {
        if (transportedUnit instanceof Infantry) {
            if (transporterType == INFANTRY_BAY || transporterType == CARGO_BAY) { // TODO from MekHQ#5928: Add Cargo Container
                return calcInfantryBayWeight(transportedUnit);
            }
            else if (transporterType == INFANTRY_COMPARTMENT) {
                return calcInfantryCompartmentWeight(transportedUnit);
            }
        } else if (transporterType == TANK_TRAILER_HITCH) {
            return transportedUnit.getWeight();
        }
        return 1.0;
    }

    /**
     * Calculates transport bay space required by an infantry platoon,
     * which is not the same as the flat weight of that platoon
     *
     * @param entity The Entity that we need the weight for
     * @return Capacity in tons needed to transport this entity
     */
    private static double calcInfantryBayWeight(Entity entity) {
        InfantryBay.PlatoonType type = InfantryBay.PlatoonType.getPlatoonType(entity);

        if ((entity instanceof Infantry) && (type == InfantryBay.PlatoonType.MECHANIZED)) {
            return type.getWeight() * ((Infantry) entity).getSquadCount();
        } else {
            return type.getWeight();
        }
    }

    /**
     * Calculates transport space required for an infantry compartment
     *
     * @param entity The Entity that we need the weight for
     * @return Capacity in tons needed to transport this entity
     */
    private static double calcInfantryCompartmentWeight(Entity entity) {
        return entity.getWeight();
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
                    transporters.add(CARGO_BAY);
                    // TODO from MekHQ#5928: Add Cargo Container
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

    /**
     * Return "None" in the first position
     * @return vector of transport options, with none first
     */
    public static Vector<Pair<String, CampaignTransportType>> getLeadershipDropdownVectorPair() {
        Vector<Pair<String, CampaignTransportType>> retVal = new Vector<>();
        retVal.add(new Pair<>(MHQInternationalization.getTextAt("mekhq.resources.AssignForceToTransport", "CampaignTransportUtilities.selectTransport.null.text"), null));
        retVal.add(new Pair<>(MHQInternationalization.getTextAt("mekhq.resources.AssignForceToTransport", "CampaignTransportUtilities.selectTransport.TACTICAL_TRANSPORT.text"), CampaignTransportType.TACTICAL_TRANSPORT));
        retVal.add(new Pair<>(MHQInternationalization.getTextAt("mekhq.resources.AssignForceToTransport", "CampaignTransportUtilities.selectTransport.SHIP_TRANSPORT.text"), CampaignTransportType.SHIP_TRANSPORT));
        retVal.add(new Pair<>(MHQInternationalization.getTextAt("mekhq.resources.AssignForceToTransport",
              "CampaignTransportUtilities.selectTransport.TOW_TRANSPORT.text"), CampaignTransportType.TOW_TRANSPORT));

        return retVal;
    }
    // endregion Static Helpers
}
