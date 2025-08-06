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

import java.io.PrintWriter;

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.ProtoMek;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.skills.SkillType;
import org.w3c.dom.Node;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ProtoMekLegActuator extends Part {
    public ProtoMekLegActuator() {
        this(0, null);
    }

    @Override
    public ProtoMekLegActuator clone() {
        ProtoMekLegActuator clone = new ProtoMekLegActuator(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
    }


    public ProtoMekLegActuator(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "ProtoMek Leg Actuator";
        this.unitTonnageMatters = true;
    }

    @Override
    public double getTonnage() {
        //TODO: how much do actuators weight?
        //apparently nothing
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(getUnitTonnage() * 540);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof ProtoMekLegActuator
                     && getUnitTonnage() == part.getUnitTonnage();
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEGCRIT, ProtoMek.LOC_LEG);
        }
    }

    @Override
    public TechBase getTechBase() {
        return TechBase.CLAN;
    }

    @Override
    public int getTechLevel() {
        return TechConstants.T_CLAN_TW;
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingProtoMekLegActuator(getUnitTonnage(), campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            int h = Math.max(2, hits);
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEGCRIT, ProtoMek.LOC_LEG, h);
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
            hits = unit.getEntity()
                         .getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEGCRIT, ProtoMek.LOC_LEG);
            if (checkForDestruction
                      && hits > priorHits
                      && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 120;
        } else if (hits <= 1) {
            return 100;
        } else if (hits == 2) {
            return 150;
        } else {
            return 200;
        }
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 0;
        } else if (hits <= 1) {
            return 0;
        } else if (hits == 2) {
            return 1;
        } else {
            return 3;
        }
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (null != unit) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_LEG);
        }
        return getUnitTonnage() + " tons";
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits > 0) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEGCRIT, ProtoMek.LOC_LEG, hits);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEGCRIT, ProtoMek.LOC_LEG);
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (isSalvaging()) {
            return null;
        }
        if (unit.isLocationBreached(ProtoMek.LOC_LEG)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_LEG) + " is breached.";
        }
        if (isMountedOnDestroyedLocation()) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_LEG) + " is destroyed.";
        }
        return null;
    }

    @Override
    public boolean isMountedOnDestroyedLocation() {
        return null != unit && unit.isLocationDestroyed(ProtoMek.LOC_LEG);
    }

    @Override
    public boolean onBadHipOrShoulder() {
        return false;
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return false;//index == type && loc == location;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MEK);
    }

    @Override
    public boolean isOmniPoddable() {
        return false;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {

    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(getLocation()) : null;
    }

    @Override
    public int getLocation() {
        return ProtoMek.LOC_LEG;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return ProtoMekLocation.TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ACTUATOR;
    }
}
