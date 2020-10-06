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

/**
 * Provides methods to gather statistics on units in a hangar.
 */
public class HangarStatistics {
    private final Hangar hangar;

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

    public int getNumberOfUnitsByType(long type, boolean inTransit, boolean lv) {
        int num = 0;
        for (Unit unit : getHangar().getUnits()) {
            if (!inTransit && !unit.isPresent()) {
                continue;
            }
            if (unit.isMothballed()) {
                if (type == Unit.ETYPE_MOTHBALLED) {
                    num++;
                }
                continue;
            }
            Entity en = unit.getEntity();
            if (en instanceof GunEmplacement || en instanceof FighterSquadron || en instanceof Jumpship) {
                continue;
            }
            if (type == Entity.ETYPE_MECH && en instanceof Mech) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_DROPSHIP && en instanceof Dropship) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_SMALL_CRAFT && en instanceof SmallCraft
                    && !(en instanceof Dropship)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_CONV_FIGHTER && en instanceof ConvFighter) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_AERO && en instanceof Aero
                    && !(en instanceof SmallCraft || en instanceof ConvFighter)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_INFANTRY && en instanceof Infantry && !(en instanceof BattleArmor)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_BATTLEARMOR && en instanceof BattleArmor) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_TANK && en instanceof Tank) {
                if ((en.getWeight() <= 50 && lv) || (en.getWeight() > 50 && !lv)) {
                    num++;
                }
                continue;
            }
            if (type == Entity.ETYPE_PROTOMECH && en instanceof Protomech) {
                num++;
            }
        }

        return num;
    }

    public int getOccupiedBays(long type) {
        return getOccupiedBays(type, false);
    }

    public int getOccupiedBays(long type, boolean lv) {
        int num = 0;
        for (Unit unit : getHangar().getUnits()) {
            if (unit.isMothballed()) {
                continue;
            }
            Entity en = unit.getEntity();
            if (en instanceof GunEmplacement || en instanceof Jumpship) {
                continue;
            }
            if (type == Entity.ETYPE_MECH && en instanceof Mech) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_DROPSHIP && en instanceof Dropship) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_SMALL_CRAFT && en instanceof SmallCraft && !(en instanceof Dropship)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_CONV_FIGHTER && en instanceof ConvFighter) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_AERO && en instanceof Aero
                    && !(en instanceof SmallCraft || en instanceof ConvFighter)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_INFANTRY && en instanceof Infantry && !(en instanceof BattleArmor)) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_BATTLEARMOR && en instanceof BattleArmor) {
                num++;
                continue;
            }
            if (type == Entity.ETYPE_TANK && en instanceof Tank) {
                if ((en.getWeight() <= 50 && lv) || (en.getWeight() > 50 && !lv)) {
                    num++;
                }
                continue;
            }
            if (type == Entity.ETYPE_PROTOMECH && en instanceof Protomech) {
                num++;
            }
        }

        if (type == Entity.ETYPE_MECH) {
            return Math.min(getTotalMechBays(), num);
        }

        if (type == Entity.ETYPE_AERO) {
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
