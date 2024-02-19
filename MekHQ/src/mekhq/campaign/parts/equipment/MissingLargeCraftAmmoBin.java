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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts.equipment;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.WeaponMounted;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.Objects;

/**
 * @author cwspain
 */
public class MissingLargeCraftAmmoBin extends MissingAmmoBin {
    private int bayEqNum;

    private transient WeaponMounted bay;

    public MissingLargeCraftAmmoBin() {
        this(0, null, -1, 1.0, null);
    }

    public MissingLargeCraftAmmoBin(int tonnage, @Nullable AmmoType et, int equipNum, double capacity,
            @Nullable Campaign c) {
        super(tonnage, et, equipNum, false, false, c);
        this.size = capacity;
    }

    /**
     * @return The <code>Mounted</code> of the unit's <code>Entity</code> that contains this ammo bin,
     *         or null if there is no unit or the ammo bin is not in any bay.
     */
    public @Nullable WeaponMounted getBay() {
        if (getUnit() == null) {
            return null;
        } else if (bay != null) {
            return bay;
        }

        if (bayEqNum >= 0) {
            WeaponMounted m = (WeaponMounted) getUnit().getEntity().getEquipment(bayEqNum);

            if (getUnit().getEntity().whichBay(equipmentNum) == m) {
                bay = m;
                return bay;
            }
        }

        for (WeaponMounted m : getUnit().getEntity().getWeaponBayList()) {
            if (getUnit().getEntity().whichBay(equipmentNum) == m) {
                return m;
            }
        }

        LogManager.getLogger().warn("Could not find weapon bay for " + typeName + " for " + unit.getName());
        return null;
    }

    /**
     * Sets the bay for this ammo bin. Does not check whether the ammo bin is actually in the bay.
     * @param bay
     */
    public void setBay(WeaponMounted bay) {
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
        this.bayEqNum = bayEqNum;
        if (null != unit) {
            bay = (WeaponMounted) unit.getEntity().getEquipment(bayEqNum);
        }
    }

    @Override
    public double getTonnage() {
        return size;
    }

    public double getCapacity() {
        return size;
    }

    public void setCapacity(double capacity) {
        this.size = capacity;
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        // Do not try to replace a MissingLargeCraftAmmoBin with anything other
        // than an LargeCraftAmmoBin. Subclasses should use a similar check, which
        // breaks Composability to a degree but in this case we've used
        // subclasses where they're not truly composable.
        if (Objects.equals(part.getClass(), LargeCraftAmmoBin.class)) {
            LargeCraftAmmoBin ammoBin = (LargeCraftAmmoBin) part;
            return getType().equals(ammoBin.getType())
                    && (getFullShots() == ammoBin.getFullShots());
        }
        return false;
    }

    @Override
    protected int getFullShots() {
        return (int) Math.floor(getCapacity() * getType().getShots() / getType().getTonnage(null));
    }

    @Override
    public LargeCraftAmmoBin getNewPart() {
        return new LargeCraftAmmoBin(getUnitTonnage(), getType(), -1, getFullShots(), size, campaign);
    }

    @Override
    public void fix() {
        LargeCraftAmmoBin replacement = getNewPart();
        unit.addPart(replacement);
        campaign.getQuartermaster().addPart(replacement, 0);

        remove(false);

        // Add the replacement part to the unit
        replacement.setEquipmentNum(getEquipmentNum());
        replacement.setBay(bayEqNum);
        replacement.updateConditionFromPart();
    }

    @Override
    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bayEqNum", bayEqNum);
        super.writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        super.loadFieldsFromXmlNode(wn);
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("bayEqNum")) {
                    bayEqNum = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }
    }
}
