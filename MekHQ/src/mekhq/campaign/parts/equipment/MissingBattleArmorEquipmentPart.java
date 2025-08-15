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
import megamek.common.Mounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingBattleArmorEquipmentPart extends MissingEquipmentPart {
    private int trooper;

    public MissingBattleArmorEquipmentPart() {
        this(0, null, -1, 1.0, -1, null, 0.0);
    }

    public MissingBattleArmorEquipmentPart(int tonnage, EquipmentType et, int equipNum, double size,
          int trooper, Campaign c, double etonnage) {
        super(tonnage, et, equipNum, size, c, etonnage);
        this.trooper = trooper;
    }

    @Override
    public int getBaseTime() {
        return 30;
    }

    @Override
    public int getDifficulty() {
        return -2;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "size", size);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "trooper", trooper);
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
            } else if (wn2.getNodeName().equalsIgnoreCase("size")) {
                size = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("trooper")) {
                trooper = Integer.parseInt(wn2.getTextContent());
            }
        }
        restore();
    }

    public int getBaMountLocation() {
        if (null != unit) {
            Mounted<?> mounted = unit.getEntity().getEquipment(equipmentNum);
            if (null != mounted) {
                return mounted.getBaMountLoc();
            }
        }
        return -1;
    }

    private boolean isModular() {
        if (null == unit) {
            return false;
        }
        for (Mounted<?> m : unit.getEntity().getEquipment()) {
            if (m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_BA_MEA) &&
                      type instanceof MiscType && type.hasFlag(MiscType.F_BA_MANIPULATOR)
                      && this.getBaMountLocation() == m.getBaMountLoc()) {
                return true;
            }
            // this is not quite right, they must be linked somehow
            /*
             * if (type instanceof InfantryWeapon &&
             * m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_AP_MOUNT)
             * && this.getBaMountLocation()== m.getBaMountLoc()) {
             * return true;
             * }
             */
        }
        return false;
    }

    @Override
    public boolean needsFixing() {
        // can only be replaced the normal way if modular and suit exists
        if (null != unit && unit.getEntity().getInternal(trooper) >= 0 && isModular()) {
            return true;
        }
        return false;
    }

    public int getTrooper() {
        return trooper;
    }

    public void setTrooper(int t) {
        trooper = t;
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (null != replacement) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            ((EquipmentPart) actualReplacement).setEquipmentNum(equipmentNum);
            ((BattleArmorEquipmentPart) actualReplacement).setTrooper(trooper);
            remove(false);
            // assign the replacement part to the unit
            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if (part instanceof BattleArmorEquipmentPart) {
            BattleArmorEquipmentPart eqpart = (BattleArmorEquipmentPart) part;
            EquipmentType et = eqpart.getType();
            return type.equals(et) && (getTonnage() == part.getTonnage())
                         && (getSize() == ((BattleArmorEquipmentPart) part).getSize());
        }
        return false;
    }

    @Override
    public BattleArmorEquipmentPart getNewPart() {
        BattleArmorEquipmentPart epart = new BattleArmorEquipmentPart(getUnitTonnage(), type, -1, size, -1, campaign);
        epart.setEquipTonnage(equipTonnage);
        return epart;
    }

    @Override
    public void updateConditionFromPart() {
        // You can't crit BA equipment, so do nothing
    }

    @Override
    public int getLocation() {
        return trooper;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (null == unit) {
            return super.getDetails(includeRepairDetails);
        }
        String toReturn = unit.getEntity().getLocationName(trooper) + "<br>";
        return toReturn + super.getDetails(includeRepairDetails);
    }
}
