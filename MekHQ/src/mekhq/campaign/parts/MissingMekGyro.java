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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Mek;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingMekGyro extends MissingPart {
    private static final MMLogger logger = MMLogger.create(MissingMekGyro.class);

    protected int type;
    protected double gyroTonnage;
    protected boolean isClan;

    public MissingMekGyro() {
        this(0, 0, 0, false, null);
    }

    public MissingMekGyro(int tonnage, int type, double gyroTonnage, boolean isClan, Campaign c) {
        super(tonnage, c);
        this.type = type;
        this.name = Mek.getGyroTypeString(type);
        this.gyroTonnage = gyroTonnage;
        this.isClan = isClan;
    }

    @Override
    public int getBaseTime() {
        return 200;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    public int getType() {
        return type;
    }

    @Override
    public double getTonnage() {
        return gyroTonnage;
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

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            int walkMP = -1;
            try {
                if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    type = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("gyroTonnage")) {
                    gyroTonnage = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("walkMP")) {
                    walkMP = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }

            if (walkMP > -1) {
                // need to calculate gyroTonnage for reverse compatibility
                gyroTonnage = MekGyro.getGyroTonnage(walkMP, type, getUnitTonnage());
            }
        }
    }

    @Override
    public int getTechRating() {
        switch (type) {
            case Mek.GYRO_COMPACT:
            case Mek.GYRO_HEAVY_DUTY:
            case Mek.GYRO_XL:
                return EquipmentType.RATING_E;
            default:
                return EquipmentType.RATING_D;
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if (part instanceof MekGyro) {
            MekGyro gyro = (MekGyro) part;
            return getType() == gyro.getType() && getTonnage() == gyro.getTonnage();
        }
        return false;
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (unit.isLocationBreached(Mek.LOC_CT)) {
            return unit.getEntity().getLocationName(Mek.LOC_CT) + " is breached.";
        }
        return null;
    }

    @Override
    public Part getNewPart() {
        return new MekGyro(getUnitTonnage(), getType(), getTonnage(), isClan, campaign);
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, Mek.LOC_CT);
        }
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return Mek.LOC_CT;
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
