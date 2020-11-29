/*
 * TankLocation.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign.parts;

import java.io.PrintWriter;

import mekhq.campaign.finances.Money;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.IArmorState;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class TankLocation extends Part {
    private static final long serialVersionUID = -122291037522319765L;

    static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2460, 2470, 2510).setApproximate(true, false, false)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
            .setTechRating(RATING_D).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    protected int loc;
    protected int damage;
    protected boolean breached;

    public TankLocation() {
        this(0, 0, null);
    }

    public TankLocation clone() {
        TankLocation clone = new TankLocation(loc, getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        clone.loc = this.loc;
        clone.damage = this.damage;
        clone.breached = this.breached;
        return clone;
    }

    public int getLoc() {
        return loc;
    }

    public TankLocation(int loc, int tonnage, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.damage = 0;
        this.breached = false;
        this.name = "Tank Location";
        switch(loc) {
            case(Tank.LOC_FRONT):
                this.name = "Vehicle Front";
                break;
            case(Tank.LOC_LEFT):
                this.name = "Vehicle Left Side";
                break;
            case(Tank.LOC_RIGHT):
                this.name = "Vehicle Right Side";
                break;
            case(Tank.LOC_REAR):
                this.name = "Vehicle Rear";
                break;
        }
        computeCost();
    }

    protected void computeCost () {
        //TODO: implement
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof TankLocation
                && getLoc() == ((TankLocation)part).getLoc()
                && getUnitTonnage() == ((TankLocation)part).getUnitTonnage();
    }

    @Override
    public boolean isSameStatus(Part part) {
        return super.isSameStatus(part) && this.getDamage() == ((TankLocation)part).getDamage();
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<loc>"
                +loc
                +"</loc>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<damage>"
                +damage
                +"</damage>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<breached>"
                +breached
                +"</breached>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                loc = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("damage")) {
                damage = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("breached")) {
                breached = wn2.getTextContent().equalsIgnoreCase("true");
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if(isBreached()) {
            breached = false;
            if (null != unit) {
                unit.getEntity().setLocationStatus(loc, ILocationExposureStatus.NORMAL, true);
                for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                    CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                    // ignore empty & non-hittable slots
                    if (slot == null) {
                        continue;
                    }
                    slot.setBreached(false);
                    Mounted m = slot.getMount();
                    if(null != m) {
                        m.setBreached(false);
                    }
                }
            }
        } else {
            damage = 0;
            if(null != unit) {
                unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
            }
        }
    }

    @Override
    public MissingPart getMissingPart() {
        //cant replace locations
        return null;
    }

    @Override
    public void remove(boolean salvage) {
        if(null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if(!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if(null != spare) {
                spare.incrementQuantity();
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {
            if(IArmorState.ARMOR_DESTROYED == unit.getEntity().getInternal(loc)) {
                remove(false);
            } else {
                int originalInternal = unit.getEntity().getOInternal(loc);
                int internal = unit.getEntity().getInternal(loc);
                damage = originalInternal - Math.min(originalInternal, Math.max(internal, 0));
                if(unit.isLocationBreached(loc)) {
                    breached = true;
                }
            }
        }
    }

    @Override
    public int getBaseTime() {
        return 60;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    public boolean isBreached() {
        return breached;
    }

    @Override
    public boolean needsFixing() {
        return damage > 0 || breached;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (includeRepairDetails) {
            if(isBreached()) {
                return "Breached";
            } else {
                return  damage + " point(s) of damage";
            }
        } else {
            return super.getDetails(includeRepairDetails);
        }
    }

    @Override
    public void updateConditionFromPart() {
        ((Tank) unit.getEntity()).setInternal(((Tank) unit.getEntity()).getOInternal(loc) - damage, loc);
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    public String checkScrappable() {
        return "Vehicle locations cannot be scrapped";
    }

    @Override
    public boolean canNeverScrap() {
        return true;
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        // TODO Auto-generated method stub
        return Money.zero();
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        if(isBreached() && !isSalvaging()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "fixing breach");
        }
        return super.getAllMods(tech);
    }

    @Override
    public String getDesc() {
        if(!isBreached() || isSalvaging()) {
            return super.getDesc();
        }
        String toReturn = "<html><font size='2'";
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>Seal " + getName() + "</b><br/>";
        toReturn += getDetails() + "<br/>";
        toReturn += "" + getTimeLeft() + " minutes" + scheduled;
        toReturn += "</font></html>";
        return toReturn;
    }

     @Override
     public boolean isRightTechType(String skillType) {
         return skillType.equals(SkillType.S_TECH_MECHANIC);
     }

     public void doMaintenanceDamage(int d) {
         int points = unit.getEntity().getInternal(loc);
         points = Math.max(points -d, 1);
         unit.getEntity().setInternal(points, loc);
         updateConditionFromEntity(false);
     }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(loc) : null;
    }

    @Override
    public int getLocation() {
        return loc;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }

    @Override
    public int getMassRepairOptionType() {
        return Part.REPAIR_PART_TYPE.GENERAL_LOCATION;
    }
}
