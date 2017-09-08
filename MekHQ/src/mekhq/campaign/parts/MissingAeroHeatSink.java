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

import org.w3c.dom.Node;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;

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
    	this(0, Aero.HEAT_SINGLE, false, null);
    }

    public MissingAeroHeatSink(int tonnage, int type, boolean omniPodded, Campaign c) {
    	super(tonnage, omniPodded, c);
    	this.type = type;
    	this.name = "Aero Heat Sink";
    }
    
    @Override 
	public int getBaseTime() {
		return isOmniPodded()? 30 : 90;
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
		return new AeroHeatSink(getUnitTonnage(), type, omniPodded, campaign);
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
		return "Fuselage";
	}

	@Override
	public int getLocation() {
		return Entity.LOC_NONE;
	}
	
    @Override
    public TechAdvancement getTechAdvancement() {
        if (type == Aero.HEAT_SINGLE) {
            return AeroHeatSink.TA_SINGLE;
        } else if (campaign.getFaction().isClan()) {
            return AeroHeatSink.TA_CLAN_DOUBLE;
        } else {
            return AeroHeatSink.TA_IS_DOUBLE;
        }
    }

    @Override
	public boolean isOmniPoddable() {
	    return true;
	}
}