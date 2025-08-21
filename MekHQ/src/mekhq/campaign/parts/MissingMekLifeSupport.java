/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts;

import megamek.common.CriticalSlot;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingMekLifeSupport extends MissingPart {
    public MissingMekLifeSupport() {
        this(0, null);
    }

    public MissingMekLifeSupport(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Mek Life Support SystemFluff";
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
            if (unit.getEntity().getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i) > 0) {
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
                if (entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i) > 0) {
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
            if (unit.getEntity().getLocationFromAbbr(loc) == Mek.LOC_LEFT_TORSO
                      || unit.getEntity().getLocationFromAbbr(loc) == Mek.LOC_RIGHT_TORSO) {
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
