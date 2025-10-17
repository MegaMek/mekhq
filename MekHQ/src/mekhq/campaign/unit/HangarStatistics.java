/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.unit;

import java.util.HashMap;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.equipment.GunEmplacement;
import megamek.common.units.*;
import mekhq.campaign.Hangar;

/**
 * Provides methods to gather statistics on units in a hangar.
 */
public class HangarStatistics {
    private final Hangar hangar;
    public final long LIGHT_VEHICLE_BIT = 1L << 62;
    public final long SUPER_HEAVY_BIT = 1L << 63;

    public HangarStatistics(Hangar hangar) {
        this.hangar = hangar;
    }

    public Hangar getHangar() {
        return hangar;
    }

    public int getNumberOfUnitsByType(long type) {
        return getNumberOfUnitsByType(type, false, false);
    }

    public int getNumberOfUnitsByType(long type, boolean inTransit) {
        return getNumberOfUnitsByType(type, inTransit, false);
    }

    /**
     * Tallies the number of units in the hangar by their entity type or bay category.
     *
     * <p>The returned {@code HashMap} maps entity type identifiers (or entity type plus vehicle bitmask) to the
     * count of units of that type currently in the hangar.</p>
     *
     * <ul>
     *   <li>If {@code inTransit} is false, only present (not in-transit) units are considered.</li>
     *   <li>Mothballed units are counted using the {@link Unit#ETYPE_MOTHBALLED} key.</li>
     *   <li>Tanks are tallied into three categories based on unit weight:
     *     <ul>
     *       <li>Light Vehicles: weight ≤ 50.0 (with LIGHT_VEHICLE_BIT)</li>
     *       <li>Heavy Vehicles: 50.0 < weight ≤ 100.0</li>
     *       <li>Super Heavy: weight > 100.0 (with SUPER_HEAVY_BIT)</li>
     *     </ul>
     *   </li>
     *   <li>Other unit types (Mek, DropShip, Small Craft, etc.) use their respective entity type long constants.</li>
     *   <li>Infantry and BattleArmor are separated; only true Infantry (not BattleArmor) are counted under
     *   {@link Entity#ETYPE_INFANTRY}.</li>
     *   <li>Types not covered by the checked entity classes are not included in the result.</li>
     * </ul>
     *
     * @param inTransit if true, includes units that are in transit; if false, only includes units that are present
     *
     * @return a {@code HashMap} mapping entity type (or bay type) keys to the number of distinct units of that type in
     *       the hangar
     */
    public HashMap<Long, Integer> tallyBaysByType(boolean inTransit) {
        HashMap<Long, Integer> hashMap = new HashMap<>();

        for (Unit unit : getHangar().getUnits()) {
            if (!inTransit && !unit.isPresent()) {
                continue;
            }
            if (unit.isMothballed()) {
                hashMap.put((long) Unit.ETYPE_MOTHBALLED, hashMap.getOrDefault((long) Unit.ETYPE_MOTHBALLED, 0) + 1);
                continue;
            }

            Entity en = unit.getEntity();

            // Can be expanded to account for arbitrary types of transportable units.
            if (en instanceof GunEmplacement) {
                hashMap.put(Entity.ETYPE_GUN_EMPLACEMENT, hashMap.getOrDefault(Entity.ETYPE_GUN_EMPLACEMENT, 0) + 1);
            } else if (en instanceof FighterSquadron) {
                hashMap.put(Entity.ETYPE_FIGHTER_SQUADRON, hashMap.getOrDefault(Entity.ETYPE_FIGHTER_SQUADRON, 0) + 1);
            } else if (en instanceof Jumpship) {
                hashMap.put(Entity.ETYPE_JUMPSHIP, hashMap.getOrDefault(Entity.ETYPE_JUMPSHIP, 0) + 1);
            } else if (en instanceof Mek) {
                hashMap.put(Entity.ETYPE_MEK, hashMap.getOrDefault(Entity.ETYPE_MEK, 0) + 1);
            } else if (en instanceof Dropship) {
                hashMap.put(Entity.ETYPE_DROPSHIP, hashMap.getOrDefault(Entity.ETYPE_DROPSHIP, 0) + 1);
            } else if (en instanceof SmallCraft) {
                hashMap.put(Entity.ETYPE_SMALL_CRAFT, hashMap.getOrDefault(Entity.ETYPE_SMALL_CRAFT, 0) + 1);
            } else if (en instanceof ConvFighter) {
                hashMap.put(Entity.ETYPE_CONV_FIGHTER, hashMap.getOrDefault(Entity.ETYPE_CONV_FIGHTER, 0) + 1);
            } else if (en instanceof AeroSpaceFighter) {
                hashMap.put(Entity.ETYPE_AEROSPACE_FIGHTER,
                      hashMap.getOrDefault(Entity.ETYPE_AEROSPACE_FIGHTER, 0) + 1);
            } else if ((en instanceof Infantry) && !(en instanceof BattleArmor)) {
                hashMap.put(Entity.ETYPE_INFANTRY, hashMap.getOrDefault(Entity.ETYPE_INFANTRY, 0) + 1);
            } else if (en instanceof BattleArmor) {
                hashMap.put(Entity.ETYPE_BATTLEARMOR, hashMap.getOrDefault(Entity.ETYPE_BATTLEARMOR, 0) + 1);
            } else if (en instanceof Tank) {
                // Split Tank into three indices, to match the two currently supported bay types and SH.
                double weight = en.getWeight();
                if (weight <= 50.0) {
                    hashMap.put(Entity.ETYPE_TANK | LIGHT_VEHICLE_BIT,
                          hashMap.getOrDefault(Entity.ETYPE_TANK | LIGHT_VEHICLE_BIT, 0) + 1);
                } else if (weight > 50.0 && weight <= 100.0) {
                    hashMap.put(Entity.ETYPE_TANK, hashMap.getOrDefault(Entity.ETYPE_TANK, 0) + 1);
                } else {
                    hashMap.put(Entity.ETYPE_TANK | SUPER_HEAVY_BIT,
                          hashMap.getOrDefault(Entity.ETYPE_TANK | SUPER_HEAVY_BIT, 0) + 1);
                }
            } else if (en instanceof ProtoMek) {
                hashMap.put(Entity.ETYPE_PROTOMEK, hashMap.getOrDefault(Entity.ETYPE_PROTOMEK, 0) + 1);
            }
        }

        return hashMap;
    }

    public int getNumberOfUnitsByType(long type, boolean inTransit, boolean lv) {
        HashMap<Long, Integer> bayMap = tallyBaysByType(inTransit);
        long key = (lv) ? type | LIGHT_VEHICLE_BIT : type;

        return bayMap.getOrDefault(key, 0);
    }

    public int getOccupiedBays(long type) {
        return getOccupiedBays(type, false);
    }

    public int getOccupiedBays(long type, boolean lv) {
        HashMap<Long, Integer> bayMap = tallyBaysByType(false);
        long key = (lv) ? type | LIGHT_VEHICLE_BIT : type;

        int num = bayMap.getOrDefault(key, 0);

        if (type == Entity.ETYPE_MEK) {
            return Math.min(getTotalMekBays(), num);
        }

        // Okay to do an equality check here because this is the hash key, not the entity's ETYPE value.
        if (type == Entity.ETYPE_AEROSPACE_FIGHTER) {
            return Math.min(getTotalASFBays(), num);
        }

        if (type == Entity.ETYPE_INFANTRY) {
            return Math.min(getTotalInfantryBays(), num);
        }

        if (type == Entity.ETYPE_BATTLEARMOR) {
            return Math.min(getTotalBattleArmorBays(), num);
        }

        if (type == Entity.ETYPE_TANK) {
            if (lv) {
                return Math.min(getTotalLightVehicleBays(), num);
            }
            return Math.min(getTotalHeavyVehicleBays(), num);
        }

        if (type == Entity.ETYPE_SMALL_CRAFT) {
            return Math.min(getTotalSmallCraftBays(), num);
        }

        if (type == Entity.ETYPE_PROTOMEK) {
            return Math.min(getTotalProtoMekBays(), num);
        }

        if (type == Entity.ETYPE_DROPSHIP) {
            return Math.min(getTotalDockingCollars(), num);
        }

        return -1; // default, this is an error condition
    }

    public int getTotalMekBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getMekCapacity)
                                      .sum());
    }

    public int getTotalASFBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getASFCapacity)
                                      .sum());
    }

    public int getTotalSmallCraftBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getSmallCraftCapacity)
                                      .sum());
    }

    public int getTotalBattleArmorBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getBattleArmorCapacity)
                                      .sum());
    }

    public int getTotalInfantryBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getInfantryCapacity)
                                      .sum());
    }

    public int getTotalHeavyVehicleBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getHeavyVehicleCapacity)
                                      .sum());
    }

    public int getTotalSuperHeavyVehicleBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getSuperHeavyVehicleCapacity)
                                      .sum());
    }

    public int getTotalLightVehicleBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getLightVehicleCapacity)
                                      .sum());
    }

    public int getTotalProtoMekBays() {
        return (int) Math.round(getHangar().getUnitsStream()
                                      .mapToDouble(Unit::getProtoMekCapacity)
                                      .sum());
    }

    public int getTotalDockingCollars() {
        return getHangar().getUnitsStream()
                     .filter(u -> u.getEntity() instanceof Jumpship)
                     .mapToInt(Unit::getDocks)
                     .sum();
    }
}
