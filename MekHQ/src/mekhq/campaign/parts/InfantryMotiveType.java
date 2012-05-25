/*
 * InfantryMotiveType.java
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

import megamek.common.Engine;
import megamek.common.EntityMovementMode;
import megamek.common.Infantry;
import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.SkillType;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class InfantryMotiveType extends Part {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2915821210551422633L;
	
	private EntityMovementMode mode;

	public InfantryMotiveType(int tonnage, Campaign c, EntityMovementMode m) {
		super(tonnage, c);
		this.mode = m;
	}
	
	@Override
	public void updateConditionFromEntity() {
		//nothing to do here
	}

	@Override
	public void updateConditionFromPart() {
		//nothing to do here
	}

	@Override
	public void remove(boolean salvage) {
		//nothing to do here
	}

	@Override
	public Part getMissingPart() {
		//this should never be missing
		return null;
	}

	@Override
	public String checkFixable() {
		//nothing to do here
		return null;
	}

	@Override
	public boolean needsFixing() {
		return false;
	}

	@Override
	public long getStickerPrice() {
		 switch (getMovementMode()){
	        case INF_UMU:
	            return 17888;
	        case INF_MOTORIZED:
	        	return (long)(17888 * 0.6);
	        case INF_JUMP:
	        	return (long)(17888 * 1.6);
	        case HOVER:
	        case WHEELED:
	        case TRACKED:
	        	return (long)(17888 * 2.2);
	        default:
	            return 0;
     }
	}

	@Override
	public double getTonnage() {
		//TODO: what should this be?
		return 0;
	}

	@Override
	public int getTechRating() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getAvailability(int era) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getTechLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isSamePartType(Part part) {
		return part instanceof InfantryMotiveType && mode == ((InfantryMotiveType)part).getMovementMode();
	}

	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Part clone() {
		return new InfantryMotiveType(0, campaign, mode);
	}
	
	public EntityMovementMode getMovementMode() {
		return mode;
	}
	
}