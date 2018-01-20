/*
 * MissingAvionics.java
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

import org.w3c.dom.Node;

import megamek.common.Aero;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.LandAirMech;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingAvionics extends MissingPart {

	/**
	 *
	 */
	private static final long serialVersionUID = 2806921577150714477L;

	public MissingAvionics() {
    	this(0, null);
    }

    public MissingAvionics(int tonnage, Campaign c) {
    	super(0, c);
    	this.name = "Avionics";
    }

    @Override
	public int getBaseTime() {
		return 4800;
	}

	@Override
	public int getDifficulty() {
		return 1;
	}

	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new Avionics(getUnitTonnage(), campaign);
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof Avionics;
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
			((Aero)unit.getEntity()).setAvionicsHits(3);
		} else if (null != unit && unit.getEntity() instanceof LandAirMech) {
		    unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMech.LAM_AVIONICS, 3);
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
	public TechAdvancement getTechAdvancement() {
	    return TA_GENERIC;
	}

}
