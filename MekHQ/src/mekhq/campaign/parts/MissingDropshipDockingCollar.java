/*
 * MissingDropshipDockingCollar.java
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

import megamek.common.Dropship;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingDropshipDockingCollar extends MissingPart {

	/**
	 * 
	 */
	private static final long serialVersionUID = -717866644605314883L;

	
	public MissingDropshipDockingCollar() {
    	this(0, null);
    }
    
    public MissingDropshipDockingCollar(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Dropship Docking Collar";
        time = 2880;
		difficulty = -2;
    }

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof Dropship) {
			((Dropship)unit.getEntity()).setDamageDockCollar(true);
		}	
		
	}

	@Override
	public Part getNewPart() {
		return new DropshipDockingCollar(getUnitTonnage(), campaign);
	}

	@Override
	public String checkFixable() {
		return null;
	}

	
	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_C;
	}

	@Override
	public int getAvailability(int era) {
		if(era == EquipmentType.ERA_SL) {
			return EquipmentType.RATING_C;
		} else if(era == EquipmentType.ERA_SW) {
			return EquipmentType.RATING_D;
		} else {
			return EquipmentType.RATING_C;
		}
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
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		//nothing
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof DropshipDockingCollar;
	}
	
}