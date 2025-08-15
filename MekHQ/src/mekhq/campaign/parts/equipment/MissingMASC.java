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
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import megamek.common.EquipmentType;
import megamek.common.MiscType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingMASC extends MissingEquipmentPart {
    protected int engineRating;

    public MissingMASC() {
        this(0, null, -1, null, 0, 0, false);
    }

    public MissingMASC(int tonnage, EquipmentType et, int equipNum, Campaign c, double etonnage,
          int rating, boolean omniPodded) {
        super(tonnage, et, equipNum, c, etonnage, 1.0, omniPodded);
        this.engineRating = rating;
    }

    @Override
    public void setUnit(Unit u) {
        super.setUnit(u);
        if (null != unit && null != unit.getEntity().getEngine()) {
            engineRating = unit.getEntity().getEngine().getRating();
        }
    }

    @Override
    public Money getStickerPrice() {
        if (isSupercharger()) {
            return Money.of(engineRating * 10000);
        } else {
            return Money.of(engineRating * getTonnage() * 1000);
        }
    }

    public int getEngineRating() {
        return engineRating;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", typeName);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "engineRating", engineRating);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
                equipmentNum = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
                typeName = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
                equipTonnage = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("engineRating")) {
                engineRating = Integer.parseInt(wn2.getTextContent());
            }
        }
        restore();
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if (part instanceof MASC) {
            EquipmentPart eqpart = (EquipmentPart) part;
            EquipmentType et = eqpart.getType();
            return type.equals(et) && getTonnage() == part.getTonnage()
                         && ((MASC) part).getEngineRating() == engineRating;
        }
        return false;
    }

    private boolean isSupercharger() {
        return type.hasSubType(MiscType.S_SUPERCHARGER);
    }

    @Override
    public boolean isUnitTonnageMatters() {
        return !isSupercharger();
    }

    @Override
    public MASC getNewPart() {
        MASC epart = new MASC(getUnitTonnage(), type, -1, campaign, engineRating, omniPodded);
        epart.setEquipTonnage(equipTonnage);
        return epart;
    }

    @Override
    public boolean isOmniPoddable() {
        return isSupercharger();
    }
}
