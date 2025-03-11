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
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.annotations.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mek;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MekLifeSupport extends Part {
    public MekLifeSupport() {
        this(0, null);
    }

    public MekLifeSupport(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Mech Life Support System";
    }

    @Override
    public MekLifeSupport clone() {
        MekLifeSupport clone = new MekLifeSupport(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        // TODO: what should this tonnage be?
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(50000);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof MekLifeSupport;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // Do nothing - no fields to load.
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT);
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingMekLifeSupport(getUnitTonnage(), campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.incrementQuantity();
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit) {
            int priorHits = hits;
            Entity entity = unit.getEntity();
            for (int i = 0; i < entity.locations(); i++) {
                if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i) > 0) {
                    if (!unit.isSystemMissing(Mek.SYSTEM_LIFE_SUPPORT, i)) {
                        hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i);
                        break;
                    } else {
                        remove(false);
                        return;
                    }
                }
            }
            if (checkForDestruction
                    && hits > priorHits && hits >= 2
                    && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 180;
        } else if (hits > 1) {
            return 120;
        } else {
            return 60;
        }
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return -1;
        } else if (hits > 1) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT);
            } else {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, hits);
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if ((unit == null) || isSalvaging()) {
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
    public boolean isMountedOnDestroyedLocation() {
        if (null == unit) {
            return false;
        }

        for (int i = 0; i < unit.getEntity().locations(); i++) {
             if ((unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i) > 0)
                     && unit.isLocationDestroyed(i)) {
                 return true;
             }
         }
        return false;
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return false;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MEK);
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
