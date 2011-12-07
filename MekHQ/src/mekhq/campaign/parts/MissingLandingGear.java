/*
 * MissingLandingGear.java
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

import megamek.common.Aero;
import megamek.common.EquipmentType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingLandingGear extends MissingPart {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2806921577150714477L;

	public MissingLandingGear() {
    	this(0);
    }
    
    public MissingLandingGear(int tonnage) {
    	super(0);
    	this.time = 1200;
    	this.difficulty = 2;
    	this.name = "Landing Gear";
    }
    
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new LandingGear(getUnitTonnage());
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof LandingGear;
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
		//go with conventional fighter avionics
		return EquipmentType.RATING_C;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setGearHit(true);
		}
	}
	
}