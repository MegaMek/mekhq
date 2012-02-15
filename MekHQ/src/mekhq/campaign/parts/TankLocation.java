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

import megamek.common.CriticalSlot;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class TankLocation extends Part {
	private static final long serialVersionUID = -122291037522319765L;
	protected int loc;
	protected int damage;
	protected boolean breached;

    public TankLocation() {
    	this(0, 0, null);
    }
    
    public TankLocation clone() {
    	TankLocation clone = new TankLocation(loc, getUnitTonnage(), campaign);
    	clone.loc = this.loc;
    	clone.damage = this.damage;
    	clone.breached = this.breached;
    	clone.time = this.time;
    	return clone;
    }
    
    public int getLoc() {
        return loc;
    }
    
    public TankLocation(int loc, int tonnage, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.damage = 0;
        this.time = 60;
        this.difficulty = 0;
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
    public boolean isSamePartTypeAndStatus (Part part) {
    	if(isReservedForRefit() || isBeingWorkedOn()
				|| part.isReservedForRefit() || part.isBeingWorkedOn()) {
    		return false;
    	}
        return part instanceof TankLocation 
        		&& getLoc() == ((TankLocation)part).getLoc() 
        		&& getUnitTonnage() == ((TankLocation)part).getUnitTonnage()
        		&& this.getDamage() == ((TankLocation)part).getDamage()
        		&& part.getSkillMin() == this.getSkillMin();
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
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					breached = true;
				else
					breached = false;
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_A;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_B;
	}

    @Override
	public int getTechLevel() {
		return TechConstants.T_INTRO_BOXSET;
	}

	@Override
	public void fix() {
		super.fix();
		if(isBreached()) {
			breached = false;
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
		} else {
		damage = 0;
			if(null != unit) {
				unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
			}
		}
	}

	@Override
	public Part getMissingPart() {
		//cant replace locations
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
		}
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			if(IArmorState.ARMOR_DESTROYED == unit.getEntity().getInternal(loc)) {
				remove(false);
			} else {
				damage = unit.getEntity().getOInternal(loc) - unit.getEntity().getInternal(loc);	
				if(unit.isLocationBreached(loc)) {
					breached = true;
				} 
			}
		}
		time = 60;
		difficulty = 0;
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
		if(isBreached()) {
			return "Breached";
		} else {
			return  damage + " point(s) of damage";
		}
    }

	@Override
	public void updateConditionFromPart() {
		//shouldn't get here
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
	public boolean canScrap() {
		return false;
	}

	@Override
	public double getTonnage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getStickerPrice() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public TargetRoll getAllMods() {
		if(isBreached() && !isSalvaging()) {
			return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "fixing breach");
		}
		return super.getAllMods();
	}
	
	@Override
	public String getDesc() {
		if(!isBreached() || isSalvaging()) {
			return super.getDesc();
		}
		String toReturn = "<html><font size='2'";
		String scheduled = "";
		if (getAssignedTeamId() != null) {
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
}
