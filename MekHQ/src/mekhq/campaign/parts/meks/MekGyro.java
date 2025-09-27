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
package mekhq.campaign.parts.meks;

import java.io.PrintWriter;

import megamek.common.CriticalSlot;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.missing.MissingMekGyro;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MekGyro extends Part {
    private static final MMLogger LOGGER = MMLogger.create(MekGyro.class);

    protected int type;
    protected double gyroTonnage;
    protected boolean isClan;

    public MekGyro() {
        this(0, 0, 0, false, null);
    }

    public MekGyro(int tonnage, int type, int walkMP, boolean isClan, Campaign c) {
        this(tonnage, type, MekGyro.getGyroTonnage(walkMP, type, tonnage), isClan, c);
    }

    public MekGyro(int tonnage, int type, double gyroTonnage, boolean isClan, Campaign c) {
        super(tonnage, c);
        this.type = type;
        this.name = Mek.getGyroTypeString(type);
        this.gyroTonnage = gyroTonnage;
        this.isClan = isClan;
    }

    @Override
    public MekGyro clone() {
        MekGyro clone = new MekGyro(getUnitTonnage(), type, gyroTonnage, isClan, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    public int getType() {
        return type;
    }

    public static int getGyroBaseTonnage(int walkMP, int unitTonnage) {
        return (int) Math.ceil(walkMP * unitTonnage / 100f);
    }

    public static double getGyroTonnage(int walkMP, int gyroType, int unitTonnage) {
        int gyroBaseTonnage = MekGyro.getGyroBaseTonnage(walkMP, unitTonnage);
        if (gyroType == Mek.GYRO_XL) {
            return gyroBaseTonnage * 0.5;
        } else if (gyroType == Mek.GYRO_COMPACT) {
            return gyroBaseTonnage * 1.5;
        } else if (gyroType == Mek.GYRO_HEAVY_DUTY) {
            return gyroBaseTonnage * 2;
        }

        return gyroBaseTonnage;
    }

    @Override
    public double getTonnage() {
        return gyroTonnage;
    }

    @Override
    public Money getStickerPrice() {
        if (getType() == Mek.GYRO_XL) {
            return Money.of(750000.0 * getTonnage());
        } else if (getType() == Mek.GYRO_COMPACT) {
            return Money.of(400000.0 * getTonnage());
        } else if (getType() == Mek.GYRO_HEAVY_DUTY) {
            return Money.of(500000.0 * getTonnage());
        } else {
            return Money.of(300000.0 * getTonnage());
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof MekGyro && getType() == ((MekGyro) part).getType()
                     && getTonnage() == part.getTonnage();
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "gyroTonnage", gyroTonnage);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        int walkMP = -1;
        int uTonnage = 0;
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    type = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("gyroTonnage")) {
                    gyroTonnage = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("walkMP")) {
                    walkMP = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("unitTonnage")) {
                    uTonnage = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
        if (gyroTonnage == 0) {
            // need to calculate gyroTonnage for reverse compatability
            gyroTonnage = MekGyro.getGyroTonnage(walkMP, type, uTonnage);
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, Mek.LOC_CENTER_TORSO);
        }

    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingMekGyro(getUnitTonnage(), getType(), getTonnage(), isClan, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, Mek.LOC_CENTER_TORSO);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.changeQuantity(1);
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0, false);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit) {
            int priorHits = hits;
            hits = unit.getEntity()
                         .getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, Mek.LOC_CENTER_TORSO);
            if (checkForDestruction && hits > priorHits && hits >= 3
                      && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 200;
        }
        if (hits >= 2) {
            return 240;
        }
        return 120;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 0;
        }
        if (hits >= 2) {
            return 4;
        }
        return 1;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, Mek.LOC_CENTER_TORSO);
            } else {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, hits);
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (!isSalvaging() && unit.isLocationBreached(Mek.LOC_CENTER_TORSO)) {
            return unit.getEntity().getLocationName(Mek.LOC_CENTER_TORSO) + " is breached.";
        }
        return null;
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
        return Mek.LOC_CENTER_TORSO;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return Mek.getGyroTechAdvancement(type);
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.GYRO;
    }
}
