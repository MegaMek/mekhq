/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.bays.BayType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.FighterSquadron;
import megamek.common.units.Jumpship;
import megamek.logging.MMLogger;
import mekhq.campaign.Hangar;

/**
 * Provides methods to gather statistics on units in a hangar.
 */
public class HangarStatistics {
    private static final MMLogger logger = MMLogger.create(HangarStatistics.class);

    private final Hangar hangar;
    private static final long LIGHT_VEHICLE_BIT = 1L << 62;
    private static final long SUPER_HEAVY_BIT = 1L << 63;

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
     *       <li>Light Vehicles: {@code weight ≤ 50.0 (with LIGHT_VEHICLE_BIT)}</li>
     *       <li>Heavy Vehicles: {@code 50.0 < weight ≤ 100.0}</li>
     *       <li>Super Heavy: {@code weight > 100.0 (with SUPER_HEAVY_BIT)}</li>
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
                hashMap.merge((long) Unit.ETYPE_MOTHBALLED, 1, Integer::sum);
                continue;
            }

            Entity en = unit.getEntity();

            // Non-transportable unit types that are tracked for other reporting purposes
            switch (en) {
                case GunEmplacement ignored -> hashMap.merge(Entity.ETYPE_GUN_EMPLACEMENT, 1, Integer::sum);
                case FighterSquadron ignored -> hashMap.merge(Entity.ETYPE_FIGHTER_SQUADRON, 1, Integer::sum);
                case Jumpship ignored -> hashMap.merge(Entity.ETYPE_JUMPSHIP, 1, Integer::sum);
                case Dropship ignored -> hashMap.merge(Entity.ETYPE_DROPSHIP, 1, Integer::sum);
                case null, default -> {
                    // For all other units, use BayType to determine the correct transport bay category.
                    // This delegates to MegaMek's canonical entity-to-bay mapping, ensuring fighters
                    // (ASF, Conventional, Fixed-Wing Support) are all correctly categorized together.
                    BayType bayType = BayType.getTypeForEntity(en);
                    if (bayType != null) {
                        long key = bayTypeToKey(bayType);
                        if (key >= 0) {
                            hashMap.merge(key, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        return hashMap;
    }

    /**
     * Maps a {@link BayType} to the ETYPE-based key used in the tally map. Returns {@code -1L} and logs a warning for
     * any unhandled bay type so that new types added to MegaMek fail visibly rather than being silently counted.
     *
     * @param bayType the bay type to map
     *
     * @return the corresponding ETYPE key for the tally map, or {@code -1L} if the bay type is not mapped
     */
    private static long bayTypeToKey(BayType bayType) {
        return switch (bayType) {
            case MEK -> Entity.ETYPE_MEK;
            case FIGHTER -> Entity.ETYPE_AEROSPACE_FIGHTER;
            case PROTOMEK -> Entity.ETYPE_PROTOMEK;
            case SMALL_CRAFT -> Entity.ETYPE_SMALL_CRAFT;
            case VEHICLE_LIGHT -> Entity.ETYPE_TANK | LIGHT_VEHICLE_BIT;
            case VEHICLE_HEAVY -> Entity.ETYPE_TANK;
            case VEHICLE_SH -> Entity.ETYPE_TANK | SUPER_HEAVY_BIT;
            case INFANTRY_FOOT, INFANTRY_JUMP, INFANTRY_MOTORIZED, INFANTRY_MECHANIZED -> Entity.ETYPE_INFANTRY;
            case BATTLEARMOR_IS, BATTLEARMOR_CLAN, BATTLEARMOR_CS -> Entity.ETYPE_BATTLEARMOR;
            default -> {
                logger.warn("Unmapped BayType in transport tally: {}", bayType);
                yield -1L;
            }
        };
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

    @Deprecated(since = "0.51.0", forRemoval = true)
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

    @Deprecated(since = "0.51.0", forRemoval = true)
    public int getTotalLargeCraftPassengerCapacity() {
        return getHangar().getUnitsStream()
                     .filter(u -> u.getEntity().isLargeCraft() || u.getEntity().isSmallCraft())
                     .mapToInt(u -> u.getEntity().getNPassenger() + u.getEntity().getBayPersonnel())
                     .sum();
    }
}
