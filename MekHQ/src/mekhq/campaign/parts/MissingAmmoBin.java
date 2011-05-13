/*
 * MissingAmmoBin.java
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

import megamek.common.AmmoType;
import megamek.common.EquipmentType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingAmmoBin extends MissingEquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

    public MissingAmmoBin() {
    	this(false, 0, null, -1);
    }
    
    public MissingAmmoBin(boolean salvage, int tonnage, EquipmentType et, int equipNum) {
        super(salvage, tonnage, et, equipNum);
        this.difficulty = -2;
        if(null != name) {
        	this.name += " Bin";
        }
    }
	
	@Override
	public boolean isAcceptableReplacement(Part part) {
		if(part instanceof AmmoBin) {
			EquipmentPart eqpart = (EquipmentPart)part;
			EquipmentType et = eqpart.getType();
			return type.equals(et);
		}
		return false;
	}

	
	@Override
	public Part getNewPart() {
		return new AmmoBin(isSalvage(), getUnitTonnage(), type, -1, ((AmmoType)type).getShots());
	}
}
