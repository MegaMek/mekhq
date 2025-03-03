/*
 * InfantryWeapon.java
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
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class InfantryWeaponPart extends EquipmentPart {
    private static final MMLogger logger = MMLogger.create(InfantryWeaponPart.class);

    private boolean primary;

    public InfantryWeaponPart() {
        this(0, null, -1, null, false);
    }

    public InfantryWeaponPart(int tonnage, EquipmentType et, int equipNum, Campaign c, boolean p) {
        super(tonnage, et, equipNum, 1.0, c);
        primary = p;
    }

    @Override
    public InfantryWeaponPart clone() {
        InfantryWeaponPart clone = new InfantryWeaponPart(getUnitTonnage(), getType(), getEquipmentNum(), campaign,
                primary);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public MissingEquipmentPart getMissingPart() {
        // shouldn't get here, but ok
        return new MissingEquipmentPart(getUnitTonnage(), type, equipmentNum, size, campaign, getTonnage());
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "primary", primary);
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
                } else if (wn2.getNodeName().equalsIgnoreCase("primary")) {
                    primary = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        restore();
    }

    public boolean isPrimary() {
        return primary;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.WEAPON;
    }
}
