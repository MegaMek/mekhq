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
 * The "missing" / needs-acquisition counterpart of {@link InfantryDisposableWeaponPart}. It is the acquisition work for
 * buying a spare Disposable Weapon (TO:AR p.106). One is created per missing trooper's disposable, so a refit shops for
 * one per trooper. It accepts a plain {@link EquipmentPart} of the same weapon type as a replacement, because the parts
 * store stocks a disposable weapon as a generic EquipmentPart - so existing warehouse stock is consumed first.
 */
public class MissingInfantryDisposableWeaponPart extends MissingEquipmentPart {
    private static final MMLogger LOGGER = MMLogger.create(MissingInfantryDisposableWeaponPart.class);
    private static final String DISPOSABLE_SUFFIX = " (Disposable)";

    public MissingInfantryDisposableWeaponPart() {
        this(0, null, -1, null);
    }

    public MissingInfantryDisposableWeaponPart(int tonnage, EquipmentType et, int equipNum, Campaign c) {
        super(tonnage, et, equipNum, c, (et == null) ? 0 : et.getTonnage(null, 1.0), 1.0, false);
        if (et != null) {
            name = et.getName() + DISPOSABLE_SUFFIX;
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
        return (part instanceof EquipmentPart equipmentPart)
              && (type != null)
              && (equipmentPart.getType() != null)
              && type.getInternalName().equals(equipmentPart.getType().getInternalName());
    }

    @Override
    public MissingInfantryDisposableWeaponPart clone() {
        MissingInfantryDisposableWeaponPart clone = new MissingInfantryDisposableWeaponPart(getUnitTonnage(), getType(),
              getEquipmentNum(), campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
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
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
        restore();
    }
}
