/*
 * VeeStabiliser.java
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

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Tank;
import mekhq.campaign.MekHqXmlUtil;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class VeeStabiliser extends Part {
	private static final long serialVersionUID = 6708245721569856817L;

	private int loc;
	
	public VeeStabiliser() {
		this(0, 0);
	}
	
	public VeeStabiliser(int tonnage, int loc) {
        super(tonnage);
        this.loc = loc;
        this.name = "Vehicle Stabiliser";
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof VeeStabiliser;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("loc")) {
				loc = Integer.parseInt(wn2.getTextContent());
			}
		}
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

	@Override
	public void fix() {
		hits = 0;
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).clearStabiliserHit(loc);
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingVeeStabiliser(getUnitTonnage(), loc);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).setStabiliserHit(loc);
			if(!salvage) {
				unit.campaign.removePart(this);
			}
			unit.removePart(this);
			setLocation(Entity.LOC_NONE);
			Part missing = getMissingPart();
			unit.campaign.addPart(missing);
			unit.addPart(missing);
		}
		setUnit(null);
	}

	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof Tank) {
			if(((Tank)unit.getEntity()).isStabiliserHit(loc)) {
				hits = 1;
			} else {
				hits = 0;
			}
		}
		if(hits > 0) {
			time = 60;
			difficulty = 1;
		} else {
			time = 0;
			difficulty = 0;
		}
		if(isSalvaging()) {
			time = 60;
			difficulty = 0;
		}
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Tank) {
			if(hits > 0 && !((Tank)unit.getEntity()).isStabiliserHit(loc)) {
				((Tank)unit.getEntity()).setStabiliserHit(loc);
			}
			else if(hits == 0 && ((Tank)unit.getEntity()).isStabiliserHit(loc)) {
				((Tank)unit.getEntity()).clearStabiliserHit(loc);
			}
		}
	}

	@Override
	public String checkFixable() {
		// TODO Auto-generated method stub
		return null;
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
	
	@Override
    public String getDetails() {
		if(null != unit) {
			return unit.getEntity().getLocationName(loc);
		}
		return "";
    }
	
	public int getLocation() {
		return loc;
	}
	
	public void setLocation(int l) {
		this.loc = l;
	}
}
