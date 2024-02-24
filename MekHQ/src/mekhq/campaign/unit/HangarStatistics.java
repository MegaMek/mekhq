/*
 * HangarStatistics.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit;

import megamek.common.*;
import mekhq.campaign.Hangar;

import java.util.HashMap;

/**
 * Provides methods to gather statistics on units in a hangar.
 */
public class HangarStatistics {
    private final Hangar hangar;
    private long LIGHT_VEHICLE_BIT = 1L << 62;
    private long SUPER_HEAVY_BIT = 1L << 63;

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
     * Tally all used bay types and return a hashmap of ETYPE : Count
     * @param inTransit
     * @return
     */
    public HashMap<Long, Integer> tallyBaysByType(boolean inTransit) {
        HashMap<Long, Integer> hashMap = new HashMap<>();

        for (Unit unit : getHangar().getUnits()) {
            if (!inTransit && !unit.isPresent()) {
                continue;
            }
            if (unit.isMothballed()) {
                hashMap.put((long) Unit.ETYPE_MOTHBALLED, hashMap.getOrDefault((long) Unit.ETYPE_MOTHBALLED, 0) + 1);
            }

            Entity en = unit.getEntity();

            // Can be expanded to account for arbitrary types of transportable units.
            if (en instanceof GunEmplacement) {
                hashMap.put(Entity.ETYPE_GUN_EMPLACEMENT, hashMap.getOrDefault(Entity.ETYPE_GUN_EMPLACEMENT, 0) + 1);
            } else if (en instanceof FighterSquadron) {
                hashMap.put(Entity.ETYPE_FIGHTER_SQUADRON, hashMap.getOrDefault(Entity.ETYPE_FIGHTER_SQUADRON, 0) + 1);
            } else if (en instanceof Jumpship) {
                hashMap.put(Entity.ETYPE_JUMPSHIP, hashMap.getOrDefault(Entity.ETYPE_JUMPSHIP, 0) + 1);
            } else if (en instanceof Mech) {
                hashMap.put(Entity.ETYPE_MECH, hashMap.getOrDefault(Entity.ETYPE_MECH, 0) + 1);
            } else if (en instanceof Dropship) {
                hashMap.put(Entity.ETYPE_DROPSHIP, hashMap.getOrDefault(Entity.ETYPE_DROPSHIP, 0) + 1);
            } else if (en instanceof SmallCraft) {
                hashMap.put(Entity.ETYPE_SMALL_CRAFT, hashMap.getOrDefault(Entity.ETYPE_SMALL_CRAFT, 0) + 1);
            } else if (en instanceof ConvFighter) {
                hashMap.put(Entity.ETYPE_CONV_FIGHTER, hashMap.getOrDefault(Entity.ETYPE_CONV_FIGHTER, 0) + 1);
            } else if (en instanceof AeroSpaceFighter) {
                hashMap.put(Entity.ETYPE_AEROSPACEFIGHTER, hashMap.getOrDefault(Entity.ETYPE_AEROSPACEFIGHTER, 0) + 1);
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
            } else if (en instanceof Protomech) {
                hashMap.put(Entity.ETYPE_PROTOMECH, hashMap.getOrDefault(Entity.ETYPE_PROTOMECH, 0) + 1);
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

        if (type == Entity.ETYPE_MECH) {
            return Math.min(getTotalMechBays(), num);
        }

        // Okay to do an equality check here because this is the hash key, not the entity's ETYPE value.
        if (type == Entity.ETYPE_AEROSPACEFIGHTER) {
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

        if (type == Entity.ETYPE_PROTOMECH) {
            return Math.min(getTotalProtomechBays(), num);
        }

        if (type == Entity.ETYPE_DROPSHIP) {
            return Math.min(getTotalDockingCollars(), num);
        }

        return -1; // default, this is an error condition
    }

    public int getTotalMechBays() {
        return (int) Math.round(getHangar().getUnitsStream()
            .mapToDouble(Unit::getMechCapacity)
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

    public int getTotalLightVehicleBays() {
        return (int) Math.round(getHangar().getUnitsStream()
            .mapToDouble(Unit::getLightVehicleCapacity)
            .sum());
    }

    public int getTotalProtomechBays() {
        return (int) Math.round(getHangar().getUnitsStream()
            .mapToDouble(Unit::getProtomechCapacity)
            .sum());
    }

    public int getTotalDockingCollars() {
        return getHangar().getUnitsStream()
            .filter(u -> u.getEntity() instanceof Jumpship)
            .mapToInt(Unit::getDocks)
            .sum();
    }
}
