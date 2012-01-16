/*
 * LandingGear.java
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

import megamek.common.Aero;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class LandingGear extends Part {

	/**
	 * 
	 */
	private static final long serialVersionUID = -717866644605314883L;

	public LandingGear() {
    	this(0, null);
    }
    
    public LandingGear(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Landing Gear";
    }
        
    public LandingGear clone() {
    	return new LandingGear(0, campaign);
    }
    
	@Override
	public void updateConditionFromEntity() {
		if(null != unit && unit.getEntity() instanceof Aero && ((Aero)unit.getEntity()).isGearHit()) {
			hits = 1;
		} else {
			hits = 0;
		}
		if(hits > 0) {
			time = 120;
			difficulty = 3;
		} else {
			time = 0;
			difficulty = 0;
		}
		if(isSalvaging()) {
			time = 1200;
			difficulty = 2;
		}
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setGearHit(needsFixing());
		}
		
	}

	@Override
	public void fix() {
		super.fix();
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setGearHit(false);
		}
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setGearHit(true);
			if(!salvage) {
				campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			campaign.addPart(missing);
			unit.addPart(missing);
		}
		setUnit(null);
	}

	@Override
	public Part getMissingPart() {
		return new MissingLandingGear(getUnitTonnage(), campaign);
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public boolean needsFixing() {
		return hits > 0;
	}

	@Override
	public long getStickerPrice() {
		return 10 * getUnitTonnage();
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		//go with conventional fighter avionics
		return EquipmentType.RATING_B;
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_C;
	}
	
	@Override
	public int getTechLevel() {
		return TechConstants.T_IS_TW_ALL;
	}
	
	@Override 
	public int getTechBase() {
		return T_BOTH;	
	}

	@Override
	public boolean isSamePartTypeAndStatus(Part part) {
		if(needsFixing() || part.needsFixing()) {
    		return false;
    	}
		return part instanceof LandingGear;
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		//nothing to load
	}
	
}