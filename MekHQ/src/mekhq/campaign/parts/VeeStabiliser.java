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

import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.Tank;
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
public class VeeStabiliser extends Part {
	private static final long serialVersionUID = 6708245721569856817L;

	private int loc;
	
	public VeeStabiliser() {
		this(0, 0, null);
	}
	
	public VeeStabiliser(int tonnage, int loc, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.name = "Vehicle Stabiliser";
    }
	
	public VeeStabiliser clone() {
		VeeStabiliser clone = new VeeStabiliser(getUnitTonnage(), 0, campaign);
        clone.copyRepairData(this);
		return clone;
	}

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof VeeStabiliser;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		writeToXmlEnd(pw1, indent);
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
	public int getTechLevel() {
		return TechConstants.T_INTRO_BOXSET;
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).clearStabiliserHit(loc);
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingVeeStabiliser(getUnitTonnage(), loc, campaign);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Tank) {
			((Tank)unit.getEntity()).setStabiliserHit(loc);
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing);
		}
		setSalvaging(false);
		setUnit(null);
		updateConditionFromEntity();
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
		if(!isSalvaging() && unit.isLocationBreached(loc)) {
    		return unit.getEntity().getLocationName(loc) + " is breached.";
		}
		return null;
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
	
	@Override
	public boolean isRightTechType(String skillType) {
		return skillType.equals(SkillType.S_TECH_MECHANIC);
	}
}
