/*
 * ProtomekActuator.java
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

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.ProtoMek;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.SkillType;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ProtoMekArmActuator extends Part {
    private static final MMLogger logger = MMLogger.create(ProtoMekArmActuator.class);

    protected int location;

    public ProtoMekArmActuator() {
        this(0, 0, null);
    }

    @Override
    public ProtoMekArmActuator clone() {
        ProtoMekArmActuator clone = new ProtoMekArmActuator(getUnitTonnage(), location, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    public ProtoMekArmActuator(int tonnage, Campaign c) {
        this(tonnage, -1, c);
    }

    public ProtoMekArmActuator(int tonnage, int loc, Campaign c) {
        super(tonnage, c);
        this.name = "ProtoMek Arm Actuator";
        this.location = loc;
        this.unitTonnageMatters = true;
    }

    public void setLocation(int loc) {
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
        return Money.of(getUnitTonnage() * 180);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof ProtoMekArmActuator
                && getUnitTonnage() == part.getUnitTonnage();
    }

    @Override
    public int getLocation() {
        return location;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "location", location);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("location")) {
                    location = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARMCRIT, location);
        }
    }

    @Override
    public int getTechBase() {
        return T_CLAN;
    }

    @Override
    public int getTechLevel() {
        return TechConstants.T_CLAN_TW;
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingProtoMekArmActuator(getUnitTonnage(), location, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            int h = Math.max(1, hits);
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARMCRIT, location, h);
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
        if (null != unit) {
            int priorHits = hits;
            hits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARMCRIT, location);
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
            return unit.getEntity().getLocationName(location);
        }
        return getUnitTonnage() + " tons";
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits > 0) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARMCRIT, location, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARMCRIT, location);
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
        return false;
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return false;// index == type && loc == location;
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
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(location) : null;
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
