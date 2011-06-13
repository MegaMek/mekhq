/*
 * MissingAeroHeatSink.java
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
public class MissingAeroHeatSink extends MissingPart {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2806921577150714477L;

	private int type;
	
	public MissingAeroHeatSink() {
    	this(0, Aero.HEAT_SINGLE);
    }
    
    public MissingAeroHeatSink(int tonnage, int type) {
    	super(0);
    	this.time = 90;
    	this.difficulty = -2;
    	this.type = type;
    	this.name = "Aero Heat Sink";
    }
    
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new AeroHeatSink(getUnitTonnage(), type);
	}

	@Override
	public long getPurchasePrice() {
		if(type == Aero.HEAT_DOUBLE) {
			return 6000;
		} else {
			return 2000;
		}
	}

	@Override
	public boolean isAcceptableReplacement(Part part) {
		return part instanceof AeroHeatSink && type == ((AeroHeatSink)part).getType();
	}

	@Override
	public double getTonnage() {
		return 1;
	}

	@Override
	public int getTechRating() {
		if(type == Aero.HEAT_DOUBLE) {
			return EquipmentType.RATING_D;
		} else {
			return EquipmentType.RATING_E;
		}
	}

	@Override
	public int getAvailability(int era) {
		if(type == Aero.HEAT_DOUBLE) {
		if(era == EquipmentType.ERA_SL) {
			return EquipmentType.RATING_C;
		} else if(era == EquipmentType.ERA_SW) {
			return EquipmentType.RATING_E;
		} else {
			return EquipmentType.RATING_D;
		}
		} else {
			return EquipmentType.RATING_B;
		}
	}
	
}