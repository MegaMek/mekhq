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
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The "missing" / needs-acquisition counterpart of {@link InfantryDisposableWeaponPart}. It is the acquisition work for
 * buying a spare Disposable Weapon (TO:AR p.106) loadout, and {@link #getNewPart()} produces the correctly-typed spare
 * so a fired platoon's disposables can be reloaded from warehouse stock.
 */
public class MissingInfantryDisposableWeaponPart extends MissingEquipmentPart {
    private static final MMLogger LOGGER = MMLogger.create(MissingInfantryDisposableWeaponPart.class);
    private static final String DISPOSABLE_SUFFIX = " (Disposable)";

    private int troopers;

    public MissingInfantryDisposableWeaponPart() {
        this(0, null, -1, 0, null);
    }

    public MissingInfantryDisposableWeaponPart(int tonnage, EquipmentType et, int equipNum, int troopers, Campaign c) {
        super(tonnage, et, equipNum, c, (et == null) ? 0 : et.getTonnage(null, 1.0), 1.0, false);
        this.troopers = troopers;
        if (et != null) {
            name = et.getName() + DISPOSABLE_SUFFIX;
        }
    }

    public int getTroopers() {
        return troopers;
    }

    @Override
    public InfantryDisposableWeaponPart getNewPart() {
        return new InfantryDisposableWeaponPart(getUnitTonnage(), type, -1, troopers, campaign);
    }

    @Override
    public MissingInfantryDisposableWeaponPart clone() {
        MissingInfantryDisposableWeaponPart clone = new MissingInfantryDisposableWeaponPart(getUnitTonnage(), getType(),
              getEquipmentNum(), troopers, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "troopers", troopers);
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
                } else if (wn2.getNodeName().equalsIgnoreCase("troopers")) {
                    troopers = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
        restore();
    }
}
