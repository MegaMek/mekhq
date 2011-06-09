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

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Rotor extends TankLocation {
	private static final long serialVersionUID = -122291037522319765L;

    public Rotor() {
    	this(0);
    }
    
    public Rotor(int tonnage) {
        super(VTOL.LOC_TURRET, tonnage);
        this.name = "Rotor";
        this.damage = 0;
        this.time = 120;
        this.difficulty = 2;
    }
 
    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
    	if(needsFixing() || part.needsFixing()) {
    		return false;
    	}
        return part instanceof Rotor && getUnitTonnage() == part.getUnitTonnage();
    }

	@Override
	public int getAvailability(int era) {
		//go with conventional fighter avionics
		if(era == EquipmentType.ERA_SL) {
			return EquipmentType.RATING_C;
		} else if(era == EquipmentType.ERA_SW) {
			return EquipmentType.RATING_D;
		} else {
			return EquipmentType.RATING_C;
		}
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_B;
	}

	@Override
	public void fix() {
		damage--;
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(unit.getEntity().getInternal(VTOL.LOC_TURRET)+1, VTOL.LOC_TURRET);
		}
	}

	@Override
	public Part getMissingPart() {
		return new MissingRotor(getUnitTonnage());
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, VTOL.LOC_TURRET);
			if(!salvage) {
				unit.campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.campaign.addPart(missing);
			unit.addPart(missing);
		}
		setUnit(null);
	}

	@Override
	public void updateConditionFromEntity() {
		super.updateConditionFromEntity();
		if(isSalvaging()) {
			this.time = 300;
			this.difficulty = 2;
		}
	}
	
	@Override
	public void updateConditionFromPart() {
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(unit.getEntity().getOInternal(VTOL.LOC_TURRET) - damage, VTOL.LOC_TURRET);
		}
	}
	
	@Override
	public boolean isSalvaging() {
		return salvaging;
	}
	
	@Override
	public boolean canScrap() {
		return true;
	}
	
	@Override
	public double getTonnage() {
		return 0.1 * getUnitTonnage();
	}

	@Override
	public long getCurrentValue() {
		return (long)(40000 * getTonnage());
	}
}
