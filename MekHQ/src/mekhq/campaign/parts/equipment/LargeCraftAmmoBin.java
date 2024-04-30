/*
 * Copyright (c) 2017 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.CriticalSlot;
import megamek.common.Mounted;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * Ammo bin for a weapon bay that combines multiple tons of ammo into a single bin. Reload times
 * are calculated per ton, and a reload tech action handles a single ton of ammo (or whatever the
 * smallest amount is for capital weapon ammo).
 *
 * When the munition type is changed, fix actions diminish the capacity of this bay and add to
 * the capacity of the appropriate bin in the same bay.
 *
 * @author Neoancient
 */
public class LargeCraftAmmoBin extends AmmoBin {
    private int bayEqNum = -1;

    transient private Mounted bay;
    transient private double ammoTonnage;

    public LargeCraftAmmoBin() {
        this(0, null, -1, 0, 0, null);
    }

    public LargeCraftAmmoBin(int tonnage, @Nullable AmmoType et, int equipNum, int shotsNeeded, double capacity,
            @Nullable Campaign c) {
        super(tonnage, et, equipNum, shotsNeeded, false, false, c);
        this.size = capacity;
        this.ammoTonnage = (et != null) ? et.getTonnage(null) : 1.0;
    }

    @Override
    public LargeCraftAmmoBin clone() {
        LargeCraftAmmoBin clone = new LargeCraftAmmoBin(getUnitTonnage(), getType(), getEquipmentNum(),
                shotsNeeded, size, campaign);
        clone.copyBaseData(this);
        clone.bayEqNum = bayEqNum;
        return clone;
    }

    /**
     * @return The <code>Mounted</code> of the unit's <code>Entity</code> that contains this ammo bin,
     *         or null if there is no unit or the ammo bin is not in any bay.
     */
    public @Nullable Mounted getBay() {
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
     * Gets the equipment number of the bay to which this ammo bin is assigned,
     * otherwise {@code -1}
     */
    public int getBayEqNum() {
        return bayEqNum;
    }

    /**
     * Sets the bay for this ammo bin. Does not check whether the ammo bin is actually in the bay.
     * @param bay the bay that will contain this ammo bin
     */
    public void setBay(Mounted bay) {
        if (null != unit) {
            bayEqNum = unit.getEntity().getEquipmentNum(bay);
            this.bay = bay;
        }
    }

    /**
     * Sets the bay for this ammo bin. Does not check whether the ammo bin is actually in the bay.
     * @param bayEqNum the number of the bay that will contain this ammo bin
     */
    public void setBay(int bayEqNum) {
        this.bayEqNum = bayEqNum;
        if (getUnit() != null) {
            bay = unit.getEntity().getEquipment(bayEqNum);
        }
    }

    @Override
    public double getTonnage() {
        return getCapacity();
    }

    /**
     * Gets the capacity of the bay, in tons.
     */
    public double getCapacity() {
        return size;
    }

    /**
     * Gets the unused capacity of the bay, in tons.
     */
    public double getUnusedCapacity() {
        return getCapacity() - Math.ceil(getCurrentShots() * ammoTonnage / getType().getShots());
    }

    @Override
    public int getFullShots() {
        return (int) Math.floor(getCapacity() * getType().getShots() / ammoTonnage);
    }

    @Override
    public Money getValueNeeded() {
        if ((getShotsPerTon() <= 0) || (shotsNeeded <= 0)) {
            return Money.zero();
        }

        return adjustCostsForCampaignOptions(getPricePerTon()
                .multipliedBy(getCapacity())
                .multipliedBy(shotsNeeded)
                .dividedBy(getShotsPerTon()));
    }

    @Override
    protected Money getPricePerTon() {
        // Since ammo swaps are handled by moving capacity from one bay to another, the ammo type
        // of the bay and the unit should be the same.
        return Money.of(getType().getRawCost());
    }

    @Override
    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bayEqNum", bayEqNum);
        super.writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                // CAW: campaigns prior to 0.47.15 stored `size` in `capacity`
                if (wn2.getNodeName().equalsIgnoreCase("capacity")) {
                    size = Double.parseDouble(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("bayEqNum")) {
                    bayEqNum = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }

        super.loadFieldsFromXmlNode(wn);
    }

    @Override
    public void restore() {
        super.restore();

        if (getType() != null) {
            ammoTonnage = getType().getTonnage(null);
        }
    }

    @Override
    public void fix() {
        // We can only work on one ton at a time
        if (shotsNeeded < 0) {
            unloadSingleTon();
        } else {
            loadBinSingleTon();
        }
    }

    /**
     * Load a single ton of ammo into the bay.
     */
    public void loadBinSingleTon() {
        Mounted mounted = getMounted();
        if (mounted != null) {
            int shots = requisitionAmmo(getType(), Math.min(shotsNeeded, getType().getShots()));

            mounted.setShotsLeft(mounted.getBaseShotsLeft() + shots);

            shotsNeeded -= shots;
        }
    }

    /**
     * Unload a single ton of ammo from the bay.
     */
    public void unloadSingleTon() {
        int shots = Math.min(getCurrentShots(), getType().getShots());
        AmmoType curType = getType();

        Mounted mounted = getMounted();
        if (mounted != null) {
            shots = Math.min(mounted.getBaseShotsLeft(), shots);
            mounted.setShotsLeft(mounted.getBaseShotsLeft() - shots);
            curType = (AmmoType) mounted.getType();
        }

        shotsNeeded += shots;
        returnAmmo(curType, shots);
    }

    @Override
    public boolean isSalvaging() {
        return super.isSalvaging() && (getCurrentShots() > 0);
    }

    @Override
    public void remove(boolean salvage) {
        // The bin represents capacity rather than an actual part, and cannot be
        // removed or damaged.
        unload();
    }

    @Override
    public MissingAmmoBin getMissingPart() {
        // Large Craft Ammo Bins cannot be removed or destroyed
        return null;
    }

    @Override
    public boolean canNeverScrap() {
        // Large Craft Ammo Bins cannot be removed or destroyed
        return true;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        Mounted mounted = getMounted();
        if (mounted != null) {
            size = mounted.getSize();
            type = mounted.getType();
            if (mounted.isMissing() || mounted.isDestroyed()) {
                mounted.setShotsLeft(0);
                shotsNeeded = getFullShots();
                return;
            }

            shotsNeeded = getFullShots() - mounted.getBaseShotsLeft();
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 120;
        } else {
            //Capital Missiles take a flat 60m per missile per errata
            //Better set this for cruise missiles and screen launchers too.
            if (getType().hasFlag(AmmoType.F_CAP_MISSILE)
                    || getType().hasFlag(AmmoType.F_CRUISE_MISSILE)
                    || getType().hasFlag(AmmoType.F_SCREEN)) {
                return 60;
            }
            return (int) Math.ceil(15 * ammoTonnage);
        }
    }

    @Override
    public void updateConditionFromPart() {
        AmmoMounted mounted = (AmmoMounted) getMounted();
        if (mounted != null) {
            mounted.setHit(false);
            mounted.setDestroyed(false);
            mounted.setRepairable(true);
            mounted.changeAmmoType(getType());
            unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNum);
            mounted.setShotsLeft(getFullShots() - shotsNeeded);
            mounted.setSize(size);
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (getClass() == part.getClass())
                && getType().isCompatibleWith(((AmmoBin) part).getType());
    }

    @Override
    public boolean needsFixing() {
        return (shotsNeeded < 0)
                || ((shotsNeeded > 0) && (ammoTonnage <= Math.ceil(bayAvailableCapacity())));
    }

    /**
     * Check all the bins in the same bay that feed the same weapon(s) to determine whether there is
     * sufficient capacity to load more ammo into this bin. In the case of an ammo swap some ammo
     * may need to be removed from another bin in this bay before more can be loaded.
     *
     * @return The amount of unused capacity that can be used to reload ammo.
     */
    protected double bayAvailableCapacity() {
        if (null != unit) {
            double space = 0.0;
            for (Part p : unit.getParts()) {
                if (p instanceof LargeCraftAmmoBin) {
                    final LargeCraftAmmoBin bin = (LargeCraftAmmoBin) p;
                    if ((getBayEqNum() == bin.getBayEqNum())
                            && getType().equalsAmmoTypeOnly(bin.getType())
                            && (getType().getRackSize() == bin.getType().getRackSize())) {
                        space += bin.getUnusedCapacity();
                    }
                }
            }
            return space;
        }
        return 0.0;
    }

    @Override
    public String getDesc() {
        if (shotsNeeded >= 0) {
            return super.getDesc();
        }
        String toReturn = "<html><font size='2'";
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>Unload " + getName() + "</b><br/>";
        toReturn += getDetails() + "<br/>";
        toReturn += "" + getTimeLeft() + " minutes" + scheduled;
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (isSalvaging()) {
            return super.getDetails(includeRepairDetails);
        }
        if (shotsNeeded < 0) {
            return getType().getDesc() + ", " + (-shotsNeeded) + " shots to remove";
        }
        if (null != unit) {
            String availability = "";
            int shotsAvailable = getAmountAvailable();
            PartInventory inventories = campaign.getPartInventory(getNewPart());
            if (shotsAvailable == 0) {
                availability = "<br><font color='red'>No ammo ("+ inventories.getTransitOrderedDetails() + ")</font>";
            } else if (shotsAvailable < shotsNeeded) {
                availability = "<br><font color='red'>Only " + shotsAvailable + " available ("+ inventories.getTransitOrderedDetails() + ")</font>";
            }
            return getType().getDesc() + ", " + shotsNeeded + " shots needed" + availability;
        } else {
            return "";
        }
    }

    @Override
    public boolean isOmniPoddable() {
        return false;
    }
}
