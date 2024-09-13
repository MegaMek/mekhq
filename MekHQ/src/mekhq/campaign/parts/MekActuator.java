/*
 * MekActuator.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.SkillType;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.StringJoiner;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MekActuator extends Part {
    static final TechAdvancement TA_STANDARD = new TechAdvancement(TECH_BASE_ALL).setAdvancement(2300, 2350, 2505)
            .setApproximate(true, false, false).setPrototypeFactions(F_TA).setProductionFactions(F_TH)
            .setStaticTechLevel(SimpleTechLevel.INTRO);
    static final TechAdvancement TA_SUPERHEAVY = new TechAdvancement(TECH_BASE_IS).setAdvancement(2905, 2940, 3076)
            .setApproximate(true, false, false).setPrototypeFactions(F_FW).setProductionFactions(F_FW)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    protected int type;
    protected int location;

    public MekActuator() {
        this(0, 0, null);
    }

    @Override
    public MekActuator clone() {
        MekActuator clone = new MekActuator(getUnitTonnage(), type, location, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    public int getType() {
        return type;
    }

    public void setLocation(int loc) {
        this.location = loc;
    }

    public MekActuator(int tonnage, int type, Campaign c) {
        this(tonnage, type, -1, c);
    }

    public MekActuator(int tonnage, int type, int loc, Campaign c) {
        super(tonnage, c);
        this.type = type;
        Mek m = new BipedMek();
        this.name = m.getSystemName(type) + " Actuator";
        this.location = loc;
    }

    @Override
    public double getTonnage() {
        // TODO: how much do actuators weight?
        // apparently nothing
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        double unitCost = 0;
        switch (getType()) {
            case Mek.ACTUATOR_UPPER_ARM:
                unitCost = 100;
                break;
            case Mek.ACTUATOR_LOWER_ARM:
                unitCost = 50;
                break;
            case Mek.ACTUATOR_HAND:
            case Mek.ACTUATOR_LOWER_LEG:
                unitCost = 80;
                break;
            case Mek.ACTUATOR_UPPER_LEG:
                unitCost = 150;
                break;
            case Mek.ACTUATOR_FOOT:
                unitCost = 120;
                break;
            case Mek.ACTUATOR_HIP:
            case Mek.ACTUATOR_SHOULDER:
                unitCost = 0;
                break;
        }
        return Money.of(getUnitTonnage() * unitCost);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof MekActuator && getType() == ((MekActuator) part).getType()
                && getUnitTonnage() == part.getUnitTonnage();
    }

    @Override
    public int getLocation() {
        return location;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "location", location);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("type")) {
                type = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("location")) {
                location = Integer.parseInt(wn2.getTextContent());
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, type, location);
        }
    }

    @Override
    public int getTechLevel() {
        return TechConstants.T_ALLOWED_ALL;
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingMekActuator(getUnitTonnage(), type, location, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, type, location);
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
        location = -1;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit) {
            // check for missing equipment
            if (unit.isSystemMissing(type, location)) {
                remove(false);
                return;
            }
            hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, type, location);
            if (checkForDestruction && hits > priorHits
                    && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return isOmniPodded() ? 30 : 90;
        }
        return 120;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return -3;
        }
        return 0;
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
            StringJoiner sj = new StringJoiner(", ");
            if (!StringUtils.isEmpty(getLocationName())) {
                sj.add(getLocationName());
            }
            if (includeRepairDetails && campaign.getCampaignOptions().isPayForRepairs()) {
                sj.add(getActualValue().multipliedBy(0.2).toAmountAndSymbolString() + " to repair");
            }
            return sj.toString();
        }
        return getUnitTonnage() + " tons";
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits > 0) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, type, location, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, type, location);
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
        if (unit.isLocationBreached(location)) {
            return unit.getEntity().getLocationName(location) + " is breached.";
        }
        if (isMountedOnDestroyedLocation()) {
            return unit.getEntity().getLocationName(location) + " is destroyed.";
        }
        return null;
    }

    @Override
    public boolean isMountedOnDestroyedLocation() {
        return null != unit && unit.isLocationDestroyed(location);
    }

    @Override
    public boolean onBadHipOrShoulder() {
        return null != unit && unit.hasBadHipOrShoulder(location);
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
    public boolean isOmniPoddable() {
        return type == Mek.ACTUATOR_LOWER_ARM || type == Mek.ACTUATOR_HAND;
    }

    @Override
    public boolean isOmniPodded() {
        return isOmniPoddable() && getUnit() != null && getUnit().getEntity().isOmni();
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(location) : null;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return (getUnitTonnage() <= 100) ? TA_STANDARD : TA_SUPERHEAVY;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ACTUATOR;
    }
}
