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

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Rotor extends TankLocation {
	private static final long serialVersionUID = -122291037522319765L;

    public Rotor() {
    	this(0, null);
    }
    
    public Rotor(int tonnage, Campaign c) {
        super(VTOL.LOC_ROTOR, tonnage, c);
        this.name = "Rotor";
        this.damage = 0;
        this.time = 120;
        this.difficulty = 2;
    }
    
    public Rotor clone() {
    	Rotor clone = new Rotor(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
    	clone.loc = this.loc;
    	clone.damage = this.damage;
    	clone.breached = this.breached;
    	return clone;
    }
 
    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof Rotor 
        		&& getLoc() == ((Rotor)part).getLoc() 
        		&& getUnitTonnage() == ((Rotor)part).getUnitTonnage()
        		&& this.getDamage() == ((Rotor)part).getDamage()
        		&& part.getSkillMin() == this.getSkillMin();
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
		super.fix();
		damage--;
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(unit.getEntity().getInternal(VTOL.LOC_ROTOR)+1, VTOL.LOC_ROTOR);
		}
	}

	@Override
	public MissingPart getMissingPart() {
		return new MissingRotor(getUnitTonnage(), campaign);
	}

	@Override
	public void remove(boolean salvage) {
		if(null != unit && unit.getEntity() instanceof VTOL) {
			unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, VTOL.LOC_ROTOR);
			Part spare = campaign.checkForExistingSparePart(this);
			if(!salvage) {
				campaign.removePart(this);
			} else if(null != spare) {
				spare.incrementQuantity();
				campaign.removePart(this);
			}
			unit.removePart(this);
			Part missing = getMissingPart();
			unit.addPart(missing);
			campaign.addPart(missing);
			((VTOL)unit.getEntity()).resetMovementDamage();
			for(Part part : unit.getParts()) {
				if(part instanceof MotiveSystem) {
					part.updateConditionFromEntity();
				}
			}
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
			unit.getEntity().setInternal(unit.getEntity().getOInternal(VTOL.LOC_ROTOR) - damage, VTOL.LOC_ROTOR);
		}
	}
	
	@Override
	public boolean isSalvaging() {
		return salvaging;
	}
	
	@Override 
	public String checkFixable() {
		if(isSalvaging()) {
			//check for armor
	        if(unit.getEntity().getArmor(loc, false) != IArmorState.ARMOR_DESTROYED) {
	        	return "must salvage armor in this location first";
	        }
		}
		return null;
	}
	
	@Override
	public String checkScrappable() {
		//check for armor
        if(unit.getEntity().getArmor(loc, false) != IArmorState.ARMOR_DESTROYED) {
        	return "You must scrap armor in the rotor first";
        }
		return null;
	}
	
	@Override
	public boolean canNeverScrap() {
		return false;
	}
	
	@Override
	public double getTonnage() {
		return 0.1 * getUnitTonnage();
	}

	@Override
	public long getStickerPrice() {
		return (long)(40000 * getTonnage());
	}
}
