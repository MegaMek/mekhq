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

import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.Tank;
import megamek.common.VTOL;
import mekhq.campaign.MekHqXmlUtil;

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

    public TankLocation() {
    	this(0, 0);
    }
    
    public int getLoc() {
        return loc;
    }
    
    public TankLocation(int loc, int tonnage) {
        super(tonnage);
        this.loc = loc;
        this.damage = 0;
        this.time = 60;
        this.difficulty = 0;
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
            case(Tank.LOC_TURRET):
            case(Tank.LOC_TURRET_2):
                this.name = "Vehicle Turret";
                break;
        }
        computeCost();
    }
    
    protected void computeCost () {
    	//TODO: implement
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
    	if(needsFixing() || part.needsFixing()) {
    		return false;
    	}
        return part instanceof TankLocation && getLoc() == ((TankLocation)part).getLoc() && getUnitTonnage() == ((TankLocation)part).getUnitTonnage();
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<damage>"
				+damage
				+"</damage>");
		writeToXmlEnd(pw1, indent, id);
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
	public void fix() {
		damage = 0;
		if(null != unit) {
			unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
		}
	}

	@Override
	public Part getMissingPart() {
		//this can only be a turret
		return new MissingTurret(getUnitTonnage());
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
			if(!salvage) {
				unit.campaign.removePart(this);
			}
			unit.removePart(this);
			if(loc == Tank.LOC_TURRET || loc == Tank.LOC_TURRET_2) {
				Part missing = getMissingPart();
				unit.campaign.addPart(missing);
				unit.addPart(missing);
			}
		}
		setUnit(null);
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit) {
			if(IArmorState.ARMOR_DESTROYED == unit.getEntity().getInternal(loc)) {
				remove(false);
			} else {
				damage = unit.getEntity().getOInternal(loc) - unit.getEntity().getInternal(loc);			
			}
		}
		if(isSalvaging()) {
			this.time = 160;
			this.difficulty = -1;
		}
	}

	@Override
	public boolean needsFixing() {
		return damage > 0;
	}
	
	@Override
    public String getDetails() {
		return damage + " point(s) of damage";
    }

	@Override
	public void updateConditionFromPart() {
		if(null != unit) {
			unit.getEntity().setInternal(unit.getEntity().getOInternal(loc) - damage, loc);
		}
	}
	
	@Override
    public String checkFixable() {
        return null;
    }
	
	@Override
	public boolean isSalvaging() {
		return salvaging && (loc == Tank.LOC_TURRET || loc == Tank.LOC_TURRET_2);
	}
	
	@Override
	public boolean canScrap() {
		return loc == Tank.LOC_TURRET || loc == Tank.LOC_TURRET_2;
	}

	@Override
	public double getTonnage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getCurrentValue() {
		// TODO Auto-generated method stub
		return 0;
	}
}
