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
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MASC extends EquipmentPart {
    private static final MMLogger logger = MMLogger.create(MASC.class);

    protected int engineRating;

    public MASC() {
        this(0, null, -1, null, 0, false);
    }

    public MASC(int tonnage, EquipmentType et, int equipNum, Campaign c, int rating, boolean omniPodded) {
        super(tonnage, et, equipNum, 1.0, omniPodded, c);
        this.engineRating = rating;
        equipTonnage = calculateTonnage();
    }

    @Override
    public MASC clone() {
        MASC clone = new MASC(getUnitTonnage(), getType(), getEquipmentNum(), campaign, engineRating, omniPodded);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void setUnit(Unit u) {
        super.setUnit(u);
        if (null != unit && null != unit.getEntity().getEngine()) {
            engineRating = unit.getEntity().getEngine().getRating();
        }
    }

    private double calculateTonnage() {
        if (null == type) {
            return 0;
        }
        // supercharger tonnage will need to be set by hand in parts store
        if (isClan()) {
            return Math.round(getUnitTonnage() / 25.0f);
        }
        return Math.round(getUnitTonnage() / 20.0f);
    }

    @Override
    public Money getStickerPrice() {
        if (isSupercharger()) {
            return Money.of(engineRating * (isOmniPodded() ? 12500 : 10000));
        } else {
            return Money.of(engineRating * getTonnage() * 1000);
        }
    }

    public int getEngineRating() {
        return engineRating;
    }

    private boolean isSupercharger() {
        return type.hasSubType(MiscType.S_SUPERCHARGER);
    }

    @Override
    public boolean isSamePartTypeAndStatus(Part part) {
        if (needsFixing() || part.needsFixing()) {
            return false;
        }
        return part instanceof MASC
                && getType().equals(((EquipmentPart) part).getType())
                && getTonnage() == part.getTonnage()
                && getEngineRating() == ((MASC) part).getEngineRating();
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "engineRating", engineRating);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
                    equipmentNum = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
                    typeName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
                    equipTonnage = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("engineRating")) {
                    engineRating = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        restore();
    }

    @Override
    public MissingMASC getMissingPart() {
        return new MissingMASC(getUnitTonnage(), type, equipmentNum, campaign, equipTonnage, engineRating,
                omniPodded);
    }

    @Override
    public boolean isUnitTonnageMatters() {
        return !isSupercharger();
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringBuilder details = new StringBuilder();
        details.append(super.getDetails(includeRepairDetails));
        if (!details.isEmpty()) {
            details.append(", ");
        }
        if (isSupercharger()) {
            // Causes extra information but needed so omnipods show all data
            details.append(equipTonnage)
                .append(" tons, ");
        }
        details.append(getEngineRating())
            .append(" rating");
        return details.toString();
    }

    @Override
    public boolean isOmniPoddable() {
        return isSupercharger();
    }
}
