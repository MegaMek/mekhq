/*
 * Rotor.java
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
import megamek.common.VTOL;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Rotor extends Part {
	private static final long serialVersionUID = -122291037522319765L;

	private int damage;
	
    public Rotor() {
    	this(0);
    }
    
    public Rotor(int tonnage) {
        super(tonnage);
        this.name = "Rotor";
        this.damage = 0;
        this.time = 120;
        this.difficulty = 2;
    }
 
    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
    	if(needsFixing() || part.needsFixing()) {
    		return false;
    	}
        return part instanceof Rotor && getUnitTonnage() == part.getUnitTonnage();
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
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
			
			if (wn2.getNodeName().equalsIgnoreCase("damage")) {
				damage = Integer.parseInt(wn2.getTextContent());
			}
		}
	}

	@Override
	public int getAvailability(int era) {
		//go with conventional fighter avionics
		if(era == EquipmentType.ERA_SL) {
			return EquipmentType.RATING_C;
		} else if(era == EquipmentType.ERA_SW) {
			return EquipmentType.RATING_D;
		} else {
			return EquipmentType.RATING_C;
		}
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_B;
	}

	@Override
	public void fix() {
		damage--;
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(unit.getEntity().getInternal(VTOL.LOC_TURRET)+1, VTOL.LOC_TURRET);
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingRotor(getUnitTonnage());
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, VTOL.LOC_TURRET);
			if(!salvage) {
				unit.campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.campaign.addPart(missing);
			unit.addPart(missing);
		}
		setUnit(null);
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof VTOL) {
			if(IArmorState.ARMOR_DESTROYED == unit.getEntity().getInternal(VTOL.LOC_TURRET)) {
				remove(false);
			} else {
				damage = unit.getEntity().getOInternal(VTOL.LOC_TURRET) - unit.getEntity().getInternal(VTOL.LOC_TURRET);			
			}
		}
		if(isSalvaging()) {
			this.time = 300;
			this.difficulty = 2;
		}
	}

	@Override
	public boolean needsFixing() {
		return damage > 0;
	}
	
	@Override
    public String getDetails() {
		return damage + " points of damage";
    }
	
	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(unit.getEntity().getOInternal(VTOL.LOC_TURRET) - damage, VTOL.LOC_TURRET);
		}
	}
	
	@Override
    public String checkFixable() {
        return null;
    }

	@Override
	public double getTonnage() {
		return 0.1 * getUnitTonnage();
	}

	@Override
	public long getCurrentValue() {
		return (long)(40000 * getTonnage());
	}
}
