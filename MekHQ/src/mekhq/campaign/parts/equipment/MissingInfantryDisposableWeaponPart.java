/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import megamek.common.equipment.EquipmentType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The "missing" / needs-acquisition counterpart of {@link InfantryDisposableWeaponPart}. It is the acquisition work
 * for buying a spare Disposable Weapon (TO:AuE p.116, Corrected Sixth Printing). One is created per missing trooper's
 * disposable, so a refit shops for one per trooper. It accepts a plain {@link EquipmentPart} of the same weapon type
 * as a replacement, because the parts store stocks a disposable weapon as a generic EquipmentPart - so existing
 * warehouse stock is consumed first.
 */
public class MissingInfantryDisposableWeaponPart extends MissingEquipmentPart {
    private static final MMLogger LOGGER = MMLogger.create(MissingInfantryDisposableWeaponPart.class);

    public MissingInfantryDisposableWeaponPart() {
        this(0, null, -1, null);
    }

    public MissingInfantryDisposableWeaponPart(int tonnage, EquipmentType equipmentType, int equipNum,
          Campaign campaign) {
        super(tonnage, equipmentType, equipNum, campaign,
              (equipmentType == null) ? 0 : equipmentType.getTonnage(null, 1.0), 1.0, false);
        if (equipmentType != null) {
            name = InfantryDisposableWeaponPart.buildDisposableName(equipmentType);
        }
    }

    @Override
    public void restore() {
        super.restore();
        if (type != null) {
            name = InfantryDisposableWeaponPart.buildDisposableName(type);
        }
    }

    @Override
    public InfantryDisposableWeaponPart getNewPart() {
        return new InfantryDisposableWeaponPart(getUnitTonnage(), type, -1, campaign);
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        // Match any spare weapon part of the same weapon type - the parts store stocks a disposable weapon as a plain
        // EquipmentPart, so existing warehouse stock (and freshly bought spares) satisfy the refit/replacement.
        // Internal name is the unique key equipment is registered under (EquipmentType.get), so IS and Clan variants
        // of a weapon - which register under different internal names - never cross-match.
        if (!(part instanceof EquipmentPart equipmentPart)) {
            return false;
        }
        boolean typesResolved = (type != null) && (equipmentPart.getType() != null);
        boolean isSameWeaponType = typesResolved
              && type.getInternalName().equals(equipmentPart.getType().getInternalName());
        return isSameWeaponType;
    }

    @Override
    public MissingInfantryDisposableWeaponPart clone() {
        MissingInfantryDisposableWeaponPart clone = new MissingInfantryDisposableWeaponPart(getUnitTonnage(), getType(),
              getEquipmentNum(), campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void writeToXML(final PrintWriter printWriter, int indent) {
        indent = writeToXMLBegin(printWriter, indent);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "equipTonnage", equipTonnage);
        writeToXMLEnd(printWriter, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            Node childNode = childNodes.item(index);
            String nodeName = childNode.getNodeName();
            try {
                if (nodeName.equalsIgnoreCase("equipmentNum")) {
                    equipmentNum = Integer.parseInt(childNode.getTextContent());
                } else if (nodeName.equalsIgnoreCase("typeName")) {
                    typeName = childNode.getTextContent();
                } else if (nodeName.equalsIgnoreCase("equipTonnage")) {
                    equipTonnage = Double.parseDouble(childNode.getTextContent());
                }
            } catch (Exception exception) {
                LOGGER.error(nodeName, exception);
            }
        }
        restore();
    }
}
