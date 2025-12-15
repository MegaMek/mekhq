/*
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.campaign.utilities;

import static mekhq.campaign.unit.enums.TransporterType.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.Cargo;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.equipment.ICarryable;
import megamek.common.units.*;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.enums.TransporterType;
import mekhq.utilities.MHQInternationalization;
import org.apache.commons.math3.util.Pair;

public class CampaignTransportUtilities {
    // region Static Variables
    static final String ASSIGN_FORCE_TO_TRANSPORT_BUNDLE = "mekhq.resources.AssignForceToTransport";
    static final String SELECT_TRANSPORT_KEY = "CampaignTransportUtilities.selectTransport";
    // endregion Static Variables

    // region Static Helpers

    interface Visitor<T extends ICarryable> {
        boolean isInterestedIn(ICarryable entity);

        EnumSet<TransporterType> getTransporterTypes(T entity, CampaignTransportType campaignTransportType);
    }

    /**
     * Helps the menus need to check less when generating Transports. Let's get a list of TransporterTypes that this
     * ICarryable could potentially be transported in. This will make it much easier to determine what Transporters we
     * should even look at. In addition, CampaignTransportTypes that can't use certain TransporterTypes is handled, like
     * Ship Transports not being able to use InfantryCompartments or BattleArmorHandles. Use a Bay! Or DockingCollar.
     *
     * @param campaignTransportType type (enum) of campaign transport - some transport types can't use certain
     *                              transporters
     * @param carryable             carryable object we want to get the Transporter types that could potentially hold
     *                              it
     *
     * @return Transporter types that could potentially transport this entity
     *
     * @see TransporterType
     */
    public static EnumSet<TransporterType> mapICarryableToTransporters(CampaignTransportType campaignTransportType,
          ICarryable carryable) {
        if (campaignTransportType.isTowTransport() && carryable instanceof Entity entity) {
            if (entity.isTrailer()) {
                return EnumSet.of(TANK_TRAILER_HITCH);
            } else {
                return EnumSet.noneOf(TransporterType.class);
            }
        }
        return getTransportTypeClassifier(carryable).map(v -> v.getTransporterTypes(carryable, campaignTransportType))
                     .orElse(EnumSet.noneOf(TransporterType.class));
    }


    /**
     * Most slots are 1:1, infantry use their tonnage in some cases TANK_TRAILER_HITCH use the maximum pulling capacity
     * of its tractor so return the transported unit's weight. If it's cargo, let's use its tonnage.
     *
     * @param transporterType type (Enum) of Transporter
     * @param transportedUnit ICarryable we want the capacity usage of
     *
     * @return how much capacity this unit uses when being transported in this kind of transporter
     */
    public static double transportCapacityUsage(TransporterType transporterType, ICarryable transportedUnit) {
        if (transportedUnit instanceof Infantry transportedInfantry) {
            if (transporterType == INFANTRY_BAY ||
                      transporterType == CARGO_BAY) { // TODO from MekHQ#5928: Add Cargo Container
                return calcInfantryBayWeight(transportedInfantry);
            } else if (transporterType == INFANTRY_COMPARTMENT) {
                return calcInfantryCompartmentWeight(transportedInfantry);
            }
        } else if (transporterType == TANK_TRAILER_HITCH || transporterType == NAVAL_REPAIR_FACILITY) {
            return transportedUnit.getTonnage();
        } else if (transportedUnit instanceof Cargo) {
            return transportedUnit.getTonnage();
        }
        return 1.0;
    }

    /**
     * Calculates transport bay space required by an infantry platoon, which is not the same as the flat weight of that
     * platoon
     *
     * @param entity The Entity that we need the weight for
     *
     * @return Capacity in tons needed to transport this entity
     */
    private static double calcInfantryBayWeight(Entity entity) {
        PlatoonType type = PlatoonType.getPlatoonType(entity);

        if ((entity instanceof Infantry) && (type == PlatoonType.MECHANIZED)) {
            return type.getWeight() * ((Infantry) entity).getSquadCount();
        } else {
            return type.getWeight();
        }
    }

    /**
     * Calculates transport space required for an infantry compartment
     *
     * @param entity The Entity that we need the weight for
     *
     * @return Capacity in tons needed to transport this entity
     */
    private static double calcInfantryCompartmentWeight(Entity entity) {
        return entity.getWeight();
    }


    private static final List<Visitor> visitors = List.of(
          new Visitor<ProtoMek>() {
              @Override
              public boolean isInterestedIn(ICarryable entity) {
                  return entity instanceof ProtoMek;
              }

              @Override
              public EnumSet<TransporterType> getTransporterTypes(ProtoMek entity,
                    CampaignTransportType campaignTransportType) {
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
              public boolean isInterestedIn(ICarryable entity) {
                  return entity instanceof Aero;
              }

              @Override
              public EnumSet<TransporterType> getTransporterTypes(Aero entity,
                    CampaignTransportType campaignTransportType) {
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
              public boolean isInterestedIn(ICarryable entity) {
                  return entity instanceof Tank;
              }

              @Override
              public EnumSet<TransporterType> getTransporterTypes(Tank entity,
                    CampaignTransportType campaignTransportType) {

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
              public boolean isInterestedIn(ICarryable entity) {
                  return entity instanceof Mek;
              }

              @Override
              public EnumSet<TransporterType> getTransporterTypes(Mek entity,
                    CampaignTransportType campaignTransportType) {
                  EnumSet<TransporterType> transporters = EnumSet.noneOf(TransporterType.class);
                  boolean loadableQuadVee = (entity instanceof QuadVee) &&
                                                  (entity.getConversionMode() == QuadVee.CONV_MODE_MEK);
                  boolean loadableLAM = (entity instanceof LandAirMek) &&
                                              (entity.getConversionMode() != LandAirMek.CONV_MODE_FIGHTER);
                  boolean loadableOtherMek = (entity != null) &&
                                                   !(entity instanceof QuadVee) &&
                                                   !(entity instanceof LandAirMek);
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
              public boolean isInterestedIn(ICarryable entity) {
                  return entity instanceof Infantry;
              }

              @Override
              public EnumSet<TransporterType> getTransporterTypes(Infantry entity,
                    CampaignTransportType campaignTransportType) {
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
          },
          new Visitor<Cargo>() {
              @Override
              public boolean isInterestedIn(ICarryable entity) {
                  return entity instanceof Cargo;
              }

              @Override
              public EnumSet<TransporterType> getTransporterTypes(Cargo cargo,
                    CampaignTransportType
                          campaignTransportType) {
                  EnumSet<TransporterType> transporters = EnumSet.noneOf(TransporterType.class);
                  transporters.add(CARGO_BAY);
                  transporters.add(REFRIGERATED_BAY);
                  transporters.add(INSULATED_BAY);

                  //Ship transports can't use some transport types
                  if (!(campaignTransportType.isShipTransport())) {
                      // Add ROOF_RACK back once we can better handle how they impact MP
                      transporters.add(LIFT_HOIST);
                      transporters.add(ROOF_RACK);
                  }

                  return transporters;
              }
          },
          new Visitor<HandheldWeapon>() {
              @Override
              public boolean isInterestedIn(ICarryable entity) {
                  return entity instanceof HandheldWeapon;
              }

              @Override
              public EnumSet<TransporterType> getTransporterTypes(HandheldWeapon handheldWeapon,
                    CampaignTransportType
                          campaignTransportType) {
                  EnumSet<TransporterType> transporters = EnumSet.noneOf(TransporterType.class);

                  //Ship transports can't use some transport types
                  if (!(campaignTransportType.isShipTransport())) {
                      // For now just MekArms. This should be expanded to include regular cargo bays and lift hoists
                      // once those are supported for HHW in MHQ
                      transporters.add(MEK_ARMS);
                  }

                  return transporters;
              }
          });

    private static Optional<Visitor> getTransportTypeClassifier(ICarryable entity) {
        return visitors.stream().filter(v -> v.isInterestedIn(entity)).findFirst();
    }

    /**
     * Return "None" in the first position
     *
     * @return vector of transport options, with none first
     */
    public static Vector<Pair<String, CampaignTransportType>> getLeadershipDropdownVectorPair() {
        Vector<Pair<String, CampaignTransportType>> retVal = new Vector<>();
        retVal.add(new Pair<>(MHQInternationalization.getTextAt(ASSIGN_FORCE_TO_TRANSPORT_BUNDLE,
              SELECT_TRANSPORT_KEY + ".null.text"), null));
        retVal.add(new Pair<>(MHQInternationalization.getTextAt(ASSIGN_FORCE_TO_TRANSPORT_BUNDLE,
              SELECT_TRANSPORT_KEY + ".TACTICAL_TRANSPORT.text"), CampaignTransportType.TACTICAL_TRANSPORT));
        retVal.add(new Pair<>(MHQInternationalization.getTextAt(ASSIGN_FORCE_TO_TRANSPORT_BUNDLE,
              SELECT_TRANSPORT_KEY + ".SHIP_TRANSPORT.text"), CampaignTransportType.SHIP_TRANSPORT));
        retVal.add(new Pair<>(MHQInternationalization.getTextAt(ASSIGN_FORCE_TO_TRANSPORT_BUNDLE,
              SELECT_TRANSPORT_KEY + ".TOW_TRANSPORT.text"), CampaignTransportType.TOW_TRANSPORT));

        return retVal;
    }
    // endregion Static Helpers
}
