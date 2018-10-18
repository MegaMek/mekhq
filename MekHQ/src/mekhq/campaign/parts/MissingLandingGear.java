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
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;

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
    	this(0, null);
    }
    
    public MissingLandingGear(int tonnage, Campaign c) {
    	super(0, c);
    	this.name = "Landing Gear";
    }
    
    @Override 
	public int getBaseTime() {
        if (campaign.getCampaignOptions().useAeroSystemHits()) {
            int time = 0;
            //Test of proposed errata for repair times
            Entity e = unit.getEntity();
            if (e.hasETypeFlag(Entity.ETYPE_DROPSHIP) || e.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
                time = 1200;
            } else {
                time = 600;
            }
            return time;
        }
        return 1200;
    }
	
	@Override
	public int getDifficulty() {
		return 2;
	}
    
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new LandingGear(getUnitTonnage(), campaign);
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
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Aero) {
			((Aero)unit.getEntity()).setGearHit(true);
        } else if (null != unit && unit.getEntity() instanceof LandAirMech) {
            unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_LANDING_GEAR, 3);
        }
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		//nothing to load
	}

    @Override
    public String getLocationName() {
        if (null != unit) {
            return unit.getEntity().getLocationName(unit.getEntity().getBodyLocation());
        }
        return null;
    }

    @Override
    public int getLocation() {
        if (null != unit) {
            return unit.getEntity().getBodyLocation();
        }
        return Entity.LOC_NONE;
    }
    
    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }
	
}