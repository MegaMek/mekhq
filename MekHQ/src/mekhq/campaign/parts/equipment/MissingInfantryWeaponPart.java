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
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A conventional infantry weapon a platoon needs, such as the laser rifles a refit gives it. It is replaced by, and
 * bought as, an {@link InfantryWeaponPart}, so infantry weapons already in the warehouse can fill it and the weapons a
 * refit buys are the parts the platoon itself uses, not generic equipment the unit then duplicates.
 */
public class MissingInfantryWeaponPart extends MissingEquipmentPart {
    private boolean primary;

    public MissingInfantryWeaponPart() {
        this(0, null, -1, null, 0, false);
    }

    /**
     * @param tonnage      the platoon's tonnage
     * @param type         the weapon
     * @param equipNum     the weapon's equipment number on the platoon
     * @param campaign     the campaign
     * @param equipTonnage the weapon's weight
     * @param primary      {@code true} for the platoon's primary weapon, {@code false} for its secondary weapon
     */
    public MissingInfantryWeaponPart(int tonnage, EquipmentType type, int equipNum, Campaign campaign,
          double equipTonnage, boolean primary) {
        super(tonnage, type, equipNum, 1.0, campaign, equipTonnage);
        this.primary = primary;
    }

    public boolean isPrimary() {
        return primary;
    }

    @Override
    public InfantryWeaponPart getNewPart() {
        InfantryWeaponPart weapon = new InfantryWeaponPart(getUnitTonnage(), type, -1, campaign, primary);
        weapon.setEquipTonnage(equipTonnage);
        return weapon;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "size", size);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "primary", primary);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        super.loadFieldsFromXmlNode(wn);
        NodeList nodes = wn.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node.getNodeName().equalsIgnoreCase("primary")) {
                primary = Boolean.parseBoolean(node.getTextContent().trim());
            }
        }
    }
}
