/*
 * Copyright (c) 2020 - The MegaMek Team
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

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * Ammo bin missing from a small support vehicle
 */
public class MissingInfantryAmmoBin extends MissingAmmoBin {

    private InfantryWeapon weaponType;

    // Used in deserialization
    @SuppressWarnings("unused")
    public MissingInfantryAmmoBin() {
        this(0, null, 0, null, 0, false, null);
    }

    /**
     * Construct a new placeholder for a missing infantry ammo bin
     *
     * @param tonnage     The weight of the unit it's installed on
     * @param ammoType    The type of ammo
     * @param equipNum    The equipment index on the unit
     * @param weaponType  The weapon this ammo is for
     * @param omniPodded  Whether the weapon is pod-mounted on an omnivehicle
     * @param c           The campaign instance
     */
    public MissingInfantryAmmoBin(int tonnage, EquipmentType ammoType, int equipNum, InfantryWeapon weaponType,
                                  double size, boolean omniPodded, Campaign c) {
        super(tonnage, ammoType, equipNum, false, omniPodded, c);
        this.weaponType = weaponType;
        this.size = size;
        if (weaponType != null) {
            name = weaponType.getName() + " Ammo Bin";
        }
    }

    @Override
    public void restore() {
        super.restore();
        name = weaponType.getName() + " Ammo Bin";
    }

    public InfantryWeapon getWeaponType() {
        return weaponType;
    }

    @Override
    public String getLocationName() {
        int loc = getLocation();
        if ((loc >= 0) && (loc < unit.getEntity().locations())) {
            return unit.getEntity().getLocationName(loc);
        } else {
            return null;
        }
    }

    @Override
    public int getLocation() {
        if (unit != null) {
            Mounted m = unit.getEntity().getEquipment(equipmentNum);
            while (m.getLinkedBy() != null) {
                m = m.getLinkedBy();
            }
            return m.getLocation();
        }
        return Entity.LOC_NONE;
    }

    protected Part getActualReplacement(AmmoBin found) {
        //Check to see if munition types are different
        if (getType() == found.getType()) {
            return found.clone();
        } else {
            return new InfantryAmmoBin(getUnitTonnage(), getType(), getEquipmentNum(),
                    getFullShots(), getWeaponType(), getSize(), isOmniPodded(), campaign);
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if (part instanceof InfantryAmmoBin) {
            InfantryAmmoBin bin = (InfantryAmmoBin) part;
            return type.equals(bin.getType()) && weaponType.equals(bin.getWeaponType())
                    && (bin.getFullShots() == getFullShots());
        }
        return false;
    }

    @Override
    protected int getFullShots() {
        return weaponType.getShots() * (int) size;
    }

    @Override
    public Part getNewPart() {
        return new InfantryAmmoBin(getUnitTonnage(), type, -1, getFullShots(),
                weaponType, size, omniPodded, campaign);
    }

    @Override
    public void writeToXmlEnd(PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, "weaponType", weaponType.getInternalName());
        super.writeToXmlEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node node) {
        NodeList nl = node.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            if (wn.getNodeName().equals("weaponType")) {
                this.weaponType = (InfantryWeapon) EquipmentType.get(wn.getTextContent().trim());
            }
        }
    }

}
