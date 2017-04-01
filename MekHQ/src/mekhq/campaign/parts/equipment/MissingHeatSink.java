/*
 * MissingEquipmentPart.java
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

package mekhq.campaign.parts.equipment;

import megamek.common.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingHeatSink extends MissingEquipmentPart {
	private static final long serialVersionUID = 2892728320891712304L;

	public MissingHeatSink() {
    	this(0, null, -1, false, null);
    }
    
    public MissingHeatSink(int tonnage, EquipmentType et, int equipNum, boolean omniPodded, Campaign c) {
        super(tonnage, et, equipNum, c, 1, omniPodded);
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
	public Part getNewPart() {
		return new HeatSink(getUnitTonnage(), type, -1, omniPodded, campaign);
	}
	
	@Override
	public int getRepairPartType() {
    	return Part.REPAIR_PART_TYPE.HEATSINK;
    }
}
