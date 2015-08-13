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
import megamek.common.Entity;
import megamek.common.EquipmentType;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;

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
    	this(0, Aero.HEAT_SINGLE, null);
    }

    public MissingAeroHeatSink(int tonnage, int type, Campaign c) {
    	super(tonnage, c);
    	this.type = type;
    	this.name = "Aero Heat Sink";
    }
    
    @Override 
	public int getBaseTime() {
		return 90;
	}
	
	@Override
	public int getDifficulty() {
		return -2;
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new AeroHeatSink(getUnitTonnage(), type, campaign);
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
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

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			if(hits == 0) {
				((Aero)unit.getEntity()).setHeatSinks(((Aero)unit.getEntity()).getHeatSinks()-1);
			}
		}
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		//nothing to load
	}

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		return Entity.LOC_NONE;
	}
	
	@Override
	public int getIntroDate() {
		if(type == Aero.HEAT_DOUBLE) {
			return 2567;
		}
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getExtinctDate() {
		//TODO: we should distinguish clan and IS here for extinction purposes
		/*if(type == Aero.HEAT_DOUBLE) {
		 * if(!isClan()) {
				return 2865;
			}
		}*/
		return EquipmentType.DATE_NONE;
	}

	@Override
	public int getReIntroDate() {
		return 3040;
	}
}