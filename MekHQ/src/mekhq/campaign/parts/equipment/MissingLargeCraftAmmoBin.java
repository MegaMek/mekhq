/*
 * Copyright (c) 2017 - The MegaMek Team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.AmmoType;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.annotations.Nullable;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;

/**
 * @author cwspain
 *
 */
public class MissingLargeCraftAmmoBin extends MissingAmmoBin {

    /**
     *
     */
    private static final long serialVersionUID = 1327103853526962103L;

    private double capacity;
    private int bayEqNum;

    private transient Mounted bay;

    public MissingLargeCraftAmmoBin() {
        this(0, null, -1, 1.0, null);
    }

    public MissingLargeCraftAmmoBin(int tonnage, EquipmentType et, int equipNum, double capacity,
            Campaign c) {
        super(tonnage, et, equipNum, false, false, c);
        this.capacity = capacity;
    }

    /**
     * @return The <code>Mounted</code> of the unit's <code>Entity</code> that contains this ammo bin,
     *         or null if there is no unit or the ammo bin is not in any bay.
     */
    public @Nullable Mounted getBay() {
        final String METHOD_NAME = "getBay()"; //$NON-NLS-1$
        if ((null != bay) || (null == unit)) {
            return null;
        }
        if (bayEqNum >= 0) {
            Mounted m = unit.getEntity().getEquipment(bayEqNum);
            if (m.getBayAmmo().contains(equipmentNum)) {
                bay = m;
                return bay;
            }
        }
        for (Mounted m : unit.getEntity().getWeaponBayList()) {
            if (m.getBayAmmo().contains(equipmentNum)) {
                return m;
            }
        }
        MekHQ.getLogger().log(LargeCraftAmmoBin.class, METHOD_NAME, LogLevel.WARNING,
                "Could not find weapon bay for " + typeName + " for " + unit.getName());
        return null;
    }

    /**
     * Sets the bay for this ammo bin. Does not check whether the ammo bin is actually in the bay.
     * @param bay
     */
    public void setBay(Mounted bay) {
        if (null != unit) {
            bayEqNum = unit.getEntity().getEquipmentNum(bay);
            this.bay = bay;
        }
    }

    /**
     * Sets the bay for this ammo bin. Does not check whether the ammo bin is actually in the bay.
     * @param bayEqNum
     */
    public void setBay(int bayEqNum) {
        if (null != unit) {
            this.bayEqNum = bayEqNum;
            bay = unit.getEntity().getEquipment(bayEqNum);
        }
    }

    @Override
    public double getTonnage() {
        return capacity;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        if (part instanceof LargeCraftAmmoBin) {
            EquipmentPart eqpart = (EquipmentPart)part;
            EquipmentType et = eqpart.getType();
            return type.equals(et) && ((AmmoBin)part).getFullShots() == getFullShots();
        }
        return false;
    }

    private int getFullShots() {
        return (int) Math.floor(capacity * ((AmmoType) type).getShots() / type.getTonnage(null));
    }

    @Override
    public Part getNewPart() {
        return new LargeCraftAmmoBin(getUnitTonnage(), type, -1, getFullShots(), capacity, campaign);
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentNum", equipmentNum);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "typeName", typeName);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "daysToWait", daysToWait);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "capacity", capacity);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "bayEqNum", bayEqNum);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        super.loadFieldsFromXmlNode(wn);
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("capacity")) {
                capacity = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("bayEqNum")) {
                bayEqNum = Integer.parseInt(wn2.getTextContent());
            }
        }
        restore();
    }
}
