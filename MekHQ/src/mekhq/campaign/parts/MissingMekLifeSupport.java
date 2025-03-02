/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.parts;

import megamek.common.annotations.Nullable;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mek;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingMekLifeSupport extends MissingPart {
    public MissingMekLifeSupport() {
        this(0, null);
    }

    public MissingMekLifeSupport(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Mek Life Support System";
    }

    @Override
    public int getBaseTime() {
        return 180;
    }

    @Override
    public int getDifficulty() {
        return -1;
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
        return part instanceof MekLifeSupport;
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        for (int i = 0; i < unit.getEntity().locations(); i++) {
            if (unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i) > 0) {
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
        return new MekLifeSupport(getUnitTonnage(), campaign);
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT);
        }
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        if (null != unit) {
            Entity entity = unit.getEntity();
            for (int i = 0; i < entity.locations(); i++) {
                if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i) > 0) {
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
         if (null == unit || null == unit.getEntity() || !(unit.getEntity() instanceof Mek)) {
             return false;
         }
         if (((Mek) unit.getEntity()).getCockpitType() == Mek.COCKPIT_TORSO_MOUNTED) {
             if (unit.getEntity().getLocationFromAbbr(loc) == Mek.LOC_LT
                     || unit.getEntity().getLocationFromAbbr(loc) == Mek.LOC_RT) {
                 return true;
             }
         } else if (unit.getEntity().getLocationFromAbbr(loc) == Mek.LOC_HEAD) {
             return true;
         }
         return false;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
