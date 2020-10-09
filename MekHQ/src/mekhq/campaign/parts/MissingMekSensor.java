/*
 * MissingMekSensor.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
package mekhq.campaign.parts;

import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingMekSensor extends MissingPart {
    private static final long serialVersionUID = 931907976883324097L;

    public MissingMekSensor() {
        this(0, null);
    }

    public MissingMekSensor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = resources.getString("MissingMekSensor.title");
    }

    @Override
    public int getBaseTime() {
        return 260;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public double getTonnage() {
        //TODO: what should this tonnage be?
        return 0;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // Do nothing - no fields to load.
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        return (part instanceof MekSensor) && (getUnitTonnage() == part.getUnitTonnage());
    }

    @Override
    public String checkFixable() {
        if (unit == null) {
            return null;
        }
        for (int i = 0; i < unit.getEntity().locations(); i++) {
            if (unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i) > 0) {
                if (unit.isLocationBreached(i)) {
                    return unit.getEntity().getLocationName(i) + " is breached.";
                } else if (unit.isLocationDestroyed(i)) {
                    return unit.getEntity().getLocationName(i) + " is destroyed.";
                }
            }
        }
        return null;
    }

    @Override
    public Part getNewPart() {
        return new MekSensor(getUnitTonnage(), campaign);
    }

    @Override
    public void updateConditionFromPart() {
        if (unit != null) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS);
        }
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public int getLocation() {
        if (unit != null) {
            Entity entity = unit.getEntity();
            for (int i = 0; i < entity.locations(); i++) {
                if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i) > 0) {
                    return i;
                }
            }
        }
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }

    @Override
    public boolean isInLocation(String loc) {
         if ((unit == null) || (unit.getEntity() == null) || !(unit.getEntity() instanceof Mech)) {
             return false;
         } else if (unit.getEntity().getLocationFromAbbr(loc) == Mech.LOC_HEAD) {
             return true;
         } else if (((Mech) unit.getEntity()).getCockpitType() == Mech.COCKPIT_TORSO_MOUNTED) {
             return unit.getEntity().getLocationFromAbbr(loc) == Mech.LOC_CT;
         }
         return false;
    }

    @Override
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
