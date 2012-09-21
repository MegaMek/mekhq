/*
 * Rotor.java
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

import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.VTOL;
import mekhq.campaign.Campaign;

import org.w3c.dom.Node;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingRotor extends MissingPart {
	private static final long serialVersionUID = -3277611762625095964L;

	public MissingRotor() {
		this(0, null);
	}
	
	public MissingRotor(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Rotor";
        this.time = 300;
        this.difficulty = 0;
    }

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		// Do nothing - no fields to load.
	}

	@Override
	public int getAvailability(int era) {
		if(era == EquipmentType.ERA_SW) {
			return EquipmentType.RATING_D;
		}
		return EquipmentType.RATING_C;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_B;
	}


	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof Rotor && part.getUnitTonnage() == getUnitTonnage();
	}
	
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		//TODO: how to get second turret location?
		return new Rotor(getUnitTonnage(), campaign);
	}

	@Override
	public double getTonnage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, VTOL.LOC_ROTOR);
		}
	}
}
