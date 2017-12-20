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
import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.Mounted;
import megamek.common.annotations.Nullable;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.work.IAcquisitionWork;

/**
 * Ammo bin for a weapon bay that combines multiple tons of ammo into a single bin. Reload times
 * are calculated per ton, and a  reload tech action handles a single ton of ammo (or whatever the
 * smallest amount is for capital weapon ammo).
 * 
 * When the munition type is changed, fix actions diminish the capacity of this bay and add to
 * the capacity of the appropriate bin in the same bay.
 * 
 * @author Neoancient
 *
 */
public class LargeCraftAmmoBin extends AmmoBin {
    
    /**
     * 
     */
    private static final long serialVersionUID = -7931419849350769887L;
    
    private double capacity;
    private int bayEqNum;
    
    transient private Mounted bay;
    
    public LargeCraftAmmoBin() {
        this(0, null, -1, 0, 0, null);
    }
    
    public LargeCraftAmmoBin(int tonnage, EquipmentType et, int equipNum, int shotsNeeded, double capacity,
            Campaign c) {
        super(tonnage, et, equipNum, shotsNeeded, false, false, c);
        this.capacity = capacity;
    }

    @Override
    public LargeCraftAmmoBin clone() {
        LargeCraftAmmoBin clone = new LargeCraftAmmoBin(getUnitTonnage(), getType(), getEquipmentNum(),
                shotsNeeded, capacity, campaign);
        clone.copyBaseData(this);
        return clone;
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
    
    
    public int getBayEqNum() {
        return bayEqNum;
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
        return getCurrentShots() * type.getTonnage(null) / ((AmmoType) type).getShots();
    }
    
    public double getCapacity() {
        return capacity;
    }
    
    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }
    
    public double getUnusedCapacity() {
        return capacity - Math.ceil(getCurrentShots() * type.getTonnage(null) / ((AmmoType) type).getShots());
    }
    
    @Override
    public int getFullShots() {
        return (int) Math.floor(capacity * ((AmmoType) type).getShots() / type.getTonnage(null));
    }
    
    @Override
    public long getValueNeeded() {
        return adjustCostsForCampaignOptions((long)(capacity * getPricePerTon()
                * ((double)shotsNeeded / getShotsPerTon())));
    }

    @Override
    protected long getPricePerTon() {
        // Since ammo swaps are handled by moving capacity from one bay to another, the ammo type
        // of the bay and the unit should be the same.
        return (long) getType().getRawCost();
    }
    
    @Override
    public long getStickerPrice() {
        return (long)(capacity * getPricePerTon() * (1.0 * getCurrentShots()/getShotsPerTon()));
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentNum", equipmentNum);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "typeName", typeName);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "shotsNeeded", shotsNeeded);
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

    @Override
    public void fix() {
        if (shotsNeeded < 0) {
            unloadSingle();
        } else {
            loadBinSingle();
        }
    }
    
    public void loadBinSingle() {
        int shots = Math.min(getAmountAvailable(), Math.min(shotsNeeded, ((AmmoType) type).getShots()));
        if(null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted && mounted.getType() instanceof AmmoType) {
                mounted.setShotsLeft(mounted.getBaseShotsLeft() + shots);
            }
        }
        changeAmountAvailable(-1 * shots, (AmmoType)type);
        shotsNeeded -= shots;
    }

    public void unloadSingle() {
        int shots = Math.min(getCurrentShots(), ((AmmoType) type).getShots());
        AmmoType curType = (AmmoType)type;
        if (null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted && mounted.getType() instanceof AmmoType) {
                shots = Math.min(mounted.getBaseShotsLeft(), shots);
                mounted.setShotsLeft(mounted.getBaseShotsLeft() - shots);
                curType = (AmmoType)mounted.getType();
            }
        }
        shotsNeeded += shots;
        if(shots > 0) {
            changeAmountAvailable(shots, curType);
        }
    }
    
    @Override
    public MissingPart getMissingPart() {
        return null;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
                capacity = mounted.getAmmoCapacity();
                type = mounted.getType();
                if(mounted.isMissing() || mounted.isDestroyed()) {
                    mounted.setShotsLeft(0);
                    return;
                }
                shotsNeeded = getFullShots() - mounted.getBaseShotsLeft();
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 120;
        } else {
            return (int) Math.ceil(15 * type.getTonnage(null));
        }
    }

    @Override
    public void updateConditionFromPart() {
        if(null != unit) {
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if(null != mounted) {
                mounted.setHit(false);
                mounted.setDestroyed(false);
                mounted.setRepairable(true);
                mounted.changeAmmoType((AmmoType) type);
                unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNum);
                mounted.setShotsLeft(getFullShots() - shotsNeeded);
                mounted.setAmmoCapacity(capacity);
            }
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        return  part instanceof LargeCraftAmmoBin
                        && getType().equals( ((AmmoBin)part).getType() );
    }

    @Override
    public boolean needsFixing() {
        return (shotsNeeded < 0)
                || ((shotsNeeded > 0) && (type.getTonnage(null) <= bayAvailableCapacity())); 
    }

    /**
     * Check all the bins in the same bay that feed the same weapon(s) to determine whether there is
     * sufficient capacity to load more ammo into this bin. In the case of an ammo swap some ammo
     * may need to be removed from another bin in this bay before more can be loaded.
     *
     * @return The amount of unused capacity that can be used to reload ammo.
     */
    private double bayAvailableCapacity() {
        if (null != unit) {
            double space = 0.0;
            for (Part p : unit.getParts()) {
                if (p instanceof LargeCraftAmmoBin) {
                    final LargeCraftAmmoBin bin = (LargeCraftAmmoBin) p;
                    if ((bin.getBayEqNum() == bayEqNum)
                            && (((AmmoType) bin.getType()).getAmmoType() == ((AmmoType) type).getAmmoType())
                            && (((AmmoType) bin.getType()).getRackSize() == ((AmmoType) type).getRackSize())) {
                        space += bin.getCapacity() - bin.getTonnage();
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
        if (getTeamId() != null) {
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
        if (isSalvaging()) {
            return super.getDetails();
        }
        if (shotsNeeded < 0) {
            return ((AmmoType) type).getDesc() + ", " + (-shotsNeeded) + " shots to remove";
        }
        if (null != unit) {
            String availability = "";
            int shotsAvailable = getAmountAvailable();
            String[] inventories = campaign.getPartInventory(getNewPart());
            if(shotsAvailable == 0) {
                availability = "<br><font color='red'>No ammo ("+ inventories[1] + " in transit, " + inventories[2] + " on order)</font>";
            } else if(shotsAvailable < shotsNeeded) {
                availability = "<br><font color='red'>Only " + shotsAvailable + " available ("+ inventories[1] + " in transit, " + inventories[2] + " on order)</font>";
            }
            return ((AmmoType)type).getDesc() + ", " + shotsNeeded + " shots needed" + availability;
        } else {
            return "";
        }
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        int shots = 1;
        if (type instanceof AmmoType) {
            shots = ((AmmoType)type).getShots() * (int) Math.floor(getUnusedCapacity() / type.getTonnage(null));
        }
        return new AmmoStorage(1, type, shots, campaign);
    }

    @Override
    public boolean isOmniPoddable() {
        return false;
    }
    
}
